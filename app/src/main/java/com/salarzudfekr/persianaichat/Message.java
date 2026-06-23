package com.salarzudfekr.persianaichat;

public class Message {
    public static final int ROLE_USER = 0;
    public static final int ROLE_BOT = 1;
    public static final int ROLE_LOADING = 2;

    public int role;
    public String content;
    public long timestamp;

    public Message() {}

    public Message(int role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
}
