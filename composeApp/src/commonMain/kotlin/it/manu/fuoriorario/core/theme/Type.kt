package it.manu.fuoriorario.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fuoriorario.composeapp.generated.resources.Res
import fuoriorario.composeapp.generated.resources.instrument_sans_400
import fuoriorario.composeapp.generated.resources.instrument_sans_500
import fuoriorario.composeapp.generated.resources.instrument_sans_600
import fuoriorario.composeapp.generated.resources.saira_condensed_500
import fuoriorario.composeapp.generated.resources.saira_condensed_600
import fuoriorario.composeapp.generated.resources.saira_condensed_700
import org.jetbrains.compose.resources.Font

/** Headings and numbers. */
@Composable
private fun displayFont() = FontFamily(
    Font(Res.font.saira_condensed_500, FontWeight.Medium),
    Font(Res.font.saira_condensed_600, FontWeight.SemiBold),
    Font(Res.font.saira_condensed_700, FontWeight.Bold)
)

/** Body text. */
@Composable
private fun bodyFont() = FontFamily(
    Font(Res.font.instrument_sans_400, FontWeight.Normal),
    Font(Res.font.instrument_sans_500, FontWeight.Medium),
    Font(Res.font.instrument_sans_600, FontWeight.SemiBold)
)

/** Prototype sizes: h1 26, h2 22 (uppercase), h3 18, body 15/1.45, label 11.5 uppercase. */
@Composable
fun foTypography(): Typography {
    val display = displayFont()
    val body = bodyFont()
    val heading = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, letterSpacing = 0.01.em)
    return Typography(
        headlineMedium = heading.copy(fontSize = 26.sp, lineHeight = 26.sp),
        titleLarge = heading.copy(fontSize = 22.sp, lineHeight = 26.sp),
        titleMedium = heading.copy(fontSize = 18.sp, lineHeight = 22.sp),
        bodyLarge = TextStyle(fontFamily = body, fontSize = 15.sp, lineHeight = 21.75.sp),
        bodyMedium = TextStyle(fontFamily = body, fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.5.sp,
            letterSpacing = 0.08.em
        )
    )
}
