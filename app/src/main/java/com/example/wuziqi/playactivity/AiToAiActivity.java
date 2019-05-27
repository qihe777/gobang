package com.example.wuziqi.playactivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.wuziqi.BaseActivity;
import com.example.wuziqi.HallActivity;
import com.example.wuziqi.MainActivity;
import com.example.wuziqi.R;
import com.example.wuziqi.playview.AiFightboard;
import com.example.wuziqi.playview.ButtonCallback;
import com.example.wuziqi.playview.WinCallBack;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static AI.ConstanNum.COM;
import static AI.ConstanNum.EQUE;
import static AI.ConstanNum.HUMEN;

public class AiToAiActivity extends BaseActivity implements WinCallBack,ButtonCallback {
    private AiFightboard aiFightboard;
    Button restart_btn;
    private boolean stop;
    private boolean first=true;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //棋盘
        setContentView(R.layout.activity_ai_to_ai);
        aiFightboard=findViewById(R.id.human_chess);
        aiFightboard.setCallBack(this,this);

        //暂停按钮
        stop=false;
        final Button stop_btn=(Button)findViewById(R.id.stop);
        stop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!stop){
                    stop=true;
                    stop_btn.setText("继续");
                    aiFightboard.tmpPause();

                }
                else{
                    stop=false;
                    stop_btn.setText("暂停");
                    aiFightboard.start();
                }

            }
        });

        //重来按钮
        restart_btn=(Button)findViewById(R.id.giveup);
        restart_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restart_btn.setClickable(false);
                aiFightboard.init();
            }
        });


    }

    @Override
    protected void onPause() {
        super.onPause();
        aiFightboard.tmpPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(first){
            first=false;
        }
        else
            aiFightboard.start();

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
        Intent intent=new Intent(context,AiToAiActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(intent);
    }

    @Override
    public void gameOver(int winner) {
        switch (winner) {
            case COM:
                showToast("黑棋胜利！");
                break;
            case EQUE:
                showToast("平局！");
                break;
            case HUMEN:
                showToast("白棋胜利！");
                break;
        }
    }
    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void change(Boolean flag) {
        restart_btn.setClickable(flag);
    }
}
