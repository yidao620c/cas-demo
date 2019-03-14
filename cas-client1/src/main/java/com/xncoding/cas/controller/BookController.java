package com.xncoding.cas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * BookController
 *
 * @author XiongNeng
 * @version 1.0
 * @since 2019/3/10
 */

@RestController
@RequestMapping
public class BookController {

    @GetMapping("/books")
    public List<String> books() {
        return Arrays.asList("《项塔兰》", "《肖申克的救赎？》", "《人类的群星闪耀时》", "《当我跑步时，我谈些什么》");
    }

}