package com.aihuishou.message.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * producer properties
 *
 * @author js.xie
 */
@ConfigurationProperties(prefix = "spring.message.ons.producer")
@Data
public class ProducerProperties {

    /**
     * producer group eg ons pid
     */
    private String producerGroup;

    /**
     * whether enable additional retry which do not rely on message system
     */
    private Boolean enableRetry = true;

    /**
     * retry do not rely on message system
     */
    @NestedConfigurationProperty
    private ProducerProperties.Retry retry;

    @Data
    public static class Retry {

        int maxAttempts = 3;

        /**
         * The backoff initial interval on retry. This is a  RetryTemplate configuration
         * which is provided by the framework.
         * Default: 1000 ms.
         * You can also provide custom RetryTemplate
         * in the event you want to take complete control of the RetryTemplate. Simply configure
         * it as @Bean inside your application configuration.
         */
        int backOffInitialInterval = 500;

        /**
         * The maximum backoff interval. This is a  RetryTemplate configuration
         * which is provided by the framework.
         * Default: 10000 ms.
         * You can also provide custom RetryTemplate
         * in the event you want to take complete control of the RetryTemplate. Simply configure
         * it as @Bean inside your application configuration.
         */
        int backOffMaxInterval = 10000;

        /**
         * The backoff multiplier.This is a  RetryTemplate configuration
         * which is provided by the framework.
         * Default: 2.0.
         * You can also provide custom RetryTemplate
         * in the event you want to take complete control of the RetryTemplate. Simply configure
         * it as @Bean inside your application configuration.
         */
        double backOffMultiplier = 2.0;
    }

}
