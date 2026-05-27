package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class VibeRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        VibeDatabase::class.java,
        "vibe_database"
    ).build()

    val dao = db.dao()

    val allTracks: Flow<List<Track>> = dao.getAllTracks()
    val playlists: Flow<List<Playlist>> = dao.getPlaylists()
    val recentlyPlayed: Flow<List<Track>> = dao.getRecentlyPlayedTracks()

    suspend fun initDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        val tracksCount = allTracks.first().size
        if (tracksCount == 0) {
            // Seed default tracks
            val mockTracks = listOf(
                // TAMIL
                Track(
                    id = "ta_1",
                    title = "Marana Mass Rave",
                    artist = "Anirudh Synth Vibe",
                    album = "Petta Pulse",
                    durationSeconds = 210,
                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300",
                    language = "Tamil",
                    genre = "Dance & Mass",
                    mood = "energetic",
                    lyrics = "[00:00] Play the mass beats!\n[00:10] Marana Mass Rave is on!\n[00:25] (Heavy bass dropping...)\n[00:45] Tamil beats spreading round the globe.",
                    streamingUrl = "8K87CZZGZ0o"
                ),
                Track(
                    id = "ta_2",
                    title = "Kadhale Dreamer",
                    artist = "Sid Sriram (AI Tribute)",
                    album = "Heart Space",
                    durationSeconds = 265,
                    imageUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=300",
                    language = "Tamil",
                    genre = "Melody",
                    mood = "chill",
                    lyrics = "[00:00] Slow violin sound...\n[00:12] Kadhale, O Kadhale...\n[00:30] Life is a beautiful neon dream with you.\n[01:10] Our path is lit up by bright neon stars.",
                    streamingUrl = "f613w5_c3S0"
                ),
                Track(
                    id = "ta_3",
                    title = "Kannazhaga Tears",
                    artist = "Sruthi H. & Dhanush Glow",
                    album = "3 Degrees of Neon",
                    durationSeconds = 240,
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                    language = "Tamil",
                    genre = "Pathos Ballad",
                    mood = "sad",
                    lyrics = "[00:00] Gentle acoustic waves...\n[00:20] Kannazhaga, don't leave me in the dark.\n[00:55] Tears shine like diamonds on a dark winter sky.\n[01:40] Underneath the rain, we hold our last breath.",
                    streamingUrl = "K_ZgqV0SWh8"
                ),
                // KOREAN
                Track(
                    id = "ko_1",
                    title = "Seoul Blackout Rap",
                    artist = "GD-Neon & Jennie Beats",
                    album = "K-Cyberpunk",
                    durationSeconds = 185,
                    imageUrl = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=300",
                    language = "Korean",
                    genre = "Korean Rap",
                    mood = "energetic",
                    lyrics = "[00:00] Seoul City lights turning off!\n[00:10] Drop it fast, neon fire!\n[00:22] (Fast rap) Neon georireul georeo tto daraseo chumeul chwo\n[00:45] We go higher than the tallest high-rises!",
                    streamingUrl = "9mQk70WSFsk"
                ),
                Track(
                    id = "ko_2",
                    title = "Fallin' Cherry Lotus",
                    artist = "IU Glow",
                    album = "Spring in Retrograde",
                    durationSeconds = 222,
                    imageUrl = "https://images.unsplash.com/photo-1526218626217-dc65a29bb444?w=300",
                    language = "Korean",
                    genre = "K-Pop",
                    mood = "happy",
                    lyrics = "[00:00] Beautiful piano intro...\n[00:15] Spring days are back in full neon light.\n[00:35] Golden leaves falling down on our heads.\n[01:05] Sranghae, say you vibe too...",
                    streamingUrl = "TgOu00Mf3-8"
                ),
                // ENGLISH
                Track(
                    id = "en_1",
                    title = "Cyber Synth Sunrise",
                    artist = "Lupa Lipa & The Grid",
                    album = "Neon Horizon 2026",
                    durationSeconds = 195,
                    imageUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300",
                    language = "English",
                    genre = "Synthpop",
                    mood = "party",
                    lyrics = "[00:00] (Instrumental synth intro...)\n[00:15] Drifting along the electronic waves\n[00:30] Feeling the rhythm of a new digital age\n[00:50] Turn up the sound, can you hear our hearts thumping?",
                    streamingUrl = "unb8K0K8jS4"
                ),
                Track(
                    id = "en_2",
                    title = "Rainy Coffee Chill",
                    artist = "The Midnight Lofi Crew",
                    album = "Late Night Raindrops",
                    durationSeconds = 180,
                    imageUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=300",
                    language = "English",
                    genre = "Lofi Hip-Hop",
                    mood = "chill",
                    lyrics = "[00:00] Rain falling down on glass plates...\n[00:10] Take a sip of hot caramel vibe.\n[00:35] Relax your mind, AI is syncing with your heartbeat.\n[01:00] Close your eyes, let the midnight lofi hold you.",
                    streamingUrl = "jfKfPfyJRdk"
                ),
                // HINDI
                Track(
                    id = "hi_1",
                    title = "Gully Power Cyberhip",
                    artist = "D-Divine Hustler",
                    album = "Slumdog Neon",
                    durationSeconds = 205,
                    imageUrl = "https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=300",
                    language = "Hindi",
                    genre = "Hustle Rap",
                    mood = "energetic",
                    lyrics = "[00:00] Gully se neon lights tak!\n[00:15] Hum toh hip-hop mein thoda glow dalte hain\n[00:30] (Heavy fast Hindi lyrics) Apna time aagaya, digital revolution hai bhai!\n[01:00] Sunn ye vibe, sunn ye dhoom!",
                    streamingUrl = "T7VWe6Sskv0"
                ),
                Track(
                    id = "hi_2",
                    title = "Zindagi Ke Naye Rang",
                    artist = "Arijit Vibe AI",
                    album = "Mellow Skies",
                    durationSeconds = 280,
                    imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300",
                    language = "Hindi",
                    genre = "Love Acoustic",
                    mood = "happy",
                    lyrics = "[00:00] Dreamy acoustic plucking...\n[00:15] Zindagi milke behti hai, jaise ganga ki dhar...\n[00:45] Tere naino ka ye surma, neon banke chamke yaar.\n[01:25] Let us float into another beautiful sky.",
                    streamingUrl = "A-rX_b8a6-8"
                ),
                // JAPANESE
                Track(
                    id = "ja_1",
                    title = "Kawaii Sparkle Pop",
                    artist = "Miku & Cyber 9",
                    album = "Anime Aurora",
                    durationSeconds = 175,
                    imageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=300",
                    language = "Japanese",
                    genre = "J-Pop",
                    mood = "happy",
                    lyrics = "[00:00] Sparkly pixel noises!\n[00:12] Let's jump high, shiny starry world!\n[00:30] (Fast Japanese) Kira kira shita kono sekai de asobou yo!\n[00:55] High five! Vibe to the rhythm of Japan!",
                    streamingUrl = "3v5fO38g7-k"
                ),
                Track(
                    id = "ja_2",
                    title = "Tokyo Rain Drift",
                    artist = "DJ Shingo & Haru",
                    album = "Shibuya Midnight",
                    durationSeconds = 215,
                    imageUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=300",
                    language = "Japanese",
                    genre = "Future Bass",
                    mood = "chill",
                    lyrics = "[00:00] Ambient police car siren & raindrops in Shibuya...\n[00:20] Driving fast through illuminated Tokyo bridges.\n[00:50] Watashi no kokoro ga yurete iru, neon no hikari ni.\n[01:20] Savor this neon chill.",
                    streamingUrl = "p8f_K8_f9S8"
                ),
                // SPANISH
                Track(
                    id = "es_1",
                    title = "Fiesta de la Luna",
                    artist = "Bad Bunny AI & Rosalía Pulse",
                    album = "Mar y Neon",
                    durationSeconds = 190,
                    imageUrl = "https://images.unsplash.com/photo-1487180142328-054b783fc471?w=300",
                    language = "Spanish",
                    genre = "Reggaeton Neon",
                    mood = "party",
                    lyrics = "[00:00] (Heavy dembow beat dropping...)\n[00:10] ¡Baila, baila bajo la luna brillante!\n[00:30] Te gusta mi vibra, te gusta el calor digital.\n[01:05] Toda la noche bailando el ritmo del mañana.",
                    streamingUrl = "5m_J6rGv6u0"
                ),
                // TELUGU & MALAYALAM BONUS
                Track(
                    id = "te_1",
                    title = "Naatu Naatu Vibe",
                    artist = "M.M. Keeravani Tribe",
                    album = "RRR Recharged",
                    durationSeconds = 200,
                    imageUrl = "https://images.unsplash.com/photo-1482440308425-276ad0f28b19?w=300",
                    language = "Telugu",
                    genre = "Mass Dance",
                    mood = "energetic",
                    lyrics = "[00:00] Naatu Naatu Mass dance beats!\n[00:15] Step with your left leg, stomp with your right!\n[00:40] Telugu pride pulsing in cyber space.\n[01:20] Dance like your heart is a nuclear engine!",
                    streamingUrl = "OsU0CGZoV8E"
                ),
                Track(
                    id = "ml_1",
                    title = "Arike Malare Glow",
                    artist = "Vijay Yesudas Tribute",
                    album = "Sufi Symphony",
                    durationSeconds = 250,
                    imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=300",
                    language = "Malayalam",
                    genre = "Melody Ballad",
                    mood = "chill",
                    lyrics = "[00:00] Slow flute in the hills of Kerala...\n[00:18] Arike, pathiye padumoru pattu pol...\n[01:00] Lightly glowing in the emerald twilight.\n[01:45] Breathe in the serene mountain mist with Vibe.",
                    streamingUrl = "TupT0d0f0D8"
                )
            )
            dao.insertTracks(mockTracks)

            // Seed default playlists
            val playlistsSeed = listOf(
                Playlist("pl_1", "Cyber Sunset", "Futuristic chill and synthwave tunes to watch the stars collide.", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300"),
                Playlist("pl_2", "AI Vibe Curated", "The recommendations engine picked these just for you based on current mood.", "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300"),
                Playlist("pl_3", "Collaborative Room #104", "Active loop with Mudaseer, Mukundha and others.", "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=300", true, 4)
            )
            for (p in playlistsSeed) {
                dao.insertPlaylist(p)
                // Add a few tracks to playlist
                dao.insertPlaylistTrack(PlaylistTrack(p.id, "ta_1"))
                if (p.id == "pl_1") {
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "en_1"))
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "ja_2"))
                } else if (p.id == "pl_2") {
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "ta_2"))
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "ko_2"))
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "hi_2"))
                } else {
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "ko_1"))
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "hi_1"))
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "es_1"))
                    dao.insertPlaylistTrack(PlaylistTrack(p.id, "te_1"))
                }
            }

            // Seed recently played
            dao.insertRecentlyPlayed(RecentlyPlayed("ta_2"))
            dao.insertRecentlyPlayed(RecentlyPlayed("en_2"))
            dao.insertRecentlyPlayed(RecentlyPlayed("ja_2"))
        }
    }
}
