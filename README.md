<p align="center">
  <img width="200" alt="Logo do app Dia a Dia" src="app/src/main/res/drawable/logo.png" />
</p>

# Dia a Dia

Dia a Dia é um aplicativo Android nativo desenvolvido em Kotlin para registrar reflexões pessoais, acompanhar a rotina e manter um pequeno diário com autenticação e sincronização em nuvem.

O projeto utiliza Firebase Authentication para login com e-mail/senha e Google, Firebase Realtime Database para persistência remota e armazenamento local em SharedPreferences para manter sessão, perfil e reflexões mesmo quando a sincronização falha.

## Sobre o Projeto

O aplicativo foi pensado como um diário pessoal simples, com fluxo direto e interface focada em escrita. Após a autenticação, o usuário acessa uma tela inicial com saudação personalizada, data atual e histórico de reflexões registradas. Também é possível editar o perfil e criar novas entradas de reflexão com fallback local quando a operação online não estiver disponível.

## Funcionalidades Implementadas

:heavy_check_mark: Cadastro com e-mail e senha\
:heavy_check_mark: Login com e-mail e senha\
:heavy_check_mark: Login e cadastro com Google\
:heavy_check_mark: Recuperação de senha por e-mail\
:heavy_check_mark: Persistência de perfil no Firebase Realtime Database\
:heavy_check_mark: Criação e listagem de reflexões pessoais\
:heavy_check_mark: Cache local de sessão, perfil e reflexões\
:heavy_check_mark: Leitura offline dos dados salvos no dispositivo\
:heavy_check_mark: Edição de perfil\
:heavy_check_mark: Interface Android nativa com Material Design e ViewBinding\

## Telas Principais

- Login: autenticação com e-mail/senha, acesso com Google e recuperação de senha.
- Cadastro: criação de conta com nome, e-mail e senha, além de cadastro com Google.
- Home: saudação personalizada, data atual e histórico das reflexões registradas.
- Criar Reflexão: formulário para registrar uma nova reflexão com data e hora.
- Editar Perfil: atualização dos dados básicos do usuário.

## Tecnologias

As principais tecnologias e bibliotecas utilizadas no projeto são:

- Kotlin
- Android SDK
- Gradle Kotlin DSL
- Firebase Authentication
- Firebase Realtime Database
- Google Sign-In
- Material Components
- SharedPreferences
- Glide
- CircleImageView

## Requisitos

Antes de executar o projeto, tenha instalado:

- Android Studio atualizado
- JDK 11
- SDK Android com API 36
- Um emulador Android ou dispositivo físico com Android 11 ou superior
- Uma conta Firebase configurada para o projeto

## Configuração do Firebase

Para que autenticação e banco funcionem corretamente, configure o Firebase antes de executar:

1. Crie um projeto no Firebase Console.
2. Cadastre um app Android com o pacote dev.victorbreno.diaadia.
3. Baixe o arquivo google-services.json e coloque em app/google-services.json.
4. Ative o provedor Authentication com E-mail/Senha.
5. Ative também o provedor Google em Authentication.
6. Adicione o SHA-1 e, de preferência, o SHA-256 do app no console do Firebase para liberar o Google Sign-In.
7. Crie a estrutura do Realtime Database e permita leitura e escrita para os usuários durante o desenvolvimento, conforme sua regra de segurança.

Se o login com Google exibir erro de configuração ausente, normalmente isso indica que o arquivo google-services.json está desatualizado ou que o SHA-1 ainda não foi cadastrado no Firebase.

## Como Executar

### Android Studio

1. Clone o repositório.
2. Abra a pasta do projeto no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Confirme que o arquivo google-services.json está presente em app/.
5. Execute o app em um emulador ou dispositivo físico.

### Linha de comando

```bash
git clone <url-do-repositorio>
cd dia-a-dia-app
./gradlew assembleDebug
```

Para instalar em um dispositivo ou emulador já conectado:

```bash
./gradlew installDebug
```

## Estrutura do Projeto

```text
app/
├── src/main/java/dev/victorbreno/diaadia/
│   ├── activities/   # Telas principais do app
│   ├── data/         # Modelos de dados
│   ├── services/     # Firebase, autenticação Google e armazenamento local
│   └── utils/        # Validações e utilitários
├── src/main/res/     # Layouts, cores, temas, drawables e strings
└── build.gradle.kts  # Configuração do módulo Android
```

## Fluxo de Dados

- A autenticação é feita com Firebase Authentication.
- Os dados de perfil e reflexões são salvos no Firebase Realtime Database.
- O app mantém cópias locais de sessão, perfil e reflexões com SharedPreferences.
- Quando a leitura remota falha, o aplicativo tenta recuperar os dados salvos no dispositivo.

## Comandos Úteis

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
```

## Melhorias Futuras

- Adicionar testes instrumentados para os fluxos de autenticação e reflexão.
- Evoluir o armazenamento offline para Room ou SQLite com sincronização estruturada.
- Incluir edição e exclusão de reflexões já registradas.
- Publicar capturas de tela reais do aplicativo no repositório.
