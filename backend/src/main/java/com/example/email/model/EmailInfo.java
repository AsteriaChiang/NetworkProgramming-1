package com.example.email.model;

public class EmailInfo {
    private Integer user_id;
    private String subject;
    private String content;
    private String date;
    private String from;
    private String to;
    private String attachment;

    public Integer getId() {
        return user_id;
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

    public String getAttachment() {
        return attachment;
    }
}
