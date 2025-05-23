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
package io.github.rose.security.rest.token;

import io.github.rose.security.util.SecurityUser;

public class RestRefreshAuthenticationToken extends AbstractRestAuthenticationToken {

    private static final long serialVersionUID = -1311042791508924523L;

    public RestRefreshAuthenticationToken(String refreshToken) {
        super(refreshToken);
    }

    public RestRefreshAuthenticationToken(SecurityUser securityUser) {
        super(securityUser);
    }
}
