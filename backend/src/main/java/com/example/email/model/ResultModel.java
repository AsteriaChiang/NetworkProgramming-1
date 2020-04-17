package com.example.email.model;

import java.util.ArrayList;
import java.util.Map;

public class ResultModel {
    private int errcode;// 返回码
    private String errmsg;// 返回消息
    private ArrayList<EmailInfo> data;// 数据源

    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public Object getData() {
        return data;
    }

    public void setData(ArrayList<EmailInfo> data) {
        this.data = data;
    }

}
