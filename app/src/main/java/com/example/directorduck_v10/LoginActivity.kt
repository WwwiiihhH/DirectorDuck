package com.example.directorduck_v10

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.directorduck_v10.data.api.UserService
import com.example.directorduck_v10.data.model.ApiResponse
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log


class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val phone = binding.etPhone.text.toString()
            val password = binding.etPassword.text.toString()

            ApiClient.userService.login(phone, password)
                .enqueue(object : Callback<ApiResponse<User>> {
                    override fun onResponse(
                        call: Call<ApiResponse<User>>,
                        response: Response<ApiResponse<User>>
                    ) {
                        val body = response.body()
                        if (body != null && body.code == 200 && body.data != null) {
//                            Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT)
//                                .show()

                            val user = body.data
                            Log.d("LoginActivity", "登录成功，用户信息: id=${user.id}, username=${user.username}, phone=${user.phone}, email=${user.email}")

                            // 这里拿到用户信息，跳转传递
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("user", body.data)
                            startActivity(intent)



                            finish()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                body?.message ?: "登录失败",
                                Toast.LENGTH_SHORT
                            ).show()

                            Log.d("aaa","登录失败+ ${body?.message}")
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<User>>, t: Throwable) {
                        Toast.makeText(
                            this@LoginActivity,
                            "网络错误: ${t.message}",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d("aaa","网络错误: ${t.message}")
                    }
                })





        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
