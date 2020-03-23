package com.example.email.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.email.model.EmailInfo;

public interface EmailMapper {

    /*******查询指定用户邮件数据********/
    @Select("select * from email_table where user_id = #{id}")
    public List<EmailInfo> selectEmailByUserid(int id);

    /*******查询符合关键字的邮件数据********/
    @Select("select * from email_table where user_id = #{id} and subject like #{keyword}")
    public List<EmailInfo> selectEmailByKeyword(int id,String keyword);

    /*******添加新邮件********/
    @Insert("insert into emai_table(user_id,subject,content,date,from,to,attachment) values (#{user_id},#{subject},#{content},#{date},#{from},#{to},#{attachment})")
    public int addEmail(EmailInfo email);

}
