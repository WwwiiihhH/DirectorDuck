package com.example.directorduck_v10.feature.mine.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.directorduck_v10.core.network.ApiClient
import com.example.directorduck_v10.core.network.isOk
import com.example.directorduck_v10.feature.mockexam.model.MockExamJoinRequest
import com.example.directorduck_v10.feature.mockexam.model.MockExamSessionDTO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MineViewModel : ViewModel() {

    // 当前展示的模考场次
    private val _targetSession = MutableLiveData<MockExamSessionDTO?>()
    val targetSession: LiveData<MockExamSessionDTO?> = _targetSession

    // 报名状态：true=已报名，false=未报名
    private val _isRegistered = MutableLiveData<Boolean>()
    val isRegistered: LiveData<Boolean> = _isRegistered

    // 提示信息
    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    /**
     * 加载逻辑修改：
     * 1. 筛选出未来的场次
     * 2. 优先找【第一个未报名】的
     * 3. 如果全都报名了，则显示【最后一场】，状态设为已报名
     */
    fun loadLatestExam(userId: Long) {
        viewModelScope.launch {
            try {
                // 1. 获取列表
                val response = ApiClient.mockExamService.listSessions()
                if (response.isSuccessful && response.body()?.isOk() == true) {
                    val allSessions = response.body()?.data ?: emptyList()
                    val now = System.currentTimeMillis()

                    // 2. 筛选条件：未结束 且 未截止报名
                    val availableSessions = allSessions
                        .filter { session ->
                            val endTime = parseTime(session.endTime)
                            val deadline = parseTime(session.registerDeadline)
                            // 还没结束 且 (无截止时间 或 还没到截止时间)
                            endTime > now && (session.registerDeadline.isNullOrEmpty() || deadline > now)
                        }
                        .sortedBy { parseTime(it.startTime) } // 按时间升序（最近的在前）

                    if (availableSessions.isEmpty()) {
                        _targetSession.value = null
                        return@launch
                    }

                    var target: MockExamSessionDTO? = null
                    var isReg = false

                    // 3. 遍历检查状态
                    // 默认假设全部已报名，取最后一场
                    var allRegistered = true

                    for (session in availableSessions) {
                        val checkRes = ApiClient.mockExamService.exists(session.id, userId)
                        val registered = if (checkRes.isSuccessful && checkRes.body()?.isOk() == true) {
                            checkRes.body()?.data == true
                        } else {
                            false
                        }

                        if (!registered) {
                            // 找到了第一个没报名的！
                            target = session
                            isReg = false
                            allRegistered = false
                            break // 停止查找，优先显示这场
                        }
                    }

                    // 4. 逻辑分支
                    if (allRegistered) {
                        // 如果全都预约了 -> 显示最后一场，状态为已预约
                        target = availableSessions.last()
                        isReg = true
                    }
                    // else { target 和 isReg 已经在循环中设置好了 (即第一个未报名的) }

                    // 更新 UI
                    _targetSession.value = target
                    _isRegistered.value = isReg
                }
            } catch (e: Exception) {
                Log.e("MineViewModel", "加载模考失败", e)
                _toastMessage.value = "网络错误"
            }
        }
    }

    /**
     * 报名参加
     */
    fun reserveExam(userId: Long, username: String) {
        val session = _targetSession.value ?: return

        viewModelScope.launch {
            try {
                val req = MockExamJoinRequest(userId, username)
                val response = ApiClient.mockExamService.join(session.id, req)

                if (response.isSuccessful && response.body()?.isOk() == true) {
                    _isRegistered.value = true // 更新状态为已报名
                    _toastMessage.value = "预约成功！请准时参加。"
                } else {
                    val msg = response.body()?.message ?: "预约失败"
                    _toastMessage.value = msg
                    if (msg.contains("重复") || msg.contains("已报名")) {
                        _isRegistered.value = true
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "网络异常，请重试"
            }
        }
    }

    private fun parseTime(timeStr: String?): Long {
        if (timeStr.isNullOrEmpty()) return 0L
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.parse(timeStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}