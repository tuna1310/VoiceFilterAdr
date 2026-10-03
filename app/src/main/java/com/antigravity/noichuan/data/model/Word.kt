package com.antigravity.noichuan.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Word(
    val id: String,
    val word: String,
    val phonetic: String,
    val tip: String,
    val level: String,
    val subject: String,
    /**
     * Danh sách các biến thể phát âm được chấp nhận (đặt trong words.json, trường tùy chọn).
     * Ví dụ: target="write" có acceptedVariants=["right"] — thay vì hardcode trong matcher.
     */
    @SerialName("accepted_variants")
    val acceptedVariants: List<String> = emptyList(),
    /**
     * Optional banner image path. Priority: word banner > category banner > default gradient.
     * Asset paths use android_asset URLs, e.g. file:///android_asset/banners/banner_colors_brown.webp.
     */
    val banner: String? = null,
    @SerialName("game_mode")
    val gameMode: String? = null
)
