package com.aihuishou.message.configuration;

import com.aihuishou.message.converter.ApplicationJsonMessageMarshallingConverter;
import com.aihuishou.message.converter.MessageConverter;
import com.aihuishou.message.producer.MqTemplate;
import com.aihuishou.message.properties.ProducerProperties;
import com.aihuishou.message.properties.ServerProperties;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.client.impl.MQClientAPIImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * @author js.xie
 */

@Configuration
@EnableConfigurationProperties(value = {ServerProperties.class, ProducerProperties.class})
@ConditionalOnClass(MQClientAPIImpl.class)
@Import(value = {ConsumerContainerConfiguration.class})
public class MqAutoConfiguration {


    @Autowired
    @Qualifier
    @Nullable
    private ObjectMapper objectMapper;

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter messageConverter() {

        ApplicationJsonMessageMarshallingConverter converter = new ApplicationJsonMessageMarshallingConverter();

        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        }
        converter.setObjectMapper(objectMapper);

        return converter;
    }

    @Bean
    @ConditionalOnClass(Producer.class)
    @ConditionalOnMissingBean(Producer.class)
    public Producer mqProducer(ServerProperties serverProperties, ProducerProperties producerProperties) {

        Properties properties = new Properties();
        properties.put(PropertyKeyConst.AccessKey, serverProperties.getAccessKey());
        properties.put(PropertyKeyConst.SecretKey, serverProperties.getSecretKey());
        if (StringUtils.hasText(serverProperties.getNameServer())) {
            properties.put(PropertyKeyConst.NAMESRV_ADDR, serverProperties.getNameServer());
        }
        properties.put(PropertyKeyConst.ProducerId, producerProperties.getProducerGroup());

        return ONSFactory.createProducer(properties);
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnBean(Producer.class)
    @ConditionalOnMissingBean(MqTemplate.class)
    public MqTemplate mqTemplate(Producer producer, MessageConverter converter, ProducerProperties producerProperties) {

        MqTemplate mqTemplate = new MqTemplate();
        mqTemplate.setProducer(producer);
        mqTemplate.setMessageConverter(converter);

        // enable additional retry
        if (producerProperties.getEnableRetry()) {
            mqTemplate.buildRetryTemplate(producerProperties.getRetry());
        }

        return mqTemplate;
    }


}
