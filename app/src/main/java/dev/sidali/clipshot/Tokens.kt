package dev.sidali.clipshot

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The one place for colors, sizes, type, springs and the shared components.
 * Colors come from the phone (Material You, drawn from the wallpaper). Screens never pick a raw
 * color: they ask Tokens.colors for a role.
 */
object Tokens {
    // 4 dp grid
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp

    val gutter = 24.dp
    val cardRadius = 22.dp
    val cardPadding = 20.dp
    val cardGap = 14.dp
    val buttonRadius = 14.dp
    val buttonHeight = 54.dp
    val heroHeight = 250.dp
    val textWidth = 320.dp
    val dot = 10.dp
    val check = 28.dp
    val hairline = 1.dp
    /** Smallest height of anything tappable. */
    val touchTarget = 48.dp
    /** Thumbnail of the last screenshot: a phone screen in miniature. */
    val thumbWidth = 54.dp
    val thumbHeight = 96.dp
    val thumbRadius = 10.dp

    /** Tinted (secondary) buttons: the role color at 12 %. */
    const val tintAlpha = 0.12f
    /** A disabled action. */
    const val disabledAlpha = 0.38f

    const val pressedScale = 0.97f
    const val pressedAlpha = 0.85f
    /** Press feedback: quick and slightly bouncy, interruptible. */
    const val pressDamping = 0.55f
    const val pressStiffness = 900f
    const val fadeInMs = 300
    const val fadeOutMs = 200
    const val pulseMs = 1400
    const val heroLoopMs = 3600

    /** The roles the screens use, resolved from the phone's palette. */
    val colors: Roles
        @Composable @ReadOnlyComposable get() = LocalRoles.current
}

/** What each color is for. */
@Immutable
class Roles(
    /** Page behind the cards. */
    val page: Color,
    /** Cards. */
    val card: Color,
    /** One step above a card: capsules, the empty thumbnail. */
    val raised: Color,
    val hairline: Color,
    val text: Color,
    val secondaryText: Color,
    /** The one filled action per screen, and links. */
    val accent: Color,
    val onAccent: Color,
    /** On, copied, switches. Green stays green, nudged toward the palette. */
    val success: Color,
    val onSuccess: Color,
    /** Something still needs doing. */
    val warning: Color,
    /** Destructive actions (Erase everything): the phone's error red. */
    val danger: Color,
    val onDanger: Color,
    /** Hero phone: a strong gradient from the palette, light content on it. */
    val heroTop: Color,
    val heroBottom: Color,
    val onHero: Color,
    val heroBody: Color,
    /** Soft shadows under the hero drawings. */
    val shadow: Color,
)

private val LocalRoles = staticCompositionLocalOf<Roles> { error("ClipshotTheme is missing") }

/**
 * Used where Android has no wallpaper colors (Android 11, some builds): an indigo tonal palette, the
 * family of the app icon. With the semantic colors below, these are the only literal colors in the
 * app's code (the launch window and the icon live in res/).
 */
private object Fallback {
    val light = lightColorScheme(
        primary = Color(0xFF4B4FC4), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE1E0FF), onPrimaryContainer = Color(0xFF0B0A72),
        inversePrimary = Color(0xFFC0C1FF),
        secondary = Color(0xFF5C5D72), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE1E0F9), onSecondaryContainer = Color(0xFF191A2C),
        tertiary = Color(0xFF7A4F72), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD7F3), onTertiaryContainer = Color(0xFF2F0D2B),
        error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B21),
        surface = Color(0xFFFBF8FF), onSurface = Color(0xFF1B1B21),
        surfaceVariant = Color(0xFFE4E1EC), onSurfaceVariant = Color(0xFF47464F),
        surfaceTint = Color(0xFF4B4FC4),
        inverseSurface = Color(0xFF303036), inverseOnSurface = Color(0xFFF2EFF7),
        outline = Color(0xFF777680), outlineVariant = Color(0xFFC8C5D0), scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFBF8FF), surfaceDim = Color(0xFFDBD9E0),
        surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF5F2FA),
        surfaceContainer = Color(0xFFEFEDF4), surfaceContainerHigh = Color(0xFFEAE7EF),
        surfaceContainerHighest = Color(0xFFE4E1E9),
    )
    val dark = darkColorScheme(
        primary = Color(0xFFC0C1FF), onPrimary = Color(0xFF1C1D93),
        primaryContainer = Color(0xFF3336AB), onPrimaryContainer = Color(0xFFE1E0FF),
        inversePrimary = Color(0xFF4B4FC4),
        secondary = Color(0xFFC5C4DD), onSecondary = Color(0xFF2E2F42),
        secondaryContainer = Color(0xFF444559), onSecondaryContainer = Color(0xFFE1E0F9),
        tertiary = Color(0xFFEAB5DD), onTertiary = Color(0xFF472241),
        tertiaryContainer = Color(0xFF603859), onTertiaryContainer = Color(0xFFFFD7F3),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131318), onBackground = Color(0xFFE4E1E9),
        surface = Color(0xFF131318), onSurface = Color(0xFFE4E1E9),
        surfaceVariant = Color(0xFF47464F), onSurfaceVariant = Color(0xFFC8C5D0),
        surfaceTint = Color(0xFFC0C1FF),
        inverseSurface = Color(0xFFE4E1E9), inverseOnSurface = Color(0xFF303036),
        outline = Color(0xFF918F9A), outlineVariant = Color(0xFF47464F), scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF39383F), surfaceDim = Color(0xFF131318),
        surfaceContainerLowest = Color(0xFF0E0E13), surfaceContainerLow = Color(0xFF1B1B21),
        surfaceContainer = Color(0xFF1F1F25), surfaceContainerHigh = Color(0xFF2A292F),
        surfaceContainerHighest = Color(0xFF35343A),
    )

    // Semantic colors, before harmonizing. Light tones stay dark enough for a white thumb (5:1).
    val successLight = Color(0xFF187A35)
    val onSuccessLight = Color(0xFFFFFFFF)
    val successDark = Color(0xFF7BD88F)
    val onSuccessDark = Color(0xFF00391A)
    val warningLight = Color(0xFF8A5100)
    val warningDark = Color(0xFFFFB86B)
}

/** Rotates [color]'s hue toward [toward] by half the gap, at most 15 degrees, like Material's harmonize. */
private fun harmonize(color: Color, toward: Color): Color {
    val a = FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    val b = FloatArray(3).also { android.graphics.Color.colorToHSV(toward.toArgb(), it) }
    val diff = ((b[0] - a[0] + 540f) % 360f) - 180f
    a[0] = (a[0] + diff.coerceIn(-30f, 30f) / 2f + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(a))
}

private fun roles(c: ColorScheme, dark: Boolean) = Roles(
    // Light: a tinted page under near-white cards. Dark: deep page, cards one tone up.
    page = if (dark) c.surface else c.surfaceContainer,
    card = if (dark) c.surfaceContainer else c.surfaceContainerLowest,
    raised = if (dark) c.surfaceContainerHighest else c.surfaceContainerHigh,
    hairline = c.outlineVariant,
    text = c.onSurface,
    secondaryText = c.onSurfaceVariant,
    accent = c.primary, onAccent = c.onPrimary,
    success = harmonize(if (dark) Fallback.successDark else Fallback.successLight, c.primary),
    onSuccess = if (dark) Fallback.onSuccessDark else Fallback.onSuccessLight,
    warning = harmonize(if (dark) Fallback.warningDark else Fallback.warningLight, c.primary),
    danger = c.error, onDanger = c.onError,
    // Mid tones in both themes, so light content reads on them.
    heroTop = if (dark) c.tertiaryContainer else c.tertiary,
    heroBottom = if (dark) c.inversePrimary else c.primary,
    onHero = if (dark) c.onPrimaryContainer else c.onPrimary,
    heroBody = if (dark) c.surfaceContainerHighest else c.inverseSurface,
    shadow = c.scrim,
)

@Composable
fun ClipshotTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    // Android 12 and later: the colors of the phone, from the wallpaper (GrapheneOS included).
    val scheme = remember(dark, ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) }
                .getOrElse { if (dark) Fallback.dark else Fallback.light }
        } else if (dark) Fallback.dark else Fallback.light
    }
    val r = remember(scheme, dark) { roles(scheme, dark) }
    // Large, tight type.
    val type = Typography().copy(
        displaySmall = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, lineHeight = 46.sp),
        headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp, lineHeight = 40.sp),
        titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.2).sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = (-0.1).sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
    )
    val shapes = Shapes(large = RoundedCornerShape(Tokens.cardRadius))
    CompositionLocalProvider(LocalRoles provides r) {
        MaterialTheme(colorScheme = scheme, typography = type, shapes = shapes) {
            Surface(color = r.page, contentColor = r.text, content = content)
        }
    }
}

/**
 * Apple-like press: shrinks to 0.97 on a spring from the first frame, no Material ripple, and a light
 * tick through the view (so the system "touch feedback" setting is respected).
 */
fun Modifier.pressable(
    role: Role = Role.Button,
    haptic: Int = HapticFeedbackConstants.CLOCK_TICK,
    onClick: () -> Unit,
): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val view = LocalView.current
    val s by animateFloatAsState(
        if (pressed) Tokens.pressedScale else 1f,
        spring(dampingRatio = Tokens.pressDamping, stiffness = Tokens.pressStiffness),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s; alpha = if (pressed) Tokens.pressedAlpha else 1f }
        .clickable(interactionSource = src, indication = null, role = role) {
            view.performHapticFeedback(haptic)
            onClick()
        }
}

/** Rounded card, no shadow. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.cardRadius)).background(Tokens.colors.card).padding(Tokens.cardPadding),
        content = content,
    )
}

/** Small uppercase label at the top of a card. */
@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Tokens.colors.secondaryText)
}

/** Large filled action: one per screen. */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(Tokens.buttonHeight)
            .pressable(haptic = HapticFeedbackConstants.CONFIRM, onClick = onClick)
            .clip(RoundedCornerShape(Tokens.buttonRadius)).background(Tokens.colors.accent),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Tokens.colors.onAccent, style = MaterialTheme.typography.labelLarge) }
}

/**
 * Destructive action in the semantic red: tinted at 12 % ([filled] false) or filled, for the final
 * confirmation. Dimmed and inert while not [enabled].
 */
@Composable
fun DangerButton(text: String, filled: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Tokens.colors
    Box(
        modifier.fillMaxWidth().height(Tokens.buttonHeight)
            .then(if (enabled) Modifier.pressable(haptic = if (filled) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK, onClick = onClick) else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else Tokens.disabledAlpha }
            .clip(RoundedCornerShape(Tokens.buttonRadius))
            .background(if (filled) c.danger else c.danger.copy(alpha = Tokens.tintAlpha)),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (filled) c.onDanger else c.danger, style = MaterialTheme.typography.labelLarge) }
}

/** Small capsule, for an action inside a row. */
@Composable
fun Capsule(text: String, onClick: () -> Unit) {
    Box(
        Modifier.pressable(onClick = onClick).clip(RoundedCornerShape(50)).background(Tokens.colors.raised)
            .padding(horizontal = Tokens.space16, vertical = Tokens.space8),
    ) { Text(text, color = Tokens.colors.accent, style = MaterialTheme.typography.labelMedium) }
}

/** Plain text link, centered. */
@Composable
fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = Tokens.colors.accent,
        modifier = Modifier.pressable(onClick = onClick).padding(vertical = Tokens.space8),
    )
}

/** Green switch, with a tick as it flips. */
@Composable
fun GreenSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val view = LocalView.current
    Switch(
        checked = checked,
        onCheckedChange = {
            view.performHapticFeedback(
                when {
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.CLOCK_TICK
                    it -> HapticFeedbackConstants.TOGGLE_ON
                    else -> HapticFeedbackConstants.TOGGLE_OFF
                },
            )
            onChange(it)
        },
        colors = SwitchDefaults.colors(
            checkedTrackColor = Tokens.colors.success, checkedThumbColor = Tokens.colors.onSuccess,
            checkedIconColor = Tokens.colors.success, checkedBorderColor = Color.Transparent,
        ),
    )
}

/** Status dot, pulsing when [live]. */
@Composable
fun StatusDot(color: Color, live: Boolean) {
    val t = rememberInfiniteTransition(label = "dot")
    val a by t.animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(Tokens.pulseMs, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a",
    )
    Box(Modifier.size(Tokens.dot).clip(CircleShape).background(if (live) color.copy(alpha = a) else color))
}

/** Round green check. */
@Composable
fun Check() {
    val bg = Tokens.colors.success
    val ink = Tokens.colors.onSuccess
    Canvas(Modifier.size(Tokens.check)) {
        drawCircle(bg)
        val u = size.minDimension / 28f
        drawLine(ink, Offset(8 * u, 14.5f * u), Offset(12.5f * u, 19 * u), 3f * u, StrokeCap.Round)
        drawLine(ink, Offset(12.5f * u, 19 * u), Offset(20 * u, 9.5f * u), 3f * u, StrokeCap.Round)
    }
}
