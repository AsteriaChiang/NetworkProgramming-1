package com.example.email.model;

public class SubContentInfo {
    private String contentType;
    private String charset;
    private String encoding;
    private String subContent = "";
    private String filename;
    private String attachment;

    public String getAttachment() {
        return attachment;
    }

    public String getCharset() {
        return charset;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getFilename() {
        return filename;
    }

    public String getSubContent() {
        return subContent;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setSubContent(String subContent) {
        this.subContent = subContent;
    }

    @Override
    public String toString() {
        return "filename:"+filename+";charset:"+charset+";encoding:"+encoding+";content:"+subContent+";bytes:"+attachment;
    }
}
