package com.example.redis_crud_sb3.RedisTemplate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    /*
     -----------------------------------------------------
     REDIS TEMPLATE CONFIGURATION
     -----------------------------------------------------

     Defines how keys and values are serialized.

     Recommended for storing JSON instead of Java binary
     serialization.
    */
    
    // Redis Keys (Simple Key-Value) working fine for person object commenting out service , controller etc.. will use  Redis Hashes
    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    //     RedisTemplate<String, Object> template = new RedisTemplate<>();
    //     template.setConnectionFactory(connectionFactory);
    //     template.setKeySerializer(new StringRedisSerializer());
    //     template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    //     return template;
    // }
 

// Redis Hashes 
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    // Redis connection as code config : 

     /*
     import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

     -----------------------------------------------------
     REDIS CONNECTION PROPERTIES
     -----------------------------------------------------

     These values can come from application.yml.

     Example:
     spring.data.redis.host=localhost
     spring.data.redis.port=6379
     spring.data.redis.username=default
     spring.data.redis.password=mypassword
     */

    // @Value("${spring.data.redis.host:localhost}")
    // private String host;

    // @Value("${spring.data.redis.port:6379}")
    // private int port;

    // @Value("${spring.data.redis.username:default}")
    // private String username;

    // @Value("${spring.data.redis.password:}")
    // private String password;

    // @Value("${spring.data.redis.database:0}")
    // private int database;



    /*
     -----------------------------------------------------
     REDIS CONNECTION FACTORY
     -----------------------------------------------------

     Creates Redis connection using Lettuce client.

     Only needed if custom configuration is required.
     */

    // @Bean
    // public RedisConnectionFactory redisConnectionFactory() {

    //     RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    //     config.setHostName(host);
    //     config.setPort(port);
    //     config.setDatabase(database);

    //     if (username != null) {
    //         config.setUsername(username);
    //     }

    //     if (password != null) {
    //         config.setPassword(RedisPassword.of(password));
    //     }

    //     LettuceClientConfiguration clientConfig =
    //             LettuceClientConfiguration.builder()

    //                     // Command timeout
    //                     .commandTimeout(Duration.ofSeconds(2))

    //                     // Shutdown timeout
    //                     .shutdownTimeout(Duration.ZERO)

    //                     // SSL support (optional)
    //                     // .useSsl()

    //                     // Start TLS (optional)
    //                     // .startTls()

    //                     .build();

    //     return new LettuceConnectionFactory(config, clientConfig);
    // }
    
}
