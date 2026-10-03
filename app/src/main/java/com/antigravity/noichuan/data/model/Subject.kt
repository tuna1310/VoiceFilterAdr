package com.antigravity.noichuan.data.model

import androidx.compose.ui.graphics.Color

enum class Subject(
    val key: String,
    val title: String,
    val titleVi: String,
    val emoji: String,
    val learnersCount: Int,
    val cardColor: Color,
    val accentColor: Color,
    val banner: String? = null,
    val defaultGameMode: String = "voice_runner"
) {
    COLORS(
        key = "colors",
        title = "Colors",
        titleVi = "Màu sắc",
        emoji = "🎨",
        learnersCount = 19740,
        cardColor = Color(0xFF2F80ED),
        accentColor = Color(0xFF1D68C7)
    ),
    CAR_BRANDS(
        key = "car_brands",
        title = "Car Brands",
        titleVi = "Hãng xe",
        emoji = "🚗",
        learnersCount = 15516,
        cardColor = Color(0xFFFF7043),
        accentColor = Color(0xFFE64A19)
    ),
    WEEKDAYS(
        key = "weekdays",
        title = "Weekdays",
        titleVi = "Ngày trong tuần",
        emoji = "📅",
        learnersCount = 24110,
        cardColor = Color(0xFF29B6F6),
        accentColor = Color(0xFF0288D1)
    ),
    MONTHS(
        key = "months",
        title = "Months",
        titleVi = "Tháng trong năm",
        emoji = "🗓️",
        learnersCount = 18230,
        cardColor = Color(0xFFAB47BC),
        accentColor = Color(0xFF7B1FA2)
    ),
    ANIMALS(
        key = "animals",
        title = "Animals",
        titleVi = "Động vật",
        emoji = "🦁",
        learnersCount = 33224,
        cardColor = Color(0xFF4CAF50),
        accentColor = Color(0xFF2E7D32)
    ),
    VEHICLES(
        key = "vehicles",
        title = "Vehicles",
        titleVi = "Phương tiện",
        emoji = "🚀",
        learnersCount = 21890,
        cardColor = Color(0xFF7E57C2),
        accentColor = Color(0xFF512DA8)
    ),
    KITCHEN(
        key = "kitchen",
        title = "Kitchen Utensils",
        titleVi = "Dụng cụ bếp",
        emoji = "🍳",
        learnersCount = 12450,
        cardColor = Color(0xFFFFA726),
        accentColor = Color(0xFFF57C00)
    ),
    COUNTRIES(
        key = "countries",
        title = "Countries",
        titleVi = "Quốc gia",
        emoji = "🌍",
        learnersCount = 32261,
        cardColor = Color(0xFF26A69A),
        accentColor = Color(0xFF00796B)
    ),
    DAILY_WORDS(
        key = "daily_words",
        title = "Daily Words",
        titleVi = "Từ vựng hàng ngày",
        emoji = "☕",
        learnersCount = 41200,
        cardColor = Color(0xFF00ACC1),
        accentColor = Color(0xFF00838F)
    ),
    TRAVEL_FOOD(
        key = "travel_food",
        title = "Travel & Food",
        titleVi = "Du lịch & Ẩm thực",
        emoji = "✈️",
        learnersCount = 38900,
        cardColor = Color(0xFFFF7043),
        accentColor = Color(0xFFD84315)
    ),
    ENTERTAINMENT_SPORTS(
        key = "entertainment_sports",
        title = "Movies & Sports",
        titleVi = "Phim ảnh & Thể thao",
        emoji = "🎬",
        learnersCount = 29500,
        cardColor = Color(0xFF8E24AA),
        accentColor = Color(0xFF6A1B9A)
    ),
    SLANG_BRAINROT(
        key = "slang_brainrot",
        title = "Slang & Pop Culture",
        titleVi = "Từ lóng & Giới trẻ",
        emoji = "🔥",
        learnersCount = 52300,
        cardColor = Color(0xFFFF1744),
        accentColor = Color(0xFFD50000)
    );

    companion object {
        fun fromKey(key: String): Subject {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: COLORS
        }
    }
}
