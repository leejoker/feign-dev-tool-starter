package io.github.leejoker.consul;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import feign.Feign;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import io.github.leejoker.ConsulProperties;
import io.github.leejoker.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Import(FeignClientsConfiguration.class)
public class ConsulComponent {
    private static final ConcurrentHashMap<String, String> SERVICE_MAP = new ConcurrentHashMap<>();

    @Autowired
    private ConsulProperties consulProperties;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Decoder decoder;

    @Autowired
    private Encoder encoder;

    public String getUrl(String serviceName) {
        return SERVICE_MAP.get(serviceName);
    }

    @PostConstruct
    private void init() throws IOException {
        if (consulProperties != null && StringUtils.isNotBlank(consulProperties.getHost())
                && consulProperties.getPort() != null
                && StringUtils.isNotBlank(consulProperties.getDataCenter())) {
            String url = "http://" + consulProperties.getHost() + ":" + consulProperties.getPort() + "/v1/internal/ui/services?dc=" + consulProperties.getDataCenter();
            String serviceListJson = HttpUtils.getJson(url);
            JSONArray serviceList = JSONArray.parseArray(serviceListJson);
            serviceList.forEach(json -> {
                JSONObject service = JSONObject.parseObject(json.toString());
                String serviceName = service.getString("Name");
                String serviceUrl = "http://" + consulProperties.getHost() + ":" + consulProperties.getPort() + "/v1/health/service/" + serviceName + "?dc=" + consulProperties.getDataCenter();
                try {
                    String jsonStr = HttpUtils.getJson(serviceUrl);
                    JSONArray nodes = JSONArray.parseArray(jsonStr);
                    if (!CollectionUtils.isEmpty(nodes)) {
                        JSONObject serviceJson = nodes.getJSONObject(0);
                        JSONObject serv = serviceJson.getJSONObject("Service");
                        String address = "http://" + serv.getString("Address") + ":" + serv.getInteger("Port");
                        SERVICE_MAP.put(serviceName, address);
                    }

                    //重新注册bean
                    replaceFeignClientBean();
                } catch (IOException e) {
                    log.error("Get Service Info Error: {}", ExceptionUtils.getStackTrace(e));
                }
            });
        }
    }

    private void replaceFeignClientBean() {
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
        Map<String, Object> beans = beanFactory.getBeansWithAnnotation(FeignClient.class);
        beans.forEach((k, v) -> {
            try {
                if (Proxy.isProxyClass(v.getClass())) {
                    InvocationHandler ih = Proxy.getInvocationHandler(v);
                    Field targetField = ih.getClass().getDeclaredField("target");
                    targetField.setAccessible(true);

                    Target target = (Target) targetField.get(ih);
                    Field nameField = target.getClass().getDeclaredField("name");
                    nameField.setAccessible(true);

                    String serviceName = (String) nameField.get(target);
                    String url = getUrl(serviceName);
                    if (StringUtils.isNotBlank(url)) {
                        Field urlField = target.getClass().getDeclaredField("url");
                        urlField.setAccessible(true);
                        urlField.set(target, url);

                        Object bean = Feign.builder().contract(new SpringMvcContract()).encoder(encoder).decoder(decoder).build().newInstance(target);
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
