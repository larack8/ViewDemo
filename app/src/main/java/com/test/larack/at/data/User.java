package com.test.larack.at.data;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    public static final String TAG = User.class.getSimpleName();

    public String uid;
    public String nick;

    public User() {

    }

    public User(String uid, String nick) {
        this.uid = uid;
        this.nick = nick;
    }

    public String getUserId() {
        return uid;
    }

    public void setUserId(String uid) {
        this.uid = uid;
    }

    public String getNickname() {
        return nick;
    }

    public void setNickname(String nick) {
        this.nick = nick;
    }

    @Override
    public String toString() {
        return nick;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uid);
        dest.writeString(this.nick);
    }

    protected User(Parcel in) {
        this.uid = in.readString();
        this.nick = in.readString();
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };
}