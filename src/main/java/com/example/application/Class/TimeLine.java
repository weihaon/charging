package com.example.application.Class;

import com.example.application.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TimeLine {
    public static ArrayList<String> Time=new ArrayList<>();
    public static ArrayList<Integer> Id=new ArrayList<>();
    public void Add(String time,int id )
    {
        Time.add(time);
        Id.add(id);
    }
    /*
    for(int i=0;i<server.tables.size();i++){
        //时间找到对应时间,开始执行任务
        if(server.tables.get(i).time==server.time){

            //执行任务
            if (server.tables.get(i).type==1){
                //在需要执行的队列里执行结束充电任务
                server.event.add(1);
                //向结束充电队列里添加充电桩
                server.endCharge.add(server.tables.get(i).pile);
            }
            else if(server.tables.get(i).type==2){
                //在需要执行的队列里执行开始充电任务
                server.event.add(2);
                //向开始充电队列里添加充电桩
                server.startCharge.add(server.tables.get(i).pile);
            }
        }
    }*/
}
