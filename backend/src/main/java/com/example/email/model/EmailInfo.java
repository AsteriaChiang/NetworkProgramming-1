package com.example.email.model;

import java.util.ArrayList;
import java.util.List;

public class EmailInfo {
    private Integer user_id;
    private String subject;
    private String content;
    private String date;
    private String from;
    private String to;
    private List<SubContentInfo> subContents = new ArrayList<>();
    private String contentType;
    private String boundary = "";


    public void setSubContent(List<String> subs){
        for(String s:subs){
            this.content += s;
        }
    }

    public List<SubContentInfo> getSubContents() {
        return subContents;
    }

    public void setSubContents(List<SubContentInfo> subContents) {
        this.subContents = subContents;
    }

    public void setBoundary(String boundary){
        this.boundary = boundary;
    }

    public void setContentType(String contentType){
        this.contentType = contentType;
    }

    public void setSubject(String subject){
        this.subject = subject;
    }

    public void setContent(String content){
        this.content = content;
    }

    public void setDate(String date){
        this.date = date;
    }

    public void setFrom(String from){
        this.from = from;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBoundary(){
        return boundary;
    }

}
