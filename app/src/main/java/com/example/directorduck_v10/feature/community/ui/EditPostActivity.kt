package com.example.directorduck_v10.feature.community.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.feature.community.model.Post
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityEditPostBinding
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class EditPostActivity : BaseActivity() {

    private lateinit var binding: ActivityEditPostBinding
    private var selectedImageUri: Uri? = null
    private lateinit var currentUser: User

    // 添加日志标签
    companion object {
        private const val TAG = "EditPostActivity"
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            Log.d(TAG, "Selected image URI: $selectedImageUri")
            binding.ivPreview.setImageURI(selectedImageUri)
            binding.ivPreview.visibility = android.view.View.VISIBLE

            // 隐藏按钮
            binding.btnSelectImage.visibility = android.view.View.GONE
        } else {
            Log.w(TAG, "Image selection cancelled or failed. Result code: ${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cancel.setOnClickListener{
            Log.d(TAG, "Cancel button clicked")
            finish()
        }

        // 从 MainActivity 获取当前登录用户
        currentUser = intent.getSerializableExtra("user") as User
        Log.d(TAG, "Current user loaded: ID=${currentUser.id}, Username=${currentUser.username}")

        binding.btnSelectImage.setOnClickListener {
            Log.d(TAG, "Select image button clicked")
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.btnPublish.setOnClickListener {
            Log.d(TAG, "Publish button clicked")
            val contentText = binding.etContent.text.toString().trim()
            Log.d(TAG, "Content text length: ${contentText.length}")
            if (contentText.isEmpty() && selectedImageUri == null) {
                Log.w(TAG, "Publish failed: Empty content and no image selected")
                Toast.makeText(this, "请输入文字或选择一张图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 准备请求体部分
            val contentPart = contentText.toRequestBody("text/plain".toMediaType())
            val publisherIdPart = currentUser.id.toString().toRequestBody("text/plain".toMediaType())
            val publisherUsernamePart = currentUser.username.toRequestBody("text/plain".toMediaType())
            Log.d(TAG, "Prepared text parts: PublisherId=${currentUser.id}, Username=${currentUser.username}")

            // 准备图片部分
            var imagePart: MultipartBody.Part? = null
            if (selectedImageUri != null) {
                try {
                    Log.d(TAG, "Processing selected image: $selectedImageUri")
                    val inputStream = contentResolver.openInputStream(selectedImageUri!!)
                    if (inputStream != null) {
                        val bytes = inputStream.readBytes()
                        Log.d(TAG, "Image read successfully, size: ${bytes.size} bytes")
                        val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData("image", "upload_${System.currentTimeMillis()}.jpg", requestFile)
                        Log.d(TAG, "Image part created successfully")
                    } else {
                        Log.e(TAG, "Failed to open input stream for image URI")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading image file", e)
                    Toast.makeText(this, "读取图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener // 如果图片读取失败，停止发布
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error reading image file", e)
                    Toast.makeText(this, "处理图片时发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                Log.d(TAG, "No image selected for upload")
            }

            // 开始网络请求
            Log.d(TAG, "Starting network request to create post...")
            Log.d(TAG, "API Endpoint: ${ApiClient.postService.javaClass.simpleName}") // 这可能不准确，但可以尝试

            ApiClient.postService.createPost(contentPart, publisherIdPart, publisherUsernamePart, imagePart).enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    Log.d(TAG, "Network request completed")
                    Log.d(TAG, "Response Code: ${response.code()}")
                    Log.d(TAG, "Response Message: ${response.message()}")
                    Log.d(TAG, "Is Successful: ${response.isSuccessful}")

                    // 尝试获取响应体（即使失败也记录）
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.d(TAG, "Error Body (if any): $errorBody")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not read error body", e)
                    }

                    if (response.isSuccessful) {
                        val post = response.body()
                        Log.d(TAG, "Post created successfully. Post ID: ${post?.id}")
                        Toast.makeText(this@EditPostActivity, "发布成功", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        // 服务器返回了错误状态码 (4xx, 5xx)
                        Log.e(TAG, "Server returned error status: ${response.code()} - ${response.message()}")
                        when(response.code()) {
                            400 -> Log.e(TAG, "Bad Request - Check request parameters/data format")
                            401 -> Log.e(TAG, "Unauthorized - Authentication required or failed")
                            403 -> Log.e(TAG, "Forbidden - Access denied")
                            404 -> Log.e(TAG, "Not Found - API endpoint might be incorrect")
                            500 -> Log.e(TAG, "Internal Server Error - Problem on the server side")
                            else -> Log.e(TAG, "Other HTTP error occurred")
                        }
                        Toast.makeText(this@EditPostActivity, "发布失败: ${response.code()} - ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Post>, t: Throwable) {
                    // 网络层面的失败（无网络、超时、DNS失败等）
                    Log.e(TAG, "Network request failed", t)
                    val errorMessage = when (t) {
                        is java.net.UnknownHostException -> "网络连接失败，请检查网络设置"
                        is java.net.SocketTimeoutException -> "请求超时，请稍后重试"
                        is java.net.ConnectException -> "无法连接到服务器，请检查服务器地址和网络"
                        else -> "网络错误: ${t.message}"
                    }
                    Toast.makeText(this@EditPostActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            })
        }

    }
}