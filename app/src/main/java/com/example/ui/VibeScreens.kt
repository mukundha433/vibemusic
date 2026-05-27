package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.Track
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Screen Enumeration representation
enum class VibeScreen {
    ONBOARDING,
    LOGIN,
    HOME,
    SEARCH,
    COLLAB_ROOM,
    LIBRARY,
    PLAYER,
    ARTIST_PROFILE,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeAppFrame(
    viewModel: VibeViewModel,
    isDarkTheme: MutableState<Boolean>
) {
    var currentScreen by remember { mutableStateOf(VibeScreen.ONBOARDING) }
    val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val activeCollabCode by viewModel.activeCollabRoomCode.collectAsStateWithLifecycle()
    val progressSecs by viewModel.progressSecs.collectAsStateWithLifecycle()

    // YouTube Player Initialization
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var activePlayer by remember { mutableStateOf<com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer?>(null) }

    // YouTube Player is initialized dynamically only when playing interactive video tracks to avoid Webview crash on startup


    var lastLoadedVideoId by remember { mutableStateOf("") }

    // Reset lastLoadedVideoId when the player restarts or updates to force reload
    LaunchedEffect(activePlayer) {
        lastLoadedVideoId = ""
    }

    // Load or playback controller synced cleanly with queue changes and user state actions
    LaunchedEffect(currentTrack, isPlaying, activePlayer) {
        val player = activePlayer ?: return@LaunchedEffect
        val track = currentTrack
        if (track != null && track.streamingUrl.isNotEmpty()) {
            if (isPlaying) {
                if (lastLoadedVideoId != track.streamingUrl) {
                    player.loadVideo(track.streamingUrl, progressSecs.toFloat())
                    lastLoadedVideoId = track.streamingUrl
                } else {
                    player.play()
                }
            } else {
                player.pause()
            }
        } else {
            player.pause()
        }
    }

    // Collect manual viewmodel seek requests
    LaunchedEffect(activePlayer) {
        viewModel.seekRequest.collect { seekSecs ->
            activePlayer?.seekTo(seekSecs)
        }
    }

    // Sync Volume Changes
    val volumeStateVal by viewModel.volume.collectAsStateWithLifecycle()
    LaunchedEffect(activePlayer, volumeStateVal) {
        val player = activePlayer ?: return@LaunchedEffect
        player.setVolume(volumeStateVal)
    }

    // Artist navigation info state
    var selectedArtistName by remember { mutableStateOf("") }
    var selectedArtistGenre by remember { mutableStateOf("") }
    var selectedArtistLang by remember { mutableStateOf("") }

    // Floating Hey Vibe Voice Assistant screen visibility
    var showVoiceAssistantOverlay by remember { mutableStateOf(false) }

    // Redirect to login if logged out
    LaunchedEffect(currentUserEmail) {
        if (currentUserEmail == null) {
            currentScreen = VibeScreen.LOGIN
        } else if (currentScreen == VibeScreen.ONBOARDING || currentScreen == VibeScreen.LOGIN) {
            currentScreen = VibeScreen.HOME
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != VibeScreen.ONBOARDING && currentScreen != VibeScreen.LOGIN && currentScreen != VibeScreen.PLAYER) {
                VibeBottomNavigation(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> currentScreen = screen },
                    activeRoomCode = activeCollabCode
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Neon Aurora Background drawing (only on Dark Mode for full aesthetic pop)
            if (isDarkTheme.value) {
                VibeNeonBackground()
            }

            // Main body selector
            when (currentScreen) {
                VibeScreen.ONBOARDING -> VibeOnboardingScreen(onGetStarted = { currentScreen = VibeScreen.LOGIN })
                VibeScreen.LOGIN -> VibeLoginScreen(viewModel = viewModel, onLoginSuccess = { currentScreen = VibeScreen.HOME })
                VibeScreen.HOME -> VibeHomeScreen(
                    viewModel = viewModel,
                    onNavigateToArtist = { artist, genre, lang ->
                        selectedArtistName = artist
                        selectedArtistGenre = genre
                        selectedArtistLang = lang
                        currentScreen = VibeScreen.ARTIST_PROFILE
                    },
                    onNavigateToCollab = { currentScreen = VibeScreen.COLLAB_ROOM },
                    onNavigateToPlayer = { currentScreen = VibeScreen.PLAYER }
                )
                VibeScreen.SEARCH -> VibeSearchScreen(
                    viewModel = viewModel,
                    onNavigateToArtist = { artist, genre, lang ->
                        selectedArtistName = artist
                        selectedArtistGenre = genre
                        selectedArtistLang = lang
                        currentScreen = VibeScreen.ARTIST_PROFILE
                    }
                )
                VibeScreen.COLLAB_ROOM -> VibeCollabRoomScreen(viewModel = viewModel)
                VibeScreen.LIBRARY -> VibeLibraryScreen(viewModel = viewModel, onNavigateToCollab = { currentScreen = VibeScreen.COLLAB_ROOM })
                VibeScreen.PLAYER -> VibePlayerScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = VibeScreen.HOME }
                )
                VibeScreen.ARTIST_PROFILE -> VibeArtistProfileScreen(
                    viewModel = viewModel,
                    artistName = selectedArtistName,
                    genre = selectedArtistGenre,
                    language = selectedArtistLang,
                    onBack = { currentScreen = VibeScreen.HOME }
                )
                VibeScreen.SETTINGS -> VibeSettingsScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme
                )
            }

            // High-fidelity Floating Player Bar (always visible if music is loaded, except in Onboarding, Login, or full Player Screens)
            if (currentScreen != VibeScreen.ONBOARDING && currentScreen != VibeScreen.LOGIN && currentScreen != VibeScreen.PLAYER) {
                currentTrack?.let { track ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
                    ) {
                        VibeMiniFloatingPlayer(
                            track = track,
                            isPlaying = isPlaying,
                            progressSecs = progressSecs,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onOpenFullPlayer = { currentScreen = VibeScreen.PLAYER }
                        )
                    }
                }
            }

            // Hey Vibe voice assistance triggering badge
            if (currentScreen != VibeScreen.ONBOARDING && currentScreen != VibeScreen.LOGIN) {
                AnimatedVisibility(
                    visible = !showVoiceAssistantOverlay,
                    enter = scaleIn(),
                    exit = scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 85.dp, end = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showVoiceAssistantOverlay = true },
                        containerColor = NeonPink,
                        contentColor = Color.White,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("floating_hey_vibe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Hey Vibe Voice Assistant Trigger",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Fully Animated "Hey Vibe" Voice Assistant Overlay screen
            AnimatedVisibility(
                visible = showVoiceAssistantOverlay,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.zIndex(100f)
            ) {
                VibeVoiceAssistantOverlay(
                    viewModel = viewModel,
                    onClose = { showVoiceAssistantOverlay = false }
                )
            }

            // Master persistent YouTube Player view to preserve/display audio streaming video stage safely
            // Deferred initialization starts when the track is playing, or on player screen, or active state exists.
            if (currentTrack?.streamingUrl?.isNotEmpty() == true && (isPlaying || currentScreen == VibeScreen.PLAYER || activePlayer != null)) {
                val isPlayerScreen = (currentScreen == VibeScreen.PLAYER)
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView(ctx).apply {
                            lifecycleOwner.lifecycle.addObserver(this)
                            addYouTubePlayerListener(object : com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer) {
                                    activePlayer = youTubePlayer
                                }

                                override fun onCurrentSecond(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer, second: Float) {
                                    viewModel.updatePlaybackProgress(second.toInt())
                                }

                                override fun onVideoDuration(youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer, duration: Float) {
                                    viewModel.updateDuration(duration.toInt())
                                }

                                override fun onStateChange(
                                    youTubePlayer: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer,
                                    state: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState
                                ) {
                                    if (state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.ENDED) {
                                        viewModel.onTrackEnded()
                                    }
                                }
                            })
                        }
                    },
                    modifier = if (isPlayerScreen) {
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 96.dp, start = 24.dp, end = 24.dp)
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    } else {
                        // Keep alive at 1.dp size with low alpha so OS/JS doesn't stop background playback
                        Modifier
                            .size(1.dp)
                            .alpha(0.01f)
                    },
                    onRelease = { youtubeView ->
                        lifecycleOwner.lifecycle.removeObserver(youtubeView)
                        youtubeView.release()
                        activePlayer = null
                    }
                )
            }
        }
    }
}

@Composable
fun VibeNeonBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "Neon Aura movement")
    val translationX by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x offset"
    )
    val translationY by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDarkBG)
            .drawBehind {
                drawRect(color = CosmicDarkBG)
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw neon purple flare
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPurple.copy(alpha = 0.45f), Color.Transparent),
                    radius = 800f
                ),
                center = center.copy(
                    x = center.x + translationX,
                    y = center.y + translationY
                )
            )
            // Draw neon cyan counterpoint glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.35f), Color.Transparent),
                    radius = 900f
                ),
                center = center.copy(
                    x = center.x - translationX,
                    y = center.y - translationY
                )
            )
            // Draw neon pink focal point glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPink.copy(alpha = 0.25f), Color.Transparent),
                    radius = 600f
                ),
                center = center.copy(
                    x = center.x + (translationX * 0.5f),
                    y = center.y + (translationY * -0.7f)
                )
            )
        }
    }
}

@Composable
fun VibeBottomNavigation(
    currentScreen: VibeScreen,
    onNavigate: (VibeScreen) -> Unit,
    activeRoomCode: String?
) {
    NavigationBar(
        containerColor = SleekDarkBG,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier
            .background(Color.Transparent)
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
    ) {
        NavigationBarItem(
            selected = currentScreen == VibeScreen.HOME,
            onClick = { onNavigate(VibeScreen.HOME) },
            icon = { Icon(if (currentScreen == VibeScreen.HOME) Icons.Default.Home else Icons.Outlined.Home, "Home") },
            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SleekPurple,
                selectedTextColor = SleekPurple,
                indicatorColor = SleekPurple.copy(alpha = 0.15f),
                unselectedIconColor = SleekTextMuted,
                unselectedTextColor = SleekTextMuted
            ),
            modifier = Modifier.testTag("nav_home_tab")
        )
        NavigationBarItem(
            selected = currentScreen == VibeScreen.SEARCH,
            onClick = { onNavigate(VibeScreen.SEARCH) },
            icon = { Icon(Icons.Default.Search, "Search") },
            label = { Text("Explore", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SleekPink,
                selectedTextColor = SleekPink,
                indicatorColor = SleekPink.copy(alpha = 0.15f),
                unselectedIconColor = SleekTextMuted,
                unselectedTextColor = SleekTextMuted
            ),
            modifier = Modifier.testTag("nav_search_tab")
        )
        NavigationBarItem(
            selected = currentScreen == VibeScreen.COLLAB_ROOM,
            onClick = { onNavigate(VibeScreen.COLLAB_ROOM) },
            icon = {
                BadgedBox(
                    badge = {
                        if (activeRoomCode != null) {
                            Badge(containerColor = SleekPink) { Text("LIVE", color = Color.White) }
                        }
                    }
                ) {
                    Icon(if (currentScreen == VibeScreen.COLLAB_ROOM) Icons.Default.Group else Icons.Outlined.Group, "Collab Session")
                }
            },
            label = { Text("Group", fontSize = 11.sp, fontWeight = FontWeight.Bold, overflow = TextOverflow.Ellipsis, maxLines = 1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SleekPurple,
                selectedTextColor = SleekPurple,
                indicatorColor = SleekPurple.copy(alpha = 0.15f),
                unselectedIconColor = SleekTextMuted,
                unselectedTextColor = SleekTextMuted
            ),
            modifier = Modifier.testTag("nav_collab_tab")
        )
        NavigationBarItem(
            selected = currentScreen == VibeScreen.LIBRARY,
            onClick = { onNavigate(VibeScreen.LIBRARY) },
            icon = { Icon(if (currentScreen == VibeScreen.LIBRARY) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List, "Library") },
            label = { Text("Library", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SleekPurple,
                selectedTextColor = SleekPurple,
                indicatorColor = SleekPurple.copy(alpha = 0.15f),
                unselectedIconColor = SleekTextMuted,
                unselectedTextColor = SleekTextMuted
            ),
            modifier = Modifier.testTag("nav_library_tab")
        )
        NavigationBarItem(
            selected = currentScreen == VibeScreen.SETTINGS,
            onClick = { onNavigate(VibeScreen.SETTINGS) },
            icon = { Icon(if (currentScreen == VibeScreen.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings, "Settings") },
            label = { Text("Vibe VIP", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CustomGlowGold,
                selectedTextColor = CustomGlowGold,
                indicatorColor = CustomGlowGold.copy(alpha = 0.15f),
                unselectedIconColor = SleekTextMuted,
                unselectedTextColor = SleekTextMuted
            ),
            modifier = Modifier.testTag("nav_settings_tab")
        )
    }
}

// ONBOARDING PAGE
@Composable
fun VibeOnboardingScreen(onGetStarted: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse logo glow")
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Logo bounce scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDarkBG)
    ) {
        // Glowing Background Visual Accent
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeonPink.copy(alpha = 0.35f), Color.Transparent),
                    radius = 700f
                ),
                center = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Big Futuristic Glowing Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(bounceScale)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.sweepGradient(listOf(NeonPink, NeonCyan, NeonPurple, NeonPink)),
                            shape = CircleShape
                        )
                        .padding(6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CosmicDarkBG, shape = CircleShape)
                    ) {
                        // V custom neon visual glyph representation
                        Text(
                            text = "V",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 80.sp,
                                brush = Brush.linearGradient(listOf(NeonPink, NeonCyan))
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "VIBE",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 8.sp,
                        color = Color.White
                    )
                )
                Text(
                    text = "AI-POWERED SOUNDSCAPE SYSTEM",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Description block & Action Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "The audio platform built for the next generation.\nIntelligent AI, worldwide soundwaves, live party sync.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextSecondaryLight,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { onGetStarted() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(listOf(NeonPink, NeonCyan)),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .testTag("get_started_onboarding_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(listOf(NeonPink.copy(alpha = 0.2f), NeonCyan.copy(alpha = 0.1f)))
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "ENTER THE PULSE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 3.sp,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = NeonCyan)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// LOGIN & SIGNUP SCREEN
@Composable
fun VibeLoginScreen(
    viewModel: VibeViewModel,
    onLoginSuccess: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isSigningUp by remember { mutableStateOf(false) }

    var otpRequested by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }

    val contentColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tiny branding logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(brush = Brush.linearGradient(listOf(NeonPink, NeonPurple)), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "VIBE SECURE ACCESS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                )
            }

            Text(
                text = if (isSigningUp) "Create Account" else "Tune in to Vibe",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )

            Text(
                text = "Authenticate securely to sync your tracks across devices.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryLight),
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // OTP Field conditional display
            if (!otpRequested) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address / Mobile Number", color = TextSecondaryLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CosmicSlateGrey,
                        focusedLabelColor = NeonCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("username_input")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Secure PIN / Password", color = TextSecondaryLight) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        unfocusedBorderColor = CosmicSlateGrey,
                        focusedLabelColor = NeonPink,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("password_input")
                )
            } else {
                Text(
                    text = "A secure verification code has been dispatched to $emailInput. Enter code below:",
                    style = MaterialTheme.typography.bodySmall.copy(color = NeonCyan),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { otpInput = it },
                    label = { Text("6-Digit Verification Code", color = TextSecondaryLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CustomGlowGold,
                        unfocusedBorderColor = CosmicSlateGrey,
                        focusedLabelColor = CustomGlowGold,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (otpInput.length == 6) {
                            viewModel.executeLogin(emailInput)
                            onLoginSuccess()
                        }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .testTag("otp_input")
                )
            }

            // Main CTA submit button
            Button(
                onClick = {
                    if (emailInput.isNotBlank()) {
                        if (!otpRequested && (emailInput.all { it.isDigit() } || emailInput.contains("otp"))) {
                            otpRequested = true
                        } else {
                            viewModel.executeLogin(emailInput)
                            onLoginSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_button")
            ) {
                Text(
                    text = if (otpRequested) "VERIFY & GO" else if (isSigningUp) "CREATE METRIC PROFILE" else "ACCESS PLATFORM",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.White)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "OR CONNECT WITH CORE SECURE ID",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryLight, letterSpacing = 1.sp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Social Logins Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Google
                IconButton(
                    onClick = {
                        viewModel.executeLogin("google.user@gmail.com")
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .background(GlassCardBG, shape = CircleShape)
                        .size(54.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = "Google authentication", tint = NeonCyan)
                }
                // Apple
                IconButton(
                    onClick = {
                        viewModel.executeLogin("apple.core@icloud.com")
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .background(GlassCardBG, shape = CircleShape)
                        .size(54.dp)
                ) {
                    Icon(Icons.Default.PhoneIphone, contentDescription = "Apple authentication", tint = Color.White)
                }
                // Secure RFID/Wearable pairing
                IconButton(
                    onClick = {
                        viewModel.executeLogin("wearable.metrics@vibe.com")
                        onLoginSuccess()
                    },
                    modifier = Modifier
                        .background(GlassCardBG, shape = CircleShape)
                        .size(54.dp)
                ) {
                    Icon(Icons.Default.CastConnected, contentDescription = "NFC Ring or Wearable login", tint = CustomGlowGold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            TextButton(onClick = { isSigningUp = !isSigningUp }) {
                Text(
                    text = if (isSigningUp) "Already have an account? Access here" else "New to Vibe? Create sound credentials here",
                    color = NeonCyan,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// HOME PAGE
@Composable
fun VibeHomeScreen(
    viewModel: VibeViewModel,
    onNavigateToArtist: (String, String, String) -> Unit,
    onNavigateToCollab: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()

    var activeLanguageSelected by remember { mutableStateOf("All") }
    val displayLanguages = listOf("All", "Tamil", "English", "Korean", "Hindi", "Japanese", "Spanish", "Telugu", "Malayalam")

    // Dynamic Filtered list based on languages
    val tracksToShow = remember(allTracks, activeLanguageSelected) {
        if (activeLanguageSelected == "All") allTracks 
        else allTracks.filter { it.language.equals(activeLanguageSelected, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_scroll"),
        contentPadding = PaddingValues(bottom = 120.dp, top = 20.dp)
    ) {
        // Sleek Web Header Component
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Branded Sleek V Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(SleekPurple, SleekPink)),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = "V",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                    Text(
                        text = "Vibe",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            brush = Brush.linearGradient(listOf(Color.White, SleekTextMuted))
                        )
                    )
                }

                // Header Search & Profile Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search icon circle button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Profile avatar image container
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(2.dp, SleekPurple, CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = "https://api.dicebear.com/7.x/avataaars/svg?seed=Alex",
                            contentDescription = "Profile Pic",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Greetings Intro Section
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Good evening, Alex",
                    style = MaterialTheme.typography.bodyMedium.copy(color = SleekTextMuted, fontWeight = FontWeight.Medium)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ready for your",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                    Text(
                         text = "Mass",
                         style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = SleekPurple)
                    )
                    Text(
                        text = "vibe?",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                // AI Curated Premium Recommendations High-fidelity Canvas/Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(SleekCardBG)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), shape = RoundedCornerShape(32.dp))
                        .clickable { onNavigateToCollab() }
                ) {
                    // Accent blur background decor circle glow
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(SleekPurple.copy(alpha = 0.15f), Color.Transparent),
                                radius = 300f
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width, size.height)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top row tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(50))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "AI RECOMMENDATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = SleekLightPurple
                                )
                            }
                            
                            // Pulse Live badge
                            Box(
                                modifier = Modifier
                                    .background(Color.Transparent, RoundedCornerShape(50))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF10B981), CircleShape)
                                    )
                                    Text(
                                        text = "LIVE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Bottom info row
                        Column {
                            Text(
                                text = "Midnight\nSynapse",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 32.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Curated based on your late-night focus",
                                style = MaterialTheme.typography.bodySmall.copy(color = SleekTextMuted)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Circular white play button
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color.White, CircleShape)
                                            .clickable { 
                                                // Trigger AI recommendation play
                                                if (viewModel.allTracks.value.isNotEmpty()) {
                                                    val randomTrack = viewModel.allTracks.value.first()
                                                    viewModel.selectTrack(randomTrack.id, viewModel.allTracks.value)
                                                    onNavigateToPlayer()
                                                }
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play Recommendations",
                                            tint = Color.Black,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    Column {
                                        Text(
                                            text = "COLLAB MODE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekPurple
                                            )
                                        )
                                        Text(
                                            text = "Mudaseer + 2 others listening",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language Filter selection pills
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayLanguages) { lang ->
                    val isSelected = activeLanguageSelected == lang
                    val bgStyle = if (isSelected) NeonPink else GlassCardBG
                    val textStyleColor = if (isSelected) Color.White else TextSecondaryLight
                    val borderStroke = if (isSelected) BorderStroke(0.dp, Color.Transparent) else BorderStroke(0.5.dp, CosmicSlateGrey)

                    Box(
                        modifier = Modifier
                            .background(bgStyle, shape = RoundedCornerShape(20.dp))
                            .border(borderStroke, shape = RoundedCornerShape(20.dp))
                            .clickable { activeLanguageSelected = lang }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("lang_pill_$lang")
                    ) {
                        Text(lang, color = textStyleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Filtered Tracks / Music section
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeLanguageSelected == "All") "AI Recommendations Feed" else "Hottest $activeLanguageSelected Waves",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                    )
                    Text("VIEW ALL", style = MaterialTheme.typography.labelMedium.copy(color = NeonCyan, letterSpacing = 1.sp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracksToShow) { track ->
                        VibeTrackCard(
                            track = track,
                            onClick = { 
                                viewModel.selectTrack(track.id, tracksToShow)
                                onNavigateToPlayer()
                            },
                            onSecondaryClick = { onNavigateToArtist(track.artist, track.genre, track.language) }
                        )
                    }
                }
            }
        }

        // Recently Played list section
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 28.dp)) {
                    Text(
                        text = "Recently Resonated",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyPlayed) { track ->
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .clickable { viewModel.selectTrack(track.id, recentlyPlayed) },
                                colors = CardDefaults.cardColors(containerColor = GlassCardBG),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = track.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(track.artist, color = TextSecondaryLight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Playlists / User-generated Mix segment
        item {
            Column(modifier = Modifier.padding(top = 28.dp)) {
                Text(
                    text = "Vibe Dynamic Folders",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playlists) { p ->
                        Column(
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { 
                                    // Trigger queue from playlist
                                    viewModel.startCollabRoomSession()
                                    onNavigateToCollab()
                                }
                        ) {
                            Box {
                                AsyncImage(
                                    model = p.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(135.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (p.isCollaborative) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .background(NeonPink, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("LIVE COLLAB", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(p.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                            Text(p.description, color = TextSecondaryLight, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Social Collab Banner (Sleek Theme feature matches HTML mockup)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .clickable { onNavigateToCollab() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekPurple.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, SleekPurple.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray)
                            ) {
                                AsyncImage(
                                    model = "https://api.dicebear.com/7.x/avataaars/svg?seed=Mudaseer",
                                    contentDescription = "Mudaseer Avatar",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Cute green online dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                                    .border(1.5.dp, SleekDarkBG, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        
                        Column {
                            Text(
                                "Mudaseer invited you",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            Text(
                                "Join 'Indie Soul' Session",
                                style = MaterialTheme.typography.bodySmall.copy(color = SleekTextMuted)
                            )
                        }
                    }
                    
                    Button(
                        onClick = { onNavigateToCollab() },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPurple),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            "JOIN LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Podcasts & Trending Charts slot
        item {
            Column(modifier = Modifier.padding(top = 32.dp, start = 20.dp, end = 20.dp)) {
                Text(
                    "Trending Quantum Charts",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // High fidelity chart row list
                val chartItems = listOf(
                    Triple("1", "Shibuya Neon Highs in Japan", "DJ Shingo & Haru"),
                    Triple("2", "Marana Mass Club Anthem", "Anirudh Synth Vibe"),
                    Triple("3", "K-Cyberpunk Seoul Drill", "GD-Neon")
                )

                chartItems.forEach { (rank, title, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(GlassCardBG, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            rank,
                            color = NeonCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            modifier = Modifier.width(36.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = TextSecondaryLight, fontSize = 11.sp)
                        }
                        IconButton(onClick = { viewModel.togglePlayPause() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = NeonPink)
                        }
                    }
                }
            }
        }
    }
}

// BEAUTIFUL CHIPS/CARDS FOR GENERAL MUSIC TRACK ROWS
@Composable
fun VibeTrackCard(
    track: Track,
    onClick: () -> Unit,
    onSecondaryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .testTag("track_card_${track.id}")
    ) {
        Box(
            modifier = Modifier
                .height(160.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = track.imageUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Accent gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
            )

            // Small language stamp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(track.language, color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = TextSecondaryLight,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { onSecondaryClick() }
        )
    }
}

// SEARCH EXPLORE PAGE
@Composable
fun VibeSearchScreen(
    viewModel: VibeViewModel,
    onNavigateToArtist: (String, String, String) -> Unit
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val youtubeResults by viewModel.youtubeSearchResults.collectAsStateWithLifecycle()
    val isLoadingYt by viewModel.isLoadingYoutubeSearch.collectAsStateWithLifecycle()

    val filteredTracks = remember(allTracks, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allTracks.filter { 
            it.title.contains(searchQuery, true) ||
            it.artist.contains(searchQuery, true) ||
            it.genre.contains(searchQuery, true) ||
            it.mood.contains(searchQuery, true) ||
            it.lyrics.contains(searchQuery, true)
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(400)
            viewModel.searchYouTube(searchQuery)
        }
    }

    val moodCategories = listOf(
        Pair("Chill Hours", NeonCyan),
        Pair("High Energy Mass", NeonPink),
        Pair("Cyber Midnight", NeonPurple),
        Pair("Golden Acoustics", CustomGlowGold),
        Pair("Reggaeton Heat", Color(0xFFFF5722)),
        Pair("K-Ballads", Color(0xFFE91E63))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search songs, artists, mood, lyrics...", color = TextSecondaryLight) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryLight) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPink,
                unfocusedBorderColor = CosmicSlateGrey,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("search_text_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isBlank()) {
            // Display mood grid
            Text(
                "Search by Mood or Vibe",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(moodCategories) { (mood, color) ->
                    Box(
                        modifier = Modifier
                            .height(100.dp)
                            .background(
                                Brush.linearGradient(listOf(color.copy(alpha = 0.3f), CosmicSlateGrey)),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .clickable {
                                // Simulate query matching mood
                                val targetMood = when {
                                    mood.contains("Chill") -> "chill"
                                    mood.contains("Energy") -> "energetic"
                                    mood.contains("Midnight") -> "sad"
                                    else -> "happy"
                                }
                                viewModel.setSearchQuery(targetMood)
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            mood,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            modifier = Modifier.align(Alignment.BottomStart)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Local matches
                if (filteredTracks.isNotEmpty()) {
                    item {
                        Text(
                            "Library Matches (${filteredTracks.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonPink),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredTracks) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBG, RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectTrack(track.id, filteredTracks) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${track.artist} • ${track.language}", color = TextSecondaryLight, fontSize = 11.sp)
                            }
                            IconButton(onClick = { onNavigateToArtist(track.artist, track.genre, track.language) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "View Artist", tint = NeonCyan)
                            }
                        }
                    }
                }

                // YouTube Streaming Matches
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "YouTube Music Streams",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NeonCyan),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (isLoadingYt) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonCyan,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                if (youtubeResults.isNotEmpty()) {
                    items(youtubeResults) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBG.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(1.dp, NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectTrack(track.id) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // YouTube visual card icon styling
                            Box(modifier = Modifier.size(50.dp)) {
                                AsyncImage(
                                    model = track.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                // Overlaid Play Symbol for YouTube
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Stream on Youtube",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${track.artist} • Live YouTube Stream",
                                    color = TextSecondaryLight,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "YouTube stream badge",
                                tint = NeonCyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp).padding(end = 4.dp)
                            )
                        }
                    }
                } else if (!isLoadingYt) {
                    item {
                        Text(
                            "Type in search box above to sync streams from YouTube Database.",
                            color = TextSecondaryLight,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                if (filteredTracks.isEmpty() && youtubeResults.isEmpty() && !isLoadingYt) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        ) {
                            Icon(Icons.Default.SentimentVeryDissatisfied, contentDescription = null, tint = TextSecondaryLight, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No tracks inside of Vibe match this wave.", color = TextSecondaryLight, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// REAL-TIME COLLABORATIVE Listening Session page
@Composable
fun VibeCollabRoomScreen(viewModel: VibeViewModel) {
    val activeRoomCode by viewModel.activeCollabRoomCode.collectAsStateWithLifecycle()
    val roomParticipants by viewModel.roomParticipants.collectAsStateWithLifecycle()
    val listMessages by viewModel.collabChatMessages.collectAsStateWithLifecycle()
    val floatingEmojis by viewModel.floatingEmojis.collectAsStateWithLifecycle()

    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isHostControlsOnly by viewModel.isHostControlsOnly.collectAsStateWithLifecycle()

    var chatTextInput by remember { mutableStateOf("") }

    if (activeRoomCode == null) {
        // Empty placeholder state for starting collab sessions
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Group, contentDescription = null, tint = NeonPink, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Listen Together in Real-Time",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Create a live session room to synchronize audio streams with up to 25 friends globally, edit queue lists collectively, and react to beats live.",
                style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondaryLight, lineHeight = 24.sp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { viewModel.startCollabRoomSession() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_collab_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("LAUNCH LIVE SYNC SESSION", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                }
            }
        }
    } else {
        // Active room UI layout with glassmorphism + reactive bubbles
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Connection header block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GlassCardBG, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Green, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Active Stereo Sync Link", fontSize = 11.sp, color = Color.Green, fontWeight = FontWeight.Bold)
                        }
                        Text(activeRoomCode!!, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = Color.White))
                    }
                    Button(
                        onClick = { viewModel.exitCollabSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LEAVE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mid part: Synchronized current track details & active members list
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Left: Synchronized audio widget
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = SolidCardBG),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("NOW STREAMING", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            currentTrack?.let { track ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = track.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(track.artist, color = TextSecondaryLight, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            } ?: Text("No audio loaded.", color = TextSecondaryLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { viewModel.playPreviousTrack() }) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                                }
                                IconButton(
                                    onClick = { viewModel.togglePlayPause() },
                                    modifier = Modifier.background(NeonPink, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "PlayPause",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { viewModel.playNextTrack() }) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Right: participants lists
                    Card(
                        modifier = Modifier
                            .weight(0.9f)
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassCardBG),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("PARTY MEMBERS (${roomParticipants.size})", color = NeonPink, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn {
                                items(roomParticipants) { member ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    brush = Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(member, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Chat Console logs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = GlassCardBG.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, CosmicSlateGrey)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("LIVE ROOM CHAT & PROGRESS LOG", color = TextSecondaryLight, fontSize = 10.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            reverseLayout = true
                        ) {
                            items(listMessages.reversed()) { msg ->
                                Column {
                                    Text(msg.sender, color = if (msg.sender == "You") NeonCyan else NeonPink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(msg.text, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }

                        // Chat Input field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = chatTextInput,
                                onValueChange = { chatTextInput = it },
                                placeholder = { Text("Group message...", fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPink,
                                    unfocusedBorderColor = CosmicSlateGrey,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (chatTextInput.isNotBlank()) {
                                        viewModel.sendCollabChatMessage(chatTextInput)
                                        chatTextInput = ""
                                    }
                                })
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (chatTextInput.isNotBlank()) {
                                        viewModel.sendCollabChatMessage(chatTextInput)
                                        chatTextInput = ""
                                    }
                                },
                                modifier = Modifier.background(NeonPink, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // padding for floating wave details
            }

            // Right side rise emoji reactions panel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 120.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("🔥", "❤️", "👏", "⚡", "🙌").forEach { emoji ->
                    IconButton(
                        onClick = { viewModel.emitEmojiReaction(emoji) },
                        modifier = Modifier
                            .background(GlassCardBG, CircleShape)
                            .size(45.dp)
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }

            // Rising floating emojis simulation loop
            floatingEmojis.forEachIndexed { idx, reaction ->
                上升EmojiReactionAnimation(reaction = reaction, index = idx)
            }
        }
    }
}

// Float animator simulation for collaborative emoji triggers
@Composable
fun 上升EmojiReactionAnimation(reaction: EmojiReaction, index: Int) {
    val animateX = remember { (100..300).random().toFloat() }
    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = true }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut()
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "rise")
        val riseY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -600f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Y rises"
        )

        Box(
            modifier = Modifier
                .offset(x = animateX.dp, y = riseY.dp + 500.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(reaction.emoji, fontSize = 28.sp)
                Text(reaction.senderName, color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// LIBRARY & PERSISTENCE PAGE (Lists offline downloads + created playlists)
@Composable
fun VibeLibraryScreen(
    viewModel: VibeViewModel,
    onNavigateToCollab: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsStateWithLifecycle()

    var activeTabSelection by remember { mutableStateOf("Playlists") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Sonic Treasury",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, color = Color.White),
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Custom segment bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassCardBG, RoundedCornerShape(20.dp))
                .padding(4.dp)
        ) {
            listOf("Playlists", "Offline Downloads").forEach { tab ->
                val isSelected = activeTabSelection == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) NeonPink else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { activeTabSelection = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tab, color = if (isSelected) Color.White else TextSecondaryLight, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (activeTabSelection == "Playlists") {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(playlists) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GlassCardBG, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.startCollabRoomSession()
                                onNavigateToCollab()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = playlist.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(playlist.description, color = TextSecondaryLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            // Offline Downloads display
            val downloadedTracks = allTracks.filter { it.isDownloaded }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Show items currently downloading
                if (downloadProgressMap.isNotEmpty()) {
                    item {
                        Text("Active Transmitting Waves:", color = NeonCyan, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(downloadProgressMap.toList()) { (trackId, progress) ->
                        val track = allTracks.firstOrNull { it.id == trackId }
                        track?.let { t ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SolidCardBG, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.title, color = Color.White, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress / 100f },
                                        color = NeonPink,
                                        trackColor = CosmicSlateGrey,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("$progress%", color = NeonPink, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                item {
                    Text(
                        "Stored Offline Offline (${downloadedTracks.size}):",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }

                if (downloadedTracks.isEmpty()) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(30.dp)) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TextSecondaryLight, modifier = Modifier.size(50.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No tracks downloaded. Tap download icons on music player pages.", color = TextSecondaryLight, textAlign = TextAlign.Center, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(downloadedTracks) { track ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GlassCardBG, RoundedCornerShape(12.dp))
                                .clickable { viewModel.selectTrack(track.id, downloadedTracks) }
                                .padding(12.dp)
                        ) {
                            AsyncImage(
                                model = track.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(track.artist, color = TextSecondaryLight, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.CheckCircle, contentDescription = "Downloaded Successfully", tint = Color.Green)
                        }
                    }
                }
            }
        }
    }
}

// MINI FLOATING AUDIO PLAYER FOOTER
@Composable
fun VibeMiniFloatingPlayer(
    track: Track,
    isPlaying: Boolean,
    progressSecs: Int,
    onTogglePlay: () -> Unit,
    onOpenFullPlayer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SleekItemBG.copy(alpha = 0.9f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), shape = RoundedCornerShape(16.dp))
            .clickable { onOpenFullPlayer() }
            .testTag("mini_player_surface")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Semi-transparent brand overlay matching HTML mockup
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SleekPurple.copy(alpha = 0.2f))
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        track.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track.artist,
                        color = SleekTextMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Devices icon
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Devices",
                    tint = SleekTextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                // Interactive miniature audio waves when playing:
                if (isPlaying) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(4) { idx ->
                            val infiniteWave = rememberInfiniteTransition(label = "audio bar wave")
                            val barHeight by infiniteWave.animateFloat(
                                initialValue = 4f,
                                targetValue = 20f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween((500..900).random(), easing = FastOutLinearInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "wave value"
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp, barHeight.dp)
                                    .background(SleekPurple, CircleShape)
                            )
                        }
                    }
                }

                IconButton(onClick = { onTogglePlay() }, modifier = Modifier.testTag("mini_play_pause_button")) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Sleek Interface premium progress indicator line at the bottom
        val progressFraction = if (track.durationSeconds > 0) {
            progressSecs.toFloat() / track.durationSeconds.toFloat()
        } else {
            0.0f
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                .height(2.dp)
                .background(SleekPurple)
        )
    }
}

// FULL SCREEN MUSIC PLAYER SCREEN (with synced scrolling lyrics & changing dynamic art)
@Composable
fun VibePlayerScreen(
    viewModel: VibeViewModel,
    onBack: () -> Unit
) {
    val track by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progressSecs by viewModel.progressSecs.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val isRepeatOne by viewModel.isRepeatOne.collectAsStateWithLifecycle()
    val currentTrackDuration by viewModel.currentTrackDuration.collectAsStateWithLifecycle()

    val crossfadeVal by viewModel.crossfadeSecs.collectAsStateWithLifecycle()
    val activeEqualizer by viewModel.selectedEqualizer.collectAsStateWithLifecycle()
    val sleepTimerMin by viewModel.sleepTimerMinutesLeft.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()

    var showControlDetailPresets by remember { mutableStateOf(false) }

    if (track == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No wave is currently resonating Vibe.", color = Color.White)
        }
        return
    }

    val currentT = track!!

    // Auto rotate vinyl animations
    val infiniteAnim = rememberInfiniteTransition(label = "Vinyl rotation")
    val rotationAngle by infiniteAnim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle rotation"
    )
    val animatedRotation = if (isPlaying) rotationAngle else 0f

    // Timed Synced Lyrics Auto Scroller
    val lyricsLines = remember(currentT.lyrics) {
        currentT.lyrics.split("\n").mapNotNull { line ->
            val regex = "\\[(\\d+):(\\d+)\\](.*)".toRegex()
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toInt()
                val sec = match.groupValues[2].toInt()
                val txt = match.groupValues[3].trim()
                Pair((min * 60) + sec, txt)
            } else null
        }
    }

    val currentLyricIndex = remember(progressSecs, lyricsLines) {
        lyricsLines.indexOfLast { progressSecs >= it.first }.coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onBack() }, modifier = Modifier.testTag("player_back_button")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "SOUNDSTAGE PLAYER",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = CustomGlowGold)
            )
            IconButton(onClick = { showControlDetailPresets = !showControlDetailPresets }) {
                Icon(Icons.Default.Tune, contentDescription = "Audio Equalizer details", tint = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Rotating Hologram Vinyl or YouTube Streaming Video Stage
        if (currentT.streamingUrl.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(3.dp, brush = Brush.linearGradient(listOf(NeonPink, NeonCyan)), shape = RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer { rotationZ = animatedRotation }
                    .background(Color.Black, shape = CircleShape)
                    .border(6.dp, brush = Brush.sweepGradient(listOf(NeonPink, NeonCyan, NeonPurple, NeonPink)), shape = CircleShape)
                    .padding(12.dp)
            ) {
                AsyncImage(
                    model = currentT.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Center spindle hole
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(45.dp)
                        .background(CosmicDarkBG, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Audio track Title & Artist info
        Text(
            text = currentT.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = Color.White),
            textAlign = TextAlign.Center
        )
        Text(
            text = currentT.artist,
            style = MaterialTheme.typography.bodyLarge.copy(color = NeonCyan, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Micro features indicators (e.g. sleep timer or equalizer status)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.background(GlassCardBG, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("EQ: $activeEqualizer", color = TextSecondaryLight, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (sleepTimerMin > 0) {
                Box(modifier = Modifier.background(NeonPink.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Sleep Timer: ${sleepTimerMin}m", color = NeonPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Synced Lyrics Display Frame
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = CardDefaults.cardColors(containerColor = GlassCardBG),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (lyricsLines.isNotEmpty()) {
                    val activeLyric = lyricsLines[currentLyricIndex]
                    Text(
                        text = activeLyric.second,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            brush = Brush.linearGradient(listOf(NeonPink, NeonCyan))
                        ),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text("Instrumental or un-synced lyric stream.", color = TextSecondaryLight, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Timeline Slider
        val maxDurationVal = if (currentT.streamingUrl.isNotEmpty()) currentTrackDuration else currentT.durationSeconds
        Slider(
            value = progressSecs.toFloat(),
            onValueChange = { viewModel.seekTo(it.toInt()) },
            valueRange = 0f..maxDurationVal.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = NeonPink,
                activeTrackColor = NeonPink,
                inactiveTrackColor = CosmicSlateGrey
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(progressSecs),
                color = TextSecondaryLight,
                fontSize = 12.sp
            )
            Text(
                formatTime(maxDurationVal),
                color = TextSecondaryLight,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Action Controllers Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle toggle",
                    tint = if (isShuffle) NeonCyan else Color.White.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = { viewModel.playPreviousTrack() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Song", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(68.dp)
                    .background(brush = Brush.linearGradient(listOf(NeonPink, NeonPurple)), shape = CircleShape)
                    .testTag("player_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play or Pause",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = { viewModel.playNextTrack() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Song", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { viewModel.toggleRepeatOne() }) {
                Icon(
                    imageVector = Icons.Default.RepeatOne,
                    contentDescription = "Repeat toggle",
                    tint = if (isRepeatOne) NeonPurple else Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Download and favourite checklist tools row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { viewModel.toggleFavorite(currentT.id) }) {
                Icon(
                    imageVector = if (currentT.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite toggle",
                    tint = if (currentT.isFavorite) NeonPink else Color.White
                )
            }
            IconButton(onClick = { viewModel.downloadTrackOffline(currentT.id) }) {
                Icon(
                    imageVector = if (currentT.isDownloaded) Icons.Default.CheckCircle else Icons.Default.ArrowCircleDown,
                    contentDescription = "Download Offline",
                    tint = if (currentT.isDownloaded) Color.Green else Color.White
                )
            }
            IconButton(onClick = { viewModel.startCollabRoomSession() }) {
                Icon(Icons.Default.Share, contentDescription = "Collab Sync Share", tint = NeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ambient Vol Controller row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (volume <= 0) Icons.Default.VolumeOff else if (volume < 50) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                contentDescription = "Volume control icon",
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = volume.toFloat(),
                onValueChange = { viewModel.setVolume(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = CosmicSlateGrey
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${volume}%",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        // Sub Settings Presets detail block displayed dynamically:
        AnimatedVisibility(visible = showControlDetailPresets) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SolidCardBG),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3D SPATIAL EQUALIZER & CONTROLS", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset buttons
                    val presets = listOf("3D Spatial Neo-Vibe", "Heavy Sub Bass", "Concert Hall Reverb", "Pure Vocals")
                    presets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setEqualizerPreset(preset) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(preset, color = Color.White)
                            if (activeEqualizer == preset) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonPink)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Crossfade Alignment duration: ${crossfadeVal.toInt()}s", color = TextSecondaryLight, fontSize = 11.sp)
                    Slider(
                        value = crossfadeVal,
                        onValueChange = { viewModel.setCrossfade(it) },
                        valueRange = 0f..12f
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Configure Sleep Timer Countdown:", color = TextSecondaryLight, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf(0, 15, 30, 60).forEach { min ->
                            Button(
                                onClick = { viewModel.setSleepTimer(min) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (sleepTimerMin == min) NeonPink else GlassCardBG),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(if (min == 0) "Off" else "${min}m", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ARTIST PROFILE SCREEN
@Composable
fun VibeArtistProfileScreen(
    viewModel: VibeViewModel,
    artistName: String,
    genre: String,
    language: String,
    onBack: () -> Unit
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val artistSongs = remember(allTracks, artistName) {
        allTracks.filter { it.artist.contains(artistName, ignoreCase = true) || it.artist == artistName }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onBack() }, modifier = Modifier.testTag("artist_back_button")) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Artist profile", color = TextSecondaryLight, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Huge profile banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = GlassCardBG),
            shape = RoundedCornerShape(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(NeonPink.copy(alpha = 0.5f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(NeonCyan, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("VERIFIED VIBE ARTIST", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                    Text(artistName, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black, color = Color.White))
                    Text("$genre • Verified stream in $language", color = TextSecondaryLight, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Discography hits",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        artistSongs.forEach { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(GlassCardBG, RoundedCornerShape(12.dp))
                    .clickable { viewModel.selectTrack(track.id, artistSongs) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(track.album, color = TextSecondaryLight, fontSize = 11.sp)
                }
                IconButton(onClick = { viewModel.toggleFavorite(track.id) }) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Fav",
                        tint = if (track.isFavorite) NeonPink else Color.White
                    )
                }
            }
        }
    }
}

// "HEY VIBE" VOICE OVERLAY INTERFACE (with fluid animated audio wave visualizers)
@Composable
fun VibeVoiceAssistantOverlay(
    viewModel: VibeViewModel,
    onClose: () -> Unit
) {
    val dialogList by viewModel.voiceDialogList.collectAsStateWithLifecycle()
    val isProcessing by viewModel.voiceIsProcessing.collectAsStateWithLifecycle()
    var rawInputText by remember { mutableStateOf("") }

    // Quick command prompts examples
    val suggestedPrompts = listOf(
        "Play a marana mass song",
        "Play sad Tamil songs",
        "Play trending Korean rap",
        "Collab with Mudaseer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDarkBG.copy(alpha = 0.96f))
            .clickable(enabled = false) {} // block click throughs
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "HEY VIBE CONSOLE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = NeonPink)
                )
                IconButton(onClick = { onClose() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close overlay", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Conversation feed scroll box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(GlassCardBG, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (dialogList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "“Hey Vibe, play some energetic dance loops.”",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap an example or send custom voice context below:",
                            color = TextSecondaryLight,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dialogList) { (text, isUser) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) NeonPink else CosmicSlateGrey
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = text,
                                        modifier = Modifier.padding(12.dp),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick suggestion chips
            Text("TAP QUICK METRICS SUGGESTIONS:", color = TextSecondaryLight, fontSize = 10.sp, letterSpacing = 1.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedPrompts.forEach { prompt ->
                    Button(
                        onClick = { viewModel.submitVoiceQuery(prompt) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlassCardBG),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(prompt, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Recording Waves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(12) { idx ->
                            val infiniteWave = rememberInfiniteTransition(label = "voice waves")
                            val hVal by infiniteWave.animateFloat(
                                initialValue = 5f,
                                targetValue = 48f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween((300..1000).random(), easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "wave heights"
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp, hVal.dp)
                                    .background(NeonPink, CircleShape)
                            )
                        }
                    }
                } else {
                    Text("ACTIVE DECIBEL PROBING SYNCED IN STEREO", color = TextSecondaryLight, fontSize = 10.sp, letterSpacing = 2.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom Text Entry Trigger (provides a dual input mode for maximum reliability in quiet/sterile spaces)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = rawInputText,
                    onValueChange = { rawInputText = it },
                    placeholder = { Text("Transcribe speaking...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CosmicSlateGrey,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (rawInputText.isNotBlank()) {
                            viewModel.submitVoiceQuery(rawInputText)
                            rawInputText = ""
                        }
                    }),
                    enabled = !isProcessing
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = {
                        if (rawInputText.isNotBlank()) {
                            viewModel.submitVoiceQuery(rawInputText)
                            rawInputText = ""
                        } else {
                            viewModel.submitVoiceQuery("play happy tamil songs") // Default trigger
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .background(NeonCyan, CircleShape),
                    enabled = !isProcessing
                ) {
                    Icon(
                        imageVector = if (isProcessing) Icons.Default.HourglassBottom else Icons.Default.Send,
                        contentDescription = "Submit Vocal soundwave",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

// SETTINGS & PREMIUM VIP PAYMENT CART PLANS
@Composable
fun VibeSettingsScreen(
    viewModel: VibeViewModel,
    isDarkTheme: MutableState<Boolean>
) {
    val emailUser by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val isPremium by viewModel.isUserPremium.collectAsStateWithLifecycle()
    val activeQuality by viewModel.audioQuality.collectAsStateWithLifecycle()

    var showPremiumUnlockDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_layout"),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Text(
                "VIP Setting Core",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black, color = Color.White)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Account section Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCardBG),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(NeonPink, NeonPurple)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emailUser?.take(1)?.uppercase() ?: "G",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Credential:", color = TextSecondaryLight, fontSize = 11.sp)
                        Text(emailUser ?: "guest.vibe@aistudio.com", color = Color.White, fontWeight = FontWeight.Bold)
                        if (isPremium) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .background(CustomGlowGold, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("VIBE PREMIER EXTREME VIP", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Premium plan banner
        if (!isPremium) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SolidCardBG),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CustomGlowGold)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = CustomGlowGold, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Join Vibe Premier VIP", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Unlock extreme lossless 3D spatial bitrates, unlimited offline downloads, customizable collaborative rooms settings, and deep lyrics streams.", color = TextSecondaryLight, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showPremiumUnlockDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CustomGlowGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("UPGRADE FOR ₹119/MONTH", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        // Audio and Theme settings list toggles
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("SOUNDPLAY ENVIRONMENT SETTING", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Dark/Light Mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Interactive Neon Darkness Mode", color = Color.White)
                }
                Switch(
                    checked = isDarkTheme.value,
                    onCheckedChange = { isDarkTheme.value = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonPink, checkedTrackColor = NeonPink.copy(alpha = 0.5f))
                )
            }

            // Audio Streaming bitrates selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Vibe Bitrate Audio Quality", color = Color.White)
                        Text(activeQuality, color = TextSecondaryLight, fontSize = 11.sp)
                    }
                }

                Row {
                    listOf("Eco", "Lossless").forEach { q ->
                        val targetQuality = if (q == "Eco") "Eco 96kbps" else "Extreme Lossless 320kbps"
                        val isSel = activeQuality == targetQuality
                        Button(
                            onClick = { viewModel.setAudioQualityOption(targetQuality) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSel) NeonPink else GlassCardBG),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(q, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { viewModel.triggerLogout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, color = Color.Red, shape = RoundedCornerShape(12.dp))
                    .testTag("logout_button")
            ) {
                Text("DISCONNECT SECURITY ID (LOGOUT)", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Dynamic Mock Payment Upgrade dialog
    if (showPremiumUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumUnlockDialog = false },
            title = { Text("Checkout Secure Gateway", color = Color.White) },
            text = { Text("Activate unlimited Vibe Premium with dynamic localized syncing and extreme bitrates.", color = TextSecondaryLight) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.togglePremiumSubscription(true)
                        showPremiumUnlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CustomGlowGold)
                ) {
                    Text("AUTHORIZE IN NEON", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumUnlockDialog = false }) {
                    Text("DECELERATE", color = Color.White)
                }
            },
            containerColor = CosmicDarkBG,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Helpers
fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
