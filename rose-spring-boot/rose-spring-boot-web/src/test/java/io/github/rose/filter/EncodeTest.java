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
package io.github.rose.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.owasp.encoder.Encode;

/**
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since
 */
class EncodeTest {
    @Test
    void test() {
        String url =
                "https://www.example.com/?param=" + Encode.forUriComponent("<script>alert(\"XSS Attack\");</script>");
        Assertions.assertNotNull(url);

        String userInput = "<script>alert(\"XSS Attack\");</script>";
        String escapedHtml = Encode.forHtml(userInput);
        Assertions.assertNotNull(escapedHtml);
    }
}
