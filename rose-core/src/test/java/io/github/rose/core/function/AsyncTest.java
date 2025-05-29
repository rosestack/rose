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

import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.rose.core.util.concurrent.Async;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since
 */
class AsyncTest {

    @Test
    void testNoCustomExecutor() {
        CompletionStage<Void> completionStage = Async.runAsync(() -> {});
        assertNull(completionStage.toCompletableFuture().join());

        completionStage = Async.supplyAsync(() -> null);
        assertNull(completionStage.toCompletableFuture().join());
    }
}
