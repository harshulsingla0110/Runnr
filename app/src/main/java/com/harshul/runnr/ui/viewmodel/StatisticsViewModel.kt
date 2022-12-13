package com.harshul.runnr.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.harshul.runnr.data.repos.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(repository: MainRepository) : ViewModel() {
}