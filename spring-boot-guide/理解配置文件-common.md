# SpringBoot2.x入门教程：理解配置文件

## 前提

这篇文章是《SpringBoot2.x入门》专辑的**第4篇**文章，使用的`SpringBoot`版本为`2.3.1.RELEASE`，`JDK`版本为`1.8`。

主要介绍`SpringBoot`配置文件一些常用属性、配置文件的加载优先级以及一些和配置相关的注意事项。

<!-- more -->

## 关于SpringBoot的配置文件

一个基于标准的引入了`SpringBoot`组件的`Maven`项目的结构一般如下：

```shell
Root（项目根目录）
  - src
   - main
    - java
    - resources  # <-- 这个就是资源文件的存放目录
  - target   
  pom.xml
```

资源文件存放在`src/main/resouces`目录下，而配置文件本质上也是资源文件，所以**项目内的**配置文件就存放于该目录下。从`SpringBoot`的属性源加载器`PropertySourceLoader`的实现来看，目前支持`Properties`和`Yaml`两种配置文件类型。两者各有优势：`Yaml`的配置属性更灵活，而`Properties`的配置不容易出错（笔者前公司的技术规范中明确了`SpringBoot`应用必须使用`Properties`配置文件，因为运维或者开发同事曾因为生产配置使用了`Yaml`格式的文件，编辑期间因为空格问题导致了严重的生产故障）。下文会使用`Properties`配置文件作为示例。

`SpringBoot`的配置文件使用了`profile`（`profile`本身就有剖面、配置文件的含义，下面会把`profile`作为一个专有名词使用）的概念，可以类比为区分不同环境的标识符，一个`SpringBoot`应用允许使用多个`profile`，所以配置文件的格式必须为`application-${profile}.文件后缀`，例如：

```shell
src/main/resources
   - application.properties
   - application-dev.properties  # <-- profile = dev，开发环境配置
   - application-test.properties # <-- profile = test，测试环境配置
```

其中不带`profile`标识的`application.properties`，可以理解为**主配置文件**，也就是`SpringBoot`的配置文件其实有继承关系，项目启动时，主配置文件无论如何都会优先加载，然后被激活的`profile`标识的配置文件才会加载，可以通过属性`spring.profiles.active`指定激活哪一个`profile`配置文件，如：

```properties
# 指定加载application-dev.properties
spring.profiles.active=dev

# 或者同时加载application-dev.properties和application-test.properties
spring.profiles.active=dev,test
```

`spring.profiles.active`一般可以在主配置文件`application.properties`中指定，获取通过启动命令参数指定（`java -jar -Dspring.profiles.active=prod app.jar`或者`java -jar app.jar --spring.profiles.active=prod`）。

可以通过自动装配`org.springframework.core.env.Environment`，通过`Environment#getActiveProfiles()`获取当前激活的`profile`数组，例如：

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import java.util.Arrays;

@Slf4j
@SpringBootApplication
public class Ch2Application implements CommandLineRunner {

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(Ch2Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Active profiles:{}", Arrays.toString(environment.getActiveProfiles()));
    }
}
```

运行结果如下：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch4-1.png)

这里用到了`CommandLineRunner`接口作为展示，后面的文章会介绍该接口的使用方式。

## 常用的基本配置属性

一般情况下会引入`spring-boot-starter-web`开发`web`项目，有几个基本配置笔者认为是必须的。主配置文件`application.properties`中应该标识应用名和默认选用的`profile`，例如`API`网关的主配置文件如下：

```properties
spring.application.name=api-gateway
spring.profiles.active=dev
```

此外，主配置中间中应该配置一些不容易变动的属性，例如`Mybatis`的`Mapper`扫描路径、模板引擎`Freemarker`的配置属性等等。而`profile`标识的配置文件中，应该配置一些跟随环境变化的配置或者经常更变的属性，例如`MySQL`数据源的配置、`Redis`的连接配置等等，以便通过`spring.profiles.active`直接切换不同环境的中间件连接属性或者第三方配置。在`Hello World`类型的项目中，一般添加`server.port`指定容器的启动端口，如`application-dev.properties`的内容如下：

```properties
server.port=8081
```

## 配置文件加载优先级与属性覆盖

除了主配置文件会优先`profile`标识的配置文件加载之外，`SpringBoot`还支持通过文件系统加载配置文件，这些配置文件不一定在项目内（准确来说是项目编译之后打出来的包内），还可以存在于特定的磁盘路径中。这一点可以参考`SpringBoot`官方文档`2.Externalized Configuration`一节：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch4-2.png)

默认的配置文件加载优先级顺序是：

1. `file:./config/`（项目部署包所在目录的同级`config`目录下的`application-[profile].[properties,yaml]`文件）
2. `file:./config/*/`（项目部署包所在目录的同级`config`目录下的任意子目录中的`application-[profile].[properties,yaml]`文件）
3. `file:./`（项目部署包所在目录的`application-[profile].[properties,yaml]`文件）
4. `classpath:/config/`（项目部署包内类路径下的`config`目录下的`application-[profile].[properties,yaml]`文件）
5. `classpath:/`（项目部署包内类路径下的`application-[profile].[properties,yaml]`文件）

**眼尖的伙伴可能会发现，在项目中的`resources`目录下添加的配置文件的加载优先级是最低的（打包后相当于第5条）**。可以通过`spring.config.location`属性覆盖上面的顺序，如`spring.config.location=classpath:/,classpath:/config/`，一般不建议改变默认的配置顺序，除非有特殊的使用场景。

另外，还可以通过`spring.config.additional-location`属性指定额外附加的搜索配置文件的路径，并且**优先级比默认的配置顺序要高**，假如只配置了`spring.config.additional-location=classpath:/custom-config/,file:./custom-config/`，那么配置文件加载优先级顺序是：

1. `file:./custom-config/`（项目部署包所在目录的同级`custom-config`目录下的`application-[profile].[properties,yaml]`文件）
2. `classpath:custom-config/`（项目部署包内类路径下的`custom-config`目录下的`application-[profile].[properties,yaml]`文件）
3. `file:./config/`（项目部署包所在目录的同级`config`目录下的`application-[profile].[properties,yaml]`文件）
4. `file:./config/*/`（项目部署包所在目录的同级`config`目录下的任意子目录中的`application-[profile].[properties,yaml]`文件）
5. `file:./`（项目部署包所在目录的`application-[profile].[properties,yaml]`文件）
6. `classpath:/config/`（项目部署包内类路径下的`config`目录下的`application-[profile].[properties,yaml]`文件）
7. `classpath:/`（项目部署包内类路径下的`application-[profile].[properties,yaml]`文件）

基于这个特性，在不对接配置中心的前提下，可以让运维伙伴在生产服务器上先配置好服务所需的生产环境的配置文件：

```shell
# 假设这个是生产服务器的文件路径
/data/apps/api-gateway
     - api-gateway.jar
     - config
       - application-prod.properties
```

在编写启动脚本的时候只需指定`profile`为`prod`即可，应用会读取`/data/apps/api-gateway/config/application-prod.properties`的属性，这样就能避免生产配置或者敏感属性泄漏给开发人员。

这里还有一个比较重要的问题就是：如果在多种路径下的配置文件定义了同一个属性，那么属性会依照一个优先级顺序进行覆盖。因为`SpringBoot`除了配置文件，还支持命令行、`JNDI`属性、系统属性等等，如果全部列举会比较复杂，这里按照目前分析过的内容列举这个优先级顺序：

1. 命令行中的属性参数。
2. 项目部署包之外的`application-profile.[properties,yaml]`文件。
3. 项目部署包内的`application-profile.[properties,yaml]`文件。
4. 项目部署包之外的`application.[properties,yaml]`文件。
5. 项目部署包内的`application.[properties,yaml]`文件。

举个例子，假如启动参数中添加`--app.author=throwable`，配置文件`application.properties`中添加属性`app.author=throwable-x`，而配置文件`application-dev.properties`中添加属性`app.author=throwable-y`，那么使用`profile=dev`启动应用的时候，优先获取到的是属性`app.author=throwable`：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/s-p-g-ch4-3.png)

> 如果看过SpringBoot属性加载的源码可知，其实属性优先级的思路在设计属性加载模块的时候正好相反，所有的配置文件都会进行解析，构成一个复合的PropertySource，后解析的参数总是在顶层，然后获取属性的时候，总是先从顶层获取。

## 自定义配置属性与IDE亲和性

有时候需要配置自定义属性，会出现在`IDE`中会无法识别而"标黄"的场景。这个时候可以应用`IDE`亲和性。在主流的`IDE`如`Eclipse`和`IntelliJ IDEA`中，只需要引入`SpringBoot`的属性元数据描述文件（`spring-configuration-metadata.json`或者`additional-spring-configuration-metadata.json`），即可让`IDE`识别，提供目录引导跳转的功能，不再"标黄"。具体的做法是在项目的`resources/META-INF`目录中引入属性元数据描述文件，然后编写属性描述即可：

```json
// resources/META-INF/spring-configuration-metadata.json
{
  "properties": [
    {
      "name": "app.author",
      "type": "java.lang.String",
      "description": "The author of app."
    }
  ]
}
```

`spring-configuration-metadata.json`文件的格式可以参考`SpringBoot`多个`starter`中已经存在的文件，**完成这一点，代码洁癖患者或者强迫症患者会感觉良好**。

## 小结

这篇文章简单总结了配置文件加载的优先级顺序和配置属性的覆盖优先级顺序，这两点需要完全掌握，可以自行通过一些例子改变一下配置文件进行熟悉。配置属性覆盖的问题很容易导致生产故障，如果掌握了本节的内容，对于`SpringBoot`配置属性方面的问题应该可以快速定位和解决。代码仓库：

- Github：https://github.com/zjcscut/spring-boot-guide/tree/master/ch2-profile

（本文完 c-2-d e-a-20200705）