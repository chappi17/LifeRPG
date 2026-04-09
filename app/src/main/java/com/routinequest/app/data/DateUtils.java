package com.routinequest.app.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static String today() { return formatDate(new Date()); }

    public static String formatDate(Date d) {
        synchronized (SDF) { return SDF.format(d); }
    }

    public static Date parseDate(String s) {
        try { synchronized (SDF) { return SDF.parse(s); } }
        catch (Exception e) { return new Date(); }
    }

    public static String shift(String date, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(parseDate(date));
        c.add(Calendar.DAY_OF_YEAR, days);
        return formatDate(c.getTime());
    }

    public static boolean isFuture(String date) {
        return date.compareTo(today()) > 0;
    }

    public static int daysBetween(String from, String to) {
        long diff = parseDate(to).getTime() - parseDate(from).getTime();
        return (int)(diff / 86400000L);
    }

    public static String displayLabel(String date) {
        Date d = parseDate(date);
        String today = today();
        if (date.equals(today)) return "오늘";
        int diff = daysBetween(date, today);
        if (diff == 1) return "어제";
        return diff + "일 전";
    }

    public static String formatKorean(String date) {
        Date d = parseDate(date);
        SimpleDateFormat f = new SimpleDateFormat("M월 d일 (E)", new Locale("ko"));
        return f.format(d);
    }

    public static String[] getLast7Days() {
        String[] arr = new String[7];
        for (int i = 6; i >= 0; i--) arr[6 - i] = shift(today(), -i);
        return arr;
    }

    public static String[] getDayLabels(String[] dates) {
        String[] labels = {"일","월","화","수","목","금","토"};
        String[] result = new String[dates.length];
        for (int i = 0; i < dates.length; i++) {
            Calendar c = Calendar.getInstance();
            c.setTime(parseDate(dates[i]));
            result[i] = labels[c.get(Calendar.DAY_OF_WEEK) - 1];
        }
        return result;
    }

    public static String[][] getMonthDates(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1);
        int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        String[][] result = new String[daysInMonth][2]; // [date, dayLabel]
        String[] dl = {"일","월","화","수","목","금","토"};
        for (int i = 0; i < daysInMonth; i++) {
            c.set(year, month, i + 1);
            result[i][0] = formatDate(c.getTime());
            result[i][1] = dl[c.get(Calendar.DAY_OF_WEEK) - 1];
        }
        return result;
    }

    public static int getFirstDayOfWeek(int year, int month) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, 1);
        return c.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
    }
}
