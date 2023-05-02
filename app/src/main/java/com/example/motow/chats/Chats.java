package com.example.motow.chats;

import java.util.Date;

public class Chats {

    public String sender, receiver, message, dateTime;
    public Date dateObject;

    public Chats(String sender, String receiver, String message, String dateTime, Date dateObject) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.dateTime = dateTime;
        this.dateObject = dateObject;
    }

    public Chats() {
    }

    public Date getDateObject() {
        return dateObject;
    }

    public void setDateObject(Date dateObject) {
        this.dateObject = dateObject;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
