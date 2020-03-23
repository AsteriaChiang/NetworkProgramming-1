package com.example.email.controller;

import com.example.email.mapper.EmailMapper;
import com.example.email.model.*;
import com.example.email.util.ResultTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class EmailController {

    @Autowired
    private EmailMapper emailMapper;

    @RequestMapping(value = {"/selectEmailByUserid"}, method = RequestMethod.GET)
    public ResultModel selectEmailByUserid(Integer id){
        try {
            if (null == id) {
                return ResultTools.result(1001, "", null);
            }
            List<EmailInfo> emails = emailMapper.selectEmailByUserid(id);
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("content", emails);
            return ResultTools.result(200, "", map);
        } catch (Exception e) {
            return ResultTools.result(404, e.getMessage(), null);
        }
    }

    @RequestMapping(value = {"/selectEmailByKeyword"}, method = RequestMethod.GET)
    public ResultModel selectUserById(Integer id,String keyword){
        try {
            if (null == keyword || null == id) {
                return ResultTools.result(1001, "", null);
            }
            List<EmailInfo> emails = emailMapper.selectEmailByKeyword(id,"%"+keyword+"%");
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("content", emails);
            return ResultTools.result(200, "", map);
        } catch (Exception e) {
            return ResultTools.result(404, e.getMessage(), null);
        }
    }

}
