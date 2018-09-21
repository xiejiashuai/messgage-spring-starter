package com.aihuishou.message.constants;

/**
 * @author js.xie
 */
public enum ConsumeMode {

    /**
     * receive asynchronously delivered messages concurrently
     */
    CONCURRENTLY,

    /**
     * receive asynchronously delivered messages orderly. one queue, one thread
     */
    ORDERLY
}
