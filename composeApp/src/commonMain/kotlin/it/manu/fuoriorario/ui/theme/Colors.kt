package it.manu.fuoriorario.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Design tokens from docs/prototype/fuori-orario.html (`:root` CSS variables). */
@Immutable
data class FuoriOrarioColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val accentInk: Color,
    val accentSoft: Color,
    val court: Color,
    val courtLine: Color,
    val hot: Color,
    val even: Color,
    val cold: Color,
    val none: Color,
    val good: Color
)

val LightColors = FuoriOrarioColors(
    bg = Color(0xFFECEFF2),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF5F6F8),
    ink = Color(0xFF141A22),
    muted = Color(0xFF5B6571),
    line = Color(0xFFD3D9E0),
    accent = Color(0xFFD9531A),
    accentInk = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFFBE3D7),
    court = Color(0xFFEAD6B6),
    courtLine = Color(0xFF7B4F25),
    hot = Color(0xFFD9531A),
    even = Color(0xFFC7AD83),
    cold = Color(0xFF3569B8),
    none = Color(0xFFD9CDB9),
    good = Color(0xFF2C8A57)
)

val DarkColors = FuoriOrarioColors(
    bg = Color(0xFF0E1217),
    surface = Color(0xFF171C23),
    surface2 = Color(0xFF1E242C),
    ink = Color(0xFFE7EAEE),
    muted = Color(0xFF9AA3AE),
    line = Color(0xFF2B323C),
    accent = Color(0xFFFF7A3D),
    accentInk = Color(0xFF1A0D06),
    accentSoft = Color(0xFF3A2216),
    court = Color(0xFF33291F),
    courtLine = Color(0xFFD8B88A),
    hot = Color(0xFFFF7A3D),
    even = Color(0xFF7A6A52),
    cold = Color(0xFF5C9CF2),
    none = Color(0xFF4A3F32),
    good = Color(0xFF4CC283)
)
