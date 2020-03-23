package com.example.email.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.email.model.UserInfo;

public interface UserMapper {

    /*******查询所有用户数据********/
    @Select("select * from user_table")
    public List<UserInfo> selectUserByAll();

    /*******根据id查询符合用户********/
    @Select("select * from user_table where user_id = #{id}")
    public UserInfo selectUserById(int id);

    /*******根据name查询符合用户********/
    @Select("select * from user_table where user_name=#{name}")
    public List<UserInfo> selectUserByName(String name);

    /*******添加新用户********/
    @Insert("insert into user_table(user_name,password,auth) values (#{name},#{password},#{auth})")
    public int addUser(UserInfo user);

}
