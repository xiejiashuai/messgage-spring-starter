package com.aihuishou.message.annotation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

import java.lang.annotation.*;

/**
 * Marker to tag an instance of {@link RetryTemplate} to be used by the producer or consumer.
 * @author js.xie
 */

@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Bean
@Qualifier
public @interface MessageRetryTemplate {

}
