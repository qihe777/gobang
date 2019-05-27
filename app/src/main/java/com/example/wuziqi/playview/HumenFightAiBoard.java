package com.example.wuziqi.playview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import android.os.Handler;

import com.example.wuziqi.R;

import AI.Board;
import AI.ConstanNum;
import AI.Point;

import static AI.ConstanNum.COM;
import static AI.ConstanNum.EQUE;
import static AI.ConstanNum.GRID_NUMBER;
//需要修改，根据机器是否先下，来确定黑棋和白起的值。
public class HumenFightAiBoard extends View implements View.OnTouchListener {

    //画笔
    private Paint paint,bluePaint;
    private Bitmap whiteChess;
    private Bitmap blackChess,miaozhun;
    private Rect chessRect;
    //棋盘
    private int [][]chessArray;
    private int BLACK_CHESS,WHITE_CHESS;
    //回调
    private WinCallBack callBack;
    ButtonCallback buttnCallBack;
    private Context mContext;

    //每格之间的距离
    private float preWidth;

    //是否正在游戏,是否该黑棋下
    private boolean isPlaying,isBlcak;
    private int tmpHumX,tmpHumY,tmpComX,tmpComY;
    private Board myBoard;
    //如果电脑在计算，则棋盘点击事件不处理
    public static boolean comPlaying;
    //如果提示
    private boolean ifHelp,regret,init;
    private int helpX,helpY;

    //异步更新线程
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1){
                case 9:
                    init=false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (comPlaying){
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            isBlcak=true;
                            isPlaying=true;
                            myBoard.init();
                            chessArray=new int [GRID_NUMBER][GRID_NUMBER];
                            //调整方向
                            if(BLACK_CHESS==ConstanNum.COM){
                                BLACK_CHESS=ConstanNum.HUMEN;
                                WHITE_CHESS=ConstanNum.COM;
                            }
                            else{
                                //如果电脑是黑棋
                                BLACK_CHESS=ConstanNum.COM;
                                WHITE_CHESS=ConstanNum.HUMEN;
                                chessArray[7][7]=ConstanNum.COM;
                                myBoard.put(7,7,ConstanNum.COM);
                            }
                            postInvalidate();
                            init=true;
                        }
                    }).start();
                    break;
                    //悔棋
                case 10:
                    regret=false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (comPlaying){
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(myBoard.backward()){
                                chessArray[tmpHumX][tmpHumY]=ConstanNum.EMPTY;
                                chessArray[tmpComX][tmpComY]=ConstanNum.EMPTY;
                            }
                            postInvalidate();
                            comPlaying=false;
                            buttnCallBack.change(true);
                            regret=true;
                        }
                    }).start();
                    break;
                    default:
                        //更新棋盘
                        postInvalidate();
                        //判断是否结束
                        if(BLACK_CHESS==ConstanNum.COM){
                            checkGameOver(tmpComX, tmpComY);
                        }
                        else{
                            isBlcak = !isBlcak;
                            checkGameOver(tmpComX, tmpComY);
                            isBlcak = !isBlcak;
                        }
                        comPlaying=false;
                        buttnCallBack.change(true);
            }
        }
    };

    public HumenFightAiBoard(Context context) {
        this(context, null);
    }
    public HumenFightAiBoard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public HumenFightAiBoard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        regret=true;
        init=true;
        //初始化Paint
        paint = new Paint();
        bluePaint=new Paint();
        //设置抗锯齿
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        bluePaint.setColor(Color.BLUE);
        bluePaint.setStyle(Paint.Style.STROKE);//画笔属性是空心圆
        bluePaint.setStrokeWidth(8);//设置画笔粗细
        mContext=context;
        //初始化图片
        //bitmap
        whiteChess=BitmapFactory.decodeResource(context.getResources(),R.drawable.white_chess);
        blackChess=BitmapFactory.decodeResource(context.getResources(),R.drawable.black_chess);
        miaozhun=BitmapFactory.decodeResource(context.getResources(),R.drawable.miaozhun);
        //初始化棋盘
        chessArray=new int [GRID_NUMBER][GRID_NUMBER];
        chessRect=new Rect();
        isBlcak=true;
        isPlaying=true;
        //设置监听
        setOnTouchListener(this);
         myBoard=new Board();
         comPlaying=false;
         WHITE_CHESS=ConstanNum.COM;
         BLACK_CHESS=ConstanNum.HUMEN;
         ifHelp=false;
    }
    //重开
    public void init(){
        if(init){
            Message message = new Message();
            message.arg1 = 9;
            handler.sendMessage(message);
        }
    }
    //悔棋按钮
    public void regretChess(){
        if(regret){
            Message message = new Message();
            message.arg1 = 10;
            handler.sendMessage(message);
        }
    }
    //帮助按钮
    public void getHelp(){
        //耗时操作在这里进行，进行完成后返回数据给ui进行更新。
        if(!ifHelp){
            ifHelp=true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Point point=myBoard.findPoint(ConstanNum.HUMEN,ConstanNum.searchDeep);
                    helpX=point.x;helpY=point.y;
                    //通知更新画面
                    Message message=new Message();
                    message.arg1=1;
                    handler.sendMessage(message);
                }
            }).start();
        }
    }

    //计算视图的宽与高
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取高宽值
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        //获取宽高中较小的值
        int len = width > height ? height : width;
        //重新设置宽高
        setMeasuredDimension(len, len);
    }

    //绘制五子棋画面
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //棋盘为一个GRID_NUMBER*GRID_NUMBER的正方形，所有棋盘宽高必须一样
        //棋盘宽高
        float len = getWidth() > getHeight() ? getHeight() : getWidth();
        preWidth = len / GRID_NUMBER;
        //边距
        //边距
        float offset = preWidth / 2;
        //绘制背景棋盘
        for (int i = 0; i < GRID_NUMBER; i++) {
            float start = i * preWidth + offset;
            //横线
            canvas.drawLine(offset, start, len - offset, start, paint);
            //竖线
            canvas.drawLine(start, offset, start, len - offset, paint);
        }

        //绘制棋子
        float rectX,rectY;
        for(int i=0;i<GRID_NUMBER;i++){
            for(int j=0;j<GRID_NUMBER;j++){
                if(chessArray[i][j]==BLACK_CHESS){
                    //rect中点坐标
                    rectX = offset + i * preWidth;
                    rectY = offset + j * preWidth;
                    //设置rect位置
                    chessRect.set((int) (rectX - offset), (int) (rectY - offset),
                            (int) (rectX + offset), (int) (rectY + offset));
                    canvas.drawBitmap(blackChess, null, chessRect, null);
                }
                else if(chessArray[i][j]==WHITE_CHESS){
                    //rect中点坐标
                    rectX = offset + i * preWidth;
                    rectY = offset + j * preWidth;
                    //设置rect位置
                    chessRect.set((int) (rectX - offset), (int) (rectY - offset),
                            (int) (rectX + offset), (int) (rectY + offset));
                    canvas.drawBitmap(whiteChess, null, chessRect, null);
                }
            }
        }
        //画红色提示信息
        rectX = offset + tmpComX * preWidth;
        rectY = offset + tmpComY * preWidth;
        chessRect.set((int) (rectX - offset), (int) (rectY - offset),
                (int) (rectX + offset), (int) (rectY + offset));
        canvas.drawBitmap(miaozhun, null, chessRect, null);
        //画提示信息
        if(ifHelp){
            rectX = offset + helpX * preWidth;
            rectY = offset + helpY * preWidth;
            canvas.drawCircle(rectX,rectY, offset, bluePaint);
            ifHelp=false;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(comPlaying||ifHelp){
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isPlaying) {
                    //获取按下时的位置
                    float downX = event.getX();
                    float downY = event.getY();
                    //点击的位置在棋盘上

                    //获取棋子对应的位置
                    int x = (int) (downX / preWidth);
                    int y = (int) (downY / preWidth);
                    tmpHumX=x;tmpHumY=y;
                    //判断当前位置是否已经有子
                    if (chessArray[x][y] != WHITE_CHESS &&
                            chessArray[x][y] != BLACK_CHESS) {
                        chessArray[x][y]=ConstanNum.HUMEN;
                        //更新棋盘
                        postInvalidate();
                        if(BLACK_CHESS==ConstanNum.HUMEN){
                            checkGameOver(x, y);
                        }
                        else{
                            isBlcak = !isBlcak;
                            checkGameOver(x, y);
                            isBlcak = !isBlcak;
                        }
                        myBoard.put(x,y,ConstanNum.HUMEN);
                        comPlaying=true;
                        buttnCallBack.change(false);
                        //耗时操作在这里进行，进行完成后返回数据给ui进行更新。
                       new Thread(new Runnable() {
                           @Override
                           public void run() {
                               Point point=myBoard.findPoint(ConstanNum.COM,ConstanNum.searchDeep);
                               tmpComX=point.x;tmpComY=point.y;
                               myBoard.put(point.x,point.y,COM);
                               chessArray[point.x][point.y]=ConstanNum.COM;
                               //通知更新画面
                               Message message=new Message();
                               message.arg1=1;
                               handler.sendMessage(message);
                           }
                       }).start();
                    }
                } else {
                    Toast.makeText(mContext, "游戏已经结束，请重新开始！",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return false;
    }
    //先判断是否连成
    void checkGameOver(int x,int y){
        //当前颜色
        int nowcolor;
        if(isBlcak){
            nowcolor=BLACK_CHESS;
        }else
            nowcolor=WHITE_CHESS;
        //当前连续子
        int nownum=0;
        //左右方向
        for(int i=1;i<5;i++){
            //如果是现在的颜色
            if(x>=i&&nowcolor==chessArray[x-i][y]){
                nownum++;
            }
            else
                break;
        }
        for(int i=1;i<5;i++){
            if(x+i<GRID_NUMBER&&nowcolor==chessArray[x+i][y]){
                nownum++;
            }
            else
                break;
        }
        if(nownum>=4){
            isPlaying=false;
            callBack.gameOver(nowcolor);
           return ;
        }
        nownum=0;
        //上下方向
        for(int i=1;i<5;i++){
            //如果是现在的颜色
            if(y+i<GRID_NUMBER&&nowcolor==chessArray[x][y+i]){
                nownum++;
            }
            else
                break;
        }
        for(int i=1;i<5;i++){
            if(y>=i&&nowcolor==chessArray[x][y-i]){
                nownum++;
            }
            else
                break;
        }
        if(nownum>=4){
            isPlaying=false;
            callBack.gameOver(nowcolor);
            return ;
        }
        nownum=0;
        //you倾斜
        for(int i=1;i<5;i++){
            //如果是现在的颜色
            if(x>=i&&y>=i&&nowcolor==chessArray[x-i][y-i]){
                nownum++;
            }
            else
                break;
        }
        for(int i=1;i<5;i++){
            if(x+i<GRID_NUMBER&&y+i<GRID_NUMBER&&nowcolor==chessArray[x+i][y+i]){
                nownum++;
            }
            else
                break;
        }
        if(nownum>=4){
            isPlaying=false;
            callBack.gameOver(nowcolor);
            return ;
        }
        nownum=0;
        //zuo倾斜
        for(int i=1;i<5;i++){
            //如果是现在的颜色
            if(x>=i&&y+i<GRID_NUMBER&&nowcolor==chessArray[x-i][y+i]){
                nownum++;
            }
            else
                break;
        }
        for(int i=1;i<5;i++){
            if(x+i<GRID_NUMBER&&y-i>=0&&nowcolor==chessArray[x+i][y-i]){
                nownum++;
            }
            else
                break;
        }
        if(nownum>=4){
            isPlaying=false;
            callBack.gameOver(nowcolor);
            return ;
        }
        //判断棋盘是否满了
        for(int i=0;i<GRID_NUMBER;i++){
            for(int j=0;j<GRID_NUMBER;j++){
                if(chessArray[i][j]==0){
                    return ;
                }
            }
        }
        callBack.gameOver(EQUE);
    }
    public void setCallBack(WinCallBack callBack,ButtonCallback buttonCallback){
        this.callBack=callBack;
        this.buttnCallBack=buttonCallback;
    }
}
