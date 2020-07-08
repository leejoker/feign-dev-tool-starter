package io.github.leejoker.aspect;


import feign.Feign;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import io.github.leejoker.consul.ConsulComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 用来动态设置Feign Url
 */
@Aspect
@Slf4j
@Component
@Import(FeignClientsConfiguration.class)
public class FeignAspect {
    @Autowired
    private ConsulComponent consulComponent;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Decoder decoder;

    @Autowired
    private Encoder encoder;

    // 定义Feign的切点，切打@FeignClient注解的
    @Pointcut("within(@org.springframework.cloud.openfeign.FeignClient *)")
    public void feignClient() {
    }

    @Around("feignClient()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        FeignClient annotation = clazz.getAnnotation(FeignClient.class);
        if (annotation != null) {
            String serviceName = annotation.name();
            String url = consulComponent.getUrl(serviceName);
            if (StringUtils.isNotBlank(url)) {
                InvocationHandler ih = Proxy.getInvocationHandler(pjp.getTarget());
                Field targetField = ih.getClass().getDeclaredField("target");
                targetField.setAccessible(true);
                Target target = (Target) targetField.get(ih);
                Field typeField = target.getClass().getDeclaredField("type");
                typeField.setAccessible(true);
                Class claz = (Class) typeField.get(target);

                //在Bean中设置url
                Field urlField = target.getClass().getDeclaredField("url");
                urlField.setAccessible(true);
                urlField.set(target, url);

                //重新注册bean
                ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
                DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
                Object bean = Feign.builder().contract(new SpringMvcContract()).encoder(encoder).decoder(decoder).build().newInstance(target);
                BeanDefinition oldBeanDefinition = beanFactory.getBeanDefinition(claz.getName());
                beanFactory.removeBeanDefinition(claz.getName());
                oldBeanDefinition.setBeanClassName(claz.getName());
                beanFactory.registerBeanDefinition(claz.getName(), oldBeanDefinition);
                beanFactory.registerSingleton(claz.getName(), bean);
            }
        }
        return pjp.proceed();
    }
}
