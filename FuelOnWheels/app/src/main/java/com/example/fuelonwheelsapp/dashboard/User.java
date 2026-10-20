package com.example.fuelonwheelsapp.dashboard;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String fullName;
    private String email;
    private String phoneNo;
    private String otp;

    public User() {
        // Default constructor
    }

    public User(String fullName, String email, String phoneNo, String otp) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNo = phoneNo;
        this.otp = otp;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
