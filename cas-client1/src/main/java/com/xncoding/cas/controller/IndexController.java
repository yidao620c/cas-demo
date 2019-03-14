package com.xncoding.cas.controller;

/**
 * IndexController
 *
 * @author XiongNeng
 * @version 1.0
 * @since 2019/3/10
 */

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @RequestMapping("/index")
    public String index() {
        return " index 接口";
    }

    @RequestMapping("/world")
    public String world() {
        return " world 接口";
    }

}