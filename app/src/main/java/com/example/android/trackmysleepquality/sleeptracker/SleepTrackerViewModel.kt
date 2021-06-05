/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
    val databaseDao: SleepDatabaseDao, application: Application
) : AndroidViewModel(application) {

    private var tonight = MutableLiveData<SleepNight?>()

    private val nights = databaseDao.getAllNights()

//    private var viewModelJob = Job()

//    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)


/*    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }*/

    val nightsString = Transformations.map(nights) { nights ->
        formatNights(nights, application.resources)
    }

    var startButtonEnaled: LiveData<Boolean> = Transformations.map(tonight) {
        null == it
    }

    var stopButtonEnaled: LiveData<Boolean> = Transformations.map(tonight) {
        null != it
    }

    var clearButtonEnaled = Transformations.map(nights) {
        it?.isNotEmpty()
    }

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    private val _showSnackbarEvent = MutableLiveData<Boolean>()
    val showSnackbarEvent: LiveData<Boolean>
        get() = _showSnackbarEvent

    fun doneShowSnackbar() {
        _showSnackbarEvent.value = false
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        viewModelScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }


    private suspend fun getTonightFromDatabase(): SleepNight? {
        var night = databaseDao.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    private suspend fun clear() {
        databaseDao.clear()
    }

    private suspend fun insert(sleepNight: SleepNight) {
        databaseDao.insert(sleepNight)
    }

    private suspend fun update(sleepNight: SleepNight) {
        databaseDao.update(sleepNight)
    }

    /**
     * Executes when the START button is clicked.
     */
    fun onStartTracking() {
        viewModelScope.launch {
            // Create a new night, which captures the current time,
            // and insert it into the database.
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }

    /**
     * Executes when the STOP button is clicked.
     */
    fun onStopTracking() {
        viewModelScope.launch {
            // In Kotlin, the return@label syntax is used for specifying which function among
            // several nested ones this statement returns from.
            // In this case, we are specifying to return from launch(),
            // not the lambda.
            val oldNight = tonight.value ?: return@launch

            // Update the night in the database to add the end time.
            oldNight.endTimeMilli = System.currentTimeMillis()
            update(oldNight)
            _navigateToSleepQuality.value = oldNight

        }
    }

    /**
     * Executes when the CLEAR button is clicked.
     */
    fun onClear() {
        viewModelScope.launch {
            // Clear the database table.
            clear()

            // And clear tonight since it's no longer in the database
            tonight.value = null
        }
        // Show a snackbar message, because it's friendly.
        _showSnackbarEvent.value = true
    }

}

