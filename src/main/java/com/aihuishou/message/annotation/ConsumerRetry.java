package com.aihuishou.message.annotation;


import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * consumer retry configuration
 * <note>
 *     retry do not rely on message system
 * </note>
 * @author js.xie
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ConsumerRetry {


    int maxAttempts() default 3;

    /**
     * The backoff initial interval on retry. This is a  RetryTemplate configuration
     * which is provided by the framework.
     * Default: 1000 ms.
     * You can also provide custom RetryTemplate
     * in the event you want to take complete control of the RetryTemplate. Simply configure
     * it as @Bean inside your application configuration.
     */
     int backOffInitialInterval() default  1000;

    /**
     * The maximum backoff interval. This is a  RetryTemplate configuration
     * which is provided by the framework.
     * Default: 10000 ms.
     * You can also provide custom RetryTemplate
     * in the event you want to take complete control of the RetryTemplate. Simply configure
     * it as @Bean inside your application configuration.
     */
     int backOffMaxInterval() default 10000;

    /**
     * The backoff multiplier.This is a  RetryTemplate configuration
     * which is provided by the framework.
     * Default: 2.0.
     * You can also provide custom RetryTemplate
     * in the event you want to take complete control of the RetryTemplate. Simply configure
     * it as @Bean inside your application configuration.
     */
     double backOffMultiplier() default  2.0;


}
