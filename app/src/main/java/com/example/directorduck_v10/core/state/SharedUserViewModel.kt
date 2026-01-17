package com.example.directorduck_v10.core.state

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.directorduck_v10.data.model.User

class SharedUserViewModel : ViewModel() {
    val user = MutableLiveData<User>()
}
