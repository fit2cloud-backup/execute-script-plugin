FIT2CLOUD Execute-Script-Plugin for Jenkins
====================

Jenkins是当前最常用的CI服务器，FIT2CLOUD Execute-Script-Plugin for Jenkins的功能是：代码在Jenkins服务器构建通过之后，可以在FIT2CLOUD中在指定范围的虚机上执行指定的脚本，这样可以满足一种轻量级的部署需求，或者实现一些其他需求。
一、安装说明
-------------------------

插件下载地址：http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/org/jenkins-ci/plugins/f2c-execute-script/0.1/f2c-execute-script-0.1.hpi
在Jenkins中安装插件, 请到 Manage Jenkins | Advanced | Upload，上传插件(.hpi文件)
安装完毕后请重新启动Jenkins


二、Post-build actions: FIT2CLOUD
-------------------------


#####FIT2CLOUD账号设置:   
1. FIT2CLOUD的Consumer Key   
2. FIT2CLOUD的Secret Key   
3. FIT2CLOUD的API EndPoint
所需的信息可以在FIT2LCOUD控制台用户的API信息中查看。


#####执行脚本设置信息:
1. 目标集群名称 (执行脚本的目标集群的名字，请在 FIT2CLOUD控制台中 > 集群 查看列表中对应集群的名称。)
2. 目标虚机组名称 (执行脚本的目标虚机组的名称，请在 FIT2CLOUD控制台中 > 虚机组 查看列表中对应虚机组的名称。如果不填写，则默认在集群下的全部虚机组执行脚本。)
3. 目标虚机Id (执行脚本的目标虚机的ID，请在 FIT2CLOUD控制台中 > 虚机组 查看列表中对应虚机组的ID。如果不填写，则默认在虚机组下的全部虚机执行脚本。)
4. 执行策略 (选择你希望使用的执行脚本策略，`全部同时执行`会一次性发送脚本执行请求到全部目标虚机，`单台依次执行`会逐台的执行脚本，如果这个过程中出现错误，会停止后续的脚本执行。)





三、插件开发说明
-------------------------

1. git clone git@github.com:fit2cloud/execute-script-plugin.git
2. mvn -Declipse.workspace=execute-script-plugin eclipse:eclipse eclipse:add-maven-repo
3. import project to eclipse
4. mvn hpi:run -Djetty.port=8090 -Pjenkins 进行本地调试
5. mvn package 打包生成hpi文件


四、历史版本说明
-------------------------
1. V0.1:  
FIT2CLOUD执行脚本插件第一个版本，完成执行脚本所需的功能。
如需使用该版本插件，可以从此下载：[http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/org/jenkins-ci/plugins/f2c-execute-script/0.1/f2c-execute-script-0.1.hpi](http://repository-proxy.fit2cloud.com:8080/service/local/repositories/releases/content/org/jenkins-ci/plugins/f2c-execute-script/0.1/f2c-execute-script-0.1.hpi)


如果有问题，请联系bohan@fit2cloud.com
