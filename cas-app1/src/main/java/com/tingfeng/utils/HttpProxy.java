package com.tingfeng.utils;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpMethod;

public class HttpProxy {


    /**
     * Http 请求(Json格式参数)
     *
     * @param requestUrl
     * @param requestJson
     * @param httpMethod
     * @return
     * @throws Exception
     */
    public static String httpRequest(String requestUrl, String requestJson, HttpMethod httpMethod) {

        CloseableHttpClient httpClient = null;
        try {
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie cookie = new BasicClientCookie("UToken", "test@TGTaut-hahdfahdf");
            cookie.setDomain("sso.server.com");
            cookie.setPath("/");
            cookieStore.addCookie(cookie);
            httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
            // httpClient = HttpClients.createDefault();
            HttpResponse response;

            if (null == httpMethod) {
                throw new RuntimeException("Http Method should be Get, Post, Put");
            }

            if (HttpMethod.GET == httpMethod) {
                HttpGet httpGet = new HttpGet(requestUrl);
                response = httpClient.execute(httpGet);
            } else {
                HttpEntityEnclosingRequestBase requestBase = null;

                switch (httpMethod) {
                    case POST:
                        requestBase = new HttpPost(requestUrl);
                        break;
                    case PUT:
                        requestBase = new HttpPut(requestUrl);
                        break;
                }

                if (null != requestJson && !requestJson.trim().equals("")) {
                    StringEntity requestEntity = new StringEntity(requestJson, ContentType.APPLICATION_JSON);
                    if (requestBase != null) {
                        requestBase.setEntity(requestEntity);
                    }
                }
                response = httpClient.execute(requestBase);
            }

            HttpEntity httpEntity = response.getEntity();
            return EntityUtils.toString(httpEntity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}