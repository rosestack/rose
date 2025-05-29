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
package org.springframework.cloud.openfeign;

import io.github.rose.feign.FeignAutoConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * <p>
 * feign 自动配置功能 from mica
 */
public class AutoFeignClientsRegistrar
        implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware, EnvironmentAware {
    private static final Logger log = LoggerFactory.getLogger(AutoFeignClientsRegistrar.class);

    private static final String BASE_URL = "http://127.0.0.1:${server.port}${server.servlet.context-path}";

    private ClassLoader beanClassLoader;

    private Environment environment;

    public ClassLoader getBeanClassLoader() {
        return beanClassLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        registerFeignClients(registry);
    }

    private void registerFeignClients(BeanDefinitionRegistry registry) {

        List<String> feignClients = new ArrayList<>(
                SpringFactoriesLoader.loadFactoryNames(getSpringFactoriesLoaderFactoryClass(), getBeanClassLoader()));

        // 支持 springboot 2.7 + 最新版本的配置方式
        ImportCandidates.load(FeignClient.class, getBeanClassLoader()).forEach(feignClients::add);
        // 如果 spring.factories 里为空
        if (feignClients.isEmpty()) {
            return;
        }
        for (String className : feignClients) {
            try {
                Class<?> clazz = beanClassLoader.loadClass(className);
                AnnotationAttributes attributes =
                        AnnotatedElementUtils.getMergedAnnotationAttributes(clazz, FeignClient.class);
                if (attributes == null) {
                    continue;
                }

                // 如果是单体项目自动注入 & url 为空
                Boolean isMicro = environment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, true);
                // 如果已经存在该 bean，支持原生的 Feign
                if (registry.containsBeanDefinition(className) && isMicro) {
                    continue;
                }

                registerClientConfiguration(registry, getClientName(attributes), attributes.get("configuration"));

                validate(attributes);
                BeanDefinitionBuilder definition =
                        BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class);
                definition.addPropertyValue("url", getUrl(registry, attributes));
                definition.addPropertyValue("path", getPath(attributes));
                String name = getName(attributes);
                definition.addPropertyValue("name", name);

                // 兼容最新版本的 spring-cloud-openfeign，尚未发布
                StringBuilder aliasBuilder = new StringBuilder(18);
                if (attributes.containsKey("contextId")) {
                    String contextId = getContextId(attributes);
                    aliasBuilder.append(contextId);
                    definition.addPropertyValue("contextId", contextId);
                } else {
                    aliasBuilder.append(name);
                }

                definition.addPropertyValue("type", className);
                definition.addPropertyValue("decode404", attributes.get("decode404"));
                definition.addPropertyValue("fallback", attributes.get("fallback"));
                definition.addPropertyValue("fallbackFactory", attributes.get("fallbackFactory"));
                definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

                AbstractBeanDefinition beanDefinition = definition.getBeanDefinition();

                // alias
                String alias = aliasBuilder.append("FeignClient").toString();

                // has a default, won't be null
                boolean primary = (Boolean) attributes.get("primary");

                beanDefinition.setPrimary(primary);

                String qualifier = getQualifier(attributes);
                if (StringUtils.hasText(qualifier)) {
                    alias = qualifier;
                }

                BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, className, new String[] {alias});
                BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
            } catch (ClassNotFoundException e) {
                log.error("ClassNotFound", e);
            }
        }
    }

    /**
     * Return the class used by {@link SpringFactoriesLoader} to load configuration
     * candidates.
     *
     * @return the factory class
     */
    private Class<?> getSpringFactoriesLoaderFactoryClass() {
        return FeignAutoConfiguration.class;
    }

    private void validate(Map<String, Object> attributes) {
        AnnotationAttributes annotation = AnnotationAttributes.fromMap(attributes);
        // This blows up if an aliased property is overspecified
        FeignClientsRegistrar.validateFallback(annotation.getClass("fallback"));
        FeignClientsRegistrar.validateFallbackFactory(annotation.getClass("fallbackFactory"));
    }

    private String getName(Map<String, Object> attributes) {
        String name = (String) attributes.get("serviceId");
        if (!StringUtils.hasText(name)) {
            name = (String) attributes.get("name");
        }
        if (!StringUtils.hasText(name)) {
            name = (String) attributes.get("value");
        }
        name = resolve(name);
        return FeignClientsRegistrar.getName(name);
    }

    private String getContextId(Map<String, Object> attributes) {
        String contextId = (String) attributes.get("contextId");
        if (!StringUtils.hasText(contextId)) {
            return getName(attributes);
        }

        contextId = resolve(contextId);
        return FeignClientsRegistrar.getName(contextId);
    }

    private String resolve(String value) {
        if (StringUtils.hasText(value)) {
            return this.environment.resolvePlaceholders(value);
        }
        return value;
    }

    private String getUrl(BeanDefinitionRegistry registry, Map<String, Object> attributes) {

        // 如果是单体项目自动注入 & url 为空
        Boolean isMicro = environment.getProperty("spring.cloud.nacos.discovery.enabled", Boolean.class, true);

        if (isMicro) {
            return null;
        }

        String url = resolve(BASE_URL);

        return FeignClientsRegistrar.getUrl(url);
    }

    private String getPath(Map<String, Object> attributes) {
        String path = resolve((String) attributes.get("path"));
        return FeignClientsRegistrar.getPath(path);
    }

    @Nullable
    private String getQualifier(@Nullable Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String qualifier = (String) client.get("qualifier");
        if (StringUtils.hasText(qualifier)) {
            return qualifier;
        }
        return null;
    }

    @Nullable
    private String getClientName(@Nullable Map<String, Object> client) {
        if (client == null) {
            return null;
        }
        String value = (String) client.get("contextId");
        if (!StringUtils.hasText(value)) {
            value = (String) client.get("value");
        }
        if (!StringUtils.hasText(value)) {
            value = (String) client.get("name");
        }
        if (!StringUtils.hasText(value)) {
            value = (String) client.get("serviceId");
        }
        if (StringUtils.hasText(value)) {
            return value;
        }

        throw new IllegalStateException(
                "Either 'name' or 'value' must be provided in @" + FeignClient.class.getSimpleName());
    }

    private void registerClientConfiguration(BeanDefinitionRegistry registry, Object name, Object configuration) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FeignClientSpecification.class);
        builder.addConstructorArgValue(name);
        builder.addConstructorArgValue(configuration);
        registry.registerBeanDefinition(
                name + "." + FeignClientSpecification.class.getSimpleName(), builder.getBeanDefinition());
    }
}
