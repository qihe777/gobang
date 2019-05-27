package service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.wuziqi.HallActivity;
import com.example.wuziqi.playactivity.HumenWebActivity;
import com.example.wuziqi.playactivity.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import AI.ConstanNum;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MyService extends Service {
    private OkHttpClient mOkHttpClient;
    private WebSocket client;
    private HallActivity hallActivity;
    private HumenWebActivity humenWebActivity;
    private final IBinder binder=new LocalBinder();
    private String competitorName,myName;
    private boolean isFirst,come=false;
//    long lasttime,nowtimr;
    public void setHallActivity(HallActivity hallActivity){
        this.hallActivity=hallActivity;
    }

    public void setHumenWebActivity(HumenWebActivity humenWebActivity) {
        this.humenWebActivity = humenWebActivity;
    }
    public void setCompetitorName(String competitorName){
        this.competitorName=competitorName;
    }

    //用于返回当前后台对象
    public class LocalBinder extends Binder {
        public MyService getService(){
            return MyService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //当另一个组件通过调用bindService()与服务绑定时，系统将调用此方法。
    //所以绑定的是否就调用这个方法。
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public void startSocket(final String name){
        myName=name;
        mOkHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("ws://"+ConstanNum.baseUrl+"/websocket/"+ name+"/")
                .build();

        WebSocketListener listener=new WebSocketListener(){
            public void onOpen(WebSocket webSocket, Response response) {
                client=webSocket;
               /* //开始发送心跳包20s发送一次
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true){
                            try {
                                Thread.sleep(20000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                            Map<String, String> map = new HashMap<String,String>();
                            map.put("num","401");
                            client.send(JSON.toJSONString(map));
                        }
                    }
                }).start();*/
            }
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println(text);
                handleMessage(text);
            }
            public void onClosing(WebSocket webSocket, int code, String reason) {
                if(null!=client){
                    client.close(1000,"再见");
                    client=null;
                }
                System.out.println("正在关闭"+reason);
            }
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("已经关闭"+reason);
            }
            public void onFailure(WebSocket webSocket, Throwable t,Response response) {
                System.out.println("出了问题"+t.getMessage());
            }
        };
        mOkHttpClient.newWebSocket(request,listener);
        mOkHttpClient.dispatcher().executorService().shutdown();

    }

    //处理服务器发来的消息，并调用activity中的函数
    private void handleMessage(String message){
        final JSONObject jsonObject= JSON.parseObject(message);
        //根据不同的序号序号，来处理不同的事件
        int x=jsonObject.getInteger("num");
        final Intent intent=new Intent();
        switch (x){
            //接收到刷新页面的请求,将返回的字符串转化为arraylist
            case 211:
                intent.putExtra("flag",211);
                ArrayList<Player> play=(ArrayList<Player>) JSONObject.parseArray(
                        jsonObject.getString("players"), Player.class);
                intent.putParcelableArrayListExtra("liebiaoData",play);
                intent.setAction("hallBroad");
                sendBroadcast(intent);
                break;
                //hallActivity.refreshListView(play);
            //接受到邀请信息
            case 212:
                //需要打开hallactivity，暂时不加在后台的逻辑
                intent.putExtra("flag",212);
                intent.putExtra("comName",jsonObject.getString("comName"));
                intent.setAction("hallBroad");
                sendBroadcast(intent);
                break;
            case 213:
                intent.putExtra("flag",213);
                intent.putExtra("ifAgree",jsonObject.getBooleanValue("ifAgree"));
                intent.setAction("hallBroad");
                sendBroadcast(intent);
                break;
            //服务器发来的开始游戏以及棋色信号
            case 216:
                //发送开局消息，需要等待service绑定完成
                come=true;
                isFirst=jsonObject.getBoolean("isFirst");
                competitorName= jsonObject.getString("name");
                break;
                //服务器发来的对面下棋的点
            case 311:
                intent.putExtra("flag",311);
                intent.putExtra("x",jsonObject.getIntValue("x"));
                intent.putExtra("y",jsonObject.getIntValue("y"));
                intent.putExtra("minute",jsonObject.getIntValue("minute"));
                intent.setAction("webBroad");
                sendBroadcast(intent);
                break;
                //申请悔棋
            case 312:
                intent.putExtra("flag",312);
                intent.setAction("webBroad");
                sendBroadcast(intent);
                break;
                //对方同意或者拒绝悔棋。
            case 313:
                intent.putExtra("flag",313);
                intent.putExtra("regret",jsonObject.getBoolean("regret"));
                intent.setAction("webBroad");
                sendBroadcast(intent);
                break;
            //对方时间未响应，则判负
            case 314:
                intent.putExtra("flag",314);
                intent.setAction("webBroad");
                sendBroadcast(intent);
                break;
            case 316:
                intent.putExtra("flag",314);
                intent.setAction("webBroad");
                sendBroadcast(intent);
                break;
        }
    }
    //下面都是处理要发送给服务器的数据

    public void getStartInfo(){
        //等待接收服务器发送来的开局信息
        while(true){
            if(!come){
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else{
                Intent intent=new Intent();
                intent.putExtra("flag",216);
                intent.putExtra("comName",competitorName);
                intent.putExtra("myName",myName);
                intent.putExtra("isFirst",isFirst);
                intent.setAction("webBroad");
                sendBroadcast(intent);
                come=false;
                break;
            }
        }
    }

    //发送邀请信息给服务器,，202消息
    public void invite(String name){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","202");
        map.put("comName",name);
        client.send(JSON.toJSONString(map));
    }
    //给服务器发送是否接受邀请，203消息
    public void feedBack(boolean ifAgree,String comName){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","203");
        map.put("ifAgree",String.valueOf(ifAgree));
        map.put("comName",comName);
        client.send(JSON.toJSONString(map));
    }
    //把当前所下点位发送给服务器,301消息
    public void sendPoint(final int x, final int y, final int minute){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","301");
        map.put("x",Integer.toString(x));
        map.put("y",Integer.toString(y));
        map.put("minute",Integer.toString(minute));
        client.send(JSON.toJSONString(map));
    }
    //发送给服务器申请悔棋,302消息
    public void askRegret(){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","302");
        client.send(JSON.toJSONString(map));
    }
    //发送自己是否同意对方悔棋，303消息
    public void ifRegret(Boolean ifregret){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","303");
        map.put("regret",String.valueOf(ifregret));
        client.send(JSON.toJSONString(map));
    }
    //游戏结束后发送消息给服务器，305消息
    public void end(){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","305");
        client.send(JSON.toJSONString(map));
    }
    public void wantLose(){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","306");
        client.send(JSON.toJSONString(map));
    }
    //关闭服务,不用发送消息，因为有回调函数
    public void close(){
        client.close(1000,"guanbi");
        stopSelf();
    }
    public void refreshList(){
        Map<String, String> map = new HashMap<String,String>();
        map.put("num","101");
        client.send(JSON.toJSONString(map));
    }
}
