# CSAgent

CobaltStrike 4.x通用白嫖及汉化加载器

采用javaagent+javassist的方式动态修改jar包，可直接加载原版cobaltstrike.jar，理论上支持到目前为止的所有4.x版本

**PS**：汉化原理部分代码白嫖于外面公开的汉化版本，非我原创

这可能是迄今为止最全面、最详细、最牛逼的汉化版本，主要体现在：
1. 汉化内容更详细，非机翻，所有文字是我一句句人工翻译的，不只简单的汉化了菜单，各类错误、说明信息都有汉化，尤其是用正则表达式覆盖了各类动态生成的错误信息
2. 汉化范围更全面，之前的各类汉化版都是没有完全汉化按钮的，因为这里涉及到java的一个坑，汉化后可能导致按钮功能失效，本版本对所有按钮全覆盖；
   另外，针对Beacon终端交互内的命令及命令帮助也都有详尽的汉化说明，部分命令还加上了我个人的说明见解
3. 汉化方式更先进，并非纯粹的正则替换，针对菜单、命令、命令帮助说明的汉化利用了Cobalt Strike加载资源文件的特性，直接翻译资源文件即可，无需再做动态替换，性能更高，后续版本更新也更方便
   针对界面的各类说明、标签汉化，全部写入配置文件中，后续版本只需修改这部分配置即可，无需再修改java代码

## 使用方法
1. 下载附件CSAgent.zip解压后放到cobaltstrike.jar同一个目录，确保CSAgent.jar、resources文件夹、scripts文件夹和cobaltstrike.jar处于同级目录

2. 从javassist官网[https://www.javassist.org/](https://www.javassist.org/)下载javassist，解压后将**javassist.jar**放到和CSAgent.jar同目录
   
   文件目录结构类似： 

   ![文件目录结构](/images/8tree.jpg?raw=true "文件目录结构")
   
3. 修改teamserver和cobaltstrike脚本，在java的命令行中加入一个参数：  
    `-javaagent:CSAgent.jar=3a4425490f389aeec312bdd758ad2b99`

    **3a4425490f389aeec312bdd758ad2b99**即4.3版本的Sleeved解密key，如果需要加载其他版本或自己修改过key，请修改为相应的解密key
    
    teamserver完整命令行为：
    
    >java -XX:ParallelGCThreads=4 -Dcobaltstrike.server_port=50050 -Dcobaltstrike.server_bindto=0.0.0.0 -Djavax.net.ssl.keyStore=./cobaltstrike.store -Djavax.net.ssl.keyStorePassword=123456 -server -XX:+AggressiveHeap -XX:+UseParallelGC -classpath cobaltstrike.jar -Duser.language=en -javaagent:CSAgent.jar=3a4425490f389aeec312bdd758ad2b99 server.TeamServer $*
    
    cobaltstrike完整命令行为：
    
    >java -XX:ParallelGCThreads=4 -XX:+AggressiveHeap -XX:+UseParallelGC -javaagent:CSAgent.jar=3a4425490f389aeec312bdd758ad2b99 -jar cobaltstrike.jar $*
    
4. 修改完成后即可正常使用teamserver和cobaltstrike脚本启动，用法与以前无任何差别

5. Windows下不能直接双击cobaltstrike.exe启动，否则无法加载CSAgent.jar导致没有汉化
   需要将java.exe目录添加到系统环境变量，然后在cmd中使用下面命令启动客户端：
   
   >java -XX:ParallelGCThreads=4 -XX:+AggressiveHeap -XX:+UseParallelGC -javaagent:CSAgent.jar=3a4425490f389aeec312bdd758ad2b99 -jar cobaltstrike.jar
   
6. 对于仅想使用破解功能的朋友，只需删除resources文件夹和scripts文件夹即可去除汉化

## 效果
主界面

![主界面](/images/1主界面.jpg?raw=true "主界面")

Console

![命令](/images/2命令.jpg?raw=true "命令")

命令帮助

![命令帮助](/images/3命令帮助.jpg?raw=true "命令帮助")

生成payload

![payload生成](/images/4payload生成.jpg?raw=true "payload生成")

监听器

![监听器](/images/5监听器.jpg?raw=true "监听器")

偏好设置

![偏好设置](/images/6偏好设置.jpg?raw=true "偏好设置")

版本信息

![版本](/images/7版本.jpg?raw=true "版本")