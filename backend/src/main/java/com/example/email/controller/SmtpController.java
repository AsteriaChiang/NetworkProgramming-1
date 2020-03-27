package com.example.email.controller;


import com.example.email.model.ResultModel;
import com.example.email.util.ResultTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
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
            if(mailSubject!=null){
                send(out, "Subject: "+mailSubject);
            }
            send(out, mailContent);
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
