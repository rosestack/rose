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

/**
 * 频繁请求异常，http 状态返回码为 404
 *
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since 0.0.1
 */
public class TooManyRequestException extends RuntimeException {

    public TooManyRequestException(final String message) {
        super(message);
    }

    /**
     * <p>
     * Constructor for RequestNotPermittedException.
     * </p>
     *
     * @param message a {@link String} object
     * @param cause   a {@link Throwable} object
     */
    public TooManyRequestException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
