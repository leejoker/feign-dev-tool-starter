package io.github.leejoker.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * http请求工具类
 */
@Slf4j
public class HttpUtils {
    public static String getJson(String url) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);
        get.setHeader("Content-Type", "application/json;charset=utf8");
        return getResponseValue(client, get);
    }

    public static String putJson(String url, String params) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPut put = new HttpPut(url);
        put.setHeader("Content-Type", "application/json;charset=utf8");
        //发送内容实体
        StringEntity entity = new StringEntity(params, "UTF-8");
        put.setEntity(entity);
        return getResponseValue(client, put);
    }

    public static String postJson(String url, JSONObject json) throws IOException {
        return postJson(url, json.toJSONString());
    }

    public static String postJson(String url, String params) throws IOException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json;charset=utf8");
        //发送内容实体
        StringEntity entity = new StringEntity(params, "UTF-8");
        post.setEntity(entity);
        return getResponseValue(client, post);
    }

    private static String getResponseValue(CloseableHttpClient client, HttpUriRequest request) throws IOException {
        CloseableHttpResponse response = null;
        try {
            response = client.execute(request);
            // 从响应模型中获取响应实体
            HttpEntity responseEntity = response.getEntity();
            log.info("响应状态为:" + response.getStatusLine());
            if (responseEntity != null) {
                return EntityUtils.toString(responseEntity);
            }
        } catch (IOException e) {
            log.error("post请求失败: " + e.getMessage());
            throw e;
        } finally {
            // 释放资源
            if (client != null) {
                client.close();
            }
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
}
