package app.oribu.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.data.OnboardingPreferences
import app.oribu.ui.navigation.Routes
import app.oribu.ui.screens.onboarding.steps.ApiKeysStep
import app.oribu.ui.screens.onboarding.steps.PermissionsStep
import app.oribu.ui.screens.onboarding.steps.StorageStep
import app.oribu.ui.screens.onboarding.steps.WelcomeThemeStep

/**
 * Fluxo de primeiro acesso: máquina de estado simples por índice (sem sub-rotas), no molde do
 * onboarding do Rokku. Nenhum passo é obrigatório — diferente do Rokku, o Oribu não tem hoje
 * nenhuma permissão que bloqueie o uso do app, então "Próximo" está sempre habilitado.
 */
@Composable
fun OnboardingScreen(navController: NavController) {
    val steps =
        remember {
            listOf<@Composable () -> Unit>(
                { WelcomeThemeStep() },
                { StorageStep() },
                { PermissionsStep() },
                { ApiKeysStep() },
            )
        }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val isLast = currentStep == steps.lastIndex

    fun finish() {
        OnboardingPreferences.setCompleted()
        navController.navigate(Routes.HOME) {
            popUpTo(Routes.ONBOARDING) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = { currentStep-- }) { Text("Voltar") }
                } else {
                    Row(Modifier.width(1.dp)) {}
                }
                Button(onClick = { if (isLast) finish() else currentStep++ }) {
                    Text(if (isLast) "Concluir" else "Próximo")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            steps[currentStep]()
        }
    }
}
