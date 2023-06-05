package Function;
import com.example.application.Class.*;
import com.example.application.AppConfig;
import com.example.application.database.DatabaseAccess;
import com.example.application.server;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import static com.example.application.server.*;

public class function {


    public static ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
    public static DatabaseAccess databaseAccess= context.getBean(DatabaseAccess.class);

    static ArrayList<ChargeBill> chargeBills=new ArrayList<>();//账单队列

    //查询充电状态
    public static ChargeStatusResponse getChargeinfo(int id) {
        ChargeStatusResponse myResponse=new ChargeStatusResponse();
        myResponse=databaseAccess.chargestatusRead(id);
        return myResponse;
    }
    //是否正在充电


    public static boolean isCharging(int id) {
        ChargeStatusResponse newresponse=new ChargeStatusResponse();
        newresponse=databaseAccess.chargestatusRead(id);
        if(newresponse==null)
        {
            return  false;
        }

        else if(Objects.equals(newresponse.status, "充电中"))
        {
            return  true;
        }
        else {
            return  false;
        }
    }


    //尝试充电(寻找空闲的充电桩)
    public static boolean trytoCharging() {

        int id=databaseAccess.ChargePileFind();
        if(id>=0)
        {
            return true;
        }
        else
        {
            return  false;
        }
    }
    //判断是否取消了充电请求
    public static boolean isDelete(String token) {
        ChargeUser myuser=new ChargeUser();
        myuser=databaseAccess.userReadByName(token);
        if(chargeRequests.get(myuser.id)==null)  return true;
        else return false;
    }

    //把新的充电请求加入等待队列
    public static void changeCharge(ChargeRequest message) {
    }
    //是否有账单

    public static boolean isBill(int billId) {

        return false;
    }

    //生成账单
    public static void createBill(int userId,ChargeRequest request,int pileId,int time) {
        ChargeBill bill=new ChargeBill();
        bill.id=chargeBills.size()+1;
        bill.userId=userId;//前端给我的
        bill.chargeAmount=request.amount;
        bill.chargeStartTime=String.valueOf(time);
        bill.pileId=pileId;
        bill.created_at = String.valueOf(time);
        databaseAccess.billWriteback(bill);
        chargeBills.add(bill);
    }
    public static void FinishBill(ChargeBill bill,int time,boolean fast)//
    {
        bill.chargeEndTime=String.valueOf(time);
        bill.created_at =bill.chargeStartTime;
        if(fast) bill.chargeAmount=30*(Double.valueOf(bill.chargeEndTime)-Double.valueOf(bill.chargeStartTime))/3600;
        else bill.chargeAmount=7*(Double.valueOf(bill.chargeEndTime)-Double.valueOf(bill.chargeStartTime))/3600;
        bill.serviceFee=0.8*bill.chargeAmount;
        bill.chargeFee=CountFee(Double.valueOf(bill.chargeStartTime),Double.valueOf(bill.chargeEndTime),fast);
        DecimalFormat df=new DecimalFormat("0.00");
        bill.chargeFee= Double.parseDouble(df.format(bill.chargeFee));
        bill.serviceFee= Double.parseDouble(df.format(bill.serviceFee));
        bill.totalFee=Double.valueOf(df.format(bill.chargeFee+bill.serviceFee));
        bill.chargeAmount=Double.valueOf(df.format(bill.chargeAmount));
        databaseAccess.billWriteback(bill);
    }
    public static void createBillByUserId(int userId,int time,boolean fast)//故障时生成的详单
    {
        ChargeBill mybill=new ChargeBill();
        mybill=databaseAccess.billReadByUserId(userId);
        mybill.chargeEndTime= String.valueOf(time);
        mybill.chargeFee=CountFee(Double.valueOf(mybill.chargeStartTime),Double.valueOf(mybill.chargeEndTime),fast);
        if(fast) mybill.chargeAmount=30*(Double.valueOf(mybill.chargeEndTime)-Double.valueOf(mybill.chargeStartTime))/3600;
        else mybill.chargeAmount=7*(Double.valueOf(mybill.chargeEndTime)-Double.valueOf(mybill.chargeStartTime))/3600;
        DecimalFormat df=new DecimalFormat("0.00");
        mybill.serviceFee=0.8*mybill.chargeAmount;
        mybill.totalFee=mybill.chargeFee+mybill.serviceFee;
        mybill.chargeFee= Double.parseDouble(df.format(mybill.chargeFee));
        mybill.serviceFee= Double.parseDouble(df.format(mybill.serviceFee));
        mybill.totalFee=Double.valueOf(df.format(mybill.chargeFee+mybill.serviceFee));
        mybill.chargeAmount=Double.valueOf(df.format(mybill.chargeAmount));
        databaseAccess.billWriteback(mybill);
        chargeBills.add(mybill);
    }
    //寻找账单

    public static ChargeBill getChargeBill(int billId) {
        ChargeBill myBill=new ChargeBill();
        myBill=databaseAccess.billReadByid(billId);
        return myBill;
    }


    //是否有该用户

    public static boolean isName(String name) {
        ChargeUser myUser=new ChargeUser();
        myUser=databaseAccess.userReadByName(name);
        ChargeBill myBill=new ChargeBill();
        myBill=databaseAccess.billReadByUserId(myUser.id);
        if(myBill != null)
        {
            return true;
        }
        else {
            return  false;
        }
    }
    //返回账单列表


    //判断是不是管理员


    //获得某充电桩状态

    public static PileStatus getPile(int pileId) throws IOException {
        PileStatus myPileStatus=new PileStatus();
        myPileStatus=databaseAccess.pileRead(pileId);
        return myPileStatus;

    }
    //重启系统初始化充电桩
    public static void restart() {
    //读data/Pile下的文件,A代表1号充电桩,以此类推
    }
    //修改某充电桩状态

    public static void changePile(int pileId, int status) {
        int id = pileId;
        PileStatus mystatus = new PileStatus();
        mystatus = databaseAccess.pileRead(id);
        mystatus.status = status;
        databaseAccess.pileWriteback(mystatus);
        ChargePile newpile = new ChargePile();
        newpile = databaseAccess.ChargePileRead(id);
        newpile.status = status;
        databaseAccess.ChargePileWriteback(newpile);
        //前端给我故障的充电桩的id
        if (status <= 0) {
            if (id <= FastChargingPileNum - 1) {
                //修改充电桩状态
                FastPiles.get(id).status = status;
                ;
                if (FastQueue[id].size() > 0) {
                    //修改正在充电的车辆的账单等数据
                    function.createBillByUserId(FastQueue[id].get(0), server.time, true);
                    //更新充电请求
                    ChargeBill newbill = new ChargeBill();
                    newbill = databaseAccess.billReadByUserId(FastQueue[id].get(0));
                    chargeRequests.get(FastQueue[id].get(0)).amount -= newbill.chargeAmount;
                    //将剩下的假如差错队列
                    for (int j = 0; j < FastQueue[id].size(); j++) {
                        ErrorQueue.add(0,FastQueue[id].get(j));
                        for (int k = 0; k < tables.size(); k++) {
                            if (tables.get(k).userId == FastQueue[id].get(j)) {
                                tables.remove(k);
                            }
                        }
                    }
                    FastQueue[id].clear();
                }
            } else {
                if (SlowQueue[id].size() > 0) {
                    SlowPiles.get(id - FastChargingPileNum).status = status;
                    function.createBillByUserId(SlowQueue[id].get(0), server.time, false);
                    //更新充电请求
                    ChargeBill newbill = new ChargeBill();
                    newbill = databaseAccess.billReadByUserId(FastQueue[id].get(0));
                    chargeRequests.get(SlowQueue[id].get(0)).amount -= newbill.chargeAmount;
                    for (int j = 0; j < SlowQueue[id].size(); j++) {
                        ErrorQueue.add(0, SlowQueue[id].get(j));
                        for (int k = 0; k < tables.size(); k++) {
                            if (tables.get(k).userId == FastQueue[id].get(j)) {
                                tables.remove(k);
                            }
                        }
                        SlowQueue[id].clear();
                    }
                }
            }
        } else {
            FastPiles.get(id).status = status;
            //将所有同类型的车分配给差错队列重新调度
            if (id < FastChargingPileNum) {
                for (int i = 0; i < FastChargingPileNum; i++) {
                    if (FastQueue[i].size() > 1) {
                        Iterator<Integer> it = FastQueue[i].iterator();
                        Integer j = it.next();
                        while (it.hasNext()) {
                            j = it.next();
                            ErrorQueue.add(j);
                            it.remove();
                        }
                    }
                }
            } else {
                for (int i = 0; i < TrickleChargingPileNum; i++) {
                    if (SlowQueue[i].size() > 1) {
                        Iterator<Integer> it = SlowQueue[i].iterator();
                        Integer j = it.next();
                        while (it.hasNext()) {
                            j = it.next();
                            ErrorQueue.add(j);
                            it.remove();
                        }
                    }
                }
            }
            //遍历WaitQueue
            while (WaitQueue.size()>0) {
                ErrorQueue.add(WaitQueue.get(0));
                WaitQueue.remove(0);
            }


        }
    }
    //获得充电桩等待情况

    public static ArrayList<PileWaitStatus> getPileWait(int pileId) {
        ArrayList<PileWaitStatus> pileWaitStatuses=new ArrayList<>();
        pileWaitStatuses=databaseAccess.waitStatusRead(pileId);
        return pileWaitStatuses;
    }
    //获得充电桩列表

    public static ChargePile getPileTable(int id) {
        ChargePile myPileStatus=new ChargePile();
        myPileStatus=databaseAccess.ChargePileRead(id);
        return  myPileStatus;
    }
    //获得某日充电桩报表

    public static ArrayList<PileReport> getReport(String date) {
        ArrayList<PileReport> mypilereports=new ArrayList<>();
        mypilereports=databaseAccess.reportReadByDate(date);
        return mypilereports;
    }



    //刷新

    //计算费用的函数
    public static double CountFee(double starttime,double endtime,boolean fast)
    {
        double dif=1e-6f;
        DecimalFormat df=new DecimalFormat("0.0");
        double rank1=0.4,rank2=0.7,rank3=1.0;//各时段的计费
        //谷时 0.4 23:00 - 7:00
        //平时 0.7 7:00-10:00 15:00-18:00 21:00-23:00
        //峰时 1.0 10:00-15:00 18:00-21:00
        double dayFee;
        double countfee;
        if(fast) dayFee=504;
        else dayFee=117.6;
        int days=(int)(endtime-starttime)/86400;
        double fee1=days*dayFee;
        double fee2=0;
        endtime-=days*86400;
        for(double i=starttime;Double.compare(i,endtime)<0;i++)
        {
            if(Double.compare(i,420.0)<0)
            {
                if (fast) fee2+=0.2;
                else fee2+=Double.valueOf(df.format(0.4*7/60));
            }
            else if(Double.compare(i,600.0)<0)
            {
                if (fast) fee2+=0.35;
                else fee2+=Double.valueOf(df.format(0.7*7/60));
            }
            else if (Double.compare(i,900.0)<0)
            {
                if (fast) fee2+=0.5;
                else fee2+=Double.valueOf(df.format(1.0*7/60));
            }
            else if(Double.compare(i,1080.0)<0)
            {
                if (fast) fee2+=0.35;
                else fee2+=Double.valueOf(df.format(0.7*7/60));
            }
            else if(Double.compare(i,1260.0)<0)
            {
                if (fast) fee2+=0.5;
                else fee2+=Double.valueOf(df.format(1.0*7/60));
            }
            else if(Double.compare(i,1380.0)<0)
            {
                if (fast) fee2+=0.35;
                else fee2+=Double.valueOf(df.format(0.7*7/60));
            }
            else {
                if (fast) fee2+=0.2;
                else fee2+=Double.valueOf(df.format(0.4*7/60));
            }
        }
        fee2=fee2/60.0;
        return fee1+fee2;
    }

    //时间转化函数
    public static String GetDate(int time)
    {

        return server.getDay(String.valueOf(server.time));
    }
    public static void PileUpdate(boolean fast,int pileId,int userId,int time) {
        if(fast)
        {
            PileStatuses.get(pileId).chargeTimes++;
            PileStatuses.get(pileId).chargeTime += time - Integer.parseInt(databaseAccess.billReadByUserId(userId).chargeStartTime);
            PileStatuses.get(pileId).chargeAmount += databaseAccess.billReadByUserId(userId).chargeAmount;
            databaseAccess.pileWriteback(PileStatuses.get(pileId));
            if(Objects.equals(PileReports.get(pileId).date, getDay(String.valueOf(time)))) {
                PileReports.get(pileId).totalFee += databaseAccess.billReadByUserId(userId).totalFee;
                PileReports.get(pileId).chargeFee += databaseAccess.billReadByUserId(userId).chargeFee;
                PileReports.get(pileId).serviceFee += databaseAccess.billReadByUserId(userId).serviceFee;
                PileReports.get(pileId).chargeAmount += databaseAccess.billReadByUserId(userId).chargeAmount;
                PileReports.get(pileId).chargeTimes++;
                databaseAccess.reportWriteBack(PileReports.get(pileId));
            }
            else
            {
                PileReports.get(pileId).date = getDay(String.valueOf(time));
                PileReports.get(pileId).totalFee = databaseAccess.billReadByUserId(userId).totalFee;
                PileReports.get(pileId).chargeFee = databaseAccess.billReadByUserId(userId).chargeFee;
                PileReports.get(pileId).serviceFee = databaseAccess.billReadByUserId(userId).serviceFee;
                PileReports.get(pileId).chargeAmount = databaseAccess.billReadByUserId(userId).chargeAmount;
                PileReports.get(pileId).chargeTimes=1;
                databaseAccess.reportWriteBack(PileReports.get(pileId));
            }

        }
        else
        {
            PileStatuses.get(pileId).chargeTimes++;
            PileStatuses.get(pileId).chargeTime+=time-Integer.parseInt(databaseAccess.billReadByUserId(userId).chargeStartTime);
            PileStatuses.get(pileId).chargeAmount+=databaseAccess.billReadByUserId(userId).chargeAmount;
            databaseAccess.pileWriteback(PileStatuses.get(pileId));
            if( Objects.equals(PileReports.get(pileId).date, getDay(String.valueOf(time)))) {
                PileReports.get(pileId).totalFee += databaseAccess.billReadByUserId(userId).totalFee;
                PileReports.get(pileId).chargeFee += databaseAccess.billReadByUserId(userId).chargeFee;
                PileReports.get(pileId).serviceFee += databaseAccess.billReadByUserId(userId).serviceFee;
                PileReports.get(pileId).chargeTimes++;
                PileReports.get(pileId).date = getDay(String.valueOf(time));
                PileReports.get(pileId).chargeAmount += databaseAccess.billReadByUserId(userId).chargeAmount;
                databaseAccess.reportWriteBack(PileReports.get(pileId));
            }
            else
            {
                PileReports.get(pileId).totalFee = databaseAccess.billReadByUserId(userId).totalFee;
                PileReports.get(pileId).chargeFee = databaseAccess.billReadByUserId(userId).chargeFee;
                PileReports.get(pileId).serviceFee = databaseAccess.billReadByUserId(userId).serviceFee;
                PileReports.get(pileId).chargeTimes=1;
                PileReports.get(pileId).date = getDay(String.valueOf(time));
                PileReports.get(pileId).chargeAmount = databaseAccess.billReadByUserId(userId).chargeAmount;
                databaseAccess.reportWriteBack(PileReports.get(pileId));
            }
        }
    }

    public static boolean isAdmin(String token) {

        if(token.equals("admin123")){
            System.out.println(token+" 尝试获得管理员权限成功");
            return true;
        }
        else{
            System.out.println(token+" 尝试获得管理员权限失败");
            return false;
        }


    }
    public static boolean login(String username, String password) {
        if(databaseAccess.login(username,password)){
            return true;
        }
        else{
            return false;
        }
    }
    //刷新

    //注册

    public static boolean register(String username, String password) {
        if(databaseAccess.register(username,password)){
            return true;
        }
        else{
            return false;
        }
    }
}
