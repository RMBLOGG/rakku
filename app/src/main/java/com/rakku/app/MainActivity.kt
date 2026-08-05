package com.rakku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rakku.app.data.local.SessionManager
import com.rakku.app.data.remote.RakkuApiRepository
import com.rakku.app.data.remote.SupabaseRepository
import com.rakku.app.ui.anime.AnimeDetailScreen
import com.rakku.app.ui.anime.AnimePlayerScreen
import com.rakku.app.ui.anime.AnimeScreen
import com.rakku.app.ui.anime.AnimeViewModel
import com.rakku.app.ui.auth.AuthViewModel
import com.rakku.app.ui.auth.LoginScreen
import com.rakku.app.ui.auth.RegisterScreen
import com.rakku.app.ui.chat.ChatScreen
import com.rakku.app.ui.chat.ChatViewModel
import com.rakku.app.ui.home.HomeScreen
import com.rakku.app.ui.home.HomeViewModel
import com.rakku.app.ui.manga.MangaDetailScreen
import com.rakku.app.ui.manga.MangaReaderScreen
import com.rakku.app.ui.manga.MangaScreen
import com.rakku.app.ui.manga.MangaViewModel
import com.rakku.app.ui.profile.ProfileScreen
import com.rakku.app.ui.profile.ProfileViewModel
import com.rakku.app.ui.theme.CyanAccent
import com.rakku.app.ui.theme.DarkSurface
import com.rakku.app.ui.theme.RakkuTheme
import com.rakku.app.ui.theme.TextSecondary
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // WAJIB dipanggil SEBELUM super.onCreate() - ini yang bikin sistem tau
        // aktivitas ini punya splash screen yang perlu ditampilkan dulu (tema
        // Theme.Rakku.Splash di manifest otomatis di-switch balik ke tema normal
        // setelah ini, lewat "postSplashScreenTheme" yang udah didefinisikan).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SessionManager(this)
        val supabaseRepository = SupabaseRepository(sessionManager)
        val rakkuApiRepository = RakkuApiRepository(applicationContext)

        val authViewModel = AuthViewModel(sessionManager, supabaseRepository)
        val homeViewModel = HomeViewModel(rakkuApiRepository, supabaseRepository)
        val animeViewModel = AnimeViewModel(rakkuApiRepository, supabaseRepository, sessionManager, applicationContext)
        val mangaViewModel = MangaViewModel(rakkuApiRepository, supabaseRepository, sessionManager)
        val chatViewModel = ChatViewModel(supabaseRepository, sessionManager)
        val profileViewModel = ProfileViewModel(sessionManager, supabaseRepository)

        // Deteksi apakah app dibuka dari link konfirmasi email
        // (rakku://login-callback...) - kalau iya, email user itu udah pasti
        // ke-confirm duluan oleh server Supabase SEBELUM redirect ke sini,
        // jadi di app tinggal kasih tau usernya & arahkan ke halaman Login.
        val emailJustConfirmed = intent?.data?.scheme == "rakku" && intent?.data?.host == "login-callback"

        setContent {
            RakkuTheme {
                MainAppScreen(
                    sessionManager = sessionManager,
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,
                    animeViewModel = animeViewModel,
                    mangaViewModel = mangaViewModel,
                    chatViewModel = chatViewModel,
                    profileViewModel = profileViewModel,
                    emailJustConfirmed = emailJustConfirmed
                )
            }
        }
    }
}

@Composable
fun MainAppScreen(
    sessionManager: SessionManager,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    animeViewModel: AnimeViewModel,
    mangaViewModel: MangaViewModel,
    chatViewModel: ChatViewModel,
    profileViewModel: ProfileViewModel,
    emailJustConfirmed: Boolean = false
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(emailJustConfirmed) {
        if (emailJustConfirmed) {
            android.widget.Toast.makeText(
                context,
                "Email berhasil dikonfirmasi! Silakan login.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            navController.navigate("login")
        }
    }

    val bottomNavRoutes = listOf("home", "anime", "manga", "chat", "profile")
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = CyanAccent
                ) {
                    val navItems = listOf(
                        Triple("home", "Beranda", Icons.Default.Home),
                        Triple("anime", "Anime", Icons.Default.PlayCircle),
                        Triple("manga", "Manga", Icons.Default.MenuBook),
                        Triple("chat", "Chat", Icons.Default.Forum),
                        Triple("profile", "Profil", Icons.Default.Person)
                    )

                    navItems.forEach { (route, label, icon) ->
                        val isSelected = currentRoute == route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) CyanAccent else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyanAccent else TextSecondary
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DarkSurface
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // TAB 1: HOME
            composable("home") {
                val userProfile by sessionManager.currentUserProfile.collectAsState()
                HomeScreen(
                    viewModel = homeViewModel,
                    userProfile = userProfile,
                    onNavigateToAnimeTab = { navController.navigate("anime") },
                    onNavigateToMangaTab = { navController.navigate("manga") },
                    onNavigateToProfile = { navController.navigate("profile") },
                    onSelectAnimeDetail = { slug -> navController.navigate("anime_detail/$slug") },
                    onSelectMangaDetail = { url ->
                        val encoded = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("manga_detail/$encoded")
                    }
                )
            }

            // TAB 2: ANIME
            composable("anime") {
                AnimeScreen(
                    viewModel = animeViewModel,
                    onSelectAnime = { slug -> navController.navigate("anime_detail/$slug") }
                )
            }

            composable("anime_detail/{slug}") { backStackEntry ->
                val slug = backStackEntry.arguments?.getString("slug") ?: ""
                AnimeDetailScreen(
                    slug = slug,
                    viewModel = animeViewModel,
                    onBack = { navController.popBackStack() },
                    onSelectEpisode = { animeSlug, episodeSlug ->
                        navController.navigate("anime_player/$animeSlug/$episodeSlug")
                    }
                )
            }

            composable("anime_player/{animeSlug}/{episodeSlug}") { backStackEntry ->
                val animeSlug = backStackEntry.arguments?.getString("animeSlug") ?: ""
                val episodeSlug = backStackEntry.arguments?.getString("episodeSlug") ?: ""
                AnimePlayerScreen(
                    animeSlug = animeSlug,
                    episodeSlug = episodeSlug,
                    viewModel = animeViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // TAB 3: MANGA
            composable("manga") {
                MangaScreen(
                    viewModel = mangaViewModel,
                    onSelectManga = { url ->
                        val encoded = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("manga_detail/$encoded")
                    }
                )
            }

            composable(
                route = "manga_detail/{url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                MangaDetailScreen(
                    url = decodedUrl,
                    viewModel = mangaViewModel,
                    onBack = { navController.popBackStack() },
                    onSelectChapter = { chapterUrl ->
                        val encodedChapter = URLEncoder.encode(chapterUrl, "UTF-8")
                        navController.navigate("manga_reader/$encodedChapter")
                    }
                )
            }

            composable(
                route = "manga_reader/{chapterUrl}",
                arguments = listOf(navArgument("chapterUrl") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("chapterUrl") ?: ""
                val decodedUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                MangaReaderScreen(
                    chapterUrl = decodedUrl,
                    viewModel = mangaViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // TAB 4: CHAT
            composable("chat") {
                ChatScreen(
                    viewModel = chatViewModel,
                    onNavigateToLogin = { navController.navigate("login") }
                )
            }

            // TAB 5: PROFILE
            composable("profile") {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToLogin = { navController.navigate("login") },
                    onSelectAnimeDetail = { slug -> navController.navigate("anime_detail/$slug") },
                    onSelectMangaDetail = { url ->
                        val encoded = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("manga_detail/$encoded")
                    },
                    onOpenHistory = { navController.navigate("history") },
                    onOpenBookmarks = { navController.navigate("bookmarks") }
                )
            }

            composable("history") {
                com.rakku.app.ui.profile.HistoryScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() },
                    onSelectAnimeDetail = { slug -> navController.navigate("anime_detail/$slug") }
                )
            }

            composable("bookmarks") {
                com.rakku.app.ui.profile.BookmarksScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() },
                    onSelectAnimeDetail = { slug -> navController.navigate("anime_detail/$slug") },
                    onSelectMangaDetail = { url ->
                        val encoded = URLEncoder.encode(url, "UTF-8")
                        navController.navigate("manga_detail/$encoded")
                    }
                )
            }

            // AUTH SCREENS
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                        profileViewModel.refreshProfile()
                        navController.popBackStack()
                    },
                    onNavigateToRegister = { navController.navigate("register") },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                        profileViewModel.refreshProfile()
                        navController.popBackStack()
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
        }
    }
}
