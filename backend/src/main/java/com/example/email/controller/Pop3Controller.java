package com.example.email.controller;

import com.example.email.model.EmailInfo;
import com.example.email.model.ResultModel;
import com.example.email.model.SubContentInfo;
import com.example.email.util.ResultTools;
import com.example.email.util.QuotedPrintable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private ArrayList<EmailInfo> emails = new ArrayList<>();
    private String charset = "";
    private String encoding = "";

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

            if(!user(userMail))
                return ResultTools.result(404, "用户名错误", null);

            if(!pass(userPwd))
                return ResultTools.result(404, "密码错误", null);

        }catch (IOException e){
            return ResultTools.result(404, e.getMessage(), null);
        }
        return ResultTools.result(200, "认证用户成功！", null);
    }

    @RequestMapping(value="/receiveEmail")
    public ResultModel receiveMail(HttpServletRequest request){
        //清空emails列表
        emails.clear();
        int mailNum = 0;
        if(socket == null)
            return ResultTools.result(404, "尚未进行认证！", null);

        try {
            mailNum = stat();
            System.out.println("stat 命令执行完毕！");
            for (int i = 1; i < mailNum + 1; i++) {
                EmailInfo email = retr(i);
                emails.add(email);
            }
            System.out.println("retr 命令执行完毕！");
        }catch(IOException e){
            return ResultTools.result(404, e.getMessage(), null);
        }
        return ResultTools.result(200, "", emails);
    }

    @RequestMapping(value = "/deleteMail")
    public ResultModel deleteMail(@RequestParam("id") String id) {
        String result = null;
        // 接收包含stuId的字符串，并将它分割成字符串数组
        String[] idList = id.split(",");
        for(String i : idList){
            //写入dele命令
            try{
                result = getResult(sendServer("dele "+i));
            }catch (IOException e){
                System.out.println(e.getMessage());
            }

            if(!"+OK".equals(result)){
                System.out.println("删除失败！");
                return ResultTools.result(404, "第"+i+"封邮件删除失败！", null);
            }
        }
        return ResultTools.result(200, "删除成功！", null);
    }

    //退出
    @RequestMapping(value = "/quit")
    public void quit() throws IOException{
        String result = null;
        try{
            result = getResult(sendServer("QUIT"));
        }catch (IOException e){
            System.out.println("quit命令出错！");
        }
        if(!"+OK".equals(result))   throw new IOException("未能正确退出");
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
    public static synchronized ResultModel saveFile(@RequestParam(value = "attachment") String base64, @RequestParam(value = "path") String path) {
        if (base64 == null && path == null) {
            return new ResultTools().result(404,"缺失文件参数",null);
        }
        File saveFile = new File(path);
        try {
            saveFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(saveFile);
            fileOutputStream.write(Base64.getDecoder().decode(base64));
            fileOutputStream.flush();
            return new ResultTools().result(200,"下载成功",null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return new ResultTools().result(404,e.getMessage(),null);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResultTools().result(404,e.getMessage(),null);
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

    //得到服务器返回的一行命令
   public String getReturn(){
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
    private String sendServer(String str) throws IOException{
        out.write(str);//发送命令
        out.newLine();//发送空行
        out.flush();//清空缓冲区
        System.out.println("已发送命令:"+str);
        return getReturn();
    }

    //user命令
    public boolean user(String user){
        String result = getResult(getReturn());

        //先检测连接服务器是否已经成功
        if(!"+OK".equals(result)){
            System.out.println("服务器连接失败！");
            return false;
        }

        //写入user命令
        try{
            result = getResult(sendServer("user "+user));
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
    public boolean pass(String password){
        String result = null;
        try{
            result = getResult(sendServer("pass "+password));
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
    public int stat()throws IOException{
        if(socket.isClosed())
            System.out.println("已断开连接");
        String line = sendServer("stat");
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
    public void list() throws IOException{
        String line = sendServer("list");
        while(!".".equalsIgnoreCase(line)){
            line = in.readLine();
            System.out.println(line);
        }
    }

    //retr命令
    //得到邮件的完整信息
    public EmailInfo retr(int mailNum){
        EmailInfo email = new EmailInfo();
        if(socket.isClosed()){
            try{
                socket = new Socket(mailServer,110);//在新建socket的时候就已经与服务器建立了连接
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
            try{
                in=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                user(userMail);
                pass(userPwd);

            }catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
        String result = null;
        List<String> content = new ArrayList<>();
        try{
            result = getResult(sendServer("retr "+ mailNum));
        }catch (IOException e){
            System.out.println("retr命令出错！");
        }
        if(!"+OK".equals(result)){
            System.out.println("接收邮件出错！");
        }
        System.out.println("第" + mailNum + "封");

        //解析邮件内容
        String tempStr;
        try {
            while(true)
            {
                tempStr = in.readLine();
                content.add(tempStr);
                if(tempStr.equals("."))
                {
                    System.out.println(content);
                    break;
                }
            }
            if(content!=null){
                email = handleContent(mailNum,content);
            }
        }catch (IOException e){
            System.out.println(e.getMessage());
        }

        return email;
    }



    public EmailInfo handleContent(int mailNum, List<String> content) {
        boolean isBody = false;
        boolean isPart = false;
        String body = "";
        String rawBody = "";
        EmailInfo email = new EmailInfo();
        email.setId(mailNum);
        List<SubContentInfo> subContents = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            String buf = content.get(i);
            if(isBody){
                if(buf.equals("--"+email.getBoundary()+"--")){
                    break;
                }
                //如果是text类型
                if(email.getContentType().contains("text")){

                    if(buf.equals(".")){
                        rawBody.replaceAll("(\\n|\\r\\n|\\n\\r)","");
                        body = convert(rawBody,charset,encoding);
                        if(email.getContentType().contains("html")){
                            body = body.replaceAll("<.*>", "");
                        }
                        break;
                    }else{
                        rawBody += buf;
                        continue;
                    }

                }else{

                    //不是text类型，是multipart类型
                    //如果当前行是boundary，说明子内容开始
                    if(buf.equals("--"+email.getBoundary())){
                        isPart = true;
                    }else if(isPart!=true){
                        continue;
                    }


                    SubContentInfo subContent= new SubContentInfo();
                    String partContent = "";
                    String attachment = "";  //附件内容

                    i++;
                    buf = content.get(i);

                    //子内容有可能为空
                    if(buf.length()==0){
                        while(!buf.contains(email.getBoundary())){
                            i++;
                            buf = content.get(i);
                        }
                        isPart = false;
                        continue;
                    }

                    String regex1 = "Content-Type: (.*);";
                    Pattern p1 = Pattern.compile(regex1);
                    Matcher m1 = p1.matcher(buf);
                    if (m1.find()) {
                        subContent.setContentType(m1.group(1));
                        System.out.println("type:"+subContent.getContentType());
                    }else{
                        break;
                    }


                    if(subContent.getContentType().contains("text")){
                        //找到该子内容的字符集
                        String regex2 = "charset=(.*)";
                        Pattern p2 = Pattern.compile(regex2);
                        Matcher m2 = p2.matcher(buf);
                        if(m2.find()){
                            subContent.setCharset(m2.group(1).replace("\"",""));
                        }else{
                            i++;
                            buf = content.get(i);
                            m2 = p2.matcher(buf);
                            if(m2.find())
                                subContent.setCharset(m2.group(1).replace("\"",""));
                        }
                        if(subContent.getCharset()!=null){
                            i++;
                            buf = content.get(i);
                        }
                    }

                    //附件
                    if((subContent.getCharset()==null&&subContent.getContentType().contains("text"))||subContent.getContentType().contains("image")||subContent.getContentType().contains("application")){
                        while(!buf.equals("")){
                            if(buf.startsWith("Content-Transfer-Encoding")){
                                subContent.setEncoding(buf.substring(27));
                            }else if(buf.contains("filename")){
                                String regex3 = "filename=(.*)";
                                Pattern p3 = Pattern.compile(regex3);
                                Matcher m3 = p3.matcher(buf);
                                if(m3.find()){
                                    //filename = m3.group(1);
                                    subContent.setFilename(convertToChinese(m3.group(1).replace("\"","")));
                                    System.out.println("filename="+subContent.getFilename());
                                }
                            }
                            i++;
                            buf = content.get(i);
                        }
                        i++;  //跳过空行
                        buf = content.get(i);
                        StringBuilder sb = new StringBuilder();
                        while(!buf.equals("")&&!buf.contains(email.getBoundary())){
                            sb.append(buf);
                            i++;
                            buf = content.get(i);
                        }
                        attachment = sb.toString().replaceAll("(\\n|\\r\\n|\\n\\r)","");
                        subContent.setAttachment(attachment);
                    }else if(subContent.getContentType().contains("plain")||(subContents.size()!=0&&(subContents.get(0).getSubContent()).contains("Please view this mail in HTML format."))){
                        while(!buf.startsWith("Content-Transfer-Encoding")){
                            i++;
                            buf = content.get(i);
                        }
                        subContent.setEncoding(buf.substring(27));

                        while(buf.length()!=0){
                            i++;
                            buf = content.get(i);
                        }
                        i++;
                        buf = content.get(i);  //这部分内容的第一行

                        StringBuilder sb = new StringBuilder();
                        while(!buf.contains(email.getBoundary())){
                            sb.append(buf);
                            i++;
                            buf = content.get(i);
                        }
                        isPart = false;
                        partContent = sb.toString();
                        partContent.replaceAll("(\\n|\\r\\n|\\n\\r)","");
                        partContent = convert(partContent, subContent.getCharset(),subContent.getEncoding());
                        subContent.setSubContent(partContent);
                    }
                    i--;
                    subContents.add(subContent);
                }
                email.setSubContents(subContents);

            } else if (buf.startsWith("Subject:")) {
                String subject = convertToChinese(buf);
                //如果是英文，从Subject:后开始截取即可
                if (subject==null) subject = buf.substring(8);
                email.setSubject(subject);
                System.out.println("subject:"+subject);
            } else if (buf.startsWith("From:")) {
                String from = convertToChinese(buf);
                if (from==null){
                    from = buf.substring(5);
                } else {
                    String regex = "<(.*)>";
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(buf);
                    if (m.find())
                        from += m.group(1);
                }
                email.setFrom(from);
                System.out.println("from:"+from);
            }else if (buf.startsWith("Date:")) {
                email.setDate(buf.substring(5));
                System.out.println("date:"+buf.substring(5));
            } else if (buf.startsWith("Content-Type:")) {
                String regex1 = ": (.*);";
                Pattern p1 = Pattern.compile(regex1);
                Matcher m1 = p1.matcher(buf);
                if (m1.find())
                    email.setContentType(m1.group(1));
                System.out.println("content-type:"+email.getContentType());
                //如果是multipart类型，提取boundary
                if(email.getContentType().contains("multipart")){
                    String regex = "(?<=\").*?(?=\")";
                    Pattern p = Pattern.compile(regex);
                    Matcher m = p.matcher(buf);
                    if(m.find()){
                        email.setBoundary(m.group(0));
                    } else{
                        buf = content.get(++i);
                        m = p.matcher(buf);
                        if(m.find())
                            email.setBoundary(m.group(0));
                    }
                    System.out.println("boundary:"+email.getBoundary());
                }

            }else if(buf.length()==0){
                //如果这是一个空行
                isBody = true;
                continue;
            }

        }
        if(subContents.size()!=0){

            Iterator iterator = subContents.iterator();
            while(iterator.hasNext()){
                body += ((SubContentInfo)iterator.next()).getSubContent();
            }

        }
        email.setContent(body);
        return email;
    }


    /*
          =?gbk?B?和?=之间的内容为真实标题内容
          先将Base64转换成字节，然后再转换成GBK字符串
          =?UTF-8?B?
    */
    public String convertToChinese(String s){
        String result = null;
        if(s.contains("GBK")){
            charset = "GBK";
        }else if(s.contains("UTF-8")){
            charset = "UTF-8";
        }else{
            //不是中文的
            return s;
        }
        String regex1 = "\\?B\\?(.*)\\?=";
        String regex2 = "\\?Q\\?(.*)\\?=";
        Pattern p1 = Pattern.compile(regex1);
        Pattern p2 = Pattern.compile(regex2);
        Matcher m1 = p1.matcher(s);
        Matcher m2 = p2.matcher(s);

        if (charset.equals("GBK")) {
            //=?GBK=?B?=
            if(m1.find()){
                encoding = "base64";
                try{
                    byte[] b = Base64.getDecoder().decode(m1.group(1));
                    result = new String(b, "GBK");
                }catch (UnsupportedEncodingException e){
                }
            }else if(m2.find()){
                encoding = "quoted-printable";
                result = QuotedPrintable.decode(m2.group(1).getBytes(),"GBK");
            }

        }else if(charset.equals("UTF-8")){
            if(m1.find()){
                encoding = "base64";
                try{
                    byte[] b = Base64.getDecoder().decode(m1.group(1));
                    result = new String(b, "UTF-8");
                }catch (UnsupportedEncodingException e){
                }
            }else if(m2.find()){
                encoding = "quoted-printable";
                result = QuotedPrintable.decode(m2.group(1).getBytes(),"UTF-8");
            }
        }
        return result;
    }

    public String convert(String s,String charset,String encoding){
        String result = "";
        if(charset==null){
            System.out.println("charset:"+charset);
            if(encoding.equals("base64")){
                byte[] b = Base64.getDecoder().decode(s);
                System.out.println("文件信息："+b);
                result = b.toString();
            }
        }else if(charset.equals("GBK")){
            if(encoding.equals("base64")){
                try{
                    byte[] b = Base64.getDecoder().decode(s);
                    result = new String(b, "GBK");
                }catch (UnsupportedEncodingException e){ }
            }else{
                result = QuotedPrintable.decode(s.getBytes(),"GBK");
            }
        }else if(charset.equals("UTF-8")){
            if(encoding.equals("base64")){
                try{
                    byte[] b = Base64.getDecoder().decode(s);
                    result = new String(b, "UTF-8");
                }catch (UnsupportedEncodingException e){ }
            }else{
                result = QuotedPrintable.decode(s.getBytes(),"UTF-8");
            }
        }
        return result;
    }

}
