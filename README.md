# gobang
android五子棋游戏gobang，联机对战，人机算法

后台使用springboot+webservice实现。

程序实例：

![Image text](https://github.com/qihe777/gobang/blob/master/1558937047808.gif)



目前联机对战的服务器已经下线，因为正在开发其他的项目，懒得改端口了，直接下线了。

此程序仍然存在bug：
1.人赢了之后，机器仍然会下棋
2.大厅界面，以及游戏界面不能切屏，否则会接收到的数据无法显示。
3.

需要做的改进：
1.接受邀请应该设置为全局对话框；
2.把服务器发来的消息作为临时变量存储起来，以便切屏之后仍然可以显示接收到的消息。
3.完善计时功能，以及
