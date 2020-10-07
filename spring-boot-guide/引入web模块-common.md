# SpringBoot2.x入门：引入web模块

## 前提

这篇文章是《SpringBoot2.x入门》专辑的**第3篇**文章，使用的`SpringBoot`版本为`2.3.1.RELEASE`，`JDK`版本为`1.8`。

主要介绍`SpringBoot`的`web`模块引入，会相对详细地分析不同的`Servlet`容器（如`Tomcat`、`Jetty`等）的切换，以及该模块提供的`SpringMVC`相关功能的使用。

<!-- more -->

## 依赖引入

笔者新建了一个多模块的`Maven`项目，这次的示例是子模块`ch1-web-module`。

`SpringBoot`的`web`模块实际上就是`spring-boot-starter-web`组件（下称`web`模块），前面的文章介绍过使用`BOM`全局管理版本，可以在（父）`POM`文件中添加`dependencyManagement`元素：

```xml
<properties>
    <spring.boot.version>2.3.1.RELEASE</spring.boot.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring.boot.version}</version>
            <scope>import</scope>
            <type>pom</type>
        </dependency>
    </dependencies>
</dependencyManagement>
```

接来下在（子）`POM`文件中的`dependencies`元素引入`spring-boot-starter-web`的依赖：

```xml
<dependencies>
    <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

项目的子`POM`大致如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>club.throwable</groupId>
        <artifactId>spring-boot-guide</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <artifactId>ch1-web-module</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>
    <name>ch1-web-module</name>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
    <build>
        <finalName>ch1-web-module</finalName>
        <!-- 引入spring-boot-maven-plugin以便项目打包 -->
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring.boot.version}</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

`spring-boot-starter-web`模块中默认使用的`Servlet`容器是嵌入式（`Embedded`）`Tomcat`，配合打包成一个`Jar`包以便可以直接使用`java -jar xxx.jar`命令启动。

## SpringMVC的常用注解

`web`模块集成和扩展了`SpringMVC`的功能，移除但兼容相对臃肿的`XML`配置，这里简单列举几个常用的`Spring`或者`SpringMVC`提供的注解，简单描述各个注解的功能：

**组件注解：**

- `@Component`：标记一个类为`Spring`组件，扫描阶段注册到`IOC`容器。
- `@Repository`：标记一个类为`Repository`（仓库）组件，它的元注解为`@Component`，一般用于`DAO`层。
- `@Service`：标记一个类为`Service`（服务）组件，它的元注解为`@Component`。
- `@Controller`：标记一个类为`Controller`（控制器）组件，它的元注解为`@Component`，一般控制器是访问的入口，衍生注解`@RestController`，简单理解为`@Controller`标记的控制器内所有方法都加上下面提到的`@ResponseBody`。


**参数注解：**

- `@RequestMapping`：设置映射参数，包括请求方法、请求的路径、接收或者响应的内容类型等等，衍生注解为`GetMapping`、`PostMapping`、`PutMapping`、`DeleteMapping`、`PatchMapping`。
- `@RequestParam`：声明一个方法参数绑定到一个请求参数。
- `@RequestBody`：声明一个方法参数绑定到请求体，常用于内容类型为`application/json`的请求体接收。
- `@ResponseBody`：声明一个方法返回值绑定到响应体。
- `@PathVariable`：声明一个方法参数绑定到一个`URI`模板变量，用于提取当前请求`URI`中的部分到方法参数中。

## 编写控制器和启动类

在项目中编写一个控制器`club.throwable.ch1.controller.HelloController`如下：

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping(path = "/ch1")
public class HelloController {
    
    @RequestMapping(path = "/hello")
    public ResponseEntity<String> hello(@RequestParam(name = "name") String name) {
        String value = String.format("[%s] say hello", name);
        log.info("调用[/hello]接口,参数:{},响应结果:{}", name, value);
        return ResponseEntity.of(Optional.of(value));
    }
}
```

`HelloController`只提供了一个接收`GET`请求且请求的路径为`/ch1/hello`的方法，它接收一个名称为`name`的参数（参数必传），然后返回简单的文本：`${name} say hello`。可以使用衍生注解简化如下：

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(path = "/ch1")
public class HelloController {

    @GetMapping(path = "/hello")
    public ResponseEntity<String> hello(@RequestParam(name = "name") String name) {
        String value = String.format("[%s] say hello", name);
        log.info("调用[/hello]接口,参数:{},响应结果:{}", name, value);
        return ResponseEntity.of(Optional.of(value));
    }
}
```

接着编写一个启动类`club.throwable.ch1.Ch1Application`，启动类是`SpringBoot`应用程序的入口，需要提供一个`main`方法：

```java
package club.throwable.ch1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Ch1Application {

    public static void main(String[] args) {
        SpringApplication.run(Ch1Application.class, args);
    }
}
```

然后以`DEBUG`模式启动一下：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch2-1.png)

`Tomcat`默认的启动端口是`8080`，启动完毕后见日志如下：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch2-2.png)

用用浏览器访问`http://localhost:8080/ch1/hello?name=thrwoable`可见输出如下：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch2-3.png)

至此，一个简单的基于`spring-boot-starter-web`开发的`web`应用已经完成。

## 切换Servlet容器

有些时候由于项目需要、运维规范或者个人喜好，并不一定强制要求使用`Tomcat`作为`Servlet`容器，常见的其他选择有`Jetty`、`Undertow`，甚至`Netty`等。以`Jetty`和`Undertow`为例，切换为其他嵌入式`Servlet`容器需要从`spring-boot-starter-web`中排除`Tomcat`的依赖，然后引入对应的`Servlet`容器封装好的`starter`。

**切换为`Jetty`**，修改`POM`文件中的`dependencies`元素：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring‐boot‐starter‐jetty</artifactId>
</dependency>
```

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch2-4.png)

**切换为`Undertow`**，修改`POM`文件中的`dependencies`元素：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring‐boot‐starter‐undertow</artifactId>
</dependency>
```

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch2-5.png)


## 小结

这篇文章主要分析了如何基于`SpringBoot`搭建一个入门的`web`服务，还简单介绍了一些常用的`SpringMVC`注解的功能，最后讲解如何基于`spring-boot-starter-web`切换底层的`Servlet`容器。学会搭建`MVC`应用后，就可以着手尝试不同的请求方法或者参数，尝试常用注解的功能。

## 代码仓库

这里给出本文搭建的`web`模块的`SpringBoot`应用的仓库地址（持续更新）：

- Github：https://github.com/zjcscut/spring-boot-guide/tree/master/ch1-web-module

（本文完 c-2-d e-a-20200703 23:09 PM）




