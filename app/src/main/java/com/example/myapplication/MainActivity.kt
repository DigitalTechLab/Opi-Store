package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import androidx.compose.foundation.background
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.draganddrop.*
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
data class AppRelease(val version: String, val downloadUrl: String, val isPreRelease: Boolean, val body: String = "", val downloadCount: Int = 0)
data class OpenSourceApp(
    val id: String, val name: String, val owner: String, val platform: String,
    val description: String, val repoUrl: String, val avatarUrl: String,
    val preFetchedReleases: List<AppRelease>? = null
)
data class UserProfile(val name: String, val login: String, val avatarUrl: String, val bio: String, val platform: String)
data class SimpleRepo(val name: String, val owner: String, val description: String, val htmlUrl: String, val stars: Int, val avatarUrl: String = "", val defaultBranch: String = "main")
data class ProjectFile(val name: String, val path: String, val type: String, val sha: String, val downloadUrl: String?)
data class DownloadInfo(val progress: String = "", val isDownloading: Boolean = false, val isDownloaded: Boolean = false)
data class Issue(val id: String, val number: Int, val title: String, val body: String, val state: String, val user: String, val userAvatar: String, val comments: Int, val nodeId: String? = null)
data class IssueComment(val id: String, val body: String, val user: String, val userAvatar: String, val reactions: Int, val myReactionId: String? = null)
data class PullRequest(val id: String, val number: Int, val title: String, val body: String, val state: String, val user: String, val userAvatar: String, val comments: Int, val createdAt: String, val headLabel: String? = null)
data class RepoStats(val stars: Int, val forks: Int, val watchers: Int, val language: String, val createdAt: String)
data class ReleaseAsset(val id: String, val name: String, val downloadUrl: String, val size: Long, val downloadCount: Int)
data class FullRelease(val id: String, val tagName: String, val name: String, val body: String, val isPreRelease: Boolean, val assets: List<ReleaseAsset>, val htmlUrl: String)


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
        "my_repositories" -> if (isDe) "Meine Repositories" else "My Repositories"
        "my_repository_editor" -> if (isDe) "Mein Repository Editor" else "My Repository Editor"
        "starred" -> if (isDe) "Markiert (Starred)" else "Starred"
        "login_title" -> if (isDe) "Login" else "Login"
        "personal_access_token" -> if (isDe) "Personal Access Token" else "Personal Access Token"
        "search_apps_hint" -> if (isDe) "Suche Apps..." else "Search apps..."
        "no_apps" -> if (isDe) "Keine Apps." else "No apps."
        "delete" -> if (isDe) "Löschen" else "Delete"
        "rename" -> if (isDe) "Umbenennen" else "Rename"
        "rename_project_title" -> if (isDe) "Projekt umbenennen" else "Rename Project"
        "new_name" -> if (isDe) "Neuer Name" else "New Name"
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
        "changelog" -> if (isDe) "Changelog" else "Changelog"
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
        "updates" -> if (isDe) "Updates" else "Updates"
        "update_available" -> if (isDe) "Update verfügbar" else "Update available"
        "update_now" -> if (isDe) "Jetzt aktualisieren" else "Update now"
        "current_version" -> if (isDe) "Aktuelle Version" else "Current version"
        "new_version" -> if (isDe) "Neue Version" else "New version"
        "download_complete" -> if (isDe) "Download abgeschlossen" else "Download complete"
        "automatic_updates" -> if (isDe) "Nach Updates suchen" else "Check for updates"
        "on" -> if (isDe) "An" else "On"
        "off" -> if (isDe) "Aus" else "Off"
        "later" -> if (isDe) "Vielleicht später" else "Maybe later"
        "changelog_title" -> if (isDe) "Was ist neu?" else "What's new?"
        "repo_stats" -> if (isDe) "Repository Statistiken" else "Repository Statistics"
        "stars" -> if (isDe) "Sterne" else "Stars"
        "forks" -> if (isDe) "Forks" else "Forks"
        "watchers" -> if (isDe) "Beobachter" else "Watchers"
        "created_at" -> if (isDe) "Erstellt am" else "Created at"
        "total_downloads" -> if (isDe) "Gesamte Downloads" else "Total downloads"
        "downloads_per_version" -> if (isDe) "Downloads pro Version" else "Downloads per version"
        "pull_requests" -> if (isDe) "Pull Requests" else "Pull Requests"
        "select_action" -> if (isDe) "Aktion auswählen" else "Select action"
        "editor" -> if (isDe) "Editor" else "Editor"
        "merge" -> if (isDe) "Mergen" else "Merge"
        "merge_confirm" -> if (isDe) "Möchtest du diesen Pull Request wirklich mergen? Die Änderungen werden in den Code übernommen." else "Do you really want to merge this pull request? The changes will be applied to the code."
        "yes_merge" -> if (isDe) "Ja, Mergen" else "Yes, merge"
        "no_pull_requests" -> if (isDe) "Keine Pull Requests gefunden." else "No pull requests found."
        "merged" -> if (isDe) "Gemerged" else "Merged"
        "category_appearance" -> if (isDe) "Aussehen" else "Appearance"
        "category_system" -> if (isDe) "System & Updates" else "System & Updates"
        "auto_delete_apk" -> if (isDe) "APK Auto-Löschen" else "Auto-delete APK"
        "searching_updates" -> if (isDe) "Suche nach Updates..." else "Checking for updates..."
        "no_update_found" -> if (isDe) "Kein Update gefunden." else "No update found."
        "update_available_msg" -> if (isDe) "Update verfügbar!" else "Update available!"
        "update_button" -> if (isDe) "Jetzt Aktualisieren" else "Update Now"
        "source_code" -> if (isDe) "Quellcode" else "Source Code"
        "no_access" -> if (isDe) "Kein Zugriff auf die Datei." else "No access to this file."
        "releases" -> if (isDe) "Releases" else "Releases"
        "edit_release" -> if (isDe) "Release bearbeiten" else "Edit Release"
        "assets" -> if (isDe) "Dateien" else "Assets"
        "add_asset" -> if (isDe) "Datei hinzufügen" else "Add Asset"
        "tag_name" -> if (isDe) "Tag Name" else "Tag Name"
        "release_title" -> if (isDe) "Release Titel" else "Release Title"
        "delete_release" -> if (isDe) "Release löschen" else "Delete Release"
        "delete_release_confirm" -> if (isDe) "Möchtest du diesen Release wirklich löschen?" else "Do you really want to delete this release?"
        "edit_changelog" -> if (isDe) "Beschreibung bearbeiten" else "Edit Description"
        "select_tag" -> if (isDe) "Tag auswählen" else "Select Tag"
        else -> key
    }
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    var githubToken by rememberSaveable { mutableStateOf(sharedPrefs.getString("GITHUB_TOKEN", "") ?: "") }
    var codebergToken by rememberSaveable { mutableStateOf(sharedPrefs.getString("CODEBERG_TOKEN", "") ?: "") }
    var languageSetting by remember { mutableStateOf(sharedPrefs.getString("LANGUAGE_SETTING", "en") ?: "en") }

    var updatesEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("UPDATES_ENABLED", true)) }
    var autoDeleteApk by remember { mutableStateOf(sharedPrefs.getBoolean("AUTO_DELETE_APK", false)) }
    var apkListRefreshTrigger by remember { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<OpenSourceApp>>(emptyList()) }
    var searchIsLoading by remember { mutableStateOf(false) }
    var searchStatusMessage by remember { mutableStateOf("") }
    var skippedVersion by remember { mutableStateOf(sharedPrefs.getString("SKIPPED_VERSION", "") ?: "") }
    var showUpdateScreen by remember { mutableStateOf<AppRelease?>(null) }

    val activeDownloads = remember { mutableStateMapOf<String, DownloadInfo>() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        try {
            val file = java.io.File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (file.exists()) file.delete()
        } catch (e: Exception) {}

        // Cleanup orphaned APKs from mapping
        val mapping = sharedPrefs.getStringSet("APK_DELETE_MAPPING", emptySet()) ?: emptySet()
        if (mapping.isNotEmpty()) {
            val toRemove = mutableSetOf<String>()
            mapping.forEach { entry ->
                val parts = entry.split("|")
                if (parts.size == 2) {
                    val pkg = parts[0]
                    val path = parts[1]
                    try {
                        context.packageManager.getPackageInfo(pkg, 0)
                        val file = File(path)
                        if (file.exists()) file.delete()
                        toRemove.add(entry)
                    } catch (e: Exception) {}
                }
            }
            if (toRemove.isNotEmpty()) {
                val newMapping = mapping.toMutableSet().apply { removeAll(toRemove) }
                sharedPrefs.edit().putStringSet("APK_DELETE_MAPPING", newMapping).apply()
            }
        }
    }

    DisposableEffect(autoDeleteApk) {
        if (!autoDeleteApk) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_ADDED || intent.action == Intent.ACTION_PACKAGE_REPLACED) {
                    val packageName = intent.data?.schemeSpecificPart ?: return
                    val mapping = sharedPrefs.getStringSet("APK_DELETE_MAPPING", emptySet()) ?: emptySet()
                    val entry = mapping.find { it.startsWith("$packageName|") }
                    if (entry != null) {
                        val path = entry.substringAfter("|")
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                            apkListRefreshTrigger++
                        }
                        val newMapping = mapping.toMutableSet().apply { remove(entry) }
                        sharedPrefs.edit().putStringSet("APK_DELETE_MAPPING", newMapping).apply()
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { try { context.unregisterReceiver(receiver) } catch (e: Exception) {} }
    }

    LaunchedEffect(updatesEnabled) {
        if (updatesEnabled) {
            val latest = checkAppUpdate()
            if (latest != null) {
                val currentVer = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
                } catch (e: Exception) { "0.0" }

                if (isNewerVersion(currentVer, latest.version) && latest.version != skippedVersion) {
                    showUpdateScreen = latest
                }
            }
        }
    }

    var selectedAppForDetail by remember { mutableStateOf<OpenSourceApp?>(null) }
    var showFullRepoListForUser by remember { mutableStateOf<String?>(null) }
    var fullRepoListPlatform by remember { mutableStateOf("GitHub") }
    var fullRepoListAppsOnly by remember { mutableStateOf(false) }
    var fullRepoListAvatarUrl by remember { mutableStateOf("") }
    var isViewingOwnProfile by remember { mutableStateOf(false) }
    var selectedRepoForFiles by remember { mutableStateOf<SimpleRepo?>(null) }
    var selectedRepoForPRs by remember { mutableStateOf<SimpleRepo?>(null) }
    var selectedFileForEdit by remember { mutableStateOf<ProjectFile?>(null) }
    var selectedBranch by remember { mutableStateOf<String?>(null) }

    var selectedAppForSourceCode by remember { mutableStateOf<OpenSourceApp?>(null) }
    var selectedFileForView by remember { mutableStateOf<ProjectFile?>(null) }
    var selectedSourceBranch by remember { mutableStateOf<String?>(null) }

    var selectedRepoForReleases by remember { mutableStateOf<SimpleRepo?>(null) }
    var selectedReleaseForEdit by remember { mutableStateOf<FullRelease?>(null) }

    if (showUpdateScreen != null) {
        UpdateScreen(
            currentVersion = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0" } catch (e: Exception) { "0.0" },
            newVersion = showUpdateScreen!!.version,
            changelog = showUpdateScreen!!.body,
            downloadUrl = showUpdateScreen!!.downloadUrl,
            onCancel = {
                skippedVersion = showUpdateScreen!!.version
                sharedPrefs.edit().putString("SKIPPED_VERSION", skippedVersion).apply()
                showUpdateScreen = null
            },
            languageSetting = languageSetting,
            context = context,
            scope = scope
        )
    } else if (selectedAppForSourceCode != null) {
        Box {
            SourceCodeBrowserScreen(app = selectedAppForSourceCode!!, token = if (selectedAppForSourceCode!!.platform == "GitHub") githubToken else codebergToken, onBack = { selectedAppForSourceCode = null; selectedSourceBranch = null }, onFileClick = { if (it.type == "file") selectedFileForView = it }, languageSetting = languageSetting, selectedBranch = selectedSourceBranch, onBranchChange = { selectedSourceBranch = it })
            if (selectedFileForView != null) {
                SourceCodeViewerScreen(app = selectedAppForSourceCode!!, file = selectedFileForView!!, token = if (selectedAppForSourceCode!!.platform == "GitHub") githubToken else codebergToken, onBack = { selectedFileForView = null }, languageSetting = languageSetting, branch = selectedSourceBranch)
            }
        }
    } else if (selectedRepoForFiles != null) {
        Box {
            ProjectFilesScreen(repo = selectedRepoForFiles!!, token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, onBack = { selectedRepoForFiles = null; selectedBranch = null }, onFileClick = { if (it.type == "file") selectedFileForEdit = it }, languageSetting = languageSetting, selectedBranch = selectedBranch, onBranchChange = { selectedBranch = it })
            if (selectedFileForEdit != null) {
                CodeEditorScreen(repo = selectedRepoForFiles!!, file = selectedFileForEdit!!, token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, onBack = { selectedFileForEdit = null }, languageSetting = languageSetting, branch = selectedBranch)
            }
        }
    } else if (selectedReleaseForEdit != null && selectedRepoForReleases != null) {
        ReleaseEditScreen(repo = selectedRepoForReleases!!, release = selectedReleaseForEdit!!, token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, onBack = { selectedReleaseForEdit = null }, languageSetting = languageSetting)
    } else if (selectedRepoForReleases != null) {
        ReleasesManagementScreen(repo = selectedRepoForReleases!!, token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, onBack = { selectedRepoForReleases = null }, onReleaseClick = { selectedReleaseForEdit = it }, languageSetting = languageSetting)
    } else if (selectedRepoForPRs != null) {
        PullRequestsScreen(app = OpenSourceApp(id = "${selectedRepoForPRs!!.owner}/${selectedRepoForPRs!!.name}", name = selectedRepoForPRs!!.name, owner = selectedRepoForPRs!!.owner, platform = fullRepoListPlatform, description = selectedRepoForPRs!!.description, repoUrl = selectedRepoForPRs!!.htmlUrl, avatarUrl = ""), githubToken = githubToken, codebergToken = codebergToken, languageSetting = languageSetting, onUserClick = { user, plat, avatar -> showFullRepoListForUser = user; fullRepoListPlatform = plat; fullRepoListAvatarUrl = avatar; fullRepoListAppsOnly = true; isViewingOwnProfile = false; selectedRepoForPRs = null }, onDismiss = { selectedRepoForPRs = null })
    } else if (selectedRepoForFiles != null) {
        ProjectFilesScreen(repo = selectedRepoForFiles!!, token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken, platform = fullRepoListPlatform, onBack = { selectedRepoForFiles = null; selectedBranch = null }, onFileClick = { if (it.type == "file") selectedFileForEdit = it }, languageSetting = languageSetting, selectedBranch = selectedBranch, onBranchChange = { selectedBranch = it })
    } else if (showFullRepoListForUser != null) {
        FullRepoListScreen(
            owner = showFullRepoListForUser!!,
            title = if (fullRepoListAppsOnly) "${t("by", languageSetting)} $showFullRepoListForUser" else if (isViewingOwnProfile) t("my_repository_editor", languageSetting) else t("my_repositories", languageSetting),
            token = if (fullRepoListPlatform == "GitHub") githubToken else codebergToken,
            platform = fullRepoListPlatform,
            appsOnly = fullRepoListAppsOnly,
            isOwnProfile = isViewingOwnProfile,
            ownerAvatarUrl = fullRepoListAvatarUrl,
            onBack = { showFullRepoListForUser = null },
            onAppSelected = { showFullRepoListForUser = null; selectedAppForDetail = it },
            onRepoSelected = { selectedBranch = it.defaultBranch; selectedRepoForFiles = it },
            onRepoSelectedPR = { selectedRepoForPRs = it },
            onRepoSelectedReleases = { selectedRepoForReleases = it },
            githubToken = githubToken,
            codebergToken = codebergToken,
            languageSetting = languageSetting
        )
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
            globalScope = scope,
            onSourceCodeClick = { app, branch -> selectedSourceBranch = branch; selectedAppForSourceCode = app }
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
                    0 -> SearchScreen(
                        githubToken = githubToken,
                        codebergToken = codebergToken,
                        onAppSelected = { selectedAppForDetail = it },
                        onUserSelected = { owner, platform, avatar ->
                            fullRepoListPlatform = platform
                            fullRepoListAppsOnly = true
                            fullRepoListAvatarUrl = avatar
                            isViewingOwnProfile = false
                            showFullRepoListForUser = owner
                        },
                        languageSetting = languageSetting,
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        searchResults = searchResults,
                        onSearchResultsChange = { searchResults = it },
                        isLoading = searchIsLoading,
                        onIsLoadingChange = { searchIsLoading = it },
                        statusMessage = searchStatusMessage,
                        onStatusMessageChange = { searchStatusMessage = it }
                    )
                    1 -> ApkManagerScreen(githubToken = githubToken, codebergToken = codebergToken, languageSetting = languageSetting, activeDownloads = activeDownloads, globalScope = scope, refreshTrigger = apkListRefreshTrigger)
                    2 -> AccountMultiScreen(githubToken = githubToken, codebergToken = codebergToken, onGithubTokenSaved = { t -> githubToken = t; sharedPrefs.edit().putString("GITHUB_TOKEN", t).apply() }, onCodebergTokenSaved = { t -> codebergToken = t; sharedPrefs.edit().putString("CODEBERG_TOKEN", t).apply() }, onUsernameClick = { login, platform -> fullRepoListPlatform = platform; fullRepoListAppsOnly = false; fullRepoListAvatarUrl = ""; isViewingOwnProfile = true; showFullRepoListForUser = login }, languageSetting = languageSetting, onAppSelected = { selectedAppForDetail = it })
                    3 -> SettingsScreen(
                        currentTheme = themeSetting.value,
                        onThemeChange = { themeSetting.value = it; sharedPrefs.edit().putString("THEME_SETTING", it).apply() },
                        currentLanguage = languageSetting,
                        onLanguageChange = { languageSetting = it; sharedPrefs.edit().putString("LANGUAGE_SETTING", it).apply() },
                        updatesEnabled = updatesEnabled,
                        onUpdatesEnabledChange = { updatesEnabled = it; sharedPrefs.edit().putBoolean("UPDATES_ENABLED", it).apply() },
                        autoDeleteApk = autoDeleteApk,
                        onAutoDeleteApkChange = { autoDeleteApk = it; sharedPrefs.edit().putBoolean("AUTO_DELETE_APK", it).apply() },
                        languageSetting = languageSetting
                    )
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
    updatesEnabled: Boolean,
    onUpdatesEnabledChange: (Boolean) -> Unit,
    autoDeleteApk: Boolean,
    onAutoDeleteApkChange: (Boolean) -> Unit,
    languageSetting: String
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAutoDeleteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text(
            text = t("settings_title", languageSetting),
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // CATEGORY: APPEARANCE
        Text(
            text = t("category_appearance", languageSetting),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
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

        Spacer(modifier = Modifier.height(32.dp))

        // CATEGORY: SYSTEM
        Text(
            text = t("category_system", languageSetting),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )

        // Updates Button
        Button(
            onClick = { showUpdateDialog = true },
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
                    Icon(Icons.Default.Update, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(t("updates", languageSetting), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Text(if (updatesEnabled) t("on", languageSetting) else t("off", languageSetting), color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Auto Delete APK Button
        Button(
            onClick = { showAutoDeleteDialog = true },
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
                    Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(t("auto_delete_apk", languageSetting), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Text(if (autoDeleteApk) t("on", languageSetting) else t("off", languageSetting), color = MaterialTheme.colorScheme.primary)
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

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(t("automatic_updates", languageSetting)) },
            text = {
                Column {
                    listOf(true to t("on", languageSetting), false to t("off", languageSetting)).forEach { (enabled, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onUpdatesEnabledChange(enabled); showUpdateDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = updatesEnabled == enabled, onClick = { onUpdatesEnabledChange(enabled); showUpdateDialog = false })
                            Spacer(Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showAutoDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showAutoDeleteDialog = false },
            title = { Text(t("auto_delete_apk", languageSetting)) },
            text = {
                Column {
                    listOf(true, false).forEach { enabled ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onAutoDeleteApkChange(enabled); showAutoDeleteDialog = false }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = autoDeleteApk == enabled, onClick = { onAutoDeleteApkChange(enabled); showAutoDeleteDialog = false })
                            Spacer(Modifier.width(12.dp))
                            Text(if (enabled) t("on", languageSetting) else t("off", languageSetting))
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun AccountMultiScreen(githubToken: String, codebergToken: String, onGithubTokenSaved: (String) -> Unit, onCodebergTokenSaved: (String) -> Unit, onUsernameClick: (String, String) -> Unit, languageSetting: String, onAppSelected: (OpenSourceApp) -> Unit) {
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
fun AccountScreenDetails(token: String, platformName: String, onTokenSaved: (String) -> Unit, fetchProfile: suspend (String) -> UserProfile?, fetchRepos: suspend (String) -> List<SimpleRepo>, fetchStarred: suspend (String) -> List<SimpleRepo>, updateProfile: suspend (String, String, String) -> Boolean, onUsernameClick: (String, String) -> Unit, languageSetting: String, onAppSelected: (OpenSourceApp) -> Unit) {
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
                    Text("@${userProfile!!.login}", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onUsernameClick(userProfile!!.login, platformName) }.padding(horizontal = 4.dp))
                    if (userProfile!!.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = userProfile!!.bio,
                            fontSize = 14.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    editNameInput = userProfile!!.name.ifBlank { userProfile!!.login }
                                    editBioInput = userProfile!!.bio
                                    showEditProfileDialog = true
                                }
                                .padding(horizontal = 4.dp)
                        )
                    }
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
fun SearchScreen(githubToken: String, codebergToken: String, onAppSelected: (OpenSourceApp) -> Unit, onUserSelected: (String, String, String) -> Unit, languageSetting: String, query: String, onQueryChange: (String) -> Unit, searchResults: List<OpenSourceApp>, onSearchResultsChange: (List<OpenSourceApp>) -> Unit, isLoading: Boolean, onIsLoadingChange: (Boolean) -> Unit, statusMessage: String, onStatusMessageChange: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    fun performSearch(searchQuery: String) {
        onQueryChange(searchQuery); onIsLoadingChange(true); onSearchResultsChange(emptyList())
        scope.launch {
            try {
                if (searchQuery.startsWith("http")) {
                    val platform = if (searchQuery.contains("codeberg.org")) "Codeberg" else "GitHub"
                    val domain = if (platform == "GitHub") "github.com/" else "codeberg.org/"
                    val parts = searchQuery.substringAfter(domain).split("/").filter { it.isNotBlank() }

                    if (parts.size == 1) {
                        val username = parts[0]
                        val avatar = if (platform == "GitHub") "https://github.com/$username.png" else ""
                        onUserSelected(username, platform, avatar)
                        onIsLoadingChange(false)
                        return@launch
                    }

                    onStatusMessageChange(t("status_loading_repo", languageSetting))
                    val app = fetchAppFromUrl(searchQuery, githubToken, codebergToken)
                    if (app != null) {
                        val releases = fetchReleasesForApp(app, githubToken, codebergToken)
                        if (releases.isNotEmpty()) {
                            onAppSelected(app.copy(preFetchedReleases = releases))
                            onStatusMessageChange("")
                        } else onStatusMessageChange(t("status_no_apks", languageSetting))
                    } else onStatusMessageChange(t("status_no_project", languageSetting))
                } else {
                    onStatusMessageChange(t("status_searching", languageSetting))
                    val results = searchMultiSourceApps(searchQuery, githubToken, codebergToken)
                    onSearchResultsChange(results)
                    onStatusMessageChange(if (results.isEmpty()) t("status_no_apps_found", languageSetting) else "")
                }
            } catch (e: Exception) { onStatusMessageChange(t("status_error", languageSetting)) } finally { onIsLoadingChange(false) }
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(t("search_title", languageSetting), fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = { onQueryChange(it) }, label = { Text(t("search_hint", languageSetting)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { IconButton(onClick = { if (query.isNotBlank()) performSearch(query) }) { Icon(Icons.Default.Search, null) } }); Spacer(modifier = Modifier.height(16.dp))
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

@Composable
fun ImageZoomScreen(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxX = (maxWidthPx * (scale - 1)) / 2
                                val maxY = (maxHeightPx * (scale - 1)) / 2
                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
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
    globalScope: CoroutineScope,
    onSourceCodeClick: (OpenSourceApp, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    var releases by remember { mutableStateOf<List<AppRelease>?>(null) }; var isLoadingReleases by remember { mutableStateOf(true) }
    var selectedRelease by remember { mutableStateOf<AppRelease?>(null) }; var showVersionSheet by remember { mutableStateOf(false) }

    var showIssues by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showImageZoom by remember { mutableStateOf(false) }
    var isPrefetchingSource by remember { mutableStateOf(false) }

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
        if (isPrefetchingSource) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            IconButton(onClick = {
                isPrefetchingSource = true
                scope.launch {
                    val token = if (app.platform == "GitHub") githubToken else codebergToken
                    val repoInfo = fetchSingleRepoInfo(app.owner, app.name, token, app.platform)
                    onSourceCodeClick(app, repoInfo?.defaultBranch)
                    isPrefetchingSource = false
                }
            }) { Icon(Icons.Default.Code, t("source_code", languageSetting)) }
        }
        IconButton(onClick = { showStats = true }) { Icon(Icons.Default.Info, null) }
        IconButton(onClick = { showIssues = true }) { Icon(Icons.Default.BugReport, null) }
    }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(model = app.avatarUrl, contentDescription = "Logo", contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp).clip(CircleShape).clickable { showImageZoom = true })
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

                        if (selectedRelease!!.body.isNotBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(t("changelog", languageSetting), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                MarkdownBody(text = selectedRelease!!.body, modifier = Modifier.padding(16.dp))
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

    if (showStats) {
        RepoStatsScreen(
            app = app,
            githubToken = githubToken,
            codebergToken = codebergToken,
            languageSetting = languageSetting,
            onDismiss = { showStats = false }
        )
    }

    if (showVersionSheet && releases != null) {
        ModalBottomSheet(
            onDismissRequest = { showVersionSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    val (stable, pre) = releases!!.partition { !it.isPreRelease }
                    if (stable.isNotEmpty()) {
                        item { Text(t("stable_releases_header", languageSetting), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) }
                        items(stable) { VersionItem(it, it == selectedRelease) { selectedRelease = it; showVersionSheet = false } }
                    }
                    if (pre.isNotEmpty()) {
                        item { Spacer(modifier = Modifier.height(12.dp)); Text(t("pre_releases_header", languageSetting), color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) }
                        items(pre) { VersionItem(it, it == selectedRelease) { selectedRelease = it; showVersionSheet = false } }
                    }
                }
            }
        }
    }
    if (showImageZoom) {
        ImageZoomScreen(imageUrl = app.avatarUrl, onDismiss = { showImageZoom = false })
    }
}

@Composable
fun VersionItem(release: AppRelease, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = release.version,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkManagerScreen(githubToken: String, codebergToken: String, languageSetting: String, activeDownloads: MutableMap<String, DownloadInfo>, globalScope: CoroutineScope, refreshTrigger: Int) {
    val context = LocalContext.current
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFileForDialog by remember { mutableStateOf<File?>(null) }
    var isRepairingLocal by remember { mutableStateOf(false) }
    var repairProgressLocal by remember { mutableStateOf("") }
    var currentFileValid by remember { mutableStateOf(true) }
    var showUpdatePanel by remember { mutableStateOf<File?>(null) }

    fun refreshFiles() { val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS); downloadedFiles = dir?.listFiles { _, name -> name.endsWith(".apk") && name != "update.apk" }?.toList() ?: emptyList() }
    LaunchedEffect(Unit, refreshTrigger) { refreshFiles() }

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
            if (!isDownloadingGlobal && file.exists()) {
                currentFileValid = isApkValid(context, file)
            }
        }
        val avatarUrl = when (platform) { "GitHub" -> "https://github.com/$owner.png"; "Codeberg" -> "https://codeberg.org/assets/img/logo.png"; else -> null }
        val apkIcon = remember(file) { getApkIcon(context, file) }

        AlertDialog(onDismissRequest = { if (!isRepairingLocal) selectedFileForDialog = null }, confirmButton = {}, title = null, text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (apkIcon != null) {
                    AsyncImage(model = apkIcon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)))
                } else if (avatarUrl != null) {
                    AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)))
                } else {
                    Icon(Icons.Default.Android, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                }
                Spacer(modifier = Modifier.height(16.dp)); Text(appName.ifBlank { file.name }, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                if (version.isNotBlank()) Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 4.dp).clickable { showUpdatePanel = file }) { Text("${t("version", languageSetting)} $version", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                if (!currentFileValid && !isDownloadingGlobal && file.exists()) {
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
            val apkIcon = remember(file) { if (isValid && !isDownloading) getApkIcon(context, file) else null }
            Card(modifier = Modifier.fillMaxWidth().clickable { selectedFileForDialog = file }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))) {
                    if (apkIcon != null) {
                        AsyncImage(model = apkIcon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else if (avatarUrl != null) {
                        AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.Android, null, modifier = Modifier.fillMaxSize(), tint = Color.Gray)
                    }
                }
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

    if (showUpdatePanel != null) {
        val file = showUpdatePanel!!
        val fileName = file.name
        val parts = fileName.removeSuffix(".apk").split("_")
        val platform = parts.getOrNull(0) ?: ""
        val owner = parts.getOrNull(1) ?: ""
        val appName = parts.getOrNull(2) ?: ""
        val version = parts.getOrNull(3) ?: ""

        var latestRelease by remember { mutableStateOf<AppRelease?>(null) }
        var isSearching by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            val rels = if (platform == "GitHub") fetchGitHubReleases(owner, appName, githubToken)
            else if (platform == "Codeberg") fetchCodebergReleases(owner, appName, codebergToken)
            else emptyList()
            
            val currentRelease = rels.find { it.version == version }
            val currentIsPre = currentRelease?.isPreRelease ?: false
            
            latestRelease = if (currentIsPre) {
                // If current is pre-release, look for the newest release of any kind
                rels.firstOrNull()
            } else {
                // If current is stable, look only for the newest stable release
                rels.firstOrNull { !it.isPreRelease } ?: rels.firstOrNull()
            }
            isSearching = false
        }

        Dialog(onDismissRequest = { showUpdatePanel = null }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(t("updates", languageSetting), fontWeight = FontWeight.Bold) },
                        navigationIcon = { IconButton(onClick = { showUpdatePanel = null }) { Icon(Icons.Default.Close, null) } }
                    )
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isSearching) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(16.dp))
                                Text(t("searching_updates", languageSetting), color = Color.Gray)
                            }
                        }
                    } else if (latestRelease != null) {
                        val lr = latestRelease!!
                        val isNewer = isNewerVersion(version, lr.version)
                        
                        Column(modifier = Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(if(isNewer) Icons.Default.NewReleases else Icons.Default.CheckCircle, null, modifier = Modifier.size(72.dp), tint = if(isNewer) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50))
                            Spacer(Modifier.height(16.dp))
                            Text(if (isNewer) t("update_available_msg", languageSetting) else t("no_update_found", languageSetting), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
                                Text(t("current_version", languageSetting), fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(version, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if(isNewer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) {
                                Text(t("new_version", languageSetting), fontSize = 11.sp, color = if(isNewer) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                                Text(lr.version, fontSize = 18.sp, fontWeight = FontWeight.Black, color = if(isNewer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        if (lr.body.isNotBlank()) {
                            Spacer(Modifier.height(32.dp))
                            Text(t("changelog", languageSetting), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(16.dp).verticalScroll(rememberScrollState())) {
                                MarkdownBody(lr.body)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        if (isNewer) {
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    showUpdatePanel = null
                                    selectedFileForDialog = null
                                    activeDownloads[fileName] = DownloadInfo(isDownloading = true)
                                    globalScope.launch {
                                        val newFileName = "${platform}_${owner}_${appName}_${lr.version}.apk"
                                        val newFile = downloadApk(context, newFileName, lr.downloadUrl, if(platform=="GitHub") githubToken else codebergToken, languageSetting) {
                                            activeDownloads[fileName] = DownloadInfo(progress = it, isDownloading = true)
                                        }
                                        if (newFile != null) {
                                            val wasSelected = selectedFileForDialog == file
                                            file.delete()
                                            withContext(Dispatchers.Main) {
                                                if (wasSelected) selectedFileForDialog = newFile
                                                refreshFiles()
                                                activeDownloads.remove(fileName)
                                            }
                                        } else {
                                            activeDownloads.remove(fileName)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(12.dp))
                                Text(t("update_button", languageSetting), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(t("no_update_found", languageSetting), color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullRepoListScreen(owner: String, title: String, token: String, platform: String, appsOnly: Boolean = false, isOwnProfile: Boolean = false, ownerAvatarUrl: String = "", onBack: () -> Unit, onAppSelected: (OpenSourceApp) -> Unit = {}, onRepoSelected: (SimpleRepo) -> Unit = {}, onRepoSelectedPR: (SimpleRepo) -> Unit = {}, onRepoSelectedReleases: (SimpleRepo) -> Unit = {}, githubToken: String = "", codebergToken: String = "", languageSetting: String) {
    val scope = rememberCoroutineScope()
    BackHandler { onBack() }
    var repos by remember { mutableStateOf<List<SimpleRepo>>(emptyList()) }; var isLoading by remember { mutableStateOf(true) }
    var currentAvatarUrl by remember { mutableStateOf(ownerAvatarUrl) }; var statusText by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }; var repoToDelete by remember { mutableStateOf<SimpleRepo?>(null) }
    var repoToRename by remember { mutableStateOf<SimpleRepo?>(null) }; var renameRepoNameInput by remember { mutableStateOf("") }
    var newRepoName by remember { mutableStateOf("") }; var newRepoDesc by remember { mutableStateOf("") }; var isPrivate by remember { mutableStateOf(false) }
    fun refresh() { scope.launch {
        isLoading = true; statusText = if (appsOnly) t("search_apps_hint", languageSetting) else ""
        if (currentAvatarUrl.isEmpty()) { (if (platform == "GitHub") fetchGitHubProfile(token, owner) else fetchCodebergProfile(token, owner))?.let { currentAvatarUrl = it.avatarUrl } }
        val all = if (platform == "GitHub") fetchGitHubReposForOwner(owner, token, isOwnProfile) else fetchCodebergReposForOwner(owner, token, isOwnProfile)
        if (appsOnly) { repos = all.map { repo -> async { if (fetchReleasesForApp(OpenSourceApp(id = "${repo.owner}/${repo.name}", name = repo.name, owner = repo.owner, platform = platform, description = repo.description, repoUrl = repo.htmlUrl, avatarUrl = currentAvatarUrl), githubToken, codebergToken).isNotEmpty()) repo else null } }.awaitAll().filterNotNull(); if (repos.isEmpty()) statusText = t("no_apps", languageSetting) } else repos = all
        isLoading = false
    } }
    LaunchedEffect(Unit) { refresh() }
    var showActionChoice by remember { mutableStateOf<SimpleRepo?>(null) }
    
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, t("cancel", languageSetting)) } }, actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, null) } }) },
        floatingActionButton = { if (isOwnProfile && platform == "GitHub") FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        if (isLoading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); if (statusText.isNotEmpty()) { Spacer(Modifier.height(16.dp)); Text(statusText, color = Color.Gray) } } }
        else if (repos.isEmpty() && statusText.isNotEmpty()) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(statusText, color = Color.Gray) }
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) { items(repos, key = { it.htmlUrl }) { repo ->
            if (isOwnProfile) {
                val state = rememberSwipeToDismissBoxState(confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        if (repoToRename == null) { repoToDelete = repo; false } else false
                    } else if (it == SwipeToDismissBoxValue.StartToEnd) {
                        if (repoToDelete == null) { repoToRename = repo; renameRepoNameInput = repo.name; false } else false
                    } else false
                })
                SwipeToDismissBox(state = state, backgroundContent = {
                    val direction = state.dismissDirection
                    val color = when (direction) {
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF5350)
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                        else -> Color.Transparent
                    }
                    Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(color)) {
                        if (direction == SwipeToDismissBoxValue.EndToStart) {
                            Row(modifier = Modifier.align(Alignment.CenterEnd).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(t("delete", languageSetting), color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Icon(Icons.Default.Delete, null, tint = Color.White)
                            }
                        } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Row(modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, null, tint = Color.White); Spacer(Modifier.width(12.dp)); Text(t("rename", languageSetting), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }) { RepoCard(repo, languageSetting = languageSetting) { showActionChoice = repo } }
            }
else RepoCard(repo, languageSetting = languageSetting) { onAppSelected(OpenSourceApp(id = "${repo.owner}/${repo.name}", name = repo.name, owner = repo.owner, platform = platform, description = repo.description, repoUrl = repo.htmlUrl, avatarUrl = currentAvatarUrl)) }
            Spacer(Modifier.height(8.dp))
        } }
        if (showCreateDialog) AlertDialog(onDismissRequest = { showCreateDialog = false }, title = { Text(t("new_github_project", languageSetting)) }, text = { Column { OutlinedTextField(value = newRepoName, onValueChange = { newRepoName = it }, label = { Text(t("project_name", languageSetting)) }); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = newRepoDesc, onValueChange = { newRepoDesc = it }, label = { Text(t("description", languageSetting)) }); Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isPrivate, onCheckedChange = { isPrivate = it }); Text(t("private_repository", languageSetting)) } } }, confirmButton = { Button(onClick = { scope.launch { if (createGitHubRepo(token, newRepoName, newRepoDesc, isPrivate)) { showCreateDialog = false; refresh() } } }) { Text(t("create", languageSetting)) } }, dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(t("cancel", languageSetting)) } })
        if (repoToDelete != null) AlertDialog(onDismissRequest = { repoToDelete = null }, title = { Text(t("delete_project_title", languageSetting)) }, text = { Text(t("delete_project_confirm", languageSetting).replace("%s", repoToDelete!!.name)) }, confirmButton = { Button(onClick = { val r = repoToDelete!!; repoToDelete = null; scope.launch { if (if (platform == "GitHub") deleteGitHubRepo(token, owner, r.name) else deleteCodebergRepo(token, owner, r.name)) refresh() } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(t("yes_delete", languageSetting)) } }, dismissButton = { TextButton(onClick = { repoToDelete = null }) { Text(t("cancel", languageSetting)) } })
        if (repoToRename != null) AlertDialog(onDismissRequest = { repoToRename = null }, title = { Text(t("rename_project_title", languageSetting)) }, text = { Column { OutlinedTextField(value = renameRepoNameInput, onValueChange = { renameRepoNameInput = it }, label = { Text(t("new_name", languageSetting)) }) } }, confirmButton = { Button(onClick = { val r = repoToRename!!; repoToRename = null; scope.launch { if (if (platform == "GitHub") renameGitHubRepo(token, owner, r.name, renameRepoNameInput) else renameCodebergRepo(token, owner, r.name, renameRepoNameInput)) refresh() } }) { Text(t("rename", languageSetting)) } }, dismissButton = { TextButton(onClick = { repoToRename = null }) { Text(t("cancel", languageSetting)) } })
        
        if (showActionChoice != null) {
            AlertDialog(
                onDismissRequest = { showActionChoice = null },
                confirmButton = {},
                title = null,
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = t("select_action", languageSetting),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = showActionChoice!!.name,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        // EDITOR BUTTON
                        Button(
                            onClick = { onRepoSelected(showActionChoice!!); showActionChoice = null },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(t("editor", languageSetting), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // PULL REQUESTS BUTTON
                        Button(
                            onClick = { 
                                onRepoSelectedPR(showActionChoice!!)
                                showActionChoice = null 
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Merge, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(t("pull_requests", languageSetting), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // RELEASES BUTTON
                        Button(
                            onClick = { onRepoSelectedReleases(showActionChoice!!); showActionChoice = null },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(t("releases", languageSetting), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { showActionChoice = null }) {
                            Text(t("cancel", languageSetting), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFilesScreen(
    repo: SimpleRepo,
    token: String,
    platform: String,
    onBack: () -> Unit,
    onFileClick: (ProjectFile) -> Unit,
    languageSetting: String,
    selectedBranch: String?,
    onBranchChange: (String) -> Unit
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<ProjectFile>>(emptyList()) }; var isLoading by remember { mutableStateOf(true) }

    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var showBranchDropdown by remember { mutableStateOf(false) }

    fun refreshFiles() {
        isLoading = true
        scope.launch {
            files = fetchFilesFromPlatform(token, repo.owner, repo.name, platform, currentPath, selectedBranch)
            isLoading = false
        }
    }

    LaunchedEffect(repo) {
        scope.launch {
            val b = fetchBranches(token, repo.owner, repo.name, platform)
            branches = b
            if (selectedBranch == null && b.isNotEmpty()) {
                onBranchChange(repo.defaultBranch)
            }
        }
    }

    LaunchedEffect(currentPath, selectedBranch) { refreshFiles() }

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
        if (bytes != null && uploadFileToPlatform(token, repo.owner, repo.name, "${pathPrefix}upload_${System.currentTimeMillis()}.png", Base64.encodeToString(bytes, Base64.NO_WRAP), platform, languageSetting, selectedBranch)) {
            Toast.makeText(context, t("uploaded", languageSetting), Toast.LENGTH_SHORT).show()
            refreshFiles()
        }
    } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(repo.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                },
                actions = {
                    Box {
                        TextButton(onClick = { showBranchDropdown = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(selectedBranch ?: "...", fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = showBranchDropdown, onDismissRequest = { showBranchDropdown = false }) {
                            branches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b) },
                                    onClick = {
                                        onBranchChange(b)
                                        showBranchDropdown = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { picker.launch("*/*") }) { Icon(Icons.Default.UploadFile, null) } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize()) {
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
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(repo: SimpleRepo, file: ProjectFile, token: String, platform: String, onBack: () -> Unit, languageSetting: String, branch: String? = null) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Theme Colors
    val editorBackground = MaterialTheme.colorScheme.surface
    val gutterBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val gutterText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val separatorLine = MaterialTheme.colorScheme.outlineVariant
    val accentColor = MaterialTheme.colorScheme.primary

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        text = fetchFileContent(token, repo.owner, repo.name, file.path, platform, branch) ?: ""
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(file.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Text(file.path, fontSize = 10.sp, color = gutterText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = accentColor, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = {
                            isSaving = true
                            scope.launch {
                                if (updateFileOnPlatform(token, repo.owner, repo.name, file.path, text, file.sha, platform, languageSetting, branch)) {
                                    onBack()
                                } else {
                                    isSaving = false
                                }
                            }
                        }) {
                            Text(t("save", languageSetting).uppercase(), color = accentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(editorBackground), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()
            val lines = text.lines()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(editorBackground)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line numbers column
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .background(gutterBackground)
                            .verticalScroll(verticalScrollState)
                            .padding(top = 12.dp, bottom = 100.dp, start = 8.dp, end = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        lines.indices.forEach { i ->
                            Text(
                                text = (i + 1).toString(),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = gutterText,
                                style = androidx.compose.ui.text.TextStyle(lineHeight = 20.sp),
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }

                    // Vertical separator
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(separatorLine))

                    // Editor area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                            .padding(top = 12.dp, bottom = 100.dp, start = 8.dp, end = 100.dp)
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = textColor,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceCodeBrowserScreen(
    app: OpenSourceApp,
    token: String,
    onBack: () -> Unit,
    onFileClick: (ProjectFile) -> Unit,
    languageSetting: String,
    selectedBranch: String?,
    onBranchChange: (String) -> Unit
) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var currentPath by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<ProjectFile>>(emptyList()) }; var isLoading by remember { mutableStateOf(true) }

    var branches by remember { mutableStateOf<List<String>>(emptyList()) }
    var showBranchDropdown by remember { mutableStateOf(false) }

    var lastFetchedParams by remember { mutableStateOf<Pair<String, String?>?>(null) }

    fun refreshFiles() {
        if (currentPath == lastFetchedParams?.first && selectedBranch == lastFetchedParams?.second) return
        isLoading = true
        scope.launch {
            files = fetchFilesFromPlatform(token, app.owner, app.name, app.platform, currentPath, selectedBranch)
            lastFetchedParams = currentPath to selectedBranch
            isLoading = false
        }
    }

    LaunchedEffect(app) {
        scope.launch {
            val repoInfo = fetchSingleRepoInfo(app.owner, app.name, token, app.platform)
            val b = fetchBranches(token, app.owner, app.name, app.platform)
            branches = b
            if (selectedBranch == null) {
                onBranchChange(repoInfo?.defaultBranch ?: (if (b.contains("main")) "main" else if (b.contains("master")) "master" else b.firstOrNull() ?: "main"))
            }
        }
    }

    LaunchedEffect(currentPath, selectedBranch) { refreshFiles() }

    BackHandler {
        if (currentPath.isEmpty()) onBack()
        else {
            val parts = currentPath.split("/")
            currentPath = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(app.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                },
                actions = {
                    Box {
                        TextButton(onClick = { showBranchDropdown = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(selectedBranch ?: t("loading", languageSetting), fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = showBranchDropdown, onDismissRequest = { showBranchDropdown = false }) {
                            branches.forEach { b ->
                                DropdownMenuItem(
                                    text = { Text(b) },
                                    onClick = {
                                        onBranchChange(b)
                                        showBranchDropdown = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.AccountTree, null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (files.isNotEmpty()) {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(files.size) { index ->
                        val f = files[index]
                        ElevatedCard(
                            onClick = {
                                if (f.type == "dir") currentPath = f.path
                                else onFileClick(f)
                            },
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    if (f.type == "dir") Icons.Default.Folder else Icons.Default.Description,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = if (f.type == "dir") Color.Cyan else Color.Gray
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = f.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceCodeViewerScreen(
    app: OpenSourceApp,
    file: ProjectFile,
    token: String,
    onBack: () -> Unit,
    languageSetting: String,
    branch: String? = null
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Theme Colors
    val editorBackground = MaterialTheme.colorScheme.surface
    val gutterBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val gutterText = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val separatorLine = MaterialTheme.colorScheme.outlineVariant
    val accentColor = MaterialTheme.colorScheme.primary

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        text = fetchFileContent(token, app.owner, app.name, file.path, app.platform, branch) ?: ""
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(file.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Text(file.path, fontSize = 10.sp, color = gutterText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().background(editorBackground), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()
            val lines = text.lines()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(editorBackground)
            ) {
                // Viewer area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                        .padding(top = 12.dp, bottom = 100.dp, start = 16.dp, end = 16.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { },
                        readOnly = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = textColor,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

val httpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

private suspend fun fetchGitHubProfile(token: String, username: String? = null): UserProfile? = withContext(Dispatchers.IO) {
    try {
        val url = if (username == null) "https://api.github.com/user" else "https://api.github.com/users/$username"
        val req = Request.Builder().url(url).addHeader("User-Agent", "OpiStore")
        if (token.isNotBlank()) req.addHeader("Authorization", "Bearer $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: "")
            UserProfile(json.optString("name", ""), json.getString("login"), json.getString("avatar_url"), json.optString("bio", ""), "GitHub")
        }
    } catch (e: Exception) { null }
}

private suspend fun fetchCodebergProfile(token: String, username: String? = null): UserProfile? = withContext(Dispatchers.IO) {
    try {
        val url = if (username == null) "https://codeberg.org/api/v1/user" else "https://codeberg.org/api/v1/users/$username"
        val req = Request.Builder().url(url).addHeader("Accept", "application/json").addHeader("User-Agent", "OpiStore")
        if (token.isNotBlank()) req.addHeader("Authorization", "token $token")
        httpClient.newCall(req.build()).execute().use { resp ->
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
                list.add(SimpleRepo(obj.getString("name"), ownerObj.getString("login"), obj.optString("description", ""), obj.getString("html_url"), obj.optInt("stargazers_count", 0), ownerObj.optString("avatar_url", ""), obj.optString("default_branch", "main")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchSingleRepoInfo(owner: String, repo: String, token: String, platform: String): SimpleRepo? = withContext(Dispatchers.IO) {
    try {
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo" else "https://codeberg.org/api/v1/repos/$owner/$repo"
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val obj = JSONObject(resp.body?.string() ?: "{}")
            val ownerObj = obj.optJSONObject("owner")
            SimpleRepo(
                obj.getString("name"),
                ownerObj?.optString("login") ?: owner,
                obj.optString("description", ""),
                obj.optString("html_url", ""),
                obj.optInt("stargazers_count", 0),
                ownerObj?.optString("avatar_url") ?: "",
                obj.optString("default_branch", "main")
            )
        }
    } catch (e: Exception) { null }
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
                list.add(SimpleRepo(obj.getString("name"), ownerObj.getString("login"), obj.optString("description", ""), obj.getString("html_url"), obj.optInt("stargazers_count", 0), ownerObj.optString("avatar_url", ""), obj.optString("default_branch", "main")))
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
                list.add(SimpleRepo(obj.getString("name"), owner.getString("login"), obj.optString("description", ""), obj.getString("html_url"), obj.optInt("stargazers_count", 0), owner.optString("avatar_url", ""), obj.optString("default_branch", "main")))
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

private suspend fun fetchRepoStats(app: OpenSourceApp, token: String): RepoStats? = withContext(Dispatchers.IO) {
    try {
        val ownerEnc = java.net.URLEncoder.encode(app.owner, "UTF-8")
        val nameEnc = java.net.URLEncoder.encode(app.name, "UTF-8")
        val url = if (app.platform == "GitHub") "https://api.github.com/repos/$ownerEnc/$nameEnc" else "https://codeberg.org/api/v1/repos/$ownerEnc/$nameEnc"
        val req = Request.Builder().url(url).addHeader("User-Agent", "OpiStore")
        if (token.isNotBlank()) req.addHeader("Authorization", if (app.platform == "GitHub") "Bearer $token" else "token $token")
        httpClient.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: "")
            RepoStats(
                stars = json.optInt(if (app.platform == "GitHub") "stargazers_count" else "stars_count", 0),
                forks = json.optInt("forks_count", 0),
                watchers = json.optInt(if (app.platform == "GitHub") "subscribers_count" else "watchers_count", 0),
                language = json.optString("language", "Unknown"),
                createdAt = json.optString("created_at", "")
            )
        }
    } catch (e: Exception) { null }
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
                    if (asset.getString("name").endsWith(".apk")) { list.add(AppRelease(rel.getString("tag_name"), asset.getString("browser_download_url"), rel.getBoolean("prerelease"), rel.optString("body", ""), asset.optInt("download_count", 0))) }
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
                    if (asset.getString("name").endsWith(".apk")) { list.add(AppRelease(rel.getString("tag_name"), asset.getString("browser_download_url"), rel.getBoolean("prerelease"), rel.optString("body", ""), asset.optInt("download_count", 0))) }
                }
            }
        }
    } catch (e: Exception) {}
    list
}


private suspend fun fetchDetailedReleases(owner: String, repo: String, token: String, platform: String): List<FullRelease> = withContext(Dispatchers.IO) {
    val list = mutableListOf<FullRelease>()
    try {
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/releases?per_page=100" else "https://codeberg.org/api/v1/repos/$owner/$repo/releases?limit=100"
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val rel = arr.getJSONObject(i)
                val assetsArr = rel.optJSONArray("assets") ?: JSONArray()
                val assets = mutableListOf<ReleaseAsset>()
                for (a in 0 until assetsArr.length()) {
                    val asset = assetsArr.getJSONObject(a)
                    assets.add(ReleaseAsset(asset.getString("id"), asset.getString("name"), asset.optString("browser_download_url", ""), asset.optLong("size", 0), asset.optInt("download_count", 0)))
                }
                list.add(FullRelease(rel.getString("id"), rel.getString("tag_name"), rel.optString("name", ""), rel.optString("body", ""), rel.optBoolean("prerelease", false), assets, rel.optString("html_url", "")))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun updateRelease(owner: String, repo: String, token: String, platform: String, releaseId: String, tagName: String, name: String, body: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/releases/$releaseId" else "https://codeberg.org/api/v1/repos/$owner/$repo/releases/$releaseId"
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val json = JSONObject().put("tag_name", tagName).put("name", name).put("body", body)
        val bodyReq = json.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url(url).patch(bodyReq).addHeader("Authorization", authHeader).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun deleteRelease(owner: String, repo: String, token: String, platform: String, releaseId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/releases/$releaseId" else "https://codeberg.org/api/v1/repos/$owner/$repo/releases/$releaseId"
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        httpClient.newCall(Request.Builder().url(url).delete().addHeader("Authorization", authHeader).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun deleteReleaseAsset(owner: String, repo: String, token: String, platform: String, assetId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/releases/assets/$assetId" else "https://codeberg.org/api/v1/repos/$owner/$repo/releases/assets/$assetId"
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        httpClient.newCall(Request.Builder().url(url).delete().addHeader("Authorization", authHeader).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun uploadReleaseAsset(owner: String, repo: String, token: String, platform: String, releaseId: String, fileName: String, fileBytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
    try {
        val uploadUrl = if (platform == "GitHub") {
            "https://uploads.github.com/repos/$owner/$repo/releases/$releaseId/assets?name=$fileName"
        } else {
            "https://codeberg.org/api/v1/repos/$owner/$repo/releases/$releaseId/assets?name=$fileName"
        }
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val bodyReq = fileBytes.toRequestBody("application/octet-stream".toMediaType())
        httpClient.newCall(Request.Builder().url(uploadUrl).post(bodyReq).addHeader("Authorization", authHeader).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}


private suspend fun fetchAppFromUrl(url: String, ghToken: String, cbToken: String): OpenSourceApp? = withContext(Dispatchers.IO) {
    try {
        if (url.contains("github.com")) {
            val parts = url.substringAfter("github.com/").split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val req = Request.Builder().url("https://api.github.com/repos/${parts[0]}/${parts[1]}")
                if (ghToken.isNotBlank()) req.addHeader("Authorization", "Bearer $ghToken")
                httpClient.newCall(req.build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val json = JSONObject(resp.body?.string() ?: "")
                    return@withContext OpenSourceApp("${parts[0]}/${parts[1]}", json.getString("name"), parts[0], "GitHub", json.optString("description", ""), json.getString("html_url"), json.getJSONObject("owner").getString("avatar_url"))
                }
            }
        } else if (url.contains("codeberg.org")) {
            val parts = url.substringAfter("codeberg.org/").split("/").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val req = Request.Builder().url("https://codeberg.org/api/v1/repos/${parts[0]}/${parts[1]}")
                if (cbToken.isNotBlank()) req.addHeader("Authorization", "token $cbToken")
                httpClient.newCall(req.build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val json = JSONObject(resp.body?.string() ?: "")
                    return@withContext OpenSourceApp("${parts[0]}/${parts[1]}", json.getString("name"), parts[0], "Codeberg", json.optString("description", ""), json.getString("html_url"), json.getJSONObject("owner").getString("avatar_url"))
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
    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    if (prefs.getBoolean("AUTO_DELETE_APK", false)) {
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        if (info != null) {
            val mapping = prefs.getStringSet("APK_DELETE_MAPPING", emptySet())?.toMutableSet() ?: mutableSetOf()
            mapping.add("${info.packageName}|${file.absolutePath}")
            prefs.edit().putStringSet("APK_DELETE_MAPPING", mapping).apply()
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK })
}

private fun isApkValid(context: Context, file: File): Boolean {
    return try {
        context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) != null
    } catch (e: Exception) { false }
}

private fun getApkIcon(context: Context, file: File): Drawable? {
    return try {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
        info?.applicationInfo?.let {
            it.sourceDir = file.absolutePath
            it.publicSourceDir = file.absolutePath
            it.loadIcon(pm)
        }
    } catch (e: Exception) { null }
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

    val context = LocalContext.current
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestsScreen(app: OpenSourceApp, githubToken: String, codebergToken: String, languageSetting: String, onUserClick: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var prs by remember { mutableStateOf<List<PullRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var filterOpen by remember { mutableStateOf(true) }
    var selectedPR by remember { mutableStateOf<PullRequest?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val token = if (app.platform == "GitHub") githubToken else codebergToken

    fun refresh() {
        isLoading = true
        scope.launch {
            prs = fetchPullRequests(token, app.owner, app.name, app.platform, if (filterOpen) "open" else "closed")
            isLoading = false
        }
    }

    LaunchedEffect(filterOpen) { refresh() }

    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t("pull_requests", languageSetting)) },
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
                                DropdownMenuItem(text = { Text(t("open", languageSetting)) }, onClick = { filterOpen = true; showFilterMenu = false }, leadingIcon = { Icon(Icons.Default.RadioButtonChecked, null, tint = Color.Green) })
                                DropdownMenuItem(text = { Text(t("closed", languageSetting)) }, onClick = { filterOpen = false; showFilterMenu = false }, leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = Color.Red) })
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                else if (prs.isEmpty()) Text(t("no_pull_requests", languageSetting), Modifier.align(Alignment.Center))
                else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(prs) { pr ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable { selectedPR = pr },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = pr.userAvatar, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(pr.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("#${pr.number} by ", fontSize = 11.sp, color = Color.Gray)
                                            Text(text = pr.user, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onUserClick(pr.user, app.platform, pr.userAvatar); onDismiss() })
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedPR != null) {
            PRDetailScreen(app = app, pr = selectedPR!!, token = token, languageSetting = languageSetting, onUserClick = { u, p, a -> onUserClick(u, p, a); onDismiss() }, onBack = { selectedPR = null; refresh() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PRDetailScreen(app: OpenSourceApp, pr: PullRequest, token: String, languageSetting: String, onUserClick: (String, String, String) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<IssueComment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    val myProfile = remember { mutableStateOf<UserProfile?>(null) }
    var showMergeConfirm by remember { mutableStateOf(false) }

    var commentToDelete by remember { mutableStateOf<IssueComment?>(null) }

    LaunchedEffect(Unit) {
        myProfile.value = if (app.platform == "GitHub") fetchGitHubProfile(token) else fetchCodebergProfile(token)
        comments = fetchIssueComments(token, app.owner, app.name, app.platform, pr.number, myProfile.value?.login)
        isLoading = false
    }

    if (showMergeConfirm) {
        AlertDialog(
            onDismissRequest = { showMergeConfirm = false },
            title = { Text(t("merge", languageSetting)) },
            text = { Text(t("merge_confirm", languageSetting)) },
            confirmButton = { Button(onClick = { scope.launch { if (mergePullRequest(token, app.owner, app.name, app.platform, pr.number)) onBack(); showMergeConfirm = false } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text(t("yes_merge", languageSetting)) } },
            dismissButton = { TextButton(onClick = { showMergeConfirm = false }) { Text(t("cancel", languageSetting)) } }
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
                            comments = fetchIssueComments(token, app.owner, app.name, app.platform, pr.number, myProfile.value?.login)
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
                    title = { Text("#${pr.number}") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                    actions = {
                        if (pr.state == "open" && app.owner == myProfile.value?.login) {
                            Button(onClick = { showMergeConfirm = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), modifier = Modifier.padding(end = 8.dp)) {
                                Text(t("merge", languageSetting))
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Surface(tonalElevation = 8.dp, modifier = Modifier.navigationBarsPadding()) {
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = commentText, onValueChange = { commentText = it }, placeholder = { Text(t("write_comment", languageSetting)) }, modifier = Modifier.weight(1f), maxLines = 5)
                        IconButton(onClick = { scope.launch { if (commentText.isNotBlank() && postComment(token, app.owner, app.name, app.platform, pr.number, commentText)) { commentText = ""; comments = fetchIssueComments(token, app.owner, app.name, app.platform, pr.number, myProfile.value?.login) } } }, enabled = commentText.isNotBlank()) { Icon(Icons.Default.Send, null, tint = if(commentText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray) }
                    }
                }
            }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(model = pr.userAvatar, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onUserClick(pr.user, app.platform, pr.userAvatar) })
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = pr.user, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onUserClick(pr.user, app.platform, pr.userAvatar) })
                                    val statusColor = when(pr.state) {
                                        "open" -> Color(0xFF4CAF50)
                                        "merged" -> Color(0xFF673AB7)
                                        else -> Color(0xFFE57373)
                                    }
                                    Text(t(pr.state, languageSetting), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp)); Text(pr.title, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            if (pr.headLabel != null) { Text("from: ${pr.headLabel}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)) }
                            Spacer(modifier = Modifier.height(8.dp))
                            MarkdownBody(pr.body)
                        }
                    }
                    Text(t("comments", languageSetting), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 16.sp)
                }
                if (isLoading) item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                items(comments) { comment ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                        Row(Modifier.padding(8.dp)) {
                            AsyncImage(model = comment.userAvatar, contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onUserClick(comment.user, app.platform, comment.userAvatar) })
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = comment.user, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f).clickable { onUserClick(comment.user, app.platform, comment.userAvatar) })
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
                                            .clickable { scope.launch { if (toggleReaction(token, app.owner, app.name, app.platform, comment.id, comment.myReactionId)) comments = fetchIssueComments(token, app.owner, app.name, app.platform, pr.number, myProfile.value?.login) } }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoStatsScreen(app: OpenSourceApp, githubToken: String, codebergToken: String, languageSetting: String, onDismiss: () -> Unit) {
    var stats by remember { mutableStateOf<RepoStats?>(null) }
    var releases by remember { mutableStateOf<List<AppRelease>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val token = if (app.platform == "GitHub") githubToken else codebergToken

    LaunchedEffect(Unit) {
        val s = fetchRepoStats(app, token)
        val r = fetchReleasesForApp(app, githubToken, codebergToken)
        stats = s
        releases = r
        isLoading = false
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(t("repo_stats", languageSetting)) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } }
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center))
                else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                        if (stats != null) {
                            item {
                                Spacer(Modifier.height(16.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    StatsCard(t("stars", languageSetting), stats!!.stars.toString(), Icons.Default.Star, Color(0xFFFFD700), Modifier.weight(1f))
                                    StatsCard(t("forks", languageSetting), stats!!.forks.toString(), Icons.Default.AccountTree, Color(0xFF607D8B), Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    StatsCard(t("watchers", languageSetting), stats!!.watchers.toString(), Icons.Default.Visibility, Color(0xFF2196F3), Modifier.weight(1f))
                                    StatsCard(t("language", languageSetting), stats!!.language, Icons.Default.Code, Color(0xFF4CAF50), Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(32.dp))
                            }
                        }

                        if (releases.isNotEmpty()) {
                            val totalDownloads = releases.sumOf { it.downloadCount }
                            item {
                                Text(t("total_downloads", languageSetting), fontWeight = FontWeight.Black, fontSize = 22.sp)
                                Text("$totalDownloads", fontSize = 42.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(24.dp))
                                Text(t("downloads_per_version", languageSetting), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(Modifier.height(12.dp))
                            }
                            items(releases) { release ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(release.version, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(if (release.isPreRelease) t("pre_release", languageSetting) else t("stable_release", languageSetting), fontSize = 12.sp, color = Color.Gray)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(6.dp))
                                            Text("${release.downloadCount}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(32.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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
                Text(
                    text = parseMarkdown(txt),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
        val italicRegex = "(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)".toRegex()

        var lastIdx = 0
        val matches = (boldRegex.findAll(text).map { it to "bold" } +
                italicRegex.findAll(text).map { it to "italic" })
            .sortedBy { it.first.range.first }

        matches.forEach { (match, type) ->
            if (match.range.first >= lastIdx) {
                append(text.substring(lastIdx, match.range.first))
                withStyle(style = SpanStyle(
                    fontWeight = if (type == "bold") FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (type == "italic") androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                )) {
                    append(match.groupValues[1])
                }
                lastIdx = match.range.last + 1
            }
        }
        append(text.substring(lastIdx))
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
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
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

private suspend fun fetchPullRequests(token: String, owner: String, repo: String, platform: String, state: String): List<PullRequest> = withContext(Dispatchers.IO) {
    val list = mutableListOf<PullRequest>()
    try {
        val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/pulls?state=$state" else "https://codeberg.org/api/v1/repos/$owner/$repo/pulls?state=$state"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", auth).addHeader("User-Agent", "OpiStore").build()).execute().use { resp ->
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val user = obj.getJSONObject("user")
                val state = obj.getString("state")
                val isMerged = if (platform == "GitHub") !obj.isNull("merged_at") else obj.optBoolean("merged", false)
                val displayState = if (isMerged) "merged" else state
                list.add(PullRequest(
                    obj.getString("id"),
                    obj.getInt("number"),
                    obj.getString("title"),
                    obj.optString("body", ""),
                    displayState,
                    user.getString("login"),
                    user.getString("avatar_url"),
                    0,
                    obj.optString("created_at", ""),
                    obj.optJSONObject("head")?.optString("label", "")
                ))
            }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun mergePullRequest(token: String, owner: String, repo: String, platform: String, number: Int): Boolean = withContext(Dispatchers.IO) {
    try {
        val auth = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/pulls/$number/merge" else "https://codeberg.org/api/v1/repos/$owner/$repo/pulls/$number/merge"
        val body = JSONObject().toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url(url).put(body).addHeader("Authorization", auth).addHeader("User-Agent", "OpiStore").build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
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
                
                var reactions = 0
                var myReactionId: String? = null
                
                if (platform == "GitHub") {
                    reactions = if (obj.has("reactions")) obj.getJSONObject("reactions").optInt("total_count", 0) else 0
                    if (myLogin != null) {
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
                } else if (platform == "Codeberg") {
                    if (obj.has("reactions")) {
                        val rArr = obj.optJSONArray("reactions")
                        if (rArr != null) {
                            reactions = rArr.length()
                            if (myLogin != null) {
                                for (j in 0 until rArr.length()) {
                                    val rObj = rArr.getJSONObject(j)
                                    if (rObj.optJSONObject("user")?.optString("login") == myLogin && rObj.optString("content") == "+1") {
                                        myReactionId = "exists"
                                        break
                                    }
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
        } else if (platform == "Codeberg") {
            val url = "https://codeberg.org/api/v1/repos/$owner/$repo/issues/comments/$commentId/reactions"
            val jsonBody = JSONObject().apply { put("content", "+1") }.toString().toRequestBody("application/json".toMediaType())
            val req = if (currentReactionId != null) {
                Request.Builder().url(url).delete(jsonBody).addHeader("Authorization", "token $token").build()
            } else {
                Request.Builder().url(url).post(jsonBody).addHeader("Authorization", "token $token").build()
            }
            httpClient.newCall(req).execute().use { it.isSuccessful }
        } else false
    } catch (e: Exception) { false }
}

private suspend fun fetchBranches(token: String, owner: String, repo: String, platform: String): List<String> = withContext(Dispatchers.IO) {
    val list = mutableListOf<String>()
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/branches" else "https://codeberg.org/api/v1/repos/$owner/$repo/branches"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) { list.add(arr.getJSONObject(i).getString("name")) }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchTags(token: String, owner: String, repo: String, platform: String): List<String> = withContext(Dispatchers.IO) {
    val list = mutableListOf<String>()
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/tags" else "https://codeberg.org/api/v1/repos/$owner/$repo/tags"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) { list.add(arr.getJSONObject(i).getString("name")) }
        }
    } catch (e: Exception) {}
    list
}

private suspend fun fetchFilesFromPlatform(token: String, owner: String, repo: String, platform: String, path: String = "", branch: String? = null): List<ProjectFile> = withContext(Dispatchers.IO) {
    val list = mutableListOf<ProjectFile>()
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val baseUrl = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents"
        var url = if (path.isEmpty()) baseUrl else "$baseUrl/$path"
        if (branch != null) url += "?ref=$branch"

        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) { val obj = arr.getJSONObject(i); list.add(ProjectFile(obj.getString("name"), obj.getString("path"), obj.getString("type"), obj.getString("sha"), obj.optString("download_url", ""))) }
        }
    } catch (e: Exception) {}
    list.sortedBy { it.type == "file" }
}

private suspend fun fetchFileContent(token: String, owner: String, repo: String, path: String, platform: String, branch: String? = null): String? = withContext(Dispatchers.IO) {
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        var url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents/$path" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents/$path"
        if (branch != null) url += "?ref=$branch"
        httpClient.newCall(Request.Builder().url(url).addHeader("Authorization", authHeader).build()).execute().use { resp ->
            val json = JSONObject(resp.body?.string() ?: "")
            String(Base64.decode(json.getString("content").replace("\n", ""), Base64.DEFAULT))
        }
    } catch (e: Exception) { null }
}

private suspend fun updateFileOnPlatform(token: String, owner: String, repo: String, path: String, content: String, sha: String, platform: String, languageSetting: String, branch: String? = null): Boolean = withContext(Dispatchers.IO) {
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents/$path" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents/$path"
        val bodyObj = JSONObject().apply {
            put("message", t("edit_via_app", languageSetting))
            put("content", Base64.encodeToString(content.toByteArray(), Base64.NO_WRAP))
            put("sha", sha)
            if (branch != null) put("branch", branch)
        }
        val body = bodyObj.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url(url).put(body).addHeader("Authorization", authHeader).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun uploadFileToPlatform(token: String, owner: String, repo: String, path: String, content: String, platform: String, languageSetting: String, branch: String? = null): Boolean = withContext(Dispatchers.IO) {
    try {
        val authHeader = if (platform == "GitHub") "Bearer $token" else "token $token"
        val url = if (platform == "GitHub") "https://api.github.com/repos/$owner/$repo/contents/$path" else "https://codeberg.org/api/v1/repos/$owner/$repo/contents/$path"
        val bodyObj = JSONObject().apply {
            put("message", t("upload_via_app", languageSetting))
            put("content", content)
            if (branch != null) put("branch", branch)
        }
        val body = bodyObj.toString().toRequestBody("application/json".toMediaType())
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

private suspend fun renameGitHubRepo(token: String, owner: String, oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject().apply { put("name", newName) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url("https://api.github.com/repos/$owner/$oldName").patch(body).addHeader("Authorization", "Bearer $token").build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

private suspend fun renameCodebergRepo(token: String, owner: String, oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject().apply { put("name", newName) }.toString().toRequestBody("application/json".toMediaType())
        httpClient.newCall(Request.Builder().url("https://codeberg.org/api/v1/repos/$owner/$oldName").patch(body).addHeader("Authorization", "token $token").build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesManagementScreen(repo: SimpleRepo, token: String, platform: String, onBack: () -> Unit, onReleaseClick: (FullRelease) -> Unit, languageSetting: String) {
    val scope = rememberCoroutineScope()
    var releases by remember { mutableStateOf<List<FullRelease>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refresh() {
        isLoading = true
        scope.launch {
            releases = fetchDetailedReleases(repo.owner, repo.name, token, platform)
            isLoading = false
        }
    }

    LaunchedEffect(repo) { refresh() }
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(repo.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(t("releases", languageSetting), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, null) } }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (releases.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text(t("no_releases", languageSetting), color = Color.Gray)
                }
            }
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(300.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(releases.size, key = { releases[it].id }) { index ->
                    val rel = releases[index]
                    ElevatedCard(
                        onClick = { onReleaseClick(rel) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rel.name.ifEmpty { rel.tagName },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(rel.tagName, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                
                                Surface(
                                    color = if (rel.isPreRelease) Color(0xFFFFA000).copy(alpha = 0.1f) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (rel.isPreRelease) "PRE" else "STABLE",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rel.isPreRelease) Color(0xFFFFA000) else Color(0xFF4CAF50)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.1f))
                            Spacer(Modifier.height(12.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                Spacer(Modifier.width(4.dp))
                                Text("${rel.assets.size} ${t("assets", languageSetting)}", fontSize = 12.sp, color = Color.Gray)
                                
                                Spacer(Modifier.weight(1f))
                                
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseEditScreen(repo: SimpleRepo, release: FullRelease, token: String, platform: String, onBack: () -> Unit, languageSetting: String) {
    val scope = rememberCoroutineScope()
    var tagName by remember { mutableStateOf(release.tagName) }
    var releaseName by remember { mutableStateOf(release.name) }
    var body by remember { mutableStateOf(release.body) }
    var assets by remember { mutableStateOf(release.assets) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingAsset by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBodyEditor by remember { mutableStateOf(false) }
    var showTagSelector by remember { mutableStateOf(false) }
    var availableTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var isDraggingOver by remember { mutableStateOf(false) }
    var stagedAssetsToDelete by remember { mutableStateOf<Set<String>>(emptySet()) }
    var stagedUploads by remember { mutableStateOf<List<Pair<String, ByteArray>>>(emptyList()) }
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var newTagNameInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    BackHandler { onBack() }

    LaunchedEffect(Unit) {
        availableTags = fetchTags(token, repo.owner, repo.name, platform)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                val fileName = uri.path?.substringAfterLast("/") ?: "upload_${System.currentTimeMillis()}.apk"
                if (bytes != null) {
                    stagedUploads = stagedUploads + (fileName to bytes)
                }
            } catch (e: Exception) {
                Toast.makeText(context, t("no_access", languageSetting), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val dragScale by androidx.compose.animation.core.animateFloatAsState(if (isDraggingOver) 1.05f else 1f)

    val hasChanges = tagName != release.tagName || releaseName != release.name || body != release.body || stagedAssetsToDelete.isNotEmpty() || stagedUploads.isNotEmpty()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(t("edit_release", languageSetting), fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    }
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    val metadataSuccess = updateRelease(repo.owner, repo.name, token, platform, release.id, tagName, releaseName, body)
                                    if (metadataSuccess) {
                                        // Process deletions
                                        stagedAssetsToDelete.forEach { assetId ->
                                            deleteReleaseAsset(repo.owner, repo.name, token, platform, assetId)
                                        }
                                        // Process uploads
                                        stagedUploads.forEach { (name, bytes) ->
                                            uploadReleaseAsset(repo.owner, repo.name, token, platform, release.id, name, bytes)
                                        }
                                        onBack()
                                    } else {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = hasChanges
                        ) {
                            Text(t("save", languageSetting).uppercase(), fontWeight = FontWeight.Bold, color = if (hasChanges) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // METADATA CARD
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Metadata", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth().clickable { showTagSelector = true }) {
                        OutlinedTextField(
                            value = tagName,
                            onValueChange = { },
                            readOnly = true,
                            enabled = false,
                            label = { Text(t("tag_name", languageSetting)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(expanded = showTagSelector, onDismissRequest = { showTagSelector = false }) {
                            availableTags.forEach { tag ->
                                DropdownMenuItem(text = { Text(tag) }, onClick = { tagName = tag; showTagSelector = false })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (languageSetting == "de") "Tag erstellen..." else "Create Tag...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = { showCreateTagDialog = true; showTagSelector = false }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = releaseName,
                        onValueChange = { releaseName = it },
                        label = { Text(t("release_title", languageSetting)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showBodyEditor = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t("edit_changelog", languageSetting))
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // ASSET CENTER
            Text(
                text = t("assets", languageSetting),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            
            // Asset Upload Zone
            val infiniteTransition = rememberInfiniteTransition(label = "upload")
            val arrowOffset by infiniteTransition.animateFloat(
                initialValue = 5f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "arrow"
            )

            Card(
                onClick = { if (!isUploadingAsset) picker.launch("*/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(dragScale),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isUploadingAsset) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            Icon(
                                Icons.Default.ArrowUpward, 
                                null, 
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(y = arrowOffset.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(t("loading", languageSetting), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(t("add_asset", languageSetting), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            assets.filter { it.id !in stagedAssetsToDelete }.forEach { asset ->
                val isSource = asset.name.endsWith(".zip") || asset.name.endsWith(".tar.gz")
                val isApk = asset.name.endsWith(".apk")
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isApk) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
                ) {
                    ListItem(
                        headlineContent = { Text(asset.name, fontWeight = if (isApk) FontWeight.Bold else FontWeight.Normal) },
                        supportingContent = { Text("${asset.size / 1024} KB") },
                        leadingContent = { 
                            Icon(
                                if (isApk) Icons.Default.Android else if (isSource) Icons.Default.Code else Icons.Default.InsertDriveFile,
                                null,
                                tint = if (isApk) Color(0xFF4CAF50) else Color.Gray
                            ) 
                        },
                        trailingContent = {
                            if (!isSource) {
                                IconButton(onClick = {
                                    stagedAssetsToDelete = stagedAssetsToDelete + asset.id
                                }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            if (stagedUploads.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(if (languageSetting == "de") "Geplante Uploads" else "Staged Uploads", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                stagedUploads.forEachIndexed { index, (name, bytes) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    ) {
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = { Text("${bytes.size / 1024} KB (Staged)") },
                            leadingContent = { Icon(Icons.Default.UploadFile, null, tint = MaterialTheme.colorScheme.primary) },
                            trailingContent = {
                                IconButton(onClick = {
                                    stagedUploads = stagedUploads.filterIndexed { i, _ -> i != index }
                                }) { Icon(Icons.Default.Close, null) }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showBodyEditor) {
        Dialog(onDismissRequest = { showBodyEditor = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(t("edit_changelog", languageSetting)) },
                            navigationIcon = { IconButton(onClick = { showBodyEditor = false }) { Icon(Icons.Default.Close, null) } },
                            actions = { TextButton(onClick = { showBodyEditor = false }) { Text("OK", fontWeight = FontWeight.Bold) } }
                        )
                    }
                ) { p ->
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        modifier = Modifier.fillMaxSize().padding(p).padding(16.dp).imePadding(),
                        label = { Text(t("body", languageSetting)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(t("delete_release", languageSetting)) },
            text = { Text(t("delete_release_confirm", languageSetting)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (deleteRelease(repo.owner, repo.name, token, platform, release.id)) onBack()
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(t("yes_delete", languageSetting))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(t("cancel", languageSetting)) } }
        )
    }

    if (showCreateTagDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = { Text(if (languageSetting == "de") "Neuen Tag erstellen" else "Create New Tag") },
            text = {
                Column {
                    Text(if (languageSetting == "de") "Geben Sie einen Namen für den neuen Tag ein:" else "Enter a name for the new tag:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTagNameInput,
                        onValueChange = { newTagNameInput = it },
                        label = { Text("Tag Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagNameInput.isNotBlank()) {
                            tagName = newTagNameInput
                            availableTags = availableTags + newTagNameInput
                            showCreateTagDialog = false
                            newTagNameInput = ""
                        }
                    },
                    enabled = newTagNameInput.isNotBlank()
                ) { Text(if (languageSetting == "de") "Erstellen" else "Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTagDialog = false }) { Text(t("cancel", languageSetting)) }
            }
        )
    }
}

@Composable
fun UpdateScreen(
    currentVersion: String,
    newVersion: String,
    changelog: String,
    downloadUrl: String,
    onCancel: () -> Unit,
    languageSetting: String,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf("") }
    var downloadedFile by remember { mutableStateOf<java.io.File?>(null) }

    BackHandler(onBack = onCancel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = t("update_available", languageSetting),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${t("current_version", languageSetting)}: $currentVersion",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${t("new_version", languageSetting)}: $newVersion",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Changelog
            if (changelog.isNotBlank()) {
                Text(
                    t("changelog_title", languageSetting),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        MarkdownBody(changelog)
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isDownloading) {
                    Text(
                        text = downloadProgress,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    val isDownloaded = downloadedFile != null
                    Button(
                        onClick = {
                            if (isDownloaded) {
                                installApk(context, downloadedFile!!)
                            } else {
                                isDownloading = true
                                scope.launch {
                                    val file = downloadApk(context, "update.apk", downloadUrl, "", languageSetting) { downloadProgress = it }
                                    if (file != null) {
                                        downloadedFile = file
                                        installApk(context, file)
                                    }
                                    isDownloading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download, null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (isDownloaded) t("install", languageSetting) else t("update_now", languageSetting),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = t("later", languageSetting),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private suspend fun checkAppUpdate(): AppRelease? = withContext(Dispatchers.IO) {
    try {
        val url = "https://api.github.com/repos/DigitalTechLab/Opi-Store/releases/latest"
        httpClient.newCall(Request.Builder().url(url).addHeader("User-Agent", "OpiStore").build()).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext null
            val obj = org.json.JSONObject(resp.body?.string() ?: "")
            val tag = obj.getString("tag_name")
            val assets = obj.getJSONArray("assets")
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }
            if (downloadUrl.isNotEmpty()) {
                AppRelease(tag, downloadUrl, obj.optBoolean("prerelease", false), obj.optString("body", ""))
            } else null
        }
    } catch (e: Exception) { null }
}

private fun isNewerVersion(current: String, latest: String): Boolean {
    val curClean = current.replace(Regex("[^0-9.]"), "")
    val latClean = latest.replace(Regex("[^0-9.]"), "")
    val curParts = curClean.split(".").mapNotNull { it.toIntOrNull() }
    val latParts = latClean.split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(curParts.size, latParts.size)) {
        val cur = curParts.getOrElse(i) { 0 }
        val lat = latParts.getOrElse(i) { 0 }
        if (lat > cur) return true
        if (cur > lat) return false
    }
    return false
}
