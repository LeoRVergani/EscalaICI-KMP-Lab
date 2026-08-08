package br.com.leorvergani.escalaici.kmp.lab.platform

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

actual fun nowIso(): String = ISO_FORMATTER.format(Instant.now())
