package io.github.leejoker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "feign-tool.consul")
public class ConsulProperties {
    private String host;
    private Integer port;
    private String dataCenter;
}
