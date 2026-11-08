package com.chme.pureblackmedia.v20261108210636

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ALL = "全部"
private const val IMAGES = "图片"
private const val VIDEOS = "视频"
private const val TRASH = "最近删除"
private val black = Color.Black
private val white = Color.White
private data class Media(val uri: Uri, val mime: String, val date: Long, val size: Long, val video: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { Home() }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Home() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("media_tags", 0) }
    var tags by remember { mutableStateOf(prefs.getStringSet("tags", emptySet())!!.toSet()) }
    var assignments by remember { mutableStateOf(loadAssignments(prefs)) }
    var media by remember { mutableStateOf(emptyList<Media>()) }
    var tab by remember { mutableStateOf(ALL) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var selection by remember { mutableStateOf(false) }
    var columns by remember { mutableFloatStateOf(4f) }
    var newest by remember { mutableStateOf(true) }
    var sizeSort by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    var assignDialog by remember { mutableStateOf(false) }
    var manageDialog by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }

    fun persist() {
        prefs.edit().putStringSet("tags", tags)
            .putString("assignments", assignments.entries.joinToString("\n") { "${it.key}\t${it.value.joinToString("|")}" }).apply()
    }
    val perms = remember {
        when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    fun permitted() = perms.any { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    LaunchedEffect(refresh) {
        media = if (permitted()) withContext(Dispatchers.IO) { readMedia(ctx) } else emptyList()
    }
    val shown = remember(media, tab, assignments, newest, sizeSort) {
        media.filter { m -> when (tab) {
            ALL -> true
            IMAGES -> !m.video
            VIDEOS -> m.video
            TRASH -> assignments[m.uri.toString()]?.contains(TRASH) == true
            else -> assignments[m.uri.toString()]?.contains(tab) == true
        }}.let { list -> if (sizeSort) list.sortedBy { if (newest) -it.size else it.size } else list.sortedBy { if (newest) -it.date else it.date } }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = black, surface = black, onBackground = white, onSurface = white, primary = white)) {
        Column(Modifier.fillMaxSize().background(black)) {
            Spacer(Modifier.fillMaxWidth().height(52.dp))
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Action("刷新", Modifier.weight(1f)) { refresh++ }
                Action(if (newest) "新→旧" else "旧→新", Modifier.weight(1f)) { newest = !newest }
                Action(if (sizeSort) "大小" else "日期", Modifier.weight(1f)) { sizeSort = !sizeSort }
            }
            HorizontalDivider(color = Color.DarkGray)
            Row(Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                LazyRow(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(listOf(ALL, IMAGES, VIDEOS, TRASH) + tags.toList()) { name ->
                        Text(name, color = if (tab == name) black else white, modifier = Modifier
                            .background(if (tab == name) white else Color(0xFF171717), MaterialTheme.shapes.large)
                            .combinedClickable(onClick = { tab = name; selected = emptySet(); selection = false }, onLongClick = { if (name !in listOf(ALL, IMAGES, VIDEOS, TRASH)) manageDialog = true })
                            .padding(horizontal = 12.dp, vertical = 9.dp), fontSize = 13.sp)
                    }
                }
                TextButton(onClick = { addDialog = true }) { Text("+", color = white, fontSize = 20.sp) }
            }
            HorizontalDivider(color = Color.DarkGray)
            if (!permitted()) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("需要媒体读取权限", color = white)
                    Button(onClick = { launcher.launch(perms) }) { Text("授权") }
                }
            } else {
                if (selection) Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("已选 ${selected.size} 项", color = white, modifier = Modifier.weight(1f))
                    TextButton(onClick = { assignDialog = true }) { Text("添加标签", color = white) }
                    TextButton(onClick = {
                        assignments = assignments + selected.associateWith { setOf(TRASH) }
                        persist(); selected = emptySet(); selection = false; tab = TRASH
                    }) { Text("移入最近删除", color = white) }
                    TextButton(onClick = { selection = false; selected = emptySet() }) { Text("取消", color = white) }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns.toInt().coerceIn(4, 10)),
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ -> columns = (columns / zoom).coerceIn(4f, 10f) }
                    },
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(shown, key = { it.uri.toString() }) { item ->
                        val key = item.uri.toString()
                        val active = key in selected
                        Box(Modifier.aspectRatio(1f).background(Color(0xFF151515)).combinedClickable(
                            onClick = {
                                if (selection) selected = if (active) selected - key else selected + key
                                else runCatching {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(item.uri, item.mime.ifBlank { if (item.video) "video/*" else "image/*" }).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                }
                            },
                            onLongClick = { selection = true; selected = selected + key }
                        )) {
                            AsyncImage(item.uri, item.mime, Modifier.fillMaxSize())
                            if (active) Box(Modifier.fillMaxSize().background(Color(0x66000000)))
                        }
                    }
                }
            }
        }
    }

    if (addDialog) AlertDialog(onDismissRequest = { addDialog = false }, title = { Text("新增标签") }, text = {
        OutlinedTextField(newTag, { newTag = it }, singleLine = true, label = { Text("标签名称") })
    }, confirmButton = { TextButton(onClick = {
        val t = newTag.trim()
        if (t.isNotEmpty() && t !in listOf(ALL, IMAGES, VIDEOS, TRASH) && t !in tags && t.none { it == '|' || it == '\n' || it == '\t' }) { tags = tags + t; persist() }
        newTag = ""; addDialog = false
    }) { Text("添加") } }, dismissButton = { TextButton(onClick = { addDialog = false }) { Text("取消") } })
    if (assignDialog) AlertDialog(onDismissRequest = { assignDialog = false }, title = { Text("添加到标签") },
        text = { Column { tags.forEach { t -> TextButton(onClick = {
            assignments = assignments + selected.associateWith { key -> (assignments[key].orEmpty() - TRASH) + t }
            persist(); selected = emptySet(); selection = false; assignDialog = false
        }) { Text(t) } } } },
        confirmButton = { TextButton(onClick = { assignDialog = false }) { Text("关闭") } })
    if (manageDialog) AlertDialog(onDismissRequest = { manageDialog = false }, title = { Text("标签管理") },
        text = { Column { tags.forEach { t -> TextButton(onClick = {
            tags = tags - t; assignments = assignments.mapValues { it.value - t }; if (tab == t) tab = ALL; persist()
        }) { Text("删除：$t") } } } },
        confirmButton = { TextButton(onClick = { manageDialog = false }) { Text("完成") } })
}

@Composable
private fun Action(label: String, modifier: Modifier, click: () -> Unit) {
    Box(modifier.fillMaxHeight().combinedClickable(onClick = click).padding(4.dp), contentAlignment = Alignment.Center) {
        Text(label, color = white, fontSize = 14.sp)
    }
}
private fun loadAssignments(p: android.content.SharedPreferences): Map<String, Set<String>> =
    p.getString("assignments", "").orEmpty().lineSequence().mapNotNull { line ->
        val a = line.split('\t', limit = 2); if (a.size == 2) a[0] to a[1].split('|').filter { it.isNotBlank() }.toSet() else null
    }.toMap()

private fun readMedia(ctx: android.content.Context): List<Media> {
    val uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.SIZE, MediaStore.Files.FileColumns.MEDIA_TYPE)
    val where = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
    val args = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(), MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
    val out = mutableListOf<Media>()
    ctx.contentResolver.query(uri, projection, where, args, "${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { c ->
        val id = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val mime = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val date = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val type = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
        while (c.moveToNext()) {
            val video = c.getInt(type) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
            out += Media(ContentUris.withAppendedId(uri, c.getLong(id)), c.getString(mime).orEmpty(), c.getLong(date), c.getLong(size), video)
        }
    }
    return out
}
