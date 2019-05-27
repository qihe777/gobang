package com.example.wuziqi.playview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.example.wuziqi.R;

import java.util.concurrent.atomic.AtomicBoolean;

import AI.Board;
import AI.ConstanNum;
import AI.Point;

import static AI.ConstanNum.COM;
import static AI.ConstanNum.EQUE;
import static AI.ConstanNum.GRID_NUMBER;
import static AI.ConstanNum.HUMEN;

//机器和机器对战
public class AiFightboard extends View {

    //画笔
    private Paint paint;
    private Bitmap whiteChess;
    private Bitmap blackChess,miaozhun;
    private Rect chessRect;
    //棋盘
    private int [][]chessArray;
    //回调
    private WinCallBack callBack;
    ButtonCallback btcall;
    private Context mContext;
    //棋盘宽高
    private float len;
    //每格之间的距离
    private float preWidth;
    //边距
    private float offset;

    //是否正在游戏
    private boolean isPlaying,isBlcak;
    private AtomicBoolean compute;
    //firstBoard永远是黑棋，且永远是com
    private Board firstBoard;
    //开局随机点位
    private int randomX,randomY;
    //记录当前下点
    private int tmpComX,tmpComY,tmpRole;
    //判断是否暂停
    private boolean pause;
    //异步更新线程
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //更新棋盘
            postInvalidate();
            switch (msg.arg1){
                case COM://如果是黑棋下完了
                    tmpRole=COM;
                    checkGameOver(tmpComX, tmpComY);
                    //新建线程继续计算
                    compute.set(false);
                    if(isPlaying&&!pause){
                        compute.set(true);
                        //白棋下
                        //耗时操作在这里进行，进行完成后返回数据给ui进行更新。
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Point point=firstBoard.findPoint(ConstanNum.HUMEN,ConstanNum.searchDeep);
                                tmpComX=point.x;tmpComY=point.y;
                                firstBoard.put(point.x,point.y,HUMEN);
                                chessArray[point.x][point.y]=ConstanNum.HUMEN;

                                //通知更新画面
                                Message message=new Message();
                                message.arg1=HUMEN;
                                handler.sendMessage(message);
                            }
                        }).start();
                    }
                    break;
                case HUMEN://如果是白棋下完了
                    tmpRole=HUMEN;
                    isBlcak = !isBlcak;
                    checkGameOver(tmpComX, tmpComY);
                    isBlcak = !isBlcak;
                    compute.set(false);
                    if(isPlaying&&!pause){
                        compute.set(true);
                        //黑棋下
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Point point = firstBoard.findPoint(ConstanNum.COM, ConstanNum.searchDeep);
                                tmpComX=point.x;tmpComY=point.y;
                                firstBoard.put(point.x, point.y, COM);
                                chessArray[point.x][point.y] = ConstanNum.COM;
                                //通知更新画面
                                Message message = new Message();
                                message.arg1 = COM;
                                handler.sendMessage(message);
                            }
                        }).start();
                    }
                    break;
                case 10:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (compute.get()){
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            compute.set(false);
                            isPlaying=true;
                            firstBoard.init();
                            chessArray=new int [GRID_NUMBER][GRID_NUMBER];
                            postInvalidate();
                            pause=false;
                            start();
                            btcall.change(true);
                        }
                    }).start();
                    break;
            }
        }
    };

    public AiFightboard(Context context) {
        this(context, null);
    }
    public AiFightboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public AiFightboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //初始化Paint
        paint = new Paint();
        //设置抗锯齿
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
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
        pause=false;
        firstBoard=new Board();
        compute=new AtomicBoolean(false);
        start();
    }
    //点击开始后，就一直下棋，知道点击暂停

    //暂停后继续游戏函数。
    public void start(){
        //如果是从头开始，则需要提前下棋,因为isplaying还有可能是游戏结束而中断
        if(!pause){
            randomY=getRandom();
            randomX=getRandom();
            firstBoard.put(randomX,randomY,ConstanNum.COM);
            chessArray[randomX][randomY]=ConstanNum.COM;
            postInvalidate();
            Message message = new Message();
            message.arg1 = COM;
            handler.sendMessage(message);
        }else{
            if(!compute.get()){
                pause=false;
                Message message = new Message();
                message.arg1 = tmpRole;
                handler.sendMessage(message);
            }
        }
    }
    //暂停函数
    public void tmpPause(){
        pause=true;
    }

    //重开函数
    public void init(){
        //必须等到计算完成后才可以重开
        tmpPause();
        Message message = new Message();
        message.arg1 = 10;
        handler.sendMessage(message);
    }
    //计算视图的宽与高
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //获取高宽值
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
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
        len = getWidth() > getHeight() ? getHeight() : getWidth();
        preWidth = len / GRID_NUMBER;
        //边距
        offset = preWidth / 2;
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
                if(chessArray[i][j]==COM){
                    //rect中点坐标
                    rectX = offset + i * preWidth;
                    rectY = offset + j * preWidth;
                    //设置rect位置
                    chessRect.set((int) (rectX - offset), (int) (rectY - offset),
                            (int) (rectX + offset), (int) (rectY + offset));
                    canvas.drawBitmap(blackChess, null, chessRect, null);
                }
                else if(chessArray[i][j]==HUMEN){
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
        rectX = offset + tmpComX * preWidth;
        rectY = offset + tmpComY * preWidth;
        chessRect.set((int) (rectX - offset), (int) (rectY - offset),
                (int) (rectX + offset), (int) (rectY + offset));
        canvas.drawBitmap(miaozhun, null, chessRect, null);
    }


    //先判断是否连成
    void checkGameOver(int x,int y){
        //当前颜色
        int nowcolor;
        if(isBlcak){
            nowcolor=COM;
        }else
            nowcolor=HUMEN;
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
    public void setCallBack(WinCallBack callBack,ButtonCallback btcall){
        this.callBack=callBack;
        this.btcall=btcall;
    }
    private int getRandom(){
        return (int)(Math.random()*6+5);
    }
}
