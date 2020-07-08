package io.github.leejoker.consul;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.github.leejoker.ConsulProperties;
import io.github.leejoker.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConsulComponent {
    private static final ConcurrentHashMap<String, String> SERVICE_MAP = new ConcurrentHashMap<>();

    @Autowired
    private ConsulProperties consulProperties;

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
                } catch (IOException e) {
                    log.error("Get Service Info Error: {}", ExceptionUtils.getStackTrace(e));
                }
            });
        }
    }
}
