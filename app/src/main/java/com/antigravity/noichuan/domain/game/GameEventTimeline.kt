package com.antigravity.noichuan.domain.game

import org.json.JSONArray
import org.json.JSONObject

/**
 * Loại sự kiện trong dòng thời gian trận đấu (Game Event Timeline).
 * Phục vụ cho việc hòa âm SFX hậu kỳ vào video MP4 ở phase A6.
 */
enum class TimelineEventType {
    GATE_SPAWN,
    MATCH_SUCCESS,
    WORD_WRONG,
    TIMEOUT,
    LIFE_LOST,
    COMBO_MILESTONE,
    GAME_PAUSED,
    GAME_RESUMED,
    GAME_OVER
}

/**
 * Bản ghi sự kiện tại mốc thời gian (millisecond từ lúc bắt đầu trận đấu).
 */
data class GameTimelineEvent(
    val timestampMs: Long,
    val eventType: TimelineEventType,
    val payload: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("timestampMs", timestampMs)
        put("eventType", eventType.name)
        put("payload", payload)
    }

    companion object {
        fun fromJson(json: JSONObject): GameTimelineEvent {
            return GameTimelineEvent(
                timestampMs = json.optLong("timestampMs", 0L),
                eventType = TimelineEventType.valueOf(json.optString("eventType", TimelineEventType.GATE_SPAWN.name)),
                payload = json.optString("payload", "")
            )
        }
    }
}

/**
 * Bộ quản lý timeline ghi nhận mọi sự kiện trong suốt vòng đời trận đấu.
 */
class GameEventTimeline {

    private val _events = mutableListOf<GameTimelineEvent>()
    val events: List<GameTimelineEvent> get() = _events.toList()

    fun record(eventType: TimelineEventType, timestampMs: Long, payload: String = "") {
        _events.add(GameTimelineEvent(timestampMs, eventType, payload))
    }

    fun clear() {
        _events.clear()
    }

    fun toJsonString(): String {
        val array = JSONArray()
        for (event in _events) {
            array.put(event.toJson())
        }
        return array.toString()
    }

    companion object {
        fun fromJsonString(jsonStr: String): GameEventTimeline {
            val timeline = GameEventTimeline()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    timeline._events.add(GameTimelineEvent.fromJson(obj))
                }
            } catch (e: Exception) {
                // Return empty timeline on parse error
            }
            return timeline
        }
    }
}
