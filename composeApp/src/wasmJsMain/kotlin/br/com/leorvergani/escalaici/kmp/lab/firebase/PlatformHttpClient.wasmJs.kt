package br.com.leorvergani.escalaici.kmp.lab.firebase

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Js)
