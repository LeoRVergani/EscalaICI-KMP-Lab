package br.com.leorvergani.escalaici.kmp.lab.platform

actual fun nowIso(): String = js("(new Date()).toISOString()")
