package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.model.Member
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumBackground
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors

@Composable
internal fun LoginGateScreen(
    members: List<Member>,
    onSelectMember: (Member) -> Unit
) {
    LabPremiumBackground {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(LabColors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.White)
                }
                Text(
                    "Escala ICI",
                    color = LabColors.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Login fake do laboratório KMP (FASE 9f). Sem MSAL ou Firebase reais — selecione um colaborador demonstrativo para continuar.",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                LabCard(title = "Selecionar colaborador demo", icon = Icons.Default.Person) {
                    members.forEach { member ->
                        OutlinedButton(
                            onClick = { onSelectMember(member) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Entrar como ${member.displayName}")
                        }
                    }
                }
                Text(
                    "Esta tela representa o LoginGate do app real, sem autenticação verdadeira.",
                    color = LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
