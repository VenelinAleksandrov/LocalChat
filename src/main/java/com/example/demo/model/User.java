package com.example.demo.model;

public class User {
    private String username;
    private String password;
    private String avatar;

    public User() {}

    public User(String username, String password, String avatar) {
        this.username = username;
        this.password = password;
        this.avatar = avatar;
    }

    public String getUsername() {
        return username;
    }

    @SuppressWarnings("unused")
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    @SuppressWarnings("unused")
    public void setPassword(String password) {
        this.password = password;
    }

    @SuppressWarnings("unused")
    public String getAvatar() {
        return avatar;
    }

    @SuppressWarnings("unused")
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}