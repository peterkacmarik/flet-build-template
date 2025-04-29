package com.example.workation.util

fun formatTime(hours: String, minutes: String, seconds: String) =
    "${hours}:${minutes}:${seconds}"

fun Int.pad() = toString().padStart(2, '0')