package com.routinequest.app.ui;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.routinequest.app.R;
import com.routinequest.app.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private int currentTabIndex = 0;

    private final Fragment questFragment    = new QuestFragment();
    private final Fragment characterFragment = new CharacterFragment();
    private final Fragment statsFragment    = new StatsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load initial fragment
        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, questFragment, "quest")
            .add(R.id.fragment_container, characterFragment, "character")
            .add(R.id.fragment_container, statsFragment, "stats")
            .hide(characterFragment)
            .hide(statsFragment)
            .commit();

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_quest)     switchTab(0);
            else if (id == R.id.nav_char) switchTab(1);
            else if (id == R.id.nav_stats)switchTab(2);
            return true;
        });

        // Gear button → settings sheet
        binding.btnGear.setOnClickListener(v -> {
            v.animate().rotation(v.getRotation() + 90).setDuration(300).start();
            new SettingsSheet().show(getSupportFragmentManager(), "settings");
        });
    }

    private void switchTab(int targetIndex) {
        if (targetIndex == currentTabIndex) return;

        Fragment from = getFragmentAt(currentTabIndex);
        Fragment to   = getFragmentAt(targetIndex);

        boolean goingRight = targetIndex > currentTabIndex;
        int enterAnim  = goingRight ? R.anim.slide_in_right  : R.anim.slide_in_left;
        int exitAnim   = goingRight ? R.anim.slide_out_left  : R.anim.slide_out_right;

        getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(enterAnim, exitAnim)
            .hide(from)
            .show(to)
            .commit();

        currentTabIndex = targetIndex;

        // Notify fragments to refresh
        if (to instanceof Refreshable) ((Refreshable) to).onRefresh();
    }

    private Fragment getFragmentAt(int index) {
        switch (index) {
            case 1: return characterFragment;
            case 2: return statsFragment;
            default: return questFragment;
        }
    }

    public interface Refreshable { void onRefresh(); }
}
