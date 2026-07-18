package br.com.leorvergani.escalaici.identity

data class DemoPersona(
    val personaId: String,
    val memberId: String,
    val displayName: String,
    val fictitiousLogin: String,
    val fictitiousEmail: String,
    val shortDescription: String
)

object DemoPersonaCatalog {
    val personas: List<DemoPersona> = listOf(
        DemoPersona(
            personaId = "persona-demo-soc-analyst-1",
            memberId = "member-demo-soc-01",
            displayName = "Analista SOC Demo 1",
            fictitiousLogin = "analista.soc.demo1",
            fictitiousEmail = "analista.soc.demo1@example.invalid",
            shortDescription = "Equipe SOC Demonstração"
        ),
        DemoPersona(
            personaId = "persona-demo-seguranca-analyst-1",
            memberId = "member-demo-seguranca-01",
            displayName = "Analista de Segurança Demo 1",
            fictitiousLogin = "analista.seguranca.demo1",
            fictitiousEmail = "analista.seguranca.demo1@example.invalid",
            shortDescription = "Equipe Segurança da Informação Demonstração"
        ),
        DemoPersona(
            personaId = "persona-demo-gestor-seguranca",
            memberId = "member-demo-gestor-seguranca",
            displayName = "Gestor de Segurança Demo",
            fictitiousLogin = "gestor.seguranca.demo",
            fictitiousEmail = "gestor.seguranca.demo@example.invalid",
            shortDescription = "Administra SOC e Segurança da Informação Demonstração"
        )
    )
}
