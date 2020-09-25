package io.github.leejoker.utils;

import io.github.leejoker.consul.ConsulProperties;
import io.github.leejoker.exceptions.ConsulException;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class ConsulUtils {
    public static String getServiceUrl(ConsulProperties consulProperties) {
        return getServiceUrl(consulProperties, null);
    }

    public static String getServiceUrl(ConsulProperties consulProperties, String serviceName) {
        String host = consulProperties.getHost();
        Integer port = consulProperties.getPort();
        String dataCenter = consulProperties.getDataCenter();
        if (StringUtils.isBlank(host)) {
            throw new ConsulException("host is empty");
        }
        if (port == null || port == 0) {
            throw new ConsulException("port is empty or port is zero");
        }
        if (StringUtils.isBlank(dataCenter)) {
            throw new ConsulException("dataCenter is empty");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("http://")
                .append(host).append(":").append(port);
        if (StringUtils.isBlank(serviceName)) {
            stringBuilder.append("/v1/internal/ui/services?dc=");
        } else {
            stringBuilder.append("/v1/health/service/").append(serviceName);
        }
        stringBuilder.append("?dc=").append(dataCenter);
        return stringBuilder.toString();
    }

    public static String getActualServiceUrl(Map<String, Object> serv) {
        return "http://" +
                serv.get("Address") +
                ":" +
                serv.get("Port");
    }
}
