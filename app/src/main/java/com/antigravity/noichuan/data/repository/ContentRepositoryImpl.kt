package com.antigravity.noichuan.data.repository

import android.content.Context
import com.antigravity.noichuan.data.local.AppDatabase
import com.antigravity.noichuan.data.local.ContentVersionDao
import com.antigravity.noichuan.data.local.ContentVersionEntity
import com.antigravity.noichuan.data.local.TongueTwisterDao
import com.antigravity.noichuan.data.local.TongueTwisterEntity
import com.antigravity.noichuan.data.local.WordDao
import com.antigravity.noichuan.data.local.WordEntity
import com.antigravity.noichuan.data.model.Word
import com.antigravity.noichuan.data.remote.RemoteContentDataSource
import com.antigravity.noichuan.domain.repository.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Cache-first ContentRepository:
 * 1. Bundled assets (words.json) -> Room DB khi khởi chạy lần đầu.
 * 2. Background coroutine kiểm tra content_versions từ Supabase remote config.
 * 3. Nếu remote version > local version: tải words mới, cập nhật Room, áp dụng từ lượt chơi sau.
 * 4. Không có key hoặc mất mạng: chạy 100% offline bằng Room cache / seed.
 * 5. Dữ liệu hỏng hoặc lỗi mạng: bỏ qua an toàn, giữ nguyên cache.
 */
class ContentRepositoryImpl(
    private val context: Context?,
    private val wordDao: WordDao,
    private val tongueTwisterDao: TongueTwisterDao,
    private val contentVersionDao: ContentVersionDao,
    private val remoteDataSource: RemoteContentDataSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : WordRepository {

    constructor(
        context: Context,
        database: AppDatabase,
        remoteDataSource: RemoteContentDataSource,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) : this(
        context = context,
        wordDao = database.wordDao(),
        tongueTwisterDao = database.tongueTwisterDao(),
        contentVersionDao = database.contentVersionDao(),
        remoteDataSource = remoteDataSource,
        scope = scope
    )

    companion object {
        const val BUNDLED_CONTENT_VERSION = 2
    }

    private val initMutex = Mutex()
    private var isInitialized = false

    init {
        scope.launch {
            ensureInitialized()
            syncRemoteContentInBackground()
        }
    }

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        initMutex.withLock {
            if (isInitialized) return@withLock
            val count = wordDao.getWordCount()
            val localVersion = contentVersionDao.getVersion("content")?.version ?: 0
            if ((count == 0 || localVersion < BUNDLED_CONTENT_VERSION) && context != null) {
                seedFromAssets()
            }
            isInitialized = true
        }
    }

    private suspend fun seedFromAssets() {
        val ctx = context ?: return
        try {
            val jsonString = ctx.assets.open("words.json").bufferedReader().use { it.readText() }
            val words = json.decodeFromString<List<Word>>(jsonString)
            val regularWords = words.filter { it.gameMode != "tongue_twister" }
            val tongueTwisters = words.filter { it.gameMode == "tongue_twister" }

            wordDao.insertWords(regularWords.map { WordEntity.fromDomainModel(it) })
            tongueTwisterDao.insertAll(tongueTwisters.map { TongueTwisterEntity.fromDomainModel(it) })
            val currentVer = contentVersionDao.getVersion("content")?.version ?: 0
            val targetVer = maxOf(currentVer, BUNDLED_CONTENT_VERSION)
            contentVersionDao.setVersion(ContentVersionEntity(key = "content", version = targetVer))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Đồng bộ ở nền: Trả về true nếu có cập nhật version mới từ remote, false nếu giữ nguyên.
     */
    suspend fun syncRemoteContentInBackground(): Boolean = withContext(Dispatchers.IO) {
        try {
            val remoteVersion = remoteDataSource.fetchRemoteVersion() ?: return@withContext false
            val localVersion = contentVersionDao.getVersion("content")?.version ?: 0

            if (remoteVersion > localVersion) {
                val newWords = remoteDataSource.fetchRemoteWords() ?: return@withContext false
                if (newWords.isNotEmpty()) {
                    wordDao.insertWords(newWords.map { WordEntity.fromDomainModel(it) })
                }
                val newTongueTwisters = remoteDataSource.fetchRemoteTongueTwisters()
                if (!newTongueTwisters.isNullOrEmpty()) {
                    tongueTwisterDao.insertAll(newTongueTwisters.map { TongueTwisterEntity.fromDomainModel(it) })
                }
                contentVersionDao.setVersion(ContentVersionEntity(key = "content", version = remoteVersion))
                return@withContext true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllWords(): List<Word> = withContext(Dispatchers.IO) {
        ensureInitialized()
        wordDao.getAllWords().map { it.toDomainModel() }
    }

    override suspend fun getWordsBySubjectAndLevel(subject: String, level: String): List<Word> = withContext(Dispatchers.IO) {
        ensureInitialized()
        wordDao.getWordsBySubjectAndLevel(subject, level).map { it.toDomainModel() }
    }

    override suspend fun getWordById(id: String): Word? = withContext(Dispatchers.IO) {
        ensureInitialized()
        wordDao.getWordById(id)?.toDomainModel()
    }

    suspend fun getTongueTwistersByLevel(level: String): List<Word> = withContext(Dispatchers.IO) {
        ensureInitialized()
        tongueTwisterDao.getByLevel(level).map { it.toDomainModel() }
    }

    suspend fun getCurrentContentVersion(): Int = withContext(Dispatchers.IO) {
        ensureInitialized()
        contentVersionDao.getVersion("content")?.version ?: 1
    }
}
