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
package io.github.rose.core.function;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * A {@link Callable} that allows for checked exceptions.
 *
 * @author Zack Young
 */
@FunctionalInterface
public interface CheckedCallable<T> extends Callable<T> {
    /**
     * @see {@link Unchecked#callable(CheckedCallable)}
     */
    static <T> Callable<T> unchecked(CheckedCallable<T> callable) {
        return Unchecked.callable(callable);
    }

    /**
     * @see {@link Unchecked#callable(CheckedCallable, Consumer)}
     */
    static <T> Callable<T> unchecked(CheckedCallable<T> callable, Consumer<Throwable> handler) {
        return Unchecked.callable(callable, handler);
    }

    /**
     * Run this callable.
     */
    T call() throws Exception;
}
