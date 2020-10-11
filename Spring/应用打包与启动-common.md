# SpringBoot2.x入门：应用打包与启动

## 前提

这篇文章是《SpringBoot2.x入门》专辑的**第5篇**文章，使用的`SpringBoot`版本为`2.3.1.RELEASE`，`JDK`版本为`1.8`。

这篇文章分析一个偏向于运维方面的内容：`SpringBoot`应用的打包与启动，分别会分析嵌入式`Servlet`容器和非嵌入式`Servlet`容器下的应用打包与启动，`Servlet`容器以比较常用的`Tomcat`为例。

## 嵌入式Tomcat的打包与启动

嵌入式`Tomcat`由`spring-boot-starter-web`这个`starter`自带，因此不需要改动关于`Servlet`容器的依赖。新建一个启动类`club.throwable.ch4.Ch4Application`：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Ch4Application {

    public static void main(String[] args) {
        SpringApplication.run(Ch4Application.class, args);
    }
}
```

添加一个主配置文件`application.properties`：

```properties
server.port=9094
spring.application.name=ch4-embedded-tomcat-deploy
```

然后在项目的`pom.xml`引入`Maven`插件`spring-boot-maven-plugin`：

```xml
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
```

然后使用命令`mvn clean compile package`（`mvn clean`、`mvn compile`和`mvn package`的组合命令）打包即可：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-1.png)

如果编译和打包命令执行成功的话，控制台输出**BUILD SUCCESS**：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-2.png)

同时项目的`target`目录下（除了一些编译出来的`class`文件）会多出了一个`Jar`包和一个`x.jar.original`文件：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-3.png)

而这个`Jar`文件正是可运行的文件，可以通过命令（确保已经安装`JDK`并且把`JRE`的`bin`目录添加到系统的`Path`中）运行：

```shell
java -jar ch4-embedded-tomcat-deploy.jar
```

控制台输出如下：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-4.png)

一般情况下`Jar`的执行命令是：

```shell
java [VM_OPTIONS] -jar 应用名.jar [SPRING_OPTIONS]
例如：
java -Xms1g -Xmx2g -jar ch4-embedded-tomcat-deploy.jar --spring.profiles.active=default
```

上面的命令会导致应用挂起在控制台，只要退出控制台，应用就会被`Shutdown`。如果在`Linux`下，可以使用`nohup`（其实就是`no hang up`的缩写）命令不挂断地运行`Jar`应用，例如：

```shell
nohup java -Xms1g -Xmx2g -jar ch4-embedded-tomcat-deploy.jar --spring.profiles.active=default >/dev/null 2>&1 &
```

## 非嵌入式Tomcat的打包与启动

一般情况下，非嵌入式`Tomcat`需要打包成一个`war`文件，然后放到外部的`Tomcat`服务中运行。

- 首先要**移除**掉`spring-boot-starter-web`依赖中的嵌入式`Tomcat`相关的依赖，并且引入`servlet-api`依赖。
- 还要把打包方式设置为`war`（`<packaging>jar</packaging>`替换为`<packaging>war</packaging>`）。
- 最后还要升级`maven-war-plugin`插件避免因为缺失`web.xml`文件导致打包失败。

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-5.png)

这里为了满足兼容性，使用的`Tomcat`版本最好和`spring-boot-starter-web`中引用的嵌入式`Tomcat`的依赖版本完全一致，在`SpringBoot:2.3.1.RELEASE`中，该版本为`9.0.36`，`pom.xml`的依赖内容如下：

```xml
<packaging>war</packaging>
<dependencies>
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
        <groupId>org.apache.tomcat</groupId>
        <artifactId>tomcat-servlet-api</artifactId>
        <version>9.0.36</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
<build>
    <finalName>ch3-tomcat-deploy</finalName>
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
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-war-plugin</artifactId>
            <version>3.3.0</version>
        </plugin>
    </plugins>
</build>
```

这里其实可以选择不排除`spring-boot-starter-tomcat`，而是把它的作用域缩小为`provided`，这样就能避免额外引入`servlet-api`依赖：

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
       <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

新建一个启动类`club.throwable.ch3.Ch3Application`，必须继承`SpringBootServletInitializer`并且重写`configure()`方法执行入口类：

```java
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class Ch3Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Ch3Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Ch3Application.class);
    }
}
```

然后使用命令`mvn clean compile package`打包：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-6.png)

下载`Tomcat9.0.36`，下载地址是`https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.36/bin`（因为开发机的系统是64bit的Windows10系统）：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-7.png)

解压`Tomcat`后，把`ch3-tomcat-deploy.war`拷贝到`webapps`目录下，然后使用`bin/startup.bat`启动`Tomcat`：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch5-8.png)

由于`application.properties`里面管理的端口和服务上下文路径配置会失效，需要从`Tomcat`的入口访问服务，如`http://localhost:8080/ch3-tomcat-deploy/`。

## 小结

这篇文章分别介绍`SpringBoot`的`Jar`和`War`两种打包和部署方式，其实更推荐`Jar`包的方式，因为嵌入式容器对于开发和发布而言都会相对简便，而且它是`SpringBoot`默认的启动方式，该方式下默认就支持静态资源整合到`Jar`包中，可以直接访问。在前后端分离的大型应用中，相对轻量级可以脱离外部容器直接运行的部署方式明显更加吃香。

项目仓库：

- Github：https://github.com/zjcscut/spring-boot-guide

（本文完 c-2-d e-a-20200709 1:15 AM）




