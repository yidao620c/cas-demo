# SpringBoot2.x入门：使用MyBatis

## 前提

这篇文章是《SpringBoot2.x入门》专辑的**第8篇**文章，使用的`SpringBoot`版本为`2.3.1.RELEASE`，`JDK`版本为`1.8`。

`SpringBoot`项目引入`MyBatis`一般的套路是直接引入`mybatis-spring-boot-starter`或者使用基于`MyBatis`进行二次封装的框架例如`MyBatis-Plus`或者`tk.mapper`等，但是本文会使用一种更加原始的方式，单纯依赖`org.mybatis:mybatis`和`org.mybatis:mybatis-spring`把`MyBatis`的功能整合到`SpringBoot`中，`Spring(Boot)`使用的是**微内核架构**，任何第三方框架或者插件都可以按照本文的思路融合到该微内核中。

<!-- more -->

## 引入MyBatis依赖

编写本文的时候（`2020-07-18`）`org.mybatis:mybatis`的最新版本是`3.5.5`，而`org.mybatis:mybatis-spring`的最新版本是`2.0.5`，在使用`BOM`管理`SpringBoot`版本的前提下，引入下面的依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.5</version>
</dependency>
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.0.5</version>
</dependency>
```

> 注意的是低版本的MyBatis如果需要使用JDK8的日期时间API，需要额外引入mybatis-typehandlers-jsr310依赖，但是某个版本之后mybatis-typehandlers-jsr310中的类已经移植到org.mybatis:mybatis中作为内建类，可以放心使用JDK8的日期时间API。

## 添加MyBatis配置

`MyBatis`的核心模块是`SqlSessionFactory`与`MapperScannerConfigurer`。前者可以使用`SqlSessionFactoryBean`，功能是为每个`SQL`的执行提供`SqlSession`和加载全局配置或者`SQL`实现的`XML`文件，后者是一个`BeanDefinitionRegistryPostProcessor`实现，主要功能是主动通过配置的基础包（`Base Package`）中递归搜索`Mapper`接口（这个算是`MyBatis`独有的扫描阶段，**务必指定明确的扫描包，否则会因为效率太低导致启动阶段耗时增加**），并且把它们注册成`MapperFactoryBean`（简单理解为接口动态代理实现添加到方法缓存中，并且委托到`IOC`容器，此后可以直接注入`Mapper`接口），注意这个`BeanFactoryPostProcessor`的回调优先级极高，在自动装配`@Autowired`族注解或者`@ConfigurationProperties`属性绑定处理之前已经回调，**因此在处理`MapperScannerConfigurer`的属性配置时候绝对不能使用`@Value`或者自定义前缀属性`Bean`进行自动装配**，但是可以从`Environment`中直接获取。

这里添加一个自定义属性前缀`mybatis`，用于绑定配置文件中的属性到`MyBatisProperties`类中：

```java
@ConfigurationProperties(prefix = "mybatis")
@Data
public class MyBatisProperties {

    private String configLocation;
    private String mapperLocations;
    private String mapperPackages;

    private static final ResourcePatternResolver RESOLVER = new PathMatchingResourcePatternResolver();

    /**
     * 转化Mapper映射文件为Resource
     */
    public Resource[] getMapperResourceArray() {
        if (!StringUtils.hasLength(mapperLocations)) {
            return new Resource[0];
        }
        List<Resource> resources = new ArrayList<>();
        String[] locations = StringUtils.commaDelimitedListToStringArray(mapperLocations);
        for (String location : locations) {
            try {
                resources.addAll(Arrays.asList(RESOLVER.getResources(location)));
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return resources.toArray(new Resource[0]);
    }
}
```

接着添加一个`MybatisAutoConfiguration`用于配置`SqlSessionFactory`：

```java
@Configuration
@EnableConfigurationProperties(value = {MyBatisProperties.class})
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@RequiredArgsConstructor
public class MybatisAutoConfiguration {

    private final MyBatisProperties myBatisProperties;

    @Bean
    public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        // 其实核心配置就是这两项,其他TypeHandlersPackage、TypeAliasesPackage等等自行斟酌是否需要添加
        bean.setConfigLocation(new ClassPathResource(myBatisProperties.getConfigLocation()));
        bean.setMapperLocations(myBatisProperties.getMapperResourceArray());
        return bean;
    }

    /**
     * 事务模板,用于编程式事务 - 可选配置
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager platformTransactionManager) {
        return new TransactionTemplate(platformTransactionManager);
    }

    /**
     * 数据源事务管理器 - 可选配置
     */
    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager platformTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

一般情况下，启用事务需要定义`PlatformTransactionManager`的实现，而`TransactionTemplate`适用于编程式事务（和声明式事务`@Transactional`区别，编程式更加灵活）。上面的配置类中只使用了两个属性，而`mybatis.mapperPackages`将用于`MapperScannerConfigurer`的加载上。添加`MapperScannerRegistrarConfiguration`如下：

```java
@Configuration
public class MapperScannerRegistrarConfiguration {

    public static class AutoConfiguredMapperScannerRegistrar implements
            BeanFactoryAware, EnvironmentAware, ImportBeanDefinitionRegistrar {

        private Environment environment;
        private BeanFactory beanFactory;


        @Override
        public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void setEnvironment(@NonNull Environment environment) {
            this.environment = environment;
        }

        @Override
        public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata,
                                            @NonNull BeanDefinitionRegistry registry) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
            builder.addPropertyValue("processPropertyPlaceHolders", true);
            StringJoiner joiner = new StringJoiner(ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
            // 这里使用了${mybatis.mapperPackages},否则会使用AutoConfigurationPackages.get(this.beanFactory)获取项目中自定义配置的包
            String mapperPackages = environment.getProperty("mybatis.mapperPackages");
            if (null != mapperPackages) {
                String[] stringArray = StringUtils.commaDelimitedListToStringArray(mapperPackages);
                for (String pkg : stringArray) {
                    joiner.add(pkg);
                }
            } else {
                List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
                for (String pkg : packages) {
                    joiner.add(pkg);
                }
            }
            builder.addPropertyValue("basePackage", joiner.toString());
            BeanWrapper beanWrapper = new BeanWrapperImpl(MapperScannerConfigurer.class);
            Stream.of(beanWrapper.getPropertyDescriptors())
                    .filter(x -> "lazyInitialization".equals(x.getName())).findAny()
                    .ifPresent(x -> builder.addPropertyValue("lazyInitialization",
                            "${mybatis.lazyInitialization:false}"));
            registry.registerBeanDefinition(MapperScannerConfigurer.class.getName(), builder.getBeanDefinition());
        }
    }

    @Configuration
    @Import(AutoConfiguredMapperScannerRegistrar.class)
    @ConditionalOnMissingBean({MapperFactoryBean.class, MapperScannerConfigurer.class})
    public static class MapperScannerRegistrarNotFoundConfiguration {

    }
}
```

到此基本的配置`Bean`已经定义完毕，接着需要添加配置项。一般一个项目的`MyBatis`配置是相对固定的，可以直接添加在主配置文件`application.properties`中：

```properties
server.port=9098
spring.application.name=ch8-mybatis
mybatis.configLocation=mybatis-config.xml
mybatis.mapperLocations=classpath:mappings/base,classpath:mappings/ext
mybatis.mapperPackages=club.throwable.ch8.repository.mapper,club.throwable.ch8.repository
```

个人喜欢在`resource/mappings`目录下定义`base`和`ext`两个目录，`base`目录用于存在`MyBatis`生成器生成的`XML`文件，这样就能在后续添加了表字段之后直接重新生成和覆盖`base`目录下对应的`XML`文件即可。同理，在项目的源码包下建`repository/mapper`，然后`Mapper`类直接存放在`repository/mapper`目录，`DAO`类存放在`repository`目录，`MyBatis`生成器生成的`Mapper`类可以直接覆盖`repository/mapper`目录中对应的类。

`resources`目录下添加一个`MyBatis`的全局配置文件`mybatis-config.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN" "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <!--下划线转驼峰-->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <!--未知列映射忽略-->
        <setting name="autoMappingUnknownColumnBehavior" value="NONE"/>
    </settings>
</configuration>
```

项目目前的基本结构如下：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/sp-g-ch8-1.png)

## 使用Mybatis

为了简单起见，这里使用`h2`内存数据库进行演示。添加`h2`的依赖：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>1.4.200</version>
</dependency>
```

`resources`目录下添加一个`schema.sql`和`data.sql`：

```sql
// resources/schema.sql
drop table if exists customer;

create table customer
(
    id            bigint generated by default as identity,
    customer_name varchar(32),
    age           int,
    create_time   timestamp default current_timestamp,
    edit_time     timestamp default current_timestamp,
    primary key (id)
);

// resources/data.sql
INSERT INTO customer(customer_name,age) VALUES ('doge', 22);
INSERT INTO customer(customer_name,age) VALUES ('throwable', 23);
```

添加对应的实体类`club.throwable.ch8.entity.Customer`：

```java
@Data
public class Customer {

    private Long id;
    private String customerName;
    private Integer age;
    private LocalDateTime createTime;
    private LocalDateTime editTime;
}
```

添加`Mapper`和`DAO`类：

```java
// club.throwable.ch8.repository.mapper.CustomerMapper
public interface CustomerMapper {

}

// club.throwable.ch8.repository.CustomerDao
public interface CustomerDao extends CustomerMapper {

    Customer queryByName(@Param("customerName") String customerName);
}
```

添加`XML`文件`resource/mappings/base/BaseCustomerMapper.xml`和`resource/mappings/base/ExtCustomerMapper.xml`：

```xml
// BaseCustomerMapper.xml

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="club.throwable.ch8.repository.mapper.CustomerMapper">

    <resultMap id="BaseResultMap" type="club.throwable.ch8.entity.Customer">
        <id column="id" jdbcType="BIGINT" property="id"/>
        <result column="customer_name" jdbcType="VARCHAR" property="customerName"/>
        <result column="age" jdbcType="INTEGER" property="age"/>
        <result column="create_time" jdbcType="TIMESTAMP" property="createTime"/>
        <result column="edit_time" jdbcType="TIMESTAMP" property="editTime"/>
    </resultMap>

</mapper>

// ExtCustomerMapper.xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="club.throwable.ch8.repository.CustomerDao">

    <resultMap id="BaseResultMap" type="club.throwable.ch8.entity.Customer"
               extends="club.throwable.ch8.repository.mapper.CustomerMapper.BaseResultMap">
    </resultMap>

    <select id="queryByName" resultMap="BaseResultMap">
        SELECT *
        FROM customer
        WHERE customer_name = #{customerName}
    </select>

</mapper>
```

> 细心的伙伴会发现，DAO和Mapper类是继承关系，而ext和base下对应的Mapper文件中的BaseResultMap也是继承关系

配置文件中增加`h2`数据源的配置：

```properties
// application.properties
spring.datasource.url=jdbc:h2:mem:db_customer;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=true
spring.datasource.schema=classpath:schema.sql
spring.datasource.data=classpath:data.sql
```

添加一个启动类进行验证：

```java
public class Ch8Application implements CommandLineRunner {

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(Ch8Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Customer customer = customerDao.queryByName("doge");
        log.info("Query [name=doge],result:{}", objectMapper.writeValueAsString(customer));
        customer = customerDao.queryByName("throwable");
        log.info("Query [name=throwable],result:{}", objectMapper.writeValueAsString(customer));
    }
}
```

执行结果如下：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/sp-g-ch8-2.png)

## 使用Mybatis生成器生成Mapper文件

有些时候为了提高开发效率，更倾向于使用生成器去预生成一些已经具备简单`CRUD`方法的`Mapper`文件，这个时候可以使用`mybatis-generator-core`。编写本文的时候（`2020-07-18`）`mybatis-generator-core`的最新版本为`1.4.0`，`mybatis-generator-core`可以通过编程式使用或者`Maven`插件形式使用。

这里仅仅简单演示一下`Maven`插件形式下使用`mybatis-generator-core`的方式，关于`mybatis-generator-core`后面会有一篇数万字的文章详细介绍此生成器的使用方式和配置项的细节。在项目的`resources`目录下添加一个`generatorConfig.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">
<generatorConfiguration>
    <context id="H2Tables" targetRuntime="MyBatis3">
        <property name="autoDelimitKeywords" value="true"/>
        <property name="javaFileEncoding" value="UTF-8"/>
        <property name="beginningDelimiter" value="`"/>
        <property name="endingDelimiter" value="`"/>

        <commentGenerator>
            <property name="suppressDate" value="true"/>
            <!-- 是否去除自动生成的注释 true：是 ： false:否 -->
            <property name="suppressAllComments" value="true"/>
            <property name="suppressDate" value="true"/>
        </commentGenerator>

        <jdbcConnection driverClass="org.h2.Driver"
                        connectionURL="jdbc:h2:mem:db_customer;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"
                        userId="root"
                        password="123456"/>

        <javaTypeResolver>
            <property name="forceBigDecimals" value="false"/>
        </javaTypeResolver>

        <!-- 生成模型的包名和位置(实体类)-->
        <javaModelGenerator targetPackage="club.throwable.ch8.entity"
                            targetProject="src/main/java">
            <property name="enableSubPackages" value="true"/>
            <property name="trimStrings" value="false"/>

        </javaModelGenerator>

        <!-- 生成映射XML文件的包名和位置-->
        <sqlMapGenerator targetPackage="mappings.base"
                         targetProject="src/main/resources">
            <property name="enableSubPackages" value="true"/>
        </sqlMapGenerator>

        <!-- 生成DAO的包名和位置-->
        <javaClientGenerator type="XMLMAPPER"
                             targetPackage="club.throwable.ch8.repository.mapper"
                             targetProject="src/main/java">
            <property name="enableSubPackages" value="true"/>
        </javaClientGenerator>

        <table tableName="customer"
               enableCountByExample="false"
               enableUpdateByExample="false"
               enableDeleteByExample="false"
               enableSelectByExample="false">
        </table>

    </context>
</generatorConfiguration>
```

然后再项目的`POM`文件添加一个`Maven`插件：

```xml
<plugins>
    <plugin>
        <groupId>org.mybatis.generator</groupId>
        <artifactId>mybatis-generator-maven-plugin</artifactId>
        <version>1.4.0</version>
        <executions>
            <execution>
                <id>Generate MyBatis Artifacts</id>
                <goals>
                    <goal>generate</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <jdbcURL>jdbc:h2:mem:db_customer;MODE=MYSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE</jdbcURL>
            <jdbcDriver>org.h2.Driver</jdbcDriver>
            <jdbcUserId>root</jdbcUserId>
            <jdbcPassword>123456</jdbcPassword>
            <sqlScript>${basedir}/src/main/resources/schema.sql</sqlScript>
        </configuration>
        <dependencies>
            <dependency>
                <groupId>com.h2database</groupId>
                <artifactId>h2</artifactId>
                <version>1.4.200</version>
            </dependency>
        </dependencies>
    </plugin>
</plugins>
```

> 笔者发现这里必须要在插件的配置中重新定义数据库连接属性和schema.sql，因为插件跑的时候无法使用项目中已经启动的h2实例，具体原因未知。

配置完毕之后，执行`Maven`命令：

```shell
mvn -Dmybatis.generator.overwrite=true mybatis-generator:generate -X
```

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/sp-g-ch8-3.png)

然后`resource/mappings/base`目录下新增了一个带有基本`CRUD`方法实现的`CustomerMapper.xml`，同时`CustoemrMapper`接口和`Customer`实体也被重新覆盖生成了。

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/sp-g-ch8-4.png)

> 这里把前面手动编写的BaseCustomerMapper.xml注释掉，预防冲突。另外，CustomerMapper.xml的insertSelective标签需要加上keyColumn="id" keyProperty="id" useGeneratedKeys="true"属性，用于实体insert后的主键回写。

最后，修改并重启启动一下`Ch8Application`验证结果：

![](https://throwable-blog-1256189093.cos.ap-guangzhou.myqcloud.com/202007/sp-g-ch8-5.png)

## 小结

这篇文章相对详细地介绍了`SpringBoot`项目如何使用`MyBatis`，如果需要连接`MySQL`或者其他数据库，只需要修改数据源配置和`MyBatis`生成器的配置文件即可，其他配置类和项目骨架可以直接复用。

本文`demo`仓库：

- `Github`：https://github.com/zjcscut/spring-boot-guide/tree/master/ch8-mybatis

（本文完 c-2-d e-a-20200719 封面来自《秒速五厘米》）