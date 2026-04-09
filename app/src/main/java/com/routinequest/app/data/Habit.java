package com.routinequest.app.data;

import java.util.HashMap;
import java.util.Map;

public class Habit {
    public String id;
    public String name;
    public String stat; // str, int, vit, wil, dex
    public Map<String, Boolean> completions = new HashMap<>();

    public Habit() {}

    public Habit(String id, String name, String stat) {
        this.id = id;
        this.name = name;
        this.stat = (stat != null) ? stat : "str";
    }

    public boolean isDoneOn(String date) {
        Boolean val = completions.get(date);
        return val != null && val;
    }

    public void setDone(String date, boolean done) {
        if (done) completions.put(date, true);
        else completions.remove(date);
    }

    public int getTotalCount() {
        int count = 0;
        for (Boolean v : completions.values()) if (v != null && v) count++;
        return count;
    }

    public int getStreak(String today) {
        int s = 0;
        long t = DateUtils.parseDate(today).getTime();
        while (true) {
            String d = DateUtils.formatDate(new java.util.Date(t));
            if (isDoneOn(d)) { s++; t -= 86400000L; }
            else break;
        }
        return s;
    }

    public int getBestStreak() {
        if (completions.isEmpty()) return 0;
        java.util.List<String> dates = new java.util.ArrayList<>();
        for (Map.Entry<String, Boolean> e : completions.entrySet())
            if (e.getValue() != null && e.getValue()) dates.add(e.getKey());
        if (dates.isEmpty()) return 0;
        java.util.Collections.sort(dates);
        int best = 1, cur = 1;
        for (int i = 1; i < dates.size(); i++) {
            long prev = DateUtils.parseDate(dates.get(i-1)).getTime();
            long curr = DateUtils.parseDate(dates.get(i)).getTime();
            if ((curr - prev) == 86400000L) { cur++; best = Math.max(best, cur); }
            else cur = 1;
        }
        return best;
    }
}
