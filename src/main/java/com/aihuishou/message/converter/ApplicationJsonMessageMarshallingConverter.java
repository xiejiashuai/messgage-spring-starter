package com.aihuishou.message.converter;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.shade.com.alibaba.rocketmq.common.message.MessageConst;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Properties;

/**
 * @author js.xie
 */
@Slf4j
public class ApplicationJsonMessageMarshallingConverter implements MessageConverter {

    private String charset = "utf-8";

    @Setter
    private ObjectMapper objectMapper;

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Override
    public Object fromMessage(Message message, Class<?> targetClass) {

        if (Objects.equals(targetClass, Message.class)) {

            return message;

        } else {

            String str = new String(message.getBody(), Charset.forName(charset));

            log.info("receive msg :{}", str);

            if (Objects.equals(targetClass, String.class)) {

                return str;

            } else {

                try {

                    return objectMapper.readValue(str, targetClass);

                } catch (Exception e) {

                    log.info("convert failed. str:{}, msgType:{}", str, targetClass);
                    throw new RuntimeException("cannot convert message to " + targetClass + "exception:{}" + e.getMessage() + e);

                }
            }
        }
    }

    @Override
    public Message toMessage(String destination, org.springframework.messaging.Message<?> message) {

        Object payloadObj = message.getPayload();

        byte[] payloads;

        if (payloadObj instanceof String) {

            payloads = ((String) payloadObj).getBytes(Charset.forName(charset));

        } else {

            try {

                String jsonObj = this.objectMapper.writeValueAsString(payloadObj);

                payloads = jsonObj.getBytes(Charset.forName(charset));

            } catch (Exception e) {

                throw new RuntimeException("convert to RocketMQ message failed.", e);

            }
        }

        String[] tempArr = destination.split(":", 2);
        String topic = tempArr[0];
        String tags = "";
        if (tempArr.length > 1) {
            tags = tempArr[1];
        }

        com.aliyun.openservices.ons.api.Message onsMsg = new com.aliyun.openservices.ons.api.Message(topic, tags, payloads);

        MessageHeaders headers = message.getHeaders();

        if (Objects.nonNull(headers) && !headers.isEmpty()) {

            Object keys = headers.get(MessageConst.PROPERTY_KEYS);

            // if headers has 'KEYS', set ons message key
            if (!StringUtils.isEmpty(keys)) {
                onsMsg.setKey(keys.toString());
            }

            Properties userProperties = new Properties();

            headers.entrySet()
                    .stream()
                    // exclude "KEYS"
                    .filter(entry -> !Objects.equals(entry.getKey(), MessageConst.PROPERTY_KEYS))
                    .forEach(entry -> userProperties.setProperty("USERS_" + entry.getKey(), String.valueOf(entry.getValue())));

            // add other properties with prefix "USERS_"
            onsMsg.setUserProperties(userProperties);

        }

        return onsMsg;
    }

    @Override
    public String toString(Object payload) throws JsonProcessingException {
        return objectMapper.writeValueAsString(payload);
    }
}
