package net.matcix.totp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import net.matcix.totp.R;
import net.matcix.totp.databinding.FragmentAddBinding;
import net.matcix.totp.model.TOTPArray;
import java.net.URI;
import java.net.URISyntaxException;

public class AddFragment extends Fragment {
    private FragmentAddBinding binding;
    private TOTPArray totpArray;
    
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startScan();
                } else {
                    showError("需要相机权限才能扫描二维码");
                }
            });

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = 
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    showError("扫描取消");
                } else {
                    processQRCode(result.getContents());
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        totpArray = TOTPArray.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);
        
        // 设置扫码卡片点击事件
        binding.scanCard.setOnClickListener(v -> {
            checkCameraPermission();
        });

        // 设置手动输入卡片点击事件
        binding.manualCard.setOnClickListener(v -> {
            showInputDialog("手动输入", "请输入密钥");
        });

        return binding.getRoot();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScan() {
        ScanOptions options = new ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("将二维码放入框内扫描")
                .setCameraId(0)
                .setBeepEnabled(false)
                .setBarcodeImageEnabled(true)
                .setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    private void processQRCode(String contents) {
        try {
            URI uri = new URI(contents);
            if (!"otpauth".equals(uri.getScheme())) {
                showError("无效的二维码格式");
                return;
            }

            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }

            String query = uri.getQuery();
            String secret = null;
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "secret".equals(keyValue[0])) {
                        secret = keyValue[1];
                        break;
                    }
                }
            }

            if (secret != null) {
                if (totpArray.addEntry(path, secret)) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    showError("密钥格式不正确");
                }
            } else {
                showError("二维码中未找到密钥");
            }
        } catch (URISyntaxException e) {
            showError("无效的二维码格式");
        }
    }

    private void showInputDialog(String title, String hint) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_totp, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        TextInputEditText secretInput = dialogView.findViewById(R.id.secretInput);
        
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = nameInput.getText().toString();
                    String secret = secretInput.getText().toString();
                    
                    if (name.isEmpty() || secret.isEmpty()) {
                        showError("名称和密钥不能为空");
                        return;
                    }
                    
                    if (totpArray.addEntry(name, secret)) {
                        // 添加成功，返回列表页面
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        showError("密钥格式不正确");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showError(String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("错误")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 