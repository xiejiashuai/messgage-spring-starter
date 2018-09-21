package com.aihuishou.message.consumer.container;

import com.aihuishou.message.annotation.ConsumerRetry;
import com.aihuishou.message.annotation.MessageListener;
import com.aihuishou.message.consumer.MqListener;
import com.aihuishou.message.properties.ConsumerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.aihuishou.message.constants.MqListenerContainerConstants.*;

/**
 * consumer registrar
 *
 * @author js.xie
 */
@Slf4j
public abstract class MqListenerContainerRegistrar {


    private static AtomicLong counter = new AtomicLong(0);

    public static void registryFromInterface(String beanName, Object bean, Class<?> originTargetClass, Environment environment, ConfigurableApplicationContext applicationContext) {

        MqListener mqListener = (MqListener) bean;
        MessageListener annotation = originTargetClass.getAnnotation(MessageListener.class);

        // handler consumer properties
        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setConsumeMode(annotation.consumeMode());
        consumerProperties.setTopic(environment.resolvePlaceholders(annotation.topic()));
        consumerProperties.setConsumerGroup(environment.resolvePlaceholders(annotation.consumerGroup()));
        consumerProperties.setConsumeThreadNum(annotation.consumeThreadNum());
        consumerProperties.setMessageModel(annotation.messageModel());
        consumerProperties.setSelectorExpress(environment.resolvePlaceholders(annotation.selectorExpress()));
        consumerProperties.setSelectorType(annotation.selectorType());
        consumerProperties.setConsumeFailedNum(annotation.consumeFailedNum());


        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(MqListenerContainer.class);
        beanBuilder.addPropertyValue(PROP_CONSUMER, consumerProperties);
        beanBuilder.addPropertyValue(PROP_MQ_LISTENER, mqListener);
        String containerBeanName = String.format("%s_%s", ClassUtils.getShortName(MqListenerContainer.class), counter.incrementAndGet());
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(containerBeanName, beanBuilder.getBeanDefinition());

        MqListenerContainer container = beanFactory.getBean(containerBeanName, MqListenerContainer.class);

        boolean enableRetry = annotation.enableRetry();

        // enable retry template
        if (enableRetry) {
            ConsumerRetry consumerRetry = annotation.retry();
            ConsumerProperties.Retry retry = new ConsumerProperties.Retry();
            retry.setMaxAttempts(consumerRetry.maxAttempts());
            retry.setBackOffInitialInterval(consumerRetry.backOffInitialInterval());
            retry.setBackOffMaxInterval(consumerRetry.backOffMaxInterval());
            retry.setBackOffMultiplier(consumerRetry.backOffMultiplier());
            consumerProperties.setRetry(retry);

            container.buildRetryTemplate(consumerProperties.getRetry());
        }


        startContainerIfNecessary(container);

        log.info("register rocketMQ listener to container, listenerBeanName:{}, containerBeanName:{}", beanName, containerBeanName);

    }


    public static void registryFromAnnotation(String beanName, Object bean, Class<?> originTargetClass, Environment environment, ConfigurableApplicationContext applicationContext, List<Method> listenerHandlerMethods) {

        MessageListener annotation = originTargetClass.getAnnotation(MessageListener.class);
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(MqListenerContainer.class);

        // handler consumer properties
        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setConsumeMode(annotation.consumeMode());
        consumerProperties.setTopic(environment.resolvePlaceholders(annotation.topic()));
        consumerProperties.setConsumerGroup(environment.resolvePlaceholders(annotation.consumerGroup()));
        consumerProperties.setConsumeThreadNum(annotation.consumeThreadNum());
        consumerProperties.setMessageModel(annotation.messageModel());
        consumerProperties.setSelectorExpress(environment.resolvePlaceholders(annotation.selectorExpress()));
        consumerProperties.setSelectorType(annotation.selectorType());
        consumerProperties.setConsumeFailedNum(annotation.consumeFailedNum());

        // todo bean 不能是代理对象
        beanBuilder.addPropertyValue(PROP_CONSUME_INSTANCE, bean);
        beanBuilder.addPropertyValue(PROP_CONSUMER, consumerProperties);

        Method handlerMethod = listenerHandlerMethods.get(0);
        beanBuilder.addPropertyValue(PROP_CONSUME_HANDLER_METHOD, handlerMethod);

        // todo
        beanBuilder.addPropertyValue(PROP_CONSUMER_METHOD_PARAMETER_TYPE, handlerMethod.getParameterTypes()[0]);

        String containerBeanName = String.format("%s_%s", ClassUtils.getShortName(MqListenerContainer.class), counter.incrementAndGet());
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(containerBeanName, beanBuilder.getBeanDefinition());

        MqListenerContainer container = beanFactory.getBean(containerBeanName, MqListenerContainer.class);

        boolean enableRetry = annotation.enableRetry();
        // enable retry template
        if (enableRetry) {
            ConsumerRetry consumerRetry = annotation.retry();
            ConsumerProperties.Retry retry = new ConsumerProperties.Retry();
            retry.setMaxAttempts(consumerRetry.maxAttempts());
            retry.setBackOffInitialInterval(consumerRetry.backOffInitialInterval());
            retry.setBackOffMaxInterval(consumerRetry.backOffMaxInterval());
            retry.setBackOffMultiplier(consumerRetry.backOffMultiplier());
            consumerProperties.setRetry(retry);
            container.buildRetryTemplate(consumerProperties.getRetry());
        }

        startContainerIfNecessary(container);

        log.info("register rocketMQ listener to container, listenerBeanName:{}, containerBeanName:{}", beanName, containerBeanName);
    }

    private static void startContainerIfNecessary(MqListenerContainer container) {

        if (!container.isStarted()) {
            try {
                container.start();
            } catch (Exception e) {
                log.error("started container failed. {}", container, e);
                throw new RuntimeException(e);
            }
        }

    }

}
