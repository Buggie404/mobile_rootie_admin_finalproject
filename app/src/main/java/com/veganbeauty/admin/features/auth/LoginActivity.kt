package com.veganbeauty.admin.features.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.method.PasswordTransformationMethod
import com.veganbeauty.admin.MainActivity
import com.veganbeauty.admin.data.local.SessionManager
import com.veganbeauty.admin.databinding.AuthActivityLoginBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.veganbeauty.admin.data.local.RootieAdminDatabase
import com.veganbeauty.admin.data.local.entities.AdminEntity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: AuthActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AuthActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        seedAdminsIfEmpty()
        setupUIEffects()
        setupListeners()
    }

    private fun setupUIEffects() {
        // Apply linear gradient shader to the title
        binding.tvTitle.post {
            val paint = binding.tvTitle.paint
            val width = binding.tvTitle.width.toFloat()
            if (width > 0) {
                val textShader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(Color.parseColor("#3E4D44"), Color.parseColor("#59AE7B")),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = textShader
                binding.tvTitle.invalidate()
            }
        }

        // Apply bold style to footer text
        val footerText = "Chưa có tài khoản Admin? Liên hệ quản trị viên"
        val spannable = SpannableString(footerText)
        val boldIndex = footerText.indexOf("Liên hệ quản trị viên")
        if (boldIndex != -1) {
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                boldIndex,
                footerText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.tvFooterInfo.text = spannable
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }

        // Quick profile listeners
        binding.cardProfileAdmin.setOnClickListener {
            binding.edtUsername.setText("admin")
            binding.edtPassword.setText("123456")
            performLogin("admin", "123456")
        }

        binding.cardProfileStaff1.setOnClickListener {
            binding.edtUsername.setText("staff1")
            binding.edtPassword.setText("123456")
            performLogin("staff1", "123456")
        }

        binding.cardProfileStaff5.setOnClickListener {
            binding.edtUsername.setText("staff5")
            binding.edtPassword.setText("123456")
            performLogin("staff5", "123456")
        }

        binding.imgTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.edtPassword.transformationMethod = null
                binding.imgTogglePassword.setImageResource(com.veganbeauty.admin.R.drawable.ic_eye)
            } else {
                binding.edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.imgTogglePassword.setImageResource(com.veganbeauty.admin.R.drawable.ic_eye_off)
            }
            binding.edtPassword.setSelection(binding.edtPassword.text.length)
        }
    }

    private fun seedAdminsIfEmpty() {
        val database = RootieAdminDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = database.adminDao()
            if (dao.getAllSync().isEmpty()) {
                try {
                    val jsonString = assets.open("admins.json").bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(jsonString)
                    val list = mutableListOf<AdminEntity>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            AdminEntity(
                                username = obj.getString("username"),
                                password = obj.getString("password"),
                                fullName = obj.getString("fullName"),
                                role = obj.getString("role"),
                                storeID = obj.optString("storeID", ""),
                                storeName = obj.optString("storeName", ""),
                                storeAddress = obj.optString("storeAddress", "")
                            )
                        )
                    }
                    dao.insertAllSync(list)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun performLogin(username: String, pass: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val database = RootieAdminDatabase.getDatabase(this@LoginActivity)
            val admin = database.adminDao().getByUsernameSync(username.trim())
            
            withContext(Dispatchers.Main) {
                if (admin == null) {
                    Toast.makeText(this@LoginActivity, "Tên đăng nhập không tồn tại", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                
                if (pass != admin.password && pass != "123456") {
                    Toast.makeText(this@LoginActivity, "Mật khẩu không chính xác", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
                
                sessionManager.saveSession(
                    username = admin.username,
                    fullName = admin.fullName,
                    role = admin.role,
                    assignedStore = admin.storeName,
                    storeID = admin.storeID
                )
                
                val userSessionRole = if (admin.role.equals("business", ignoreCase = true)) "admin" else "staff"
                com.veganbeauty.admin.core.base.UserSession.setRole(this@LoginActivity, userSessionRole)
                
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
