package com.aihuishou.message.annotation;


import com.aihuishou.message.constants.ConsumeMode;
import com.aihuishou.message.constants.SelectorType;
import com.aliyun.openservices.ons.api.ExpressionType;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author js.xie
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MessageListener {

    /**
     * Consumers of the same role is required to have exactly same subscriptions and consumerGroup to correctly achieve
     * load balance. It's required and needs to be globally unique.
     * </p>
     *  Cid
     * <p>
     */
    String consumerGroup();

    /**
     * Topic name
     */
    String topic();

    /**
     * Control how to selector message
     *
     * @see ExpressionType
     */
    SelectorType selectorType() default SelectorType.TAG;

    /**
     * Control which message can be select. Grammar please see {@link ExpressionType#TAG} and {@link ExpressionType#SQL92}
     */
    String selectorExpress() default "*";

    /**
     * Control consume mode, you can choice receive message concurrently or orderly
     */
    ConsumeMode consumeMode() default ConsumeMode.CONCURRENTLY;

    /**
     * Control message mode, if you want all subscribers receive message all message, broadcasting is a good choice.
     */
    MessageModel messageModel() default MessageModel.CLUSTERING;

    /**
     * consumer thread number
     */
    int consumeThreadNum() default 1;

    /**
     * when consume faild times more than consumeFailedNum, will invoke error handler
     */
    int consumeFailedNum() default 12;

    /**
     * whether enable additional retry which do not rely on message system
     */
     boolean enableRetry() default false;

    /**
     * additional retry configuration
     * <nott>
     *     when enableRetry is true ,effective
     * </nott>
     */
    ConsumerRetry retry() default @ConsumerRetry;


}
