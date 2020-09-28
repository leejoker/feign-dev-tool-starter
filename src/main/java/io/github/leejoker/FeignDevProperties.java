package io.github.leejoker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "feign-tool")
public class FeignDevProperties {
    private List<Redirect> redirects;
}
