package com.example.wuziqi.playactivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wuziqi.BaseActivity;
import com.example.wuziqi.MainActivity;
import com.example.wuziqi.R;
import com.example.wuziqi.playview.ButtonCallback;
import com.example.wuziqi.playview.HumenFightAiBoard;
import com.example.wuziqi.playview.WinCallBack;
import com.facebook.shimmer.ShimmerFrameLayout;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static AI.ConstanNum.COM;
import static AI.ConstanNum.EQUE;
import static AI.ConstanNum.HUMEN;

public class HumenToAiActivity extends BaseActivity implements WinCallBack ,ButtonCallback {

    private HumenFightAiBoard humenFightAiBoard;
    Button regret_btn,restart_btn,help_btn;
    ShimmerFrameLayout playContain;
    TextView textView;
    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //通过arg1来判断消息内容
            switch (msg.arg1){
                //1为当前名称可以使用。
                case 1:
                    new SweetAlertDialog(HumenToAiActivity.this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("输给电脑了哦")
                            .showCancelButton(true)
                            .show();
                    break;
                case 2:
                    new SweetAlertDialog(HumenToAiActivity.this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("哎呦，不错哦")
                            .showCancelButton(true)
                            .show();
                    break;
                    //改人下了，
                case 3:
                    playContain.startShimmer();
                    textView.setText("等待您下棋");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message m=new Message();
                            m.arg1=5;
                            handler.sendMessage(m);
                        }
                    }).start();
                    break;
                    //该机器下了
                case 4:
                    textView.setText("机器正在思考，给他一点时间(2分钟左右)");
                    playContain.startShimmer();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Message m=new Message();
                            m.arg1=5;
                            handler.sendMessage(m);
                        }
                    }).start();
                    break;
                case 5:
                    if(playContain.isShimmerStarted()){
                        playContain.stopShimmer();
                    }
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_humen_to_ai);
        humenFightAiBoard=findViewById(R.id.human_chess);
        playContain=findViewById(R.id.playContain);
        textView=findViewById(R.id.playText);
        //悔棋按钮
        regret_btn=(Button)findViewById(R.id.regret);
        regret_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                humenFightAiBoard.regretChess();
            }
        });
        //重来按钮
        restart_btn=(Button)findViewById(R.id.giveup);
        restart_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                humenFightAiBoard.init();
            }
        });

        //帮助按钮
        help_btn=(Button)findViewById(R.id.help);
        help_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                humenFightAiBoard.getHelp();
            }
        });
        humenFightAiBoard.setCallBack(this,this);
    }
    public static void actionStart(Context context){
        Intent intent=new Intent(context,HumenToAiActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
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
    //这个胜利需要自己更改样式
    @Override
    public void gameOver(int winner) {
        Message m=new Message();
        switch (winner) {
            case COM:
                m.arg1=1;
                handler.sendMessage(m);
                break;
            case EQUE:
                showToast("平局！");
                break;
            case HUMEN:
                m.arg1=2;
                handler.sendMessage(m);
                break;
        }
    }
    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }



    @Override
    public void change(Boolean flag) {
        if(flag){
            Message m=new Message();
            m.arg1=3;
            handler.sendMessage(m);
        }
        else{
            Message m=new Message();
            m.arg1=4;
            handler.sendMessage(m);
        }
        help_btn.setClickable(flag);
        regret_btn.setClickable(flag);
        restart_btn.setClickable(flag);
    }
}