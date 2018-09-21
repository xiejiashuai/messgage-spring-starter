package com.aihuishou.message.error;

import com.aihuishou.message.properties.ConsumerProperties;
import com.aliyun.openservices.ons.api.Message;

/**
 * strategy for error message when large than
 *
 * @author js.xie
 * @see ConsumerProperties#consumeFailedNum
 */
public interface ErrorMessageHandlerStrategy {

    /**
     * handle error message
     *
     * @param message
     * @param throwable
     */
    default void handleErrorMessage(Message message, Throwable throwable) {

    }

}
