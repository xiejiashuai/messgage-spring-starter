package com.aihuishou.message.producer;

import com.aihuishou.message.converter.MessageConverter;
import com.aihuishou.message.properties.ProducerProperties;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.SendCallback;
import com.aliyun.openservices.ons.api.SendResult;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.MessageConst;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * producer template send message
 *
 * @author js.xie
 */
@Slf4j
public class MqTemplate extends AbstractMessageSendingTemplate<String> implements InitializingBean, DisposableBean {

    /**
     * delegate producer send message
     */
    @Getter
    @Setter
    private Producer producer;

    /**
     * convert ons message to spring message and so on
     */
    @Setter
    private MessageConverter messageConverter;

    /**
     * additional retry
     */
    @Nullable
    private RetryTemplate producerRetryTemplate;

    /**
     * <p> Send message in synchronous mode. This method returns only when the sending procedure totally completes.
     * Reliable synchronous transmission is used in extensive scenes, such as important notification messages, SMS
     * notification, SMS marketing system, etc.. </p>
     * <p>
     *
     * @param destination formats: `topicName:tags`
     * @param message     {@link org.springframework.messaging.Message}
     * @return {@link SendResult}
     */
    public SendResult syncSend(String destination, Message<?> message) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.info("syncSend failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {

            com.aliyun.openservices.ons.api.Message onsMsg = messageConverter.toMessage(destination, message);

            SendResult sendResult;

            // need additional retry
            if (null != producerRetryTemplate) {

                sendResult = producerRetryTemplate.execute((RetryCallback<SendResult, Throwable>) context -> producer.send(onsMsg));

            } else {
                sendResult = producer.send(onsMsg);
            }

            log.info("sync send message msgId:{}", sendResult.getMessageId());

            return sendResult;

        } catch (Throwable throwable) {
            log.info("syncSend failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(throwable.getMessage(), throwable);

        }

    }


    /**
     * Same to {@link #syncSend(String, Object)}
     *
     * @param destination formats: `topicName:tags`
     * @param key         message key
     * @param payload     the Object to use as payload
     * @return {@link SendResult}
     */
    public SendResult syncSend(String destination, String key, Object payload) {
        Map<String, Object> headers = new HashMap<>();
        if (StringUtils.hasText(key)) {
            headers.put(MessageConst.PROPERTY_KEYS, key);
        }
        Message<?> message = this.doConvert(payload, headers, null);
        return syncSend(destination, message);
    }

    /**
     * Same to {@link #syncSend(String, Object)}
     *
     * @param destination formats: `topicName:tags`
     * @param payload     the Object to use as payload
     * @return {@link SendResult}
     */
    public SendResult syncSend(String destination, Object payload) {
        return syncSend(destination, null, payload);
    }


    /**
     * <p> Send message to broker asynchronously. asynchronous transmission is generally used in response time sensitive
     * business scenarios. </p>
     * <p>
     * This method returns immediately. On sending completion, <code>sendCallback</code> will be executed.
     * <p>
     * Similar to {@link #syncSend(String, Object)}, internal implementation would potentially retry up to {@link
     * DefaultMQProducer#getRetryTimesWhenSendAsyncFailed} times before claiming sending failure, which may yield
     * message duplication and application developers are the one to resolve this potential issue.
     *
     * @param destination  formats: `topicName:tags`
     * @param message      {@link org.springframework.messaging.Message}
     * @param sendCallback {@link SendCallback}
     */
    public void asyncSend(String destination, Message<?> message, SendCallback sendCallback) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.info("asyncSend failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {

            com.aliyun.openservices.ons.api.Message onsMsg = messageConverter.toMessage(destination, message);


            // need additional retry
            if (null != producerRetryTemplate) {

                producerRetryTemplate.execute((RetryCallback<Object, Throwable>) context -> {
                    producer.sendAsync(onsMsg, sendCallback);
                    return null;
                });

            } else {

                producer.send(onsMsg);

            }

        } catch (Throwable throwable) {
            log.info("asyncSend failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(throwable.getMessage(), throwable);
        }
    }


    /**
     * Same to {@link #asyncSend(String, Object, SendCallback)} with send timeout specified in addition.
     *
     * @param destination  formats: `topicName:tags`
     * @param payload      the Object to use as payload
     * @param sendCallback {@link SendCallback}
     */
    public void asyncSend(String destination, Object payload, SendCallback sendCallback) {
        asyncSend(destination, null, payload, sendCallback);
    }

    /**
     * Same to {@link #asyncSend(String, Object, SendCallback)} with send timeout specified in addition.
     *
     * @param destination  formats: `topicName:tags`
     * @param payload      the Object to use as payload
     * @param sendCallback {@link SendCallback}
     */
    public void asyncSend(String destination, String key, Object payload, SendCallback sendCallback) {
        Map<String, Object> headers = new HashMap<>();
        if (StringUtils.hasText(key)) {
            headers.put(MessageConst.PROPERTY_KEYS, key);
        }
        Message<?> message = this.doConvert(payload, headers, null);
        asyncSend(destination, message, sendCallback);
    }


    /**
     * Similar to <a href="https://en.wikipedia.org/wiki/User_Datagram_Protocol">UDP</a>, this method won't wait for
     * acknowledgement from broker before return. Obviously, it has maximums throughput yet potentials of message loss.
     * <p>
     * One-way transmission is used for cases requiring moderate reliability, such as log collection.
     *
     * @param destination formats: `topicName:tags`
     * @param message     {@link org.springframework.messaging.Message}
     */
    public void sendOneWay(String destination, Message<?> message) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.info("sendOneWay failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {

            com.aliyun.openservices.ons.api.Message onsMsg = messageConverter.toMessage(destination, message);

            // need additional retry
            if (null != producerRetryTemplate) {

                producerRetryTemplate.execute((RetryCallback<Object, Throwable>) context -> {
                    producer.sendOneway(onsMsg);
                    return null;
                });

            } else {

                producer.send(onsMsg);

            }
            producer.sendOneway(onsMsg);
        } catch (Throwable throwable) {
            log.info("sendOneWay failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(throwable.getMessage(), throwable);
        }
    }

    /**
     * Same to {@link #sendOneWay(String, Message)}
     *
     * @param destination formats: `topicName:tags`
     * @param payload     the Object to use as payload
     */
    public void sendOneWay(String destination, Object payload) {
        sendOneWay(destination, null, payload);
    }

    /**
     * Same to {@link #sendOneWay(String, Message)}
     *
     * @param destination formats: `topicName:tags`
     * @param key         message key
     * @param payload     the Object to use as payload
     */
    public void sendOneWay(String destination, String key, Object payload) {

        Map<String, Object> headers = new HashMap<>();
        if (StringUtils.hasText(key)) {
            headers.put(MessageConst.PROPERTY_KEYS, key);
        }

        Message<?> message = this.doConvert(payload, headers, null);
        sendOneWay(destination, message);

    }


    @Override
    protected void doSend(String destination, Message<?> message) {
        SendResult sendResult = syncSend(destination, message);
        log.debug("send message to `{}` finished. result:{}", destination, sendResult);
    }


    @Override
    protected Message<?> doConvert(Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) {
        String content;
        if (payload instanceof String) {
            content = (String) payload;
        } else {
            // if payload not as string, use objectMapper change it.
            try {
                content = messageConverter.toString(payload);
            } catch (JsonProcessingException e) {
                log.info("convert payload to String failed. payload:{}", payload);
                throw new RuntimeException("convert to payload to String failed.", e);
            }
        }

        MessageBuilder<?> builder = MessageBuilder.withPayload(content);
        if (headers != null) {
            builder.copyHeaders(headers);
        }
        builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);

        Message<?> message = builder.build();
        if (postProcessor != null) {
            message = postProcessor.postProcessMessage(message);
        }
        return message;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        Assert.isTrue(producer != null, "Property 'producer' or 'orderProducer' is required");

        if (null != producer) {
            producer.start();
        }


    }

    @Override
    public void destroy() {

        if (Objects.nonNull(producer)) {
            producer.shutdown();
        }

    }

    @SuppressWarnings("all")
    public void buildRetryTemplate(ProducerProperties.Retry retry) {

        RetryTemplate rt = this.producerRetryTemplate;
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

            producerRetryTemplate = rt;

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

            producerRetryTemplate = rt;
        }

    }
}
