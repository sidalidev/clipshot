package dev.sidali.clipshot

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    var captureOn by remember { mutableStateOf(BuildConfig.CAPTURE && CaptureService.isEnabled(ctx)) }

    fun refresh() {
        access = Shots.hasAccess(ctx); enabled = Prefs.enabled(ctx)
        removeOn = Prefs.removeFromGallery(ctx); canDelete = Shots.canDelete()
        captureOn = BuildConfig.CAPTURE && CaptureService.isEnabled(ctx)
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Tokens.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Tokens.space24))
        Hero(active = access && enabled, modifier = Modifier.fillMaxWidth().height(Tokens.heroHeight))
        Spacer(Modifier.height(Tokens.space24))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(Tokens.space8))
        Text(
            stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = Tokens.colors.secondaryText, modifier = Modifier.widthIn(max = Tokens.textWidth),
        )
        Spacer(Modifier.height(Tokens.space32))

        AnimatedContent(
            targetState = access, label = "access",
            transitionSpec = { fadeIn(tween(Tokens.fadeInMs)) togetherWith fadeOut(tween(Tokens.fadeOutMs)) },
        ) { ok ->
            if (!ok) AccessCard(askedOnce,
                onAsk = { ask.launch(Shots.READ_PERMISSION) },
                onSettings = {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                })
            else Column(verticalArrangement = Arrangement.spacedBy(Tokens.cardGap)) {
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
                    captureOn = captureOn,
                    onCapture = { if (BuildConfig.CAPTURE) ctx.startActivity(Intent(ctx, CaptureSetupActivity::class.java)) },
                    onAddTile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ({
                        ctx.getSystemService(StatusBarManager::class.java).requestAddTileService(
                            ComponentName(ctx, PauseTile::class.java), ctx.getString(R.string.app_name),
                            Icon.createWithResource(ctx, R.drawable.ic_tile), ctx.mainExecutor,
                        ) { }
                    }) else null,
                )
            }
        }
        Spacer(Modifier.height(Tokens.space24))
        Text(
            stringResource(R.string.privacy), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            color = Tokens.colors.secondaryText, modifier = Modifier.widthIn(max = Tokens.textWidth),
        )
        Spacer(Modifier.height(Tokens.space8))
        TextLink(stringResource(R.string.about)) { AboutActivity.open(ctx) }
        Spacer(Modifier.height(Tokens.space16))
    }
}

@Composable
private fun AccessCard(asked: Boolean, onAsk: () -> Unit, onSettings: () -> Unit) {
    Card {
        Text(stringResource(R.string.access_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Tokens.space8))
        Text(stringResource(R.string.access_body), style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
        Spacer(Modifier.height(Tokens.space20))
        PrimaryButton(stringResource(R.string.access_button), onClick = onAsk)
        if (asked) {
            Spacer(Modifier.height(Tokens.space8))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextLink(stringResource(R.string.access_settings), onSettings)
            }
        }
    }
}

@Composable
private fun StatusCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (enabled) Tokens.colors.success else Tokens.colors.hairline, live = enabled)
            Spacer(Modifier.width(Tokens.space12))
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (enabled) R.string.status_on else R.string.status_off), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (enabled) R.string.status_on_body else R.string.status_off_body),
                    style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText,
                )
            }
            GreenSwitch(enabled, onToggle)
        }
    }
}

@Composable
private fun LastCard(thumb: ImageBitmap?, lastTime: Long, count: Int) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(width = Tokens.thumbWidth, height = Tokens.thumbHeight)
                    .clip(RoundedCornerShape(Tokens.thumbRadius)).background(Tokens.colors.raised),
                contentAlignment = Alignment.Center,
            ) {
                if (thumb != null) Image(thumb, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(Tokens.space16))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (lastTime > 0) R.string.last_copied else R.string.none_yet),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Tokens.space4))
                Text(
                    if (lastTime > 0) DateUtils.getRelativeTimeSpanString(lastTime, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString()
                    else stringResource(R.string.none_yet_body),
                    style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText,
                )
                if (count > 0) {
                    Spacer(Modifier.height(Tokens.space8))
                    Text(pluralStringResource(R.plurals.count, count, count), style = MaterialTheme.typography.bodySmall, color = Tokens.colors.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(title: String, body: String, note: String? = null, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Tokens.space4))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
            if (note != null) {
                Spacer(Modifier.height(Tokens.space8))
                Text(note, style = MaterialTheme.typography.bodySmall, color = Tokens.colors.warning)
            }
        }
        Spacer(Modifier.width(Tokens.space12))
        trailing()
    }
}

@Composable
private fun Divider() {
    Spacer(Modifier.height(Tokens.space16))
    Box(Modifier.fillMaxWidth().height(Tokens.hairline).background(Tokens.colors.hairline))
    Spacer(Modifier.height(Tokens.space16))
}

@Composable
private fun OptionsCard(
    removeOn: Boolean, canDelete: Boolean, onRemove: (Boolean) -> Unit,
    captureOn: Boolean, onCapture: () -> Unit,
    onAddTile: (() -> Unit)?,
) {
    if (!BuildConfig.REMOVE_FROM_GALLERY && !BuildConfig.CAPTURE && onAddTile == null) return
    Card {
        SectionLabel(stringResource(R.string.options))
        Spacer(Modifier.height(Tokens.space16))
        var first = true
        if (BuildConfig.CAPTURE) {
            first = false
            OptionRow(stringResource(R.string.capture_title), stringResource(R.string.capture_body)) {
                if (captureOn) Box(Modifier.pressable(onClick = onCapture)) { Check() }
                else Capsule(stringResource(R.string.capture_setup), onCapture)
            }
        }
        if (BuildConfig.REMOVE_FROM_GALLERY) {
            if (!first) Divider()
            first = false
            OptionRow(
                stringResource(R.string.remove_title), stringResource(R.string.remove_body),
                note = if (removeOn && !canDelete) stringResource(R.string.remove_needs) else null,
            ) { GreenSwitch(removeOn, onRemove) }
        }
        if (onAddTile != null) {
            if (!first) Divider()
            OptionRow(stringResource(R.string.tile_title), stringResource(R.string.tile_body)) {
                Capsule(stringResource(R.string.tile_add), onAddTile)
            }
        }
    }
}

/* ---------- hero: the two buttons, the flash, the shot flying into the clipboard ---------- */
@Composable
private fun Hero(active: Boolean, modifier: Modifier) {
    val t by rememberInfiniteTransition(label = "hero").animateFloat(
        0f, 1f, infiniteRepeatable(tween(Tokens.heroLoopMs, easing = LinearEasing)), label = "t",
    )
    val c = Tokens.colors
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
        drawRoundRect(c.shadow.copy(alpha = 0.10f), phoneTL + Offset(0f, 8 * u), Size(phoneW, phoneH), CornerRadius(26 * u))
        drawRoundRect(c.heroBody, phoneTL, Size(phoneW, phoneH), CornerRadius(26 * u))
        // side buttons (right edge): power + volume down, pushed in while pressed
        val btnX = phoneTL.x + phoneW - 1.5f * u - press * 3.5f * u
        drawRoundRect(c.heroBody, Offset(btnX, phoneTL.y + 46 * u), Size(5 * u, 30 * u), CornerRadius(2.5f * u))
        drawRoundRect(c.heroBody, Offset(btnX, phoneTL.y + 92 * u), Size(5 * u, 40 * u), CornerRadius(2.5f * u))
        // screen
        val sTL = phoneTL + Offset(6 * u, 6 * u); val sSize = Size(phoneW - 12 * u, phoneH - 12 * u)
        drawScreen(sTL, sSize, 20 * u, c.heroTop, c.heroBottom, c.onHero, u)
        if (flash > 0f) drawRoundRect(c.onHero.copy(alpha = flash * 0.85f), sTL, sSize, CornerRadius(20 * u))

        // clipboard
        val bw = 82 * u; val bh = 104 * u
        val bTL = boardC - Offset(bw / 2, bh / 2)
        val scale = 1f + land * 0.06f
        val bSize = Size(bw * scale, bh * scale); val bTL2 = boardC - Offset(bSize.width / 2, bSize.height / 2)
        drawRoundRect(c.shadow.copy(alpha = 0.06f), bTL2 + Offset(0f, 6 * u), bSize, CornerRadius(18 * u))
        drawRoundRect(c.card, bTL2, bSize, CornerRadius(18 * u))
        drawRoundRect(c.hairline, bTL2, bSize, CornerRadius(18 * u), style = Stroke(1.2f * u))
        drawRoundRect(c.secondaryText.copy(alpha = 0.55f), Offset(boardC.x - 18 * u, bTL2.y - 7 * u), Size(36 * u, 14 * u), CornerRadius(7 * u))
        // empty lines on the board until the shot lands
        val lineA = 1f - fly
        for (i in 0..2) drawLine(
            c.hairline.copy(alpha = lineA), Offset(bTL.x + 18 * u, bTL.y + (34 + i * 20) * u),
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
            drawRoundRect(c.shadow.copy(alpha = 0.18f * fly), tl + Offset(0f, 5 * u), Size(w, h), CornerRadius(12 * u))
            drawScreen(tl, Size(w, h), 12 * u, c.heroTop, c.heroBottom, c.onHero, u * (1f - 0.4f * fly))
            drawRoundRect(c.onHero, tl, Size(w, h), CornerRadius(12 * u), style = Stroke(2.5f * u))
        }
        // check mark
        if (check > 0f) {
            val r = 15 * u; val cc = Offset(bTL.x + bw - 6 * u, bTL.y + bh - 6 * u)
            drawCircle(c.success.copy(alpha = check), r, cc)
            drawLine(c.onSuccess.copy(alpha = check), cc + Offset(-6 * u, 0f), cc + Offset(-1.5f * u, 5 * u), 3.2f * u, StrokeCap.Round)
            drawLine(c.onSuccess.copy(alpha = check), cc + Offset(-1.5f * u, 5 * u), cc + Offset(7 * u, -5 * u), 3.2f * u, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawScreen(tl: Offset, s: Size, r: Float, top: Color, bot: Color, ink: Color, u: Float) {
    drawRoundRect(Brush.verticalGradient(listOf(top, bot), tl.y, tl.y + s.height), tl, s, CornerRadius(r))
    // a few soft "content" blocks so it reads as a screenshot
    val pad = s.width * 0.14f
    drawRoundRect(ink.copy(alpha = 0.85f), tl + Offset(pad, s.height * 0.16f), Size(s.width * 0.5f, s.height * 0.05f), CornerRadius(4 * u))
    drawRoundRect(ink.copy(alpha = 0.45f), tl + Offset(pad, s.height * 0.26f), Size(s.width * 0.72f, s.height * 0.04f), CornerRadius(4 * u))
    drawRoundRect(ink.copy(alpha = 0.25f), tl + Offset(pad, s.height * 0.38f), Size(s.width - 2 * pad, s.height * 0.32f), CornerRadius(10 * u))
}
