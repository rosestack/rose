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
package io.github.rose.security.support;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationServiceException;

public class DefaultTokenExtractor implements TokenExtractor {

    @Override
    public String extract(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION);
        if (StringUtils.isNotBlank(header)) {
            if (header.length() < HEADER_PREFIX.length()) {
                throw new AuthenticationServiceException("Invalid authorization header size.");
            }
            header = header.substring(HEADER_PREFIX.length());
        } else {
            header = request.getParameter(REQUEST_PREFIX);
        }

        if (StringUtils.isBlank(header)) {
            throw new AuthenticationServiceException("Authorization header cannot be blank!");
        }
        return header;
    }
}
