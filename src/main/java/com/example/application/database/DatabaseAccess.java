package com.example.application.database;

import com.example.application.Class.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.util.*;

@Component
public class DatabaseAccess {

    private final JdbcTemplate jdbcTemplate ;


    @Autowired
    public DatabaseAccess(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;



    }

    public ArrayList<ChargeStatusResponse> chargeStatusResponseReadAll()
    {
        String sql="SELECT * FROM chargestatus";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql);
        ArrayList<ChargeStatusResponse> list=new ArrayList<>();
        if(r==null || r.isEmpty())
        {
            return list;
        }
        else
        {
            for(int i=0;i<r.size();i++) {
                ChargeStatusResponse myChargestatus = new ChargeStatusResponse();
                myChargestatus.userID = (int) r.get(i).get("userID");
                myChargestatus.amount = (float) r.get(i).get("amount");
                myChargestatus.code = (int) r.get(i).get("code");
                //如果chargeingArea为1，chargingArea为true，否则为false
                if (Objects.equals((String) r.get(i).get("chargingArea"), "1")) myChargestatus.chargingArea = true;
                else myChargestatus.chargingArea = false;
                if (Objects.equals((String) r.get(i).get("fast"), "1")) myChargestatus.fast = true;
                else myChargestatus.fast = false;
                myChargestatus.pile = (int) r.get(i).get("pile");
                myChargestatus.position = (int) r.get(i).get("position");
                myChargestatus.status = (String) r.get(i).get("status");
                myChargestatus.totalAmount = (float) r.get(i).get("totalAmount");
                if (Objects.equals((String) r.get(i).get("waitingArea"), "1")) myChargestatus.waitingArea = true;
                else myChargestatus.waitingArea = false;
                list.add(myChargestatus);
            }
            return list;
        }
    }
    public  boolean login(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ?";
        List<Map<String, Object>> r= jdbcTemplate.queryForList(sql, username);
        if (r==null || r.isEmpty()) {
            // 不存在符合条件的记录
            return false;
        } else {
            // 存在符合条件的记录
            // 检查密码是否正确
            String correctPassword = (String) r.get(0).get("password");
            if (!correctPassword.equals(password)) {
                return false;
            }
            return true;
        }
    }

    public boolean register(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ?";
        List<Map<String, Object>> r= jdbcTemplate.queryForList(sql, username);
        //r转成json
        //

        if (r==null || r.isEmpty()) {
            // 不存在符合条件的记录
            //计算表的总行数
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            jdbcTemplate.update("INSERT INTO user(username, password,id) VALUES (?, ?, ?)", username, password,count+1);
            return true;
        } else {
            // 存在符合条件的记录
            return false;
        }
    }

    public int ChargePileFind()
    {
        String sql="SELECT * FROM pileline WHERE status=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,0);
        if(r==null || r.isEmpty())
        {
            return -1;
        }
        else
        {

            return Integer.parseInt((String) r.get(0).get("id"));
        }
    }
    public ChargePile ChargePileRead(int id) {
        String sql="SELECT * FROM pileline WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            ChargePile myChargepile = new ChargePile();
            myChargepile.created_at=(String)r.get(0).get("createdAt");
            myChargepile.deleted_at=(String)r.get(0).get("deletedAt");
            if(Objects.equals((String) r.get(0).get("fast"), "true")) myChargepile.fast=true;
            else myChargepile.fast=false;
            myChargepile.id=(int) r.get(0).get("id");
            myChargepile.status=(int)r.get(0).get("status");
            myChargepile.updated_at=(String)r.get(0).get("updatedAt");

            return myChargepile;
        }
    }
    public void ChargePileWriteback(ChargePile pile){
        String sql = "SELECT * FROM pileline WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,pile.id);
        if(r==null || r.isEmpty())
        {
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pileline", Integer.class);
            jdbcTemplate.update("INSERT INTO pileline(createdAt,deletedAt,fast,status,updatedAT,id) VALUES (?,?,?,?,?,?)",
                    pile.created_at,pile.deleted_at,pile.fast,pile.status,pile.updated_at,count+1);
        }
        else
        {
            jdbcTemplate.update("UPDATE  pileline SET createdAt=?,deletedAt=?,fast=?,status=?,updatedAT=? WHERE id=?"
            , pile.created_at,pile.deleted_at,pile.fast,pile.status,pile.updated_at,pile.id);
        }
    }
    public boolean ChargePileDelte(int id)
    {
        String sql="SELECT * FROM pileline WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return false;
        }
        else
        {
            String sql1="DELETE FROM pileline WHERE id=?";
            jdbcTemplate.update(sql1,id);
            return true;
        }
    }
    public void billWriteback(ChargeBill bill) {
        String sql= "SELECT * FROM bill WHERE id=? AND userId=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,bill.id,bill.userId);
        if(r==null || r.isEmpty())
        {
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bill", Integer.class);
            jdbcTemplate.update("INSERT INTO bill(chargeAmount,chargeEndTime,chargeFee,chargeStartTime,createdAt,deletedAt,pileId,serviceFee,totalFee,updatedAt,userId,id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    bill.chargeAmount,bill.chargeEndTime,bill.chargeFee,bill.chargeStartTime
                    ,bill.created_at,bill.deleted_at,bill.pileId,bill.serviceFee,bill.totalFee,bill.updated_at,bill.userId,count+1);
        }
        else
        {
            jdbcTemplate.update("UPDATE bill SET chargeAmount=?,chargeEndTime=?,chargeFee=?,chargeStartTime=?,createdAt=?,deletedAt=?,pileId=?,serviceFee=?,totalFee=?,updatedAt=? WHERE id=? AND userId=?",
                    bill.chargeAmount,bill.chargeEndTime,bill.chargeFee,bill.chargeStartTime,bill.created_at,bill.deleted_at,bill.pileId,bill.serviceFee,bill.totalFee,bill.updated_at,bill.id,bill.userId);
        }
    }
    public ChargeBill billReadByid(int id)
    {
        String sql="SELECT * FROM bill WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            DecimalFormat df=new DecimalFormat("0.0");
            ChargeBill myBill=new ChargeBill();
            myBill.chargeAmount=Double.parseDouble((String) r.get(0).get("chargeAmount"));
            myBill.id=Integer.parseInt((String)r.get(0).get("id"));
            myBill.chargeEndTime=(String) r.get(0).get("chargeEndTime");
            myBill.chargeFee=Double.parseDouble((String)r.get(0).get("chargeFee"));
            myBill.chargeFee= Double.parseDouble(df.format(myBill.chargeFee));
            myBill.chargeStartTime=(String)r.get(0).get("chargeStartTime");
            myBill.created_at =(String)r.get(0).get("createdAt");
            myBill.deleted_at =(String)r.get(0).get("deletedAt");
            myBill.pileId=Integer.parseInt((String)r.get(0).get("pileId"));
            myBill.serviceFee=Double.parseDouble((String)r.get(0).get("serviceFee"));
            myBill.serviceFee= Double.parseDouble(df.format(myBill.serviceFee));
            myBill.totalFee=Double.parseDouble((String)r.get(0).get("totalFee"));
            myBill.updated_at =(String)r.get(0).get("updatedAt");
            myBill.userId=Integer.parseInt((String)r.get(0).get("userId"));
            return myBill;
        }
    }
    public ChargeBill billReadByUserId(int id)
    {
        String sql="SELECT * FROM bill WHERE userId=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {

            return null;
        }
        else
        {
            ChargeBill myBill=new ChargeBill();
            myBill.chargeAmount=Double.parseDouble((String) r.get(r.size()-1).get("chargeAmount"));
            myBill.id=Integer.parseInt((String)r.get(r.size()-1).get("id"));
            myBill.chargeEndTime=(String) r.get(r.size()-1).get("chargeEndTime");
            myBill.chargeFee=Double.parseDouble((String)r.get(r.size()-1).get("chargeFee"));
            myBill.chargeStartTime=(String)r.get(r.size()-1).get("chargeStartTime");
            myBill.created_at =(String)r.get(r.size()-1).get("createdAt");
            myBill.deleted_at =(String)r.get(r.size()-1).get("deletedAt");
            myBill.pileId=Integer.parseInt((String)r.get(r.size()-1).get("pileId"));
            myBill.serviceFee=Double.parseDouble((String)r.get(r.size()-1).get("serviceFee"));
            myBill.totalFee=Double.parseDouble((String)r.get(r.size()-1).get("totalFee"));
            myBill.updated_at =(String)r.get(r.size()-1).get("updatedAt");
            myBill.userId=Integer.parseInt((String)r.get(r.size()-1).get("userId"));
            return myBill;
        }
    }
    public ArrayList<ChargeBill> ReadAllBill(int userId)
    {
        String sql="SELECT * FROM bill WHERE userId=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,userId);
        ArrayList<ChargeBill> BILLS=new ArrayList<>();
        if(r==null || r.isEmpty())
        {
            return BILLS;
        }
        else
        {
            DecimalFormat df=new DecimalFormat("0.0");
            for(int i=0;i<r.size();i++) {
                ChargeBill myBill = new ChargeBill();
                myBill.chargeAmount = Double.parseDouble((String) r.get(i).get("chargeAmount"));
                myBill.id = Integer.parseInt((String) r.get(i).get("id"));
                myBill.chargeEndTime = (String) r.get(i).get("chargeEndTime");
                myBill.chargeFee = Double.parseDouble((String) r.get(i).get("chargeFee"));
                myBill.chargeFee= Double.parseDouble(df.format(myBill.chargeFee));
                myBill.chargeStartTime = (String) r.get(i).get("chargeStartTime");
                myBill.created_at = (String) r.get(i).get("createdAt");
                myBill.deleted_at = (String) r.get(i).get("deletedAt");
                myBill.pileId = Integer.parseInt((String) r.get(i).get("pileId"));
                myBill.serviceFee = Double.parseDouble((String) r.get(i).get("serviceFee"));
                myBill.serviceFee= Double.parseDouble(df.format(myBill.serviceFee));
                myBill.totalFee = Double.parseDouble((String) r.get(i).get("totalFee"));
                myBill.updated_at = (String) r.get(i).get("updatedAt");
                myBill.userId = Integer.parseInt((String) r.get(i).get("userId"));
                if(myBill.chargeEndTime!=null)  BILLS.add(myBill);
            }
            return BILLS;
        }
    }
    public boolean billDelete(int id)
    {
        String sql="SELECT * FROM bill WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return false;
        }
        else
        {
            String sql1="DELETE FROM bill WHERE id=?";
            jdbcTemplate.update(sql1,id);
            return true;
        }
    }
    public void billupdate()
    {
        String sql1="UPDATE bill SET chargeEndtime = 0, chargeAmount = 0, serviceFee = 0, totalFee = 0 WHERE chargeEndtime IS NULL;";
        jdbcTemplate.update(sql1);
    }
    public void chargestatusWriteback(ChargeStatusResponse chargestatus)
    {
        String sql = "SELECT * FROM chargestatus WHERE userID=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,chargestatus.userID);
        if(r==null || r.isEmpty())
        {
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chargestatus", Integer.class);
            jdbcTemplate.update("INSERT INTO chargestatus(amount,chargingArea,code,fast,position,status,totalAmount,waitingArea,pile,userID) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    chargestatus.amount, chargestatus.chargingArea,chargestatus.code,chargestatus.fast,chargestatus.position,chargestatus.status,chargestatus.totalAmount,chargestatus.waitingArea,chargestatus.pile,chargestatus.userID);
        }
        else
        {
            jdbcTemplate.update("UPDATE chargestatus SET amount=? ,chargingArea=? ,code=? ,fast=? ,position=? ,status=? ,totalAmount=? ,waitingArea=?,pile=? WHERE userID=?",
                    chargestatus.amount, chargestatus.chargingArea,chargestatus.code,chargestatus.fast,chargestatus.position,chargestatus.status,chargestatus.totalAmount,chargestatus.waitingArea,chargestatus.pile,chargestatus.userID);
        }
    }
    public ChargeStatusResponse chargestatusRead(int userId)
    {
        String sql="SELECT * FROM chargestatus WHERE userID=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,userId);
        if(r==null || r.isEmpty() )
        {
            return null;
        }
        else if(r.get(0).get("status")!=null && r.get(0).get("status").equals("充电完成")){
            return null;
        }
        else
        {
            ChargeStatusResponse myChargestatus=new ChargeStatusResponse();
            myChargestatus.userID=(int)r.get(0).get("userID");
            myChargestatus.amount=(float)r.get(0).get("amount");
            myChargestatus.code=(int)r.get(0).get("code");
            //如果chargingArea为1，返回true，否则返回false
            if(Objects.equals((String) r.get(0).get("chargingArea"), "1")) myChargestatus.chargingArea=true;
            else myChargestatus.chargingArea=false;
            if(Objects.equals((String) r.get(0).get("fast"), "1")) myChargestatus.fast=true;
            else myChargestatus.fast=false;
            myChargestatus.pile=(int)r.get(0).get("pile");
            myChargestatus.position=(int)r.get(0).get("position");
            myChargestatus.status=(String) r.get(0).get("status");
            myChargestatus.totalAmount= (float) r.get(0).get("totalAmount");
            //如果waitingArea为1，返回true，否则返回false
            if(Objects.equals((String) r.get(0).get("waitingArea"), "1")) myChargestatus.waitingArea=true;
            else myChargestatus.waitingArea=false;
            return myChargestatus;
        }
    }
    public void ChargeStatusRemove()
    {
        String sql="DELETE FROM chargestatus WHERE status NOT IN ('充电中', '充电完成');";
        //删除status为空的数据
        String sql1 = "DELETE FROM chargestatus WHERE status IS NULL";
        //把充电中改为充电完成
        String sql2 = "UPDATE chargestatus SET status = '充电完成' WHERE status = '充电中'";

        jdbcTemplate.update(sql);
        jdbcTemplate.update(sql1);
        jdbcTemplate.update(sql2);
    }
    public boolean ChargeStatusDelete(int id)
    {
        String sql="SELECT * FROM chargestatus WHERE userID=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return false;
        }
        else
        {
            String sql1="DELETE FROM chargestatus WHERE userID=?";
            jdbcTemplate.update(sql1,id);
            return true;
        }
    }
    public void pileWriteback(PileStatus pileStatus)
    {
        String sql="SELECT * FROM pile WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,pileStatus.id);
        if(r==null || r.isEmpty())
        {
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pile", Integer.class);
            jdbcTemplate.update("INSERT INTO pile(chargeAmount,chargeTime,chargeTimes,status,id) VALUES (?,?,?,?,?)",
                    pileStatus.chargeAmount,pileStatus.chargeTime,pileStatus.chargeTimes,pileStatus.status,count+1);
        }
        else
        {
            jdbcTemplate.update("UPDATE  pile SET chargeAmount=?,chargeTime=?,chargeTimes=?,status=? WHERE id=?"
                    ,pileStatus.chargeAmount,pileStatus.chargeTime,pileStatus.chargeTimes,pileStatus.status,pileStatus.id);
        }
    }
    public PileStatus pileRead(int id)
    {
        String sql="SELECT * FROM pile WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            PileStatus myPile=new PileStatus();
            myPile.id=(int)r.get(0).get("id");
            myPile.chargeAmount=(double)r.get(0).get("chargeAmount");
            myPile.chargeTime=(double)r.get(0).get("chargeTime");
            myPile.status=(int)r.get(0).get("status");
            myPile.chargeTimes=(int)r.get(0).get("chargeTimes");

            return myPile;
        }
    }
    public boolean pileDelete(int id)
    {
        String sql="SELECT * FROM pile WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return false;
        }
        else
        {
            String sql1="DELETE FROM pile WHERE id=?";
            jdbcTemplate.update(sql1,id);
            return true;
        }
    }
    public void reportWriteBack(PileReport report)
    {
        String sql="SELECT * FROM report WHERE id=? AND date=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,report.id,report.date);
        if(r==null || r.isEmpty())
        {
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM report", Integer.class);
            jdbcTemplate.update("INSERT INTO report(chargeAmount,chargeFee,chargeTime,chargeTimes,serviceFee,totalFee,date,id) VALUES (?,?,?,?,?,?,?,?)",
                    report.chargeAmount,report.chargeFee,report.chargeTime,report.chargeTimes,report.serviceFee,report.totalFee,report.date,report.id);
        }
        else
        {
            jdbcTemplate.update("UPDATE  report SET chargeAmount=?,chargeFee=?,chargeTime=?,chargeTimes=?,serviceFee=?,totalFee=? WHERE id=? AND date=?"
                    ,report.chargeAmount,report.chargeFee,report.chargeTime,report.chargeTimes,report.serviceFee,report.totalFee,report.id,report.date);
        }
    }
    public PileReport reportReadById(int id)
    {
        String sql="SELECT * FROM report WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            PileReport myReport=new PileReport();
            myReport.chargeTimes=Integer.parseInt((String) r.get(0).get("chargeTimes"));
            myReport.id=Integer.parseInt((String)r.get(0).get("id"));
            myReport.chargeTime=Double.parseDouble((String) r.get(0).get("chargeTime"));
            myReport.chargeFee=Double.parseDouble((String)r.get(0).get("chargeFee"));
            myReport.chargeAmount=Double.parseDouble((String)r.get(0).get("chargeAmount"));
            myReport.serviceFee=Double.parseDouble((String)r.get(0).get("serviceFee"));
            myReport.totalFee=Double.parseDouble((String)r.get(0).get("totalFee"));
            myReport.date=(String) r.get(0).get("date");
            return myReport;
        }
    }
    public ArrayList<PileReport> reportReadByDate(String date)
    {
        String sql="SELECT * FROM report WHERE date=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,date);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            ArrayList<PileReport> pileReports=new ArrayList<>();
            for(int i=0;i<r.size();i++) {
                PileReport myReport = new PileReport();
                myReport.chargeTimes = Integer.parseInt((String) r.get(i).get("chargeTimes"));
                myReport.id = Integer.parseInt((String) r.get(i).get("id"));
                myReport.chargeTime = Double.parseDouble((String) r.get(i).get("chargeTime"));
                myReport.chargeFee = Double.parseDouble((String) r.get(i).get("chargeFee"));
                myReport.chargeAmount = Double.parseDouble((String) r.get(i).get("chargeAmount"));
                myReport.serviceFee = Double.parseDouble((String) r.get(i).get("serviceFee"));
                myReport.totalFee = Double.parseDouble((String) r.get(i).get("totalFee"));
                myReport.date =(String) r.get(i).get("date");

                pileReports.add(myReport);
            }
            return  pileReports;
        }
    }
    public boolean reportDelete(int id)
    {
        String sql="SELECT * FROM report WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return false;
        }
        else
        {
            String sql1="DELETE FROM report WHERE id=?";
            jdbcTemplate.update(sql1,id);
            return true;
        }
    }
    public void userWriteBack(ChargeUser user)
    {
        String sql="SELECT * FROM user WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,user.id);
        if(r==null || r.isEmpty())
        {
            int count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user", Integer.class);
            jdbcTemplate.update("INSERT INTO user(usernanme,password,id) VALUES (?,?,?)",
                    user.username,user.password,count+1);
        }
        else
        {
            jdbcTemplate.update("UPDATE  user SET usernanme=?,password=? WHERE id=?",user.username,user.password,user.id);
        }
    }
    public ChargeUser userReadById(int id)
    {
        String sql="SELECT * FROM user WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            ChargeUser myUser=new ChargeUser();
            myUser.id=Integer.parseInt((String) r.get(0).get("id"));
            myUser.username=(String) r.get(0).get("username");
            myUser.password=(String) r.get(0).get("password");
            return myUser;
        }
    }
    public ChargeUser userReadByName(String name)
    {
        String sql="SELECT * FROM user WHERE username=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,name);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            ChargeUser myUser=new ChargeUser();
            myUser.id=Integer.parseInt((String) r.get(0).get("id"));
            myUser.username=(String) r.get(0).get("username");
            myUser.password=(String) r.get(0).get("password");
            return myUser;
        }
    }
    public boolean userDelete(int id)
    {
        String sql="SELECT * FROM user WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return false;
        }
        else
        {
            String sql1="DELETE FROM user WHERE id=?";
            jdbcTemplate.update(sql1,id);
            return true;
        }
    }
    public void waitStatusWriteBack(PileWaitStatus pileWaitStatus)
    {
        String sql="SELECT * FROM waitstatus WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,pileWaitStatus.id);
        if(r==null || r.isEmpty())
        {
            jdbcTemplate.update("INSERT INTO waitstatus(user_amount,user_id,user_status,user_totalAmount,user_waitTime,id) VALUES (?,?,?,?,?,?)",
                    pileWaitStatus.users.amount,pileWaitStatus.users.id,pileWaitStatus.users.status,pileWaitStatus.users.totalAmount,pileWaitStatus.users.waitTime,pileWaitStatus.id);
        }
        else
        {
            jdbcTemplate.update("UPDATE  waitstatus SET user_amount=?,user_id=?,user_status=?,user_totalAmount=?,user_waitTime=? WHERE id=?",
                    pileWaitStatus.users.amount,pileWaitStatus.users.id,pileWaitStatus.users.status,pileWaitStatus.users.totalAmount,pileWaitStatus.users.waitTime,pileWaitStatus.id);
        }
    }
    public ArrayList<PileWaitStatus> waitStatusRead(int pileId)
    {
        String sql="SELECT * FROM waitstatus WHERE id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,pileId);
        if(r==null || r.isEmpty())
        {
            return null;
        }
        else
        {
            ArrayList<PileWaitStatus> pileWaitStatuses=new ArrayList<>();
            for(int i=0;i<r.size();i++) {
                PileWaitStatus myPile = new PileWaitStatus();
                myPile.users=new PileWaitUser();
                myPile.id =(int)r.get(i).get("id");
                myPile.users.amount =(double)r.get(i).get("user_amount");
                myPile.users.status = (String) r.get(i).get("user_status");
                myPile.users.id = (int)r.get(i).get("user_id");
                myPile.users.waitTime=(int)r.get(i).get("user_waitTime");
                myPile.users.totalAmount = (double)r.get(i).get("user_totalAmount");
                pileWaitStatuses.add(myPile);
            }
            return pileWaitStatuses;
        }
    }
    public boolean waitStatusDelete(int id)
    {
        String sql="SELECT * FROM waitstatus WHERE user_id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,id);
        if(r==null || r.isEmpty())
        {
            return false;
        }
        else
        {
            //删除waitstatus表中的全部数据

            String sql1="DELETE FROM waitstatus WHERE user_id=?";
            jdbcTemplate.update(sql1,id);
            return true;
        }
    }
    public void waitStatusDeleteAll()
    {
        String sql1="DELETE FROM waitstatus";
        jdbcTemplate.update(sql1);
    }
    public PileWaitStatus waitStatusReadByuser(int userId)
    {
        String sql="SELECT * FROM waitstatus WHERE user_id=?";
        List<Map<String,Object>> r=jdbcTemplate.queryForList(sql,userId);
        if(r==null || r.isEmpty() )
        {
            return null;
        }
        else
        {
                PileWaitStatus myPile = new PileWaitStatus();
                myPile.users=new PileWaitUser();
                myPile.id =(int)r.get(0).get("id");
                myPile.users.amount =(double)r.get(0).get("user_amount");
                myPile.users.status = (String) r.get(0).get("user_status");
                myPile.users.id = (int)r.get(0).get("user_id");
                myPile.users.waitTime=(int)r.get(0).get("user_waitTime");
                myPile.users.totalAmount = (double)r.get(0).get("user_totalAmount");
            return myPile;
        }
    }
}
