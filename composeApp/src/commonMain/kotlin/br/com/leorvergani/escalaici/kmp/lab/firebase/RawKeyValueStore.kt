package br.com.leorvergani.escalaici.kmp.lab.firebase

/** Armazenamento chave/valor cru usado pelo cache de escala (nao sensivel - diferente de [SessionTokenStore], nao precisa de criptografia). */
interface RawKeyValueStore {
    fun read(key: String): String?
    fun write(key: String, value: String): Boolean
    fun remove(key: String)
}

expect fun createRawKeyValueStore(): RawKeyValueStore
