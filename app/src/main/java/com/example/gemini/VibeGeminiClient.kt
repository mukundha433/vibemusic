package com.example.gemini

import android.util.Log
import com.example.BuildConfig
import com.example.data.Track
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: okhttp3.RequestBody
    ): ResponseBody
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .build()
            .create(GeminiApiService::class.java)
    }
}

// Structured output representing the voice AI's understanding
data class VibeAssistantResult(
    val replyText: String,
    val actionCommand: String, // "PLAY_TRACK", "PLAY_MOOD", "PLAY_LANGUAGE", "INVITE_COLLAB", "SHOW_LYRICS", "GENERATE_PLAYLIST", "TALK"
    val actionTargetValue: String // ID or search value
)

class VibeGeminiClient {

    suspend fun processAssistantVoiceQuery(
        query: String,
        availableTracks: List<Track>
    ): VibeAssistantResult {
        // Build descriptive metadata for Gemini to make decisions
        val trackTextList = availableTracks.joinToString("\n") { 
            "ID: ${it.id} | Title: '${it.title}' | Artist: '${it.artist}' | Language: '${it.language}' | Genre: '${it.genre}' | Mood: '${it.mood}'"
        }

        val systemPrompt = """
            You are "Hey Vibe", a futuristic, sleek, friendly AI DJ and host for Vibe Music Streaming.
            Your job is to understand the user's audio request (transcribed here as text) and return a structured JSON response indicating how to respond and what action to execute.
            Take into account multilingual references, youth slang ("marana mass", "vibey", "sad boy hours", "korean hype", "reggaeton madness", "despacito mood", "shibuya vibes").
            
            Based on the user's prompt, parse the appropriate command:
            - "PLAY_TRACK" if they want a specific song. (set target id e.g. "ta_1")
            - "PLAY_MOOD" if they are feeling "sad", "energetic", "chill", "happy", "party". (set target value to mood name)
            - "PLAY_LANGUAGE" if they ask for a world language filter like "Tamil", "Korean", "English", "Hindi", "Japanese", "Spanish", "Telugu", "Malayalam".
            - "INVITE_COLLAB" if they want to collaborate with someone (e.g. "Collab with Mudaseer"). Set target value to collaborator's name.
            - "GENERATE_PLAYLIST" if they request an AI-generated playlist (e.g., "make a starry night playlist").
            - "TALK" if they are just having a conversation or asking a question about music.
            
            Return raw JSON matching this schema precisely:
            {
               "replyText": "The conversational reply spoken with high charisma as 'Hey Vibe' (max 2 sentences)",
               "actionCommand": "PLAY_TRACK" | "PLAY_MOOD" | "PLAY_LANGUAGE" | "INVITE_COLLAB" | "GENERATE_PLAYLIST" | "TALK",
               "actionTargetValue": "The matching ID, genre, mood, language, or person's name"
            }
            
            Available Track Database in Vibe App:
            ${trackTextList.replace("\"", "\\\"")}
        """.trimIndent()

        val jsonSchemaString = """
        {
          "type": "OBJECT",
          "properties": {
            "replyText": {
              "type": "STRING",
              "description": "A conversational response spoken with high charisma as 'Hey Vibe'"
            },
            "actionCommand": {
              "type": "STRING",
              "description": "One of: PLAY_TRACK, PLAY_MOOD, PLAY_LANGUAGE, INVITE_COLLAB, GENERATE_PLAYLIST, TALK"
            },
            "actionTargetValue": {
              "type": "STRING",
              "description": "The evaluated ID, mood string, language name, playlist prompt, or name of a collaborator."
            }
          },
          "required": ["replyText", "actionCommand", "actionTargetValue"]
        }
        """.trimIndent()

        val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val escapedSystemPrompt = systemPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

        val rawRequestJson = """
        {
          "contents": [
            {
              "parts": [
                { "text": "User Voice Command: \"$escapedQuery\"" }
              ]
            }
          ],
          "generationConfig": {
            "responseMimeType": "application/json",
            "responseSchema": $jsonSchemaString,
            "temperature": 0.4
          },
          "systemInstruction": {
            "parts": [
              { "text": "$escapedSystemPrompt" }
            ]
          }
        }
        """.trimIndent()

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("VibeGeminiClient", "API key missing or placeholder. Running offline fallback rule.")
            return runOfflineFallback(query, availableTracks)
        }

        return try {
            val body = rawRequestJson.toRequestBody("application/json".toMediaType())
            val response = RetrofitClient.service.generateContent(apiKey, body)
            val responseString = response.string()
            
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentNode = firstCandidate?.optJSONObject("content")
            val parts = contentNode?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val jsonText = firstPart?.optString("text") ?: ""

            val jsonObj = JSONObject(jsonText)
            val replyText = jsonObj.optString("replyText", "Let's tune in together!")
            val actionCommand = jsonObj.optString("actionCommand", "TALK")
            val actionTargetValue = jsonObj.optString("actionTargetValue", "")

            VibeAssistantResult(replyText, actionCommand, actionTargetValue)
        } catch (e: Exception) {
            Log.e("VibeGeminiClient", "Gemini error: ${e.message}", e)
            runOfflineFallback(query, availableTracks)
        }
    }

    private fun runOfflineFallback(query: String, availableTracks: List<Track>): VibeAssistantResult {
        val q = query.lowercase()
        return when {
            q.contains("marana") || q.contains("mass") -> {
                VibeAssistantResult(
                    replyText = "Boom! Loading up the heavy Tamil bass with Marana Mass Rave! Let's get energetic!",
                    actionCommand = "PLAY_TRACK",
                    actionTargetValue = "ta_1"
                )
            }
            q.contains("sad tamil") || q.contains("kannazhaga") -> {
                VibeAssistantResult(
                    replyText = "I feel you. Playing some emotional Tamil melodies with 'Kannazhaga Tears'... pull up a seat.",
                    actionCommand = "PLAY_TRACK",
                    actionTargetValue = "ta_3"
                )
            }
            q.contains("sad") -> {
                VibeAssistantResult(
                    replyText = "Let's lean back with some nostalgic sad tracks.",
                    actionCommand = "PLAY_MOOD",
                    actionTargetValue = "sad"
                )
            }
            q.contains("korean") || q.contains("rap") -> {
                VibeAssistantResult(
                    replyText = "Seoul lights activated! Loading K-Cyberpunk: Seoul Blackout Rap!",
                    actionCommand = "PLAY_TRACK",
                    actionTargetValue = "ko_1"
                )
            }
            q.contains("collab") || q.contains("mudaseer") -> {
                VibeAssistantResult(
                    replyText = "Collab request broadcasted instantly! Opening collaborative listening room with Mudaseer.",
                    actionCommand = "INVITE_COLLAB",
                    actionTargetValue = "Mudaseer"
                )
            }
            q.contains("playlist") || q.contains("generate") -> {
                VibeAssistantResult(
                    replyText = "Synthesizing a personalized AI-generated playlist based on your prompt: '$query'. Let the algorithm flow!",
                    actionCommand = "GENERATE_PLAYLIST",
                    actionTargetValue = query
                )
            }
            q.contains("spanish") || q.contains("reggaeton") || q.contains("fiesta") -> {
                VibeAssistantResult(
                    replyText = "¡De una! Dropping high energy Latin vibes with Fiesta de la Luna!",
                    actionCommand = "PLAY_TRACK",
                    actionTargetValue = "es_1"
                )
            }
            else -> {
                // Fuzzy match track list
                val matchedTrack = availableTracks.firstOrNull { 
                    q.contains(it.title.lowercase()) || q.contains(it.artist.lowercase()) 
                }
                if (matchedTrack != null) {
                    VibeAssistantResult(
                        replyText = "Found it! Opening '${matchedTrack.title}' by ${matchedTrack.artist}.",
                        actionCommand = "PLAY_TRACK",
                        actionTargetValue = matchedTrack.id
                    )
                } else {
                    VibeAssistantResult(
                        replyText = "I'm on it! Curating a supreme custom soundscape matching '$query'. Tune in!",
                        actionCommand = "PLAY_MOOD",
                        actionTargetValue = "energetic"
                    )
                }
            }
        }
    }
}
