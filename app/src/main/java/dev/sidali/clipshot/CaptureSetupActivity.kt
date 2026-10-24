package dev.sidali.clipshot

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay

/**
 * Opened by the Capture tile while its accessibility service is off, and from the main screen.
 * Says in one sentence why Android needs it, and leads to the setting.
 */
class CaptureSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ClipshotTheme { CaptureSetup(onClose = ::finish) } }
    }
}

@Composable
private fun CaptureSetup(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var on by remember { mutableStateOf(CaptureService.isEnabled(ctx)) }
    // The setting is changed in another screen: check again every second while this one is shown.
    LaunchedEffect(Unit) { while (true) { on = CaptureService.isEnabled(ctx); delay(1000) } }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Tokens.gutter),
    ) {
        Spacer(Modifier.height(Tokens.space8))
        TextLink(stringResource(R.string.close), onClose)
        Spacer(Modifier.height(Tokens.space16))
        Text(stringResource(R.string.capture_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Tokens.space12))
        Text(stringResource(R.string.capture_why), style = MaterialTheme.typography.bodyLarge, color = Tokens.colors.secondaryText)
        Spacer(Modifier.height(Tokens.space24))

        AnimatedContent(
            targetState = on, label = "on",
            transitionSpec = { fadeIn(tween(Tokens.fadeInMs)) togetherWith fadeOut(tween(Tokens.fadeOutMs)) },
        ) { ready ->
            Card {
                if (!ready) {
                    Text(stringResource(R.string.capture_steps), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(Tokens.space20))
                    PrimaryButton(stringResource(R.string.capture_open)) { openAccessibility(ctx) }
                    Spacer(Modifier.height(Tokens.space16))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.capture_restricted), style = MaterialTheme.typography.bodySmall,
                            color = Tokens.colors.secondaryText, modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(Tokens.space12))
                        Capsule(stringResource(R.string.app_info)) {
                            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Check()
                        Spacer(Modifier.width(Tokens.space12))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.capture_ready), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(Tokens.space4))
                            Text(stringResource(R.string.capture_ready_body), style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(Modifier.height(Tokens.space20))
                        PrimaryButton(stringResource(R.string.capture_add_tile)) { addCaptureTile(ctx) }
                    }
                    Spacer(Modifier.height(Tokens.space12))
                    TextLink(stringResource(R.string.capture_settings)) { openAccessibility(ctx) }
                }
            }
        }
        Spacer(Modifier.height(Tokens.space24))
    }
}

/** The accessibility settings, with Clipshot highlighted in the list where Settings supports it. */
private fun openAccessibility(ctx: Context) {
    val me = ComponentName(ctx, CaptureService::class.java).flattenToString()
    ctx.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(":settings:fragment_args_key", me)
            .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", me) }),
    )
}

fun addCaptureTile(ctx: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    ctx.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(ctx, CaptureTile::class.java), ctx.getString(R.string.capture_tile),
        Icon.createWithResource(ctx, R.drawable.ic_capture), ctx.mainExecutor,
    ) { }
}
