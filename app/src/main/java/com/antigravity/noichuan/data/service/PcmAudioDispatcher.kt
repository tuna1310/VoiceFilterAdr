package com.antigravity.noichuan.data.service

import java.util.concurrent.CopyOnWriteArrayList

typealias PcmChunkListener = (buffer: ShortArray, readSize: Int) -> Unit

/**
 * Bộ điều phối luồng âm thanh PCM từ một AudioRecord duy nhất (Tee / Fan-out Pattern).
 * Cho phép nhiều bên nhận cùng lúc:
 * - SpeechRecognizerEngine (nhận diện giọng nói)
 * - SilenceWatchdog (giám sát khoảng lặng & tính RMS)
 * - AudioFileWriter (lưu file âm thanh phục vụ ghép video)
 */
class PcmAudioDispatcher {

    private val listeners = CopyOnWriteArrayList<PcmChunkListener>()

    fun registerListener(listener: PcmChunkListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: PcmChunkListener) {
        listeners.remove(listener)
    }

    fun clearListeners() {
        listeners.clear()
    }

    fun dispatchChunk(buffer: ShortArray, readSize: Int) {
        if (readSize <= 0) return
        for (listener in listeners) {
            try {
                listener(buffer, readSize)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun listenerCount(): Int = listeners.size
}
