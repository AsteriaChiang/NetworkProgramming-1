package com.example.email.controller;


import com.example.email.model.ResultModel;
import com.example.email.util.ResultTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.Socket;
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
    private String attachFile=null;
    private static Map<String, String> attachTypeMap;

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
        userMail = request.getParameter("userMail");
        userPwd = request.getParameter("userPwd");
        recipient = request.getParameter("recipient");
        cc = request.getParameter("cc");
        bcc = request.getParameter("bcc");
        mailSubject=request.getParameter("mailSubject");
        mailContent=request.getParameter("mailContent");
        attachFile=request.getParameter("attachFile");


        try{
            String mailServer = "smtp." + userMail.substring(userMail.lastIndexOf("@") + 1);
            client = new Socket(mailServer, 25); //建立连接

            //IO流
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new DataOutputStream(client.getOutputStream());
            getReturn(in);

            //Helo
            if(mailServer.contains("qq.com")){
                send(out,"HELO smtp");
                getReturn(in);
                getReturn(in);
                getReturn(in);
            }else if(mailServer.contains("whu.edu")){
                send(out,"HELO "+userMail);
                getReturn(in);
            }

            //name&pwd
            send(out,"auth login");
            getReturn(in);
            if(!user(userMail)){
                return ResultTools.result(404, "用户名错误", null);
            }
            if(!pass(userPwd)){
                return ResultTools.result(404, "密码错误", null);
            }

            //usermail&recipient&cc&bcc
            send(out,"MAIL FROM:<"+userMail+">");
            if(!getReturn(in).startsWith("250")){
                return ResultTools.result(404, "MAIL FROM命令错误！", null);
            }
            splitAddress(recipient);
            splitAddress(cc);
            splitAddress(bcc);

            //DATA
            send(out,"DATA");
            if(!getReturn(in).startsWith("354")){
                return ResultTools.result(404, "DATA命令错误！", null);
            }
            //邮件主题
            if(mailSubject!=null){
                //如果是中文需要进行base64编码
                if(isChinese(mailSubject)){
                    mailSubject = new String(encoder.encode(mailSubject.getBytes("UTF-8")));
                    mailSubject = "=?UTF-8=?B?"+mailSubject+"?=";
                }
                send(out, "Subject: "+mailSubject);
            }
            //收件人&发件人
            send(out, "From: <"+userMail+">");
            send(out,"To: <"+recipient+">");
            //密送
            if(cc!=null){
                send(out,"Cc: <"+cc+">");
                System.out.println("抄送");
            }
            //抄送
            if(bcc!=null){
                send(out,"Bcc: <"+bcc+">");
                System.out.println("密送");
            }
            send(out,"Content-Type: multipart/alternative; boundary=\"b\"");
            send(out,"\r\n");
            //Text
            if(mailContent!=null){
                send(out,"--b");
                send(out,"Content-Type: text/plain; charset=\"UTF-8\"");
                send(out,"Content-Transfer-Encoding: base64");
                send(out,"\r\n");
                send(out, new String(encoder.encode(mailContent.getBytes("UTF-8"))));

            }
            //Attachment
            if(attachFile!=null){
                send(out,"--b");
                attachment(attachFile);
            }else{
                send(out,"--b--");
            }

            send(out, ".");
            if(!getReturn(in).startsWith("250")){
                return ResultTools.result(404, "未能成功发送！", null);
            }
            send(out,"QUIT");
            if(!getReturn(in).startsWith("221")){
                return ResultTools.result(404, "未能成功退出！", null);
            }
            client.close();
        }
        catch (Exception e){
            return ResultTools.result(404, e.getMessage(), null);
        }

        return ResultTools.result(200, "发送成功", null);
    }

    //user命令
    public boolean user(String user){
        send(out,new String(encoder.encode(user.getBytes())));
        String result = getReturn(in);
        if(!"334 UGFzc3dvcmQ6".equals(result)){
            System.out.println("用户名错误！");
            return false;
        }
        return  true;
    }


    //pass命令
    public boolean pass(String password){
        send(out,new String(encoder.encode(password.getBytes())));
        String result = getReturn(in);
        if(!"235 Authentication successful".equals(result)){
            System.out.println("密码错误！");
            return false;
        }
        return  true;
    }

    public static boolean isChinese(char c) {
        return c >= 0x4E00 &&  c <= 0x9FA5;// 根据字节码判断
    }

    // 判断一个字符串是否含有中文
    public static boolean isChinese(String str) {
        if (str == null) return false;
        for (char c : str.toCharArray()) {
            if (isChinese(c)) return true;// 有一个中文字符就返回
        }
        return false;
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
            send(out,"--b--");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
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
            System.out.println(e.getMessage());
        }
    }

    //返回结果
    public String getReturn(BufferedReader in) {
        String s="";
        try {
            s = in.readLine();
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return s;
    }


}
