package com.example.application;
import com.example.application.Class.*;
import com.example.application.database.DatabaseAccess;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import Function.function;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static Function.function.CountFee;
import static Function.function.databaseAccess;
import static com.example.application.server.*;

@SpringBootApplication
public class server implements Runnable{
    public static class  events{
        public volatile int usrid=-1;
        public volatile int kind;
    }
    public class table implements Comparable<table>{
        int time=0;//到该时间要做什么
        int type;//要做什么
        int pile;//对应充电桩
        public int userId;//对应用户
        @Override
        public int compareTo(table other) {
            // 将time属性从小到大进行比较
            return Integer.compare(this.time, other.time);
        }
        public void addTable(int time,int type,int pile,int userId){
            server.table table=new server.table();
            table.time=time;
            table.type=type;
            table.pile=pile;
            table.userId=userId;
            //把table加到tables开头
            int index = Collections.binarySearch(tables, table);
            if (index < 0) {
                index = -(index + 1); // 找到要插入的位置
            }
            tables.add(index, table);

        }
    }
    public static volatile ArrayList<table> tables=new ArrayList<>();//用于储存所有的时间表
    public static volatile ArrayList<events> event=new ArrayList<>();//用于储存所有的任务
    public static volatile ArrayList<Integer> startCharge=new ArrayList<>();//开始充电队列
    public static volatile ArrayList<Integer> endCharge=new ArrayList<>();//结束充电队列
    public static volatile Map<Integer,ChargeRequest> chargeRequests= new HashMap<>();//充电申请队列
    public static volatile ArrayList<Integer> WaitQueue=new ArrayList<>();//等待队列
    public static volatile ArrayList<Integer> ErrorQueue=new ArrayList<>();//出错之后的优先处理的队列
    public static volatile Map<Integer,String> Number=new HashMap<>();//用户排队号码
    public static volatile ArrayList<ChargePile> FastPiles=new ArrayList<>();
    public static volatile ArrayList<ChargePile> SlowPiles=new ArrayList<>();
    public static volatile Map<Integer,PileReport> PileReports=new HashMap<>();
    public static volatile ArrayList<PileStatus> PileStatuses=new ArrayList<>();
    public static volatile Map<Integer,Integer> TimeUser=new HashMap<>();//用户id和提出申请的时间的对应
    public static volatile Map<Integer,ChargeStatusResponse> UserChargeStatus=new HashMap<>();//将用户id和充电状态绑定起来
    public static  Object lock = new Object();
    public Charge charge = new Charge();
    public Others others = new Others();
    public static int index=0;
    public  static volatile int time=0;
    public static  volatile int userID=0;
    public static int FastChargingPileNum=2;//快充充电桩数目
    public static int TrickleChargingPileNum=3;//慢充充电桩数目
    public static int WaitingAreaSize=6;//等候区队列长度
    public static int ChargingQueueLen=2;//充电桩等候区长度
    public static volatile ArrayList<Integer>[] FastQueue=new ArrayList[FastChargingPileNum];//快充队列
    public static volatile ArrayList<Integer>[] SlowQueue=new ArrayList[TrickleChargingPileNum];//慢充队列
    //初始化时间
    static LocalDateTime modifiedDateTime;
    static LocalDateTime Day;
    public static String getTime(String time)
    {
        //把字符串转换数字
        int time1=Integer.parseInt(time);
        LocalDateTime modifiedDateTime1 = modifiedDateTime.plusMinutes(time1);
        //把时间转换成字符串
        // 定义日期时间格式化器
        // 创建日期时间格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 将修改后的日期时间转换为字符串
        String modifiedDateTimeStr = modifiedDateTime1.format(formatter)+".000";


        return modifiedDateTimeStr;

    }
    public static String getDay(String time)
    {
        //把字符串转换数字
        int time1=Integer.parseInt(time);

        LocalDateTime modifiedDateTime1 = modifiedDateTime.plusMinutes(time1);
        //把时间转换成字符串
        // 定义日期时间格式化器
        // 创建日期时间格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // 将修改后的日期时间转换为字符串
        String modifiedDateTimeStr = modifiedDateTime1.format(formatter);


        return modifiedDateTimeStr;

    }
    public static void main(String[] args) throws InterruptedException {
        //获得当前时间
        LocalDateTime currentDateTime = LocalDateTime.now();

        // 将秒钟部分设置为0
        modifiedDateTime = currentDateTime.withSecond(0);
        // 将毫秒部分设置为0
        modifiedDateTime = modifiedDateTime.withNano(0);
        //获得当前时间
        Day=LocalDateTime.now();


        new Thread(new Time()).start();

        databaseAccess.ChargeStatusRemove();
        databaseAccess.waitStatusDeleteAll();
        databaseAccess.billupdate();
        //快充队列初始化
        for (int i=0;i<2;i++)
        {
            FastQueue[i]=new ArrayList<>();
        }
        //慢充队列初始化
        for(int i=0;i<3;i++)
        {
            SlowQueue[i]=new ArrayList<>();
        }
        //充电请求初始化
        ArrayList<ChargeStatusResponse> list=new ArrayList<>();
        list=databaseAccess.chargeStatusResponseReadAll();
        for(int i=0;i<list.size();i++)
        {
            ChargeRequest newrequest=new ChargeRequest();
            newrequest.fast=list.get(i).fast;
            newrequest.amount=list.get(i).amount;
            newrequest.totalAmount=(int)list.get(i).totalAmount;
            chargeRequests.put(list.get(i).userID,newrequest);
            UserChargeStatus.put(list.get(i).userID,list.get(i));
            if(Objects.equals(list.get(i).status, "充电中"))
            {
                if(list.get(i).fast)  FastQueue[list.get(i).pile].add(list.get(i).userID);
                else SlowQueue[list.get(i).pile].add(list.get(i).userID);
            }
            else if(Objects.equals(list.get(i).status, "充电区等候中"))
            {
                if(list.get(i).fast)  FastQueue[list.get(i).pile].add(list.get(i).userID);
                else SlowQueue[list.get(i).pile].add(list.get(i).userID);
            }
            else if(Objects.equals(list.get(i).status, "等待区排队中"))
            {
                WaitQueue.add(list.get(i).userID);
            }
        }
        //充电桩的初始化(从数据库里面读取数据)
        for(int i=0;i<FastChargingPileNum;i++)
        {
            ChargePile pile=new ChargePile();
            pile=databaseAccess.ChargePileRead(i);
            PileStatus pileStatus=new PileStatus();
            pileStatus=databaseAccess.pileRead(i);
            databaseAccess.pileWriteback(pileStatus);
            PileReport report=new PileReport();
            report=databaseAccess.reportReadById(i);
            PileReports.put(i,report);
            FastPiles.add(pile);
            PileStatuses.add(pileStatus);
        }
        for(int i=0;i<TrickleChargingPileNum;i++)
        {
            ChargePile pile=new ChargePile();
            pile=databaseAccess.ChargePileRead(i);
            PileStatus pileStatus=new PileStatus();
            pileStatus=databaseAccess.pileRead(i+FastChargingPileNum);
            databaseAccess.pileWriteback(pileStatus);
            PileReport report=new PileReport();
            report=databaseAccess.reportReadById(i+FastChargingPileNum);
            PileReports.put(i+FastChargingPileNum,report);
            SlowPiles.add(pile);
            PileStatuses.add(pileStatus);
        }
        SpringApplication.run(server.class, args);
        //执行所有命令


        //启动线程
        new Thread(new server()).start();
        new Thread(new Schedule()).start();

    }




    @Override
    public void run() {
        while(true){
            //执行任务
            synchronized (server.lock){
                if (ErrorQueue.size() > 0)
                {
                    //查到充电方式
                    boolean fast=chargeRequests.get(ErrorQueue.get(0)).fast;
                    boolean flag=false;

                    if(fast)//快充模式
                    {
                        for(int i=0;i<FastChargingPileNum;i++)
                        {
                            if(FastPiles.get(i).status!=0)//假如有充电桩处于运行状态
                            {
                                flag=true;
                                break;
                            }
                        }
                        if(!flag) {System.out.println("Error");}//所有快充充电桩都处于故障状态，无法进行快充
                        else
                        {
                            index++;
                            boolean flag1=false;
                            for(int i=0;i<FastChargingPileNum;i++)
                            {
                                if(FastQueue[i].size()<ChargingQueueLen&&FastPiles.get(i).status==1)//看是否有充电桩是工作状态并且是空闲的
                                {
                                    flag1=true;
                                    break;
                                }
                            }
                            if(!flag1)//所有工作的充电桩都满了
                            {

                                    String wait = "F" + String.valueOf(index);
                                    //更新用户状态信息
                                    UserChargeStatus.get(ErrorQueue.get(0)).waitingArea=true;
                                    UserChargeStatus.get(ErrorQueue.get(0)).status="等候区排队中";
                                    UserChargeStatus.get(ErrorQueue.get(0)).chargingArea=false;
                                    UserChargeStatus.get(ErrorQueue.get(0)).position=WaitQueue.size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(ErrorQueue.get(0)));
                                    Number.put(ErrorQueue.get(0), wait);
                                    //ErrorQueue.get(0)放到WaitQueue第一个
                                    WaitQueue.add(0, ErrorQueue.get(0));
                                    while(WaitQueue.size()>WaitingAreaSize) {
                                       //删除等候区最后一个
                                        WaitQueue.remove(WaitQueue.size()-1);

                                    }

                            }
                            else
                            {
                                boolean flag2=false;
                                //先看有没有空闲状态的充电桩
                                for(int i=0;i<FastChargingPileNum;i++)
                                {
                                    if(FastPiles.get(i).status==1&&FastQueue[i].size()==0)
                                    {
                                        flag2=true;
                                        String wait = "F" + String.valueOf(index);
                                        Number.put(ErrorQueue.get(0), wait);
                                        FastQueue[i].add(ErrorQueue.get(0));
                                        //创建时钟表
                                        table newtable = new table();
                                        newtable.pile = i;
                                        newtable.time = server.time;
                                        newtable.type = 2;
                                        UserChargeStatus.get(ErrorQueue.get(0)).waitingArea=false;
                                        UserChargeStatus.get(ErrorQueue.get(0)).status="充电中";
                                        UserChargeStatus.get(ErrorQueue.get(0)).chargingArea=true;
                                        UserChargeStatus.get(ErrorQueue.get(0)).pile=i;
                                        //添加到时间表
                                        newtable.addTable(newtable.time, newtable.type, newtable.pile, ErrorQueue.get(0));
                                        break;
                                    }
                                }
                                if(!flag2)//不存在空闲的充电桩
                                {
                                    int min=Integer.MAX_VALUE;
                                    int pileId=0;
                                    //循环找到时间最小(等待加充电时间最短)的充电桩
                                    for(int i=0;i<FastChargingPileNum;i++)
                                    {
                                        if(FastPiles.get(i).status==1&&FastQueue[i].size()<ChargingQueueLen)
                                        {
                                            int amount=0;
                                            for(int j=0;j<FastQueue[i].size();j++)
                                            {
                                                amount+=chargeRequests.get(FastQueue[i].get(j)).amount;//当前一共要冲的电量
                                            }
                                            //总时间=等待时间（总的充电时间-当前已经充电的时间）+自己需要的充电时间
                                            int temp=(int)(60*amount/30+TimeUser.get(FastQueue[i].get(0))-server.time+60*chargeRequests.get(ErrorQueue.get(0)).amount/30);
                                            if(temp<min)
                                            {
                                                min=temp;
                                                pileId=i;
                                            }
                                        }
                                    }
                                    String wait = "F" + String.valueOf(index);
                                    Number.put(ErrorQueue.get(0), wait);
                                    //更新用户状态信息
                                    UserChargeStatus.get(ErrorQueue.get(0)).pile=pileId;
                                    UserChargeStatus.get(ErrorQueue.get(0)).waitingArea=false;
                                    UserChargeStatus.get(ErrorQueue.get(0)).status="充电区等候中";
                                    UserChargeStatus.get(ErrorQueue.get(0)).chargingArea=true;
                                    UserChargeStatus.get(ErrorQueue.get(0)).position=FastQueue[pileId].size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(ErrorQueue.get(0)));
                                    //更新充电桩等候信息
                                    PileWaitUser newuser=new PileWaitUser();
                                    newuser.id=ErrorQueue.get(0);
                                    newuser.status="Waiting";
                                    newuser.amount=chargeRequests.get(ErrorQueue.get(0)).amount;
                                    newuser.totalAmount=chargeRequests.get(ErrorQueue.get(0)).totalAmount;
                                    newuser.waitTime=server.time-TimeUser.get(ErrorQueue.get(0));
                                    PileWaitStatus newstatus=new PileWaitStatus();
                                    newstatus.id=pileId;
                                    newstatus.users=newuser;
                                    newstatus.users.waitTime= server.time-TimeUser.get(ErrorQueue.get(0));
                                    databaseAccess.waitStatusWriteBack(newstatus);
                                    FastQueue[pileId].add(ErrorQueue.get(0));
                                }
                            }
                        }
                    }
                    else//慢充模式
                    {
                        for(int i=0;i<TrickleChargingPileNum;i++)
                        {
                            if(SlowPiles.get(i).status!=0)//假如有充电桩处于运行状态
                            {
                                flag=true;
                                break;
                            }
                        }
                        if(!flag) {System.out.println("Error");}//所有快充充电桩都处于故障状态，无法进行快充
                        else
                        {
                            index++;
                            boolean flag1=false;
                            for(int i=0;i<TrickleChargingPileNum;i++)
                            {
                                if(SlowQueue[i].size()<ChargingQueueLen&&SlowPiles.get(i).status==1)//看是否有充电桩是工作状态并且是空闲的
                                {
                                    flag1=true;
                                    break;
                                }
                            }
                            if(!flag1)//所有工作的充电桩都满了
                            {

                                    String wait = "T" + String.valueOf(index);
                                    UserChargeStatus.get(ErrorQueue.get(0)).waitingArea=true;
                                    UserChargeStatus.get(ErrorQueue.get(0)).status="等候区排队中";
                                    UserChargeStatus.get(ErrorQueue.get(0)).chargingArea=false;
                                    UserChargeStatus.get(ErrorQueue.get(0)).position=WaitQueue.size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(ErrorQueue.get(0)));
                                    Number.put(ErrorQueue.get(0), wait);
                                    Number.put(ErrorQueue.get(0), wait);
                                    WaitQueue.add(0, ErrorQueue.get(0));
                                    while(WaitQueue.size()>WaitingAreaSize) {
                                        //删除等候区最后一个
                                        WaitQueue.remove(WaitQueue.size()-1);

                                    }

                            }
                            else
                            {
                                boolean flag2=false;
                                //先看有没有空闲状态的充电桩
                                for(int i=0;i<TrickleChargingPileNum;i++)
                                {
                                    if(SlowPiles.get(i).status==1&&SlowQueue[i].size()==0)
                                    {
                                        flag2=true;
                                        String wait = "T" + String.valueOf(index);
                                        Number.put(ErrorQueue.get(0), wait);
                                        SlowQueue[i].add(ErrorQueue.get(0));
                                        //创建时钟表
                                        table newtable = new table();
                                        newtable.pile = i+FastChargingPileNum;
                                        newtable.time = server.time;
                                        newtable.type = 2;
                                        UserChargeStatus.get(ErrorQueue.get(0)).waitingArea=false;
                                        UserChargeStatus.get(ErrorQueue.get(0)).status="充电中";
                                        UserChargeStatus.get(ErrorQueue.get(0)).chargingArea=true;
                                        UserChargeStatus.get(ErrorQueue.get(0)).pile=i+FastChargingPileNum;
                                        //添加到时间表
                                        newtable.addTable(newtable.time, newtable.type, newtable.pile, ErrorQueue.get(0));
                                        break;
                                    }
                                }
                                if(!flag2)//不存在空闲的充电桩
                                {
                                    int min=Integer.MAX_VALUE;
                                    int pileId=0;
                                    //循环找到时间最小(等待加充电时间最短)的充电桩
                                    for(int i=0;i<TrickleChargingPileNum;i++)
                                    {
                                        if(SlowPiles.get(i).status==1&&SlowQueue[i].size()<ChargingQueueLen)
                                        {
                                            int amount=0;
                                            for(int j=0;j<SlowQueue[i].size();j++)
                                            {
                                                amount+=chargeRequests.get(SlowQueue[i].get(j)).amount;//当前一共要冲的电量
                                            }
                                            //总时间=等待时间（总的充电时间-当前已经充电的时间）+自己需要的充电时间
                                            int temp=(int)(60*amount/7+TimeUser.get(SlowQueue[i].get(0))-server.time+60*chargeRequests.get(ErrorQueue.get(0)).amount/7);
                                            if(temp<min)
                                            {
                                                min=temp;
                                                pileId=i;
                                            }
                                        }
                                    }
                                    String wait = "T" + String.valueOf(index);
                                    //更新用户状态信息
                                    UserChargeStatus.get(ErrorQueue.get(0)).pile=pileId+FastChargingPileNum;
                                    UserChargeStatus.get(ErrorQueue.get(0)).waitingArea=false;
                                    UserChargeStatus.get(ErrorQueue.get(0)).status="充电区等候中";
                                    UserChargeStatus.get(ErrorQueue.get(0)).chargingArea=true;
                                    UserChargeStatus.get(ErrorQueue.get(0)).position=SlowQueue[pileId].size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(ErrorQueue.get(0)));
                                    //更新充电桩等候信息
                                    PileWaitUser newuser=new PileWaitUser();
                                    newuser.id=ErrorQueue.get(0);
                                    newuser.status="Waiting";
                                    newuser.amount=chargeRequests.get(ErrorQueue.get(0)).amount;
                                    newuser.totalAmount=chargeRequests.get(ErrorQueue.get(0)).totalAmount;
                                    newuser.waitTime=server.time-TimeUser.get(ErrorQueue.get(0));
                                    PileWaitStatus newstatus=new PileWaitStatus();
                                    newstatus.id=pileId+FastChargingPileNum;
                                    newstatus.users=newuser;
                                    newstatus.users.waitTime= server.time-TimeUser.get(ErrorQueue.get(0));
                                    databaseAccess.waitStatusWriteBack(newstatus);
                                    Number.put(ErrorQueue.get(0), wait);
                                    SlowQueue[pileId].add(ErrorQueue.get(0));
                                }
                            }
                        }
                    }
                    ErrorQueue.remove(0);
                }
            if(event.size()>0){
                //打印event全部
                System.out.println("执行任务(有任务就会打印): ");
                for(int i=0;i<event.size();i++)
                {
                    System.out.println("任务: "+event.get(0).kind+" "+"用户:"+event.get(0).usrid);
                }
                //先查看有无出错队列的车，有的话优先处理，不考虑等候区

                if(event.get(0).kind==1) {
                    //对指定充电桩执行结束充电任务,其充电桩编号为endcharge.get(1)
                    //执行结束充电任务,完善账单，删除对应充电桩的首元素，后面的元素往前移


                    function.FinishBill(databaseAccess.billReadByUserId(event.get(0).usrid),server.time,chargeRequests.get(event.get(0).usrid).fast);

                    //更新用户状态信息
                    UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                    UserChargeStatus.get(event.get(0).usrid).status="充电完成";
                    UserChargeStatus.get(event.get(0).usrid).chargingArea=false;
                    UserChargeStatus.get(event.get(0).usrid).position=0;
                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(event.get(0).usrid));

                    //充电桩数据的更新
                    function.PileUpdate(chargeRequests.get(event.get(0).usrid).fast,endCharge.get(0),event.get(0).usrid,server.time);
                    //后面的元素往前移，并立刻构造充电事件
                    if (UserChargeStatus.get(event.get(0).usrid).fast)
                    {
                        if(FastQueue[endCharge.get(0)].size()>0) {
                            FastQueue[endCharge.get(0)].remove(0);
                            if (FastQueue[endCharge.get(0)].size() > 0) {
                                table newtable = new table();
                                newtable.pile = endCharge.get(0);
                                newtable.time = server.time;
                                newtable.type = 2;
                                newtable.addTable(newtable.time, newtable.type, newtable.pile, FastQueue[endCharge.get(0)].get(0));
                            }
                        }
                    }
                    else
                    {
                        if(SlowQueue[endCharge.get(0)-FastChargingPileNum].size()>0) {
                            SlowQueue[endCharge.get(0) - FastChargingPileNum].remove(0);
                            if (SlowQueue[endCharge.get(0) - FastChargingPileNum].size() > 0) {
                                table newtable = new table();
                                newtable.pile = endCharge.get(0);
                                newtable.time = server.time;
                                newtable.type = 2;
                                newtable.addTable(newtable.time, newtable.type, newtable.pile, SlowQueue[endCharge.get(0) - FastChargingPileNum].get(0));
                            }
                        }
                    }
                    //调度事件

                    if (WaitQueue.size() > 0) {
                        //构造调度事件
                        events newevent = new events();
                        newevent.kind = 3;
                        newevent.usrid = WaitQueue.get(0);
                        //删除等待队列的第一个
                        WaitQueue.remove(0);
                        for(int i=0;i<WaitQueue.size();i++)
                        {
                            UserChargeStatus.get(WaitQueue.get(i)).position--;
                            databaseAccess.chargestatusWriteback(UserChargeStatus.get(WaitQueue.get(i)));
                        }
                        event.add(newevent);
                    }
                    event.remove(0);   //任务结束以后删除任务
                    endCharge.remove(0); //任务结束以后删除改任务
                }
                //充电开启任务
                else if(event.get(0).kind==2){
                    //对指定充电桩执行开始充电任务,其充电桩编号为startCharge.get(1)

                    //执行开始充电任务
                    //向时钟表里面加入一个充电结束时间节点
                    Double temp;
                    //更新用户状态信息
                    UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                    UserChargeStatus.get(event.get(0).usrid).status="充电中";
                    UserChargeStatus.get(event.get(0).usrid).chargingArea=true;
                    UserChargeStatus.get(event.get(0).usrid).position=0;
                    UserChargeStatus.get(event.get(0).usrid).pile=startCharge.get(0);
                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(event.get(0).usrid));
                    //更新充电桩等候服务信息
                    databaseAccess.waitStatusDelete(event.get(0).usrid);
                    if(server.chargeRequests.get(event.get(0).usrid).fast) temp=60*server.chargeRequests.get(event.get(0).usrid).amount/30;
                    else temp=60.0*server.chargeRequests.get(event.get(0).usrid).amount/7;
                    table newtable=new table();
                    newtable.type=1;
                    newtable.time=(int)(server.time+temp);
                    newtable.pile=startCharge.get(0);
                    newtable.userId=event.get(0).usrid;
                    newtable.addTable(newtable.time,newtable.type,newtable.pile,newtable.userId);
                    //创建账单
                    function.createBill(event.get(0).usrid,chargeRequests.get(event.get(0).usrid),startCharge.get(0),server.time);
                    event.remove(0);  //任务结束以后删除任务
                    startCharge.remove(0); //任务结束以后删除改任务
                }
                //调度任务
                else if(event.get(0).kind==3)
                {
                    boolean flag=false;
                    if(chargeRequests.get(event.get(0).usrid).fast)//快充模式
                    {
                        for(int i=0;i<FastChargingPileNum;i++)
                        {
                            if(FastPiles.get(i).status!=0)//假如有充电桩处于运行状态
                            {
                                flag=true;
                                break;
                            }
                        }
                        if(!flag) {System.out.println("Error");}//所有快充充电桩都处于故障状态，无法进行快充
                        else
                        {
                            index++;
                            boolean flag1=false;
                            for(int i=0;i<FastChargingPileNum;i++)
                            {
                                if(FastQueue[i].size()<ChargingQueueLen&&FastPiles.get(i).status==1)//看是否有充电桩是工作状态并且是空闲的
                                {
                                    flag1=true;
                                    break;
                                }
                            }
                            if(!flag1)//所有工作的充电桩都满了
                            {
                                if(WaitQueue.size()==WaitingAreaSize)
                                {
                                    //等候区也满了，所以他两个区域都不在
                                    UserChargeStatus.get(event.get(0).usrid).chargingArea=false;
                                    UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                                }
                                else//空闲区没满
                                {
                                    String wait = "F" + String.valueOf(index);
                                    //更新用户状态信息
                                    UserChargeStatus.get(event.get(0).usrid).waitingArea=true;
                                    UserChargeStatus.get(event.get(0).usrid).status="等候区排队中";
                                    UserChargeStatus.get(event.get(0).usrid).chargingArea=false;
                                    UserChargeStatus.get(event.get(0).usrid).position=WaitQueue.size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(event.get(0).usrid));
                                    Number.put(event.get(0).usrid, wait);
                                    WaitQueue.add(event.get(0).usrid);
                                }
                            }
                            else
                            {
                                boolean flag2=false;
                                //先看有没有空闲状态的充电桩
                                for(int i=0;i<FastChargingPileNum;i++)
                                {
                                    if(FastPiles.get(i).status==1&&FastQueue[i].size()==0)
                                    {
                                        flag2=true;
                                        String wait = "F" + String.valueOf(index);
                                        Number.put(event.get(0).usrid, wait);
                                        FastQueue[i].add(event.get(0).usrid);
                                        //创建时钟表
                                        table newtable = new table();
                                        newtable.pile = i;
                                        newtable.time = server.time;
                                        newtable.type = 2;
                                        UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                                        UserChargeStatus.get(event.get(0).usrid).status="充电中";
                                        UserChargeStatus.get(event.get(0).usrid).chargingArea=true;
                                        UserChargeStatus.get(event.get(0).usrid).pile=i;
                                        //添加到时间表
                                        newtable.addTable(newtable.time, newtable.type, newtable.pile, event.get(0).usrid);
                                        break;
                                    }
                                }
                                if(!flag2)//不存在空闲的充电桩
                                {
                                    int min=Integer.MAX_VALUE;
                                    int pileId=0;
                                    //循环找到时间最小(等待加充电时间最短)的充电桩
                                    for(int i=0;i<FastChargingPileNum;i++)
                                    {
                                        if(FastPiles.get(i).status==1&&FastQueue[i].size()<ChargingQueueLen)
                                        {
                                            int amount=0;
                                            for(int j=0;j<FastQueue[i].size();j++)
                                            {
                                                amount+=chargeRequests.get(FastQueue[i].get(j)).amount;//当前一共要冲的电量
                                            }
                                            //总时间=等待时间（总的充电时间-当前已经充电的时间）+自己需要的充电时间
                                            int temp=(int)(60*amount/30+TimeUser.get(FastQueue[i].get(0))-server.time+60*chargeRequests.get(event.get(0).usrid).amount/30);
                                            if(temp<min)
                                            {
                                                min=temp;
                                                pileId=i;
                                            }
                                        }
                                    }
                                    String wait = "F" + String.valueOf(index);
                                    Number.put(event.get(0).usrid, wait);
                                    //更新用户状态信息
                                    UserChargeStatus.get(event.get(0).usrid).pile=pileId;
                                    UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                                    UserChargeStatus.get(event.get(0).usrid).status="充电区等候中";
                                    UserChargeStatus.get(event.get(0).usrid).chargingArea=true;
                                    UserChargeStatus.get(event.get(0).usrid).position=FastQueue[pileId].size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(event.get(0).usrid));
                                    //更新充电桩等候信息
                                    PileWaitUser newuser=new PileWaitUser();
                                    newuser.id=event.get(0).usrid;
                                    newuser.status="Waiting";
                                    newuser.amount=chargeRequests.get(event.get(0).usrid).amount;
                                    newuser.totalAmount=chargeRequests.get(event.get(0).usrid).totalAmount;
                                    newuser.waitTime=server.time-TimeUser.get(event.get(0).usrid);
                                    PileWaitStatus newstatus=new PileWaitStatus();
                                    newstatus.id=pileId;
                                    System.out.println(pileId);
                                    newstatus.users=newuser;
                                    newstatus.users.waitTime= server.time-TimeUser.get(event.get(0).usrid);
                                    databaseAccess.waitStatusWriteBack(newstatus);
                                    FastQueue[pileId].add(event.get(0).usrid);
                                }
                            }
                        }
                    }
                    else//慢充模式
                    {
                        for(int i=0;i<TrickleChargingPileNum;i++)
                        {
                            if(SlowPiles.get(i).status!=0)//假如有充电桩处于运行状态
                            {
                                flag=true;
                                break;
                            }
                        }
                        if(!flag) {System.out.println("Error");}//所有快充充电桩都处于故障状态，无法进行快充
                        else
                        {
                            index++;
                            boolean flag1=false;
                            for(int i=0;i<TrickleChargingPileNum;i++)
                            {
                                if(SlowQueue[i].size()<ChargingQueueLen&&SlowPiles.get(i).status==1)//看是否有充电桩是工作状态并且是空闲的
                                {
                                    flag1=true;
                                    break;
                                }
                            }
                            if(!flag1)//所有工作的充电桩都满了
                            {
                                if(WaitQueue.size()==WaitingAreaSize)
                                {
                                    //等候区也满了，两个区域都不在
                                    UserChargeStatus.get(event.get(0).usrid).chargingArea=false;
                                    UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                                }
                                else//空闲区没满
                                {
                                    String wait = "T" + String.valueOf(index);
                                    UserChargeStatus.get(event.get(0).usrid).waitingArea=true;
                                    UserChargeStatus.get(event.get(0).usrid).status="等候区排队中";
                                    UserChargeStatus.get(event.get(0).usrid).chargingArea=false;
                                    UserChargeStatus.get(event.get(0).usrid).position=WaitQueue.size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(event.get(0).usrid));
                                    Number.put(event.get(0).usrid, wait);
                                    Number.put(event.get(0).usrid, wait);
                                    WaitQueue.add(event.get(0).usrid);
                                }
                            }
                            else
                            {
                                boolean flag2=false;
                                //先看有没有空闲状态的充电桩
                                for(int i=0;i<TrickleChargingPileNum;i++)
                                {
                                    if(SlowPiles.get(i).status==1&&SlowQueue[i].size()==0)
                                    {
                                        flag2=true;
                                        String wait = "T" + String.valueOf(index);
                                        Number.put(event.get(0).usrid, wait);
                                        SlowQueue[i].add(event.get(0).usrid);
                                        //创建时钟表
                                        table newtable = new table();
                                        newtable.pile = i+FastChargingPileNum;
                                        newtable.time = server.time;
                                        newtable.type = 2;
                                        UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                                        UserChargeStatus.get(event.get(0).usrid).status="充电中";
                                        UserChargeStatus.get(event.get(0).usrid).chargingArea=true;
                                        UserChargeStatus.get(event.get(0).usrid).pile=i+FastChargingPileNum;
                                        //添加到时间表
                                        newtable.addTable(newtable.time, newtable.type, newtable.pile, event.get(0).usrid);
                                        break;
                                    }
                                }
                                if(!flag2)//不存在空闲的充电桩
                                {
                                    int min=Integer.MAX_VALUE;
                                    int pileId=0;
                                    //循环找到时间最小(等待加充电时间最短)的充电桩
                                    for(int i=0;i<TrickleChargingPileNum;i++)
                                    {
                                        if(SlowPiles.get(i).status==1&&SlowQueue[i].size()<ChargingQueueLen)
                                        {
                                            int amount=0;
                                            for(int j=0;j<SlowQueue[i].size();j++)
                                            {
                                                amount+=chargeRequests.get(SlowQueue[i].get(j)).amount;//当前一共要冲的电量
                                            }
                                            //总时间=等待时间（总的充电时间-当前已经充电的时间）+自己需要的充电时间
                                            int temp=(int)(60*amount/7+TimeUser.get(SlowQueue[i].get(0))-server.time+60*chargeRequests.get(event.get(0).usrid).amount/7);
                                            if(temp<min)
                                            {
                                                min=temp;
                                                pileId=i;
                                            }
                                        }
                                    }
                                    String wait = "T" + String.valueOf(index);
                                    //更新用户状态信息
                                    UserChargeStatus.get(event.get(0).usrid).pile=pileId+FastChargingPileNum;
                                    UserChargeStatus.get(event.get(0).usrid).waitingArea=false;
                                    UserChargeStatus.get(event.get(0).usrid).status="充电区等候中";
                                    UserChargeStatus.get(event.get(0).usrid).chargingArea=true;
                                    UserChargeStatus.get(event.get(0).usrid).position=SlowQueue[pileId].size();
                                    databaseAccess.chargestatusWriteback(UserChargeStatus.get(event.get(0).usrid));
                                    //更新充电桩等候信息
                                    PileWaitUser newuser=new PileWaitUser();
                                    newuser.id=event.get(0).usrid;
                                    newuser.status="Waiting";
                                    newuser.amount=chargeRequests.get(event.get(0).usrid).amount;
                                    newuser.totalAmount=chargeRequests.get(event.get(0).usrid).totalAmount;
                                    newuser.waitTime=server.time-TimeUser.get(event.get(0).usrid);
                                    PileWaitStatus newstatus=new PileWaitStatus();
                                    newstatus.id=pileId+FastChargingPileNum;
                                    newstatus.users=newuser;
                                    newstatus.users.waitTime= server.time-TimeUser.get(event.get(0).usrid);
                                    databaseAccess.waitStatusWriteBack(newstatus);
                                    Number.put(event.get(0).usrid, wait);
                                    SlowQueue[pileId].add(event.get(0).usrid);
                                }
                            }
                        }
                    }

                    event.remove(0);   //任务结束以后删除任务
                }
            }
            }
        }


    }
}
class Time implements Runnable {
    int ms=5000;//每多少毫秒模拟一分钟

    @Override
    public void run() {
        //进程sleep
        try {
            while(true) {
                Thread.sleep(ms);
                server.time++;//时间加了一分钟
                if(server.time%10==0)
                {
                    //打印tables
                    System.out.println("时间表(10s刷新一次):"+"目前时间: "+server.time+" "+server.getTime(String.valueOf(server.time)));
                    for(int i=0;i<server.tables.size();i++)
                    {
                        System.out.println(" 预期时间"+server.tables.get(i).time+" 种类:"+server.tables.get(i).type+" 充电桩:"+server.tables.get(i).pile+" 用户:"+server.tables.get(i).userId);
                    }
                    //打印fastqueue和slowqueue
                    System.out.println("充电桩状态:");
                    for(int i=0;i<server.FastChargingPileNum;i++)
                    {
                        System.out.print("快充"+i+":");
                        System.out.println(server.FastQueue[i]);
                    }
                    for(int i=0;i<server.TrickleChargingPileNum;i++)
                    {
                        System.out.print("慢充"+i+":");
                        System.out.println(server.SlowQueue[i]);
                    }


                    if(WaitQueue.size()>0) {
                        System.out.print("等候区:");
                        System.out.println(WaitQueue + " ");
                    }
                    if(ErrorQueue.size()>0)
                    {
                        System.out.print("差错区:");
                        System.out.println(ErrorQueue);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 时间进程
    }

}
class Schedule implements Runnable {
    //时间表可能出现的任务
    //1代表充电结束,2代表充电开始

    //查时间表执行任务

    @Override
    public void run() {
        while(true){
            //时间找到对应时间,开始执行任务
            synchronized (server.lock) {
            if( server.tables.size()>0) {
                if (server.tables.get(0).time <= server.time) {

                        //执行任务
                        if (server.tables.get(0).type == 1) {

                            //向结束充电队列里添加充电桩
                            server.endCharge.add(server.tables.get(0).pile);
                            //在需要执行的队列里执行结束充电任务
                            server.events newevent = new server.events();
                            newevent.usrid = server.tables.get(0).userId;
                            newevent.kind = 1;
                            server.event.add(newevent);

                        } else if (server.tables.get(0).type == 2) {
                            //向开始充电队列里添加充电桩
                            server.startCharge.add(server.tables.get(0).pile);
                            //在需要执行的队列里执行开始充电任务
                            server.events newevent = new server.events();
                            newevent.usrid = server.tables.get(0).userId;
                            newevent.kind = 2;
                            server.event.add(newevent);
                        }
                        server.tables.remove(0);
                    }
                }
            }

        }

    }

}