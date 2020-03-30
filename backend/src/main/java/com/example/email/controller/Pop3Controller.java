package com.example.email.controller;

import com.example.email.model.ResultModel;
import com.example.email.util.ResultTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@RestController
@RequestMapping(value="/pop3")
public class Pop3Controller {

    private Socket socket = null;
    private BufferedReader in = null;
    private BufferedWriter out = null;
    private String userMail = null;
    private String userPwd = null;
    private String mailServer = null;

    @RequestMapping(value = "/auth")
    public ResultModel authUser(HttpServletRequest request){

        userMail = request.getParameter("userMail");
        userPwd = request.getParameter("userPwd");
        mailServer = "pop." + userMail.substring(userMail.lastIndexOf("@") + 1);

        //建立连接
        try{
            socket = new Socket(mailServer,110);//在新建socket的时候就已经与服务器建立了连接
        }catch(Exception e){
            return ResultTools.result(404, e.getMessage(), null);
        }
        System.out.println("建立连接！");
        try{
            in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            if(!user(userMail,in,out))
                return ResultTools.result(404, "用户名错误", null);

            if(!pass(userPwd,in,out))
                return ResultTools.result(404, "密码错误", null);

        }catch (IOException e){
            return ResultTools.result(404, e.getMessage(), null);
        }

        return ResultTools.result(200, "认证用户成功！", null);
    }

    @RequestMapping(value="/receiveEmail")
    public ResultModel receiveMail(HttpServletRequest request){
        int mailNum = 0;
        if(socket == null)
            return ResultTools.result(404, "尚未进行认证！", null);

        try {
            mailNum = stat(in,out);
            System.out.println("stat 命令执行完毕！");
            list(in,out);
            System.out.println("list 命令执行完毕！");
            //获取全部邮件信息
            for (int i = 1; i < mailNum + 1; i++) {//依次打印出邮件的内容
                System.out.println("以下为第" + i + "封邮件的内容");
                retr(i,in,out);
            }
            System.out.println("retr 命令执行完毕！");
            quit(in,out);
            System.out.println("quit 命令执行完毕！");
        }catch(IOException e){
            return ResultTools.result(404, e.getMessage(), null);
        }
        return ResultTools.result(200, "", null);
    }

    @RequestMapping(value = "/deleteMail")
    public ResultModel receiveMail(int id) {
        String result = getResult(getReturn(in));

        //先检测连接服务器是否已经成功
        if(!"+OK".equals(result)){
            System.out.println("服务器连接失败！");
        }
        //写入dele命令
        try{
            result = getResult(sendServer("dele "+id, in, out));
        }catch (IOException e){
            System.out.println(e.getMessage());
        }

        if(!"+OK".equals(result)){
            System.out.println("删除失败！");
            return ResultTools.result(404, "删除失败！", null);
        }
        return ResultTools.result(200, "删除成功！", null);
    }


            //得到服务器返回的一行命令
   public String getReturn(BufferedReader in){
        String line="";
        try{
            line=in.readLine();
            System.out.println("服务器返回状态:"+line);

        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        return line;
    }

    //得到服务器的返回状态码(+OK或者-ERR)
    public String getResult(String line){
        StringTokenizer st = new StringTokenizer(line," ");
        if(st.hasMoreTokens())
            return st.nextToken();
        else
            return "";
    }

    //发送命令
    private String sendServer(String str,BufferedReader in,BufferedWriter out) throws IOException{
        out.write(str);//发送命令
        out.newLine();//发送空行
        out.flush();//清空缓冲区
        System.out.println("已发送命令:"+str);
        return getReturn(in);
    }

    //user命令
    public boolean user(String user,BufferedReader in,BufferedWriter out){
        String result = getResult(getReturn(in));

        //先检测连接服务器是否已经成功
        if(!"+OK".equals(result)){
            System.out.println("服务器连接失败！");
            return false;
        }

        //写入user命令
        try{
            result = getResult(sendServer("user "+user,in,out));
        }catch (IOException e){
            System.out.println(e.getMessage());
            return false;
        }

        //检查user命令是否成功
        if(!"+OK".equals(result)){
            System.out.println("用户名错误！");
            return false;
        }
        return  true;
    }


    //pass命令
    public boolean pass(String password,BufferedReader in,BufferedWriter out){
        String result = null;
        try{
            result = getResult(sendServer("pass "+password,in,out));
        }catch (IOException e){
            System.out.println(e.getMessage());
            return false;
        }

        if(!"+OK".equals(result)){
            System.out.println("密码错误！");
            return false;
        }
        return  true;
    }

    //stat命令
    //请求服务器发回关于邮箱的统计资料，如邮件总数和总字节数
    public int stat(BufferedReader in,BufferedWriter out)throws IOException{
        if(socket.isClosed())
            System.out.println("已断开连接");
        String line = sendServer("stat",in,out);
        StringTokenizer st = new StringTokenizer(line," ");
        String result = st.nextToken();
        int mailNum = 0;
        if(st.hasMoreTokens())
            mailNum = Integer.parseInt(st.nextToken());
        if(!"+OK".equals(result)){
            throw new IOException("查看邮箱状态出错!");
        }
        System.out.println("共有邮件"+mailNum+"封");
        return mailNum;

    }

    //无参数list命令
    //返回邮件的编号和邮件的字节数
    public void list(BufferedReader in,BufferedWriter out) throws IOException{
        String line = sendServer("list",in,out);
        while(!".".equalsIgnoreCase(line)){
            line = in.readLine();
            System.out.println(line);
        }
    }

    //retr命令
    //得到邮件的完整信息
    public void retr(int mailNum,BufferedReader in,BufferedWriter out){
        if(socket.isClosed()){
            try{
                socket = new Socket(mailServer,110);//在新建socket的时候就已经与服务器建立了连接
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
            try{
                in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                user(userMail,in,out);
                pass(userPwd,in,out);

            }catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
        String result = null;
        try{
            result = getResult(sendServer("retr "+ mailNum, in, out));
        }catch (IOException e){
            System.out.println("retr命令出错！");
        }
        if(!"+OK".equals(result)){
            System.out.println("接收邮件出错！");
        }
        System.out.println("第" + mailNum + "封");
        StringBuffer emailContent = null;

        //解析邮件内容
        StringBuilder receive=new StringBuilder();
        String tempStr;
        try {
            while(true)
            {
                tempStr = in.readLine();
                receive.append("\r\n");
                receive.append(tempStr);
                if(tempStr.equals("."))
                {
                    break;
                }
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }

        // /s:匹配任何空白字符，包括空格、制表符、换页符等。
        // [\s\S]*:匹配任意字符包括换行符
        String from,to,subject,type,subtype,flag;
        Pattern pattern = Pattern.compile("From:[\\s\\S]*<([\\s\\S]*)>/r/n");
        Matcher matcher = pattern.matcher(receive);
        if (matcher.find())
            System.out.println("from:"+matcher.group(1));

    }


    //退出
    public void quit(BufferedReader in,BufferedWriter out) throws IOException{
        String result = null;
        try{
            result = getResult(sendServer("QUIT", in, out));
        }catch (IOException e){
            System.out.println("quit命令出错！");
        }
        if(!"+OK".equals(result)){
            throw new IOException("未能正确退出");
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    @RequestMapping(value = "/saveFile")
    public static synchronized boolean SaveFile(byte[] Data, String Path, String FileName) {
        File saveFile = new File(Path, FileName);
        try {
            saveFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(saveFile);
            fileOutputStream.write(Data);
            fileOutputStream.flush();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
