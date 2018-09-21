package com.aihuishou.message.converter;


import com.aliyun.openservices.ons.api.Message;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author js.xie
 */
public interface MessageConverter {

    /**
     * convert ali yun message to consumer handler method argument type
     *
     * @param message     ali yun message
     * @param targetClass consumer handler method argument type
     * @return Object the method argument instance
     */
    default Object fromMessage(Message message, Class<?> targetClass) {
        throw new IllegalArgumentException("can not support convert");
    }

    /**
     * convert org.springframework.messaging.Message<?> to  ons Message
     *
     * @param destination formats: `topicName:tags`
     * @param message     spring messaging message {@link org.springframework.messaging.Message}
     * @return instance of {@link com.aliyun.openservices.ons.api.Message}
     */
    default Message toMessage(String destination, org.springframework.messaging.Message<?> message) {
        throw new IllegalArgumentException("can not support convert");
    }

    /**
     * convert payLoad to string
     *
     * @param payload
     * @return
     */
    default String toString(Object payload) throws JsonProcessingException {
        throw new IllegalArgumentException("can not support convert");
    }


}
