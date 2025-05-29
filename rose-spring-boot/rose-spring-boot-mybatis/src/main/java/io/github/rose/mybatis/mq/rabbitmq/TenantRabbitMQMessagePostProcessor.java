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
package io.github.rose.mybatis.mq.rabbitmq;

import static io.github.rose.core.util.Constants.HEADER_TENANT_ID;

import io.github.rose.mybatis.tenant.util.TenantContextHolder;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * RabbitMQ 消息队列的多租户 {@link ProducerInterceptor} 实现类
 * <p>
 * 1. Producer 发送消息时，将 {@link TenantContextHolder} 租户编号，添加到消息的 Header 中 2. Consumer
 * 消费消息时，将消息的 Header 的租户编号，添加到 {@link TenantContextHolder} 中，通过
 * {@link InvocableHandlerMethod} 实现
 */
public class TenantRabbitMQMessagePostProcessor implements MessagePostProcessor {

    @Override
    public Message postProcessMessage(Message message) throws AmqpException {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            message.getMessageProperties().getHeaders().put(HEADER_TENANT_ID, tenantId);
        }
        return message;
    }
}
