package io.github.leejoker;

import io.github.leejoker.consul.ConsulProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FeignDevProperties.class, ConsulProperties.class})
public class FeignDevToolAutoConfiguration {

}
