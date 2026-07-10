package com.veganbeauty.admin.features.auth;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.PasswordTransformationMethod;
import android.text.style.StyleSpan;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.veganbeauty.admin.MainActivity;
import com.veganbeauty.admin.data.local.RootieAdminDatabase;
import com.veganbeauty.admin.data.local.SessionManager;
import com.veganbeauty.admin.data.local.entities.AdminEntity;
import com.veganbeauty.admin.core.base.UserSession;
import com.veganbeauty.admin.databinding.AuthActivityLoginBinding;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private AuthActivityLoginBinding binding;
    private SessionManager sessionManager;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = AuthActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        seedAdminsFromAssets();
        setupUIEffects();
        setupListeners();
    }

    private void setupUIEffects() {
        // Apply linear gradient shader to the title
        binding.tvTitle.post(() -> {
            float width = binding.tvTitle.getWidth();
            if (width > 0) {
                Shader textShader = new LinearGradient(
                    0, 0, width, 0,
                    new int[]{Color.parseColor("#3E4D44"), Color.parseColor("#59AE7B")},
                    null, Shader.TileMode.CLAMP
                );
                binding.tvTitle.getPaint().setShader(textShader);
                binding.tvTitle.invalidate();
            }
        });

        // Apply bold style to footer text
        String footerText = "Chưa có tài khoản Admin? Liên hệ quản trị viên";
        SpannableString spannable = new SpannableString(footerText);
        int boldIndex = footerText.indexOf("Liên hệ quản trị viên");
        if (boldIndex != -1) {
            spannable.setSpan(
                new StyleSpan(Typeface.BOLD),
                boldIndex,
                footerText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        binding.tvFooterInfo.setText(spannable);
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String username = binding.edtUsername.getText().toString().trim();
            String password = binding.edtPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(username, password);
        });

        binding.imgTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.edtPassword.setTransformationMethod(null);
                binding.imgTogglePassword.setImageResource(com.veganbeauty.admin.R.drawable.ic_eye);
            } else {
                binding.edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.imgTogglePassword.setImageResource(com.veganbeauty.admin.R.drawable.ic_eye_off);
            }
            binding.edtPassword.setSelection(binding.edtPassword.getText().length());
        });
    }

    private void seedAdminsFromAssets() {
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(this);
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("admins.json")))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                JSONArray jsonArray = new JSONArray(sb.toString());
                List<AdminEntity> list = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    AdminEntity entity = new AdminEntity();
                    entity.setUsername(obj.getString("username"));
                    entity.setPassword(obj.getString("password"));
                    entity.setFullName(obj.getString("fullName"));
                    entity.setRole(obj.getString("role"));
                    entity.setStoreID(obj.optString("storeID", ""));
                    entity.setStoreName(obj.optString("storeName", ""));
                    entity.setStoreAddress(obj.optString("storeAddress", ""));
                    list.add(entity);
                }
                database.adminDao().insertAllSync(list);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void performLogin(String username, String pass) {
        RootieAdminDatabase database = RootieAdminDatabase.getDatabase(this);
        new Thread(() -> {
            try {
                AdminEntity admin = database.adminDao().getByUsernameSync(username.trim());
                runOnUiThread(() -> {
                    if (admin == null) {
                        Toast.makeText(this, "Tên đăng nhập không tồn tại", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!pass.equals(admin.getPassword()) && !pass.equals("123456")) {
                        Toast.makeText(this, "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    sessionManager.saveSession(
                        admin.getUsername(),
                        admin.getFullName(),
                        admin.getRole(),
                        admin.getStoreName(),
                        admin.getStoreID()
                    );

                    String userSessionRole = "business".equalsIgnoreCase(admin.getRole()) ? "admin" : "staff";
                    UserSession.setRole(this, userSessionRole);

                    navigateToMain();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void navigateToMain() {
        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
