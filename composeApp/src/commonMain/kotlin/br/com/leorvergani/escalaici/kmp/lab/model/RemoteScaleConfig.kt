package br.com.leorvergani.escalaici.kmp.lab.model

/**
 * Mesmo link Dropbox que o app Android real usa para o usuário comum ler a
 * escala publicada (`DropboxCloudConfig.DROPBOX_SCALE_URL` no `EscalaSOC`) —
 * é um shared link com `dl=1` (força download direto), não uma API key.
 * Já é um valor público, versionado no repositório do app real.
 */
object RemoteScaleConfig {
    const val DROPBOX_SCALE_URL: String =
        "https://www.dropbox.com/scl/fi/ysokgourqysx9hfjytdgy/Escala-SOC-Controle-Atual.xls?rlkey=z73ut8k7w0dzclhbqxsolky8n&st=a1h10mb7&dl=1"
    const val DROPBOX_FILE_NAME: String = "Escala-SOC-Controle-Atual.xls"
}
