package app.oribu.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import app.oribu.data.OnboardingPreferences
import app.oribu.ui.navigation.Routes
import app.oribu.ui.screens.onboarding.steps.ApiKeysStep
import app.oribu.ui.screens.onboarding.steps.PermissionsStep
import app.oribu.ui.screens.onboarding.steps.StorageStep
import app.oribu.ui.screens.onboarding.steps.WelcomeThemeStep

/**
 * Fluxo de primeiro acesso, na organização visual do onboarding do Rokku: cabeçalho fixo
 * (ícone de foguete + título + descrição, iguais em todo passo), um card arredondado com o
 * conteúdo do passo atual, e um único botão de largura total embaixo — sem "Voltar" visível, o
 * retrocesso é pelo botão físico/gesto do sistema. Nenhum passo é obrigatório — diferente do
 * Rokku, o Oribu não tem hoje nenhuma permissão que bloqueie o uso do app, então o botão está
 * sempre habilitado.
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

    BackHandler(enabled = currentStep > 0) { currentStep-- }

    Scaffold(
        bottomBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                HorizontalDivider()
                Button(
                    onClick = { if (isLast) finish() else currentStep++ },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Text(if (isLast) "Começar" else "Próxima")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Icon(
                Icons.Outlined.RocketLaunch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("Bem Vindo(a)!", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Vamos definir algumas coisas primeiro. Você sempre pode fazer alterações nas " +
                    "configurações depois também.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(20.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(20.dp),
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(220)) { w -> if (forward) w else -w } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(220)) { w -> if (forward) -w else w } + fadeOut(tween(220)))
                    },
                    label = "onboardingStep",
                ) { step ->
                    Column(Modifier.fillMaxWidth()) {
                        steps[step]()
                    }
                }
            }
        }
    }
}
