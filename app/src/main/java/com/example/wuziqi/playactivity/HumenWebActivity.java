package com.example.wuziqi.playactivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wuziqi.BaseActivity;
import com.example.wuziqi.HallActivity;
import com.example.wuziqi.MainActivity;
import com.example.wuziqi.playview.ButtonCallback;
import com.example.wuziqi.playview.CountDownProgressBar;
import com.example.wuziqi.playview.GameCallBack;
import com.example.wuziqi.R;
import com.example.wuziqi.playview.HumenWebBoard;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.io.Serializable;

import AI.ConstanNum;
import cn.pedant.SweetAlert.SweetAlertDialog;
import service.MyService;

import static AI.ConstanNum.COM;
import static AI.ConstanNum.EQUE;
import static AI.ConstanNum.HUMEN;

public class HumenWebActivity extends BaseActivity implements GameCallBack{

    private BroadcastReceiver dataReceiver;
    private HumenWebBoard humenWebBoard;
    private MyService myService;
    private boolean ifFirst,playing;
    private TextView leftText,rightText;
    String comName,myName;
    private CountDownProgressBar cpb_countdown;
    private SweetAlertDialog regretDialog,successDialog,
            failDialog,winDialog,loseDialog,askRegretDialog,
            waitDailog;
//    ShimmerFrameLayout playContain;
//    TextView textView;
    Button giveup_btn,regret_btn,goon_btn;
    //异步更新线程
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //通过arg1来判断消息内容
            switch (msg.arg1){
                //赢了
                case 1:
                    winDialog.show();
                    break;
                //2.输了
                case 2:
                    loseDialog.show();
                    break;

                case 5:
                    //悔棋框10s后消失
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(8000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message message=new Message();
                            message.arg1=51;
                            handler.sendMessage(message);

                        }
                    }).start();
                    break;
                case 51:
                    if(regretDialog.isShowing())
                        regretDialog.cancel();
                    break;

                case 6:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(4000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message message=new Message();
                            message.arg1=61;
                            handler.sendMessage(message);
                        }
                    }).start();
                    break;
                case 61:
                    if(askRegretDialog.isShowing()){
                        askRegretDialog.cancel();
                    }
                    break;
                case 7:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message message=new Message();
                            message.arg1=71;
                            handler.sendMessage(message);
                        }
                    }).start();
                    break;
                case 71:
                    if(successDialog.isShowing())
                        successDialog.cancel();
                    humenWebBoard.regretChess();
                    break;
                case 8:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message message=new Message();
                            message.arg1=81;
                            handler.sendMessage(message);
                        }
                    }).start();
                    break;
                case 81:
                    if(failDialog.isShowing())
                    failDialog.cancel();
                    break;
                case 91:
                    new SweetAlertDialog(HumenWebActivity.this,SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Black")
                            .setContentText("你是黑色你先走")
                            .showCancelButton(true).show();
                    humenWebBoard.startGame(true);
                    break;
                case 92:
                    new SweetAlertDialog(HumenWebActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("White")
                            .setContentText("白色往后稍稍")
                            .showCancelButton(true).show();
                    humenWebBoard.startGame(false);
                    changeButton(false);
                    break;
                case 93:
                    leftText.setText(myName);
                    rightText.setText(comName);
                    waitDailog.hide();
                    break;
            }
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //绑定服务
        Intent intent=new Intent(HumenWebActivity.this,MyService.class);
        bindService(intent,sc,BIND_AUTO_CREATE);


       /* playContain=findViewById(R.id.playContain);
        textView=findViewById(R.id.playText);
        playContain.startShimmer();*/

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_humen_web);
        humenWebBoard=findViewById(R.id.web_chess);

        //悔棋按钮
        regret_btn= findViewById(R.id.regret1);
        regret_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(HumenWebActivity.this,"悔棋",Toast.LENGTH_SHORT).show();
                regret();
            }
        });

        //认输按钮
        giveup_btn=(Button)findViewById(R.id.giveup);
        giveup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(HumenWebActivity.this,"认输",Toast.LENGTH_SHORT).show();
                myService.wantLose();
                if(ifFirst){
                    GameOver(ConstanNum.COM);
                }
                else{
                    GameOver(ConstanNum.HUMEN);
                }
            }
        });
        leftText=findViewById(R.id.blackName);
        rightText=findViewById(R.id.whiteName);
        //帮助按钮
        goon_btn=(Button)findViewById(R.id.help);
        goon_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                humenWebBoard.getHelp();
                //Toast.makeText(HumenWebActivity.this,"帮助",Toast.LENGTH_SHORT).show();
            }
        });

        /*cpb_countdown = (CountDownProgressBar) findViewById(R.id.cpb_countdown);
        cpb_countdown.setDuration(30000, new CountDownProgressBar.OnFinishListener() {
            @Override
            public void onFinish() {
                //Toast.makeText(MainActivity.this,"时间到",Toast.LENGTH_SHORT).show();
            }
        });*/

        //等待话框
        waitDailog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        waitDailog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        waitDailog.setTitleText("等待游戏开始");
        waitDailog.setCancelable(false);
        //等待游戏开始的进度条
        if(!waitDailog.isShowing()){
            waitDailog.show();
        }
       newSuccess();
       newFail();
        winDialog=new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
                winDialog.setTitleText("Good job!，Well Done")
                        .setContentText("请自行返回大厅")
                .showCancelButton(true);
                loseDialog=new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE);
        loseDialog.setTitleText("输了哦")
                .setContentText("请自行返回大厅")
                .showCancelButton(true);
        //设置回调函数
        humenWebBoard.setCallBack(this);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        while(myService==null){
                            try {
                                Thread.sleep(800);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        myService.getStartInfo();
                    }
                }
        ).start();
    }

    private void changeButton(Boolean touch){
        giveup_btn.setClickable(touch);
        goon_btn.setClickable(touch);
        regret_btn.setClickable(touch);
       /* if(touch){
            textView.setText("该您下棋了");
        }
        else{
            textView.setText("等待对方下棋，小心对方走了");
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注册接收器
        dataReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getIntExtra("flag",0)){
                    case 216:
                        playing=true;
                        comName=intent.getStringExtra("comName");
                        myName=intent.getStringExtra("myName");
                        ifFirst=intent.getBooleanExtra("isFirst",false);
                        Message m2=new Message();
                        m2.arg1=93;
                        handler.sendMessage(m2);
                        //提示当前棋盘颜色
                        if(ifFirst){
                            Message m=new Message();
                            m.arg1=91;
                            handler.sendMessage(m);
                        }
                        else{
                            Message m=new Message();
                            m.arg1=92;
                            handler.sendMessage(m);
                        }
                        break;

                    case 311:
                        int x=intent.getIntExtra("x",0);
                        receivePoint(x,
                                intent.getIntExtra("y",0),intent.getIntExtra("minute",0));
                        changeButton(true);
                        break;
                    case 312:
                        beaskRegret();
                        break;
                    case 313:
                        getregret(intent.getBooleanExtra("regret",false));
                        break;
                    case 314:
                        comLose();
                        break;
                }
            }
        };
        IntentFilter filter=new IntentFilter();
        filter.addAction("webBroad");
        registerReceiver(dataReceiver,filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(sc);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(dataReceiver);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //如果按了返回键
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if(playing){
                end();
                new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("确定退出吗")
                        .setContentText("？？？")
                        .setCancelText("算了")
                        .setConfirmText("偏要")
                        .showCancelButton(true)
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                //发送推出消息
                                myService.wantLose();
                                sDialog.dismissWithAnimation();
                                HallActivity.actionStart(HumenWebActivity.this);
                                HumenWebActivity.this.finish();
                            }
                        })
                        .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.cancel();
                            }
                        })
                        .show();
            }
            else{
                HallActivity.actionStart(HumenWebActivity.this);
                HumenWebActivity.this.finish();
            }
            return false;
        }
        else
        return super.onKeyUp(keyCode, event);
    }

    public static void actionStart(Context context){
        Intent intent=new Intent(context,HumenWebActivity.class);
        context.startActivity(intent);
    }
    //通过对话框来进行重新展示
    @Override
    public void GameOver(int winner) {
        Message message=new Message();
        end();
        //给服务器发送
        switch (winner) {
            //赢了
            case COM:
                message.arg1=1;
                handler.sendMessage(message);
                break;
            case EQUE:
                showToast("平局！");
                break;
                //输了
            case HUMEN:
                message.arg1=2;
                handler.sendMessage(message);
                break;
        }
    }
    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }
    //服务器发出的开始游戏命令，未开始之前游戏界面应该提示正在加载
    //接受棋子颜色信息，并开始计时，注意提醒玩家下棋

    //当view点击了之后

    @Override
    public void sendPoint(int x, int y) {
        //设置按钮不可点击
        changeButton(false);
        //暂时设置minute为0
        int minute=0;
        myService.sendPoint(x,y,minute);
    }
    //当接收到消息时，则调用棋盘界面的函数
    @Override
    public void receivePoint(int x, int y,int minute) {
        humenWebBoard.receivePoint(x,y);
    }

    //点击了悔棋按钮
    void newRegretDialog(){
        //悔棋对话框
        regretDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        regretDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        regretDialog.setTitleText("等待对方回应(10s)");
        regretDialog.setCancelable(false);
    }

    void newFail(){
        failDialog=new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE);
        failDialog.setTitleText("对方小嘴抹了蜜")
                .setContentText("让你滚")
                .showCancelButton(true);
    }
    void newSuccess(){
        successDialog=new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        successDialog.setTitleText("cool")
                .setContentText("舔狗同意了您的请求")
                .showCancelButton(true);
    }
    private void regret(){
        newRegretDialog();
        //弹出对话框
        regretDialog.show();
       Message message=new Message();
       message.arg1=5;
       handler.sendMessage(message);
        myService.askRegret();
    }
    void newBeask(){
        askRegretDialog= new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE);
        askRegretDialog.setTitleText("小伙汁")
                .setContentText("悔棋了解一下(5s)")
                .setCancelText("滚")
                .setConfirmText("行吧")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        myService.ifRegret(true);
                        sDialog.cancel();
                        humenWebBoard.regretChess();
                    }
                })
                .showCancelButton(true)
                .setCancelClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        myService.ifRegret(false);
                        sDialog.cancel();
                    }
                });
    }
    //接收到对方申请悔棋,弹出对话框，且如果同意则需要调用humenWebBoard.regretChess();
    public void beaskRegret(){
        newBeask();
        askRegretDialog.show();
        Message message=new Message();
        message.arg1=6;
        handler.sendMessage(message);
    }
    //如果对方掉线了，则判己方胜利，弹出对话框，点击确定后返回大厅
    public void comLose(){
        end();
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText("Moss已经叛逃")
                .setContentText("您需要返回大厅")
                .setConfirmText("返回")
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        sDialog.cancel();
                        HallActivity.actionStart(HumenWebActivity.this);
                        HumenWebActivity.this.finish();
                    }
                })
                .show();
    }
    //收到对方是否同意悔棋的消息
    public void getregret(boolean ifAgree){
        regretDialog.cancel();
        //对方同意
        if(ifAgree){
            newSuccess();
            successDialog.show();
            Message message=new Message();
            message.arg1=7;
            handler.sendMessage(message);

        }
        else{
            newFail();
            failDialog.show();
            Message message=new Message();
            message.arg1=8;
            handler.sendMessage(message);
        }
    }
    //结束后发送消息给服务器
    private void end(){
        playing=false;
        myService.end();
        //按钮不可点击
        changeButton(false);
    }

}
