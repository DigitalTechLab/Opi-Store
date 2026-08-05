package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

// --- DATENMODELLE ---
data class AppRelease(val version: String, val downloadUrl: String, val isPreRelease: Boolean)
data class OpenSourceApp(
    val id: String, val name: String, val owner: String, val platform: String,
    val description: String, val repoUrl: String, val avatarUrl: String,
    val preFetchedReleases: List<AppRelease>? = null
)
data class UserProfile(val name: String, val login: String, val avatarUrl: String, val bio: String, val platform: String)
data class SimpleRepo(val name: String, val owner: String, val description: String, val htmlUrl: String, val stars: Int, val avatarUrl: String = "")
data class ProjectFile(val name: String, val path: String, val type: String, val sha: String, val downloadUrl: String?)
data class DownloadInfo(val progress: String = "", val isDownloading: Boolean = false, val isDownloaded: Boolean = false)
data class Issue(val id: String, val number: Int, val title: String, val body: String, val state: String, val user: String, val userAvatar: String, val comments: Int, val nodeId: String? = null)
data class IssueComment(val id: String, val body: String, val user: String, val userAvatar: String, val reactions: Int, val myReactionId: String? = null)

private fun t(key: String, lang: String): String {
    val isDe = lang == "de"
    return when (key) {
        "tab_search" -> if (isDe) "Suche" else "Search"
        "tab_apk_manager" -> if (isDe) "APK Manager" else "APK Manager"
        "tab_account" -> if (isDe) "Account" else "Account"
        "tab_settings" -> if (isDe) "Settings" else "Settings"
        "search_title" -> "🚀 Opi Store"
        "search_hint" -> if (isDe) "Suche..." else "Search..."
        "status_loading_repo" -> if (isDe) "Lade Repo vom Link..." else "Loading repo from link..."
        "status_no_apks" -> if (isDe) "Dieses Projekt hat keine Android APKs." else "This project has no Android APKs."
        "status_no_project" -> if (isDe) "Kein passendes Projekt gefunden." else "No matching project found."
        "status_searching" -> if (isDe) "Suche und filtere APKs..." else "Searching and filtering APKs..."
        "status_no_apps_found" -> if (isDe) "Keine Android-Apps mit APK gefunden." else "No Android apps with APK found."
        "status_error" -> if (isDe) "Fehler aufgetreten." else "Error occurred."
        "by" -> if (isDe) "von" else "by"
        "description" -> if (isDe) "Beschreibung" else "Description"
        "no_releases" -> if (isDe) "Keine APK-Releases für dieses Projekt gefunden." else "No APK releases found for this project."
        "stable_release" -> if (isDe) "Stable Release" else "Stable Release"
        "pre_release" -> if (isDe) "Pre-Release" else "Pre-Release"
        "downloading" -> if (isDe) "Lade..." else "Downloading..."
        "download_apk" -> if (isDe) "APK Herunterladen" else "Download APK"
        "already_downloaded" -> if (isDe) "Bereits geladen (Installieren)" else "Already downloaded (Install)"
        "select_version" -> if (isDe) "Version auswählen" else "Select Version"
        "stable_build" -> if (isDe) "Stable Build" else "Stable Build"
        "beta_experimental" -> if (isDe) "Beta / Experimental" else "Beta / Experimental"
        "apk_manager_title" -> if (isDe) "📦 APK Manager" else "APK Manager"
        "apk_manager_subtitle" -> if (isDe) "Tippe auf eine App, um sie zu verwalten." else "Tap on an app to manage it."
        "no_apks_downloaded" -> if (isDe) "Noch keine APKs heruntergeladen." else "No APKs downloaded yet."
        "install" -> if (isDe) "Installieren" else "Install"
        "repair" -> if (isDe) "Reparieren" else "Repair"
        "apk_corrupted" -> if (isDe) "APK unvollständig oder beschädigt!" else "APK incomplete or corrupted!"
        "repairing" -> if (isDe) "Repariere..." else "Repairing..."
        "delete_apk" -> if (isDe) "APK Datei löschen" else "Delete APK file"
        "issues" -> if (isDe) "Issues" else "Issues"
        "open" -> if (isDe) "Offen" else "Open"
        "closed" -> if (isDe) "Geschlossen" else "Closed"
        "no_issues" -> if (isDe) "Keine Issues gefunden." else "No issues found."
        "new_issue" -> if (isDe) "Neues Issue" else "New Issue"
        "title" -> if (isDe) "Titel" else "Title"
        "body" -> if (isDe) "Beschreibung" else "Body"
        "post" -> if (isDe) "Posten" else "Post"
        "comments" -> if (isDe) "Kommentare" else "Comments"
        "write_comment" -> if (isDe) "Kommentar schreiben..." else "Write a comment..."
        "settings_title" -> if (isDe) "⚙️ Einstellungen" else "⚙️ Settings"
        "design" -> if (isDe) "Design" else "Design"
        "language" -> if (isDe) "Sprache" else "Language"
        "select_design" -> if (isDe) "Design wählen" else "Select Design"
        "select_language" -> if (isDe) "Sprache wählen" else "Select Language"
        "theme_system" -> if (isDe) "System" else "System"
        "theme_light" -> if (isDe) "Hell" else "Light"
        "theme_dark" -> if (isDe) "Dunkel" else "Dark"
        "profile_edit_title" -> if (isDe) "Profil bearbeiten" else "Edit Profile"
        "display_name" -> if (isDe) "Anzeigename" else "Display Name"
        "bio" -> if (isDe) "Bio" else "Bio"
        "save" -> if (isDe) "Speichern" else "Save"
        "cancel" -> if (isDe) "Abbrechen" else "Cancel"
        "tap_to_edit" -> if (isDe) "Name/Bio tippen zum Bearbeiten" else "Tap Name/Bio to edit"
        "change_token" -> if (isDe) "Token ändern" else "Change token"
        "my_repositories" -> if (isDe) "Deine Repositories" else "Your Repositories"
        "starred" -> if (isDe) "Markiert (Starred)" else "Starred"
        "login_title" -> if (isDe) "Login" else "Login"
        "personal_access_token" -> if (isDe) "Personal Access Token" else "Personal Access Token"
        "search_apps_hint" -> if (isDe) "Suche Apps..." else "Search apps..."
        "no_apps" -> if (isDe) "Keine Apps." else "No apps."
        "delete" -> if (isDe) "Löschen" else "Delete"
        "new_github_project" -> if (isDe) "Neues GitHub Projekt" else "New GitHub Project"
        "project_name" -> if (isDe) "Projektname" else "Project Name"
        "private_repository" -> if (isDe) "Privates Repository" else "Private repository"
        "create" -> if (isDe) "Erstellen" else "Create"
        "delete_project_title" -> if (isDe) "🗑️ Projekt löschen?" else "🗑️ Delete project?"
        "delete_project_confirm" -> if (isDe) "Möchtest du das Repository wirklich endgültig löschen? Diese Aktion kann nicht rückgängig gemacht werden!" else "Do you really want to delete the repository permanently? This action cannot be undone!"
        "delete_issue_title" -> if (isDe) "Issue löschen?" else "Delete Issue?"
        "delete_issue_confirm" -> if (isDe) "Möchtest du dieses Issue wirklich löschen?" else "Do you really want to delete this issue?"
        "delete_comment_title" -> if (isDe) "Kommentar löschen?" else "Delete Comment?"
        "delete_comment_confirm" -> if (isDe) "Möchtest du diesen Kommentar wirklich löschen?" else "Do you really want to delete this comment?"
        "yes_delete" -> if (isDe) "Ja, Löschen" else "Yes, delete"
        "uploaded" -> if (isDe) "Hochgeladen!" else "Uploaded!"
        "upload_image" -> if (isDe) "Bild hochladen" else "Upload image"
        "edit_via_app" -> if (isDe) "Edit via App" else "Edit via App"
        "upload_via_app" -> if (isDe) "Upload via App" else "Upload via App"
        "mb_loaded" -> if (isDe) "MB geladen" else "MB loaded"
        "stable_releases_header" -> if (isDe) "✅ STABLE RELEASES" else "✅ STABLE RELEASES"
        "pre_releases_header" -> if (isDe) "⚠️ PRE-RELEASES" else "⚠️ PRE-RELEASES"
        "tutorial" -> if (isDe) "Tutorial" else "Tutorial"
        "github_tutorial_title" -> if (isDe) "GitHub Token Tutorial" else "GitHub Token Tutorial"
        "github_tutorial_step1" -> if (isDe) "1. Öffne diesen Link: https://github.com/settings/tokens" else "1. Open this link: https://github.com/settings/tokens"
        "github_tutorial_step2" -> if (isDe) "2. Melde dich an (oder erstelle einen Account)." else "2. Log in (or create an account)."
        "github_tutorial_step3" -> if (isDe) "3. Klicke auf \"Neuen Token generieren\" und wähle \"klassisch\" aus." else "3. Click on \"Generate new token\" and select \"classic\"."
        "github_tutorial_step4" -> if (isDe) "4. Gib dem Token einen Namen." else "4. Give the token a name."
        "github_tutorial_step5" -> if (isDe) "5. Klicke ALLE Scopes an." else "5. Select ALL scopes."
        "github_tutorial_step6" -> if (isDe) "6. Bei \"Ablaufdatum\" wähle \"Kein Ablaufdatum\" aus." else "6. For \"Expiration\", select \"No expiration\"."
        "github_tutorial_step7" -> if (isDe) "7. Klicke unten auf \"Token generieren\"." else "7. Click \"Generate token\" at the bottom."
        "github_tutorial_step8" -> if (isDe) "8. Kopiere den Token und füge ihn hier in der App ein." else "8. Copy the token and paste it here in the app."
        "codeberg_tutorial_title" -> if (isDe) "Codeberg Token Tutorial" else "Codeberg Token Tutorial"
        "codeberg_tutorial_step1" -> if (isDe) "1. Öffne diesen Link: https://codeberg.org/user/settings/applications" else "1. Open this link: https://codeberg.org/user/settings/applications"
        "codeberg_tutorial_step2" -> if (isDe) "2. Melde dich an (oder erstelle einen Account)." else "2. Log in (or create an account)."
        "codeberg_tutorial_step3" -> if (isDe) "3. Klicke unter \"Zugriffstoken\" auf \"Neuer Zugangstoken\"." else "3. Under \"Access Tokens\", click \"New Access Token\"."
        "codeberg_tutorial_step4" -> if (isDe) "4. Gib dem Token einen Namen." else "4. Give the token a name."
        "codeberg_tutorial_step5" -> if (isDe) "5. Wähle bei \"Repository- und Organisationszugriff\" die Option \"Alle (öffentlich, privat, begrenzt)\" aus." else "5. For \"Repository and Organization Access\", select \"All (public, private, limited)\"."
        "codeberg_tutorial_step6" -> if (isDe) "6. Wähle bei allen Berechtigungen \"Lesen und Schreiben\" aus." else "6. For all permissions, select \"Read and Write\"."
        "codeberg_tutorial_step7" -> if (isDe) "7. Klicke auf \"Token generieren\"." else "7. Click \"Generate Token\"."
        "codeberg_tutorial_step8" -> if (isDe) "8. Kopiere den Token und füge ihn hier in der App ein." else "8. Copy the token and paste it here in the app."
        "version" -> "Version"
        else -> key
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        setContent {
            val themeSetting = remember { mutableStateOf(sharedPrefs.getString("THEME_SETTING", "System") ?: "System") }
            val isDarkTheme = when (themeSetting.value) {
                "Light" -> false
                "Dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OpenSourceStoreApp(sharedPrefs, themeSetting)
                }
            }
        }
    }
}

@Composable
fun OpenSourceStoreApp(sharedPrefs: android.content.SharedPreferences, themeSetting: MutableState<String>) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var githubToken by remember { mutableStateOf(sharedPrefs.getString("GITHUB_TOKEN", "") ?: "") }
    var codebergToken by remember { mutableStateOf(sharedPrefs.getString("CODEBERG_TOKEN", "") ?: "") }
    var languageSetting by remember { mutableStateOf(sharedPrefs.getString("LANGUAGE_SETTING", "en") ?: "en") }

    val activeDownloads = remember { mutableStateMapOf<String, DownloadInfo>() }
    val scope = rememberCoroutineScope()

    var selectedAppForDetail by remember { mutableStateOf<OpenSourceApp?>(null) }
    var showFullRepoListForUser by remember { mutableStateOf<String?>(null) }
    var fullRepoListPlatform by remember { mutableStateOf("GitHub") }
    var fullRepoListAppsOnly by remember { mutableStateOf(false) }
    var fullRepoListAvatarUrl by remember { mutableStateOf("") }
    var isViewingOwnProfile by remember { mutableStateOf(false) }
    var selectedRepoForFiles by remember { mutableStateOf<SimpleRepo?>(null) }
    var selectedFileForEdit by remember { mutableStateOf<ProjectFile?>(null) }

    if (selectedFileForEdit != null && selectedRepoForFiles != null) {
        CodeEditorScreen(repo = selectedRepoForFiles!!, file = selectedFileForEdit!!, token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, onBack = { selectedFileForEdit = null }, languageSetting = languageSetting)
    } else if (selectedRepoForFiles != null) {
        ProjectFilesScreen(repo = selectedRepoForFiles!!, token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, onBack = { selectedRepoForFiles = null }, onFileClick = { if (it.type == "file") selectedFileForEdit = it }, languageSetting = languageSetting)
    } else if (showFullRepoListForUser != null) {
        FullRepoListScreen(owner = showFullRepoListForUser!!, title = if (fullRepoListAppsOnly) "${t("by", languageSetting)} $showFullRepoListForUser" else t("my_repositories", languageSetting), token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, appsOnly = fullRepoListAppsOnly, isOwnProfile = isViewingOwnProfile, ownerAvatarUrl = fullRepoListAvatarUrl, onBack = { showFullRepoListForUser = null }, onAppSelected = { showFullRepoListForUser = null; selectedAppForDetail = it }, onRepoSelected = { selectedRepoForFiles = it }, githubToken = githubToken, codebergToken = codebergToken, languageSetting = languageSetting)
    } else if (selectedAppForDetail != null) {
        AppDetailFullScreen(
            app = selectedAppForDetail!!, 
            githubToken = githubToken, 
            codebergToken = codebergToken, 
            onBack = { selectedAppForDetail = null }, 
            onOwnerClick = { owner, platform, avatar -> 
                fullRepoListPlatform = platform
                fullRepoListAppsOnly = true
                fullRepoListAvatarUrl = avatar
                isViewingOwnProfile = false
                showFullRepoListForUser = owner 
            }, 
            languageSetting = languageSetting,
            activeDownloads = activeDownloads,
            globalScope = scope
        )
    } else {
        BackHandler {
            if (selectedTab != 0) {
                selectedTab = 0
            }
        }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text(t("tab_search", languageSetting)) }, icon = { Icon(Icons.Default.Search, null) })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text(t("tab_apk_manager", languageSetting)) }, icon = { Icon(Icons.Default.Folder, null) })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, label = { Text(t("tab_account", languageSetting)) }, icon = { Icon(Icons.Default.AccountCircle, null) })
                    NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, label = { Text(t("tab_settings", languageSetting)) }, icon = { Icon(Icons.Default.Settings, null) })
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> SearchScreen(githubToken = githubToken, codebergToken = codebergToken, onAppSelected = { selectedAppForDetail = it }, languageSetting = languageSetting)
                    1 -> ApkManagerScreen(githubToken = githubToken, codebergToken = codebergToken, languageSetting = languageSetting, activeDownloads = activeDownloads, globalScope = scope)
                    2 -> AccountMultiScreen(githubToken = githubToken, codebergToken = codebergToken, onGithubTokenSaved = { t -> githubToken = t; sharedPrefs.edit().putString("GITHUB_TOKEN", t).apply() }, onCodebergTokenSaved = { t -> codebergToken = t; sharedPrefs.edit().putString("CODEBERG_TOKEN", t).apply() }, onUsernameClick = { login -> fullRepoListPlatform = if (sharedPrefs.getString("LAST_PLATFORM", "GitHub") == "GitHub") "GitHub" else "Codeberg"; fullRepoListAppsOnly = false; fullRepoListAvatarUrl = ""; isViewingOwnProfile = true; showFullRepoListForUser = login }, languageSetting = languageSetting, onAppSelected = { selectedAppForDetail = it })
                    3 -> SettingsScreen(currentTheme = themeSetting.value, onThemeChange = { themeSetting.value = it; sharedPrefs.edit().putString("THEME_SETTING", it).apply() }, currentLanguage = languageSetting, onLanguageChange = { languageSetting = it; sharedPrefs.edit().putString("LANGUAGE_SETTING", it).apply() }, languageSetting = languageSetting)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    languageSetting: String
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = t("settings_title", languageSetting), 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Design Button
        Button(
            onClick = { showThemeDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(t("design", languageSetting), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                val themeLabel = when(currentTheme) {
                    "System" -> t("theme_system", languageSetting)
                    "Light" -> t("theme_light", languageSetting)
                    "Dark" -> t("theme_dark", languageSetting)
                    else -> currentTheme
                }
                Text(themeLabel, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sprache Button
        Button(
            onClick = { showLanguageDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(t("language", languageSetting), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                val langLabel = when(currentLanguage) {
                    "de" -> "Deutsch"
                    "en" -> "English"
                    else -> currentLanguage
                }
                Text(langLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(t("select_design", languageSetting)) },
            text = {
                Column {
                    listOf("System", "Light", "Dark").forEach { theme ->
                        val themeLabel = when(theme) {
                            "System" -> t("theme_system", languageSetting)
                            "Light" -> t("theme_light", languageSetting)
                            "Dark" -> t("theme_dark", languageSetting)
                            else -> theme
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onThemeChange(theme); showThemeDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentTheme == theme, onClick = { onThemeChange(theme); showThemeDialog = false })
                            Spacer(Modifier.width(12.dp))
                            Text(themeLabel)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(t("select_language", languageSetting)) },
            text = {
                Column {
                    listOf("Deutsch" to "de", "English" to "en").forEach { (label, code) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onLanguageChange(code); showLanguageDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentLanguage == code, onClick = { onLanguageChange(code); showLanguageDialog = false })
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun AccountMultiScreen(githubToken: String, codebergToken: String, onGithubTokenSaved: (String) -> Unit, onCodebergTokenSaved: (String) -> Unit, onUsernameClick: (String) -> Unit, languageSetting: String, onAppSelected: (OpenSourceApp) -> Unit) {
    var selectedPlatform by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).height(48.dp), horizontalArrangement = Arrangement.Center) {
            val ghSelected = selectedPlatform == 0
            Surface(modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp), color = if (ghSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, onClick = { selectedPlatform = 0 }) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = "https://github.com/fluidicon.png", contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
                    Spacer(modifier = Modifier.width(8.dp)); Text("GitHub", fontWeight = FontWeight.Bold, color = if (ghSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
            Surface(modifier = Modifier.weight(1f).fillMaxHeight(), shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp), color = if (!ghSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, onClick = { selectedPlatform = 1 }) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = "https://codeberg.org/assets/img/logo.png", contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
                    Spacer(modifier = Modifier.width(8.dp)); Text("Codeberg", fontWeight = FontWeight.Bold, color = if (!ghSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (selectedPlatform == 0) {
                AccountScreenDetails(token = githubToken, platformName = "GitHub", onTokenSaved = onGithubTokenSaved, fetchProfile = { fetchGitHubProfile(it) }, fetchRepos = { fetchUserRepos(it, "https://api.github.com/user/repos") }, fetchStarred = { fetchUserRepos(it, "https://api.github.com/user/starred") }, updateProfile = { t, n, b -> updateGitHubProfile(t, n, b) }, onUsernameClick = onUsernameClick, languageSetting = languageSetting, onAppSelected = onAppSelected)
            } else {
                AccountScreenDetails(token = codebergToken, platformName = "Codeberg", onTokenSaved = onCodebergTokenSaved, fetchProfile = { fetchCodebergProfile(it) }, fetchRepos = { fetchUserRepos(it, "https://codeberg.org/api/v1/user/repos") }, fetchStarred = { fetchUserRepos(it, "https://api.github.com/user/starred") }, updateProfile = { t, n, b -> updateCodebergProfile(t, n, b) }, onUsernameClick = onUsernameClick, languageSetting = languageSetting, onAppSelected = onAppSelected)
            }
        }
    }
}

@Composable
fun AccountScreenDetails(token: String, platformName: String, onTokenSaved: (String) -> Unit, fetchProfile: suspend (String) -> UserProfile?, fetchRepos: suspend (String) -> List<SimpleRepo>, fetchStarred: suspend (String) -> List<SimpleRepo>, updateProfile: suspend (String, String, String) -> Boolean, onUsernameClick: (String) -> Unit, languageSetting: String, onAppSelected: (OpenSourceApp) -> Unit) {
    val scope = rememberCoroutineScope()
    var tokenInput by remember { mutableStateOf(token) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var ownRepos by remember { mutableStateOf<List<SimpleRepo>>(emptyList()) }
    var starredRepos by remember { mutableStateOf<List<SimpleRepo>>(emptyList()) }
    val repoReleases = remember { mutableStateMapOf<String, List<AppRelease>>() }
    var isCheckingToken by remember { mutableStateOf(token.isNotBlank()) }
    var isEditingToken by remember { mutableStateOf(token.isBlank()) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    var editBioInput by remember { mutableStateOf("") }
    
    var showTutorialDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(token) {
        if (token.isNotBlank()) {
            isCheckingToken = true; isEditingToken = false
            val profile = fetchProfile(token)
            if (profile != null) { 
                userProfile = profile
                ownRepos = fetchRepos(token)
                starredRepos = fetchStarred(token) 
                
                // Pre-fetch releases for first few repos to avoid "loading" look
                (ownRepos.take(5) + starredRepos.take(5)).forEach { repo ->
                    launch {
                        val rels = fetchReleasesForApp(OpenSourceApp(id = "${repo.owner}/${repo.name}", name = repo.name, owner = repo.owner, platform = platformName, description = repo.description, repoUrl = repo.htmlUrl, avatarUrl = repo.avatarUrl), if(platformName == "GitHub") token else "", if(platformName == "Codeberg") token else "")
                        repoReleases[repo.htmlUrl] = rels
                    }
                }
            } else { userProfile = null; isEditingToken = true }
            isCheckingToken = false
        } else { userProfile = null; isEditingToken = true; isCheckingToken = false }
    }
    if (showEditProfileDialog && userProfile != null) {
        AlertDialog(onDismissRequest = { showEditProfileDialog = false }, title = { Text("$platformName ${t("profile_edit_title", languageSetting)}") }, text = { Column { OutlinedTextField(value = editNameInput, onValueChange = { editNameInput = it }, label = { Text(t("display_name", languageSetting)) }); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = editBioInput, onValueChange = { editBioInput = it }, label = { Text(t("bio", languageSetting)) }, maxLines = 3) } }, confirmButton = { Button(onClick = { scope.launch { val success = updateProfile(token, editNameInput, editBioInput); if (success) userProfile = userProfile?.copy(name = editNameInput, bio = editBioInput); showEditProfileDialog = false } }) { Text(t("save", languageSetting)) } }, dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text(t("cancel", languageSetting)) } })
    }

    if (isCheckingToken) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    else if (userProfile != null && !isEditingToken) {
        LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Spacer(modifier = Modifier.height(8.dp)); AsyncImage(model = userProfile!!.avatarUrl, contentDescription = "Avatar", modifier = Modifier.size(100.dp).clip(CircleShape)); Spacer(modifier = Modifier.height(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Text(userProfile!!.name.ifBlank { userProfile!!.login }, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { editNameInput = userProfile!!.name.ifBlank { userProfile!!.login }; editBioInput = userProfile!!.bio; showEditProfileDialog = true }.padding(horizontal = 4.dp))
                    Text("@${userProfile!!.login}", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onUsernameClick(userProfile!!.login) }.padding(horizontal = 4.dp))
                    if (userProfile!!.bio.isNotBlank()) { Spacer(modifier = Modifier.height(8.dp)); Text(userProfile!!.bio, fontSize = 14.sp, color = Color.LightGray, textAlign = TextAlign.Center) }
                    Text("✏️ ${t("tap_to_edit", languageSetting)}", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp)); OutlinedButton(onClick = { isEditingToken = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(t("change_token", languageSetting)) }; Spacer(modifier = Modifier.height(24.dp))
            }
            if (ownRepos.isNotEmpty()) { 
                item { Text(t("my_repositories", languageSetting), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) }
                items(ownRepos) { repo -> 
                    RepoCard(repo, languageSetting = languageSetting) { 
                        onAppSelected(OpenSourceApp(id = "${repo.owner}/${repo.name}", name = repo.name, owner = repo.owner, platform = platformName, description = repo.description, repoUrl = repo.htmlUrl, avatarUrl = repo.avatarUrl, preFetchedReleases = repoReleases[repo.htmlUrl])) 
                    } 
                }
                item { Spacer(modifier = Modifier.height(16.dp)) } 
            }
            if (starredRepos.isNotEmpty()) { 
                item { Text(t("starred", languageSetting), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) }
                items(starredRepos) { repo -> 
                    RepoCard(repo, languageSetting = languageSetting) { 
                        onAppSelected(OpenSourceApp(id = "${repo.owner}/${repo.name}", name = repo.name, owner = repo.owner, platform = platformName, description = repo.description, repoUrl = repo.htmlUrl, avatarUrl = repo.avatarUrl, preFetchedReleases = repoReleases[repo.htmlUrl])) 
                    } 
                } 
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary); Spacer(modifier = Modifier.height(16.dp)); Text("$platformName ${t("login_title", languageSetting)}", fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(16.dp)); 
            OutlinedTextField(value = tokenInput, onValueChange = { tokenInput = it }, label = { Text(t("personal_access_token", languageSetting)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation()); 
            if (platformName == "GitHub" || platformName == "Codeberg") {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showTutorialDialog = true }, modifier = Modifier.align(Alignment.End)) { Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(t("tutorial", languageSetting)) }
            }
            Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (token.isNotBlank()) OutlinedButton(onClick = { isEditingToken = false; tokenInput = token }, modifier = Modifier.weight(1f)) { Text(t("cancel", languageSetting)) }; Button(onClick = { onTokenSaved(tokenInput) }, modifier = Modifier.weight(1f)) { Text(t("save", languageSetting)) } }
        }
    }

    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = { Text(t(if (platformName == "GitHub") "github_tutorial_title" else "codeberg_tutorial_title", languageSetting), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val prefix = if (platformName == "GitHub") "github" else "codeberg"
                    val url = if (platformName == "GitHub") "https://github.com/settings/tokens" else "https://codeberg.org/user/settings/applications"
                    
                    val step1Full = t("${prefix}_tutorial_step1", languageSetting)
                    val annotatedStep1 = buildAnnotatedString {
                        val parts = step1Full.split(url)
                        if (parts.size > 1) {
                            append(parts[0])
                            withStyle(style = SpanStyle(color = Color(0xFF2196F3))) { append(url) }
                            append(parts[1])
                        } else { append(step1Full) }
                    }
                    Text(annotatedStep1, modifier = Modifier.clickable { uriHandler.openUri(url) }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(t("${prefix}_tutorial_step2", languageSetting))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("${prefix}_tutorial_step3", languageSetting))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("${prefix}_tutorial_step4", languageSetting))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("${prefix}_tutorial_step5", languageSetting))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("${prefix}_tutorial_step6", languageSetting))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("${prefix}_tutorial_step7", languageSetting))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(t("${prefix}_tutorial_step8", languageSetting))
                }
            },
            confirmButton = { Button(onClick = { showTutorialDialog = false }) { Text("OK") } }
        )
    }
}

@Composable
fun RepoCard(repo: SimpleRepo, languageSetting: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(repo.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("${repo.stars}", fontSize = 14.sp) }
            }
            Text("${t("by", languageSetting)} ${repo.owner}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SearchScreen(githubToken: String, codebergToken: String, onAppSelected: (OpenSourceApp) -> Unit, languageSetting: String) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<OpenSourceApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    fun performSearch(searchQuery: String) {
        query = searchQuery; isLoading = true; searchResults = emptyList()
        scope.launch {
            try {
                if (searchQuery.startsWith("http")) {
                    statusMessage = t("status_loading_repo", languageSetting)
                    val app = fetchAppFromUrl(searchQuery, githubToken, codebergToken)
                    if (app != null) { 
                        val releases = fetchReleasesForApp(app, githubToken, codebergToken)
                        if (releases.isNotEmpty()) { 
                            onAppSelected(app.copy(preFetchedReleases = releases))
                            statusMessage = "" 
                        } else statusMessage = t("status_no_apks", languageSetting) 
                    } else statusMessage = t("status_no_project", languageSetting)
                } else {
                    statusMessage = t("status_searching", languageSetting)
                    searchResults = searchMultiSourceApps(searchQuery, githubToken, codebergToken)
                    statusMessage = if (searchResults.isEmpty()) t("status_no_apps_found", languageSetting) else ""
                }
            } catch (e: Exception) { statusMessage = t("status_error", languageSetting) } finally { isLoading = false }
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(t("search_title", languageSetting), fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(t("search_hint", languageSetting)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { IconButton(onClick = { if (query.isNotBlank()) performSearch(query) }) { Icon(Icons.Default.Search, null) } }); Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (statusMessage.isNotEmpty()) Text(statusMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) { items(searchResults) { app -> AppCard(app = app, onClick = { onAppSelected(app) }) } }
    }
}

@Composable
fun AppCard(app: OpenSourceApp, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = app.avatarUrl, contentDescription = "Logo", contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(CircleShape))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(app.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, modifier = Modifier.weight(1f)); Text(app.platform, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) }
                Spacer(modifier = Modifier.height(4.dp)); Text(app.description, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailFullScreen(
    app: OpenSourceApp, 
    githubToken: String, 
    codebergToken: String, 
    onBack: () -> Unit, 
    onOwnerClick: (String, String, String) -> Unit, 
    languageSetting: String,
    activeDownloads: MutableMap<String, DownloadInfo>,
    globalScope: CoroutineScope
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    var releases by remember { mutableStateOf<List<AppRelease>?>(null) }; var isLoadingReleases by remember { mutableStateOf(true) }
    var selectedRelease by remember { mutableStateOf<AppRelease?>(null) }; var showVersionSheet by remember { mutableStateOf(false) }
    
    var showIssues by remember { mutableStateOf(false) }
    
    LaunchedEffect(app) { 
        if (app.preFetchedReleases != null) {
            releases = app.preFetchedReleases
            selectedRelease = app.preFetchedReleases.firstOrNull()
            isLoadingReleases = false
        } else {
            isLoadingReleases = true
            val fetched = fetchReleasesForApp(app, githubToken, codebergToken)
            releases = fetched
            selectedRelease = fetched.firstOrNull()
            isLoadingReleases = false
        }
    }
    
    val expectedFileName = if (selectedRelease != null) "${app.platform}_${app.owner}_${app.name}_${selectedRelease!!.version}.apk" else ""
    val targetFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), expectedFileName)
    
    val downloadState = activeDownloads[expectedFileName] ?: DownloadInfo()
    var isDownloadedLocal by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedRelease, downloadState.isDownloaded) { 
        if(expectedFileName.isNotEmpty()) isDownloadedLocal = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), expectedFileName).exists() 
    }
    
    BackHandler { onBack() }
    Scaffold(topBar = { TopAppBar(title = { Text(app.name) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("cancel", languageSetting)) } }, actions = {
        IconButton(onClick = { showIssues = true }) { Icon(Icons.Default.BugReport, null) }
    }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = app.avatarUrl, contentDescription = "Logo", contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp).clip(CircleShape))
                Spacer(modifier = Modifier.height(16.dp)); Text(text = app.name, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${t("by", languageSetting)} ${app.owner}", 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onOwnerClick(app.owner, app.platform, app.avatarUrl) }
                            .padding(4.dp)
                    )
                    Text(text = " • ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = app.platform, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { uriHandler.openUri(app.repoUrl) }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp)); Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(modifier = Modifier.padding(16.dp)) { Text(t("description", languageSetting), fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(modifier = Modifier.height(8.dp)); Text(app.description, fontSize = 14.sp) } }

                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    if (isLoadingReleases) Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    else if (releases.isNullOrEmpty() || selectedRelease == null) Text(t("no_releases", languageSetting), color = Color.Red, modifier = Modifier.padding(16.dp))
                    else {
                        OutlinedButton(onClick = { showVersionSheet = true }, modifier = Modifier.fillMaxWidth().wrapContentHeight(), contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)) { Text("${selectedRelease!!.version} (${if(selectedRelease!!.isPreRelease) t("pre_release", languageSetting) else t("stable_release", languageSetting)})", fontSize = 16.sp, textAlign = TextAlign.Center) }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (downloadState.isDownloading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { 
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                Text(downloadState.progress, fontSize = 14.sp) 
                            }
                        }
                        
                        if (isDownloadedLocal && !downloadState.isDownloading) {
                            Button(onClick = { installApk(context, targetFile) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Icon(Icons.Default.Check, null); Spacer(modifier = Modifier.width(8.dp)); Text(t("already_downloaded", languageSetting), fontSize = 16.sp) }
                        } else {
                            Button(
                                onClick = { 
                                    activeDownloads[expectedFileName] = DownloadInfo(isDownloading = true)
                                    globalScope.launch { 
                                        val file = downloadApk(context, expectedFileName, selectedRelease!!.downloadUrl, githubToken, languageSetting) { 
                                            activeDownloads[expectedFileName] = DownloadInfo(progress = it, isDownloading = true)
                                        }
                                        activeDownloads[expectedFileName] = DownloadInfo(isDownloaded = file != null, isDownloading = false)
                                    } 
                                }, 
                                modifier = Modifier.fillMaxWidth().height(56.dp), 
                                enabled = !downloadState.isDownloading
                            ) { 
                                Icon(Icons.Default.Download, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (downloadState.isDownloading) t("downloading", languageSetting) else t("download_apk", languageSetting), fontSize = 18.sp) 
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIssues) {
        IssuesScreen(
            app = app, 
            githubToken = githubToken, 
            codebergToken = codebergToken, 
            languageSetting = languageSetting, 
            onUserClick = { user, platform, avatar -> 
                onOwnerClick(user, platform, avatar) 
            },
            onDismiss = { showIssues = false }
        )
    }

    if (showVersionSheet && releases != null) {
        val window = (context as? android.app.Activity)?.window
        SideEffect {
            if (window != null) {
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.hide(WindowInsetsCompat.Type.statusBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        ModalBottomSheet(onDismissRequest = { showVersionSheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 12.dp, dragHandle = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } }) {
            SideEffect {
                if (window != null) {
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(12.dp)); Text(t("select_version", languageSetting), fontSize = 22.sp, fontWeight = FontWeight.Black) }
                LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    val (stable, pre) = releases!!.partition { !it.isPreRelease }
                    if (stable.isNotEmpty()) { item { Text(t("stable_releases_header", languageSetting), color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)) }; items(stable) { VersionItem(it, it == selectedRelease, languageSetting = languageSetting) { selectedRelease = it; showVersionSheet = false } } }
                    if (pre.isNotEmpty()) { item { Spacer(modifier = Modifier.height(16.dp)); Text(t("pre_releases_header", languageSetting), color = Color(0xFFFFA000), fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) }; items(pre) { VersionItem(it, it == selectedRelease, languageSetting = languageSetting) { selectedRelease = it; showVersionSheet = false } } }
                }
            }
        }
    }
}

@Composable
fun VersionItem(release: AppRelease, isSelected: Boolean, languageSetting: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).clickable { onClick() }, color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) { Text(text = release.version, fontSize = 17.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface); Text(text = if (release.isPreRelease) t("beta_experimental", languageSetting) else t("stable_build", languageSetting), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (release.isPreRelease) Color(0xFFFFA000) else Color(0xFF4CAF50)) }
            Icon(imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(if (isSelected) 24.dp else 20.dp))
        }
    }
}

@Composable
fun ApkManagerScreen(githubToken: String, codebergToken: String, languageSetting: String, activeDownloads: MutableMap<String, DownloadInfo>, globalScope: CoroutineScope) {
    val context = LocalContext.current
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFileForDialog by remember { mutableStateOf<File?>(null) }
    var isRepairingLocal by remember { mutableStateOf(false) }
    var repairProgressLocal by remember { mutableStateOf("") }
    var currentFileValid by remember { mutableStateOf(true) }

    fun refreshFiles() { val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS); downloadedFiles = dir?.listFiles { _, name -> name.endsWith(".apk") }?.toList() ?: emptyList() }
    LaunchedEffect(Unit) { refreshFiles() }

    if (selectedFileForDialog != null) {
        val file = selectedFileForDialog!!
        val fileName = file.name
        val parts = fileName.removeSuffix(".apk").split("_")
        val platform = parts.getOrNull(0) ?: ""
        val owner = parts.getOrNull(1) ?: ""
        val appName = parts.getOrNull(2) ?: ""
        val version = parts.getOrNull(3) ?: ""
        
        val downloadState = activeDownloads[fileName] ?: DownloadInfo()
        val isDownloadingGlobal = downloadState.isDownloading
        
        LaunchedEffect(file, isDownloadingGlobal) { 
            if (!isDownloadingGlobal) {
                currentFileValid = isApkValid(context, file)
            }
        }
        val avatarUrl = when (platform) { "GitHub" -> "https://github.com/$owner.png"; "Codeberg" -> "https://codeberg.org/assets/img/logo.png"; else -> null }
        
        AlertDialog(onDismissRequest = { if (!isRepairingLocal) selectedFileForDialog = null }, confirmButton = {}, title = null, text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (avatarUrl != null) AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))) else Icon(Icons.Default.Android, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp)); Text(appName.ifBlank { file.name }, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                if (version.isNotBlank()) Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 4.dp)) { Text("${t("version", languageSetting)} $version", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                
                if (!currentFileValid && !isDownloadingGlobal) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("apk_corrupted", languageSetting), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Text("${file.length() / (1024 * 1024)} MB", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)); Spacer(modifier = Modifier.height(24.dp))
                
                if (isRepairingLocal || isDownloadingGlobal) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                    Text(if (isDownloadingGlobal) downloadState.progress else repairProgressLocal, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = { installApk(context, file); selectedFileForDialog = null }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if(currentFileValid) MaterialTheme.colorScheme.primary else Color.Gray)) { Icon(Icons.Default.Download, null); Spacer(modifier = Modifier.width(10.dp)); Text(t("install", languageSetting), fontWeight = FontWeight.Bold) }
                    
                    if (!currentFileValid && platform.isNotBlank() && owner.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { 
                            isRepairingLocal = true
                            activeDownloads[fileName] = DownloadInfo(isDownloading = true, progress = t("repairing", languageSetting))
                            globalScope.launch {
                                val success = repairApk(context, file, platform, owner, appName, version, githubToken, codebergToken, languageSetting) { 
                                    repairProgressLocal = it
                                    activeDownloads[fileName] = DownloadInfo(isDownloading = true, progress = it)
                                }
                                isRepairingLocal = false
                                activeDownloads[fileName] = DownloadInfo(isDownloading = false, isDownloaded = success)
                                currentFileValid = isApkValid(context, file)
                                if (success) { refreshFiles() }
                            }
                        }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))) {
                            Icon(Icons.Default.Build, null); Spacer(modifier = Modifier.width(10.dp)); Text(t("repair", languageSetting), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp)); TextButton(onClick = { file.delete(); refreshFiles(); selectedFileForDialog = null }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(t("delete_apk", languageSetting), fontWeight = FontWeight.Medium) }
                }
            }
        })
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(t("apk_manager_title", languageSetting), fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(t("apk_manager_subtitle", languageSetting), fontSize = 12.sp, color = Color.Gray); Spacer(modifier = Modifier.height(16.dp))
        if (downloadedFiles.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(t("no_apks_downloaded", languageSetting)) }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(downloadedFiles) { file ->
            val fileName = file.name
            val parts = fileName.removeSuffix(".apk").split("_")
            val isDownloading = activeDownloads[fileName]?.isDownloading == true
            val isValid = if (isDownloading) true else isApkValid(context, file)
            val avatarUrl = when (parts.getOrNull(0)) { "GitHub" -> "https://github.com/${parts.getOrNull(1)}.png"; "Codeberg" -> "https://codeberg.org/assets/img/logo.png"; else -> null }
            Card(modifier = Modifier.fillMaxWidth().clickable { selectedFileForDialog = file }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))) { if (avatarUrl != null) AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) else Icon(Icons.Default.Android, null, modifier = Modifier.fillMaxSize(), tint = Color.Gray) }
                Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(parts.getOrNull(2) ?: file.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!isValid) { Spacer(Modifier.width(8.dp)); Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                        if (isDownloading) { Spacer(Modifier.width(8.dp)); CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp) }
                    }
                    Text(if (isDownloading) activeDownloads[fileName]?.progress ?: "" else "${file.length() / (1024 * 1024)} MB", fontSize = 12.sp, color = Color.Gray) 
                }; Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
            } }
        } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullRepoListScreen(owner: String, title: String, token: String, platform: String, appsOnly: Boolean = false, isOwnProfile: Boolean = false, ownerAvatarUrl: String = "", onBack: () -> Unit, onAppSelected: (OpenSourceApp) -> Unit = {}, onRepoSelected: (SimpleRepo) -> Unit = {}, githubToken: String = "", codebergToken: String = "", languageSetting: String) {
    val scope = rememberCoroutineScope()
    BackHandler { onBack() }
    var repos by remember { mutableStateOf<List<SimpleRepo>>(emptyList()) }; var isLoading by remember { mutableStateOf(true) }
    var currentAvatarUrl by remember { mutableStateOf(ownerAvatarUrl) }; var statusText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }; var repoToDelete by remember { mutableStateOf<SimpleRepo?>(null) }
    var newRepoName by remember { mutableStateOf("") }; var newRepoDesc by remember { mutableStateOf("") }; var isPrivate by remember { mutableStateOf(false) }
    fun refresh() { scope.launch {
        isLoading = true; statusText = if (appsOnly) t("search_apps_hint", languageSetting) else ""
        if (currentAvatarUrl.isEmpty()) { (if (platform == "GitHub") fetchGitHubProfile(token) else fetchCodebergProfile(token))?.let { currentAvatarUrl = it.avatarUrl } }
        val all = if (platform == "GitHub") fetchGitHubReposForOwner(owner, token, isOwnProfile) else fetchCodebergReposForOwner(owner, token, isOwnProfile)
        if (appsOnly) { repos = all.map { repo -> async { if (fetchReleasesForApp(OpenSourceApp(id = "${repo.owner}/${repo.name}", name = repo.name, owner = repo.owner, platform = platform, description = repo.description, repoUrl = repo.htmlUrl, avatarUrl = currentAvatarUrl), githubToken, codebergToken).isNotEmpty()) repo else null } }.awaitAll().filterNotNull(); if (repos.isEmpty()) statusText = t("no_apps", languageSetting) } else repos = all
        isLoading = false
    } }
    LaunchedEffect(Unit) { refresh() }
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("cancel", languageSetting)) } }, actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, null) } }) },
        floatingActionButton = { if (isOwnProfile && platform == "GitHub") FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); if (statusText.isNotEmpty()) { Spacer(Modifier.height(16.dp)); Text(statusText, color = Color.Gray) } } }
        else if (repos.isEmpty() && statusText.isNotEmpty()) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(statusText, color = Color.Gray) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) { items(repos, key = { it.htmlUrl }) { repo ->
            if (isOwnProfile) {
                val state = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { repoToDelete = repo; true } else false })
                LaunchedEffect(repoToDelete) { if (repoToDelete == null && state.currentValue != SwipeToDismissBoxValue.Settled) state.snapTo(SwipeToDismissBoxValue.Settled) }
                SwipeToDismissBox(state = state, enableDismissFromStartToEnd = false, backgroundContent = { Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color(0xFFEF5350) else Color.Transparent), contentAlignment = Alignment.CenterEnd) { Row(modifier = Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) { Text(t("delete", languageSetting), color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Icon(Icons.Default.Delete, null, tint = Color.White) } } }) { RepoCard(repo, languageSetting = languageSetting) { onRepoSelected(repo) } }
            } else RepoCard(repo, languageSetting = languageSetting) { onAppSelected(OpenSourceApp(id = "${repo.owner}/${repo.name}", name = repo.name, owner = repo.owner, platform = platform, description = repo.description, repoUrl = repo.htmlUrl, avatarUrl = currentAvatarUrl)) }
            Spacer(Modifier.height(8.dp))
        } }
        if (showCreateDialog) AlertDialog(onDismissRequest = { showCreateDialog = false }, title = { Text(t("new_github_project", languageSetting)) }, text = { Column { OutlinedTextField(value = newRepoName, onValueChange = { newRepoName = it }, label = { Text(t("project_name", languageSetting)) }); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = newRepoDesc, onValueChange = { newRepoDesc = it }, label = { Text(t("description", languageSetting)) }); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it }); Text(t("private_repository", languageSetting)) } } }, confirmButton = { Button(onClick = { scope.launch { if (createGitHubRepo(token, newRepoName, newRepoDesc, isPrivate)) { showCreateDialog = false; refresh() } } }) { Text(t("create", languageSetting)) } }, dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(t("cancel", languageSetting)) } })
        if (repoToDelete != null) AlertDialog(onDismissRequest = { repoToDelete = null }, title = { Text(t("delete_project_title", languageSetting)) }, text = { Text(t("delete_project_confirm", languageSetting).replace("%s", repoToDelete!!.name)) }, confirmButton = { Button(onClick = { val r = repoToDelete!!; repoToDelete = null; scope.launch { if (if (platform == "GitHub") deleteGitHubRepo(token, owner, r.name) else deleteCodebergRepo(token, owner, r.name)) refresh() } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(t("yes_delete", languageSetting)) } }, dismissButton = { TextButton(onClick = { repoToDelete = null }) { Text(t("cancel", languageSetting)) } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFilesScreen(repo: SimpleRepo, token: String, platform: String, onBack: () -> Unit, onFileClick: (ProjectFile) -> Unit, languageSetting: String) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<ProjectFile>>(emptyList()) }; var isLoading by remember { mutableStateOf(true) }

    fun refreshFiles() {
        isLoading = true
        scope.launch {
            files = fetchFilesFromPlatform(token, repo.owner, repo.name, platform, currentPath)
            isLoading = false
        }
    }

    LaunchedEffect(currentPath) { refreshFiles() }

    BackHandler {
        if (currentPath.isEmpty()) onBack()
        else {
            val parts = currentPath.split("/")
            currentPath = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) scope.launch {
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        val pathPrefix = if (currentPath.isEmpty()) "" else "$currentPath/"
        if (bytes != null && uploadFileToPlatform(token, repo.owner, repo.name, "${pathPrefix}upload_${System.currentTimeMillis()}.png", Base64.encodeToString(bytes, Base64.NO_WRAP), platform, languageSetting)) {
            Toast.makeText(context, t("uploaded", languageSetting), Toast.LENGTH_SHORT).show()
            refreshFiles()
        }
    } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column { 
                        Text(repo.name)
                        if (currentPath.isNotEmpty()) Text(currentPath, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }, 
                navigationIcon = { 
                    IconButton(onClick = { 
                        if (currentPath.isEmpty()) onBack()
                        else {
                            val parts = currentPath.split("/")
                            currentPath = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("cancel", languageSetting)) } 
                }
            ) 
        }, 
        floatingActionButton = { FloatingActionButton(onClick = { picker.launch("*/*") }) { Icon(Icons.Default.UploadFile, null) } }
    ) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding)) { 
            items(files) { f -> 
                ListItem(
                    headlineContent = { Text(f.name) }, 
                    leadingContent = { Icon(if (f.type == "dir") Icons.Default.Folder else Icons.Default.Description, null, tint = if (f.type == "dir") Color.Cyan else Color.Gray) }, 
                    modifier = Modifier.clickable { 
                        if (f.type == "dir") currentPath = f.path
                        else onFileClick(f) 
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.1f)) 
            } 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(repo: SimpleRepo, file: ProjectFile, token: String, platform: String, onBack: () -> Unit, languageSetting: String) {
    val scope = rememberCoroutineScope(); var text by remember { mutableStateOf("") }; var isLoading by remember { mutableStateOf(true) }; var isSaving by remember { mutableStateOf(false) }
    BackHandler { onBack() }
    LaunchedEffect(Unit) { text = fetchFileContent(token, repo.owner, repo.name, file.path, platform) ?: ""; isLoading = false }
    Scaffold(topBar = { TopAppBar(title = { Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, t("cancel", languageSetting)) } }, actions = { if (isSaving) CircularProgressIndicator(Modifier.size(24.dp)) else IconButton(onClick = { isSaving = true; scope.launch { if (updateFileOnPlatform(token, repo.owner, repo.name, file.path, text, file.sha, platform, languageSetting)) onBack() else isSaving = false } }) { Icon(Icons.Default.Save, null, tint = Color.Cyan) } }) }) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else TextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxSize().padding(padding), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp))
    }
}

val httpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

private suspend fun fetchGitHubProfile(token: String): UserProfile? = withContext(Dispatchers.IO) {
    try {
        val req = Request.Builder().url("https://api.github.com/user").addHeader("Authorization", "Bearer $token").build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: "")
            UserProfile(json.optString("name", ""), json.getString("login"), json.getString("avatar_url"), json.optString("bio", ""), "GitHub")
        }
    } catch (e: Exception) { null }
}

private suspend fun fetchCodebergProfile(token: String): UserProfile? = withContext(Dispatchers.IO) {
    try {
        val req = Request.Builder().url("https://codeberg.org/api/v1/user").addHeader("Authorization", "token $token").addHeader("Accept", "application/json").build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: "")
            UserProfile(json.optString("full_name", ""), json.getString("login"), json.getString("avatar_url"), json.optString("description", "").ifBlank { json.optString("biography", "") }, "Codeberg")
        }
    } catch (e: Exception) { null }
}

private suspend fun fetchGitHubReposForOwner(owner: String, token: String, isOwn: Boolean): List<SimpleRepo> = withContext(Dispatchers.IO) {
    val list = mutableListOf<SimpleRepo>()
    try {
        val url = if (isOwn) "https://api.github.com/user/repos?sort=updated&per_page=100" else "https://api.github.com/users/$owner/repos?sort=updated&per_page=100"
        val req = Request.Builder().url(url); if (token.isNotBlank()) req.addHeader("Authorization", "Bearer $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext list
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i); val ownerObj = obj.optJSONObject("owner") ?: continue
                list.add(SimpleRepo(obj.getString("name"), ownerObj.getString("login"), obj.optString("description", ""), obj.getString("html_url"), obj.optInt("stargazers_count", 0), ownerObj.optString("avatar_url", "")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchCodebergReposForOwner(owner: String, token: String, isOwn: Boolean): List<SimpleRepo> = withContext(Dispatchers.IO) {
    val list = mutableListOf<SimpleRepo>()
    try {
        val url = if (isOwn) "https://codeberg.org/api/v1/user/repos?sort=updated&per_page=50" else "https://codeberg.org/api/v1/users/$owner/repos?sort=updated&per_page=50"
        val req = Request.Builder().url(url).addHeader("Accept", "application/json"); if (token.isNotBlank()) req.addHeader("Authorization", "token $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext list
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i); val ownerObj = obj.optJSONObject("owner") ?: continue
                list.add(SimpleRepo(obj.getString("name"), ownerObj.getString("login"), obj.optString("description", ""), obj.getString("html_url"), obj.optInt("stargazers_count", 0), ownerObj.optString("avatar_url", "")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchUserRepos(token: String, url: String): List<SimpleRepo> = withContext(Dispatchers.IO) {
    val list = mutableListOf<SimpleRepo>()
    try {
        val req = Request.Builder().url("$url?sort=updated&per_page=20").addHeader("Authorization", "Bearer $token").addHeader("Accept", "application/json").build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext list
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i); val owner = obj.optJSONObject("owner") ?: continue
                list.add(SimpleRepo(obj.getString("name"), owner.getString("login"), obj.optString("description", ""), obj.getString("html_url"), obj.optInt("stargazers_count", 0), owner.optString("avatar_url", "")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun updateGitHubProfile(token: String, newName: String, newBio: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject().apply { put("name", newName); put("bio", newBio) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url("https://api.github.com/user").patch(body).addHeader("Authorization", "Bearer $token").build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun updateCodebergProfile(token: String, newName: String, newBio: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject().apply { put("full_name", newName); put("biography", newBio) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url("https://codeberg.org/api/v1/user").patch(body).addHeader("Authorization", "token $token").build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun searchMultiSourceApps(query: String, ghToken: String, cbToken: String): List<OpenSourceApp> = coroutineScope {
    val gh = async(Dispatchers.IO) { searchGitHub(query, ghToken) }
    val cb = async(Dispatchers.IO) { searchCodeberg(query, cbToken) }
    val all = (gh.await() + cb.await()).distinctBy { it.id }
    all.map { app -> 
        async(Dispatchers.IO) { 
            val releases = fetchReleasesForApp(app, ghToken, cbToken)
            if (releases.isNotEmpty()) app.copy(preFetchedReleases = releases) else null 
        } 
    }.awaitAll().filterNotNull()
}

private suspend fun searchGitHub(query: String, token: String): List<OpenSourceApp> = withContext(Dispatchers.IO) {
    val list = mutableListOf<OpenSourceApp>()
    try {
        val req = Request.Builder().url("https://api.github.com/search/repositories?q=$query+in:name,description&sort=stars&per_page=15")
        if (token.isNotBlank()) req.addHeader("Authorization", "Bearer $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            val items = JSONObject(resp.body?.string() ?: "").optJSONArray("items") ?: return@withContext list
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i); val owner = item.getJSONObject("owner")
                list.add(OpenSourceApp(id = "${owner.getString("login")}/${item.getString("name")}", name = item.getString("name"), owner = owner.getString("login"), platform = "GitHub", description = item.optString("description", ""), repoUrl = item.getString("html_url"), avatarUrl = owner.getString("avatar_url")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun searchCodeberg(query: String, token: String): List<OpenSourceApp> = withContext(Dispatchers.IO) {
    val list = mutableListOf<OpenSourceApp>()
    try {
        val req = Request.Builder().url("https://codeberg.org/api/v1/repos/search?q=$query&limit=10")
        if (token.isNotBlank()) req.addHeader("Authorization", "token $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            val items = JSONObject(resp.body?.string() ?: "").optJSONArray("data") ?: return@withContext list
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i); val owner = item.getJSONObject("owner")
                list.add(OpenSourceApp(id = "${owner.getString("login")}/${item.getString("name")}", name = item.getString("name"), owner = owner.getString("login"), platform = "Codeberg", description = item.optString("description", ""), repoUrl = item.getString("html_url"), avatarUrl = owner.getString("avatar_url")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchReleasesForApp(app: OpenSourceApp, ghToken: String, cbToken: String): List<AppRelease> = withContext(Dispatchers.IO) {
    if (app.platform == "GitHub") fetchGitHubReleases(app.owner, app.name, ghToken) else fetchCodebergReleases(app.owner, app.name, cbToken)
}

private suspend fun fetchGitHubReleases(owner: String, repo: String, token: String): List<AppRelease> = withContext(Dispatchers.IO) {
    val list = mutableListOf<AppRelease>()
    try {
        val req = Request.Builder().url("https://api.github.com/repos/$owner/$repo/releases?per_page=100")
        if (token.isNotBlank()) req.addHeader("Authorization", "Bearer $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val rel = arr.getJSONObject(i); val assets = rel.optJSONArray("assets") ?: continue
                for (a in 0 until assets.length()) {
                    val asset = assets.getJSONObject(a)
                    if (asset.getString("name").endsWith(".apk")) { list.add(AppRelease(rel.getString("tag_name"), asset.getString("browser_download_url"), rel.getBoolean("prerelease"))) }
                }
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchCodebergReleases(owner: String, repo: String, token: String): List<AppRelease> = withContext(Dispatchers.IO) {
    val list = mutableListOf<AppRelease>()
    try {
        val req = Request.Builder().url("https://codeberg.org/api/v1/repos/$owner/$repo/releases?limit=100")
        if (token.isNotBlank()) req.addHeader("Authorization", "token $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val rel = arr.getJSONObject(i); val assets = rel.optJSONArray("assets") ?: continue
                for (a in 0 until assets.length()) {
                    val asset = assets.getJSONObject(a)
                    if (asset.getString("name").endsWith(".apk")) { list.add(AppRelease(rel.getString("tag_name"), asset.getString("browser_download_url"), rel.getBoolean("prerelease"))) }
                }
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchAppFromUrl(url: String, ghToken: String, cbToken: String): OpenSourceApp? = withContext(Dispatchers.IO) {
    try {
        if (url.contains("github.com")) {
            val parts = url.substringAfter("github.com/").split("/")
            if (parts.size >= 2) {
                val req = Request.Builder().url("https://api.github.com/repos/${parts[0]}/${parts[1]}")
                if (ghToken.isNotBlank()) req.addHeader("Authorization", "Bearer $ghToken")
                httpClient.newCall(req.build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val json = JSONObject(resp.body?.string() ?: "")
                    return@withContext OpenSourceApp("${parts[0]}/${parts[1]}", json.getString("name"), parts[0], "GitHub", json.optString("description", ""), json.getString("html_url"), json.getJSONObject("owner").getString("avatar_url"))
                }
            }
        }
    } catch (e: Exception) {}
    null
}

private suspend fun downloadApk(context: Context, fileName: String, url: String, token: String, languageSetting: String, onProgress: (String) -> Unit): File? = withContext(Dispatchers.IO) {
    try {
        val req = Request.Builder().url(url); if (token.isNotBlank()) req.addHeader("Authorization", "Bearer $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            val body = resp.body ?: return@withContext null
            body.byteStream().use { input -> file.outputStream().use { out ->
                val buf = ByteArray(8 * 1024); var total = 0L; var read = input.read(buf); val len = body.contentLength()
                while (read >= 0) { out.write(buf, 0, read); total += read; withContext(Dispatchers.Main) { if (len > 0) onProgress("${(total * 100 / len).toInt()}%") else onProgress("${total / 1024 / 1024}${t("mb_loaded", languageSetting)}") }; read = input.read(buf) }
            } }
            withContext(Dispatchers.Main) { onProgress("100%") }
            return@withContext file
        }
    } catch (e: Exception) { null }
}

private fun installApk(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK })
}

private fun isApkValid(context: Context, file: File): Boolean {
    return try {
        context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) != null
    } catch (e: Exception) { false }
}

private suspend fun repairApk(context: Context, file: File, platform: String, owner: String, repo: String, version: String, ghToken: String, cbToken: String, languageSetting: String, onProgress: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
    try {
        val app = OpenSourceApp("", repo, owner, platform, "", "", "")
        val releases = fetchReleasesForApp(app, ghToken, cbToken)
        val release = releases.find { it.version == version } ?: return@withContext false
        val url = release.downloadUrl
        
        val currentSize = file.length()
        val req = Request.Builder().url(url).addHeader("Range", "bytes=$currentSize-")
        val token = if (platform == "GitHub") ghToken else cbToken
        if (token.isNotBlank()) {
            val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
            req.addHeader("Authorization", authHeader)
        }
        
        httpClient.newCall(req.build()).execute().use { resp ->
            if (resp.code == 416) return@withContext isApkValid(context, file)
            if (!resp.isSuccessful) return@withContext false
            val body = resp.body ?: return@withContext false
            val len = body.contentLength()
            body.byteStream().use { input ->
                java.io.FileOutputStream(file, true).use { out ->
                    val buf = ByteArray(8 * 1024)
                    var totalRead = 0L
                    var read = input.read(buf)
                    while (read >= 0) {
                        out.write(buf, 0, read)
                        totalRead += read
                        withContext(Dispatchers.Main) {
                            if (len > 0) onProgress("${(totalRead * 100 / len).toInt()}%")
                            else onProgress("${totalRead / 1024 / 1024}${t("mb_loaded", languageSetting)}")
                        }
                        read = input.read(buf)
                    }
                }
            }
        }
        return@withContext isApkValid(context, file)
    } catch (e: Exception) { false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuesScreen(app: OpenSourceApp, githubToken: String, codebergToken: String, languageSetting: String, onUserClick: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filterOpen by remember { mutableStateOf(true) }
    var selectedIssue by remember { mutableStateOf<Issue?>(null) }
    var showCreateIssue by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val token = if (app.platform == "GitHub") githubToken else codebergToken

    fun refresh(newIssue: Issue? = null) {
        if (newIssue != null) {
            if (filterOpen) issues = listOf(newIssue) + issues
            return
        }
        isLoading = true
        scope.launch {
            issues = fetchIssues(token, app.owner, app.name, app.platform, if (filterOpen) "open" else "closed")
            isLoading = false
        }
    }

    LaunchedEffect(filterOpen) { refresh() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t("issues", languageSetting)) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    actions = {
                        Box {
                            TextButton(onClick = { showFilterMenu = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (filterOpen) t("open", languageSetting) else t("closed", languageSetting))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                            DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(t("open", languageSetting)) },
                                    onClick = { filterOpen = true; showFilterMenu = false },
                                    leadingIcon = { Icon(Icons.Default.RadioButtonChecked, null, tint = Color.Green) }
                                )
                                DropdownMenuItem(
                                    text = { Text(t("closed", languageSetting)) },
                                    onClick = { filterOpen = false; showFilterMenu = false },
                                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color.Red) }
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showCreateIssue = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, null)
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                else if (issues.isEmpty()) Text(t("no_issues", languageSetting), Modifier.align(Alignment.Center))
                else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(issues) { issue ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable { selectedIssue = issue },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = issue.userAvatar, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(issue.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("#${issue.number} by ", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = issue.user,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { onUserClick(issue.user, app.platform, issue.userAvatar); onDismiss() }
                                            )
                                        }
                                    }
                                    if (issue.comments > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Comment, null, Modifier.size(14.dp), tint = Color.Gray)
                                            Spacer(Modifier.width(4.dp))
                                            Text("${issue.comments}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedIssue != null) {
            IssueDetailScreen(app = app, issue = selectedIssue!!, token = token, languageSetting = languageSetting, onUserClick = { u, p, a -> onUserClick(u, p, a); onDismiss() }, onBack = { selectedIssue = null; refresh() })
        }

        if (showCreateIssue) {
            CreateIssueDialog(app = app, token = token, languageSetting = languageSetting, onDismiss = { newIssue -> 
                showCreateIssue = false
                if (newIssue != null) refresh(newIssue) else refresh()
            })
        }
    }
}

@Composable
fun MarkdownBody(text: String, modifier: Modifier = Modifier) {
    val imageRegex = "!\\[.*?]\\((.*?)\\)".toRegex()
    val parts = mutableListOf<Pair<String, String?>>()
    var lastIndex = 0
    imageRegex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            parts.add(text.substring(lastIndex, match.range.first) to null)
        }
        parts.add("" to match.groupValues[1])
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        parts.add(text.substring(lastIndex) to null)
    }

    Column(modifier = modifier) {
        parts.forEach { (txt, url) ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else if (txt.isNotBlank()) {
                Text(txt, fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(app: OpenSourceApp, issue: Issue, token: String, languageSetting: String, onUserClick: (String, String, String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<IssueComment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    val myProfile = remember { mutableStateOf<UserProfile?>(null) }
    
    var uploadingImage by remember { mutableStateOf(false) }
    
    var showDeleteIssueConfirm by remember { mutableStateOf(false) }
    var commentToDelete by remember { mutableStateOf<IssueComment?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadingImage = true
            scope.launch {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val fileName = "issue_img_${System.currentTimeMillis()}.png"
                    if (uploadFileToPlatform(token, app.owner, app.name, ".opi_store/uploads/$fileName", base64, app.platform, languageSetting)) {
                        val rawUrl = if (app.platform == "GitHub") 
                            "https://raw.githubusercontent.com/${app.owner}/${app.name}/HEAD/.opi_store/uploads/$fileName"
                        else 
                            "https://codeberg.org/${app.owner}/${app.name}/raw/branch/main/.opi_store/uploads/$fileName"
                        commentText += "\n![]($rawUrl)\n"
                    }
                }
                uploadingImage = false
            }
        }
    }

    LaunchedEffect(Unit) {
        myProfile.value = if (app.platform == "GitHub") fetchGitHubProfile(token) else fetchCodebergProfile(token)
        comments = fetchIssueComments(token, app.owner, app.name, app.platform, issue.number, myProfile.value?.login)
        isLoading = false
    }

    if (showDeleteIssueConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteIssueConfirm = false },
            title = { Text(t("delete_issue_title", languageSetting)) },
            text = { Text(t("delete_issue_confirm", languageSetting)) },
            confirmButton = { 
                Button(onClick = { 
                    scope.launch { 
                        if (deleteIssue(token, app.owner, app.name, app.platform, issue)) onBack() 
                        showDeleteIssueConfirm = false
                    } 
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(t("yes_delete", languageSetting)) } 
            },
            dismissButton = { TextButton(onClick = { showDeleteIssueConfirm = false }) { Text(t("cancel", languageSetting)) } }
        )
    }

    if (commentToDelete != null) {
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text(t("delete_comment_title", languageSetting)) },
            text = { Text(t("delete_comment_confirm", languageSetting)) },
            confirmButton = { 
                Button(onClick = { 
                    scope.launch { 
                        if (deleteComment(token, app.owner, app.name, app.platform, commentToDelete!!.id)) {
                            comments = fetchIssueComments(token, app.owner, app.name, app.platform, issue.number, myProfile.value?.login)
                        }
                        commentToDelete = null
                    } 
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(t("yes_delete", languageSetting)) } 
            },
            dismissButton = { TextButton(onClick = { commentToDelete = null }) { Text(t("cancel", languageSetting)) } }
        )
    }

    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = { 
                TopAppBar(
                    title = { Text("#${issue.number}") }, 
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = {
                        if (issue.user == myProfile.value?.login) {
                            IconButton(onClick = { showDeleteIssueConfirm = true }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        }
                    }
                ) 
            },
            bottomBar = {
                Surface(tonalElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
                    Column(Modifier.padding(8.dp)) {
                        if (uploadingImage) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                        }
                        // Simple preview logic: look for images in commentText
                        val imageLinks = "!\\[.*?]\\((.*?)\\)".toRegex().findAll(commentText).map { it.groupValues[1] }.toList()
                        if (imageLinks.isNotEmpty()) {
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                items(imageLinks) { url ->
                                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { photoPicker.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary) }
                            OutlinedTextField(value = commentText, onValueChange = { commentText = it }, placeholder = { Text(t("write_comment", languageSetting)) }, modifier = Modifier.weight(1f), maxLines = 5)
                            IconButton(onClick = { scope.launch { if (commentText.isNotBlank() && postComment(token, app.owner, app.name, app.platform, issue.number, commentText)) { commentText = ""; comments = fetchIssueComments(token, app.owner, app.name, app.platform, issue.number, myProfile.value?.login) } } }, enabled = commentText.isNotBlank() && !uploadingImage) { Icon(Icons.Default.Send, null, tint = if(commentText.isNotBlank() && !uploadingImage) MaterialTheme.colorScheme.primary else Color.Gray) }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = issue.userAvatar, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onUserClick(issue.user, app.platform, issue.userAvatar) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column { 
                                    Text(
                                        text = issue.user, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 14.sp,
                                        modifier = Modifier.clickable { onUserClick(issue.user, app.platform, issue.userAvatar) }
                                    )
                                    Text(if (issue.state == "open") t("open", languageSetting) else t("closed", languageSetting), color = if (issue.state == "open") Color(0xFF4CAF50) else Color(0xFFE57373), fontSize = 11.sp, fontWeight = FontWeight.Bold) 
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp)); Text(issue.title, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            MarkdownBody(issue.body)
                        }
                    }
                    Text(t("comments", languageSetting), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 16.sp)
                }
                if (isLoading) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                items(comments) { comment ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                        Row(Modifier.padding(8.dp)) {
                            AsyncImage(
                                model = comment.userAvatar, 
                                contentDescription = null, 
                                modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onUserClick(comment.user, app.platform, comment.userAvatar) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { 
                                    Text(
                                        text = comment.user, 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 13.sp, 
                                        modifier = Modifier.weight(1f).clickable { onUserClick(comment.user, app.platform, comment.userAvatar) }
                                    )
                                    if (comment.user == myProfile.value?.login) {
                                        IconButton(onClick = { commentToDelete = comment }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) }
                                    }
                                }
                                MarkdownBody(comment.body)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    val isLiked = comment.myReactionId != null
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { scope.launch { if (toggleReaction(token, app.owner, app.name, app.platform, comment.id, comment.myReactionId)) comments = fetchIssueComments(token, app.owner, app.name, app.platform, issue.number, myProfile.value?.login) } }
                                            .padding(6.dp)
                                    ) {
                                        Icon(
                                            if (isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                            null,
                                            Modifier.size(18.dp),
                                            tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                        if (comment.reactions > 0) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "${comment.reactions}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun CreateIssueDialog(app: OpenSourceApp, token: String, languageSetting: String, onDismiss: (Issue?) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var uploadingImage by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadingImage = true
            scope.launch {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val fileName = "issue_img_${System.currentTimeMillis()}.png"
                    if (uploadFileToPlatform(token, app.owner, app.name, ".opi_store/uploads/$fileName", base64, app.platform, languageSetting)) {
                        val rawUrl = if (app.platform == "GitHub") 
                            "https://raw.githubusercontent.com/${app.owner}/${app.name}/HEAD/.opi_store/uploads/$fileName"
                        else 
                            "https://codeberg.org/${app.owner}/${app.name}/raw/branch/main/.opi_store/uploads/$fileName"
                        body += "\n![]($rawUrl)\n"
                    }
                }
                uploadingImage = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss(null) },
        title = { Text(t("new_issue", languageSetting)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(t("title", languageSetting)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text(t("body", languageSetting)) }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                Spacer(Modifier.height(12.dp))

                if (uploadingImage) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                }
                val imageLinks = "!\\[.*?]\\((.*?)\\)".toRegex().findAll(body).map { it.groupValues[1] }.toList()
                if (imageLinks.isNotEmpty()) {
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        items(imageLinks) { url ->
                            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { photoPicker.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary) }
                    Text(t("upload_image", languageSetting), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = { Button(onClick = { scope.launch { val newIssue = createIssue(token, app.owner, app.name, app.platform, title, body); if (newIssue != null) onDismiss(newIssue) } }, enabled = title.isNotBlank() && !uploadingImage) { Text(t("post", languageSetting)) } },
        dismissButton = { TextButton(onClick = { onDismiss(null) }) { Text(t("cancel", languageSetting)) } }
    )
}

private suspend fun fetchIssues(token: String, owner: String, repo: String, platform: String, state: String): List<Issue> = withContext(Dispatchers.IO) {
    val list = mutableListOf<Issue>()
    try {
        val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/issues?state=$state" else "https://codeberg.org/api/v1/repos/$owner/$repo/issues?state=$state"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", auth).build()).execute().use { resp ->
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i); if (obj.has("pull_request")) continue
                val user = obj.getJSONObject("user")
                list.add(Issue(obj.getString("id"), obj.getInt("number"), obj.getString("title"), obj.optString("body", ""), obj.getString("state"), user.getString("login"), user.getString("avatar_url"), obj.optInt("comments", 0), obj.optString("node_id")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchIssueComments(token: String, owner: String, repo: String, platform: String, number: Int, myLogin: String?): List<IssueComment> = withContext(Dispatchers.IO) {
    val list = mutableListOf<IssueComment>()
    try {
        val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/issues/comments" else "https://codeberg.org/api/v1/repos/$owner/$repo/issues/$number/comments"
        // For GitHub, the structure is slightly different if we want reactions per comment. 
        // We'll use the issue-specific comment URL.
        val targetUrl = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/issues/$number/comments" else url
        
        httpClient.newCall(Request.Builder().url(targetUrl).addHeader("Authorization", auth).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext list
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i); val user = obj.getJSONObject("user")
                val reactions = if (obj.has("reactions")) obj.getJSONObject("reactions").optInt("total_count", 0) else 0
                
                // For GitHub, we can fetch reactions to see if "I" liked it. 
                // Simplified: we only check if the current user has a reaction ID.
                var myReactionId: String? = null
                if (platform == "GitHub" && myLogin != null) {
                    val reactionUrl = "https://api.github.com/repos/$owner/$repo/issues/comments/${obj.getString("id")}/reactions"
                    httpClient.newCall(Request.Builder().url(reactionUrl).addHeader("Authorization", auth).build()).execute().use { rResp ->
                        if (rResp.isSuccessful) {
                            val rArr = JSONArray(rResp.body?.string() ?: "[]")
                            for (j in 0 until rArr.length()) {
                                val rObj = rArr.getJSONObject(j)
                                if (rObj.getJSONObject("user").getString("login") == myLogin && rObj.getString("content") == "+1") {
                                    myReactionId = rObj.getString("id")
                                    break
                                }
                            }
                        }
                    }
                }
                
                list.add(IssueComment(obj.getString("id"), obj.getString("body"), user.getString("login"), user.getString("avatar_url"), reactions, myReactionId))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun postComment(token: String, owner: String, repo: String, platform: String, number: Int, body: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/issues/$number/comments" else "https://codeberg.org/api/v1/repos/$owner/$repo/issues/$number/comments"
        val json = JSONObject().apply { put("body", body) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url(url).post(json).addHeader("Authorization", auth).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun createIssue(token: String, owner: String, repo: String, platform: String, title: String, body: String): Issue? = withContext(Dispatchers.IO) {
    try {
        val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/issues" else "https://codeberg.org/api/v1/repos/$owner/$repo/issues"
        val json = JSONObject().apply { put("title", title); put("body", body) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url(url).post(json).addHeader("Authorization", auth).addHeader("User-Agent", "OpiStore").build()).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            if (resp.isSuccessful && bodyStr.isNotBlank()) {
                val obj = JSONObject(bodyStr)
                val user = obj.getJSONObject("user")
                Issue(obj.getString("id"), obj.getInt("number"), obj.getString("title"), obj.optString("body", ""), obj.getString("state"), user.getString("login"), user.getString("avatar_url"), 0, obj.optString("node_id"))
            } else null
        }
    } catch (e: Exception) { null }
}

private suspend fun deleteIssue(token: String, owner: String, repo: String, platform: String, issue: Issue): Boolean = withContext(Dispatchers.IO) {
    try {
        if (platform == "GitHub" && issue.nodeId != null) {
            val query = "mutation { deleteIssue(input: {issueId: \"${issue.nodeId}\"}) { clientMutationId } }"
            val json = JSONObject().apply { put("query", query) }.toString().toRequestBody("application/json".toMediaType())
            httpClient.newCall(Request.Builder().url("https://api.github.com/graphql").post(json).addHeader("Authorization", "Bearer $token").addHeader("User-Agent", "OpiStore").build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext false
                val body = resp.body?.string() ?: ""
                !body.contains("\"errors\":")
            }
        } else {
            val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
            val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/issues/${issue.number}" else "https://codeberg.org/api/v1/repos/$owner/$repo/issues/${issue.number}"
            httpClient.newCall(Request.Builder().url(url).delete().addHeader("Authorization", auth).addHeader("User-Agent", "OpiStore").build()).execute().use { it.isSuccessful }
        }
    } catch (e: Exception) { false }
}

private suspend fun deleteComment(token: String, owner: String, repo: String, platform: String, id: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/issues/comments/$id" else "https://codeberg.org/api/v1/repos/$owner/$repo/issues/comments/$id"
        httpClient.newCall(Request.Builder().url(url).delete().addHeader("Authorization", auth).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun toggleReaction(token: String, owner: String, repo: String, platform: String, commentId: String, currentReactionId: String?): Boolean = withContext(Dispatchers.IO) {
    try {
        if (platform == "GitHub") {
            if (currentReactionId != null) {
                // Delete reaction
                val url = "https://api.github.com/repos/$owner/$repo/issues/comments/$commentId/reactions/$currentReactionId"
                httpClient.newCall(Request.Builder().url(url).delete().addHeader("Authorization", "Bearer $token").build()).execute().use { it.isSuccessful }
            } else {
                // Add reaction
                val url = "https://api.github.com/repos/$owner/$repo/issues/comments/$commentId/reactions"
                val json = JSONObject().apply { put("content", "+1") }.toString().toRequestBody("application/json".toMediaType())
                httpClient.newCall(Request.Builder().url(url).post(json).addHeader("Authorization", "Bearer $token").addHeader("Accept", "application/vnd.github.squirrel-girl-preview+json").build()).execute().use { it.isSuccessful }
            }
        } else false
    } catch (e: Exception) { false }
}

private suspend fun fetchFilesFromPlatform(token: String, owner: String, repo: String, platform: String, path: String = ""): List<ProjectFile> = withContext(Dispatchers.IO) {
    val list = mutableListOf<ProjectFile>()
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val baseUrl = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents"
        val url = if (path.isEmpty()) baseUrl else "$baseUrl/$path"
        
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) { val obj = arr.getJSONObject(i); list.add(ProjectFile(obj.getString("name"), obj.getString("path"), obj.getString("type"), obj.getString("sha"), obj.optString("download_url", ""))) }
        }
    } catch (e: Exception) {}
    list.sortedBy { it.type == "file" }
}

private suspend fun fetchFileContent(token: String, owner: String, repo: String, path: String, platform: String): String? = withContext(Dispatchers.IO) {
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents/$path" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents/$path"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            val json = JSONObject(resp.body?.string() ?: "")
            String(Base64.decode(json.getString("content").replace("\n", ""), Base64.DEFAULT))
        }
    } catch (e: Exception) { null }
}

private suspend fun updateFileOnPlatform(token: String, owner: String, repo: String, path: String, content: String, sha: String, platform: String, languageSetting: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents/$path" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents/$path"
        val body = JSONObject().apply { put("message", t("edit_via_app", languageSetting)); put("content", Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP)); put("sha", sha) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url(url).put(body).addHeader("Authorization", authHeader).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun uploadFileToPlatform(token: String, owner: String, repo: String, path: String, content: String, platform: String, languageSetting: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents/$path" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents/$path"
        val body = JSONObject().apply { put("message", t("upload_via_app", languageSetting)); put("content", content) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url(url).put(body).addHeader("Authorization", authHeader).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun createGitHubRepo(token: String, name: String, desc: String, priv: Boolean): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject().apply { put("name", name); put("description", desc); put("private", priv); put("auto_init", true) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url("https://api.github.com/user/repos").post(body).addHeader("Authorization", "Bearer $token").build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun deleteGitHubRepo(token: String, owner: String, repo: String): Boolean = withContext(Dispatchers.IO) {
    try { httpClient.newCall(Request.Builder().url("https://api.github.com/repos/$owner/$repo").delete().addHeader("Authorization", "Bearer $token").build()).execute().use { it.isSuccessful } } catch (e: Exception) { false }
}

private suspend fun deleteCodebergRepo(token: String, owner: String, repo: String): Boolean = withContext(Dispatchers.IO) {
    try { httpClient.newCall(Request.Builder().url("https://codeberg.org/api/v1/repos/$owner/$repo").delete().addHeader("Authorization", "token $token").build()).execute().use { it.isSuccessful } } catch (e: Exception) { false }
}
