package com.example.wuziqi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ServiceWorkerWebSettings;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wuziqi.playactivity.HumenToAiActivity;
import com.example.wuziqi.playactivity.HumenWebActivity;
import com.example.wuziqi.playactivity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import AI.ConstanNum;
import cn.pedant.SweetAlert.SweetAlertDialog;
import service.MyService;
import java.util.*;

import static com.example.wuziqi.R.layout.activity_hall;

public class HallActivity extends AppCompatActivity {

    private BroadcastReceiver dataReceiver;
    private MyService myService;
    private List<HashMap<String,Object>> listdata;
    private ListView listview;
    private int nowchoose;
    private SimpleAdapter adapter;
    private String comName;
    public static String myName;
    private Button binvite;
    SweetAlertDialog regretDialog,receiveDialog;
    boolean asking;
    //是否绑定成功
    boolean bound;
    private ServiceConnection sc=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.LocalBinder binder= (MyService.LocalBinder) service;
            myService=binder.getService();
            bound=true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound=false;
        }
    };

    //异步更新线程
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //通过arg1来判断消息内容
            switch (msg.arg1){
                //1为当前名称可以使用。
                case 1:
                    receiveDialog.dismiss();
                   break;
                case 2:
                    if(regretDialog.isShowing()){
                        asking=false;
                        regretDialog.hide();
                        new SweetAlertDialog(HallActivity.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("拒绝邀请")
                                .setContentText("对方不在线啊")
                                .showCancelButton(true)
                                .show();
                    }
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        //绑定服务
        Intent intent=new Intent(HallActivity.this,MyService.class);
        bindService(intent,sc,BIND_AUTO_CREATE);

        regretDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        regretDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        regretDialog.setTitleText("等待对方响应(8s)");
        regretDialog.setCancelable(false);

        setContentView(R.layout.activity_hall);
        listview=(ListView)findViewById(R.id.listview);
        listdata=new ArrayList<HashMap<String, Object>>();
        //适配器设置
        adapter=new SimpleAdapter(this,
                listdata,
                R.layout.activity_hall_listitem,
                new String[]{"选中","名字","状态"},
                new int[]{R.id.chosen,R.id.name,R.id.state}){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                View view = super.getView(position, convertView, parent);
                TextView text=view.findViewById(R.id.state);
                if(listdata.get(position).get("状态").equals("游戏中"))
                {
                    text.setBackgroundColor(Color.rgb(243,8,21));
                }
                else
                {
                    text.setBackgroundColor(Color.rgb(15,249,27));
                }
                return view;
            }
        };
        nowchoose=-1;
        listview.setAdapter(adapter);
        //邀请按钮
        binvite=(Button)findViewById(R.id.button2);
        binvite.setEnabled(false);
        binvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                asking=true;
                regretDialog.show();
                invite((String)listdata.get(nowchoose).get("名字"));
                binvite.setEnabled(false);
                listview.setEnabled(false);
                //8s后返回无人应答
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(7000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(regretDialog.isShowing()){
                                    Message m=new Message();
                                    m.arg1=2;
                                    handler.sendMessage(m);
                                }
                            }
                        }
                ).start();
            }
        });
        Button bback=(Button)findViewById(R.id.button);
        bback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //退出登录
                myService.close();
                //解除绑定
                HallActivity.this.unbindService(sc);
                MainActivity.ifLog=false;
                //关闭此页面
                finish();
                MainActivity.actionStart(HallActivity.this);
            }
        });
        Button fresh=(Button)findViewById(R.id.button3);
        fresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               myService.refreshList();
            }
        });
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(nowchoose>=0) {
                    listdata.get(nowchoose).put("选中", false);
                }
                nowchoose=position;
                listdata.get(nowchoose).put("选中",true);
                adapter.notifyDataSetChanged();
                if(Objects.requireNonNull(listdata.get(nowchoose).get("状态")).equals("游戏中")||Objects.requireNonNull(listdata.get(nowchoose).get("名字")).equals(myName))
                {
                    binvite.setEnabled(false);
                }
                else
                {
                    binvite.setEnabled(true);
                }
            }
        });

        //如果myServie为空，则新建线程来等待开启通信
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(myService!=null){
                        myService.startSocket(myName);
                        break;
                    }
                    else {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        if(myService!=null)
        myService.refreshList();
        super.onResume();
        //注册接收器
        dataReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra("flag",0)){
                    case 211:
                        refreshListView(intent.<Player>getParcelableArrayListExtra("liebiaoData"));
                        break;
                    case 212:
                        if(!asking)
                        receive(intent.getStringExtra("comName"));
                        break;
                    case 213:
                        regretDialog.hide();
                        asking=false;
                        beFeedBack(intent.getBooleanExtra("ifAgree",false));
                        break;
                    case 216:
                        System.out.println("大厅接收到信息？");
                        break;
                }
            }
        };
        IntentFilter filter=new IntentFilter();
        filter.addAction("hallBroad");
        registerReceiver(dataReceiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(dataReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //如果按了返回键
        if(keyCode==KeyEvent.KEYCODE_BACK){
            MainActivity.actionStart(this);
            return false;
        }
        else
            return super.onKeyUp(keyCode, event);
    }
    public static void actionStart(Context context){
        Intent intent=new Intent(context,HallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    //显示arraylist中的数据，在此之前页面有正在刷新的提示
    public void refreshListView(List<Player> players)
    {
        int i=0;
        Player thisplayer;
        while(i<players.size())
        {
            i++;
        }
        i=0;
        listdata.clear();
        while(i<players.size())
        {
            HashMap<String,Object> datamap=new HashMap<String,Object>();
            thisplayer=players.get(i);
            if(i==nowchoose)
            {
                datamap.put("选中",true);
            }
            else
            {
                datamap.put("选中",false);
            }
            datamap.put("名字",thisplayer.getName());
            if(thisplayer.isPlaying())
            {
                datamap.put("状态","游戏中");
            }
            else
                {
                datamap.put("状态","空闲");

            }
            listdata.add(datamap);

            i++;
        }
        adapter.notifyDataSetChanged();
    }
    //接收到邀请信息,则需要弹出一个全局对话框，
    public void receive(final String name)
    {
        comName=name;
         receiveDialog=new SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                .setTitleText("对弈邀请")
                .setContentText("玩家"+name+"邀请您进行对弈!(5s内回应)")
                .setCancelText("拒绝")
                .setConfirmText("接受")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        feedBack(true,name);
                        sDialog.dismiss();
                    }
                })
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        feedBack(false,name);
                        sDialog.dismiss();
                    }
                });
         receiveDialog.show();
         new Thread(
                 new Runnable() {
                     @Override
                     public void run() {
                         try {
                             Thread.sleep(5000);
                         } catch (InterruptedException e) {
                             e.printStackTrace();
                         }
                         Message m=new Message();
                         m.arg1=1;
                         handler.sendMessage(m);
                     }
                 }
         ).start();
    }
    //接收到对面的回应,因为一次只能发送一个信息，所以不需要对手的名字。

    public void beFeedBack(boolean ifAgree){
        //对面同意了你的邀请,打开窗口，等待服务器的开始命令
        if(ifAgree){
            HumenWebActivity.actionStart(this);
        }
        //对方拒绝了,做出提示
        else{
            new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText("拒绝邀请")
                    .setContentText("你的对弈邀请被对方拒绝!")
            .showCancelButton(true)
            .show();
        }
        binvite.setEnabled(true);
        listview.setEnabled(true);
    }

    //发送同意或者不同意别人的邀请的信息,comName为对手名字
    private void feedBack(boolean ifAgree,String comName){
        myService.feedBack(ifAgree,comName);
        //如果同意打开游戏界面
        if(ifAgree){
            HumenWebActivity.actionStart(this);
        }
    }
    //发送邀请信息
    private void invite(String name){
        myService.invite(name);
    }
}

