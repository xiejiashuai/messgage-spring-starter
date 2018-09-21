package com.aihuishou.message.annotation;

import java.lang.annotation.*;

/**
 * mark one method to consumer handler method
 * @author js.xie
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageListenerHandlerMethod {


}
