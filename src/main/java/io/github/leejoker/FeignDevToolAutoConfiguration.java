package io.github.leejoker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConsulProperties.class)
@ConditionalOnProperty("feign-tool")
public class FeignDevToolAutoConfiguration {

}
