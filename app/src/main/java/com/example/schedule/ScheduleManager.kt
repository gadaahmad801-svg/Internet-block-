package com.example.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class ScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysOfWeek: Set<Int>, // Calendar.SUNDAY(1) .. Calendar.SATURDAY(7)
    val isEnabled: Boolean = true,
    val controlInternet: Boolean = true,
    val controlVolume: Boolean = true,
    val controlAppLock: Boolean = true
) {
    fun formatStartTime(): String = String.format("%02d:%02d", startHour, startMinute)
    fun formatEndTime(): String = String.format("%02d:%02d", endHour, endMinute)

    fun formatDays(): String {
        if (daysOfWeek.size == 7) return "Every day"
        if (daysOfWeek == setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)) return "Weekdays"
        if (daysOfWeek == setOf(Calendar.SATURDAY, Calendar.SUNDAY)) return "Weekends"
        val dayNames = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        return daysOfWeek.sorted().joinToString(", ") { dayNames[it] ?: "" }
    }
}

class ScheduleManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        const val TAG = "ScheduleManager"
        const val ACTION_SCHEDULE_START = "com.example.schedule.ACTION_SCHEDULE_START"
        const val ACTION_SCHEDULE_END = "com.example.schedule.ACTION_SCHEDULE_END"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val _schedules = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val schedules: StateFlow<List<ScheduleItem>> = _schedules.asStateFlow()

    private val _nextActivationSummary = MutableStateFlow("No upcoming schedule")
    val nextActivationSummary: StateFlow<String> = _nextActivationSummary.asStateFlow()

    suspend fun loadSchedules() {
        val rawJson = settingsRepository.schedulesJson.first()
        val list = parseJson(rawJson)
        _schedules.value = list
        updateNextActivationSummary()
    }

    suspend fun addOrUpdateSchedule(item: ScheduleItem) {
        val current = _schedules.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(item)
        }
        saveAndApply(current)
    }

    suspend fun deleteSchedule(id: String) {
        cancelAlarmsForSchedule(id)
        val current = _schedules.value.filter { it.id != id }
        saveAndApply(current)
    }

    suspend fun toggleScheduleEnabled(id: String) {
        val current = _schedules.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
        saveAndApply(current)
    }

    private suspend fun saveAndApply(list: List<ScheduleItem>) {
        _schedules.value = list
        val json = serializeJson(list)
        settingsRepository.saveSchedulesJson(json)

        // Reschedule all active alarms
        list.forEach { item ->
            if (item.isEnabled) {
                scheduleAlarm(item)
            } else {
                cancelAlarmsForSchedule(item.id)
            }
        }
        updateNextActivationSummary()
    }

    fun scheduleAlarm(item: ScheduleItem) {
        cancelAlarmsForSchedule(item.id)

        val nextStartMillis = calculateNextEpochMillis(item.startHour, item.startMinute, item.daysOfWeek)
        val nextEndMillis = calculateNextEpochMillis(item.endHour, item.endMinute, item.daysOfWeek)

        if (nextStartMillis > 0) {
            val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
                action = ACTION_SCHEDULE_START
                putExtra(EXTRA_SCHEDULE_ID, item.id)
            }
            val startPending = PendingIntent.getBroadcast(
                context,
                item.id.hashCode(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(nextStartMillis, startPending)
            Log.d(TAG, "Scheduled start for ${item.title} at $nextStartMillis")
        }

        if (nextEndMillis > 0) {
            val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
                action = ACTION_SCHEDULE_END
                putExtra(EXTRA_SCHEDULE_ID, item.id)
            }
            val endPending = PendingIntent.getBroadcast(
                context,
                item.id.hashCode() + 1,
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(nextEndMillis, endPending)
            Log.d(TAG, "Scheduled end for ${item.title} at $nextEndMillis")
        }
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAlarmsForSchedule(id: String) {
        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_SCHEDULE_START
        }
        val startPending = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPending != null) {
            alarmManager.cancel(startPending)
        }

        val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_SCHEDULE_END
        }
        val endPending = PendingIntent.getBroadcast(
            context,
            id.hashCode() + 1,
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPending != null) {
            alarmManager.cancel(endPending)
        }
    }

    private fun calculateNextEpochMillis(hour: Int, minute: Int, daysOfWeek: Set<Int>): Long {
        if (daysOfWeek.isEmpty()) return 0L
        val now = Calendar.getInstance()

        var bestDiff = Long.MAX_VALUE
        var bestTime = 0L

        for (targetDay in daysOfWeek) {
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            var daysToAdd = (targetDay - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
            if (daysToAdd == 0 && candidate.before(now)) {
                daysToAdd = 7
            }
            candidate.add(Calendar.DAY_OF_YEAR, daysToAdd)

            val diff = candidate.timeInMillis - now.timeInMillis
            if (diff in 1 until bestDiff) {
                bestDiff = diff
                bestTime = candidate.timeInMillis
            }
        }
        return bestTime
    }

    private fun updateNextActivationSummary() {
        val activeSchedules = _schedules.value.filter { it.isEnabled }
        if (activeSchedules.isEmpty()) {
            _nextActivationSummary.value = "No active schedules"
            return
        }

        var earliestTime = Long.MAX_VALUE
        var earliestTitle = ""

        activeSchedules.forEach { item ->
            val nextTime = calculateNextEpochMillis(item.startHour, item.startMinute, item.daysOfWeek)
            if (nextTime in 1 until earliestTime) {
                earliestTime = nextTime
                earliestTitle = "${item.title} (${item.formatStartTime()})"
            }
        }

        if (earliestTime != Long.MAX_VALUE) {
            val cal = Calendar.getInstance().apply { timeInMillis = earliestTime }
            val today = Calendar.getInstance()
            val dayPrefix = if (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                "Today"
            } else if (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1) {
                "Tomorrow"
            } else {
                val dayNames = arrayOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                dayNames[cal.get(Calendar.DAY_OF_WEEK)]
            }
            _nextActivationSummary.value = "$dayPrefix at ${String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))} • $earliestTitle"
        } else {
            _nextActivationSummary.value = "No upcoming schedule"
        }
    }

    private fun parseJson(json: String?): List<ScheduleItem> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ScheduleItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val daysArr = obj.getJSONArray("days")
                val days = mutableSetOf<Int>()
                for (j in 0 until daysArr.length()) {
                    days.add(daysArr.getInt(j))
                }
                list.add(
                    ScheduleItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        startHour = obj.getInt("startHour"),
                        startMinute = obj.getInt("startMinute"),
                        endHour = obj.getInt("endHour"),
                        endMinute = obj.getInt("endMinute"),
                        daysOfWeek = days,
                        isEnabled = obj.optBoolean("isEnabled", true),
                        controlInternet = obj.optBoolean("controlInternet", true),
                        controlVolume = obj.optBoolean("controlVolume", true),
                        controlAppLock = obj.optBoolean("controlAppLock", true)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeJson(list: List<ScheduleItem>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("startHour", item.startHour)
                put("startMinute", item.startMinute)
                put("endHour", item.endHour)
                put("endMinute", item.endMinute)
                put("isEnabled", item.isEnabled)
                put("controlInternet", item.controlInternet)
                put("controlVolume", item.controlVolume)
                put("controlAppLock", item.controlAppLock)
                val daysArr = JSONArray()
                item.daysOfWeek.forEach { daysArr.put(it) }
                put("days", daysArr)
            }
            arr.put(obj)
        }
        return arr.toString()
    }
}
