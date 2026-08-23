plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.aboutlibraries.android)
}

// Conta de commits usada para nomear builds nightly (r<N>). Fora de um repo git
// (ex.: antes do primeiro `git init`), cai para "0" em vez de quebrar o build.
fun runCommandOrDefault(command: String, default: String): String =
    runCatching {
        providers.exec { commandLine(command.split(" ")) }.standardOutput.asText.get().trim()
    }.getOrElse { default }

val commitCount by lazy { runCommandOrDefault("git rev-list --count HEAD", "0") }

val supportedAbis = setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

// Epoch millis em vez de String ISO formatada aqui — o pacote java.time não resolve nesse
// script Kotlin DSL (precompiled script classpath não expõe java.time), então a formatação
// pra exibição acontece do lado do app (AboutScreen.kt), onde java.time funciona normalmente.
val buildTimeMillis = System.currentTimeMillis()

android {
    namespace   = "app.oribu"
    compileSdk  = 37

    defaultConfig {
        applicationId = "app.oribu"
        minSdk        = 34
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"

        // Expostos pra tela Sobre (checagem/exibição de versão) — commitCount já existia só
        // pro nome da build nightly, BUILD_TIME é novo.
        buildConfigField("int", "COMMIT_COUNT", commitCount.ifBlank { "0" })
        buildConfigField("long", "BUILD_TIME", "${buildTimeMillis}L")

        ndk {
            // False positive, we have x86 abi support
            //noinspection ChromeOsAbiSupport
            abiFilters += supportedAbis
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            // False positive, we have x86 abi support
            //noinspection ChromeOsAbiSupport
            include(*supportedAbis.toTypedArray())
            isUniversalApk = true
        }
    }

    signingConfigs {
        // Keystore de baixo risco dedicada às builds nightly/qa, comitada no repo (ver
        // exceção em .gitignore) — não é uma credencial real, é só uma chave de
        // assinatura pra manter a mesma assinatura entre execuções de CI. Sem isso,
        // cada run do GitHub Actions gera um ~/.android/debug.keystore novo e aleatório
        // (a AGP cria um na hora se não existir), então cada nightly saía com uma
        // assinatura diferente da anterior e o updater in-app falhava ao instalar por
        // cima ("conflito com um pacote já existente" — certificados não batem).
        create("nightly") {
            storeFile     = file("nightly.keystore")
            storePassword = "android"
            keyAlias      = "androiddebugkey"
            keyPassword   = "android"
        }
    }

    buildTypes {
        debug {
            // Sufixo próprio para instalar lado a lado com a versão de produção sem
            // conflito de assinatura/dados — importante para APKs de teste (ex.: com
            // dados fake) em um celular que já tem o app "de verdade" instalado.
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Build de pré-lançamento gerado a cada push em master pelo CI (workflow build_push.yml).
        // Assinada com a keystore "nightly" fixa (sem depender de secrets) e instalável lado
        // a lado com a versão estável graças ao applicationIdSuffix. Precisa ser uma chave
        // ESTÁVEL entre execuções de CI — diferente da keystore "debug" (gerada on-the-fly
        // pela AGP se não existir) — pra permitir atualizar uma nightly já instalada sem
        // desinstalar antes.
        create("nightly") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            applicationIdSuffix = ".nightly"
            versionNameSuffix   = "-r$commitCount"
            signingConfig       = signingConfigs.getByName("nightly")
        }
        // Build de pré-lançamento disparada manualmente (workflow_dispatch), assinada em CI
        // com a keystore de release real via secrets — sem signingConfig aqui de propósito,
        // o Gradle produz um APK unsigned e o step "Sign APK" do build_push.yml assina depois.
        create("beta") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
        }
        // Build minificada (menor que a "debug" pura) para distribuir APKs de teste ad hoc —
        // ex.: builds com dados fake para testes de usabilidade fora do time de dev. Assinada
        // com a keystore "nightly" fixa (mesmo motivo do build type nightly acima — permite
        // atualizar uma build "qa" já instalada) e isDebuggable=true para poder rodar código
        // guardado por BuildConfig.DEBUG (como o DebugSeeder).
        create("qa") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            applicationIdSuffix = ".qa"
            versionNameSuffix   = "-qa"
            signingConfig       = signingConfigs.getByName("nightly")
            isDebuggable        = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    androidResources {
        // Prevent aapt from re-compressing already-gzipped assets (stored as .bin)
        noCompress += "bin"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.activity)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // WorkManager
    implementation(libs.workmanager)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Images
    implementation(libs.coil)

    // DataStore
    implementation(libs.datastore)

    // Splash screen
    implementation(libs.core.splashscreen)

    // Charts
    implementation(libs.vico.compose)

    // Open source licenses list (Sobre > Licenças)
    implementation(libs.aboutlibraries)

    // Shimmer
    implementation(libs.shimmer)

    // ML Kit Translation
    implementation(libs.mlkit.translate)

    // JSON
    implementation(libs.gson)

    // Coroutines
    implementation(libs.coroutines.android)

    // AppCompat (required for theme bridge with Compose)
    implementation(libs.appcompat)

    // Testes unitários (JVM, sem dependência de Android/emulador)
    testImplementation(libs.junit)
}
