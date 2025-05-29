/*
 * Copyright © 2025 rosestack.github.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.rose.core.reflect;

import static io.github.rose.core.reflect.ExecutablePredicates.executableIsEquivalentTo;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class Annotations {
    private static final String JAVA_LANG = "java.lang";
    private static final String KOTLIN_ANNOTATION = "kotlin.annotation";
    private static Map<Context, List<Annotation>> cache = new ConcurrentHashMap<>();

    private Annotations() {
        // no instantiation allowed
    }

    public static OnAnnotatedElement on(AnnotatedElement annotatedElement) {
        return new OnAnnotatedElement(new Context(annotatedElement));
    }

    public static OnAnnotatedElement on(Field field) {
        return new OnAnnotatedElement(new Context(field));
    }

    public static OnExecutable on(Executable someExecutable) {
        return new OnExecutable(new Context(someExecutable));
    }

    public static OnClass on(Class<?> someClass) {
        return new OnClass(new Context(someClass));
    }

    public static class OnClass {
        final Context context;

        OnClass(Context context) {
            this.context = context;
        }

        public OnClass traversingSuperclasses() {
            context.setTraversingSuperclasses(true);
            return this;
        }

        public OnClass traversingInterfaces() {
            context.setTraversingInterfaces(true);
            return this;
        }

        public OnClass includingMetaAnnotations() {
            context.setIncludingMetaAnnotations(true);
            return this;
        }

        /**
         * Return the first annotation of the specified class.
         *
         * @param annotationClass the class of the annotation to return.
         * @param <T>             the annotation type.
         * @return the optionally found annotation.
         */
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Optional<T> find(Class<T> annotationClass) {
            return (Optional<T>) findAll()
                    .filter(AnnotationPredicates.annotationIsOfClass(annotationClass))
                    .findFirst();
        }

        /**
         * Returns a stream of all the annotations found.
         *
         * @return a stream of annotation objects.
         */
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Stream<T> findAll(Class<T> annotationClass) {
            return (Stream<T>)
                    cache.get(context).stream().filter(AnnotationPredicates.annotationIsOfClass(annotationClass));
        }

        /**
         * Returns a stream of all the annotations found.
         *
         * @return a stream of annotation objects.
         */
        public Stream<Annotation> findAll() {
            return cache.computeIfAbsent(context, Context::gather).stream();
        }
    }

    public static class OnAnnotatedElement extends OnClass {
        OnAnnotatedElement(Context context) {
            super(context);
        }

        public OnClass fallingBackOnClasses() {
            context.setFallingBackOnClasses(true);
            return this;
        }
    }

    public static class OnExecutable extends OnAnnotatedElement {
        OnExecutable(Context context) {
            super(context);
        }

        public OnExecutable traversingOverriddenMembers() {
            context.setTraversingOverriddenMembers(true);
            return this;
        }
    }

    private static final class Context {
        private final AnnotatedElement annotatedElement;
        private boolean traversingInterfaces = false;
        private boolean traversingSuperclasses = false;
        private boolean traversingOverriddenMembers = false;
        private boolean fallingBackOnClasses = false;
        private boolean includingMetaAnnotations = false;
        private int hashCode;

        private Context(AnnotatedElement annotatedElement) {
            this.annotatedElement = annotatedElement;
        }

        void setTraversingInterfaces(boolean traversingInterfaces) {
            this.traversingInterfaces = traversingInterfaces;
        }

        void setTraversingSuperclasses(boolean traversingSuperclasses) {
            this.traversingSuperclasses = traversingSuperclasses;
        }

        void setTraversingOverriddenMembers(boolean traversingOverriddenMembers) {
            this.traversingOverriddenMembers = traversingOverriddenMembers;
        }

        void setFallingBackOnClasses(boolean fallingBackOnClasses) {
            this.fallingBackOnClasses = fallingBackOnClasses;
        }

        void setIncludingMetaAnnotations(boolean includingMetaAnnotations) {
            this.includingMetaAnnotations = includingMetaAnnotations;
        }

        List<Annotation> gather() {
            List<Annotation> annotations = new ArrayList<>(32);
            gather(annotations);
            return annotations;
        }

        private void gather(List<Annotation> list) {
            List<AnnotatedElement> annotatedElements = new ArrayList<>();

            if (annotatedElement instanceof Field) {
                annotatedElements.add(annotatedElement);
                if (fallingBackOnClasses) {
                    annotatedElements.add(((Field) annotatedElement).getDeclaringClass());
                }
            } else if (annotatedElement instanceof Method) {
                if (traversingOverriddenMembers) {
                    Classes.from(((Method) annotatedElement).getDeclaringClass())
                            .traversingInterfaces()
                            .traversingSuperclasses()
                            .methods()
                            .filter(executableIsEquivalentTo(((Method) annotatedElement)))
                            .forEach(method -> {
                                annotatedElements.add(method);
                                if (fallingBackOnClasses) {
                                    annotatedElements.add(method.getDeclaringClass());
                                }
                            });
                } else {
                    annotatedElements.add(annotatedElement);
                    if (fallingBackOnClasses) {
                        annotatedElements.add(((Method) annotatedElement).getDeclaringClass());
                    }
                }
            } else if (annotatedElement instanceof Constructor) {
                if (traversingOverriddenMembers) {
                    Classes.from(((Constructor) annotatedElement).getDeclaringClass())
                            .traversingSuperclasses()
                            .constructors()
                            .filter(executableIsEquivalentTo(((Constructor) annotatedElement)))
                            .forEach(constructor -> {
                                annotatedElements.add(constructor);
                                if (fallingBackOnClasses) {
                                    annotatedElements.add(constructor.getDeclaringClass());
                                }
                            });
                } else {
                    annotatedElements.add(annotatedElement);
                    if (fallingBackOnClasses) {
                        annotatedElements.add(((Constructor) annotatedElement).getDeclaringClass());
                    }
                }
            } else {
                annotatedElements.add(annotatedElement);
            }

            for (AnnotatedElement ae : annotatedElements) {
                if (ae instanceof Class<?> && (traversingInterfaces || traversingSuperclasses)) {
                    Classes.FromClass from = Classes.from(((Class<?>) ae));
                    if (traversingInterfaces) {
                        from.traversingInterfaces();
                    }
                    if (traversingSuperclasses) {
                        from.traversingSuperclasses();
                    }
                    from.classes().forEach(c -> findAnnotations(c, list));
                } else {
                    findAnnotations(ae, list);
                }
            }
        }

        private void findAnnotations(AnnotatedElement ae, List<Annotation> list) {
            for (Annotation annotation : ae.getAnnotations()) {
                Class<? extends Annotation> annotationType = annotation.annotationType();
                String annotationPackageName = annotationType.getPackage().getName();
                if (!annotationPackageName.startsWith(JAVA_LANG)
                        && !annotationPackageName.startsWith(KOTLIN_ANNOTATION)
                        && !annotationType.equals(ae)) {
                    list.add(annotation);
                    if (includingMetaAnnotations) {
                        findAnnotations(annotationType, list);
                    }
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || Context.class != o.getClass()) {
                return false;
            }

            Context context = (Context) o;

            if (traversingInterfaces != context.traversingInterfaces) {
                return false;
            }
            if (traversingSuperclasses != context.traversingSuperclasses) {
                return false;
            }
            if (traversingOverriddenMembers != context.traversingOverriddenMembers) {
                return false;
            }
            if (fallingBackOnClasses != context.fallingBackOnClasses) {
                return false;
            }
            if (includingMetaAnnotations != context.includingMetaAnnotations) {
                return false;
            }
            return annotatedElement.equals(context.annotatedElement);
        }

        @Override
        public int hashCode() {
            int result = hashCode;
            if (result == 0) {
                result = annotatedElement.hashCode();
                result = 31 * result + (traversingInterfaces ? 1 : 0);
                result = 31 * result + (traversingSuperclasses ? 1 : 0);
                result = 31 * result + (traversingOverriddenMembers ? 1 : 0);
                result = 31 * result + (fallingBackOnClasses ? 1 : 0);
                result = 31 * result + (includingMetaAnnotations ? 1 : 0);
                hashCode = result;
            }
            return result;
        }
    }
}
