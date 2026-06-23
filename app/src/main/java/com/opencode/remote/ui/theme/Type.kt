package com.opencode.remote.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.font
import androidx.compose.ui.unit.sp

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms",
    providerPackage = "com.google.android.gms",
    certificates = listOf("com.google.android.gms")
)

val ArabicFont = FontFamily(
    Font(googleFont = GoogleFont("Noto Sans Arabic"), fontLoader = provider),
    Font(googleFont = GoogleFont("Noto Sans Arabic"), weight = FontWeight.Bold, fontLoader = provider)
)

val RobotoFont = FontFamily(
    Font(googleFont = GoogleFont("Roboto"), fontLoader = provider),
    Font(googleFont = GoogleFont("Roboto"), weight = FontWeight.Bold, fontLoader = provider)
)
