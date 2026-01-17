package com.example.directorduck_v10.feature.favorite.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.example.directorduck_v10.data.model.User
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.databinding.ActivityFavoriteCollectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.example.directorduck_v10.feature.favorite.adapter.FavoriteQuestionPagerAdapter
import com.example.directorduck_v10.core.base.BaseActivity
import com.example.directorduck_v10.feature.favorite.model.FavoriteQuestionDetailDTO

class FavoriteCollectionActivity : BaseActivity() {

    private lateinit var binding: ActivityFavoriteCollectionBinding
    private lateinit var currentUser: User

    private val favorites = mutableListOf<FavoriteQuestionDetailDTO>()
    private lateinit var adapter: FavoriteQuestionPagerAdapter

    // 记录“用户在收藏集做题”的作答状态：uuid -> selectedOption(A/B/C/D)
    private val answeredOptionByUuid = mutableMapOf<String, String>()

    // 分页
    private var page = 1
    private val size = 20
    private var total: Long = 0L
    private var loading = false

    private var removing = false


    companion object {
        fun start(context: Context, user: User) {
            val intent = Intent(context, FavoriteCollectionActivity::class.java).apply {
                putExtra("user", user)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUser = intent.getSerializableExtra("user") as User

        binding.back.setOnClickListener { finish() }

        binding.ivBookmark.setOnClickListener {
            removeCurrentFavorite()
        }


        setupPager()
        loadNextPage() // 先加载第一页
    }

    private fun setupPager() {
        adapter = FavoriteQuestionPagerAdapter(
            items = favorites,
            getSelectedOption = { uuid -> answeredOptionByUuid[uuid] },
            onAnswered = { uuid, selected ->
                answeredOptionByUuid[uuid] = selected
            }
        )

        binding.viewPagerFavorites.adapter = adapter

        binding.viewPagerFavorites.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateProgress(position)

                // 临近末尾，自动加载下一页
                val needLoadMore = (position >= favorites.size - 3)
                val hasMore = favorites.size.toLong() < total
                if (needLoadMore && hasMore) {
                    loadNextPage()
                }
            }
        })
    }

    private fun updateProgress(position: Int) {
        val cur = if (favorites.isEmpty()) 0 else position + 1
        val tot = if (total > 0) total else favorites.size.toLong()
        binding.tvProgress.text = "$cur/$tot"
    }

    private fun loadNextPage() {
        if (loading) return
        loading = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = ApiClient.favoriteService.listFavorites(
                    userId = currentUser.id,
                    page = page,
                    size = size
                )

                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body?.code == 200 && body.data != null) {
                        val dto = body.data
                        val newList = dto.list ?: emptyList()

                        withContext(Dispatchers.Main) {
                            total = dto.total
                            val start = favorites.size
                            favorites.addAll(newList)
                            adapter.notifyItemRangeInserted(start, newList.size)

                            if (favorites.isEmpty()) {
                                Toast.makeText(this@FavoriteCollectionActivity, "暂无收藏题目", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                updateProgress(binding.viewPagerFavorites.currentItem)
                            }

                            page += 1
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FavoriteCollectionActivity, body?.message ?: "加载失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@FavoriteCollectionActivity, "服务器错误: ${resp.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FavoriteCollectionActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                loading = false
            }
        }
    }

    private fun removeCurrentFavorite() {
        if (removing) return
        if (favorites.isEmpty()) return

        val curPos = binding.viewPagerFavorites.currentItem
        val cur = favorites.getOrNull(curPos) ?: return
        val uuid = cur.uuid
        if (uuid.isNullOrBlank()) return

        removing = true
        binding.ivBookmark.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ✅ 调后端取消收藏（你项目里 QuizActivity 已经在用 removeFavorite(uuid, userId)）
                val resp = ApiClient.favoriteService.removeFavorite(uuid, currentUser.id)

                val ok = resp.isSuccessful && resp.body()?.code == 200
                withContext(Dispatchers.Main) {
                    if (ok) {
                        // 1) 本地缓存清理
                        answeredOptionByUuid.remove(uuid)

                        // 2) 从数据源移除
                        favorites.removeAt(curPos)
                        adapter.notifyItemRemoved(curPos)
                        adapter.notifyItemRangeChanged(curPos, favorites.size - curPos)

                        // 3) total 本地减一（让顶部进度更合理）
                        if (total > 0) total -= 1

                        // 4) 处理 ViewPager2 当前页索引
                        if (favorites.isEmpty()) {
                            Toast.makeText(this@FavoriteCollectionActivity, "收藏集已空", Toast.LENGTH_SHORT).show()
                            finish()
                            return@withContext
                        }

                        // 删除的是当前页，删除后“原curPos位置”会变成下一题；如果删的是最后一题就回到上一题
                        val newPos = if (curPos >= favorites.size) favorites.size - 1 else curPos
                        binding.viewPagerFavorites.setCurrentItem(newPos, false)

                        updateProgress(newPos)

                        Toast.makeText(this@FavoriteCollectionActivity, "已取消收藏", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this@FavoriteCollectionActivity,
                            resp.body()?.message ?: "取消收藏失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FavoriteCollectionActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    removing = false
                    binding.ivBookmark.isEnabled = true
                }
            }
        }
    }

}
