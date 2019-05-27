package com.example.wuziqi;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.wuziqi.playactivity.AiToAiActivity;
import com.example.wuziqi.playactivity.HumenToAiActivity;
import com.example.wuziqi.playactivity.HumenWebActivity;
import com.suke.widget.SwitchButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import AI.ConstanNum;
import cn.pedant.SweetAlert.SweetAlertDialog;
import service.MyService;

public class MainActivity extends BaseActivity{

    //是否已经登录
    public static boolean ifLog;
    //背景音乐
    private MediaPlayer mp;//mediaPlayer对象
    SweetAlertDialog regretDialog;

    @Override
    protected void onPause() {
        super.onPause();
        mp.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mp.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //默认是开启背景音乐
        //BGM按钮
        SwitchButton switch_button = (SwitchButton) findViewById(R.id.switch_button);
        mp=MediaPlayer.create(MainActivity.this, R.raw.wuziqibgm);//重新设置要播放的音频
        //重复播放
        mp.setOnCompletionListener(new OnCompletionListener(){
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });
        mp.start();//开始播放
        switch_button.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked)
            {
                if (isChecked)
                {
                    try{
                        mp=MediaPlayer.create(MainActivity.this, R.raw.wuziqibgm);//重新设置要播放的音频
                        mp.start();//开始播放
                    }catch(Exception e){
                        e.printStackTrace();//输出异常信息
                    }
                }
                else
                    {
                        mp.stop();
                    }
            }
        });

        Button button1=(Button)findViewById(R.id.button_1);
        button1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
               AiToAiActivity.actionStart(MainActivity.this);
            }
        });

        Button button2=(Button)findViewById(R.id.button_2);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                HumenToAiActivity.actionStart(MainActivity.this);
            }
        });

        Button button3=(Button)findViewById(R.id.button_3);
        button3.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(!ifLog)
                {
                    login();
                }

                else
                {
                    HallActivity.actionStart(MainActivity.this);
                }
            }
        });

        ifLog=false;

    }
    //异步更新线程
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //通过arg1来判断消息内容
            switch (msg.arg1){
                //1为当前名称可以使用。
                case 1:
                    entranceHall((String)msg.obj);
                    break;
                //2.为当前名称不可以使用。
                case 2:

                    new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("用户名重复")
                            .setContentText("换个名字喽")
                            .showCancelButton(true)
                    .show();
                    break;
                case 5:
                    if(regretDialog!=null&&regretDialog.isShowing()){
                        regretDialog.dismiss();
                    }
            }
        }
    };
    private boolean rightName(String name){
        String regex="^[0-9a-zA-Z]+$";
        return name.matches(regex);
    }

    //若无登录，弹出对话框，要求输入用户名
    public void login(){
        final EditText et=new EditText(this);
        AlertDialog.Builder logindailog=new AlertDialog.Builder(MainActivity.this);
        logindailog.setTitle("请输入用户名（仅英文和数字）");
        logindailog.setIcon(R.drawable.icon);
        logindailog.setView(et);
        logindailog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Toast.makeText(getApplicationContext(),et.getText().toString(),Toast.LENGTH_LONG).show();
                String tempName=et.getText().toString();
                //如果tmpName含有非法字符
                if(!tempName.isEmpty()){
                    if(rightName(tempName)){
                        checkName(tempName);

                    }
                    else{
                        if(regretDialog!=null&&regretDialog.isShowing()){
                            regretDialog.dismiss();
                        }
                        new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("名字输错了")
                                .setContentText("只能字母和数字了啦( # ▽ # )")
                                .showCancelButton(true)
                                .show();
                    }

                }
                else
                {
                    if(regretDialog!=null&&regretDialog.isShowing()){
                        regretDialog.dismiss();
                    }
                    new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("输入个名字")
                            .setContentText("好不了喽")
                            .showCancelButton(true)
                    .show();
                }

            }
        }).setNegativeButton("取消",null).show();

    }
    //确认名字是否可行,名字不可行则进入按钮不能使用
    //因为是联网的有延迟，所以需要异步更新状态。
    public void checkName(final String name){
        //打开对话框
        regretDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        regretDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        regretDialog.setTitleText("正在登陆");
        regretDialog.setCancelable(false);
        regretDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //直接通过url来处理
                URL url = null;
                try {
                    url = new URL("http://"+ConstanNum.baseUrl+"/checkname/"+name);
                    HttpURLConnection conn = null;
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5 * 1000);
                    conn.setRequestMethod("GET");
                    InputStream inStream = conn.getInputStream();
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int n;
                    while (-1 != (n = inStream.read(buffer))) {
                        output.write(buffer, 0, n);
                    }
                    byte[] data = output.toByteArray();
                    String result = new String(data, "UTF-8");
                    //System.out.println(result);
                    Message m=new Message();
                    m.arg1=5;
                    handler.sendMessage(m);
                    Message message=new Message();
                    //如果是t，则为可用
                    if(result.equals("true"))
                        message.arg1=1;
                    else
                        message.arg1=2;
                    message.obj=name;
                    handler.sendMessage(message);
                }
                 catch (MalformedURLException e) {
                     errordialog();
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    errordialog();
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    errordialog();
                    e.printStackTrace();
                } catch (IOException e) {
                    errordialog();
                    e.printStackTrace();
                }
            }
        }).start();
    }
    void errordialog(){
        new SweetAlertDialog(MainActivity.this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("上不去网")
                .setContentText("要不就是服务器炸了")
                .showCancelButton(true)
                .show();
    }
    //如果名字可用后,点击确认按钮进入大厅，开启服务
    public void entranceHall(final String name){
        HallActivity.myName=name;
        ifLog=true;
        Message m=new Message();
        m.arg1=5;
        handler.sendMessage(m);
        //进入游戏大厅
        HallActivity.actionStart(MainActivity.this);
    }

    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //如果按了返回键
        if(keyCode==KeyEvent.KEYCODE_BACK){
            new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("退出还是最小化")
                    .setContentText("？？？")
                    .setCancelText("退出")
                    .setConfirmText("最小化")
                    .showCancelButton(true)
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            sDialog.dismissWithAnimation();
                            moveTaskToBack(true);
                        }
                    })
                    .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            moveTaskToBack(true);
                            sDialog.cancel();
                            close();
                        }
                    })
                    .show();
            return false;
        }
        else
            return super.onKeyUp(keyCode, event);
    }
    public static void actionStart(Context context){
        Intent intent=new Intent(context,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    private void close(){
        //myService.close();
        ActivityCollector.finishAll();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
