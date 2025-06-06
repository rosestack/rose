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
package io.github.rose.security.rest.mfa.config;

import io.github.rose.security.rest.mfa.provider.MfaProviderType;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class EmailMfaConfig extends OtpBasedMfaConfig {

    @NotBlank
    @Email
    private String email;

    @Override
    public MfaProviderType getProviderType() {
        return MfaProviderType.EMAIL;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "EmailMfaConfig{" + "email='" + email + '\'' + ", serializeHiddenFields=" + serializeHiddenFields + '}';
    }
}
