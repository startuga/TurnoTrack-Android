# TurnoTrack — Registo de Trabalho para Android

App Android nativa (Kotlin + Jetpack Compose) da web app
[Registo de Trabalho AI](https://github.com/startuga/https-github.com-startuga-TurnoTrack):
registo de turnos, horas extra, atrasos, feriados, folgas, faltas e férias, com estatísticas
e um assistente IA (Gemini).

## Instalar no telemóvel

1. Abre **Releases → latest** neste repositório (no telemóvel, com sessão iniciada no GitHub)
   e descarrega `TurnoTrack.apk`.
   Link fixo: `https://github.com/startuga/TurnoTrack-Android/releases/latest/download/TurnoTrack.apk`
2. Abre o ficheiro. Se o Android pedir, autoriza o navegador/gestor de ficheiros a
   **instalar apps desconhecidas**.
3. Para atualizar, repete: o APK novo instala por cima e os registos mantêm-se.

## Passar os dados da web app

1. Na web app: ⚙️ **Gestão de Dados → Descarregar Ficheiro (.json)**.
2. Na app Android: **Definições → Importar / restaurar backup** e escolhe esse ficheiro.

O formato do backup é o mesmo nas duas versões, nos dois sentidos.

## Assistente IA

Em **Definições → Assistente IA**, cola a tua chave Gemini API
(obtém uma em <https://aistudio.google.com/apikey>). A chave fica guardada só no telemóvel:
não está no código, não vai no APK e fica fora dos backups. O modelo predefinido é
`gemini-3.8-flash` (o mesmo da web app) e pode ser alterado no mesmo ecrã.

## O que mudou face à web app

| Web app | Android |
|---|---|
| `localStorage` | Base de dados Room (SQLite) + backup automático do Android |
| Chave Gemini embutida no bundle | Chave introduzida na app, guardada só no telemóvel |
| Tailwind / fontes / ícone via CDN | Tudo incluído na app, funciona offline |
| Recharts | Gráfico desenhado em Canvas (sem bibliotecas externas) |
| Download/upload de JSON | Seletor de ficheiros do Android (mesmo formato JSON) |
| Temas claro / escuro / OLED | Iguais, mais "Automático" (segue o sistema) |

Extras: deslizar entre meses no calendário, botão "Registar hoje", "Igual ao horário",
anular ao apagar, toque nas barras do gráfico para ver detalhes, e o assistente mantém
o contexto da conversa.

## Desenvolvimento

- Abrir a pasta no Android Studio (Ladybug ou mais recente) e correr a configuração `app`.
- Testes: `./gradlew testDebugUnitTest` — os testes de `WorkCalculationsTest` usam valores
  obtidos a correr o `utils.ts` original, para garantir que os cálculos são idênticos.
- Cada push para `main` corre os testes e gera o APK no GitHub Actions
  (`.github/workflows/android.yml`).

Estrutura:

```
app/src/main/java/com/startuga/turnotrack/
├── domain/      Modelos e cálculos (port de types.ts e utils.ts)
├── data/        Room, DataStore, backup JSON
├── ai/          Cliente Gemini (REST) e assistente com function calling
└── ui/          Ecrãs Compose: calendário, registo, estatísticas, assistente, definições
```

## Assinatura

O APK é assinado com a chave em `signing/` para que cada versão nova instale por cima
da anterior. **Mantém este repositório privado.** Se perderes esta chave, a próxima versão
terá de ser instalada de raiz (exporta um backup antes). Para a tirar do repositório,
move-a para GitHub Secrets e ajusta `signingConfigs` em `app/build.gradle.kts`.
