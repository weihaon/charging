package com.example.application;
import Function.JwtUtil;
import com.example.application.Class.*;
import Function.function;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;

@RestController
public class Others {
    //token代表用户名
    @GetMapping("/pile/{pileId}")
    public String getPile(@PathVariable("pileId") int pileId,@RequestHeader("Authorization") String token)  throws IOException {

        // 验证和解析JWT Token
        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        Gson gson = new Gson();
        String json="";
        if(!function.isAdmin(token) ){
            GenericResponse result=new GenericResponse();
            result.code = 4;
            result.message = "没有权限";
            json = gson.toJson(result);
        }

        else if(function.getPile(pileId)!=null){
            PileStatus result=function.getPile(pileId);
            json = gson.toJson(result);
        }
        else{
            PileStatus result= new PileStatus();

            json = gson.toJson(result);
        }





        return json;
    }
//需要修改
    @PutMapping("/pile/{pileId}")
    public String changePile(@PathVariable("pileId") int pileId,@RequestBody UpdatePileStatusRequest status, @RequestHeader("Authorization") String token) throws IOException, InterruptedException {
        // 验证和解析JWT Token
        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");

        Gson gson = new Gson();
        String json="";
        synchronized (server.lock) {

            if (function.isAdmin(token)) {
                function.changePile(pileId, status.status);
                ChargePile newpile=function.getPileTable(pileId);


                json = gson.toJson(newpile);
            } else {
                GenericResponse result=new GenericResponse();
                result.code = 4;
                result.message = "没有权限";
                json = gson.toJson(result);
            }
        }


        return json;
    }

    @GetMapping("/pile/{pileId}/wait")
    public String infoPile(@PathVariable("pileId") int pileId,@RequestHeader("Authorization") String token)  {
        // 验证和解析JWT Token
        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        Gson gson = new Gson();
        String json="";
        ArrayList<PileWaitStatus> newpilewaits=new ArrayList<>();
        newpilewaits=function.getPileWait(pileId);
        if(!function.isAdmin(token) ){
            GenericResponse result=new GenericResponse();
            result.code = 4;
            result.message = "没有权限";
            json = gson.toJson(result);
        }
        else if( newpilewaits!=null){
            PileWait newwait=new PileWait();
            newwait.id =newpilewaits.get(0).id;
            newwait.users=new ArrayList<>();
            for(int i=0;i<newpilewaits.size();i++)
            {
                newwait.users.add(newpilewaits.get(i).users);
            }
            json = gson.toJson(newwait);
        }
        else {
            Tmp newwait= new Tmp();
            json = gson.toJson(newwait);
            //添加键user
        }

        return json;
    }

    @GetMapping("/piles")
    public String getPileTable(@RequestParam(value ="limit", defaultValue = "-1") int limit,@RequestParam(value = "skip", defaultValue = "0") int skip,@RequestHeader("Authorization") String token){
        // 验证和解析JWT Token
        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        //如果没有参数limit和skip，就默认为limit=5，skip=0

        Gson gson = new Gson();
        String json="";
        if(limit==-1){
            limit=5;
            skip=0;
        }
        if(limit+skip>5){
            limit=5;
            skip=0;
        }

        if(function.isAdmin(token)){
            ArrayList<ChargePile> newpilestatus=new ArrayList<>();
            for(int i=skip;i<skip+limit;i++) {
                //if(function.getPileTable(i)!=null)
                ChargePile newpile=function.getPileTable(i);


                newpilestatus.add(newpile);
            }
            json = gson.toJson(newpilestatus);

        }
        else{
                GenericResponse result=new GenericResponse();
                result.code = 4;
                result.message = "没有权限";
                json = gson.toJson(result);
        }


        return json;
    }
    //获取报表（所有充电桩的数据）
    @GetMapping("/report")
    public String getReport(@RequestParam("date") String date,@RequestHeader("Authorization") String token) {
        // 验证和解析JWT Token
        // 验证和解析JWT Token
        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        Gson gson = new Gson();
        String json="";

        if(function.isAdmin(token)){
            ArrayList<PileReport> newpilereports=new ArrayList<>();
            newpilereports=function.getReport(date);
            if(newpilereports==null){
                newpilereports=new ArrayList<>();
            }
            else
                json = gson.toJson(newpilereports);
            json = gson.toJson(newpilereports);

        }

        else{
                GenericResponse result=new GenericResponse();
                result.code = 4;
                result.message = "没有权限";
                json = gson.toJson(result);
        }

        return json;
    }
    @GetMapping("/ping")
    public String getPing() {
        Gson gson = new Gson();
        String json="";
        GenericResponse result= new GenericResponse();
        result.code=0;
        result.message="sccuess";
        json = gson.toJson(result);
        return json;
    }
    @GetMapping("/time")
    public String getTime() {
        Gson gson = new Gson();
        String json="";
        time time=new time();
        time.time=server.stamp+(long)server.time* 1000;
        json=gson.toJson(time);
        return json;
    }
    @PostMapping("/user/login")
    public String login(@RequestBody UserLoginRequest account) throws InterruptedException {
        //登录成功返回token


        Gson gson = new Gson();
        String json="";
        UserLoginResponse result= new UserLoginResponse();
        if (function.login(account.username,account.password)){
            result.code=0;
            result.expire="86400";
            result.token=JwtUtil.genJwt(account.username,account.password);
            json = gson.toJson(result);
        }
        else{
            result.code=1;
            result.expire="0";
            result.token="";
            json = gson.toJson(result);
        }


        return json;
    }
    @GetMapping("/user/refresh")
    public String refresh(@RequestHeader("Authorization") String token) throws JsonProcessingException {
        // 验证和解析JWT Token

        Claims claims=JwtUtil.getUserIdFromToken(token);
        //提取username
        String username = (String) claims.get("username");
        String password = (String) claims.get("password");
        Gson gson = new Gson();
        String json="";

        if (function.login(username,password)){
            UserLoginResponse result= new UserLoginResponse();
            result.code=0;
            result.expire="86400";
            result.token=JwtUtil.genJwt(username,password);
            json = gson.toJson(result);
        }
        else{
            GenericResponse result= new GenericResponse();
            result.code=1;
            result.message="登录失败";

            json = gson.toJson(result);
        }

        return json;




    }
    @PostMapping("/user/register")
    public String register(@RequestBody UserRegisterRequest account) throws InterruptedException {
//登录成功返回token

        Gson gson = new Gson();
        String json="";
        GenericResponse result= new GenericResponse();
        if (function.register(account.username,account.password)){
            result.code=0;
            result.message="注册成功";
            json = gson.toJson(result);
        }
        else{
            result.code=1;
            result.message="注册失败";
            json = gson.toJson(result);
        }

        return json;



    }

}