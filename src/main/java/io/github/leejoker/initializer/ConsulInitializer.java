package io.github.leejoker.initializer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.github.leejoker.ConsulProperties;
import io.github.leejoker.feign.FeignClientRegister;
import io.github.leejoker.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnClass(ConsulProperties.class)
public class ConsulInitializer {
	private static final ConcurrentHashMap<String, String> SERVICE_MAP = new ConcurrentHashMap<>();

	@Autowired
	private ConsulProperties consulProperties;

	@Autowired
	private FeignClientRegister feignClientRegister;

	@PostConstruct
	private void init() throws IOException {
		if (consulProperties != null && StringUtils.isNotBlank(consulProperties.getHost())
				&& consulProperties.getPort() != null && StringUtils.isNotBlank(consulProperties.getDataCenter())) {
			String url = "http://" + consulProperties.getHost() + ":" + consulProperties.getPort()
					+ "/v1/internal/ui/services?dc=" + consulProperties.getDataCenter();
			String serviceListJson = HttpUtils.getJson(url);
			JSONArray serviceList = JSONArray.parseArray(serviceListJson);
			serviceList.forEach(json -> {
				JSONObject service = JSONObject.parseObject(json.toString());
				String serviceName = service.getString("Name");
				String serviceUrl = "http://" + consulProperties.getHost() + ":" + consulProperties.getPort()
						+ "/v1/health/service/" + serviceName + "?dc=" + consulProperties.getDataCenter();
				try {
					String jsonStr = HttpUtils.getJson(serviceUrl);
					JSONArray nodes = JSONArray.parseArray(jsonStr);
					if (!CollectionUtils.isEmpty(nodes)) {
						JSONObject serviceJson = nodes.getJSONObject(0);
						JSONObject serv = serviceJson.getJSONObject("Service");
						String address = "http://" + serv.getString("Address") + ":" + serv.getInteger("Port");
						SERVICE_MAP.put(serviceName, address);
					}

					// 重新注册bean
					feignClientRegister.replaceFeignClientBean(SERVICE_MAP);
				} catch (IOException e) {
					log.error("Get Service Info Error: {}", ExceptionUtils.getStackTrace(e));
				}
			});
		}
	}

}
