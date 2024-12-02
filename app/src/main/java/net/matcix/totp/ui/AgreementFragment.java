package net.matcix.totp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import net.matcix.totp.Constants;
import net.matcix.totp.R;
import net.matcix.totp.databinding.FragmentAgreementBinding;

public class AgreementFragment extends Fragment {
    private FragmentAgreementBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAgreementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.agreeButton.setOnClickListener(v -> {
            // 保存用户同意状态
            if (getContext() != null) {
                SharedPreferences prefs = getContext().getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
                prefs.edit().putBoolean(Constants.KEY_AGREEMENT_ACCEPTED, true).apply();
            }
            
            // 导航到列表页面
            Navigation.findNavController(v).navigate(R.id.action_agreement_to_list);
        });

        binding.disagreeButton.setOnClickListener(v -> {
            showExitConfirmationDialog();
        });
    }

    private void showExitConfirmationDialog() {
        Context context = getContext();
        if (context == null) return;

        new MaterialAlertDialogBuilder(context)
            .setTitle("提示")
            .setMessage("如果不同意用户协议，将无法使用本应用。确定要退出吗？")
            .setPositiveButton("确定退出", (dialog, which) -> {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.finish();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 