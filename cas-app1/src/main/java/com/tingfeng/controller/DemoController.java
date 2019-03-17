package com.tingfeng.controller;

import com.google.gson.Gson;
import com.tingfeng.cas.config.CasConfig;
import com.tingfeng.domain.User;
import com.tingfeng.utils.HttpProxy;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DemoController {
    // 后台接口服务，暂时不用
    private static String API_BASE_URL = "http://client1.com:8888/";
    private static String SSO_BASE_URL = "http://sso.server.com:9000/sso/user/logout";

    @GetMapping("/hello")
    public String hello() {
        return "前端 Hello 接口响应";
    }

    @GetMapping("/world")
    public String world() {
        // String result = HttpProxy.httpRequest(API_BASE_URL + "/world", null, HttpMethod.GET);
        String result = "world";
        System.out.println("Client1 接口响应结果：" + result);
        return result;
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpSession session) {
        System.out.println("Client1 登出开始了。。。。。。。。。。。" );
        Cookie[] cookies = request.getCookies();

        // if (cookies != null) {
        //     System.out.println(new Gson().toJson(cookies));
        //
        //     for (Cookie cookie : request.getCookies()) {
        //         if (cookie.getName().equals(CasConfig.COOKIE_NAME)) {
        //             username = cookie.getValue().split("@")[0];
        //             tgt = cookie.getValue().split("@")[1];
        //             break;
        //         }
        //     }
        //
        //     if (username != null) {
        //         // 获取Redis值
        //         String value = tgtServer.getTGT(username);
        //         System.out.println("Redis value：" + value);
        //
        //         // 匹配Redis中的TGT与Cookie中的TGT是否相等
        //         if (tgt.equals(value)) {
        //
        //             // 获取 ST
        //             String st = CasServerUtil.getST(tgt, service);
        //             System.out.println("ST：" + st);
        //
        //             result.setStatus(1);
        //             result.setData(service + "?ticket=" + st);
        //         }
        //     }
        // }


        String result = HttpProxy.httpRequest(SSO_BASE_URL, null, HttpMethod.GET);
        System.out.println("logout result = " + result);
        if ("logout".equals(result)) {
            System.out.println("同时注销掉本地session");
            // 同时注销掉本地session
            session.invalidate();
        }
        return result;
    }

    @GetMapping("/users")
    public List<User> users(HttpSession session) {
        System.out.println("session name: " + session.getAttribute("name"));
        if (session.getAttribute("name") == null) {
            session.setAttribute("name", "username");
        }
        // String result = HttpProxy.httpRequest(API_BASE_URL + "/user/users", null, HttpMethod.GET);
        String result = "users";
        System.out.println("Client1 接口响应结果：" + result);

        // if (null != result && !result.equals("")) {
        //     Gson gson = new Gson();
        //     List<User> userList = gson.fromJson(result, List.class);
        //     return userList;
        // }

        return new ArrayList<>();
    }

    @GetMapping("/books")
    public List<String> books() {

        String result = HttpProxy.httpRequest(API_BASE_URL + "/book/books", null, HttpMethod.GET);
        System.out.println("Client1 接口响应结果：" + result);

        if (null != result && !result.equals("")) {
            Gson gson = new Gson();
            List<String> nameList = gson.fromJson(result, List.class);
            return nameList;
        }

        return null;
    }

}
