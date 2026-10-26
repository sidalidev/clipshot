package dev.sidali.clipshot

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/*
 * Permissions: every access Clipshot uses, why, and what depends on it. Turning one on and turning it
 * off are the same single tap: off, the row asks (system dialog) or opens the exact settings page;
 * on, it opens the settings page where it can be turned off. The state is read again every time the
 * screen comes back, and nothing is ever asked twice in a row on its own.
 */

/** The accesses Clipshot can use. All files access and the accessibility service are full flavor only. */
enum class Access { PHOTOS, ALL_FILES, CAPTURE }

/** What a tap on a row does. */
enum class Tap { ASK, OPEN_SETTINGS }

object PermissionRules {
    /** The rows to show, in this order, for a build with or without the optional features. */
    fun rows(removeFromGallery: Boolean, capture: Boolean): List<Access> = buildList {
        add(Access.PHOTOS)
        if (removeFromGallery) add(Access.ALL_FILES)
        if (capture) add(Access.CAPTURE)
    }

    /**
     * On: the settings page, where it can be turned off. Off: the system dialog for the photos
     * permission when Android will still show it (never asked, or asked once and refused), else the
     * settings page. Special accesses only exist in settings.
     */
    fun tap(access: Access, granted: Boolean, askedBefore: Boolean, canAskAgain: Boolean): Tap = when {
        granted -> Tap.OPEN_SETTINGS
        access != Access.PHOTOS -> Tap.OPEN_SETTINGS
        !askedBefore || canAskAgain -> Tap.ASK
        else -> Tap.OPEN_SETTINGS
    }

    fun granted(ctx: Context, access: Access): Boolean = when (access) {
        Access.PHOTOS -> Shots.hasAccess(ctx)
        Access.ALL_FILES -> Shots.canDelete()
        Access.CAPTURE -> BuildConfig.CAPTURE && CaptureService.isEnabled(ctx)
    }
}

/** The exact settings page for [access]: the same page turns it on and off. */
fun openSettingsFor(ctx: Context, access: Access) {
    val pkg = Uri.fromParts("package", ctx.packageName, null)
    when (access) {
        Access.PHOTOS -> ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg))
        Access.ALL_FILES -> try {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkg))
        } catch (_: ActivityNotFoundException) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
        Access.CAPTURE -> openAccessibility(ctx)
    }
}

@Composable
internal fun Permissions(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val rows = remember { PermissionRules.rows(BuildConfig.REMOVE_FROM_GALLERY, BuildConfig.CAPTURE) }
    fun read() = rows.associateWith { PermissionRules.granted(ctx, it) }
    var state by remember { mutableStateOf(read()) }
    var removeOn by remember { mutableStateOf(Prefs.removeFromGallery(ctx)) }

    // Every return to the screen (from Settings, the system dialog, or the app switcher).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { state = read(); removeOn = Prefs.removeFromGallery(ctx) }
        }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        Prefs.setPhotosAsked(ctx); state = read(); ShotJob.schedule(ctx)
    }

    fun onTap(access: Access, granted: Boolean) {
        val canAskAgain = (ctx as? Activity)?.shouldShowRequestPermissionRationale(Shots.READ_PERMISSION) ?: false
        when (PermissionRules.tap(access, granted, Prefs.photosAsked(ctx), canAskAgain)) {
            Tap.ASK -> ask.launch(Shots.READ_PERMISSION)
            Tap.OPEN_SETTINGS -> openSettingsFor(ctx, access)
        }
    }

    Page(stringResource(R.string.back), stringResource(R.string.perms_title), onClose) {
        Text(stringResource(R.string.perms_intro), style = MaterialTheme.typography.bodyLarge, color = Tokens.colors.secondaryText)
        Card {
            rows.forEachIndexed { i, access ->
                if (i > 0) { Spacer(Modifier.height(Tokens.space8)); Hairline(); Spacer(Modifier.height(Tokens.space8)) }
                val granted = state[access] == true
                PermissionRow(access, granted, removeOn) { onTap(access, granted) }
            }
        }
        Text(stringResource(R.string.perms_boot), style = MaterialTheme.typography.bodySmall, color = Tokens.colors.secondaryText)
    }
}

@Composable
private fun PermissionRow(access: Access, granted: Boolean, removeOn: Boolean, onClick: () -> Unit) {
    val title = stringResource(
        when (access) { Access.PHOTOS -> R.string.perm_photos; Access.ALL_FILES -> R.string.perm_files; Access.CAPTURE -> R.string.perm_capture },
    )
    val why = stringResource(
        when (access) { Access.PHOTOS -> R.string.perm_photos_why; Access.ALL_FILES -> R.string.perm_files_why; Access.CAPTURE -> R.string.perm_capture_why },
    )
    val off = stringResource(
        when (access) {
            Access.PHOTOS -> R.string.perm_photos_off
            Access.ALL_FILES -> if (removeOn) R.string.perm_files_off else R.string.perm_files_unused
            Access.CAPTURE -> R.string.perm_capture_off
        },
    )
    val status = stringResource(if (granted) R.string.perm_allowed else R.string.perm_not_allowed)
    val action = stringResource(
        when {
            granted -> R.string.perm_turn_off
            access == Access.PHOTOS -> R.string.perm_allow
            else -> R.string.perm_turn_on
        },
    )
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.touchTarget)
            .pressable(onClickLabel = action, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = status },
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Tokens.space4))
            Text(why, style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
            if (!granted) {
                Spacer(Modifier.height(Tokens.space8))
                Text(off, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(Tokens.space8))
            Text(action, style = MaterialTheme.typography.labelMedium, color = Tokens.colors.accent)
        }
        Spacer(Modifier.width(Tokens.space12))
        if (granted) Check() else EmptyCheck()
    }
}
