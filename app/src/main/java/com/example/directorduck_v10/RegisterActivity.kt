package com.example.directorduck_v10

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.directorduck_v10.data.api.UserService
import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.RegisterRequest
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityRegisterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val request = RegisterRequest(
                username = binding.etUsername.text.toString(),
                phone = binding.etPhone.text.toString(),
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString()
            )

            ApiClient.userService.register(request).enqueue(object : Callback<ApiResponse<User>> {
                override fun onResponse(call: Call<ApiResponse<User>>, response: Response<ApiResponse<User>>) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
//                        Toast.makeText(this@RegisterActivity, "注册成功", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, body?.message ?: "注册失败", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })



        }

        binding.tvToLogin.setOnClickListener {
            finish()
        }


    }
}
