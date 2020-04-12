package com.example.email.controller;


import com.example.email.model.ResultModel;
import com.example.email.util.ResultTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;


@RestController
@RequestMapping(value="/smtp")
public class SmtpController {

    private Socket client = null;
    private String userMail = null;
    private String userPwd = null;
    private String recipient = null;
    private String cc = null;
    private String bcc = null;
    private String mailSubject = null;
    private String mailContent = null;
    private List<String> attachList=new ArrayList<String>();;
    private static Map<String, String> attachTypeMap;
    private String datePattern=null;

    private BufferedReader in = null;
    private DataOutputStream out = null;
    private Base64.Encoder encoder = Base64.getEncoder();

    //文件格式
    static {
        attachTypeMap = new HashMap<String, String>();
        attachTypeMap.put("xls", "application/vnd.ms-excel");
        attachTypeMap.put("xlsx", "application/vnd.ms-excel");
        attachTypeMap.put("xlsm", "application/vnd.ms-excel");
        attachTypeMap.put("xlsb", "application/vnd.ms-excel");

        attachTypeMap.put("doc", "application/msword");
        attachTypeMap.put("dot", "application/msword");
        attachTypeMap.put("docx", "application/msword");
        attachTypeMap.put("docm", "application/msword");
        attachTypeMap.put("dotm", "application/msword");

        attachTypeMap.put("ppt","application/vnd.ms-powerpoint");
        attachTypeMap.put("pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation");
        attachTypeMap.put("pps","application/vnd.ms-powerpoint");
        attachTypeMap.put("ppsx","application/vnd.openxmlformats-officedocument.presentationml.slideshow");

        attachTypeMap.put("pdf", "application/pdf");
        attachTypeMap.put("rar", "application/octet-stream");
        attachTypeMap.put("zip", "application/x-zip-compressed");

        attachTypeMap.put("jpe", "image/jpeg");
        attachTypeMap.put("jpeg", "image/jpeg");
        attachTypeMap.put("jpg", "image/jpeg");
        attachTypeMap.put("png", "image/png");

    }

    @GetMapping(value = "/sendEmail")
    public ResultModel sendEmail(HttpServletRequest request) {
        recipient = "957529483@qq.com,AsteriaChiang@outlook.com";
                //request.getParameter("recipient");
        cc = request.getParameter("cc");
        bcc = request.getParameter("bcc");
        userMail = "957529483@qq.com";
                //request.getParameter("userMail");
        userPwd ="urxztsypbwsebeej";
                //request.getParameter("userPwd");
        mailSubject="test subject";
                //request.getParameter("mailSubject");
        mailContent="test content";
                //request.getParameter("mailContent");
        //datePattern="yyyy-MM-dd HH:mm:ss";
        attachList.add("/Users/asteriachiang/Desktop/test.docx");

        if(userMail == null || userPwd == null || recipient==null){
            return ResultTools.result(2001, "", null);
        }

        String mailServer = "smtp." + userMail.substring(userMail.lastIndexOf("@") + 1);
        try{
            client = new Socket(mailServer, 25);
            //IO流
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new DataOutputStream(client.getOutputStream());

            getReturn(in);
            send(out,"HELO smtp");
            getReturn(in);
            getReturn(in);
            getReturn(in);
            send(out,"auth login");
            getReturn(in);

            //name&pwd
            send(out,new String(encoder.encode(userMail.getBytes())));
            getReturn(in);
            send(out,new String(encoder.encode(userPwd.getBytes())));
            getReturn(in);

            //receiver
            send(out,"MAIL FROM:<"+userMail+">");
            getReturn(in);
            splitAddress(recipient);
            splitAddress(cc);
            splitAddress(bcc);

            //DATA
            send(out,"DATA");
            getReturn(in);
            send(out, "Subject: "+mailSubject);
            send(out, "From:<957529483@qq.com>");
            send(out,"To: <"+recipient+">");
            if(cc!=null){
                send(out,"Cc: <"+cc+">");
            }
            if(bcc!=null){
                send(out,"Bcc: <"+cc+">");
            }

//            SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
//            send(out,"Date:"+sdf.format(new Date()));
            send(out,"Content-Type: multipart/mixed; boundary=b");
            //Text
            send(out,"--b");
            send(out,"Content-Type:text/html");
            send(out,"\r\n");
            send(out, mailSubject);
            send(out,"--b");


            if(attachList!=null){
                int attachsize=attachList.size();
                for(int i=0;i<attachsize;i++){
                    String filePath=attachList.get(i);
                    attachment(filePath);
                }
            }
            //attachment("/Users/asteriachiang/Desktop/test.docx");

            send(out, ".");
            send(out,"QUIT");
            client.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return ResultTools.result(200, "发送成功", null);
    }

    //送达多个邮箱
    public void splitAddress(String addressString){
        if(addressString!=null){
            String[] addressList=addressString.split(",");
            for(int i=0;i<addressList.length;i++){
                send(out,"RCPT TO: <"+addressList[i]+">");
                getReturn(in);
            }
        }
    }

    //发送单个附件
    public void attachment(String filePath){
        try{
            File file = new File(filePath);
            long fileSize = file.length();
            FileInputStream fi = new FileInputStream(file);
            byte[] buffer = new byte[(int) fileSize];
            int offset = 0;
            int numRead = 0;
            while (offset < buffer.length
                    && (numRead = fi.read(buffer, offset, buffer.length - offset)) >= 0) {
                offset += numRead;
            }
            // 确保所有数据均被读取
            if (offset != buffer.length) {
                throw new IOException("Could not completely read file "
                        + file.getName());
            }
            fi.close();

            String filename=file.getName();
            String fileType=getFileType(filename);
            send(out,"Content--Type:"+ fileType+"; name=\""+filename+"\"");
            send(out,"Content-Disposition: attachment; filename=\""+filename+"\"");
            send(out,"Content-Transfer-Encoding: base64");
            send(out,"\r\n");
            send(out,new String(encoder.encode(buffer)));
            send(out,"--b");
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    //获得文件格式
    public String getFileType(String name){
        String fileType=null;
        if (null != name) {
            int flag = name.lastIndexOf(".");
            if (0 <= flag && flag < name.length() - 1) {
                name = name.substring(flag + 1);
            }
            fileType = attachTypeMap.get(name);
        }

        return fileType;
    }

    //发送命令
    public void send(DataOutputStream out, String s){
        try{
            out.writeBytes(s+"\r\n");
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //返回结果
    public void getReturn(BufferedReader in) {
        String s="";
        try {
            s = in.readLine();
        }catch (Exception e) {
            e.printStackTrace();
        }
        //return s;
    }


}
