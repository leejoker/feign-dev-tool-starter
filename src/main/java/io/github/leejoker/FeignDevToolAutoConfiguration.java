package io.github.leejoker;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConsulProperties.class)
public class FeignDevToolAutoConfiguration {

}
