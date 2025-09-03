package com.truholdem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.websocket.ClusterSessionRegistry;
import com.truholdem.websocket.RedisGameEventBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
@ConditionalOnProperty(name = "app.websocket.cluster.enabled", havingValue = "true")
public class WebSocketClusterConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClusterConfig.class);

    public static final String GAME_EVENTS_CHANNEL = "truholdem:game:events";
    public static final String SESSION_EVENTS_CHANNEL = "truholdem:session:events";

    @Value("${app.websocket.cluster.instance-id:#{T(java.util.UUID).randomUUID().toString()}}")
    private String instanceId;

    
    @Bean(name = "webSocketRedisTemplate")
    public RedisTemplate<String, Object> webSocketRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));
        template.afterPropertiesSet();
        
        logger.info("WebSocket Redis template configured for instance: {}", instanceId);
        return template;
    }

    
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter gameEventListenerAdapter,
            MessageListenerAdapter sessionEventListenerAdapter) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        
        container.addMessageListener(gameEventListenerAdapter, new ChannelTopic(GAME_EVENTS_CHANNEL));
        
        
        container.addMessageListener(sessionEventListenerAdapter, new ChannelTopic(SESSION_EVENTS_CHANNEL));
        
        logger.info("Redis message listener container configured for channels: {}, {}", 
                GAME_EVENTS_CHANNEL, SESSION_EVENTS_CHANNEL);
        
        return container;
    }

    
    @Bean
    public MessageListenerAdapter gameEventListenerAdapter(RedisGameEventBroadcaster broadcaster) {
        return new MessageListenerAdapter(broadcaster, "handleRedisMessage");
    }

    
    @Bean
    public MessageListenerAdapter sessionEventListenerAdapter(ClusterSessionRegistry registry) {
        return new MessageListenerAdapter(registry, "handleSessionEvent");
    }

    
    @Bean
    public ChannelTopic gameEventsTopic() {
        return new ChannelTopic(GAME_EVENTS_CHANNEL);
    }

    
    @Bean
    public ChannelTopic sessionEventsTopic() {
        return new ChannelTopic(SESSION_EVENTS_CHANNEL);
    }

    
    @Bean
    public String clusterInstanceId() {
        logger.info("Cluster instance ID: {}", instanceId);
        return instanceId;
    }
}
