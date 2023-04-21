package com.example.motow;

public class Chats {

    String message, time, name;

    public Chats() {
        // Empty constructor needed
    }

    public Chats(String message, String time, String name) {
        this.message = message;
        this.time = time;
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
