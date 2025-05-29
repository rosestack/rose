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
package io.github.rose.core.util;

import com.github.f4b6a3.uuid.UuidCreator;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since
 */
public abstract class Uuids {
    // Use Java 8's built-in Base64 encoder/decoder
    private static final Base64.Encoder URL_SAFE_ENCODER =
            Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_SAFE_DECODER = Base64.getUrlDecoder();

    private Uuids() {}

    public static UUID getUUID() {
        return UuidCreator.getTimeOrderedEpoch(); // UUIDv7
    }

    /**
     * Generate a UUID and encode it to a URL-safe Base64 string.
     *
     * @return A URL-safe Base64 encoded UUID string.
     */
    public static String uuidToBase64(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return URL_SAFE_ENCODER.encodeToString(bb.array());
    }

    /**
     * Decode a URL-safe Base64 string back to a UUID.
     *
     * @param base64 A URL-safe Base64 encoded UUID string.
     * @return The decoded UUID.
     */
    public static UUID base64ToUuid(String base64) {
        byte[] bytes = URL_SAFE_DECODER.decode(base64);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}
