package org.sunbird.user.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {
    public static Date addDaysToDate(Date dateObj){
        final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        // convert date to calendar
        Calendar c = Calendar.getInstance();
        c.setTime(dateObj);
        // manipulate date
        c.add(Calendar.DATE, 1);
        // convert calendar to date
        return c.getTime();
    }

    public static Timestamp getCurrentDateTimestamp(){
        return  new Timestamp(Calendar.getInstance().getTime().getTime());
    }
}
