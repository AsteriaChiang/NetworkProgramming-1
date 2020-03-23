package com.example.email.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.email.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.email.model.*;
import com.example.email.util.ResultTools;


@RestController
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @RequestMapping(value = {"/selectUserByAll"}, method = RequestMethod.GET)
    public ResultModel selectUserByAll() {
        try {
            List<UserInfo> userLs = userMapper.selectUserByAll();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("content", userLs);
            return ResultTools.result(200, "", map);
        } catch (Exception e) {
            return ResultTools.result(404, e.getMessage(), null);
        }
    }

    @RequestMapping(value = {"/selectUserByName"}, method = RequestMethod.GET)
    public ResultModel selectUserByName(String name){
        try {
            if (null == name) {
                return ResultTools.result(1001, "", null);
            }
            List<UserInfo> userLs = userMapper.selectUserByName(name);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("content", userLs);
            return ResultTools.result(200, "", map);
        } catch (Exception e) {
            return ResultTools.result(404, e.getMessage(), null);
        }
    }

    @RequestMapping(value = {"/selectUserById"}, method = RequestMethod.GET)
    public ResultModel selectUserById(Integer id){
        try {
            if (null == id) {
                return ResultTools.result(1001, "", null);
            }
            UserInfo user = userMapper.selectUserById(id);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("content", user);
            return ResultTools.result(200, "", map);
        } catch (Exception e) {
            return ResultTools.result(404, e.getMessage(), null);
        }
    }

}
