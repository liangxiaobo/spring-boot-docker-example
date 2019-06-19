  Docker是一个开源的引擎，可以轻松的为任何应用创建一个轻量级的、可移植的、自给自足的容器。开发者在笔记本上编译测试通过的容器可以批量地在生产环境中部署，包括VMs（虚拟机）、bare metal、OpenStack 集群和其他的基础应用平台。

# docker 环境准备
[https://docs.docker.com/installation/#installation](https://docs.docker.com/installation/#installation)
# 打包方式一
简单的创建一个Dockerfile，放在项目根目录
```bash
FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```
执行mvn命令
```bash
mvn install 或 mvn package
# 打包docker image
docker build -t myorg/myapp .
```
# 打包方式二
这种方式其实和上面的方式一样都是把target目录下生成的jar打包进镜像，不过需要插件帮助完成
```xml
<properties>
        <docker.image.prefix>springio</docker.image.prefix>
        <dockerfile.plugin.verion>1.4.9</dockerfile.plugin.verion>
    </properties>
 <build>
        <plugins>

            <!-- tag::plugin [] -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <!--end::plugin [] -->

            <!-- tag::dockerfile [] -->
            <plugin>
                <groupId>com.spotify</groupId>
                <artifactId>dockerfile-maven-plugin</artifactId>
                <version>${dockerfile.plugin.verion}</version>
                <configuration>
                    <repository>${docker.image.prefix}/${project.artifactId}</repository>
                    <!--<tag>${project.version}</tag>-->
                    <tag>latest</tag>
                    <buildArgs>
                        <JAR_FILE>target/${project.build.finalName}.jar</JAR_FILE>
                    </buildArgs>
                </configuration>
            </plugin>
            <!-- end::dockerfile [] -->
        </plugins>
    </build>
```
**Dockerfile**
```bash
FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
```
**dockerfile插件：** [说明](https://github.com/spotify/dockerfile-maven)
* **repository** 
指的是你的镜像仓库地址，如果你是私有库一般是 ```ip:端口/imageName:tag```,如果是公共库如[docker hub](https://hub.docker.com) 这 ```用户名/imageName:tag```，假如只是本地测试不用```docker push```，这里可以随便写符合规则就行

* **tag**
设置镜像的tag属性，不设置的话默认就是最新 ***latest***

* **buildArgs**
指的就是Dockerfile中的ARG 参数，这里JAR_FILE参数指的就是Dockerfile中的JAR_FILE
具体的[ARG参数可以看这里](https://docs.docker.com/engine/reference/builder/#understand-how-arg-and-from-interact)

## 运行打包测试
进入项目目录
```bash
cd docker-example-copy-jar
mvn install dockerfile:build
```
打包成功后可以查看本地镜像 ```docker images```
```bash
LiangdeMacBook-Pro:docker-example-copy-jar liangbo$ docker images
REPOSITORY                                 TAG                 IMAGE ID            CREATED             SIZE
springio/docker-example-copy-jar           latest              d6b3d432c014        42 seconds ago      122MB
openjdk                                    8-jdk-alpine        a3562aa0b991        5 weeks ago         105MB
```
运行容器
```docker run -ti -p 8080:8080 --entrypoint /bin/sh springio/docker-example-copy-jar```
```bash
LiangdeMacBook-Pro:docker-example-copy-jar liangbo$ docker run -ti -p 8080:8080 --entrypoint /bin/sh springio/docker-example-copy-jar
/ # ls
app.jar  bin      dev      etc      home     lib      media    mnt      opt      proc     root     run      sbin     srv      sys      tmp      usr      var
```
这种启动方式只适合测试，当你exit退出容器时，容器也停止运行

另一种方式
```bash
LiangdeMacBook-Pro:docker-example-copy-jar liangbo$ docker run -d --name app -p 8080:8080 springio/docker-example-copy-jar
819c4cd863857c90031ffded0681d90cc1b63c96c8b55d1e00f4cd9652dfc4be
LiangdeMacBook-Pro:docker-example-copy-jar liangbo$ docker ps
CONTAINER ID        IMAGE                              COMMAND                  CREATED             STATUS              PORTS                    NAMES
819c4cd86385        springio/docker-example-copy-jar   "java -Djava.securit…"   2 seconds ago       Up 1 second         0.0.0.0:8080->8080/tcp   app
LiangdeMacBook-Pro:docker-example-copy-jar liangbo$ docker exec -it  app ls
app.jar  etc      media    proc     sbin     tmp
bin      home     mnt      root     srv      usr
dev      lib      opt      run      sys      var
```
访问 http://localhost:8080/hello

# 打包方式三
先将jar解压后进接打包进镜像，这样容器启动会更快一些

**需要解压插件**
```xml
<!-- tag::unpack [] -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>unpack</id>
                        <phase>package</phase>
                        <goals>
                            <goal>unpack</goal>
                        </goals>
                        <configuration>
                            <artifactItems>
                                <artifactItem>
                                    <groupId>${project.groupId}</groupId>
                                    <artifactId>${project.artifactId}</artifactId>
                                    <version>${project.version}</version>
                                </artifactItem>
                            </artifactItems>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- end::unpack [] -->
```
完成的pom
```xml
<properties>
        <docker.image.prefix>springio</docker.image.prefix>
        <dockerfile.plugin.verion>1.4.9</dockerfile.plugin.verion>
    </properties>
<build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- tag::plugin [] -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <!--end::plugin [] -->

            <!-- tag::dockerfile [] -->
            <plugin>
                <groupId>com.spotify</groupId>
                <artifactId>dockerfile-maven-plugin</artifactId>
                <version>${dockerfile.plugin.verion}</version>
                <configuration>
                    <repository>${docker.image.prefix}/${project.artifactId}</repository>
                    <!--<tag>${project.version}</tag>-->
                    <tag>latest</tag>
                </configuration>
            </plugin>
            <!-- end::dockerfile [] -->

            <!-- tag::unpack [] -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>unpack</id>
                        <phase>package</phase>
                        <goals>
                            <goal>unpack</goal>
                        </goals>
                        <configuration>
                            <artifactItems>
                                <artifactItem>
                                    <groupId>${project.groupId}</groupId>
                                    <artifactId>${project.artifactId}</artifactId>
                                    <version>${project.version}</version>
                                </artifactItem>
                            </artifactItems>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- end::unpack [] -->

        </plugins>
    </build>
```
**Dockerfile**
```bash
FROM openjdk:8-jdk-alpine
VOLUME /tmp
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","com.example.docker.dockerexamplecopysource.DockerExampleCopySourceApplication"]

```
> 当解压插件执行完成后，target目录下会有一个dependency目录
> .
├── BOOT-INF
│   ├── classes
│   └── lib
├── META-INF
│   ├── MANIFEST.MF
│   └── maven
└── org
    └── springframework
> ```ENTRYPOINT ``` 最后一个参数是启动类

## 打包测试
和上面一样
```bash
cd docker-example-copy-source
mvn install dockerfile:build
```
打包成功后可以在本地查看镜像 ```docker images```
```bash
LiangdeMacBook-Pro:docker-example-copy-source liangbo$ docker images
REPOSITORY                            TAG                 IMAGE ID            CREATED              SIZE
springio/docker-example-copy-source   latest              6dc944ee8069        About a minute ago   121MB
springio/docker-example-copy-jar      latest              d6b3d432c014        40 minutes ago       122MB
openjdk                               8-jdk-alpine        a3562aa0b991        5 weeks ago          105MB
```
**运行容器**
```bash
LiangdeMacBook-Pro:docker-example-copy-source liangbo$ docker run -d --name app2 -p 8081:8080 springio/docker-example-copy-source
faa9e4cf2399b61b315d7dc4d226b599a4b1aeaa58bc33c02f524a557fcfaca0
LiangdeMacBook-Pro:docker-example-copy-source liangbo$ docker ps
CONTAINER ID        IMAGE                                 COMMAND                  CREATED             STATUS              PORTS                    NAMES
faa9e4cf2399        springio/docker-example-copy-source   "java -cp app:app/li…"   2 seconds ago       Up 1 second         0.0.0.0:8081->8080/tcp   app2
```
可访问 http://localhost:8081/hello

最后查看一下容器内的文件
```bash
LiangdeMacBook-Pro:docker-example-copy-source liangbo$ docker exec -it app2 /bin/sh 
/ # ls
app    bin    dev    etc    home   lib    media  mnt    opt    proc   root   run    sbin   srv    sys    tmp    usr    var
/ # ls app
META-INF                application.properties  com                     lib

```
