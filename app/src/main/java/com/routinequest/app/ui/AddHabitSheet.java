package com.routinequest.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.routinequest.app.R;
import com.routinequest.app.data.GameEngine;
import com.routinequest.app.data.Habit;
import com.routinequest.app.databinding.SheetAddHabitBinding;

import java.util.UUID;

public class AddHabitSheet extends BottomSheetDialogFragment {

    public interface OnAdded { void onHabitAdded(Habit h); }

    private SheetAddHabitBinding b;
    private String selectedStat = "str";
    private OnAdded listener;

    public void setOnAdded(OnAdded l) { this.listener = l; }

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        b = SheetAddHabitBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        View[] statBtns = {b.btnStr, b.btnInt, b.btnVit, b.btnWil, b.btnDex};

        for (int i = 0; i < statBtns.length; i++) {
            final int idx = i;
            final String key = GameEngine.STAT_KEYS[i];
            statBtns[i].setOnClickListener(x -> {
                selectedStat = key;
                for (int j = 0; j < statBtns.length; j++) updateStatBtn(statBtns[j], j, j == idx);
            });
        }
        updateStatBtn(b.btnStr, 0, true); // default selected

        b.etHabitName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable e) {
                b.btnAdd.setEnabled(e.length() > 0);
            }
        });
        b.btnAdd.setEnabled(false);

        b.btnAdd.setOnClickListener(x -> {
            String name = b.etHabitName.getText().toString().trim();
            if (name.isEmpty()) return;
            Habit h = new Habit(UUID.randomUUID().toString(), name, selectedStat);
            if (listener != null) listener.onHabitAdded(h);
            dismiss();
        });

        b.btnCancel.setOnClickListener(x -> dismiss());
    }

    private void updateStatBtn(View btn, int idx, boolean selected) {
        int color = GameEngine.STAT_COLORS[idx];
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(btn.getResources().getDisplayMetrics().density * 10);
        if (selected) {
            bg.setColor((color & 0x00FFFFFF) | 0x20000000);
            bg.setStroke((int)(btn.getResources().getDisplayMetrics().density * 2), color);
        } else {
            bg.setColor(0xFF1A1830);
            bg.setStroke((int)(btn.getResources().getDisplayMetrics().density), 0xFF2E2B50);
        }
        btn.setBackground(bg);
        if (btn instanceof android.widget.LinearLayout) {
            ((android.widget.LinearLayout) btn).setAlpha(selected ? 1f : 0.5f);
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
