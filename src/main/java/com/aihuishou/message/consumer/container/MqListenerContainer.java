package com.aihuishou.message.consumer.container;

import com.aihuishou.message.annotation.MessageListenerHandlerMethod;
import com.aihuishou.message.annotation.MessageRetryTemplate;
import com.aihuishou.message.constants.ConsumeMode;
import com.aihuishou.message.constants.MqListenerContainerConstants;
import com.aihuishou.message.consumer.MqListener;
import com.aihuishou.message.converter.MessageConverter;
import com.aihuishou.message.error.ErrorMessageHandlerStrategy;
import com.aihuishou.message.properties.ConsumerProperties;
import com.aihuishou.message.properties.ServerProperties;
import com.aihuishou.message.util.ClassUtils;
import com.aliyun.openservices.ons.api.*;
import com.aliyun.openservices.ons.api.order.ConsumeOrderContext;
import com.aliyun.openservices.ons.api.order.MessageOrderListener;
import com.aliyun.openservices.ons.api.order.OrderAction;
import com.aliyun.openservices.ons.api.order.OrderConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * consumer instance
 *
 * @author js.xie
 */
@Slf4j
public class MqListenerContainer {

    /**
     * broker properties eg name server address and so on
     * globally unique
     */
    @Autowired
    private ServerProperties serverProperties;

    /**
     * message converter
     */
    @Autowired
    private MessageConverter messageConverter;

    /**
     * consumer properties
     * each consumer has one different consumer properties
     *
     * @see MqListenerContainerConstants#PROP_CONSUMER
     */
    @Getter
    @Setter
    private ConsumerProperties consumerProperties;

    /**
     * handler when not use {@link MessageListenerHandlerMethod} annotation but implement MQListener
     *
     * @see MqListenerContainerConstants#PROP_MQ_LISTENER
     */
    @Setter
    @Getter
    private MqListener mqListener;

    /**
     * consumer method argument type
     *
     * @see MqListenerContainerConstants#PROP_CONSUMER_METHOD_PARAMETER_TYPE
     */
    @Setter
    @Getter
    private Class messageType;

    /**
     * consumer handler method when use {@link MessageListenerHandlerMethod} annotation
     *
     * @see MqListenerContainerConstants#PROP_CONSUME_HANDLER_METHOD
     */
    @Setter
    @Getter
    private Method handlerMethod;

    /**
     * consumer instance when use {@link MessageListenerHandlerMethod} annotation
     *
     * @see MqListenerContainerConstants#PROP_CONSUME_INSTANCE
     */
    @Setter
    @Getter
    private Object instance;

    /**
     * one flag , consumer is start
     */
    @Setter
    @Getter
    private volatile boolean started;

    /**
     * unordered consumer
     */
    private Consumer consumer;

    /**
     * ordered consumer
     */
    private OrderConsumer orderConsumer;

    @Autowired(required = false)
    @MessageRetryTemplate
    private RetryTemplate retryTemplate;

    /**
     * error message handlers
     */
    @Autowired(required = false)
    private List<ErrorMessageHandlerStrategy> errorMessageHandlerStrategies;

    public void start() {

        if (!this.isStarted()) {

            initMQConsumer();

            // parse message type
            if (null != mqListener) {
                this.messageType = ClassUtils.getMethodParameterType(mqListener.getClass());
            }

            if (consumer != null) {
                consumer.start();
            }

            if (orderConsumer != null) {
                orderConsumer.start();
            }


            this.setStarted(true);

            log.info("started container: {}", this.toString());
        }

    }


    @PreDestroy
    public void stop() {

        this.setStarted(false);

        if (Objects.nonNull(consumer)) {
            consumer.shutdown();
        }

        if (Objects.nonNull(orderConsumer)) {
            orderConsumer.shutdown();
        }

        log.info("container destroyed, {}", this.toString());
    }


    private void initMQConsumer() {

        Assert.notNull(consumerProperties.getConsumerGroup(), "Property 'consumerGroup' is required");
        Assert.notNull(serverProperties.getAccessKey(), "Property 'accessKey' is required");
        Assert.notNull(serverProperties.getSecretKey(), "Property 'secretKey' is required");
        Assert.notNull(consumerProperties.getTopic(), "Property 'topic' is required");

        Properties properties = new Properties();
        properties.put(PropertyKeyConst.AccessKey, serverProperties.getAccessKey());
        properties.put(PropertyKeyConst.SecretKey, serverProperties.getSecretKey());
        properties.put(PropertyKeyConst.ConsumerId, consumerProperties.getConsumerGroup());

        if (consumerProperties.getConsumeThreadNum() != null) {
            properties.put(PropertyKeyConst.ConsumeThreadNums, consumerProperties.getConsumeThreadNum());
        }

        if (StringUtils.hasText(serverProperties.getNameServer())) {
            properties.put(PropertyKeyConst.NAMESRV_ADDR, serverProperties.getNameServer());
        }

        if (null != consumerProperties.getMessageModel()) {
            properties.put(PropertyKeyConst.MessageModel, consumerProperties.getMessageModel().getModeCN());
        }

        // ordered consumer will be initialized
        if (consumerProperties.getConsumeMode().equals(ConsumeMode.ORDERLY)) {

            this.orderConsumer = ONSFactory.createOrderedConsumer(properties);

            switch (consumerProperties.getSelectorType()) {
                case TAG:
                    orderConsumer.subscribe(consumerProperties.getTopic(), consumerProperties.getSelectorExpress(), new DefaultMessageListenerOrderly());
                    break;
                case SQL92:
                    orderConsumer.subscribe(consumerProperties.getTopic(), MessageSelector.bySql(consumerProperties.getSelectorExpress()), new DefaultMessageListenerOrderly());
                    break;
                default:
                    throw new IllegalArgumentException("Property 'selectorType' was wrong.");
            }

        }

        // concurrent consumer will be initialized
        if (consumerProperties.getConsumeMode().equals(ConsumeMode.CONCURRENTLY)) {

            this.consumer = ONSFactory.createConsumer(properties);

            switch (consumerProperties.getSelectorType()) {
                case TAG:
                    consumer.subscribe(consumerProperties.getTopic(), consumerProperties.getSelectorExpress(), new DefaultMessageListener());
                    break;
                case SQL92:
                    consumer.subscribe(consumerProperties.getTopic(), MessageSelector.bySql(consumerProperties.getSelectorExpress()), new DefaultMessageListener());
                    break;
                default:
                    throw new IllegalArgumentException("Property 'selectorType' was wrong.");
            }

        }

    }

    /**
     * unordered message listener implemention
     */
    public class DefaultMessageListener implements MessageListener {

        @Override
        public Action consume(final Message message, final ConsumeContext context) {

            try {

                new MessageListenerMethodHandler().doConsume(message);

            } catch (Exception e) {

                log.error("consume message failed. message:{},exception:{}", message, e.getMessage(), e);

                handleErrorMessage(message, e);

                return Action.ReconsumeLater;

            }

            return Action.CommitMessage;
        }
    }

    /**
     * ordered message listener implemention
     */
    public class DefaultMessageListenerOrderly implements MessageOrderListener {

        @Override
        public OrderAction consume(final Message message, final ConsumeOrderContext context) {

            try {

                new MessageListenerMethodHandler().doConsume(message);

            } catch (Exception e) {

                log.error("consume message failed. message:{},exception:{}", message, e.getMessage(), e);

                handleErrorMessage(message, e);

                return OrderAction.Suspend;
            }

            return OrderAction.Success;
        }
    }

    public class MessageListenerMethodHandler {

        public void doConsume(final Message message) throws InvocationTargetException, IllegalAccessException {

            Object obj = messageConverter.fromMessage(message, messageType);

            if (disableRetry()) {

                if (mqListener != null) {
                    mqListener.onMessage(obj);
                }

                if (null != handlerMethod) {
                    handlerMethod.invoke(instance, obj);
                }

            } else {

                retryTemplate.execute((RetryCallback) context -> {

                    if (mqListener != null) {
                        mqListener.onMessage(obj);
                    }

                    if (null != handlerMethod) {
                        handlerMethod.invoke(instance, obj);
                    }

                    return null;
                });

            }


        }

    }

    public void buildRetryTemplate(ConsumerProperties.Retry retry) {

        RetryTemplate rt = this.retryTemplate;
        if (rt == null && retry != null) {
            rt = new RetryTemplate();

            SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
            retryPolicy.setMaxAttempts(retry.getMaxAttempts());
            rt.setRetryPolicy(retryPolicy);

            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setInitialInterval(retry.getBackOffInitialInterval());
            backOffPolicy.setMultiplier(retry.getBackOffMultiplier());
            backOffPolicy.setMaxInterval(retry.getBackOffMaxInterval());
            rt.setBackOffPolicy(backOffPolicy);

            retryTemplate = rt;

        }

        // use default
        if (null == rt && null == retry) {
            rt = new RetryTemplate();

            SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
            simpleRetryPolicy.setMaxAttempts(3);
            rt.setRetryPolicy(simpleRetryPolicy);

            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setInitialInterval(100);
            backOffPolicy.setMultiplier(2);
            backOffPolicy.setMaxInterval(1000);
            rt.setBackOffPolicy(backOffPolicy);

            retryTemplate = rt;
        }

    }

    private void handleErrorMessage(Message message, Throwable t) {

        if (message.getReconsumeTimes() >= consumerProperties.getConsumeFailedNum()) {

            CompletableFuture.supplyAsync(() -> {

                for (ErrorMessageHandlerStrategy errorMessageHandlerStrategy : errorMessageHandlerStrategies) {
                    errorMessageHandlerStrategy.handleErrorMessage(message, t);
                }

                return null;

            });

        }
    }

    /**
     * whether disable additional retry
     *
     * @return
     */
    private boolean disableRetry() {
        return null == retryTemplate && !consumerProperties.getEnableRetry();
    }

}
