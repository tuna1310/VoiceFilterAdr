package com.antigravity.noichuan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.antigravity.noichuan.data.model.Word

@Entity(tableName = "tongue_twisters")
data class TongueTwisterEntity(
    @PrimaryKey val id: String,
    val sentence: String,
    val phonetic: String,
    val tip: String,
    val level: String
) {
    fun toDomainModel(): Word = Word(
        id = id,
        word = sentence,
        phonetic = phonetic,
        tip = tip,
        level = level,
        subject = "tongue_twister",
        gameMode = "tongue_twister"
    )

    companion object {
        fun fromDomainModel(word: Word): TongueTwisterEntity = TongueTwisterEntity(
            id = word.id,
            sentence = word.word,
            phonetic = word.phonetic,
            tip = word.tip,
            level = word.level
        )
    }
}
