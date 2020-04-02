package com.example.email.controller;

import com.example.email.model.ResultModel;
import com.example.email.util.ResultTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
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
        List<String> content = new ArrayList<>();
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
        //StringBuilder receive=new StringBuilder();
        String tempStr;
        try {
            while(true)
            {
                tempStr = in.readLine();
                //receive.append("\r\n");
                //receive.append(tempStr);
                content.add(tempStr);
                if(tempStr.equals("."))
                {
                    break;
                }
            }
            if(content!=null){
                handleContent(content);
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }


    }


    public void handleContent(List<String> content) {
        //决定当前行是否解码
        boolean decodeWithBase64 = false;
        //排除前一行的干扰
        boolean isPreLine = false;
        //是否已经收集完内容  不要html
        boolean isBody = false;
        boolean hasContent = false;
        HashMap<String, String> email = new HashMap<>();
        for (int i = 0; i < content.size(); i++) {
            String buf = content.get(i);
            isPreLine = false;
            if (buf.startsWith("Subject:")) {
                String subject = convertToChinese(buf);
                //如果是英文，从Subject:后开始截取即可
                if (buf.equals("")) subject = buf.substring(8);
                email.put("subject", subject);
            } else if (buf.startsWith("From:")) {
                String from = convertToChinese(buf);
                //如果是英文，从Subject:后开始截取即可
                if (buf.equals("")){
                    from = buf.substring(5);
                } else {
                    String regex = "<(.*)>";
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(buf);
                    if (m.find())
                        from += m.group(1);
                }
                email.put("from", from);
            }else if (buf.startsWith("To:")) {
                String to = convertToChinese(buf);
                if (buf.equals("")){
                    to = buf.substring(3);
                } else {
                    String regex = "<(.*)>";
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(buf);
                    if (m.find())
                        to += m.group(1);
                }
                email.put("to", to);
            } else if (buf.startsWith("Date:")) {
                email.put("date", buf.substring(5));
            } else if (buf.startsWith("Content-Type:")) {
                //Content-Type: text/plain; charset=UTF-8
                //要根据Content-Type来提取这一段的内容
                String regex1 = ": (.*);";
                Pattern p1 = Pattern.compile(regex1);
                Matcher m1 = p1.matcher(buf);
                if (m1.find())
                    email.put("content-type", m1.group(1));
                String regex2 = "charset=(.*)";
                Pattern p2 = Pattern.compile(regex2);
                Matcher m2 = p2.matcher(buf);
                if(m2.find())
                    email.put("content-type", m2.group(1));
            }

        }
    }


    /*
          =?gbk?B?和?=之间的内容为真实标题内容
          先将Base64转换成字节，然后再转换成GBK字符串
          =?UTF-8?B?
    */
    public String convertToChinese(String s){
        String result = "";
        boolean isGBK = false;
        boolean isUTF8 = false;
        // . 匹配除换行符 \n 之外的任何单字符
        String regex1 = "=\\?GBK\\?B\\?(.*)\\?=";
        String regex2 = "=\\?UTF-8\\?B\\?(.*)\\?=";
        Pattern p1 = Pattern.compile(regex1);
        Pattern p2 = Pattern.compile(regex2);
        Matcher m1 = p1.matcher(s);
        Matcher m2 = p2.matcher(s);
        if (m1.find()) {
            isGBK = true;
            try{
                byte[] b = Base64.getDecoder().decode(m1.group(1));
                result = new String(b, "GBK");
            }catch (UnsupportedEncodingException e){
                System.out.println(e.getMessage());
            }
        }else if(m2.find()){
            isUTF8 = true;
            try{
                byte[] b = Base64.getDecoder().decode(m1.group(1));
                result = new String(b, "UTF-8");
            }catch (UnsupportedEncodingException e){
                System.out.println(e.getMessage());
            }
        }
        return result;
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
