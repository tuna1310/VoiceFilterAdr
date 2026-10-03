package com.antigravity.noichuan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.noichuan.data.model.Word
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val id: String,
    val word: String,
    val phonetic: String,
    val tip: String,
    val level: String,
    val subject: String,
    val acceptedVariantsJson: String = "[]",
    val banner: String? = null,
    val gameMode: String? = null
) {
    fun toDomainModel(): Word {
        val variants = runCatching {
            Json.decodeFromString<List<String>>(acceptedVariantsJson)
        }.getOrDefault(emptyList())

        return Word(
            id = id,
            word = word,
            phonetic = phonetic,
            tip = tip,
            level = level,
            subject = subject,
            acceptedVariants = variants,
            banner = banner,
            gameMode = gameMode
        )
    }

    companion object {
        fun fromDomainModel(word: Word): WordEntity {
            val variantsJson = runCatching {
                Json.encodeToString(word.acceptedVariants)
            }.getOrDefault("[]")

            return WordEntity(
                id = word.id,
                word = word.word,
                phonetic = word.phonetic,
                tip = word.tip,
                level = word.level,
                subject = word.subject,
                acceptedVariantsJson = variantsJson,
                banner = word.banner,
                gameMode = word.gameMode
            )
        }
    }
}
