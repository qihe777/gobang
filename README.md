# gobang
android五子棋游戏gobang，联机对战，人机算法

其中有apk，可以下载尝试。

android端使用了sweetalert对话框库：https://github.com/F0RIS/sweet-alert-dialog

五子棋算法实现参考了（抄袭了）：https://github.com/lihongxun945/gobang

后台使用springboot+websocket实现。其中websocket使用的是okhttp的库

程序实例：


![Image text](https://github.com/qihe777/gobang/blob/master/1558937047808.gif)


目前联机对战的服务器已经下线，因为正在开发其他的项目，懒得改端口了，直接下线了。


此程序仍然存在bug：

1.人赢了之后，机器仍然会下棋

2.大厅界面，以及游戏界面不能切屏，否则会接收到的数据无法显示。


需要做的改进：

1.接受邀请应该设置为全局对话框；

2.把服务器发来的消息作为临时变量存储起来，以便切屏之后仍然可以显示接收到的消息。

3.完善计时功能，以及增加消息对话功能。
