package com.supreme.priceintelligence.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.supreme.priceintelligence.resources.Res
import com.supreme.priceintelligence.resources.inter_variable
import com.supreme.priceintelligence.resources.lato_bold
import com.supreme.priceintelligence.resources.lato_medium
import com.supreme.priceintelligence.resources.lato_regular
import com.supreme.priceintelligence.resources.lato_semibold
import com.supreme.priceintelligence.resources.montserrat_variable
import com.supreme.priceintelligence.resources.open_sans_variable
import com.supreme.priceintelligence.resources.poppins_bold
import com.supreme.priceintelligence.resources.poppins_medium
import com.supreme.priceintelligence.resources.poppins_regular
import com.supreme.priceintelligence.resources.poppins_semibold
import com.supreme.priceintelligence.resources.roboto_variable
import com.supreme.priceintelligence.settings.AppFontStyle
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

private val BaseTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
private fun variableFontFamily(
    resource: FontResource
): FontFamily =
    FontFamily(
        Font(
            resource = resource,
            weight = FontWeight.Normal
        ),
        Font(
            resource = resource,
            weight = FontWeight.Medium
        ),
        Font(
            resource = resource,
            weight = FontWeight.SemiBold
        ),
        Font(
            resource = resource,
            weight = FontWeight.Bold
        )
    )

@Composable
private fun staticFontFamily(
    regular: FontResource,
    medium: FontResource,
    semiBold: FontResource,
    bold: FontResource
): FontFamily =
    FontFamily(
        Font(
            resource = regular,
            weight = FontWeight.Normal
        ),
        Font(
            resource = medium,
            weight = FontWeight.Medium
        ),
        Font(
            resource = semiBold,
            weight = FontWeight.SemiBold
        ),
        Font(
            resource = bold,
            weight = FontWeight.Bold
        )
    )

@Composable
internal fun supremeTypography(
    fontStyle: AppFontStyle
): Typography {
    val fontFamily = when (fontStyle) {
        AppFontStyle.SYSTEM ->
            FontFamily.Default

        AppFontStyle.ROBOTO ->
            variableFontFamily(
                resource = Res.font.roboto_variable
            )

        AppFontStyle.INTER ->
            variableFontFamily(
                resource = Res.font.inter_variable
            )

        AppFontStyle.OPEN_SANS ->
            variableFontFamily(
                resource = Res.font.open_sans_variable
            )

        AppFontStyle.LATO ->
            staticFontFamily(
                regular = Res.font.lato_regular,
                medium = Res.font.lato_medium,
                semiBold = Res.font.lato_semibold,
                bold = Res.font.lato_bold
            )

        AppFontStyle.MONTSERRAT ->
            variableFontFamily(
                resource = Res.font.montserrat_variable
            )

        AppFontStyle.POPPINS ->
            staticFontFamily(
                regular = Res.font.poppins_regular,
                medium = Res.font.poppins_medium,
                semiBold = Res.font.poppins_semibold,
                bold = Res.font.poppins_bold
            )

        AppFontStyle.TECHNICAL ->
            FontFamily.Monospace
    }

    fun TextStyle.customized(): TextStyle =
        copy(
            fontFamily = fontFamily
        )

    return Typography(
        displayLarge =
            BaseTypography.displayLarge.customized(),
        displayMedium =
            BaseTypography.displayMedium.customized(),
        displaySmall =
            BaseTypography.displaySmall.customized(),
        headlineLarge =
            BaseTypography.headlineLarge.customized(),
        headlineMedium =
            BaseTypography.headlineMedium.customized(),
        headlineSmall =
            BaseTypography.headlineSmall.customized(),
        titleLarge =
            BaseTypography.titleLarge.customized(),
        titleMedium =
            BaseTypography.titleMedium.customized(),
        titleSmall =
            BaseTypography.titleSmall.customized(),
        bodyLarge =
            BaseTypography.bodyLarge.customized(),
        bodyMedium =
            BaseTypography.bodyMedium.customized(),
        bodySmall =
            BaseTypography.bodySmall.customized(),
        labelLarge =
            BaseTypography.labelLarge.customized(),
        labelMedium =
            BaseTypography.labelMedium.customized(),
        labelSmall =
            BaseTypography.labelSmall.customized()
    )
}