package com.aihuishou.message.configuration;

import com.aihuishou.message.annotation.MessageListener;
import com.aihuishou.message.annotation.MessageListenerHandlerMethod;
import com.aihuishou.message.consumer.MqListener;
import com.aihuishou.message.consumer.container.MqListenerContainerRegistrar;
import com.aihuishou.message.properties.ServerProperties;
import com.aliyun.openservices.ons.api.ONSFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * consumer auto assembly , detect beans with {@link MessageListener}
 *
 * @author js.xie
 * @see MessageListener
 * @see MessageListenerHandlerMethod
 * @see MqListener
 */
@Configuration
@ConditionalOnClass(ONSFactory.class)
@EnableConfigurationProperties(ServerProperties.class)
@Order
public class ConsumerContainerConfiguration implements ApplicationContextAware, EnvironmentAware {

    private ConfigurableApplicationContext applicationContext;

    private Environment environment;

    @PostConstruct
    public void init() {

        // get bean that annotated MessageListener
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(MessageListener.class);

        if (Objects.nonNull(beans)) {
            beans.forEach(this::registerConsumerContainer);
        }

    }

    private void registerConsumerContainer(String beanName, Object bean) {

        // get actual bean class that not proxy bean
        Class<?> originTargetClass = AopUtils.getTargetClass(bean);

        Method[] uniqueDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(originTargetClass);

        // hold methods that annotated ListenerHandlerMethod
        List<Method> listenerHandlerMethods = new ArrayList<>();

        for (Method method : uniqueDeclaredMethods) {

            MessageListenerHandlerMethod annotation = AnnotationUtils.findAnnotation(method, MessageListenerHandlerMethod.class);

            if (annotation != null) {
                listenerHandlerMethods.add(method);
            }

        }

        // if not have method that annotated ListenerHandlerMethod,detect whether implement MQListener interface
        if (CollectionUtils.isEmpty(listenerHandlerMethods)) {

            if (MqListener.class.isAssignableFrom(originTargetClass)) {

                MqListenerContainerRegistrar.registryFromInterface(beanName, bean, originTargetClass, environment, applicationContext);

                return;

            }


        }

        // method that annotated ListenerHandlerMethod must not more than one
        if (listenerHandlerMethods.size() > 1) {
            throw new IllegalStateException("originTargetClass:" + originTargetClass + "must not have more than one listenerHandlerMethod,excepted one but find" + listenerHandlerMethods.size());
        }


        // handler method that annotated ListenerHandlerMethod
        MqListenerContainerRegistrar.registryFromAnnotation(beanName, bean, originTargetClass, environment, applicationContext, listenerHandlerMethods);

    }


    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

}
