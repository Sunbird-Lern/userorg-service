package org.sunbird.bean;


import java.io.Serializable;

public class MigrationUser implements Serializable {

    private String email;
    private String phone;
    private String name;
    private String userExternalId;
    private String orgExternalId;
    private String channel;
    private String inputStatus;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserExternalId(String userExternalId) {
        this.userExternalId = userExternalId;
    }

    public void setOrgExternalId(String orgExternalId) {
        this.orgExternalId = orgExternalId;
    }

    public void setChannel(String state) {
        this.channel = state;
    }

    public void setInputStatus(String inputStatus) {
        this.inputStatus = inputStatus;
    }

    public MigrationUser() {
    }


    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }

    public String getUserExternalId() {
        return userExternalId;
    }

    public String getOrgExternalId() {
        return orgExternalId;
    }

    public String getChannel() {
        return channel;
    }

    public String getInputStatus() {
        return inputStatus;
    }

    @Override
    public String toString() {
        return "MigrationUser{" +
                "email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", name='" + name + '\'' +
                ", userExternalId='" + userExternalId + '\'' +
                ", orgExternalId='" + orgExternalId + '\'' +
                ", state='" + channel + '\'' +
                ", inputStatus=" + inputStatus +
                '}';
    }
}
