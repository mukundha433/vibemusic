package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.example.gemini.VibeAssistantResult
import com.example.gemini.VibeGeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit

data class EmojiReaction(
    val senderName: String,
    val emoji: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CollabMessage(
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class VibeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VibeRepository(application)
    private val geminiClient = VibeGeminiClient()

    // Database entries
    val allTracks: StateFlow<List<Track>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = repository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Loaded Queue
    private val _currentQueue = MutableStateFlow<List<Track>>(emptyList())
    val currentQueue: StateFlow<List<Track>> = _currentQueue.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    val currentTrack: StateFlow<Track?> = combine(currentQueue, _currentTrackIndex) { queue, index ->
        if (queue.isNotEmpty() && index in queue.indices) queue[index] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Player Playback Engine
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progressSecs = MutableStateFlow(0)
    val progressSecs: StateFlow<Int> = _progressSecs.asStateFlow()

    private val _seekRequest = MutableSharedFlow<Float>(replay = 0)
    val seekRequest = _seekRequest.asSharedFlow()

    private val _currentTrackDuration = MutableStateFlow(180)
    val currentTrackDuration: StateFlow<Int> = _currentTrackDuration.asStateFlow()

    private val _youtubeSearchResults = MutableStateFlow<List<Track>>(emptyList())
    val youtubeSearchResults: StateFlow<List<Track>> = _youtubeSearchResults.asStateFlow()

    private val _isLoadingYoutubeSearch = MutableStateFlow(false)
    val isLoadingYoutubeSearch: StateFlow<Boolean> = _isLoadingYoutubeSearch.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeatOne = MutableStateFlow(false)
    val isRepeatOne: StateFlow<Boolean> = _isRepeatOne.asStateFlow()

    // Crossfade (0s - 12s)
    private val _crossfadeSecs = MutableStateFlow(5f)
    val crossfadeSecs: StateFlow<Float> = _crossfadeSecs.asStateFlow()

    // Volume state (0 - 100)
    private val _volume = MutableStateFlow(80)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    fun setVolume(vol: Int) {
        _volume.value = vol.coerceIn(0, 100)
    }

    // Equalizer State
    private val _selectedEqualizer = MutableStateFlow("3D Spatial Neo-Vibe")
    val selectedEqualizer: StateFlow<String> = _selectedEqualizer.asStateFlow()

    private val _audioQuality = MutableStateFlow("Extreme Lossless 320kbps")
    val audioQuality: StateFlow<String> = _audioQuality.asStateFlow()

    // Sleep Timer
    private val _sleepTimerMinutesLeft = MutableStateFlow(0)
    val sleepTimerMinutesLeft: StateFlow<Int> = _sleepTimerMinutesLeft.asStateFlow()

    // AI Voice Assistant State
    private val _voiceIsProcessing = MutableStateFlow(false)
    val voiceIsProcessing: StateFlow<Boolean> = _voiceIsProcessing.asStateFlow()

    private val _voiceDialogList = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList()) // Pair(Text, IsUserMessage)
    val voiceDialogList: StateFlow<List<Pair<String, Boolean>>> = _voiceDialogList.asStateFlow()

    private val _lastAssistantReply = MutableStateFlow<String?>(null)
    val lastAssistantReply: StateFlow<String?> = _lastAssistantReply.asStateFlow()

    // Real-Time Collaborative Room Session State
    private val _activeCollabRoomCode = MutableStateFlow<String?>(null)
    val activeCollabRoomCode: StateFlow<String?> = _activeCollabRoomCode.asStateFlow()

    private val _roomParticipants = MutableStateFlow<List<String>>(emptyList())
    val roomParticipants: StateFlow<List<String>> = _roomParticipants.asStateFlow()

    private val _floatingEmojis = MutableStateFlow<List<EmojiReaction>>(emptyList())
    val floatingEmojis: StateFlow<List<EmojiReaction>> = _floatingEmojis.asStateFlow()

    private val _collabChatMessages = MutableStateFlow<List<CollabMessage>>(emptyList())
    val collabChatMessages: StateFlow<List<CollabMessage>> = _collabChatMessages.asStateFlow()

    private val _isHostControlsOnly = MutableStateFlow(false)
    val isHostControlsOnly: StateFlow<Boolean> = _isHostControlsOnly.asStateFlow()

    // Auth Simulation
    private val _currentUserEmail = MutableStateFlow<String?>("guest.vibes@aistudio.com")
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _isUserPremium = MutableStateFlow(false)
    val isUserPremium: StateFlow<Boolean> = _isUserPremium.asStateFlow()

    // UI search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLanguageFilter = MutableStateFlow("All")
    val selectedLanguageFilter: StateFlow<String> = _selectedLanguageFilter.asStateFlow()

    private val _selectedMoodFilter = MutableStateFlow("All")
    val selectedMoodFilter: StateFlow<String> = _selectedMoodFilter.asStateFlow()

    // Download animations simulations map {trackId -> progress0..100}
    private val _downloadProgressMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Int>> = _downloadProgressMap.asStateFlow()

    private var playbackJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        // Initialize Default Track Database contents automatically
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
            // Set initial queue
            allTracks.collectLatest { tracks ->
                if (tracks.isNotEmpty() && _currentQueue.value.isEmpty()) {
                    _currentQueue.value = tracks
                    _currentTrackIndex.value = 0
                }
            }
        }

        // Start playback counter ticker job
        startPlaybackTicker()
    }

    // Playback control functions
    private fun startPlaybackTicker() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isPlaying.value) {
                    val track = currentTrack.value
                    if (track != null) {
                        // Only tick locally if we do not have a streaming URL (YouTube handles the ticking itself)
                        if (track.streamingUrl.isEmpty()) {
                            val nextPos = _progressSecs.value + 1
                            if (nextPos >= track.durationSeconds) {
                                _progressSecs.value = 0
                                playNextTrack()
                            } else {
                                _progressSecs.value = nextPos
                            }
                        }
                    }
                }
            }
        }
    }

    fun updatePlaybackProgress(seconds: Int) {
        _progressSecs.value = seconds
    }

    fun updateDuration(durationSecs: Int) {
        _currentTrackDuration.value = durationSecs
    }

    fun onTrackEnded() {
        playNextTrack()
    }

    fun searchYouTube(query: String) {
        if (query.isBlank()) {
            _youtubeSearchResults.value = emptyList()
            return
        }
        _isLoadingYoutubeSearch.value = true
        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.YOUTUBE_API_KEY
                if (apiKey.isEmpty() || apiKey == "YOUR_YOUTUBE_API_KEY") {
                    Log.e("VibeViewModel", "YouTube API Key is missing. Falling back to local fuzzy query.")
                    // Filter matches in local tracks
                    val filtered = allTracks.value.filter {
                        it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
                    }
                    _youtubeSearchResults.value = filtered
                    _isLoadingYoutubeSearch.value = false
                    return@launch
                }

                val client = OkHttpClient.Builder().build()
                val response = withContext(Dispatchers.IO) {
                    val request = okhttp3.Request.Builder()
                        .url("https://www.googleapis.com/youtube/v3/search?part=snippet&q=${java.net.URLEncoder.encode(query, "UTF-8")}&type=video&maxResults=8&key=$apiKey")
                        .build()
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string() ?: ""
                val root = JSONObject(responseBody)
                val items = root.optJSONArray("items")
                val results = mutableListOf<Track>()
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val idObj = item.optJSONObject("id")
                        val videoId = idObj?.optString("videoId") ?: ""
                        val snippet = item.optJSONObject("snippet")
                        val title = snippet?.optString("title") ?: "YouTube Track"
                        val artist = snippet?.optString("channelTitle") ?: "YouTube Artist"
                        val thumbnails = snippet?.optJSONObject("thumbnails")
                        val high = thumbnails?.optJSONObject("high") ?: thumbnails?.optJSONObject("default")
                        val imageUrl = high?.optString("url") ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"

                        if (videoId.isNotEmpty()) {
                            results.add(
                                Track(
                                    id = "yt_$videoId",
                                    title = title.replace("&amp;", "&").replace("&#39;", "'").replace("&quot;", "\""),
                                    artist = artist,
                                    album = "YouTube Music Stream",
                                    durationSeconds = 180, // Updated by player duration once loaded
                                    imageUrl = imageUrl,
                                    language = "English",
                                    genre = "YouTube Stream",
                                    mood = "energetic",
                                    lyrics = "[00:00] Streaming live from YouTube Music...",
                                    streamingUrl = videoId
                                )
                            )
                        }
                    }
                }
                _youtubeSearchResults.value = results
            } catch (e: Exception) {
                Log.e("VibeViewModel", "YouTube search failed: ${e.message}", e)
                val filtered = allTracks.value.filter {
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
                }
                _youtubeSearchResults.value = filtered
            } finally {
                _isLoadingYoutubeSearch.value = false
            }
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
        val track = currentTrack.value
        if (track != null) {
            recordRecentlyPlayed(track.id)
        }
    }

    fun selectTrack(trackId: String, customizedQueue: List<Track>? = null) {
        val targetQueue = customizedQueue ?: allTracks.value
        val idx = targetQueue.indexOfFirst { it.id == trackId }
        val finalQueue = if (idx == -1) {
            // Check if it's from Youtube search results and merge it
            val ytTrack = youtubeSearchResults.value.firstOrNull { it.id == trackId }
            if (ytTrack != null) {
                viewModelScope.launch {
                    repository.dao.insertTracks(listOf(ytTrack))
                }
                targetQueue + ytTrack
            } else targetQueue
        } else targetQueue

        val finalIdx = finalQueue.indexOfFirst { it.id == trackId }
        if (finalIdx != -1) {
            _currentQueue.value = finalQueue
            _currentTrackIndex.value = finalIdx
            _progressSecs.value = 0
            _isPlaying.value = true
            recordRecentlyPlayed(trackId)
        }
    }

    fun playNextTrack() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        if (_isRepeatOne.value) {
            _progressSecs.value = 0
            val track = currentTrack.value
            if (track != null) {
                viewModelScope.launch {
                    _seekRequest.emit(0f)
                }
            }
            return
        }

        if (_isShuffle.value) {
            _currentTrackIndex.value = (queue.indices).random()
        } else {
            val nextIdx = _currentTrackIndex.value + 1
            _currentTrackIndex.value = if (nextIdx < queue.size) nextIdx else 0
        }
        _progressSecs.value = 0
        _isPlaying.value = true
    }

    fun playPreviousTrack() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        val prevIdx = _currentTrackIndex.value - 1
        _currentTrackIndex.value = if (prevIdx >= 0) prevIdx else queue.size - 1
        _progressSecs.value = 0
        _isPlaying.value = true
    }

    fun seekTo(positionSecs: Int) {
        val track = currentTrack.value ?: return
        val duration = _currentTrackDuration.value.coerceAtLeast(track.durationSeconds)
        _progressSecs.value = positionSecs.coerceIn(0, duration)
        viewModelScope.launch {
            _seekRequest.emit(positionSecs.toFloat())
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeatOne() {
        _isRepeatOne.value = !_isRepeatOne.value
    }

    fun setCrossfade(seconds: Float) {
        _crossfadeSecs.value = seconds
    }

    fun setEqualizerPreset(presetName: String) {
        _selectedEqualizer.value = presetName
    }

    fun setAudioQualityOption(quality: String) {
        _audioQuality.value = quality
    }

    // Toggle Favourite
    fun toggleFavorite(trackId: String) {
        viewModelScope.launch {
            val track = allTracks.value.firstOrNull { it.id == trackId }
            if (track != null) {
                val updatedTrack = track.copy(isFavorite = !track.isFavorite)
                repository.dao.updateTrack(updatedTrack)
            }
        }
    }

    // Simulate physical downloads
    fun downloadTrackOffline(trackId: String) {
        if (_downloadProgressMap.value.containsKey(trackId)) return // already download in progress or active

        viewModelScope.launch {
            // start animation loop
            for (progress in 0..100 step 10) {
                _downloadProgressMap.update { it + (trackId to progress) }
                delay(300)
            }

            // update Room Database
            val track = allTracks.value.firstOrNull { it.id == trackId }
            if (track != null) {
                repository.dao.updateTrack(track.copy(isDownloaded = true))
            }
            _downloadProgressMap.update { it - trackId } // Clear from progress tracking bar
        }
    }

    // Record played items in Room
    private fun recordRecentlyPlayed(trackId: String) {
        viewModelScope.launch {
            repository.dao.insertRecentlyPlayed(RecentlyPlayed(trackId))
        }
    }

    // Set Sleep Timer with visual countdown
    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutesLeft.value = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                while (_sleepTimerMinutesLeft.value > 0) {
                    delay(60000) // minute block
                    val dec = _sleepTimerMinutesLeft.value - 1
                    _sleepTimerMinutesLeft.value = dec
                    if (dec == 0) {
                        _isPlaying.value = false // Stop music playback
                    }
                }
            }
        }
    }

    // Vocal Assistance NLU Handler
    fun submitVoiceQuery(rawQuery: String) {
        if (rawQuery.isBlank()) return
        
        _voiceDialogList.update { it + (rawQuery to true) } // Add user text bubble
        _voiceIsProcessing.value = true

        viewModelScope.launch {
            try {
                // Synthesize with AI Gemini model or fallbacks
                val result = geminiClient.processAssistantVoiceQuery(rawQuery, allTracks.value)
                
                _voiceDialogList.update { it + (result.replyText to false) } // Add Assistant text bubble
                _lastAssistantReply.value = result.replyText

                // Execute appropriate action commanded by AI NLU engine
                when (result.actionCommand) {
                    "PLAY_TRACK" -> {
                        val trackId = result.actionTargetValue
                        if (allTracks.value.any { it.id == trackId }) {
                            selectTrack(trackId)
                        }
                    }
                    "PLAY_MOOD" -> {
                        val mood = result.actionTargetValue.lowercase()
                        val matchedTracks = allTracks.value.filter { it.mood.lowercase() == mood }
                        if (matchedTracks.isNotEmpty()) {
                            _currentQueue.value = matchedTracks
                            _currentTrackIndex.value = 0
                            _progressSecs.value = 0
                            _isPlaying.value = true
                        }
                    }
                    "PLAY_LANGUAGE" -> {
                        val lang = result.actionTargetValue
                        val matchedTracks = allTracks.value.filter { it.language.equals(lang, ignoreCase = true) }
                        if (matchedTracks.isNotEmpty()) {
                            _currentQueue.value = matchedTracks
                            _currentTrackIndex.value = 0
                            _progressSecs.value = 0
                            _isPlaying.value = true
                        }
                    }
                    "INVITE_COLLAB" -> {
                        val partnerName = result.actionTargetValue
                        startCollabRoomSession(partnerName)
                    }
                    "GENERATE_PLAYLIST" -> {
                        val playlistTitle = result.actionTargetValue
                        createAIPlaylistedSongs(playlistTitle)
                    }
                    else -> {
                        // TALK - do nothing, just play conversation speech
                    }
                }
            } catch (e: Exception) {
                Log.e("VibeViewModel", "Error processing voice query: ${e.message}")
                _voiceDialogList.update { it + ("My connection to the cloud audio core timing seems blurred, but let me keep playing awesome tunes!" to false) }
            } finally {
                _voiceIsProcessing.value = false
            }
        }
    }

    private fun createAIPlaylistedSongs(title: String) {
        viewModelScope.launch {
            val randomId = "pl_ai_${System.currentTimeMillis()}"
            val newPl = Playlist(
                id = randomId,
                name = "AI: " + title.replace("generate", "", true).replace("playlist", "", true).trim().capitalize(),
                description = "Custom synthesized lofi-acoustic blend matching: '$title'",
                imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"
            )
            repository.dao.insertPlaylist(newPl)
            // Add half of our tracks to this new playlist
            allTracks.value.shuffled().take(4).forEach { t ->
                repository.dao.insertPlaylistTrack(PlaylistTrack(randomId, t.id))
            }
        }
    }

    // Real-Time Sync Collaborative Session
    fun startCollabRoomSession(partner: String = "Mudaseer") {
        _activeCollabRoomCode.value = "VIBE-${(100..999).random()}-${(10..99).random()}"
        _roomParticipants.value = listOf("You (Host)", partner, "Stella Vibe")
        
        // Post welcome dialogs
        _collabChatMessages.value = listOf(
            CollabMessage("System AI Host", "Room created successfully. Live synchronization activated in high resolution."),
            CollabMessage(partner, "Ayo what is up! Loving this synced player!"),
            CollabMessage("Stella Vibe", "Let's queue that mass Tamil track! 🔥")
        )

        // Periodically post floating heart / fire reaction simulations from friends to show true live micro-interaction visual polish
        viewModelScope.launch {
            val emojis = listOf("🔥", "❤️", "👏", "⚡", "🙌")
            val users = listOf(partner, "Stella Vibe")
            while (_activeCollabRoomCode.value != null) {
                delay((3000..7000).random().toLong())
                val newReaction = EmojiReaction(
                    senderName = users.random(),
                    emoji = emojis.random()
                )
                _floatingEmojis.update { (it + newReaction).takeLast(10) }
            }
        }
    }

    fun exitCollabSession() {
        _activeCollabRoomCode.value = null
        _roomParticipants.value = emptyList()
        _floatingEmojis.value = emptyList()
        _collabChatMessages.value = emptyList()
    }

    fun emitEmojiReaction(emoji: String) {
        val newReaction = EmojiReaction(
            senderName = "You",
            emoji = emoji
        )
        _floatingEmojis.update { (it + newReaction).takeLast(10) }
    }

    fun sendCollabChatMessage(text: String) {
        if (text.isBlank()) return
        val newMsg = CollabMessage("You", text)
        _collabChatMessages.update { it + newMsg }
    }

    fun toggleHostOnlyControls(enabled: Boolean) {
        _isHostControlsOnly.value = enabled
    }

    // Filter selectors
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectLanguageFilter(lang: String) {
        _selectedLanguageFilter.value = lang
    }

    fun selectMoodFilter(mood: String) {
        _selectedMoodFilter.value = mood
    }

    // Auth flows
    fun executeLogin(email: String) {
        _currentUserEmail.value = email
    }

    fun triggerLogout() {
        _currentUserEmail.value = null
    }

    fun togglePremiumSubscription(purchased: Boolean = true) {
        _isUserPremium.value = purchased
    }
}
