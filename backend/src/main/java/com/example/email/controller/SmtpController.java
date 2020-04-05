package com.example.email.controller;


import com.example.email.model.ResultModel;
import com.example.email.util.ResultTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.Socket;
import java.util.Base64;

@RestController
@RequestMapping(value="/smtp")
public class SmtpController {

    @GetMapping(value = "/sendEmail")
    public ResultModel sendEmail(HttpServletRequest request) {
        String recipient = request.getParameter("recipient");
        String userMail = request.getParameter("userMail");
        String userPwd = request.getParameter("userPwd");
        String mailSubject=request.getParameter("mailSubject");
        String mailContent=request.getParameter("mailContent");

        if(userMail == null || userPwd == null || recipient==null){
            return ResultTools.result(2001, "", null);
        }

        return emailProgress(recipient,userMail,userPwd,mailSubject,mailContent);
    }

    //发送邮件
    public ResultModel emailProgress(String recipient,String userMail,String userPwd,String mailSubject,String mailContent){
        String mailServer = "smtp." + recipient.substring(recipient.lastIndexOf("@") + 1);
        try{
            Socket client = new Socket(mailServer, 25);
            //IO流
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            in.readLine();
            send(in,out,"HELO smtp");
            in.readLine();
            in.readLine();
            send(in,out,"auth login");
            //name&pwd
            Base64.Encoder encoder = Base64.getEncoder();
            send(in,out,new String(encoder.encode(userMail.getBytes())));
            send(in,out,new String(encoder.encode(userPwd.getBytes())));

            send(in,out,"MAIL FROM:<"+recipient+">");
            send(in,out,"RCPT TO: <"+userMail+">");
            //DATA
            send(in,out,"DATA");
            send(out, "Subject: "+mailSubject);
            send(out,"Content-Type: multipart/mixed; boundary=b");
            //Text
            send(out,"--b");
            send(out,"Content-Type:text/html");
            send(out, mailContent);
            send(out,"--b");

            //attachment
            File file = new File("/Users/asteriachiang/Desktop/test.docx");
            InputStream attachmentStream=new FileInputStream(file);
            byte[] bs = null;
            if (null != attachmentStream) {
                int buffSize = 1024;
                byte[] buff = new byte[buffSize];
                byte[] temp;
                bs = new byte[0];
                int readTotal = 0;
                while (-1 != (readTotal = attachmentStream.read(buff))) {
                    temp = new byte[bs.length];
                    System.arraycopy(bs, 0, temp, 0, bs.length);
                    bs = new byte[temp.length + readTotal];
                    System.arraycopy(temp, 0, bs, 0, temp.length);
                    System.arraycopy(buff, 0, bs, temp.length, readTotal);
                }
            }
            String filename=file.getName();
            send(out,"Content--Type: application/msword; name=\""+filename+"\"");
            send(out,"Content-Disposition: attachment; filename=\""+filename+"\"");
            send(out,"Content-Transfer-Encoding: base64");
            send(out,"\r\n");
            send(out,new String(encoder.encode(bs)));
            send(out,"--b");

            send(out, ".");
            send(out,"QUIT");
            client.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return ResultTools.result(200, "", null);
    }

    public void send(DataOutputStream out, String s){
        try{
            out.writeBytes(s+"\r\n");
            out.flush();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    //read info and input order
    public void send(BufferedReader in, DataOutputStream out, String s) {
        try {
            out.writeBytes(s + "\r\n");
            out.flush();
            s = in.readLine();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


}
