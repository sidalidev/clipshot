package dev.sidali.clipshot

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ClipshotTheme { Screen() } }
    }

    override fun onResume() { super.onResume(); ShotJob.schedule(this) }
}

/* ---------- palette: quiet neutrals, one accent ---------- */
private object Palette {
    val accent = Color(0xFF0A84FF)
    val green = Color(0xFF30D158)
}

@Composable
private fun ClipshotTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        primary = Palette.accent, background = Color(0xFF000000), surface = Color(0xFF1C1C1E),
        onBackground = Color.White, onSurface = Color.White, surfaceVariant = Color(0xFF2C2C2E),
        onSurfaceVariant = Color(0xFF98989F), outline = Color(0xFF38383A),
    ) else lightColorScheme(
        primary = Palette.accent, background = Color(0xFFF2F2F7), surface = Color.White,
        onBackground = Color(0xFF000000), onSurface = Color(0xFF000000), surfaceVariant = Color(0xFFE5E5EA),
        onSurfaceVariant = Color(0xFF6C6C70), outline = Color(0xFFD1D1D6),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun Screen() {
    val ctx = LocalContext.current
    var access by remember { mutableStateOf(Shots.hasAccess(ctx)) }
    var enabled by remember { mutableStateOf(Prefs.enabled(ctx)) }
    var lastTime by remember { mutableLongStateOf(Prefs.lastTime(ctx)) }
    var count by remember { mutableIntStateOf(Prefs.count(ctx)) }
    var thumb by remember { mutableStateOf<ImageBitmap?>(null) }
    var askedOnce by remember { mutableStateOf(false) }
    var removeOn by remember { mutableStateOf(Prefs.removeFromGallery(ctx)) }
    var canDelete by remember { mutableStateOf(Shots.canDelete()) }

    fun refresh() {
        access = Shots.hasAccess(ctx); enabled = Prefs.enabled(ctx)
        removeOn = Prefs.removeFromGallery(ctx); canDelete = Shots.canDelete()
        val t = Prefs.lastTime(ctx)
        if (t != lastTime || (thumb == null && t > 0)) {
            thumb = Shots.lastFile(ctx)?.let { f ->
                val o = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(f.path, o)?.asImageBitmap()
            }
        }
        lastTime = t; count = Prefs.count(ctx)
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(Unit) { while (true) { refresh(); delay(1000) } }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        askedOnce = true; refresh(); ShotJob.schedule(ctx)
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            Hero(active = access && enabled, modifier = Modifier.fillMaxWidth().height(250.dp))
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.app_name), fontSize = 40.sp, fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp, color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tagline), fontSize = 17.sp, lineHeight = 23.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 320.dp),
            )
            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = access, label = "access",
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            ) { ok ->
                if (!ok) AccessCard(askedOnce,
                    onAsk = { ask.launch(Shots.READ_PERMISSION) },
                    onSettings = {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                    })
                else Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StatusCard(enabled) { v -> Prefs.setEnabled(ctx, v); enabled = v; ShotJob.schedule(ctx) }
                    LastCard(thumb, lastTime, count)
                    OptionsCard(
                        removeOn = removeOn, canDelete = canDelete,
                        onRemove = { v ->
                            Prefs.setRemoveFromGallery(ctx, v); removeOn = v
                            if (v && !Shots.canDelete()) ctx.startActivity(
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.fromParts("package", ctx.packageName, null)),
                            )
                        },
                        onAddTile = if (Build.VERSION.SDK_INT >= 33) ({
                            ctx.getSystemService(StatusBarManager::class.java).requestAddTileService(
                                ComponentName(ctx, PauseTile::class.java), ctx.getString(R.string.app_name),
                                Icon.createWithResource(ctx, R.drawable.ic_tile), ctx.mainExecutor,
                            ) { }
                        }) else null,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.privacy), fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp),
        content = content,
    )
}

@Composable
private fun AccessCard(asked: Boolean, onAsk: () -> Unit, onSettings: () -> Unit) {
    Card {
        Text(stringResource(R.string.access_title), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.access_body), fontSize = 15.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Palette.accent).clickable(onClick = onAsk),
            contentAlignment = Alignment.Center,
        ) { Text(stringResource(R.string.access_button), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        if (asked) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.access_settings), fontSize = 14.sp, color = Palette.accent,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSettings).padding(vertical = 6.dp), textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatusCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val pulse = rememberInfiniteTransition(label = "pulse")
            val a by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a")
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (enabled) Palette.green.copy(alpha = a) else MaterialTheme.colorScheme.outline))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (enabled) R.string.status_on else R.string.status_off),
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(if (enabled) R.string.status_on_body else R.string.status_off_body),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = Palette.green, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent),
            )
        }
    }
}

@Composable
private fun LastCard(thumb: ImageBitmap?, lastTime: Long, count: Int) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(width = 54.dp, height = 96.dp).shadow(6.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (thumb != null) Image(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (lastTime > 0) stringResource(R.string.last_copied) else stringResource(R.string.none_yet),
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (lastTime > 0) DateUtils.getRelativeTimeSpanString(lastTime, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString()
                    else stringResource(R.string.none_yet_body),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (count > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        LocalContext.current.resources.getQuantityString(R.plurals.count, count, count),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionsCard(removeOn: Boolean, canDelete: Boolean, onRemove: (Boolean) -> Unit, onAddTile: (() -> Unit)?) {
    Card {
        Text(
            stringResource(R.string.options).uppercase(), fontSize = 12.sp, letterSpacing = 0.6.sp,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.remove_title), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.remove_body), fontSize = 14.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (removeOn && !canDelete) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.remove_needs), fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFFFF9F0A))
                }
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = removeOn, onCheckedChange = onRemove,
                colors = SwitchDefaults.colors(checkedTrackColor = Palette.green, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent),
            )
        }
        if (onAddTile != null) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.tile_title), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.tile_body), fontSize = 14.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onAddTile).padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(stringResource(R.string.tile_add), color = Palette.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/* ---------- hero: the two buttons, the flash, the shot flying into the clipboard ---------- */
@Composable
private fun Hero(active: Boolean, modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "hero").animateFloat(
        0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "t",
    )
    val c = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    Canvas(modifier) {
        val u = size.minDimension / 250f
        val phoneW = 112 * u; val phoneH = 222 * u
        val phoneTL = Offset(size.width / 2 - 118 * u, (size.height - phoneH) / 2)
        val boardC = Offset(size.width / 2 + 92 * u, size.height / 2 + 6 * u)

        fun seg(a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
        val press = if (!active) 0f else (seg(0.12f, 0.18f) - seg(0.24f, 0.30f))
        val flash = if (!active) 0f else (seg(0.24f, 0.27f) - seg(0.27f, 0.40f))
        val fly = if (!active) 0f else FastOutSlowInEasing.transform(seg(0.32f, 0.70f))
        val land = if (!active) 0f else (seg(0.68f, 0.74f) - seg(0.82f, 0.95f))
        val check = if (!active) 0f else (seg(0.72f, 0.80f) - seg(0.90f, 0.98f))

        // phone body
        val bodyCol = if (dark) Color(0xFF2C2C2E) else Color(0xFF1C1C1E)
        drawRoundRect(Color.Black.copy(alpha = 0.10f), phoneTL + Offset(0f, 8 * u), Size(phoneW, phoneH), CornerRadius(26 * u))
        drawRoundRect(bodyCol, phoneTL, Size(phoneW, phoneH), CornerRadius(26 * u))
        // side buttons (right edge): power + volume down, pushed in while pressed
        val btnX = phoneTL.x + phoneW - 1.5f * u - press * 3.5f * u
        drawRoundRect(bodyCol, Offset(btnX, phoneTL.y + 46 * u), Size(5 * u, 30 * u), CornerRadius(2.5f * u))
        drawRoundRect(bodyCol, Offset(btnX, phoneTL.y + 92 * u), Size(5 * u, 40 * u), CornerRadius(2.5f * u))
        // screen
        val sTL = phoneTL + Offset(6 * u, 6 * u); val sSize = Size(phoneW - 12 * u, phoneH - 12 * u)
        val screenTop = Color(0xFF5E5CE6); val screenBot = Color(0xFF0A84FF)
        drawScreen(sTL, sSize, 20 * u, screenTop, screenBot, u)
        if (flash > 0f) drawRoundRect(Color.White.copy(alpha = flash * 0.85f), sTL, sSize, CornerRadius(20 * u))

        // clipboard
        val bw = 82 * u; val bh = 104 * u
        val bTL = boardC - Offset(bw / 2, bh / 2)
        val scale = 1f + land * 0.06f
        val bSize = Size(bw * scale, bh * scale); val bTL2 = boardC - Offset(bSize.width / 2, bSize.height / 2)
        drawRoundRect(Color.Black.copy(alpha = 0.06f), bTL2 + Offset(0f, 6 * u), bSize, CornerRadius(18 * u))
        drawRoundRect(c.surface, bTL2, bSize, CornerRadius(18 * u))
        drawRoundRect(c.outline, bTL2, bSize, CornerRadius(18 * u), style = Stroke(1.2f * u))
        drawRoundRect(c.onSurfaceVariant.copy(alpha = 0.55f), Offset(boardC.x - 18 * u, bTL2.y - 7 * u), Size(36 * u, 14 * u), CornerRadius(7 * u))
        // empty lines on the board until the shot lands
        val lineA = 1f - fly
        for (i in 0..2) drawLine(
            c.outline.copy(alpha = lineA), Offset(bTL.x + 18 * u, bTL.y + (34 + i * 20) * u),
            Offset(bTL.x + bw - (18 + i * 12) * u, bTL.y + (34 + i * 20) * u), strokeWidth = 5 * u, cap = StrokeCap.Round,
        )

        // the shot: shrinks from the screen and arcs into the board
        if (fly > 0f) {
            val start = sTL + Offset(sSize.width / 2, sSize.height / 2)
            val end = boardC + Offset(0f, 4 * u)
            val p = Offset(
                start.x + (end.x - start.x) * fly,
                start.y + (end.y - start.y) * fly - sin(fly * Math.PI).toFloat() * 46 * u,
            )
            val w = sSize.width * (1f - 0.5f * fly); val h = sSize.height * (1f - 0.62f * fly)
            val tl = p - Offset(w / 2, h / 2)
            drawRoundRect(Color.Black.copy(alpha = 0.18f * fly), tl + Offset(0f, 5 * u), Size(w, h), CornerRadius(12 * u))
            drawScreen(tl, Size(w, h), 12 * u, screenTop, screenBot, u * (1f - 0.4f * fly))
            drawRoundRect(Color.White, tl, Size(w, h), CornerRadius(12 * u), style = Stroke(2.5f * u))
        }
        // check mark
        if (check > 0f) {
            val r = 15 * u; val cc = Offset(bTL.x + bw - 6 * u, bTL.y + bh - 6 * u)
            drawCircle(Palette.green.copy(alpha = check), r, cc)
            val k = check
            drawLine(Color.White.copy(alpha = k), cc + Offset(-6 * u, 0f), cc + Offset(-1.5f * u, 5 * u), 3.2f * u, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = k), cc + Offset(-1.5f * u, 5 * u), cc + Offset(7 * u, -5 * u), 3.2f * u, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawScreen(tl: Offset, s: Size, r: Float, top: Color, bot: Color, u: Float) {
    drawRoundRect(Brush.verticalGradient(listOf(top, bot), tl.y, tl.y + s.height), tl, s, CornerRadius(r))
    // a few soft "content" blocks so it reads as a screenshot
    val pad = s.width * 0.14f
    drawRoundRect(Color.White.copy(alpha = 0.85f), tl + Offset(pad, s.height * 0.16f), Size(s.width * 0.5f, s.height * 0.05f), CornerRadius(4 * u))
    drawRoundRect(Color.White.copy(alpha = 0.45f), tl + Offset(pad, s.height * 0.26f), Size(s.width * 0.72f, s.height * 0.04f), CornerRadius(4 * u))
    drawRoundRect(Color.White.copy(alpha = 0.25f), tl + Offset(pad, s.height * 0.38f), Size(s.width - 2 * pad, s.height * 0.32f), CornerRadius(10 * u))
}
