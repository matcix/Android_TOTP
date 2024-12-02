package net.matcix.totp.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.matcix.totp.databinding.FragmentListBinding;
import net.matcix.totp.model.TOTPArray;
import net.matcix.totp.ui.adapter.TOTPAdapter;

public class ListFragment extends Fragment implements TOTPAdapter.OnItemClickListener {
    private FragmentListBinding binding;
    private TOTPAdapter adapter;
    private Handler handler;
    private static final int UPDATE_INTERVAL = 100; // 100ms更新一次进度
    private static final int TOTP_PERIOD = 30000; // 30秒更新一次TOTP码

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        
        // 设置RecyclerView
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TOTPAdapter(TOTPArray.getInstance().getEntries());
        adapter.setOnItemClickListener(this);
        binding.recyclerView.setAdapter(adapter);

        // 初始化定时器
        handler = new Handler(Looper.getMainLooper());
        startUpdateTimer();

        return binding.getRoot();
    }

    @Override
    public void onItemClick(int position, TOTPArray.TOTPEntry entry) {
        new MaterialAlertDialogBuilder(requireContext())
                .setItems(new String[]{"复制密钥", "分享", "删除"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // 复制密钥
                            copyToClipboard(entry.getSecret());
                            break;
                        case 1: // 分享
                            shareEntry(entry);
                            break;
                        case 2: // 删除
                            showDeleteConfirmDialog(position);
                            break;
                    }
                })
                .show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("TOTP Secret", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), "密钥已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmDialog(int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除这个TOTP条目吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    TOTPArray.getInstance().removeEntry(position);
                    adapter.notifyItemRemoved(position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void shareEntry(TOTPArray.TOTPEntry entry) {
        String shareText = String.format("otpauth://totp/%s?secret=%s", 
                entry.getName(), entry.getSecret());
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        // 创建分享对话框
        Intent chooser = Intent.createChooser(shareIntent, "分享 TOTP");
        
        // 检查是否有应用可以处理分享
        if (shareIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(requireContext(), "没有找到可以分享的应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void startUpdateTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                long currentTimeMillis = System.currentTimeMillis();
                int progress = (int)((currentTimeMillis % TOTP_PERIOD) * 100 / TOTP_PERIOD);
                adapter.updateProgress(progress);
                
                // 如果进度接近100%，更新TOTP码
                if (progress > 98) {
                    adapter.updateCodes();
                }
                
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }
} 