package com.example.wuziqi.playactivity;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Player implements Parcelable {
    private String name;
    private boolean playing;

    public Player(String name, boolean playing) {
        this.name = name;
        this.playing = playing;
    }
    public Player(){}

    protected Player(Parcel in) {
        name = in.readString();
        playing = in.readByte() != 0;
    }

    public static final Creator<Player> CREATOR = new Creator<Player>() {
        @Override
        public Player createFromParcel(Parcel in) {
            return new Player(in);
        }

        @Override
        public Player[] newArray(int size) {
            return new Player[size];
        }
    };

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeByte((byte)(playing? 1:0));
    }


}
