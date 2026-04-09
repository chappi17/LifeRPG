package com.routinequest.app.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.routinequest.app.R;
import com.routinequest.app.data.DataManager;
import com.routinequest.app.data.DateUtils;
import com.routinequest.app.data.GameEngine;
import com.routinequest.app.data.Habit;
import com.routinequest.app.databinding.FragmentStatsBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StatsFragment extends Fragment implements MainActivity.Refreshable {

    private FragmentStatsBinding b;
    private DataManager dm;
    private boolean showWeekly = true;
    private int viewYear, viewMonth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        b = FragmentStatsBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        dm = DataManager.get(requireContext());
        Calendar now = Calendar.getInstance();
        viewYear = now.get(Calendar.YEAR);
        viewMonth = now.get(Calendar.MONTH);

        b.btnWeekly.setOnClickListener(x -> { showWeekly = true; updateTabUI(); renderContent(); });
        b.btnMonthly.setOnClickListener(x -> { showWeekly = false; updateTabUI(); renderContent(); });
        b.btnPrevMonth.setOnClickListener(x -> { shiftMonth(-1); renderContent(); });
        b.btnNextMonth.setOnClickListener(x -> { shiftMonth(1); renderContent(); });

        updateTabUI();
        renderContent();
    }

    private void updateTabUI() {
        b.btnWeekly.setSelected(showWeekly);
        b.btnMonthly.setSelected(!showWeekly);
        b.weeklyContent.setVisibility(showWeekly ? View.VISIBLE : View.GONE);
        b.monthlyContent.setVisibility(showWeekly ? View.GONE : View.VISIBLE);
    }

    private void shiftMonth(int dir) {
        viewMonth += dir;
        if (viewMonth > 11) { viewMonth = 0; viewYear++; }
        if (viewMonth < 0) { viewMonth = 11; viewYear--; }
        Calendar now = Calendar.getInstance();
        if (viewYear > now.get(Calendar.YEAR) ||
            (viewYear == now.get(Calendar.YEAR) && viewMonth > now.get(Calendar.MONTH))) {
            viewYear = now.get(Calendar.YEAR); viewMonth = now.get(Calendar.MONTH);
        }
        b.btnNextMonth.setEnabled(!(viewYear == now.get(Calendar.YEAR) && viewMonth == now.get(Calendar.MONTH)));
    }

    private void renderContent() {
        if (showWeekly) renderWeekly(); else renderMonthly();
    }

    // ── Weekly ────────────────────────────────────
    private void renderWeekly() {
        List<Habit> habits = dm.getHabits();
        String today = DateUtils.today();
        if (habits.isEmpty()) { b.weeklyEmpty.setVisibility(View.VISIBLE); b.weeklyChartArea.setVisibility(View.GONE); return; }
        b.weeklyEmpty.setVisibility(View.GONE); b.weeklyChartArea.setVisibility(View.VISIBLE);

        String[] last7 = DateUtils.getLast7Days();
        String[] dayLabels = DateUtils.getDayLabels(last7);

        // Summary cards
        int doneToday = 0; for (Habit h : habits) if (h.isDoneOn(today)) doneToday++;
        int maxStreak = 0; for (Habit h : habits) maxStreak = Math.max(maxStreak, h.getStreak(today));
        int avg = 0;
        for (String d : last7) { int cnt = 0; for (Habit h : habits) if (h.isDoneOn(d)) cnt++; avg += cnt; }
        avg = habits.size() > 0 ? avg * 100 / (7 * habits.size()) : 0;
        int todayXP = 0;
        for (Habit h : habits) if (h.isDoneOn(today)) todayXP += (int)(20 * GameEngine.streakMultiplier(h.getStreak(today)));

        b.tvWeekAvg.setText(avg + "%"); b.tvWeekStreak.setText(String.valueOf(maxStreak));
        b.tvWeekToday.setText(doneToday + "/" + habits.size()); b.tvWeekXp.setText(todayXP + " XP");

        // Bar chart
        ArrayList<BarEntry> entries = new ArrayList<>();
        int[] colors = new int[7];
        for (int i = 0; i < 7; i++) {
            int cnt = 0; for (Habit h : habits) if (h.isDoneOn(last7[i])) cnt++;
            float pct = habits.size() > 0 ? (cnt * 100f / habits.size()) : 0;
            entries.add(new BarEntry(i, pct));
            colors[i] = last7[i].equals(today) ? 0xFFF5C842 : 0x33E8E4F5;
        }
        BarDataSet ds = new BarDataSet(entries, "");
        ds.setColors(colors); ds.setDrawValues(false);
        styleBarChart(b.weeklyChart, ds, dayLabels);

        // Per-habit rates
        b.habitRatesContainer.removeAllViews();
        for (Habit h : habits) {
            int done = 0; for (String d : last7) if (h.isDoneOn(d)) done++;
            int pct = done * 100 / 7;
            int color = GameEngine.STAT_COLORS[GameEngine.statIndex(h.stat)];
            b.habitRatesContainer.addView(buildHabitRateRow(h.name, pct, color));
        }
    }

    // ── Monthly ───────────────────────────────────
    private void renderMonthly() {
        b.tvMonthTitle.setText(viewYear + "년 " + (viewMonth + 1) + "월");
        List<Habit> habits = dm.getHabits();
        String today = DateUtils.today();
        String[][] dates = DateUtils.getMonthDates(viewYear, viewMonth);
        List<String[]> validDays = new ArrayList<>();
        for (String[] d : dates) if (d[0].compareTo(today) <= 0) validDays.add(d);

        int totalDays = validDays.size();
        int perfectDays = 0, totalChecks = 0;
        int sumPct = 0;
        for (String[] d : validDays) {
            int cnt = 0;
            for (Habit h : habits) if (h.isDoneOn(d[0])) { cnt++; totalChecks++; }
            if (!habits.isEmpty() && cnt == habits.size()) perfectDays++;
            if (!habits.isEmpty()) sumPct += cnt * 100 / habits.size();
        }
        int avgPct = totalDays > 0 ? sumPct / totalDays : 0;

        b.tvMonthAvg.setText(avgPct + "%"); b.tvMonthPerfect.setText(String.valueOf(perfectDays));
        b.tvMonthDays.setText(String.valueOf(totalDays)); b.tvMonthChecks.setText(String.valueOf(totalChecks));

        // Heatmap calendar
        buildCalendar(dates, habits, today);

        // Bar chart (daily %)
        if (!validDays.isEmpty()) {
            ArrayList<BarEntry> entries = new ArrayList<>();
            int[] colors = new int[validDays.size()];
            String[] xlabels = new String[validDays.size()];
            for (int i = 0; i < validDays.size(); i++) {
                String d = validDays.get(i)[0];
                int cnt = 0; for (Habit h : habits) if (h.isDoneOn(d)) cnt++;
                float pct = habits.isEmpty() ? 0 : cnt * 100f / habits.size();
                entries.add(new BarEntry(i, pct));
                colors[i] = d.equals(today) ? 0xFFF5C842 : 0x33E8E4F5;
                Calendar cal = Calendar.getInstance(); cal.setTime(DateUtils.parseDate(d));
                xlabels[i] = cal.get(Calendar.DAY_OF_MONTH) % 5 == 1 ? cal.get(Calendar.DAY_OF_MONTH) + "일" : "";
            }
            BarDataSet ds = new BarDataSet(entries, "");
            ds.setColors(colors); ds.setDrawValues(false);
            styleBarChart(b.monthlyChart, ds, xlabels);
        }

        // Per-habit monthly rates
        b.monthHabitRates.removeAllViews();
        for (Habit h : habits) {
            int done = 0; for (String[] d : validDays) if (h.isDoneOn(d[0])) done++;
            int pct = totalDays > 0 ? done * 100 / totalDays : 0;
            int color = GameEngine.STAT_COLORS[GameEngine.statIndex(h.stat)];
            b.monthHabitRates.addView(buildHabitRateRow(h.name + " (" + done + "/" + totalDays + "일)", pct, color));
        }
    }

    private void buildCalendar(String[][] dates, List<Habit> habits, String today) {
        b.calendarGrid.removeAllViews();
        // Day labels
        String[] heads = {"일","월","화","수","목","금","토"};
        for (String h : heads) {
            TextView tv = new TextView(requireContext());
            tv.setText(h); tv.setTextSize(9); tv.setTextColor(0xFF6B6890);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setLayoutParams(makeCalCellLp());
            b.calendarGrid.addView(tv);
        }
        // Empty cells for first week
        int firstDow = DateUtils.getFirstDayOfWeek(viewYear, viewMonth);
        for (int i = 0; i < firstDow; i++) {
            View v = new View(requireContext()); v.setLayoutParams(makeCalCellLp());
            b.calendarGrid.addView(v);
        }
        // Day cells
        for (String[] d : dates) {
            int cnt = 0;
            for (Habit h : habits) if (h.isDoneOn(d[0])) cnt++;
            float pct = habits.isEmpty() ? 0 : cnt * 100f / habits.size();
            int bgColor = heatColor(pct, d[0].compareTo(today) > 0);
            TextView cell = new TextView(requireContext());
            Calendar cal = Calendar.getInstance(); cal.setTime(DateUtils.parseDate(d[0]));
            cell.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            cell.setTextSize(9); cell.setTextColor(pct > 66 ? Color.WHITE : 0xFF6B6890);
            cell.setGravity(android.view.Gravity.CENTER);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(bgColor); bg.setCornerRadius(dp(5));
            cell.setBackground(bg);
            if (d[0].equals(today)) cell.setBackground(makeTodayBg(bgColor));
            cell.setLayoutParams(makeCalCellLp());
            b.calendarGrid.addView(cell);
        }
    }

    private int heatColor(float pct, boolean future) {
        if (future) return 0x08FFFFFF;
        if (pct <= 0)  return 0x0FFFFFFF;
        if (pct <= 33) return 0x30F5C842;
        if (pct <= 66) return 0x60F5C842;
        if (pct < 100) return 0xA0F5C842;
        return 0xFFF5C842;
    }

    private android.graphics.drawable.GradientDrawable makeTodayBg(int bgColor) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor); bg.setCornerRadius(dp(5));
        bg.setStroke(dp(2), 0xFFF5C842);
        return bg;
    }

    private ViewGroup.LayoutParams makeCalCellLp() {
        android.widget.GridLayout.LayoutParams lp = new android.widget.GridLayout.LayoutParams();
        lp.width = 0; lp.height = dp(32);
        lp.columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, android.widget.GridLayout.FILL, 1f);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        return lp;
    }

    private View buildHabitRateRow(String name, int pct, int color) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(6), 0, dp(6));
        row.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        android.graphics.drawable.GradientDrawable dot = new android.graphics.drawable.GradientDrawable();
        dot.setShape(android.graphics.drawable.GradientDrawable.OVAL); dot.setColor(color);
        View dotV = new View(requireContext());
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(8), dp(8));
        dlp.gravity = android.view.Gravity.CENTER_VERTICAL; dotV.setLayoutParams(dlp); dotV.setBackground(dot);
        TextView nameTv = new TextView(requireContext());
        nameTv.setText(name); nameTv.setTextSize(12); nameTv.setTextColor(0xFFE8E4F5);
        nameTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        nameTv.setMaxLines(1); nameTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        android.widget.ProgressBar pb = new android.widget.ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100); pb.setProgress(pct);
        pb.setLayoutParams(new LinearLayout.LayoutParams(dp(80), dp(5)));
        TextView pctTv = new TextView(requireContext());
        pctTv.setText(pct + "%"); pctTv.setTextSize(11); pctTv.setTextColor(0xFFE8E4F5);
        pctTv.setPadding(dp(8), 0, 0, 0);
        row.addView(dotV); View sp = new View(requireContext());
        sp.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 0));
        row.addView(sp); row.addView(nameTv); row.addView(pb); row.addView(pctTv);
        return row;
    }

    private void styleBarChart(BarChart chart, BarDataSet ds, String[] labels) {
        BarData data = new BarData(ds); data.setBarWidth(0.6f);
        chart.setData(data); chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false); chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.getAxisLeft().setEnabled(false); chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0); chart.getAxisLeft().setAxisMaximum(100);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(0xFF6B6890); xAxis.setTextSize(10);
        xAxis.setGranularity(1);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.animateY(600); chart.invalidate();
    }

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    @Override public void onRefresh() { renderContent(); }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
