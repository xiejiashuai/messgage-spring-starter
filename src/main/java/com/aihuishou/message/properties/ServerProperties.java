package com.aihuishou.message.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author js.xie
 */
@ConfigurationProperties(prefix = "spring.message.ons.server")
@Data
public class ServerProperties {

    /**
     * name server for ons, formats: `host:port;host:port`
     */
    private String nameServer;

    private String accessKey;

    private String secretKey;


}
