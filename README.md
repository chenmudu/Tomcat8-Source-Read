# Tomcat8-Source-Read

[![standard-readme compliant](https://img.shields.io/github/downloads/chenmudu/Tomcat8-Source-Read/total?style=social)](https://github.com/chenmudu/Tomcat8-Source-Read)

> Tomcat8-Source-Read(源码解读)

当你遇到以下类似问题时，`Tomcat8-Source-Read`可以帮你：

0. 想系统的读一个开源软件的源码，但是不知道该读哪个开源项目。
0. 读源码英文水平有限，每次需要借助翻译软件的生涩翻译才能持续Debug读下去，但是翻译的时候又浪费了巨多时间。
0. 想去读Tomcat的代码，一部分原因是网上关于Tomcat源码大多为Tomcat5/6/7的源码。另一部分因为代码结构复杂，不知道如何下手。
0. 从Tomcat官网下载了对应的代码，但是不知道如何基于Maven和IDEA去构建自己的源码。

`Tomcat8-Source-Read`基于Maven + IDEA，通过配置少量启动参数去构建。内置主要功能源码的中英翻译，持续更新个人的读源码感想。以及持续更新类结构图和关键逻辑的流程图。

## 快速开始

0.下载源码包

```sh
git clone -b branch-chenchen  git@github.com:chenmudu/Tomcat8-Source-Read.git
```
1.选择构建工具和调试工具

```sh
Maven + IDEA
```

2 点击Edit Configurations,选择Application构建项目。

```sh
Maven + IDEA
```
3 修改Main class参数值为(启动入口)：

```sh
org.apache.catalina.startup.Bootstrap
```

4 修改Vm options参数值为(调试和源码分开)：

```sh
-Dcatalina.home=catalina-home 
-Dcatalina.base=catalina-home 
-Djava.endorsed.dirs=catalina-home/endorsed 
-Djava.io.tmpdir=catalina-home/temp 
-Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager 
-Djava.util.logging.config.file=catalina-home/conf/logging.properties
```

5 Run Application：

```sh
Web Browser keys：localhost:8080
```
6 选择感兴趣的模块调试读源码即可。如果出现乱码，请点击
```sh
catalina-home/conf/logging.properties 文件内修改对应参数.
```

## 维护者

[@陈晨(chenchen6)](https://github.com/chenmudu).

## 如何贡献(中英翻译,源码解读感想)

非常欢迎你的加入! [提一个Issue](https://github.com/chenmudu/Tomcat8-Source-Read/issues/new) 或者提交一个 Pull Request.