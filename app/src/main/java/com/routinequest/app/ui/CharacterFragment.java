package com.routinequest.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.routinequest.app.R;
import com.routinequest.app.data.DataManager;
import com.routinequest.app.data.DateUtils;
import com.routinequest.app.data.GameEngine;
import com.routinequest.app.data.GameEngine.Achievement;
import com.routinequest.app.data.GameEngine.HabitAchievement;
import com.routinequest.app.data.GameEngine.Summary;
import com.routinequest.app.data.Habit;
import com.routinequest.app.data.Title;
import com.routinequest.app.databinding.FragmentCharacterBinding;

import java.util.List;

public class CharacterFragment extends Fragment implements MainActivity.Refreshable {

    private FragmentCharacterBinding b;
    private DataManager dm;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        b = FragmentCharacterBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle s) {
        dm = DataManager.get(requireContext());
        buildAll();
    }

    @Override public void onRefresh() { buildAll(); }

    private void buildAll() {
        if (b == null) return;
        List<Habit> habits = dm.getHabits();
        Summary sum = GameEngine.calcSummary(habits);
        int[] xpProg = GameEngine.xpProgress(sum.totalXP);
        int lv = xpProg[0], curXp = xpProg[1], neededXp = xpProg[2], pct = xpProg[3];

        buildCharCard(lv, curXp, neededXp, pct, sum);
        buildTitles(sum, habits);
        buildStats(sum);
        buildSkills(habits);
        buildAchievements(sum, habits);
    }

    // ── Character Card ────────────────────────────
    private void buildCharCard(int lv, int curXp, int needed, int pct, Summary sum) {
        b.tvAvatar.setText(GameEngine.getClassAvatar(lv));
        b.tvLevel.setText("Lv." + lv);
        b.tvClassName.setText(GameEngine.getClassName(lv));
        b.tvClassTitle.setText(GameEngine.getClassTitle(lv));
        b.tvTotalXp.setText("총 " + sum.totalXP + " XP 획득");
        b.tvXpProgress.setText(curXp + " / " + needed + " XP");

        // Animated XP bar
        ValueAnimator anim = ValueAnimator.ofInt(0, pct);
        anim.setDuration(800);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            if (b == null) return;
            int p = (int) a.getAnimatedValue();
            b.xpBar.setProgress(p);
        });
        anim.start();
    }

    // ── Titles ────────────────────────────────────
    private void buildTitles(Summary sum, List<Habit> habits) {
        b.titleContainer.removeAllViews();
        List<Title> titles = GameEngine.buildTitles(sum, habits);
        String equippedId = dm.getEquippedTitleId();

        // Equipped title banner
        Title equipped = GameEngine.findTitle(titles, equippedId);
        if (equipped == null || !equipped.unlocked) {
            // auto-equip first unlocked
            for (Title t : titles) if (t.unlocked) { equipped = t; dm.equipTitle(t.id); break; }
        }
        if (equipped != null) {
            View banner = buildEquippedBanner(equipped);
            b.titleContainer.addView(banner);
        }

        // Title list
        for (Title t : titles) {
            View row = buildTitleRow(t, t.id.equals(equippedId));
            b.titleContainer.addView(row);
        }
    }

    private View buildEquippedBanner(Title t) {
        Context ctx = requireContext();
        CardView card = makeCard(ctx, 16, 0xFFF5C842, 0x1AFFFFFF);
        LinearLayout inner = makeLl(ctx, LinearLayout.HORIZONTAL, 14, 14);
        TextView ico = makeText(ctx, "👑", 22, 0xFFF5C842, false);
        LinearLayout info = makeLl(ctx, LinearLayout.VERTICAL, 0, 0);
        TextView sub = makeText(ctx, "현재 장착 칭호", 10, 0xFF6B6890, false);
        TextView name = makeText(ctx, t.name, 16, 0xFFF5C842, true);
        info.addView(sub); info.addView(name);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        info.setLayoutParams(infoLp);
        TextView rarity = makeText(ctx, t.rarityLabel(), 10, t.rarityColor(), true);
        setRoundBg(rarity, t.rarityColor(), 10);
        rarity.setPadding(dp(8), dp(3), dp(8), dp(3));
        inner.addView(ico); inner.addView(makeSpace(ctx, 12)); inner.addView(info); inner.addView(rarity);
        card.addView(inner);
        card.setOnClickListener(v -> { /* already equipped */ });
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        return card;
    }

    private View buildTitleRow(Title t, boolean isEquipped) {
        Context ctx = requireContext();
        CardView card = makeCard(ctx, 12,
            isEquipped ? 0x1AF5C842 : 0xFF1A1830,
            isEquipped ? 0x33F5C842 : 0xFF2E2B50);
        LinearLayout inner = makeLl(ctx, LinearLayout.HORIZONTAL, 12, 12);
        // rarity dot
        View dot = new View(ctx);
        int sz = dp(8); LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(sz, sz);
        dlp.gravity = Gravity.CENTER_VERTICAL; dot.setLayoutParams(dlp);
        GradientDrawable dotBg = new GradientDrawable(); dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(t.unlocked ? t.rarityColor() : 0xFF2E2B50);
        dot.setBackground(dotBg);
        LinearLayout info = makeLl(ctx, LinearLayout.VERTICAL, 0, 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView name = makeText(ctx, t.name, 13, t.unlocked ? 0xFFE8E4F5 : 0xFF6B6890, true);
        TextView cond = makeText(ctx, t.condition, 10, 0xFF6B6890, false);
        info.addView(name); info.addView(cond);
        TextView badge = makeText(ctx, t.unlocked ? (isEquipped ? "장착 중" : "탭하여 장착") : "🔒", 10,
            t.unlocked ? (isEquipped ? 0xFFF5C842 : 0xFF6B6890) : 0xFF6B6890, false);
        inner.addView(dot); inner.addView(makeSpace(ctx, 8)); inner.addView(info); inner.addView(badge);
        card.addView(inner);
        if (t.unlocked && !isEquipped) {
            card.setOnClickListener(v -> {
                dm.equipTitle(t.id);
                animate(card); buildTitles(GameEngine.calcSummary(dm.getHabits()), dm.getHabits());
                Toast.makeText(ctx, "'" + t.name + "' 칭호를 장착했어요!", Toast.LENGTH_SHORT).show();
            });
        }
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(6); card.setLayoutParams(lp);
        card.setAlpha(t.unlocked ? 1f : 0.4f);
        return card;
    }

    // ── Stats ─────────────────────────────────────
    private void buildStats(Summary sum) {
        b.statsGrid.removeAllViews();
        int[] stats = sum.stats;
        for (int i = 0; i < GameEngine.STAT_KEYS.length; i++) {
            View card = buildStatCard(i, stats[i]);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.columnSpec = GridLayout.spec(i % 2, GridLayout.FILL, 1f);
            lp.rowSpec = GridLayout.spec(i / 2);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            card.setLayoutParams(lp);
            b.statsGrid.addView(card);
        }
    }

    private View buildStatCard(int idx, int val) {
        Context ctx = requireContext();
        int color = GameEngine.STAT_COLORS[idx];
        CardView card = makeCard(ctx, 14, 0xFF1A1830, color);
        LinearLayout inner = makeLl(ctx, LinearLayout.VERTICAL, 14, 14);
        LinearLayout top = makeLl(ctx, LinearLayout.HORIZONTAL, 0, 0);
        TextView ico = makeText(ctx, GameEngine.STAT_ICONS[idx], 16, color, false);
        TextView lbl = makeText(ctx, "  " + GameEngine.STAT_LABELS[idx] + " · " + GameEngine.STAT_NAMES[idx], 10, 0xFF6B6890, false);
        top.addView(ico); top.addView(lbl);
        TextView valTv = makeText(ctx, String.valueOf(val), 24, color, true);
        TextView desc = makeText(ctx, "총 " + val + "회 달성", 10, 0xFF6B6890, false);
        inner.addView(top); inner.addView(valTv); inner.addView(desc);
        card.addView(inner);
        // animate count
        ValueAnimator a = ValueAnimator.ofInt(0, val);
        a.setDuration(600); a.setStartDelay(idx * 80L);
        a.addUpdateListener(an -> valTv.setText(String.valueOf((int)an.getAnimatedValue())));
        a.start();
        return card;
    }

    // ── Skills ────────────────────────────────────
    private void buildSkills(List<Habit> habits) {
        b.skillsContainer.removeAllViews();
        if (habits.isEmpty()) {
            b.skillsContainer.addView(makeEmptyView("🌱", "습관을 추가하면 스킬이 해금됩니다!"));
            return;
        }
        String[][] milestones = {{"3","🌱","입문"},{"7","🌿","수련"},{"14","🌳","숙련"},{"30","🏆","고급"},{"66","⭐","마스터"},{"100","🔥","전설"}};
        for (Habit h : habits) {
            int streak = h.getBestStreak();
            int color = GameEngine.STAT_COLORS[GameEngine.statIndex(h.stat)];
            String[] best = null;
            String[] next = milestones[0];
            for (String[] m : milestones) {
                int d = Integer.parseInt(m[0]);
                if (streak >= d) best = m;
                else { next = m; break; }
            }
            View card = buildSkillCard(h, best, next, streak, color);
            b.skillsContainer.addView(card);
        }
    }

    private View buildSkillCard(Habit h, String[] best, String[] next, int streak, int color) {
        Context ctx = requireContext();
        CardView card = makeCard(ctx, 12, 0xFF1A1830, 0xFF2E2B50);
        LinearLayout inner = makeLl(ctx, LinearLayout.HORIZONTAL, 12, 12);
        // Icon
        FrameLayout iconBg = new FrameLayout(ctx);
        int iconSz = dp(44);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSz, iconSz);
        iconBg.setLayoutParams(iconLp);
        GradientDrawable iconBgD = new GradientDrawable();
        iconBgD.setCornerRadius(dp(10));
        iconBgD.setColor((color & 0x00FFFFFF) | 0x22000000);
        iconBg.setBackground(iconBgD);
        TextView iconTv = makeText(ctx, best != null ? best[1] : next[1], 20, color, false);
        iconTv.setGravity(Gravity.CENTER);
        iconTv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iconBg.addView(iconTv);
        // Info
        LinearLayout info = makeLl(ctx, LinearLayout.VERTICAL, 0, 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        String tierName = best != null ? h.name + " · " + best[2] : h.name;
        TextView name = makeText(ctx, tierName, 13, best != null ? color : 0xFFE8E4F5, true);
        int totalDone = h.getTotalCount();
        String desc = GameEngine.STAT_ICONS[GameEngine.statIndex(h.stat)] + " " +
            GameEngine.STAT_NAMES[GameEngine.statIndex(h.stat)] + " · 연속 " + streak + "일 · 총 " + totalDone + "회";
        TextView descTv = makeText(ctx, desc, 10, 0xFF6B6890, false);
        info.addView(name); info.addView(descTv);
        if (best == null && next != null) {
            TextView progress = makeText(ctx, next[0] + "일 연속 달성 시 해금 (" + streak + "/" + next[0] + "일)", 10, 0xFF6B6890, false);
            info.addView(progress);
        }
        // Badge
        TextView badge = makeText(ctx, best != null ? best[2] : "잠김", 10,
            best != null ? 0xFFF5C842 : 0xFF6B6890, true);
        setRoundBg(badge, best != null ? 0x1AF5C842 : 0x0AFFFFFF, 20);
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        inner.addView(iconBg); inner.addView(makeSpace(ctx, 0));
        inner.addView(info); inner.addView(badge);
        card.setAlpha(best != null ? 1f : 0.5f);
        card.addView(inner);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8); card.setLayoutParams(lp);
        return card;
    }

    // ── Achievements ──────────────────────────────
    private void buildAchievements(Summary sum, List<Habit> habits) {
        b.achContainer.removeAllViews();
        int totalUnlocked = 0, totalAll = 0;
        // Per-habit
        for (Habit h : habits) {
            List<HabitAchievement> achs = GameEngine.buildHabitAchs(h);
            totalAll += achs.size();
            for (HabitAchievement a : achs) if (a.unlocked) totalUnlocked++;
            b.achContainer.addView(buildHabitAchSection(h, achs));
        }
        // Global
        List<Achievement> globals = GameEngine.buildGlobalAchs(sum);
        totalAll += globals.size();
        for (Achievement a : globals) if (a.unlocked) totalUnlocked++;
        b.tvAchCount.setText("🏆 업적 (" + totalUnlocked + "/" + totalAll + ")");
        b.achContainer.addView(buildGlobalAchSection(globals));
    }

    private View buildHabitAchSection(Habit h, List<HabitAchievement> achs) {
        Context ctx = requireContext();
        int color = GameEngine.STAT_COLORS[GameEngine.statIndex(h.stat)];
        CardView card = makeCard(ctx, 16, 0xFF1A1830, 0xFF2E2B50);
        LinearLayout outer = makeLl(ctx, LinearLayout.VERTICAL, 0, 0);
        // Header
        LinearLayout header = makeLl(ctx, LinearLayout.HORIZONTAL, 14, 12);
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setColor((color & 0x00FFFFFF) | 0x12000000);
        header.setBackground(headerBg);
        View dot = new View(ctx); int dsz = dp(8);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dsz, dsz);
        dlp.gravity = Gravity.CENTER_VERTICAL; dot.setLayoutParams(dlp);
        GradientDrawable dotD = new GradientDrawable(); dotD.setShape(GradientDrawable.OVAL); dotD.setColor(color);
        dot.setBackground(dotD);
        TextView hName = makeText(ctx, h.name, 13, 0xFFE8E4F5, true);
        hName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        int unlocked = 0; for (HabitAchievement a : achs) if (a.unlocked) unlocked++;
        TextView cnt = makeText(ctx, unlocked + "/" + achs.size(), 10, 0xFF6B6890, false);
        header.addView(dot); header.addView(makeSpace(ctx, 8)); header.addView(hName); header.addView(cnt);
        outer.addView(header);
        // Stats mini
        int streak = h.getBestStreak(), total = h.getTotalCount(), curStreak = h.getStreak(DateUtils.today());
        LinearLayout miniStats = makeLl(ctx, LinearLayout.HORIZONTAL, 10, 10);
        miniStats.addView(buildMiniStat(ctx, String.valueOf(streak), "최장 연속", color));
        miniStats.addView(buildMiniStat(ctx, String.valueOf(total), "누적 달성", color));
        miniStats.addView(buildMiniStat(ctx, String.valueOf(curStreak), "현재 연속", color));
        outer.addView(miniStats);
        // Streak achs
        outer.addView(buildAchSubHeader(ctx, "연속 달성"));
        for (HabitAchievement a : achs) {
            if ("streak".equals(a.type)) outer.addView(buildHabitAchRow(a, color));
        }
        // Count achs
        outer.addView(buildAchSubHeader(ctx, "누적 달성"));
        for (HabitAchievement a : achs) {
            if ("count".equals(a.type)) outer.addView(buildHabitAchRow(a, color));
        }
        card.addView(outer);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10); card.setLayoutParams(lp);
        return card;
    }

    private View buildHabitAchRow(HabitAchievement a, int color) {
        Context ctx = requireContext();
        LinearLayout row = makeLl(ctx, LinearLayout.HORIZONTAL, 12, 8);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        if (a.unlocked) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor((color & 0x00FFFFFF) | 0x08000000);
            bg.setStroke(1, (color & 0x00FFFFFF) | 0x20000000);
            bg.setCornerRadius(dp(8));
            row.setBackground(bg);
        }
        TextView ico = makeText(ctx, a.icon, 18, color, false);
        LinearLayout info = makeLl(ctx, LinearLayout.VERTICAL, 0, 0);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView name = makeText(ctx, a.name, 12, a.unlocked ? 0xFFE8E4F5 : 0xFF6B6890, true);
        // Progress bar
        ProgressBar pb = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(a.unlocked ? 100 : (a.target > 0 ? (int)(100f * a.current / a.target) : 0));
        pb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)));
        info.addView(name); info.addView(pb);
        TextView valTv = makeText(ctx, a.current + "/" + a.target + (a.type.equals("streak") ? "일" : "회"),
            10, a.unlocked ? color : 0xFF6B6890, false);
        row.addView(ico); row.addView(makeSpace(ctx, 4)); row.addView(info); row.addView(valTv);
        row.setAlpha(a.unlocked ? 1f : 0.45f);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), dp(2), dp(10), dp(2)); row.setLayoutParams(lp);
        return row;
    }

    private View buildGlobalAchSection(List<Achievement> achs) {
        Context ctx = requireContext();
        CardView card = makeCard(ctx, 16, 0xFF1A1830, 0xFF2E2B50);
        LinearLayout outer = makeLl(ctx, LinearLayout.VERTICAL, 0, 0);
        outer.addView(buildAchSubHeader(ctx, "전체 기록"));
        for (Achievement a : achs) {
            LinearLayout row = makeLl(ctx, LinearLayout.HORIZONTAL, 14, 10);
            if (a.unlocked) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x0AF5C842); bg.setStroke(1, 0x33F5C842); bg.setCornerRadius(dp(8));
                row.setBackground(bg);
            }
            TextView ico = makeText(ctx, a.icon, 20, 0xFFE8E4F5, false);
            LinearLayout info = makeLl(ctx, LinearLayout.VERTICAL, 0, 0);
            info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView name = makeText(ctx, a.name, 12, a.unlocked ? 0xFFE8E4F5 : 0xFF6B6890, true);
            TextView req = makeText(ctx, a.req, 10, 0xFF6B6890, false);
            info.addView(name); info.addView(req);
            TextView check = makeText(ctx, a.unlocked ? "✅" : "🔒", 14, 0xFFE8E4F5, false);
            row.addView(ico); row.addView(makeSpace(ctx, 4)); row.addView(info); row.addView(check);
            row.setAlpha(a.unlocked ? 1f : 0.38f);
            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(10), dp(2), dp(10), dp(2)); row.setLayoutParams(lp);
            outer.addView(row);
        }
        card.addView(outer);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10); card.setLayoutParams(lp);
        return card;
    }

    // ── Helper builders ───────────────────────────
    private View buildMiniStat(Context ctx, String val, String label, int color) {
        CardView c = makeCard(ctx, 8, 0xFF201E38, 0xFF2E2B50);
        LinearLayout inner = makeLl(ctx, LinearLayout.VERTICAL, 10, 6);
        inner.setGravity(Gravity.CENTER);
        TextView v = makeText(ctx, val, 18, color, true);
        v.setGravity(Gravity.CENTER);
        TextView l = makeText(ctx, label, 9, 0xFF6B6890, false);
        l.setGravity(Gravity.CENTER);
        inner.addView(v); inner.addView(l); c.addView(inner);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(dp(4), dp(0), dp(4), dp(0)); c.setLayoutParams(lp);
        return c;
    }

    private View buildAchSubHeader(Context ctx, String text) {
        TextView tv = makeText(ctx, text, 10, 0xFF6B6890, true);
        tv.setLetterSpacing(0.1f);
        tv.setAllCaps(true);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(14), dp(10), dp(14), dp(4)); tv.setLayoutParams(lp);
        return tv;
    }

    private View makeEmptyView(String ico, String msg) {
        Context ctx = requireContext();
        LinearLayout ll = makeLl(ctx, LinearLayout.VERTICAL, 40, 20);
        ll.setGravity(Gravity.CENTER);
        TextView i = makeText(ctx, ico, 36, 0xFF6B6890, false); i.setGravity(Gravity.CENTER);
        TextView m = makeText(ctx, msg, 13, 0xFF6B6890, false); m.setGravity(Gravity.CENTER);
        ll.addView(i); ll.addView(m);
        return ll;
    }

    private CardView makeCard(Context ctx, int radius, int bgColor, int strokeColor) {
        CardView card = new CardView(ctx);
        card.setRadius(dp(radius));
        card.setCardBackgroundColor(bgColor);
        card.setCardElevation(0);
        GradientDrawable fg = new GradientDrawable();
        fg.setStroke(dp(1), strokeColor);
        fg.setCornerRadius(dp(radius));
        card.setForeground(fg);
        ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(lp);
        return card;
    }

    private LinearLayout makeLl(Context ctx, int orientation, int padV, int padH) {
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(orientation);
        ll.setPadding(dp(padH), dp(padV), dp(padH), dp(padV));
        ll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return ll;
    }

    private TextView makeText(Context ctx, String text, int spSize, int color, boolean bold) {
        TextView tv = new TextView(ctx);
        tv.setText(text); tv.setTextSize(spSize); tv.setTextColor(color);
        if (bold) tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    private View makeSpace(Context ctx, int dp) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(dp), dp(dp)));
        return v;
    }

    private void setRoundBg(View v, int color, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dp(radius)); v.setBackground(bg);
    }

    private void animate(View v) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()).start();
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
