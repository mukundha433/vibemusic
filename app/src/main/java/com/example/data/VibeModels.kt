package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val imageUrl: String,
    val language: String,
    val genre: String,
    val mood: String, // "energetic", "sad", "chill", "happy", "party"
    val lyrics: String, // String of timestamps and lyrics: "00:05:Intro\n00:15:Yeah Vibe let it play" etc.
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val streamingUrl: String = ""
)

@Entity(tableName = "recently_played")
data class RecentlyPlayed(
    @PrimaryKey val trackId: String,
    val playedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val imageUrl: String = "",
    val isCollaborative: Boolean = false,
    val userCount: Int = 1
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrack(
    val playlistId: String,
    val trackId: String
)

@Dao
interface VibeDao {
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: String): Track?

    @Query("SELECT * FROM tracks WHERE language = :language")
    fun getTracksByLanguage(language: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE genre = :genre")
    fun getTracksByGenre(genre: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE mood = :mood")
    fun getTracksByMood(mood: String): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<Track>)

    @Update
    suspend fun updateTrack(track: Track)

    // Recently played queries
    @Query("SELECT t.* FROM tracks t INNER JOIN recently_played r ON t.id = r.trackId ORDER BY r.playedAt DESC LIMIT 20")
    fun getRecentlyPlayedTracks(): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(recent: RecentlyPlayed)

    // Playlists queries
    @Query("SELECT * FROM playlists")
    fun getPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrack)

    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON t.id = pt.trackId WHERE pt.playlistId = :playlistId")
    fun getTracksForPlaylist(playlistId: String): Flow<List<Track>>
}

@Database(entities = [Track::class, RecentlyPlayed::class, Playlist::class, PlaylistTrack::class], version = 1, exportSchema = false)
abstract class VibeDatabase : RoomDatabase() {
    abstract fun dao(): VibeDao
}
