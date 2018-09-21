package com.aihuishou.message.annotation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import java.lang.annotation.*;

/**
 * @author  js.xie
 */

@Target(value = {ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Bean
@Qualifier
public @interface MessageObjectMapper {

}
