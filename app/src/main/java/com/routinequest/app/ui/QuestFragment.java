package com.routinequest.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.routinequest.app.R;
import com.routinequest.app.adapter.HabitAdapter;
import com.routinequest.app.data.DataManager;
import com.routinequest.app.data.DateUtils;
import com.routinequest.app.data.GameEngine;
import com.routinequest.app.data.Habit;
import com.routinequest.app.databinding.FragmentQuestBinding;

import java.util.List;

public class QuestFragment extends Fragment implements MainActivity.Refreshable {

    private FragmentQuestBinding b;
    private DataManager dm;
    private HabitAdapter adapter;
    private String activeDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup container, Bundle saved) {
        b = FragmentQuestBinding.inflate(inf, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        dm = DataManager.get(requireContext());
        activeDate = DateUtils.today();

        // RecyclerView
        adapter = new HabitAdapter(dm.getHabits(), activeDate, new HabitAdapter.Listener() {
            @Override public void onToggle(Habit h) {
                int prevLevel = GameEngine.calcLevel(GameEngine.calcTotalXP(dm.getHabits()));
                dm.toggle(h.id, activeDate);
                int newLevel = GameEngine.calcLevel(GameEngine.calcTotalXP(dm.getHabits()));
                if (newLevel > prevLevel)
                    Toast.makeText(getContext(), "🎉 레벨 업! Lv." + newLevel + " 달성!", Toast.LENGTH_SHORT).show();
                refresh();
            }
            @Override public void onDelete(Habit h) {
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("퀘스트 삭제")
                    .setMessage("'" + h.name + "' 을 삭제할까요?")
                    .setPositiveButton("삭제", (d,w) -> { dm.removeHabit(h.id); refresh(); })
                    .setNegativeButton("취소", null).show();
            }
        });
        b.recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        b.recycler.setAdapter(adapter);
        b.recycler.setItemAnimator(new androidx.recyclerview.widget.DefaultItemAnimator());

        // Date navigator
        b.btnPrevDate.setOnClickListener(v -> shiftDate(-1));
        b.btnNextDate.setOnClickListener(v -> shiftDate(1));

        // Add habit
        b.btnAddHabit.setOnClickListener(v -> {
            AddHabitSheet sheet = new AddHabitSheet();
            sheet.setOnAdded(habit -> { dm.addHabit(habit); refresh(); });
            sheet.show(getChildFragmentManager(), "add_habit");
        });

        refresh();
    }

    private void shiftDate(int dir) {
        String next = DateUtils.shift(activeDate, dir);
        if (DateUtils.isFuture(next)) return;
        activeDate = next;
        animateDateShift(dir > 0);
        refresh();
    }

    private void animateDateShift(boolean forward) {
        float startX = forward ? 200f : -200f;
        b.dateContent.setTranslationX(startX);
        b.dateContent.setAlpha(0f);
        b.dateContent.animate().translationX(0).alpha(1f).setDuration(220)
            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }

    private void refresh() {
        if (b == null) return;
        String today = DateUtils.today();
        boolean isToday = activeDate.equals(today);

        // Header title
        b.tvTitle.setText(isToday ? "⚔️ 오늘의 퀘스트" : "📜 과거 기록 수정");

        // Date navigator
        b.tvDateLabel.setText(isToday ? "오늘" : DateUtils.formatKorean(activeDate));
        if (!isToday) {
            int diff = DateUtils.daysBetween(activeDate, today);
            b.tvDateSub.setText(diff + "일 전");
            b.tvDateSub.setVisibility(View.VISIBLE);
        } else {
            b.tvDateSub.setVisibility(View.GONE);
        }
        b.btnNextDate.setEnabled(!isToday);
        b.btnNextDate.setAlpha(isToday ? 0.3f : 1f);

        // Add button visibility
        b.btnAddHabit.setVisibility(isToday ? View.VISIBLE : View.GONE);

        // XP preview
        List<Habit> habits = dm.getHabits();
        int doneXP = 0, totalXP = 0;
        for (Habit h : habits) {
            int xp = (int)(20 * GameEngine.streakMultiplier(h.getStreak(today)));
            totalXP += xp;
            if (h.isDoneOn(activeDate)) doneXP += xp;
        }
        if (isToday) {
            b.tvXpLabel.setText("오늘 획득 가능 경험치");
            b.tvXpVal.setText(doneXP + " / " + totalXP + " XP");
        } else {
            b.tvXpLabel.setText("이 날 획득한 경험치");
            b.tvXpVal.setText(doneXP + " XP");
        }

        // Empty state
        if (habits.isEmpty()) {
            b.tvEmpty.setVisibility(View.VISIBLE);
            b.recycler.setVisibility(View.GONE);
        } else {
            b.tvEmpty.setVisibility(View.GONE);
            b.recycler.setVisibility(View.VISIBLE);
            adapter.update(habits, activeDate);
        }
    }

    @Override public void onRefresh() { activeDate = DateUtils.today(); refresh(); }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
