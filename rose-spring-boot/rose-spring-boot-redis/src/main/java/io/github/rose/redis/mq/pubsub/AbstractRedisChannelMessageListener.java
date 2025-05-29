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
package io.github.rose.redis.mq.pubsub;

import io.github.rose.core.json.JsonUtils;
import io.github.rose.core.reflect.Types;
import io.github.rose.redis.mq.RedisMQTemplate;
import io.github.rose.redis.mq.interceptor.RedisMessageInterceptor;
import io.github.rose.redis.mq.message.AbstractRedisMessage;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Redis Pub/Sub 监听器抽象类，用于实现广播消费
 *
 * @param <T> 消息类型。一定要填写噢，不然会报错
 * @author EnjoyIot
 */
public abstract class AbstractRedisChannelMessageListener<T extends AbstractRedisChannelMessage>
        implements MessageListener {

    /**
     * 消息类型
     */
    private final Class<T> messageType;

    /**
     * Redis Channel
     */
    private final String channel;

    /**
     * RedisMQTemplate
     */
    private RedisMQTemplate redisMQTemplate;

    protected AbstractRedisChannelMessageListener() {
        this.messageType = getMessageClass();
        try {
            this.channel = messageType.getDeclaredConstructor().newInstance().getChannel();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Class<T> getMessageType() {
        return messageType;
    }

    public RedisMQTemplate getRedisMQTemplate() {
        return redisMQTemplate;
    }

    public void setRedisMQTemplate(RedisMQTemplate redisMQTemplate) {
        this.redisMQTemplate = redisMQTemplate;
    }

    /**
     * 获得 Sub 订阅的 Redis Channel 通道
     *
     * @return channel
     */
    public final String getChannel() {
        return channel;
    }

    @Override
    public final void onMessage(Message message, byte[] bytes) {
        T messageObj = JsonUtils.fromJson(message.getBody(), messageType);
        try {
            consumeMessageBefore(messageObj);
            // 消费消息
            this.onMessage(messageObj);
        } finally {
            consumeMessageAfter(messageObj);
        }
    }

    /**
     * 处理消息
     *
     * @param message 消息
     */
    public abstract void onMessage(T message);

    /**
     * 通过解析类上的泛型，获得消息类型
     *
     * @return 消息类型
     */
    @SuppressWarnings("unchecked")
    private Class<T> getMessageClass() {
        Type type = Types.getTypeArgument(getClass());
        if (type == null) {
            throw new IllegalStateException(String.format(
                    Locale.getDefault(), "类型(%s) 需要设置消息类型", getClass().getName()));
        }
        return (Class<T>) type;
    }

    private void consumeMessageBefore(AbstractRedisMessage message) {
        assert redisMQTemplate != null;
        List<RedisMessageInterceptor> interceptors = redisMQTemplate.getInterceptors();
        // 正序
        interceptors.forEach(interceptor -> interceptor.consumeMessageBefore(message));
    }

    private void consumeMessageAfter(AbstractRedisMessage message) {
        assert redisMQTemplate != null;
        List<RedisMessageInterceptor> interceptors = redisMQTemplate.getInterceptors();
        // 倒序
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            interceptors.get(i).consumeMessageAfter(message);
        }
    }
}
