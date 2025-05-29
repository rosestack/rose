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
package io.github.rose.redis.mq.config;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;

import io.github.rose.core.util.NetUtils;
import io.github.rose.core.util.text.TextFormatUtils;
import io.github.rose.redis.config.RedisCacheConfig;
import io.github.rose.redis.mq.RedisMQTemplate;
import io.github.rose.redis.mq.job.RedisPendingMessageResendJob;
import io.github.rose.redis.mq.pubsub.AbstractRedisChannelMessageListener;
import io.github.rose.redis.mq.stream.AbstractRedisStreamMessageListener;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Redis 消息队列 Consumer 配置类
 *
 * @author EnjoyIot
 */
@EnableScheduling // 启用定时任务，用于 RedisPendingMessageResendJob 重发消息
@AutoConfiguration(after = RedisCacheConfig.class)
public class EnjoyRedisMQConsumerAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(EnjoyRedisMQConsumerAutoConfiguration.class);

    /**
     * 构建消费者名字，使用本地 IP + 进程编号的方式。 参考自 RocketMQ clientId 的实现
     *
     * @return 消费者名字
     */
    private static String buildConsumerName() {
        return String.format(
                Locale.getDefault(),
                "%s@%d",
                NetUtils.getLocalAddress(),
                Long.parseLong(getRuntimeMXBean().getName().split("@")[0]));
    }

    /**
     * 校验 Redis 版本号，是否满足最低的版本号要求！
     */
    private static void checkRedisVersion(RedisTemplate<String, ?> redisTemplate) {
        // 获得 Redis 版本
        Properties info = redisTemplate.execute((RedisCallback<Properties>) RedisServerCommands::info);
        assert info != null;
        String version = info.getProperty("redis_version");
        // 校验最低版本必须大于等于 5.0.0
        int majorVersion = Integer.parseInt(StringUtils.substringBefore(version, '.'));
        if (majorVersion < 5) {
            throw new IllegalStateException(TextFormatUtils.format("您当前的 Redis 版本为 {}，小于最低要求的 5.0.0 版本！", version));
        }
    }

    /**
     * 创建 Redis Pub/Sub 广播消费的容器
     */
    @Bean
    @ConditionalOnBean(AbstractRedisChannelMessageListener.class)
    // 只有 AbstractChannelMessageListener 存在的时候，才需要注册 Redis pubsub 监听
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisMQTemplate redisMQTemplate, List<AbstractRedisChannelMessageListener<?>> listeners) {
        // 创建 RedisMessageListenerContainer 对象
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        // 设置 RedisConnection 工厂。
        container.setConnectionFactory(redisMQTemplate.getRedisTemplate().getRequiredConnectionFactory());
        // 添加监听器
        listeners.forEach(listener -> {
            listener.setRedisMQTemplate(redisMQTemplate);
            container.addMessageListener(listener, new ChannelTopic(listener.getChannel()));
            log.info(
                    "[redisMessageListenerContainer][注册 Channel({}) 对应的监听器({})]",
                    listener.getChannel(),
                    listener.getClass().getName());
        });
        return container;
    }

    /**
     * 创建 Redis Stream 重新消费的任务
     */
    @Bean
    @ConditionalOnBean(AbstractRedisStreamMessageListener.class)
    // 只有 AbstractStreamMessageListener 存在的时候，才需要注册 Redis pubsub 监听
    public RedisPendingMessageResendJob redisPendingMessageResendJob(
            List<AbstractRedisStreamMessageListener<?>> listeners,
            RedisMQTemplate redisTemplate,
            @Value("${spring.application.name}") String groupName,
            RedissonClient redissonClient) {
        return new RedisPendingMessageResendJob(listeners, redisTemplate, groupName, redissonClient);
    }

    /**
     * 创建 Redis Stream 集群消费的容器
     * <p>
     * 基础知识：<a href=
     * "https://www.geek-book.com/src/docs/redis/redis/redis.io/commands/xreadgroup.html">Redis
     * Stream 的 xreadgroup 命令</a>
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnBean(AbstractRedisStreamMessageListener.class)
    // 只有 AbstractStreamMessageListener 存在的时候，才需要注册 Redis pubsub 监听
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> redisStreamMessageListenerContainer(
            RedisMQTemplate redisMQTemplate, List<AbstractRedisStreamMessageListener<?>> listeners) {
        RedisTemplate<String, ?> redisTemplate = redisMQTemplate.getRedisTemplate();
        checkRedisVersion(redisTemplate);
        // 第一步，创建 StreamMessageListenerContainer 容器
        // 创建 options 配置
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>>
                containerOptions = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(10) // 一次性最多拉取多少条消息
                        .targetType(String.class) // 目标类型。统一使用 String，通过自己封装的
                        // AbstractStreamMessageListener 去反序列化
                        .build();
        // 创建 container 对象
        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container =
                StreamMessageListenerContainer.create(
                        redisMQTemplate.getRedisTemplate().getRequiredConnectionFactory(), containerOptions);

        // 第二步，注册监听器，消费对应的 Stream 主题
        String consumerName = buildConsumerName();
        listeners.parallelStream().forEach(listener -> {
            log.info(
                    "[redisStreamMessageListenerContainer][开始注册 StreamKey({}) 对应的监听器({})]",
                    listener.getStreamKey(),
                    listener.getClass().getName());
            // 创建 listener 对应的消费者分组
            try {
                redisTemplate.opsForStream().createGroup(listener.getStreamKey(), listener.getGroup());
            } catch (Exception ignore) {
            }
            // 设置 listener 对应的 redisTemplate
            listener.setRedisMQTemplate(redisMQTemplate);
            // 创建 Consumer 对象
            Consumer consumer = Consumer.from(listener.getGroup(), consumerName);
            // 设置 Consumer 消费进度，以最小消费进度为准
            StreamOffset<String> streamOffset = StreamOffset.create(listener.getStreamKey(), ReadOffset.lastConsumed());
            // 设置 Consumer 监听
            StreamMessageListenerContainer.StreamReadRequestBuilder<String> builder =
                    StreamMessageListenerContainer.StreamReadRequest.builder(streamOffset)
                            .consumer(consumer)
                            .autoAcknowledge(false) // 不自动 ack
                            .cancelOnError(throwable -> false); // 默认配置，发生异常就取消消费，显然不符合预期；因此，我们设置为
            // false
            container.register(builder.build(), listener);
            log.info(
                    "[redisStreamMessageListenerContainer][完成注册 StreamKey({}) 对应的监听器({})]",
                    listener.getStreamKey(),
                    listener.getClass().getName());
        });
        return container;
    }
}
