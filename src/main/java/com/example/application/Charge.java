package com.example.application;
import Function.JwtUtil;
import Function.function;
import com.example.application.Class.*;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import static Function.function.CountFee;
import static Function.function.databaseAccess;
import static com.example.application.server.*;

@RestController
public class Charge {
    //返回JSON格式
    @GetMapping("/charge")
    //返回JSON格式
    public String getChargeinfo( @RequestHeader("Authorization") String token)  {

        // 验证和解析JWT Token

        Claims claims= JwtUtil.getUserIdFromToken(token);

        //提取username
        token = (String) claims.get("username");
        ChargeUser myuser=new ChargeUser();
        myuser=databaseAccess.userReadByName(token);
        ChargeStatusResponse response=new ChargeStatusResponse();
        response=databaseAccess.chargestatusRead(myuser.id);
        Gson gson = new Gson();
        String json="";
        //判断是否在充电
        if(response !=null){

            json = gson.toJson(response);
        }
        else{
            GenericResponse result=new GenericResponse();
            result.code=4;
            result.message="找不到对应的充电状态";
            json = gson.toJson(result);
        }
        return json;
    }


    @DeleteMapping("/charge")
    public String deleteRequest( @RequestHeader("Authorization") String token) throws InterruptedException {
        // 验证和解析JWT Token
        synchronized (server.lock) {

            Claims claims = JwtUtil.getUserIdFromToken(token);
            //提取username
            token = (String) claims.get("username");
            Gson gson = new Gson();
            String json;
            ChargeUser myuser = new ChargeUser();
            myuser = databaseAccess.userReadByName(token);
            ChargeStatusResponse response = new ChargeStatusResponse();
            response = databaseAccess.chargestatusRead(myuser.id);
            GenericResponse result = new GenericResponse();
            if (Objects.equals(response.status, "充电中")) {
                result.code = 4;
                result.message = "正在充电，无法取消";
                json = gson.toJson(result);
            } else {
                //在对应队列中删除该用户
                //1.时间队列队列
                for (int k = 0; k < tables.size(); k++) {
                    if (tables.get(k).userId == myuser.id) {
                        tables.remove(k);
                    }
                }
                //充电区等待队列
                if (response.chargingArea) {
                    if (response.fast) {
                        Iterator<Integer> it = FastQueue[response.pile].iterator();
                        while (it.hasNext()) {
                            Integer i = it.next();
                            if (i.equals(myuser.id)) {
                                it.remove();
                            }
                        }
                    } else {
                        Iterator<Integer> it = SlowQueue[response.pile - FastChargingPileNum].iterator();
                        while (it.hasNext()) {
                            Integer i = it.next();
                            if (i.equals(myuser.id)) {
                                it.remove();
                            }
                        }
                    }
                    //构建充电事件
                    if (ErrorQueue.size() > 0) {
                        events newevent = new events();
                        newevent.kind = 3;
                        newevent.usrid = ErrorQueue.get(0);
                        ErrorQueue.remove(0);
                        event.add(newevent);
                    } else //出错队列不存在车辆，等候区存在车辆，需要进行调度
                    {
                        if (WaitQueue.size() > 0) {
                            //构造调度事件
                            events newevent = new events();
                            newevent.kind = 3;
                            newevent.usrid = WaitQueue.get(0);
                            //删除等待队列的第一个
                            WaitQueue.remove(0);
                            for (int i = 0; i < WaitQueue.size(); i++) {
                                UserChargeStatus.get(WaitQueue.get(i)).position--;
                                databaseAccess.chargestatusWriteback(UserChargeStatus.get(WaitQueue.get(i)));
                            }
                            event.add(newevent);
                        }

                    }
                }
                //等候区队列
                else {
                    Iterator<Integer> it = WaitQueue.iterator();
                    while (it.hasNext()) {
                        Integer i = it.next();
                        if (i.equals(myuser.id)) {
                            it.remove();
                        }
                    }
                }
                //删除充电申请
                chargeRequests.remove(myuser.id);
                //数据库中充电数据的删除
                databaseAccess.waitStatusDelete(myuser.id);
                databaseAccess.ChargeStatusDelete(myuser.id);
                result.code = 0;
                result.message = "取消成功";
                json = gson.toJson(result);
            }


            return json;
        }
    }



    @PostMapping("/charge")
    public String ChargeRequest(@RequestBody ChargeRequest message, @RequestHeader("Authorization") String token) throws InterruptedException {
//充电状态的绑定
        ChargeStatusResponse mystatus = new ChargeStatusResponse();
        ChargeUser myuser = new ChargeUser();
        Claims claims = JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        // 验证和解析JWT Token
        Gson gson = new Gson();
        String json = "";
// 验证和解析JWT Token
        synchronized (server.lock) {
            System.out.println(message.amount);
            myuser = databaseAccess.userReadByName(token);
            userID = myuser.id;//得到用户的Id
            ChargeRequest request = new ChargeRequest();
            request.fast = message.fast;
            request.amount = message.amount;
            request.totalAmount = message.totalAmount;
            mystatus.fast = message.fast;
            mystatus.totalAmount = message.totalAmount;
            mystatus.amount = message.amount;
            mystatus.userID = myuser.id;
            databaseAccess.chargestatusWriteback(mystatus);
            UserChargeStatus.put(userID, mystatus);
            //将用户Id和request绑定
            TimeUser.put(userID, server.time);
            chargeRequests.put(userID, request);
            //插入事件
            server.events newevent = new events();
            newevent.kind = 3;
            newevent.usrid = userID;
            event.add(newevent);
        }
        while(event.size()>0);
        synchronized (server.lock) {
            mystatus = databaseAccess.chargestatusRead(myuser.id);
            //尝试进行充电
            if (!UserChargeStatus.get(userID).chargingArea && !UserChargeStatus.get(userID).waitingArea) {
                GenericResponse result = new GenericResponse();
                result.code = 4;
                result.message = "排队错误,请检查您输入的内容是否正确";
                json = gson.toJson(result);
            } else {
                mystatus.amount=message.amount;
                mystatus.status = UserChargeStatus.get(userID).status;
                mystatus.chargingArea = UserChargeStatus.get(userID).chargingArea;
                mystatus.waitingArea = UserChargeStatus.get(userID).waitingArea;
                mystatus.pile = UserChargeStatus.get(userID).pile;
                mystatus.position = UserChargeStatus.get(userID).position;
                json = gson.toJson(mystatus);
            }
        }
        return json;
    }
    @PutMapping("/charge")
    public String ChangeRequest(@RequestHeader("Authorization") String token,@RequestBody ChargeRequest message) throws InterruptedException {
// 验证和解析JWT Token
        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        Gson gson = new Gson();
        String json="";
        //修改充电
        synchronized (server.lock) {
            GenericResponse result = new GenericResponse();
            ChargeUser user = new ChargeUser();
            user = databaseAccess.userReadByName(token);
            ChargeStatusResponse response = new ChargeStatusResponse();
            response = databaseAccess.chargestatusRead(user.id);
            //如果respone为空，说明用户没有充电
            if (response == null) {
                result.code = 4;
                result.message = "您没有充电";
                json = gson.toJson(result);
            }
            //修改充电
            else if (response.chargingArea) {
                result.code = 4;
                result.message = "您已在充电区，无法进行修改";
                json = gson.toJson(result);
            } else {
                int temp = WaitQueue.get(0);
                chargeRequests.get(user.id).amount = message.amount;
                response.amount = message.amount;
                if (chargeRequests.get(user.id).fast != message.fast) {
                    response.fast = message.fast;
                    databaseAccess.chargestatusWriteback(response);
                    WaitQueue.remove(0);
                    WaitQueue.add(temp);
                    if (message.fast) {
                        String wait = "F" + WaitQueue.size();
                        Number.put(temp, wait);
                    } else {
                        String wait = "T" + WaitQueue.size();
                        Number.put(temp, wait);
                    }

                } else {
                    databaseAccess.chargestatusWriteback(response);
                }
                json = gson.toJson(response);
            }


            return json;
        }
    }
    @GetMapping("/charge/bill/{billId}")
    public String ChargeBill(@PathVariable("billId") String billId, @RequestHeader("Authorization") String token) throws InterruptedException {
        // 验证和解析JWT Token
        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        Gson gson = new Gson();
        String json="";
        ChargeBill bill=new ChargeBill();
        System.out.println(billId);
        bill=databaseAccess.billReadByid(Integer.parseInt(billId));
        //查询是否存在该账单
        if(bill!=null){
            bill.updated_at = String.valueOf(server.time);
            json = gson.toJson(bill);
            bill.updated_at=server.getTime(bill.updated_at);

            bill.chargeStartTime=server.getTime(bill.chargeStartTime);
            bill.chargeEndTime=server.getTime(bill.chargeEndTime);
            bill.created_at=server.getTime(bill.created_at);
        }
        else{
            GenericResponse result=new GenericResponse();
            result.code=4;
            result.message="未找到充电订单";
            json = gson.toJson(result);
        }


        return json;
    }

    @GetMapping("/charge/bills")
    public String getBill(@RequestHeader("Authorization") String token) {
        // 验证和解析JWT Token

        Claims claims= JwtUtil.getUserIdFromToken(token);
        //提取username
        token = (String) claims.get("username");
        Gson gson = new Gson();
        String json="";
        ChargeUser myuser=new ChargeUser();
        myuser=databaseAccess.userReadByName(token);
        //查询是否存在该用户账单(有就返回，没有就报错)
        ArrayList<ChargeBill> bills=new ArrayList<>();

        for(int i=0;i<databaseAccess.ReadAllBill(myuser.id).size();i++)
        {
            ChargeBill bill=new ChargeBill();
            bill=databaseAccess.ReadAllBill(myuser.id).get(i);
            bill.updated_at = String.valueOf(server.time);
            bill.updated_at=server.getTime(bill.updated_at);
            bill.chargeEndTime=server.getTime(bill.chargeEndTime);
            bill.chargeStartTime=server.getTime(bill.chargeStartTime);
            bill.created_at=server.getTime(bill.created_at);
            bills.add(bill);
        }
        if(bills.size()!=0){
            json = gson.toJson(bills);
        }
        else{
            GenericResponse result=new GenericResponse();
            result.code=4;
            result.message="未找到充电订单";
            json = gson.toJson(result);
        }
        return json;
    }

    @PostMapping("/charge/finish")
    public String finishCharge(@RequestHeader("Authorization") String token) throws InterruptedException {
        // 验证和解析JWT Token
        synchronized (server.lock) {

            Claims claims = JwtUtil.getUserIdFromToken(token);
            //提取username
            token = (String) claims.get("username");

            Gson gson = new Gson();
            String json = "";
            ChargeUser myuser = new ChargeUser();
            myuser = databaseAccess.userReadByName(token);
            //判断是否在充电(已完成)
            if (function.isCharging(myuser.id)) {

                ChargeBill result = new ChargeBill();
                result = databaseAccess.billReadByUserId(myuser.id);
                result.chargeEndTime=String.valueOf(time-1);
                result.created_at =result.chargeStartTime;
                if(UserChargeStatus.get(myuser.id).fast) result.chargeAmount=30*(Double.valueOf(result.chargeEndTime)-Double.valueOf(result.chargeStartTime))/3600;
                else result.chargeAmount=15*(Double.valueOf(result.chargeEndTime)-Double.valueOf(result.chargeStartTime))/3600;
                result.serviceFee=0.8*result.chargeAmount;
                result.chargeFee=CountFee(Double.valueOf(result.chargeStartTime),Double.valueOf(result.chargeEndTime),UserChargeStatus.get(myuser.id).fast);
                DecimalFormat df=new DecimalFormat("0.00");
                result.totalFee=Double.valueOf(df.format(result.chargeFee+result.serviceFee));
                //向结束充电队列里添加充电桩
                server.endCharge.add(result.pileId);
                for (int k = 0; k < tables.size(); k++) {
                    if (tables.get(k).userId == myuser.id) {
                        tables.remove(k);
                    }
                }

                server.events newevent = new server.events();
                newevent.usrid = myuser.id;
                newevent.kind = 1;
                event.add(newevent);
                result.updated_at= String.valueOf(server.time);
                result.updated_at=server.getTime(result.updated_at);
                result.chargeEndTime=server.getTime(result.chargeEndTime);
                result.chargeStartTime=server.getTime(result.chargeStartTime);
                result.created_at=server.getTime(result.created_at);
                json = gson.toJson(result);
            } else {
                GenericResponse result = new GenericResponse();
                result.code = 4;
                result.message = "未在充电";
                json = gson.toJson(result);
            }


            return json;
        }
    }

}