# Feign dev tool

This is a debug-tool for feign users.
You can use it to specify a consul server without changing the class with an FeignClient annotation.

## Install

```shell

git clone https://github.com/leejoker/feign-dev-tool-starter.git

cd feign-dev-tool-starter

mvn install

```

## How to use

1.add dependency in you pom.xml

```xml

<dependency>
    <groupId>io.github.leejoker</groupId>
    <artifactId>feign-dev-tool-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

```

2.config the application.yml

```yaml

# add feign-tool config
feign-tool:
  consul:
    host: 127.0.0.1
    port: 8500
    dataCenter: dc1
    excludes: user-service, push-service
  redirects:
    demo-service:
      url: http://localhost:8989/demo

```


3.add the package to your ComponentScan annotation

```java

@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"your package paths","io.github.leejoker"})
public class DemoApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder().sources(DemoApplication.class).run(args);
	}
}

```