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
package io.github.rose.core.benchmarks;

import io.github.rose.core.reflect.Annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class AnnotationsBenchmark {
    private static Method notAnnotatedMethod;

    static {
        try {
            notAnnotatedMethod = MetaAnnotatedByInterface.class.getDeclaredMethod("notAnnotatedMethod");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    public void findMetaAnnotationByInterface() {
        Annotations.on(notAnnotatedMethod)
                .fallingBackOnClasses()
                .includingMetaAnnotations()
                .traversingSuperclasses()
                .traversingInterfaces()
                .find(TypeAnnotation.class)
                .get();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    private @interface TypeAnnotation {}

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @TypeAnnotation
    private @interface TypeMetaAnnotation {}

    @TypeMetaAnnotation
    private interface MetaAnnotatedInterface {
        void notAnnotatedMethod();
    }

    private static class MetaAnnotatedByInterface implements MetaAnnotatedInterface {
        Object notAnnotatedField;

        MetaAnnotatedByInterface() {}

        public void notAnnotatedMethod() {}

        void notAnnotatedLocalMethod() {}
    }
}
