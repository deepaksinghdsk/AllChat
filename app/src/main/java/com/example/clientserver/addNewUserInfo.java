package com.example.clientserver;

public class addNewUserInfo {
    public String myProfilePic = "0", name = "unknown", newmessages = "0", phone_number = "0000000000", writing = "0";

    public addNewUserInfo(String name) {
        this.name = name;
    }

    public addNewUserInfo(String phone_number, String name) {
        this.phone_number = phone_number;
        this.name = name;
    }
}
