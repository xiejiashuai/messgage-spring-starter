package com.aihuishou.message.consumer;

/**
 * consumer contract
 *
 * @param <T> message type
 * @author js.xie
 */
public interface MqListener<T> {

    /**
     * listener external message system,then handler message
     *
     * @param message message
     */
    void onMessage(T message);
}
