package com.aihuishou.message.properties;

import com.aihuishou.message.constants.ConsumeMode;
import com.aihuishou.message.constants.SelectorType;
import com.aihuishou.message.error.ErrorMessageHandlerStrategy;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import lombok.Data;

/**
 * @author js.xie
 */

@Data
public class ConsumerProperties {

    /**
     * ons cid
     */
    private String consumerGroup;

    /**
     * ons topic
     */
    private String topic;

    /**
     * when consumer failed , will retry. if the retry times is more than max retry times will do something eg send notification
     */
    private String dingTalkServerUrl;

    /**
     * ordered consumer or concurrently consumer
     */
    private ConsumeMode consumeMode = ConsumeMode.CONCURRENTLY;

    /**
     * message filter type tag or sql92
     */
    private SelectorType selectorType = SelectorType.TAG;

    /**
     * when selectorType is tag , selectorExpress is tag value , eg  tag1 || tag2 || tag3
     */
    private String selectorExpress = "*";

    /**
     * sub type
     */
    private MessageModel messageModel = MessageModel.CLUSTERING;

    /**
     * consumer consume thread num
     */
    private Integer consumeThreadNum;

    /**
     * when consume failed times more than consumeFailedNum, will invoke error handler
     *
     * @see ErrorMessageHandlerStrategy
     */
    private Integer consumeFailedNum;

    /**
     * whether enable additional retry which do not rely on message system
     */
    private Boolean enableRetry = false;

    /**
     * retry do not rely on message system
     */
    private Retry retry;

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
        int backOffInitialInterval = 100;

        /**
         * The maximum backoff interval. This is a  RetryTemplate configuration
         * which is provided by the framework.
         * Default: 10000 ms.
         * You can also provide custom RetryTemplate
         * in the event you want to take complete control of the RetryTemplate. Simply configure
         * it as @Bean inside your application configuration.
         */
        int backOffMaxInterval = 1000;

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

