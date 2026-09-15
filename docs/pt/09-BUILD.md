# FlowFinance — Build & Release

## 1. Estrutura do projeto (Gradle)

```
FlowFinance/
├── build.gradle.kts          # raiz: plugins AGP, Kotlin, compose, KSP, Hilt (apply false)
├── settings.gradle.kts       # repositórios (google/mavenCentral), inclui :app
├── gradle.properties         # jvmargs -Xmx2048m, useAndroidX, nonTransitiveRClass
├── gradle/libs.versions.toml # version catalog (todas as versões em um lugar)
├── gradlew / gradlew.bat     # wrapper (Gradle 9.5.1)
├── app/
│   ├── build.gradle.kts      # configuração do módulo + dependências
│   ├── proguard-rules.pro
│   └── src/
└── docs/
```

## 2. Requisitos de ambiente

- **JDK 17** (exigido pelo AGP atual) — confirme com `java -version`.
- **Android Studio** (Giraffe 2023.3.1 ou mais novo, segundo o README; é recomendada a última estável) ou CLI + Android SDK `platforms;android-35`, build-tools, JDK 17.
- Componentes do SDK Manager: `platforms;android-35`, `build-tools` do AGP 8.13, `platform-tools`.
- `local.properties` não é commitado; o Android Studio o cria (ou defina `sdk.dir` manualmente).

## 3. Destaques do version catalog (`gradle/libs.versions.toml`)

| Componente | Versão |
|-----------|--------|
| AGP (`com.android.application`) | 8.13.2 |
| Kotlin (`org.jetbrains.kotlin.android`) | 2.0.21 |
| Plugin do compilador Compose (`org.jetbrains.kotlin.plugin.compose`) | 2.0.21 |
| KSP (`com.google.devtools.ksp`) | 2.0.21-1.0.27 |
| Hilt (`com.google.dagger.hilt.android`) | 2.51.1 |
| compose-bom | 2024.09.00 |

Bumps de versão precisam ser coordenados com a compatibilidade KSP↔Kotlin (versões de KSP são atadas à versão do Kotlin).

## 4. Configuração do módulo (`app/build.gradle.kts`)

- `namespace = applicationId = "com.flowfinance.app"`.
- `compileSdk = 35`, `minSdk = 24`, `targetSdk = 35`, `versionCode = 5`, `versionName = "2.0.1"`.
- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`.
- Build types:
  - **debug** — padrão.
  - **release** — `isMinifyEnabled = true`, `isShrinkResources = true`, proguard padrão + `proguard-rules.pro`.
- Toolchain Java/Kotlin: source/target Java 11, `jvmTarget = 11`, **core library desugaring** habilitado (`desugar_jdk_libs 2.0.4`) — necessário para `java.time` no API < 26.
- `buildFeatures { compose = true; buildConfig = true }`.
- `vectorDrawables.useSupportLibrary = true`.

## 5. Assinatura

- A configuração commitada **não tem signingConfig** de release → builds release são assinados com a chave de debug, a menos que você configure `signingConfigs` localmente (não comite segredos). O APK publicado em `app/release/` foi assinado durante a criação.

## 6. Comandos de build

```bash
# APK debug
./gradlew assembleDebug
# APK release (não assinado por padrão / com seu keystore configurado)
./gradlew assembleRelease
# Instalar em dispositivo/emulador
./gradlew installDebug
# Testes
./gradlew testDebugUnitTest          # testes unitários (JUnit)
./gradlew connectedDebugAndroidTest  # testes instrumentados (precisa de dispositivo/emulador)
# Limpar
./gradlew clean
```

Saídas:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 7. Testes

Estado atual (apenas placeholders):
- `app/src/test/java/com/flowfinance/app/ExampleUnitTest.kt` — teste unitário de exemplo.
- `app/src/androidTest/java/com/flowfinance/app/ExampleInstrumentedTest.kt` — teste instrumentado de exemplo.

**Não há suíte de testes real** cobrindo repositórios, ViewModels ou fluxos — bom alvo para trabalho futuro (ex.: testes in-memory de Room para DAOs, `MainDispatcherRule` para ViewModels, testes de UI Compose para o `AddTransactionSheet`).

## 8. ProGuard / R8 (`app/proguard-rules.pro`)

- Mantém campos de entidades Room e modelos Gson (ver 07-SEGURANCA).
- Gson: `Signature, *Annotation*`, `TypeToken` e subtipos.
- Release usa `getDefaultProguardFile("proguard-android-optimize.txt")` + regras do projeto.

## 9. Fluxo de release (conforme observado no repositório)

1. `./gradlew assembleRelease` (R8 minify+shrink).
2. APK copiado para `app/release/FlowFinance-v2.0.1.apk` junto com baseline profiles (`app/release/baselineProfiles/`) e `output-metadata.json`.
3. Publicar no GitHub Releases em `Gabrick75/FlowFinance/releases`.

## 10. Sem CI/CD

Não há workflow de GitHub Actions, pipeline scriptado ou imagem Docker. O release é um processo local (ou manual). Se CI for desejado, um job mínimo de GitHub Actions seria: `actions/setup-java@v4` (JDK 17) → `gradlew assembleRelease` → upload do artefato APK.

## 11. Checklist de bump de versão (vNext)

- `app/build.gradle.kts`: `versionCode`/`versionName`.
- `values/strings.xml`: `app_version_name`.
- `BackupMetadata.dbVersion` (se o schema mudar) — manter em sincronia com `AppDatabase.version`.
- Ao mudar o schema: adicionar uma `Migration` explícita em `DatabaseModule` e registrá-la em `provideAppDatabase()` (sem fallback destrutivo).