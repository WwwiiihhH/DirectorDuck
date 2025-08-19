package com.example.directorduck_v10

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.directorduck_v10.data.model.CreatePostRequest
import com.example.directorduck_v10.data.model.Post
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.data.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityEditPostBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditPostActivity : BaseActivity() {

    private lateinit var binding: ActivityEditPostBinding
    private var selectedImageUri: Uri? = null
    private lateinit var currentUser: User

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            binding.ivPreview.setImageURI(selectedImageUri)
            binding.ivPreview.visibility = android.view.View.VISIBLE

            // 隐藏按钮
            binding.btnSelectImage.visibility = android.view.View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("EditPostActivity", "onCreate 调用")
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cancel.setOnClickListener{
            finish()
        }


        // 从 MainActivity 获取当前登录用户
        currentUser = intent.getSerializableExtra("user") as User

        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.btnPublish.setOnClickListener {
            val contentText = binding.etContent.text.toString().trim()
            if (contentText.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, "请输入文字或选择一张图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contentPart = contentText.toRequestBody("text/plain".toMediaType())
            val publisherIdPart = currentUser.id.toString().toRequestBody("text/plain".toMediaType())
            val publisherUsernamePart = currentUser.username.toRequestBody("text/plain".toMediaType())

            val imagePart = selectedImageUri?.let { uri ->
                val inputStream = contentResolver.openInputStream(uri)!!
                val bytes = inputStream.readBytes()
                val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", "upload.jpg", requestFile)
            }

            ApiClient.postService.createPost(contentPart,publisherIdPart,publisherUsernamePart,imagePart).enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditPostActivity, "发布成功", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@EditPostActivity, "发布失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    Toast.makeText(this@EditPostActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

    }
}