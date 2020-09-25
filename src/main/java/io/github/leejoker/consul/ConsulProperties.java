package io.github.leejoker.consul;

import io.github.leejoker.Redirect;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "feign-tool.consul")
public class ConsulProperties {
    private String host;
    private Integer port;
    private String dataCenter;
    private String excludes;
    private List<Redirect> redirects;
}
