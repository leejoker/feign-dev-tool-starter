package io.github.leejoker.feign;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import feign.Feign;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Import(FeignClientsConfiguration.class)
public class FeignClientRegister {
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Decoder decoder;

	@Autowired
	private Encoder encoder;

	public void replaceFeignClientBean(Map<String, String> map) {
		ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
		DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
		Map<String, Object> beans = beanFactory.getBeansWithAnnotation(FeignClient.class);
		beans.forEach((k, v) -> {
			try {
				if (Proxy.isProxyClass(v.getClass())) {
					InvocationHandler ih = Proxy.getInvocationHandler(v);
					Field targetField = ih.getClass().getDeclaredField("target");
					targetField.setAccessible(true);

					Target<?> target = (Target<?>) targetField.get(ih);
					Field nameField = target.getClass().getDeclaredField("name");
					nameField.setAccessible(true);

					String serviceName = (String) nameField.get(target);
					String url = map.get(serviceName);
					if (StringUtils.isNotBlank(url)) {
						Field urlField = target.getClass().getDeclaredField("url");
						urlField.setAccessible(true);
						urlField.set(target, url);

						Object bean = Feign.builder().contract(new SpringMvcContract()).encoder(encoder)
								.decoder(decoder).build().newInstance(target);
						BeanDefinition oldBeanDefinition = beanFactory.getBeanDefinition(k);
						beanFactory.removeBeanDefinition(k);
						oldBeanDefinition.setBeanClassName(k);
						beanFactory.registerBeanDefinition(k, oldBeanDefinition);
						beanFactory.registerSingleton(k, bean);
					}
				}
			} catch (Exception e) {
				log.error("重新注册Bean失败,exception={}", ExceptionUtils.getStackTrace(e));
			}
		});
	}
}
