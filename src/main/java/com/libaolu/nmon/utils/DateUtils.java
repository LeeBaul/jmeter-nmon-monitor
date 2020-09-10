package com.libaolu.nmon.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * <p/>
 *
 * @author libaolu
 * @version 1.0
 * @dateTime 2020/4/29 14:43
 **/
public class DateUtils {
    public static boolean isExpire(String date) {
        Long value = dateToStamp(date, "yyyy-MM-dd HH:mm:ss");
        return (System.currentTimeMillis() >= value);
    }

    public static boolean isExpire(String date1,String date2) {
        Long value1 = dateToStamp(date1, "yyyy-MM-dd HH:mm:ss");
        Long value2 = dateToStamp(date2, "yyyy-MM-dd HH:mm:ss");
        return (value1 >= value2);
    }

    public static String stampToDate(Long timeStamp, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String sd = sdf.format(new Date(Long.parseLong(String.valueOf(timeStamp))));

        return sd;
    }

    public static String stampToDate(Long timeStamp) {
        return stampToDate(timeStamp, "yyyy-MM-dd HH:mm:ss");
    }

    public static Long dateToStamp(String date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date2 = null;
        try {
            date2 = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date2.getTime();
    }

    public static Long dateToStamp(String date) {
        return dateToStamp(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static String getCurrentDate(String format) {
        Date date = new Date();
        return new SimpleDateFormat(format).format(date);
    }


    public static String getCurrentDate() {
        String format = "yyyy-MM-dd HH:mm:ss";
        return getCurrentDate(format);
    }

    public static String totalTime(long start,long end) {
        long temp = end -start;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        return sdf.format((temp - TimeZone.getDefault().getRawOffset()));
    }
}
