package ba.osgrivice.kviz.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Noć — osnovna pozadina igračkih ekrana
val Noc = Color(0xFF141B3D)
val Noc2 = Color(0xFF1F2A5C)
val Noc3 = Color(0xFF0C1230)
val NocTekst = Color(0xFFAAB3D9)
val NocTekst2 = Color(0xFFC9D0EE)

// Šafran — akcija, nagrada
val Safran = Color(0xFFF5B54A)
val Safran2 = Color(0xFFE0902A)
val SafranTamno = Color(0xFF5B4A20)

// Tirkiz — napredak, savladano
val Tirkiz = Color(0xFF4FD1C5)
val Tirkiz2 = Color(0xFF2BAFA3)

// Papir — ekran pitanja
val Krem = Color(0xFFFBF6EC)
val Tinta = Color(0xFF1B1F33)
val Siva = Color(0xFF6B7086)
val Linija = Color(0xFFE8E1D2)
val Tackica = Color(0xFFDDD5C3)

// Semantičke
val Ok = Color(0xFF2FBF71)
val OkBg = Color(0xFFEAF8F0)
val Ne = Color(0xFFE85D5D)
val NeBg = Color(0xFFFDECEC)

val PozadinaNoc: Brush = Brush.verticalGradient(0f to Noc2, 0.35f to Noc, 1f to Noc3)
val PozadinaRezultat: Brush = Brush.verticalGradient(0f to Color(0xFF2A3A7A), 0.5f to Noc, 1f to Noc3)
