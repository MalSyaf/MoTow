package com.example.motow.users;

import java.io.Serializable;

public class Users implements Serializable {

    public String userId, pfp, name;

    public Users(String userId, String pfp, String name) {
        this.userId = userId;
        this.pfp = pfp;
        this.name = name;
    }

    public Users() {
        //
    }

    public String getPfp() {
        return pfp;
    }

    public void setPfp(String pfp) {
        this.pfp = pfp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
