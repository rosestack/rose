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

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A {@link Predicate} that allows for checked exceptions.
 *
 * @author Lukas Eder
 */
@FunctionalInterface
public interface CheckedPredicate<T> {
    /**
     * @see {@link Unchecked#predicate(CheckedPredicate)}
     */
    static <T> Predicate<T> unchecked(CheckedPredicate<T> predicate) {
        return Unchecked.predicate(predicate);
    }

    /**
     * @see {@link Unchecked#predicate(CheckedPredicate, Consumer)}
     */
    static <T> Predicate<T> unchecked(CheckedPredicate<T> function, Consumer<Throwable> handler) {
        return Unchecked.predicate(function, handler);
    }

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param t the input argument
     * @return {@code true} if the input argument matches the predicate, otherwise
     * {@code false}
     */
    boolean test(T t) throws Exception;
}
