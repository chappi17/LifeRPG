package com.routinequest.app.adapter;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.routinequest.app.R;
import com.routinequest.app.data.DateUtils;
import com.routinequest.app.data.GameEngine;
import com.routinequest.app.data.Habit;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.VH> {

    public interface Listener {
        void onToggle(Habit h);
        void onDelete(Habit h);
    }

    private List<Habit> habits;
    private String activeDate;
    private final Listener listener;

    public HabitAdapter(List<Habit> habits, String activeDate, Listener listener) {
        this.habits = habits;
        this.activeDate = activeDate;
        this.listener = listener;
    }

    public void update(List<Habit> habits, String activeDate) {
        this.habits = habits;
        this.activeDate = activeDate;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_habit, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Habit habit = habits.get(pos);
        boolean done = habit.isDoneOn(activeDate);
        boolean isToday = activeDate.equals(DateUtils.today());
        int streak = habit.getStreak(DateUtils.today());
        int statIdx = GameEngine.statIndex(habit.stat);
        int color = GameEngine.STAT_COLORS[statIdx];
        double mult = GameEngine.streakMultiplier(streak);
        int xp = (int)(20 * mult);

        h.tvName.setText(habit.name);
        h.tvName.setTextColor(done ? 0xFF6B6890 : 0xFFE8E4F5);
        if (done) h.tvName.setPaintFlags(h.tvName.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        else h.tvName.setPaintFlags(h.tvName.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

        // Stat badge
        h.tvStat.setText(GameEngine.STAT_ICONS[statIdx] + " " + GameEngine.STAT_LABELS[statIdx]);
        setStatBadgeColor(h.tvStat, color);

        // XP label
        String xpStr = "+" + xp + " XP";
        if (streak >= 30) xpStr += " 🔥";
        else if (streak >= 7) xpStr += " ⚡";
        h.tvXp.setText(xpStr);
        h.tvXp.setTextColor(0xFFF5C842);

        // Streak
        if (isToday && streak > 0) {
            h.tvStreak.setVisibility(View.VISIBLE);
            h.tvStreak.setText("🔥 " + streak + "일");
        } else {
            h.tvStreak.setVisibility(View.GONE);
        }

        // Checkbox styling
        bindCheckbox(h.vCheck, h.tvCheck, done, color);

        // Card background
        h.card.setCardBackgroundColor(done ? 0xFF201E38 : 0xFF1A1830);

        h.card.setOnClickListener(v -> {
            animateCheck(h.vCheck, h.tvCheck, !done, color);
            listener.onToggle(habit);
        });
        h.btnDelete.setOnClickListener(v -> listener.onDelete(habit));
    }

    private void bindCheckbox(View vCheck, TextView tvCheck, boolean done, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(18);
        if (done) {
            bg.setColor(color);
            tvCheck.setText("✓");
            tvCheck.setTextColor(Color.WHITE);
        } else {
            bg.setColor(0x00000000);
            bg.setStroke(3, (color & 0x00FFFFFF) | 0x66000000);
            tvCheck.setText("");
        }
        vCheck.setBackground(bg);
    }

    private void animateCheck(View vCheck, TextView tvCheck, boolean newDone, int color) {
        vCheck.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100)
            .withEndAction(() ->
                vCheck.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> vCheck.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                    .start()
            ).start();
    }

    @Override public int getItemCount() { return habits.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CardView card;
        View vCheck;
        TextView tvCheck, tvName, tvStat, tvXp, tvStreak;
        ImageView btnDelete;

        VH(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            vCheck = v.findViewById(R.id.v_check);
            tvCheck = v.findViewById(R.id.tv_check);
            tvName = v.findViewById(R.id.tv_name);
            tvStat = v.findViewById(R.id.tv_stat);
            tvXp = v.findViewById(R.id.tv_xp);
            tvStreak = v.findViewById(R.id.tv_streak);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }

    private void setStatBadgeColor(TextView tv, int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12);
        bg.setColor((color & 0x00FFFFFF) | 0x22000000);
        bg.setStroke(1, color);
        tv.setBackground(bg);
        tv.setTextColor(color);
    }
}
