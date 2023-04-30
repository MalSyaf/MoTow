package com.example.motow.chats;

import java.util.Date;

public class Chats {

    String message, dateTime, name, senderId, receiverId;
    Date dateObject;

    public Chats() {
        // Empty constructor needed
    }

    public Chats(String message, String dateTime, String name, String senderId, String receiverId) {
        this.message = message;
        this.dateTime = dateTime;
        this.name = name;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public Date getDateObject() {
        return dateObject;
    }

    public void setDateObject(Date dateObject) {
        this.dateObject = dateObject;
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
