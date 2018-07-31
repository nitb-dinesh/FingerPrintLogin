package com.inducesmile.androidfingerprintlogin;


public class UserObject {

    private String username;
    private String email;
    private String password;
    private String address;
    private String phone;
    private boolean loginOption;

    public UserObject(String username, String email, String password, String address, String phone, boolean loginOption) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.address = address;
        this.phone = phone;
        this.loginOption = loginOption;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isLoginOption() {
        return loginOption;
    }
}
