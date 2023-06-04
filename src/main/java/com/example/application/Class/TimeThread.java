package com.example.application.Class;
public class TimeThread extends Thread {
    private int hours;
    private int minutes;

    public TimeThread(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
    }

    @Override
    public void run() {
        try {
            while (true) {
                System.out.println(this.getTime());
                // 增加一分钟
                minutes=minutes+10;
                if (minutes == 60) {
                    minutes = 0;
                    hours++;
                    if (hours == 24) {
                        hours = 0;
                    }
                }
                // 线程休眠一秒
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public String getTime()
    {
        if(hours>=10&&minutes>=10)
        {
            return String.valueOf(hours)+":"+String.valueOf(minutes);
        }
        else if (hours<10&&minutes>=10)
        {
            return "0"+String.valueOf(hours)+":"+String.valueOf(minutes);
        }
        else if(hours>=10&&minutes<10)
        {
            return String.valueOf(hours)+":"+"0"+String.valueOf(minutes);
        }
        else
        {
            return "0"+String.valueOf(hours)+":"+"0"+String.valueOf(minutes);
        }
    }
}
