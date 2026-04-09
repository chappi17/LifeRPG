package com.routinequest.app.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameEngine {

    // ── Stat metadata ─────────────────────────────
    public static final String[] STAT_KEYS = {"str","int","vit","wil","dex"};
    public static final String[] STAT_LABELS = {"STR","INT","VIT","WIL","DEX"};
    public static final String[] STAT_NAMES = {"체력","지성","생명력","의지","민첩"};
    public static final String[] STAT_ICONS = {"💪","🧠","🌿","🔮","⚡"};
    public static final int[] STAT_COLORS = {
        0xFFFF6B6B, 0xFF60A5FA, 0xFF34D399, 0xFFA78BFA, 0xFFFBBF24
    };
    public static int statIndex(String key) {
        for (int i = 0; i < STAT_KEYS.length; i++) if (STAT_KEYS[i].equals(key)) return i;
        return 0;
    }

    // ── Character classes ─────────────────────────
    static final int[][] CLASS_TABLE = {{1,0},{5,1},{10,2},{15,3},{20,4},{30,5},{40,6}};
    static final String[] CLASS_NAMES = {"견습 모험가","수련자","모험가","용사","영웅","전설","신화"};
    static final String[] CLASS_TITLES = {
        "용기를 갈고닦는 중","검을 쥐는 법을 배우다","던전의 문을 두드리다",
        "악을 물리치는 자","전설이 되어가는 길","이름이 역사에 새겨지다","신들도 두려워하는 자"
    };
    static final String[] CLASS_AVATARS = {"🧑","🧙","⚔️","🦸","🌟","👑","⚡"};

    public static int getClassIndex(int level) {
        int idx = 0;
        for (int[] row : CLASS_TABLE) if (level >= row[0]) idx = row[1];
        return idx;
    }
    public static String getClassName(int level)  { return CLASS_NAMES[getClassIndex(level)]; }
    public static String getClassTitle(int level) { return CLASS_TITLES[getClassIndex(level)]; }
    public static String getClassAvatar(int level){ return CLASS_AVATARS[getClassIndex(level)]; }

    // ── XP / Level ────────────────────────────────
    public static int xpForLevel(int lv) { return lv * lv * 80; }

    public static int calcLevel(int totalXP) {
        int lv = 1;
        while (xpForLevel(lv + 1) <= totalXP) lv++;
        return Math.min(lv, 50);
    }

    public static int calcTotalXP(List<Habit> habits) {
        int xp = 0;
        for (Habit h : habits) xp += h.getTotalCount() * 20;
        return xp;
    }

    public static int[] xpProgress(int totalXP) {
        int lv = calcLevel(totalXP);
        int cur = xpForLevel(lv), nxt = xpForLevel(lv + 1);
        int progress = totalXP - cur, needed = nxt - cur;
        int pct = needed > 0 ? (int)(100f * progress / needed) : 100;
        return new int[]{lv, progress, needed, pct};
    }

    public static double streakMultiplier(int streak) {
        if (streak >= 66) return 5;
        if (streak >= 30) return 3;
        if (streak >= 14) return 2;
        if (streak >= 7)  return 1.5;
        return 1;
    }

    // ── Stats ─────────────────────────────────────
    public static int[] calcStats(List<Habit> habits) {
        int[] stats = new int[5];
        for (Habit h : habits) {
            int idx = statIndex(h.stat);
            stats[idx] += h.getTotalCount();
        }
        return stats;
    }

    // ── Summary ───────────────────────────────────
    public static class Summary {
        public int totalXP, level, totalCompletions, maxStreak;
        public int totalHabits, perfectDays, perfectWeeks, multiStreakCount;
        public int[] stats;
    }

    public static Summary calcSummary(List<Habit> habits) {
        Summary s = new Summary();
        String today = DateUtils.today();
        s.totalHabits = habits.size();
        s.stats = calcStats(habits);

        for (Habit h : habits) {
            s.totalCompletions += h.getTotalCount();
            int streak = h.getStreak(today);
            if (streak > s.maxStreak) s.maxStreak = streak;
        }

        s.totalXP = calcTotalXP(habits);
        s.level = calcLevel(s.totalXP);

        // Perfect days
        if (!habits.isEmpty()) {
            Set<String> allDates = new HashSet<>();
            for (Habit h : habits)
                for (String d : h.completions.keySet())
                    if (h.completions.get(d) != null && h.completions.get(d)) allDates.add(d);
            for (String date : allDates) {
                if (date.compareTo(today) > 0) continue;
                boolean perfect = true;
                for (Habit h : habits) if (!h.isDoneOn(date)) { perfect = false; break; }
                if (perfect) s.perfectDays++;
            }
            // Perfect weeks — check each Mon-Sun block from first record to today
            if (!allDates.isEmpty()) {
                List<String> sorted = new ArrayList<>(allDates);
                java.util.Collections.sort(sorted);
                java.util.Calendar weekStart = java.util.Calendar.getInstance();
                weekStart.setTime(DateUtils.parseDate(sorted.get(0)));
                // Align to Monday
                int dow = weekStart.get(java.util.Calendar.DAY_OF_WEEK);
                int backDays = (dow == java.util.Calendar.SUNDAY) ? 6 : dow - java.util.Calendar.MONDAY;
                weekStart.add(java.util.Calendar.DAY_OF_YEAR, -backDays);

                while (true) {
                    String weekStartStr = DateUtils.formatDate(weekStart.getTime());
                    if (weekStartStr.compareTo(today) > 0) break;

                    boolean perfectWeek = true;
                    java.util.Calendar day = (java.util.Calendar) weekStart.clone();
                    for (int i = 0; i < 7; i++) {
                        String d = DateUtils.formatDate(day.getTime());
                        if (d.compareTo(today) > 0) { perfectWeek = false; break; }
                        for (Habit h : habits) {
                            if (!h.isDoneOn(d)) { perfectWeek = false; break; }
                        }
                        if (!perfectWeek) break;
                        day.add(java.util.Calendar.DAY_OF_YEAR, 1);
                    }
                    if (perfectWeek) s.perfectWeeks++;
                    weekStart.add(java.util.Calendar.DAY_OF_YEAR, 7);
                }
            }
        }

        s.multiStreakCount = 0;
        for (Habit h : habits) if (h.getStreak(today) >= 7) s.multiStreakCount++;
        return s;
    }

    // ── Achievement tiers per habit ────────────────
    public static class HabitAchievement {
        public String id, name, req, icon;
        public boolean unlocked;
        public int current, target;
        public int color;
        public String type; // "streak" or "count"
    }

    public static List<HabitAchievement> buildHabitAchs(Habit h) {
        int streak = h.getBestStreak();
        int count  = h.getTotalCount();
        int color  = STAT_COLORS[statIndex(h.stat)];
        String n   = h.name.length() > 9 ? h.name.substring(0, 9) + "…" : h.name;
        List<HabitAchievement> list = new ArrayList<>();

        // {target, icon, label}
        Object[][] streakTiers = {
            {1,   "🌱", "첫 시작"},
            {3,   "🌿", "3일 연속"},
            {7,   "⚔️", "7일 전사"},
            {14,  "🌳", "2주 기사"},
            {30,  "🏆", "한 달 영웅"},
            {66,  "⭐", "66일 전설"},
            {100, "👑", "백일의 왕"}
        };
        for (Object[] t : streakTiers) {
            int target = (int) t[0];
            HabitAchievement a = new HabitAchievement();
            a.type    = "streak";
            a.target  = target;
            a.current = Math.min(streak, target);
            a.icon    = (String) t[1];
            a.name    = n + " " + t[2];
            a.req     = target + "일 연속";
            a.unlocked = streak >= target;
            a.color   = color;
            a.id      = h.id + "_s" + target;
            list.add(a);
        }

        Object[][] countTiers = {
            {5,   "📍", "다섯 걸음"},
            {20,  "💪", "스무 걸음"},
            {50,  "🔥", "50회 돌파"},
            {100, "💎", "100회 달인"},
            {200, "🌟", "200회 고수"},
            {365, "🌌", "1년의 기록"}
        };
        for (Object[] t : countTiers) {
            int target = (int) t[0];
            HabitAchievement a = new HabitAchievement();
            a.type    = "count";
            a.target  = target;
            a.current = Math.min(count, target);
            a.icon    = (String) t[1];
            a.name    = n + " " + t[2];
            a.req     = target + "회 달성";
            a.unlocked = count >= target;
            a.color   = color;
            a.id      = h.id + "_c" + target;
            list.add(a);
        }
        return list;
    }

    // ── Global Achievements ───────────────────────
    public static class Achievement {
        public String id, name, req, icon;
        public boolean unlocked;
        public int rarity; // 1=common 2=rare 3=epic 4=legendary
    }

    public static List<Achievement> buildGlobalAchs(Summary s) {
        List<Achievement> list = new ArrayList<>();
        Object[][] defs = {
            // id, name, req, icon, rarity, unlocked
            {"lv5",  "이름을 얻다",  "레벨 5",   "🏷️",1, s.level>=5},
            {"lv10", "두 자릿수",    "레벨 10",  "🏅",2, s.level>=10},
            {"lv20", "은빛 영혼",    "레벨 20",  "🥈",2, s.level>=20},
            {"lv30", "황금 용사",    "레벨 30",  "🥇",3, s.level>=30},
            {"lv50", "신화의 경지",  "레벨 50",  "🌌",4, s.level>=50},
            {"perf1","완벽한 하루",  "완벽한 날 1번","✨",1,s.perfectDays>=1},
            {"perf7","완벽한 주간",  "완벽한 날 7번","🌈",2,s.perfectDays>=7},
            {"perf30","퍼펙트 달인","완벽한 날 30번","☀️",3,s.perfectDays>=30},
            {"m2",  "이도류",        "2개 동시 7일+","⚔️",2,s.multiStreakCount>=2},
            {"m3",  "삼신기",        "3개 동시 7일+","🔱",3,s.multiStreakCount>=3},
            {"m5",  "요새의 주인",   "5개 동시 7일+","🏰",4,s.multiStreakCount>=5},
            {"h1",  "첫 퀘스트",     "습관 1개 등록","📌",1,s.totalHabits>=1},
            {"h3",  "삼위일체",      "습관 3개 등록","📚",1,s.totalHabits>=3},
            {"h5",  "다재다능",      "습관 5개 등록","🎒",2,s.totalHabits>=5},
            {"w1",  "주간 정복",     "한 주 완주 1회","🗓️",2,s.perfectWeeks>=1},
            {"w4",  "4주 정복",      "주간 완주 4회","🏆",3,s.perfectWeeks>=4},
            {"str100","근육의 신",   "STR 100","💪",3,s.stats[0]>=100},
            {"int100","지식의 탑",   "INT 100","🧠",3,s.stats[1]>=100},
            {"vit100","생명의 나무", "VIT 100","🌿",3,s.stats[2]>=100},
            {"wil100","의지의 수정", "WIL 100","🔮",3,s.stats[3]>=100},
            {"dex100","번개의 발",   "DEX 100","⚡",3,s.stats[4]>=100},
        };
        for (Object[] d : defs) {
            Achievement a = new Achievement();
            a.id = (String)d[0]; a.name = (String)d[1]; a.req = (String)d[2];
            a.icon = (String)d[3]; a.rarity = (Integer)d[4]; a.unlocked = (Boolean)d[5];
            list.add(a);
        }
        return list;
    }

    // ── Titles ────────────────────────────────────
    public static List<Title> buildTitles(Summary s, List<Habit> habits) {
        List<Title> list = new ArrayList<>();
        // [id, name, condition, rarity, unlocked]
        Object[][] defs = {
            {"t_start",  "신참 모험가",    "첫 번째 습관 등록",         Title.COMMON,    s.totalHabits >= 1},
            {"t_steady", "꾸준한 자",      "7일 연속 달성",             Title.COMMON,    s.maxStreak >= 7},
            {"t_iron",   "철의 의지",      "30일 연속 달성",            Title.RARE,      s.maxStreak >= 30},
            {"t_century","백일의 수련자",  "100일 연속 달성",           Title.EPIC,      s.maxStreak >= 100},
            {"t_perfect","완벽주의자",     "완벽한 날 10번",            Title.RARE,      s.perfectDays >= 10},
            {"t_flawless","무결점 영웅",   "완벽한 날 30번",            Title.EPIC,      s.perfectDays >= 30},
            {"t_lv10",   "수련의 기사",    "레벨 10 달성",              Title.COMMON,    s.level >= 10},
            {"t_lv20",   "은빛 검사",      "레벨 20 달성",              Title.RARE,      s.level >= 20},
            {"t_lv30",   "황금 용사",      "레벨 30 달성",              Title.EPIC,      s.level >= 30},
            {"t_lv50",   "신화의 존재",    "레벨 50 달성",              Title.LEGENDARY, s.level >= 50},
            {"t_str",    "근육의 신",      "STR 100 달성",              Title.EPIC,      s.stats[0] >= 100},
            {"t_int",    "지식의 현자",    "INT 100 달성",              Title.EPIC,      s.stats[1] >= 100},
            {"t_vit",    "불사의 몸",      "VIT 100 달성",              Title.EPIC,      s.stats[2] >= 100},
            {"t_wil",    "강철 의지",      "WIL 100 달성",              Title.EPIC,      s.stats[3] >= 100},
            {"t_dex",    "바람의 발",      "DEX 100 달성",              Title.EPIC,      s.stats[4] >= 100},
            {"t_multi",  "만능 영웅",      "3가지 습관 동시 7일+",      Title.RARE,      s.multiStreakCount >= 3},
            {"t_legend", "전설의 도전자",  "업적 10개 달성",            Title.LEGENDARY, countUnlockedGlobal(s) >= 10},
            {"t_week4",  "주간 챔피언",    "주간 완주 4회",             Title.RARE,      s.perfectWeeks >= 4},
        };
        for (Object[] d : defs) {
            Title t = new Title((String)d[0],(String)d[1],(String)d[2],(Integer)d[3]);
            t.unlocked = (Boolean)d[4];
            list.add(t);
        }
        return list;
    }

    private static int countUnlockedGlobal(Summary s) {
        return buildGlobalAchs(s).stream().mapToInt(a -> a.unlocked ? 1 : 0).sum();
    }

    public static Title findTitle(List<Title> titles, String id) {
        for (Title t : titles) if (t.id.equals(id)) return t;
        return null;
    }
}
