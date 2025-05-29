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
package io.github.rose.security.rest.mfa;

import io.github.rose.core.json.JsonUtils;
import io.github.rose.security.rest.mfa.config.MfaConfig;
import io.github.rose.security.rest.mfa.provider.MfaProviderConfig;
import io.github.rose.security.rest.mfa.provider.MfaProviderType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.mfa", ignoreUnknownFields = false)
public class MfaProperties {

    private boolean enabled = false;

    @Valid
    @NotEmpty
    private List<Map<String, Object>> providers;

    @NotNull
    @Min(value = 5)
    private Integer minVerificationCodeSendPeriod;

    @Min(value = 0, message = "must be positive")
    private Integer maxVerificationFailuresBeforeUserLockout;

    @NotNull
    @Min(value = 60)
    private Long totalAllowedTimeForVerification = 3600L; // sec

    @Valid
    @NotNull
    private List<Map<String, Object>> configs;

    public MfaProperties() {}

    public List<MfaConfig> getAllConfigs() {
        return configs.stream()
                .map(twoFaConfig -> JsonUtils.fromJson(JsonUtils.toJson(twoFaConfig), MfaConfig.class))
                .collect(Collectors.toList());
    }

    public MfaConfig getDefaultConfig() {
        return getAllConfigs().stream()
                .filter(MfaConfig::isUseByDefault)
                .findAny()
                .orElse(null);
    }

    public Optional<MfaProviderConfig> getProviderConfig(MfaProviderType providerType) {
        return Optional.ofNullable(providers).flatMap(providersConfigs -> providersConfigs.stream()
                .map(providerConfig -> JsonUtils.fromJson(JsonUtils.toJson(providerConfig), MfaProviderConfig.class))
                .filter(providerConfig -> providerConfig.getProviderType().equals(providerType))
                .findFirst());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Map<String, Object>> getProviders() {
        return providers;
    }

    public void setProviders(List<Map<String, Object>> providers) {
        this.providers = providers;
    }

    public Integer getMinVerificationCodeSendPeriod() {
        return minVerificationCodeSendPeriod;
    }

    public void setMinVerificationCodeSendPeriod(Integer minVerificationCodeSendPeriod) {
        this.minVerificationCodeSendPeriod = minVerificationCodeSendPeriod;
    }

    public Integer getMaxVerificationFailuresBeforeUserLockout() {
        return maxVerificationFailuresBeforeUserLockout;
    }

    public void setMaxVerificationFailuresBeforeUserLockout(Integer maxVerificationFailuresBeforeUserLockout) {
        this.maxVerificationFailuresBeforeUserLockout = maxVerificationFailuresBeforeUserLockout;
    }

    public Long getTotalAllowedTimeForVerification() {
        return totalAllowedTimeForVerification;
    }

    public void setTotalAllowedTimeForVerification(Long totalAllowedTimeForVerification) {
        this.totalAllowedTimeForVerification = totalAllowedTimeForVerification;
    }

    public List<Map<String, Object>> getConfigs() {
        return configs;
    }

    public void setConfigs(List<Map<String, Object>> configs) {
        this.configs = configs;
    }
}
