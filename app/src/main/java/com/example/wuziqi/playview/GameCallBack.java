package com.example.wuziqi.playview;

public interface GameCallBack {
    void GameOver(int winner);
    void sendPoint(int x,int y);
    void receivePoint(int x,int y,int minute);
}
