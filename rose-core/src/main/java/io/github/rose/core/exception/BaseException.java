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
package io.github.rose.core.exception;

import io.github.rose.core.util.text.TextFormatUtils;
import io.github.rose.core.util.text.TextUtils;
import io.github.rose.core.util.text.TextWrapper;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the base class for all technical SeedStack exceptions. It provides additional information
 * over traditional exception: detailed message, fix advice and online URL. Extra attributes can be
 * added to the exception and used in message templates.
 * <p>
 *
 * @from SeedStack
 */
public abstract class BaseException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private static final String MULTIPLE_CAUSES_PATTERN = "%d. %s";
    private static final String CAUSE_PATTERN = "%s%n\tat %s.%s(%s:%d)";
    private static final String ERROR_CODE_PATTERN = "%s";
    private static final String JAVA_LANG_THROWABLE = "java.lang.Throwable";
    private static final String PRINT_STACK_TRACE = "printStackTrace";
    private static final String CONSTRUCTOR = "<init>";
    private static final TextWrapper textWrapper = new TextWrapper(120);

    private final ErrorCode errorCode;
    private final Map<String, Object> properties = new HashMap<>();
    private final AtomicBoolean alreadyComputed = new AtomicBoolean(false);
    private final ThreadLocal<Boolean> alreadyVisited = ThreadLocal.withInitial(() -> false);

    private List<String> causes;
    private String message;

    /**
     * Create a BaseException from an {@link ErrorCode}.
     *
     * @param errorCode the error code.
     */
    protected BaseException(ErrorCode errorCode) {
        super(formatErrorCode(errorCode));
        this.errorCode = errorCode;
    }

    /**
     * Create a BaseException from an {@link ErrorCode} wrapping a {@link Throwable}.
     *
     * @param errorCode the error code.
     * @param cause     the cause of this exception if any.
     */
    protected BaseException(ErrorCode errorCode, Throwable cause) {
        super(formatErrorCode(errorCode), cause);
        this.errorCode = errorCode;
    }

    private static String formatErrorCode(ErrorCode errorCode) {
        String name = errorCode.toString().toLowerCase(Locale.ENGLISH).replace("_", " ");
        return String.format(ERROR_CODE_PATTERN, name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1));
    }

    /**
     * Create a new subclass of BaseException from an {@link ErrorCode}.
     *
     * @param exceptionType the subclass of BaseException to create.
     * @param errorCode     the error code to set.
     * @param <E>           the subtype.
     * @return the created BaseException.
     */
    public static <E extends BaseException> E createNew(Class<E> exceptionType, ErrorCode errorCode) {
        try {
            Constructor<E> constructor = exceptionType.getDeclaredConstructor(ErrorCode.class);
            constructor.setAccessible(true);
            return constructor.newInstance(errorCode);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException
                | InstantiationException e) {
            throw new IllegalArgumentException(
                    exceptionType.getCanonicalName() + " must implement a constructor with ErrorCode as parameter", e);
        }
    }

    /**
     * Wrap a subclass of BaseException with an {@link ErrorCode} around an existing {@link
     * Throwable}.
     *
     * @param exceptionType the subclass of BaseException to create.
     * @param throwable     the existing throwable to wrap.
     * @param errorCode     the error code to set.
     * @param <E>           the subtype.
     * @return the created BaseException.
     */
    public static <E extends BaseException> E wrap(Class<E> exceptionType, Throwable throwable, ErrorCode errorCode) {
        try {
            Constructor<E> constructor = exceptionType.getDeclaredConstructor(ErrorCode.class, Throwable.class);
            constructor.setAccessible(true);
            return constructor.newInstance(errorCode, throwable);
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException
                | InstantiationException e) {
            throw new IllegalArgumentException(
                    exceptionType.getCanonicalName()
                            + " must implement a constructor with an ErrorCode and a Throwable as parameters",
                    e);
        }
    }

    /**
     * Retrieve the {@link ErrorCode} of this exception.
     *
     * @return the error code instance.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Return the properties of this exception.
     *
     * @return the map of the properties.
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Return a property of this exception.
     *
     * @param name the name of the property.
     * @param <T>  the type of the property.
     * @return the value of the property.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        return (T) properties.get(name);
    }

    /**
     * Put a property in this exception.
     *
     * @param name  the name of the property.
     * @param value the value of the property.
     * @param <E>   the type fo the property.
     * @return this exception (to chain calls).
     */
    @SuppressWarnings("unchecked")
    public <E extends BaseException> E put(String name, Object value) {
        properties.put(name, value);
        return (E) this;
    }

    /**
     * The toString() method is overloaded to provide additional exception details. When invoked
     * directly it only returns the details of this exception. When invoked from printStackTrace() it
     * returns the details of this exception and flags all causes of BaseException type to only
     * display their short message when their toString() method will be invoked by printStacktrace().
     * This uses a ThreadLocal implementation of the flag to stay thread-safe.
     *
     * @return a textual representation of the exception.
     */
    @Override
    public String toString() {
        int location = getLocation();

        if (location == 1) {
            // if called from throwable constructor or from a verbose Guice exception
            // we return the simple message to avoid messing the stack trace
            return getMessage();
        }

        if (location == 2) {
            // if called from printStackTrace() we ensure that only the first BaseException is fully
            // displayed
            try {
                if (alreadyVisited.get()) {
                    // Already displayed in the cause list of the first BaseException
                    return super.toString();
                } else {
                    // First BaseException to be displayed in a causal chain
                    Throwable theCause = getCause();
                    while (theCause != null) {
                        if (theCause instanceof BaseException) {
                            ((BaseException) theCause).alreadyVisited.set(true);
                        }
                        theCause = theCause.getCause();
                    }
                }
            } finally {
                alreadyVisited.remove();
            }
        }

        compute();

        StringBuilder s = new StringBuilder(16384);

        s.append(super.toString());

        String roseMessage = getDescription();
        if (roseMessage != null) {
            ensureBlankLine(s);
            s.append("Description\n-----------\n");
            s.append(textWrapper.wrap(roseMessage));
        }

        int i = causes.size();
        if (i == 1) {
            ensureBlankLine(s);
            s.append("Cause\n-----\n");
            s.append(textWrapper.wrap(causes.get(0)));
        } else if (i > 1) {
            ensureBlankLine(s);
            s.append("Causes\n------\n");
            int count = 1;
            for (String seedCause : causes) {
                ensureBlankLine(s);
                s.append(String.format(
                        MULTIPLE_CAUSES_PATTERN, count, TextUtils.leftPad(textWrapper.wrap(seedCause), "   ", 1)));
                count++;
            }
        }

        if (location == 2) {
            // this header is displayed only if called from printStackTrace()
            ensureBlankLine(s);
            s.append("Stacktrace\n----------");
        }

        return s.toString();
    }

    private void ensureBlankLine(StringBuilder s) {
        if (s.charAt(s.length() - 1) != '\n') {
            s.append("\n\n");
        } else if (s.charAt(s.length() - 2) != '\n') {
            s.append("\n");
        }
    }

    /**
     * Provides additional information beyond the short message.
     *
     * @return the exception description or null if none exists.
     */
    public String getDescription() {
        compute();
        return this.message;
    }

    /**
     * Provides a list describing the causes of this exception. This list is built by iterating
     * through this exception causes and storing the description through {@link #getMessage()} if
     * present or the message through {@link #getMessage()} as a fallback.
     *
     * @return the list of causes, possibly empty.
     */
    public List<String> getCauses() {
        compute();
        return this.causes;
    }

    private void compute() {
        if (alreadyComputed.getAndSet(true)) {
            return;
        }

        causes = new ArrayList<>();

        Throwable theCause = getCause();
        while (theCause != null) {
            String causeMessage;
            if (theCause instanceof BaseException) {
                BaseException seedCause = (BaseException) theCause;
                Map<String, Object> processedProperties = processProperties(seedCause.getProperties());

                // Collects all cause messages from highest to lowest level
                String seedCauseErrorTemplate = seedCause.getTemplate(null);
                if (seedCauseErrorTemplate != null) {
                    causeMessage = String.format(
                            ERROR_CODE_PATTERN,
                            TextFormatUtils.substituteVariables(seedCauseErrorTemplate, processedProperties));
                } else {
                    causeMessage = seedCause.getMessage();
                }
            } else {
                causeMessage = theCause.toString();
            }
            StackTraceElement stackTraceElement = findRelevantStackTraceElement(theCause);
            if (stackTraceElement != null) {
                causes.add(String.format(
                        CAUSE_PATTERN,
                        causeMessage,
                        stackTraceElement.getClassName(),
                        stackTraceElement.getMethodName(),
                        stackTraceElement.getFileName(),
                        stackTraceElement.getLineNumber()));
            }

            theCause = theCause.getCause();
        }

        Map<String, Object> processedProperties = processProperties(getProperties());

        if (message == null) {
            String messageTemplate = getTemplate(null);
            if (messageTemplate != null) {
                message = TextFormatUtils.substituteVariables(messageTemplate, processedProperties);
            }
        }
    }

    private Map<String, Object> processProperties(Map<String, Object> properties) {
        Map<String, Object> processedProperties = new HashMap<>();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Class<?>) {
                if (Annotation.class.isAssignableFrom(((Class) value))) {
                    processedProperties.put(entry.getKey(), "@" + ((Class) value).getSimpleName());
                } else {
                    processedProperties.put(entry.getKey(), getSourceLocation((Class) value, false));
                }
            } else if (value instanceof Method) {
                processedProperties.put(
                        entry.getKey(),
                        ((Method) value).getName() + (((Method) value).getParameters().length > 0 ? "(...)" : "()"));
            } else if (value instanceof Field) {
                processedProperties.put(entry.getKey(), ((Field) value).getName());
            } else {
                processedProperties.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return processedProperties;
    }

    private String getSourceLocation(Class someClass, boolean simple) {
        if (someClass.getDeclaringClass() == null && Modifier.isPublic(someClass.getModifiers())) {
            return (simple ? "." + someClass.getSimpleName() : someClass.getName()) + "(" + someClass.getSimpleName()
                    + ".java" + ":1)";
        } else {
            return simple ? someClass.getSimpleName() : someClass.getName();
        }
    }

    private StackTraceElement findRelevantStackTraceElement(Throwable t) {
        for (StackTraceElement stackTraceElement : t.getStackTrace()) {
            if (!stackTraceElement.getClassName().endsWith("Exception")) {
                return stackTraceElement;
            }
        }
        return null;
    }

    private String getTemplate(String key) {
        try {
            return ResourceBundle.getBundle(errorCode.getClass().getName())
                    .getString(key == null ? errorCode.toString() : errorCode.toString() + "." + key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    private int getLocation() {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            // In Throwable constructor
            if (JAVA_LANG_THROWABLE.equals(stackTraceElement.getClassName())
                    && CONSTRUCTOR.equals(stackTraceElement.getMethodName())) {
                return 1;
            }
            // In Throwable printStackTrace
            if (JAVA_LANG_THROWABLE.equals(stackTraceElement.getClassName())
                    && PRINT_STACK_TRACE.equals(stackTraceElement.getMethodName())) {
                return 2;
            }
        }
        // Elsewhere
        return 0;
    }
}
