package com.routinequest.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.routinequest.app.data.DataManager;
import com.routinequest.app.data.GameEngine;
import com.routinequest.app.databinding.SheetSettingsBinding;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SettingsSheet extends BottomSheetDialogFragment {

    private SheetSettingsBinding b;
    private DataManager dm;

    private final ActivityResultLauncher<String[]> importLauncher =
        registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) return;
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                is.close();
                byte[] bytes = baos.toByteArray();
                String json = new String(bytes, StandardCharsets.UTF_8);
                boolean ok = dm.importJson(json);
                Toast.makeText(getContext(), ok ? "🎉 복원 완료!" : "❌ 올바른 백업 파일이 아니에요", Toast.LENGTH_SHORT).show();
                if (ok) dismiss();
            } catch (Exception e) {
                Toast.makeText(getContext(), "❌ 파일을 읽을 수 없어요", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        b = SheetSettingsBinding.inflate(inf, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        dm = DataManager.get(requireContext());
        GameEngine.Summary summ = GameEngine.calcSummary(dm.getHabits());
        b.tvDataInfo.setText("습관 " + dm.getHabits().size() + "개 · 총 " + summ.totalCompletions + "회 기록");

        b.btnExport.setOnClickListener(x -> exportData());
        b.btnImport.setOnClickListener(x -> importLauncher.launch(new String[]{"application/json", "*/*"}));
        b.btnReset.setOnClickListener(x -> confirmReset());
        b.btnClose.setOnClickListener(x -> dismiss());
    }

    private void exportData() {
        String json = dm.exportJson();
        String date = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new java.util.Date());
        try {
            java.io.File file = new java.io.File(requireContext().getCacheDir(), "루틴퀘스트_백업_" + date + ".json");
            java.io.FileWriter fw = new java.io.FileWriter(file);
            fw.write(json); fw.close();
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(), requireContext().getPackageName() + ".provider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/json");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "백업 파일 저장"));
            Toast.makeText(getContext(), "✅ 백업 파일을 내보냈어요!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "❌ 내보내기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmReset() {
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ 모든 데이터 초기화")
            .setMessage("습관, 기록, 경험치가 모두 삭제됩니다.\n정말 초기화하시겠어요?")
            .setPositiveButton("초기화", (d, w) -> {
                dm.resetAll();
                Toast.makeText(getContext(), "🗑 초기화 완료", Toast.LENGTH_SHORT).show();
                dismiss();
            })
            .setNegativeButton("취소", null).show();
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
