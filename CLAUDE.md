# we-sync app — diretrizes do Claude Code

App Android (cliente) da gestão financeira pessoal we-sync. Consome a API REST do repositório irmão
`../we-sync-api` (Go). Este repositório é **somente o app**: não crie código de backend aqui, e não crie
código de UI no repositório da API.

Atue como engenheiro sênior: simplicidade, correção, segurança e consistência com o código existente
valem mais do que demonstrar padrões. Qualidade **não** se mede em número de camadas ou abstrações.

## Estado atual

Projeto recém-gerado pelo template "Basic Views Activity" do Android Studio. `MainActivity`,
`FirstFragment`, `SecondFragment`, seus layouts e os testes `Example*` são placeholders do template —
podem ser substituídos quando a primeira feature real for implementada.

Ainda **não** há: cliente HTTP, serialização JSON, ViewModel/coroutines, injeção de dependência,
nem armazenamento de tokens. Essas escolhas **ainda não foram feitas**: proponha e
confirme antes de adicionar (ver "Dependência nova").

## Stack (evidência: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/`)

- Kotlin, Gradle Kotlin DSL com version catalog (`gradle/libs.versions.toml`), Gradle 9.6.0 (wrapper), AGP 9.4.1
- Módulo único `:app`, namespace/applicationId `com.github.jonasjesusamerico.we_sync_app`
- `minSdk 29`, `compileSdk`/`targetSdk 37`, `sourceCompatibility`/`targetCompatibility` Java 11, toolchain do daemon JVM 25
- UI: **Views XML + ViewBinding** (não Compose), Navigation Component (`res/navigation/nav_graph.xml`), Material Components, AppCompat
- Release: R8 habilitado via `optimization { enable = true }`; regras em `app/src/main/keepRules/rules.keep`
- Testes: JUnit 4 (`app/src/test`), AndroidX Test + Espresso (`app/src/androidTest`)
- Estilo: `kotlin.code.style=official`

Versões das bibliotecas AndroidX/Material no catálogo vieram defasadas do template; atualizar é tarefa
própria — não faça de passagem.

## Mapa do código

```
settings.gradle.kts, build.gradle.kts   projeto raiz (repositórios, plugins)
gradle/libs.versions.toml               ÚNICA fonte de versões e coordenadas de dependências
app/build.gradle.kts                    configuração Android do módulo
app/src/main/java/com/github/jonasjesusamerico/we_sync_app/   código Kotlin
app/src/main/res/                       layouts, navigation, strings, temas, dimens
app/src/main/AndroidManifest.xml
app/src/test/                           testes unitários (JVM)
app/src/androidTest/                    testes instrumentados (dispositivo/emulador)
docs/specs/NNN-nome/                    specs de feature (spec → plan → tasks), mesmo fluxo de ../we-sync-api/docs/specs/README.md
```

Ao criar a primeira feature, organize por feature (ex.: `auth/`, `purchase/`, `category/`,
`paymentmethod/`), espelhando os domínios da API, e registre a estrutura escolhida aqui.

## Contrato com a API (fonte da verdade: `../we-sync-api`)

Consulte o código/Swagger da API (`../we-sync-api/docs/swagger/`, `internal/<domínio>/model.go`,
`internal/router/router.go`) em vez de supor o contrato. Pontos já estabelecidos:

- Base: `/api/v1`; health em `/health-check`. Em dev local, nginx expõe a API em `:8080`
  (no emulador, o host é `10.0.2.2`).
- Auth: `POST /auth/register|login|refresh|logout`; resposta de login traz `access_token` (JWT),
  `refresh_token` (opaco) e `token_type`. Demais rotas exigem `Authorization: Bearer <access_token>`.
- Recursos autenticados: `/purchases` (CRUD), `/categories`, `/payment-methods`.
- JSON em `snake_case`. Erros sempre no envelope `{"code": "...", "message": "..."}`
  (`VALIDATION_ERROR`→422, `UNAUTHORIZED`→401, `NOT_FOUND`→404, `CONFLICT`→409 etc.).
- Dinheiro (`amount`) chega como **string decimal** (ex.: `"12.34"`): use `BigDecimal`, **nunca**
  `Double`/`Float`. Datas sem hora (`purchase_date`) são `"YYYY-MM-DD"`: use `java.time.LocalDate`.

Se o app precisar de algo que a API não oferece, aponte a lacuna — não contorne no cliente nem altere
a API sem pedido.

## Comandos

```bash
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # unitários (JVM), não exigem dispositivo
./gradlew lintDebug              # Android Lint (única análise estática configurada)
./gradlew connectedDebugAndroidTest   # instrumentados: exigem emulador/dispositivo conectado
```

Não existe CI — não crie sem pedido. `local.properties` é gerado pelo Android Studio e não é versionado.

## Fluxo de trabalho

1. Entenda o requisito. Ambiguidade que muda fluxo de tela, regra de negócio ou segurança → pergunte;
   detalhe menor com convenção clara → siga a convenção e mencione.
2. Leia o código e os testes do trecho, e o contrato correspondente na API.
3. Escolha a solução de menor complexidade que atende ao requisito atual.
4. Implemente e teste (lógica fora da UI com testes unitários: sucesso, falha, limites).
5. Verifique com os comandos acima (escopo menor no ciclo, build + unitários + lint antes de concluir).
6. Revise o diff: regressão, token/dado sensível exposto, erro engolido, abstração desnecessária, escopo extra.
7. Atualize este `CLAUDE.md` se a mudança o afetar (ver "Manutenção deste arquivo").
8. Resuma: o que mudou, comandos executados e resultado real, limitações e pendências.

Tarefa grande (nova feature/tela com fluxo, arquitetura base, auth, nova dependência): plano curto e
confirmação antes de implementar. Tarefa pequena: execute direto.

## Regras inegociáveis

- **Escopo:** só o necessário; refatorações e "melhorias de passagem" vão para o resumo como sugestão.
- **Dependência nova:** justifique e avise antes; declare sempre via `libs.versions.toml`.
- **Tokens e dados do usuário:** refresh/access token nunca em `SharedPreferences` em texto puro, logs,
  `Toast`/`Snackbar`, analytics ou backups. Nada de `usesCleartextTraffic` global — se HTTP for
  necessário em dev, restrinja por `network_security_config` ao host local e só no build debug.
- **Segredos:** nunca em código, recursos, testes ou docs; config por build type/`local.properties`.
- **Main thread:** nada de rede ou disco na thread principal.
- **Honestidade:** nunca afirme que build/teste/lint passou sem executar e ler o resultado; diga o que
  não foi executado (ex.: testes instrumentados sem emulador).

## Manutenção deste arquivo

Este `CLAUDE.md` é a referência dos próximos agentes: mantê-lo correto faz parte da tarefa, não é extra.
Atualize-o **no mesmo diff** da mudança que o tornar desatualizado, por exemplo:

- dependência ou ferramenta adicionada/removida/trocada (Stack);
- decisão de arquitetura tomada (estrutura de pacotes, HTTP, DI, armazenamento de tokens) — mova o item
  de "Estado atual / ainda não há" para a seção correspondente;
- pastas, módulos ou convenções novas (Mapa do código);
- comando novo ou alterado, ou CI criada (Comandos);
- mudança no contrato da API que o app passe a usar (Contrato com a API).

Regras: registre só o que tem evidência no código (cite o arquivo quando útil); remova o que deixou de
ser verdade em vez de acumular; mantenha o arquivo enxuto — detalhe longo vai para `docs/`. Mudanças
nas regras inegociáveis ou de segurança exigem pedido explícito do usuário. Mencione no resumo o que
foi alterado aqui.

## Conflitos entre instruções

Prioridade: (1) pedido explícito do usuário, (2) segurança e dados do usuário, (3) consistência com o
código existente, (4) preferências destas regras. Pedido que conflite com segurança → aponte o risco e pergunte.

## Idioma

- **Tudo o que é versionado em inglês**: identificadores, comentários, logs, nomes de recursos
  (`R.string.login_button`) e docs.
- **Textos exibidos ao usuário** (valores em `res/values/strings.xml`) em **pt-BR**; nunca string
  literal de UI no código ou no layout — sempre `@string/...`.
- Chat com o usuário: português.
