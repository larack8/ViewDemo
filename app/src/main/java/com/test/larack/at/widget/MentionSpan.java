package com.test.larack.at.widget;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;

public class MentionSpan extends ForegroundColorSpan {

    public String uid;
    public String nick;

    private OnSpanClickListener clickListener;

    public static final int DEFAULT_BK_COLOR = Color.RED;

    public MentionSpan(String uid, String nick) {
        this(uid, nick, DEFAULT_BK_COLOR, null);
    }

    public MentionSpan(String uid, String nick, OnSpanClickListener listener) {
        this(uid, nick, DEFAULT_BK_COLOR, listener);
    }

    public MentionSpan(String uid, String nick, int color, OnSpanClickListener listener) {
        super(color);
        this.uid = uid;
        this.nick = nick;
        this.clickListener = listener;
    }

    public void onClick(View widget) {
        if (clickListener != null) clickListener.onSpanClick(this.uid, this.nick);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(ds.linkColor);
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

    interface OnSpanClickListener {
        void onSpanClick(String uid, String nick);
    }
}
