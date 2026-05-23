# NoTUB - Documento de Especificação do Sistema

> **Sistema de Gestão de Informação de Transporte Público (PIM)**
> Versão 2.0 - Especificação Full Stack

---

## Índice

1. [Arquitetura do Sistema](#1-arquitetura-do-sistema)
2. [Modelo de Dados](#2-modelo-de-dados)
3. [Modelo de Segurança](#3-modelo-de-segurança)
4. [Referência da API](#4-referência-da-api)
5. [Microsserviço de Email](#5-microsserviço-de-email)
6. [Arquitetura do Frontend](#6-arquitetura-do-frontend)
7. [Estrutura de Pastas](#7-estrutura-de-pastas)
8. [Docker e Infraestrutura](#8-docker-e-infraestrutura)
9. [Exemplos de Código Chave](#9-exemplos-de-código-chave)
10. [Escalabilidade e Melhorias](#10-escalabilidade-e-melhorias)

---

## 1. Arquitetura do Sistema

```
                          ┌──────────────────────┐
                          │    Browser / Mobile   │
                          │       (PWA)           │
                          └──────────┬────────────┘
                                     │ HTTPS
                                     ▼
                          ┌──────────────────────┐
                          │       Nginx          │
                          │   Reverse Proxy      │
                          │   Terminação SSL     │
                          │  Balanceador Carga   │
                          │     :443 / :80       │
                          └───┬──────────────┬───┘
                              │              │
                    /         │              │  /api/
                    /*.html   │              │  /oauth2/
                    /assets   │              │  /login/
                              ▼              ▼
                   ┌──────────────┐  ┌──────────────────┐
                   │   Frontend   │  │    Backend API    │
                   │  Quasar PWA  │  │   Spring Boot     │
                   │    Vue 3     │  │     :8080         │
                   │     :80      │  │                    │
                   └──────────────┘  └──────┬─────────────┘
                                            │
                              ┌──────────────┼──────────────┐
                              │              │              │
                              ▼              │              ▼
                     ┌──────────────┐        │     ┌──────────────────┐
                     │  PostgreSQL  │        │     │    RabbitMQ      │
                     │   :5432      │        │     │  :5672 / :15672  │
                     └──────────────┘        │     └────────┬─────────┘
                                             │              │
                                             │              │ consome
                                             │              ▼
                                             │     ┌──────────────────┐
                                             │     │ Serviço de Email │
                                             │     │  Spring Boot     │
                                             │     │  Mailtrap SMTP   │
                                             │     │    :8081         │
                                             │     └──────────────────┘
                                             │
                                             └──── ambos os serviços ligam ao PostgreSQL
```

### Inventário de Serviços

| Serviço | Tecnologia | Porta | Propósito |
|---------|-----------|-------|-----------|
| nginx | Nginx stable | 443, 80 | Reverse proxy, SSL, balanceamento |
| frontend | Vue 3 + Quasar (PWA) | 80 (interno) | SPA servido pelo nginx |
| backend | Spring Boot 4 (Java 25) | 8080 (interno) | Servidor API principal |
| servico-email | Spring Boot 4 (Java 25) | 8081 (interno) | Envio de emails via RabbitMQ |
| postgres | PostgreSQL 18.3 | 5432 (interno) | Base de dados principal |
| rabbitmq | RabbitMQ 4 Management | 5672, 15672 (interno) | Broker de mensagens |

### Fluxo de Pedidos

```
Browser ──HTTPS──▶ Nginx ──HTTP──▶ Frontend (ficheiros estáticos SPA)
                         ──HTTP──▶ Backend (pedidos API)
                         ──HTTP──▶ Backend ──AMQP──▶ RabbitMQ ──AMQP──▶ Serviço Email
                                                              ──SMTP──▶ Mailtrap
```

---

## 2. Modelo de Dados

### 2.1 Enumerações

```java
public enum TipoUtilizador {
    CRIANCA,
    ESTUDANTE,
    ADULTO,
    SENIOR
}

public enum TipoPapel {
    ADMINISTRADOR,
    UTILIZADOR
}

public enum Direcao {
    IDA,
    VOLTA
}

public enum EstadoViagem {
    ATIVA,
    CONCLUIDA
}

public enum EstadoPagamento {
    EM_CURSO,
    CANCELADO,
    CONCLUIDO,
    REJEITADO
}

public enum ModalidadePasse {
    MENSAL,
    ANUAL,
    H24,
    H48,
    H72,
    SEMANAL
}
```

### 2.2 Entidades

#### Utilizador

```
┌─────────────────────────────────────────┐
│              Utilizador                  │
├─────────────────────────────────────────┤
│ id              : Long (PK, auto)       │
│ primeiroNome    : String (obrigatório)  │
│ ultimoNome      : String (obrigatório)  │
│ email           : String (único, obr.)  │
│ password        : String (hash, obr.)   │
│ nif             : String (único)        │
│ dataNascimento  : LocalDate             │
│ nrPontos        : int (padrão 0)        │
│ tipoUtilizador  : TipoUtilizador        │
├─────────────────────────────────────────┤
│ papeis          : List<Papel>    (N:N)  │
│ bilhetes        : List<Bilhete>  (1:N)  │
│ passe           : Passe          (1:1)  │
│ transacoes      : List<Transacao>(1:N)  │
│ viagens         : List<ViagemUtil>(1:N) │
│ historicoPontos : List<HistoricoP> (1:N)│
└─────────────────────────────────────────┘
```

#### Papel (autorização)

```
┌─────────────────────────┐
│         Papel            │
├─────────────────────────┤
│ id    : Long (PK, auto) │
│ nome  : TipoPapel        │
└─────────────────────────┘

// Tabela de junção: utilizador_papeis
// utilizador_id : Long (FK)
// papel_id      : Long (FK)
```

#### TituloTransporte (abstrato)

```
┌──────────────────────────────────────────┐
│       TituloTransporte (abstrato)         │
├──────────────────────────────────────────┤
│ id    : Long (PK, auto)                  │
├──────────────────────────────────────────┤
│ zonas : List<Zona>             (N:N)     │
│ viagens: List<ViagemUtilizador>(1:N)     │
└─────────────┬────────────────────────────┘
              │ herança JOINED
    ┌─────────┴─────────┐
    ▼                   ▼
┌──────────────┐  ┌──────────────┐
│   Bilhete    │  │    Passe     │
├──────────────┤  ├──────────────┤
│ usado: bool  │  │ inicio: LDT  │
│              │  │ fim:    LDT  │
│ utilizador:  │  │ modalidade:  │
│  Utilizador  │  │  Modalidade  │
│   (N:1)      │  │ utilizador:  │
└──────────────┘  │  Utilizador  │
                  │   (1:1)      │
                  └──────────────┘
```

#### Zona (círculos concêntricos)

```
┌──────────────────────────────────────┐
│               Zona                    │
├──────────────────────────────────────┤
│ id   : Long (PK, auto)              │
│ num  : int (nº da zona, ex: 1, 2)   │
│ nome : String (ex: "Zona 1")        │
├──────────────────────────────────────┤
│ paragens : List<Paragem> (1:N)      │
└──────────────────────────────────────┘

// Modelo de zonas: círculos concêntricos
// Zona 1 = centro da cidade (interior)
// Zona 2 = Zona 1 + área envolvente
// Zona 3 = Zona 2 + anel exterior (se necessário)
// Atravessar mais zonas = tarifa mais alta
```

#### Paragem

```
┌──────────────────────────────────────┐
│              Paragem                  │
├──────────────────────────────────────┤
│ id         : Long (PK, auto)        │
│ nome       : String (obrigatório)   │
│ localizacao: Point (Embutido)       │
├──────────────────────────────────────┤
│ zona       : Zona (FK via zona_id)  │
└──────────────────────────────────────┘

┌─────────────────────────────┐
│  Point (Embutido)           │
├─────────────────────────────┤
│ latitude  : double          │
│ longitude : double          │
└─────────────────────────────┘
```

#### Linha

```
┌──────────────────────────────────────────┐
│               Linha                      │
├──────────────────────────────────────────┤
│ id                   : Long (PK, auto)   │
│ nome                 : String (obrigat.) │
│ identificadorServico : String            │
├──────────────────────────────────────────┤
│ trajetos : List<Trajeto> (1:N)          │
└──────────────────────────────────────────┘
```

#### Trajeto

```
┌──────────────────────────────────────────┐
│               Trajeto                    │
├──────────────────────────────────────────┤
│ id      : Long (PK, auto)               │
│ direcao : Direcao (IDA / VOLTA)          │
├──────────────────────────────────────────┤
│ linha              : Linha        (N:1)  │
│ pontosDePassagem   : List<PdP>    (1:N)  │
│ viagensVeiculo     : List<VV>     (1:N)  │
└──────────────────────────────────────────┘
```

#### PontosDePassagem

```
┌─────────────────────────────────────────────┐
│          PontosDePassagem                    │
├─────────────────────────────────────────────┤
│ id                : Long (PK, auto)         │
│ ordem             : int (ordem da paragem)  │
│ horaChegada       : LocalDateTime           │
│ tempoDesdeInicio  : Duration                │
├─────────────────────────────────────────────┤
│ trajeto  : Trajeto  (N:1)                   │
│ paragem  : Paragem  (N:1)                   │
└─────────────────────────────────────────────┘
```

#### Veiculo (abstrato)

```
┌──────────────────────────────────────────┐
│       Veiculo (abstrato)                  │
├──────────────────────────────────────────┤
│ id              : Long (PK, auto)        │
│ matricula       : String (única)         │
│ nLugares        : int (capacidade total) │
│ lotacaoAtual    : int (ocupação atual)   │
│ localizacaoAtual: Point (Embutido)       │
├──────────────────────────────────────────┤
│ viagens : List<ViagemVeiculo> (1:N)     │
└─────────────┬────────────────────────────┘
              │ herança JOINED
              ▼
       ┌──────────────┐
       │  Autocarro   │
       └──────────────┘
```

#### ViagemVeiculo

```
┌──────────────────────────────────────────┐
│          ViagemVeiculo                    │
├──────────────────────────────────────────┤
│ id     : Long (PK, auto)                │
│ tripId : String (ref. externa viagem)   │
├──────────────────────────────────────────┤
│ veiculo           : Veiculo       (N:1) │
│ trajeto           : Trajeto       (N:1) │
│ viagensUtilizador : List<ViagemU>  (1:N)│
└──────────────────────────────────────────┘
```

#### ViagemUtilizador

```
┌──────────────────────────────────────────────┐
│          ViagemUtilizador                     │
├──────────────────────────────────────────────┤
│ id    : Long (PK, auto)                     │
│ inicio: LocalDateTime                       │
│ fim   : LocalDateTime (null até terminar)   │
│ estado: EstadoViagem (ATIVA / CONCLUIDA)    │
├──────────────────────────────────────────────┤
│ titulo         : TituloTransporte (N:1)     │
│ viagemVeiculo  : ViagemVeiculo     (N:1)    │
│ paragemEntrada : Paragem           (N:1)    │
│ paragemSaida   : Paragem           (N:1)    │
│ coima          : Coima             (1:1)    │
└──────────────────────────────────────────────┘
```

#### Tarifa (baseada em zonas)

```
┌──────────────────────────────────────────────────┐
│               Tarifa                              │
├──────────────────────────────────────────────────┤
│ id              : Long (PK, auto)                │
│ valor           : float (preço em EUR)           │
│ tipoUtilizador  : TipoUtilizador                 │
│ modalidade      : ModalidadePasse                │
│ nrZonas         : int (nº de zonas, mínimo 1)    │
├──────────────────────────────────────────────────┤
// Restrição única: (tipoUtilizador, modalidade, nrZonas)
// Exemplo de dados:
//   CRIANCA,  MENSAL, 1 → 15.00
//   CRIANCA,  MENSAL, 2 → 22.50
//   ADULTO,   MENSAL, 1 → 30.00
//   ADULTO,   MENSAL, 2 → 45.00
//   ESTUDANTE,MENSAL, 1 → 20.00
//   ESTUDANTE,MENSAL, 2 → 30.00
//   null,     H24,    1 → 4.50   (bilhete: tipoUtilizador=null)
//   null,     H24,    2 → 6.75
//   Quando tipoUtilizador é null  → preço de bilhete
//   Quando tipoUtilizador é definido → preço de passe
└──────────────────────────────────────────────────┘
```

#### Transacao

```
┌──────────────────────────────────────────┐
│            Transacao                      │
├──────────────────────────────────────────┤
│ id                : Long (PK, auto)      │
│ dataHora          : LocalDateTime        │
│ referenciaExterna : String               │
│ estadoPagamento   : EstadoPagamento      │
├──────────────────────────────────────────┤
│ titulo     : TituloTransporte (N:1)      │
│ utilizador : Utilizador       (N:1)      │
└──────────────────────────────────────────┘
```

#### Coima

```
┌──────────────────────────────┐
│           Coima               │
├──────────────────────────────┤
│ id    : Long (PK, auto)     │
│ valor : float (montante)    │
│ motivo: String              │
└──────────────────────────────┘
```

#### TokenRecuperacaoSenha

```
┌──────────────────────────────────────────────┐
│          TokenRecuperacaoSenha                │
├──────────────────────────────────────────────┤
│ id          : Long (PK, auto)               │
│ token       : String (UUID, único)          │
│ dataExpiracao: LocalDateTime (30 min)       │
│ utilizado   : boolean (padrão false)        │
├──────────────────────────────────────────────┤
│ utilizador : Utilizador (N:1)               │
└──────────────────────────────────────────────┘
```

#### HistoricoPontos

```
┌──────────────────────────────────────────┐
│          HistoricoPontos                  │
├──────────────────────────────────────────┤
│ id         : Long (PK, auto)            │
│ pontos     : int (positivo=ganho,       │
│                    negativo=utilização)  │
│ tipo       : String (VIAGEM, COMPRA,    │
│               RECOMPENSA, RESGATE)       │
│ descricao  : String                     │
│ dataHora   : LocalDateTime              │
├──────────────────────────────────────────┤
│ utilizador : Utilizador (N:1)           │
└──────────────────────────────────────────┘
```

### 2.3 Diagrama de Relações entre Entidades (textual)

```
┌──────────┐    N:N    ┌───────────┐
│  Papel   │◄─────────►│Utilizador │
└──────────┘           └─────┬─────┘
                             │ 1
          ┌──────────────────┼────────────────┬───────────────┐
          │ 1                │ 1              │ N             │ 1
          ▼                  ▼                ▼               ▼
   ┌────────────┐    ┌──────────┐   ┌───────────────┐ ┌────────────┐
   │   Passe    │    │ Bilhete  │   │HistoricoPontos│ │ Transacao  │
   └─────┬──────┘    └────┬─────┘   └───────────────┘ └──────┬─────┘
         │                │                                  │
         └────────┬───────┘                                  │
                  │ herda                                     │
                  ▼                                           │
         ┌────────────────┐                                  │
         │TituloTransporte│◄─────────────────────────────────┘
         │   (abstrato)   │
         └───────┬────────┘
                 │ N:N
                 ▼
          ┌──────────┐ 1:N ┌──────────┐
          │   Zona   │◄────│ Paragem  │
          └──────────┘     └────┬─────┘
                                │
                 ┌──────────────┘
                 │ N:1
                 ▼
        ┌─────────────────┐ 1:N ┌──────────────────┐
        │ PontosDePassagem│────►│    Trajeto        │
        └─────────────────┘     └───────┬───────────┘
                                        │ N:1
                                        ▼
                                ┌──────────────┐
                                │    Linha     │
                                └──────────────┘

        ┌──────────┐ 1:N ┌───────────────┐ 1:N ┌─────────────────┐
        │ Veiculo  │────►│ViagemVeiculo  │────►│ViagemUtilizador │
        │(abstrato)│     └───────┬───────┘     └────┬────────────┘
        └──────────┘             │ N:1              │ 1:1
                                ▼                   ▼
                        ┌──────────────┐    ┌────────────┐
                        │   Trajeto    │    │   Coima    │
                        └──────────────┘    └────────────┘
```

---

## 3. Modelo de Segurança

### 3.1 Fluxo de Autenticação JWT

```
1. POST /api/auth/login { email, password }
2. Backend valida credenciais via AuthenticationManager
3. Backend gera JWT (HS512, 24h de validade)
4. Resposta: { token, id, email }
5. Frontend guarda token em localStorage
6. Pedidos subsequentes: Authorization: Bearer <token>
```

### 3.2 Fluxo OAuth2 Google

```
1. Utilizador clica em "Continuar com o Google"
2. Frontend redireciona para: /oauth2/authorization/google
3. Nginx faz proxy para backend → Spring OAuth2 redireciona para Google
4. Utilizador autoriza no Google
5. Google redireciona para: /login/oauth2/code/google
6. OAuth2AuthenticationSuccessHandler do backend:
   - Cria/procura utilizador na BD
   - Gera JWT
   - Redireciona para: {FRONTEND_URL}/oauth2/redirect?token=<jwt>
7. Página OAuth2Redirect do frontend extrai o token, guarda, busca utilizador
8. Redireciona para /home
```

### 3.3 Fluxo de Recuperação de Palavra-passe

```
1. Utilizador clica em "Esqueceu a palavra-passe?" na página de Login
2. Frontend: POST /api/auth/esqueceu-password { email }
3. Backend:
   - Procura utilizador por email
   - Gera TokenRecuperacaoSenha (UUID, expira em 30 min)
   - Guarda token na BD
   - Publica evento RECUPERACAO_PASSWORD_PEDIDA no RabbitMQ
   - Retorna 200 (sempre, para prevenir enumeração de emails)
4. Serviço de Email consome o evento:
   - Envia email com link: {FRONTEND_URL}/redefinir-password?token=<uuid>
5. Utilizador clica no link do email
6. Frontend mostra a página RedefinirPasswordPage
7. Utilizador introduz nova palavra-passe
8. Frontend: POST /api/auth/redefinir-password { token, novaPassword }
9. Backend:
   - Valida token (existe, não expirou, não utilizado)
   - Atualiza palavra-passe do utilizador
   - Marca token como utilizado
   - Retorna 200
10. Frontend redireciona para /login com mensagem de sucesso
```

### 3.4 Matriz de Autorização

| Padrão de Endpoint | UTILIZADOR | ADMINISTRADOR | Público |
|-------------------|------------|---------------|---------|
| /api/auth/login | - | - | SIM |
| /api/auth/register | - | - | SIM |
| /api/auth/esqueceu-password | - | - | SIM |
| /api/auth/redefinir-password | - | - | SIM |
| /api/auth/me | SIM | SIM | - |
| /api/test/** | SIM | SIM | SIM |
| /api/network/** (GET) | SIM | SIM | SIM |
| /api/tarifas/** (GET) | SIM | SIM | SIM |
| /api/zonas/** (GET) | SIM | SIM | SIM |
| /api/tickets/** | SIM | SIM | - |
| /api/utilizadores/perfil (GET/PUT) | próprio | SIM | - |
| /api/viagens/** | SIM | SIM | - |
| /api/transacoes/minhas | SIM | SIM | - |
| /api/pontos/** | SIM | SIM | - |
| /api/validacao/** | SIM | SIM | - |
| /api/utilizadores (GET todos) | - | SIM | - |
| /api/utilizadores/{id} (GET/DELETE) | - | SIM | - |
| /api/utilizadores/{id}/papeis | - | SIM | - |
| /api/admin/** | - | SIM | - |
| /api/transacoes (GET todos) | - | SIM | - |

### 3.5 Estrutura do Token JWT

```
Header: { alg: HS512, typ: JWT }
Payload: {
  sub: "utilizador@email.com",
  iat: 1716300000,
  exp: 1716386400
}
Segredo: configurado via variável de ambiente JWT_SECRET (Base64, min 64 carateres)
```

### 3.6 Configuração de Segurança (Spring Security)

```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(csrf -> csrf.disable())
    .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth ->
        auth.requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/test/**").permitAll()
            .requestMatchers("/api/network/**").permitAll()
            .requestMatchers("/api/tarifas/**").permitAll()
            .requestMatchers("/api/zonas/**").permitAll()
            .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
            .requestMatchers("/api/utilizadores").hasRole("ADMINISTRADOR")
            .anyRequest().authenticated()
    )
    .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler));
```

---

## 4. Referência da API

### 4.1 Autenticação (`/api/auth`)

#### POST /api/auth/login

```
Pedido:
{
  "email": "utilizador@email.com",
  "password": "palavra_passe_texto"
}

Resposta 200:
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "id": 1,
  "email": "utilizador@email.com"
}

Resposta 401:
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Bad credentials",
  "path": "/api/auth/login"
}
```

#### POST /api/auth/register

```
Pedido:
{
  "email": "novo@email.com",
  "password": "palavra_passe",
  "primeiroNome": "Joao",
  "ultimoNome": "Silva",
  "nif": "123456789",
  "dataNascimento": "2000-01-15"
}

Resposta 200: "Utilizador registado com sucesso!"

Resposta 400: "Erro: Email já em uso!"
```

O backend publica o evento `UTILIZADOR_CRIADO` no RabbitMQ após registo bem-sucedido.

#### GET /api/auth/me

```
Headers: Authorization: Bearer <token>

Resposta 200:
{
  "id": 1,
  "primeiroNome": "Joao",
  "ultimoNome": "Silva",
  "email": "joao@email.com",
  "nif": "123456789",
  "dataNascimento": "2000-01-15",
  "nrPontos": 150,
  "tipoUtilizador": "ADULTO",
  "papeis": [{"id": 1, "nome": "UTILIZADOR"}]
}
```

#### POST /api/auth/esqueceu-password

```
Pedido:
{
  "email": "utilizador@email.com"
}

Resposta 200: "Se o email existir, foi enviado um link de recuperação."
// Retorna sempre 200 para prevenir enumeração de emails
// Se o email existir: publica evento RECUPERACAO_PASSWORD_PEDIDA
```

#### POST /api/auth/redefinir-password

```
Pedido:
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "novaPassword": "nova_palavra_passe"
}

Resposta 200: "Palavra-passe redefinida com sucesso"

Resposta 400: "Token inválido ou expirado"
```

---

### 4.2 Utilizadores (`/api/utilizadores`)

#### GET /api/utilizadores — Listar todos os utilizadores `[ADMINISTRADOR]`

```
Resposta 200:
[
  {
    "id": 1,
    "primeiroNome": "Joao",
    "ultimoNome": "Silva",
    "email": "joao@email.com",
    "nif": "123456789",
    "dataNascimento": "2000-01-15",
    "nrPontos": 150,
    "tipoUtilizador": "ADULTO",
    "papeis": [{"id": 1, "nome": "UTILIZADOR"}]
  }
]
// Nota: password é sempre @JsonIgnore
```

#### GET /api/utilizadores/{id} — Obter utilizador por ID `[ADMINISTRADOR]`

```
Resposta 200: { /* objeto utilizador */ }
Resposta 404: null
```

#### GET /api/utilizadores/perfil — Obter o meu perfil

```
Headers: Authorization: Bearer <token>
Resposta 200: { /* objeto utilizador atual */ }
```

#### PUT /api/utilizadores/perfil — Atualizar o meu perfil

```
Headers: Authorization: Bearer <token>
Pedido:
{
  "primeiroNome": "Joao",
  "ultimoNome": "Silva Atualizado",
  "nif": "987654321",
  "dataNascimento": "2000-01-15",
  "tipoUtilizador": "ESTUDANTE"
}
Resposta 200: { /* utilizador atualizado */ }
```

#### PUT /api/utilizadores/{id}/papeis — Atualizar papéis do utilizador `[ADMINISTRADOR]`

```
Pedido:
{
  "nomesPapeis": ["ADMINISTRADOR", "UTILIZADOR"]
}
Resposta 200: { /* utilizador atualizado com novos papéis */ }
```

#### DELETE /api/utilizadores/{id} — Eliminar utilizador `[ADMINISTRADOR]`

```
Resposta 200: "Utilizador eliminado"
```

---

### 4.3 Bilhetes e Passes (`/api/tickets`)

#### POST /api/tickets/comprar — Comprar bilhetes

```
Headers: Authorization: Bearer <token>
Pedido:
{
  "quantidade": 3,
  "zonaIds": [1]
}

Resposta 200:
[
  {
    "id": 10,
    "usado": false,
    "utilizador": { "id": 1 },
    "zonas": [{ "id": 1, "nome": "Zona 1", "num": 1 }]
  }
]
```

#### POST /api/tickets/passe/comprar — Comprar passe

```
Headers: Authorization: Bearer <token>
Pedido:
{
  "modalidade": "MENSAL",
  "zonaIds": [1, 2]
}

Resposta 200:
{
  "id": 15,
  "inicio": "2026-05-22T10:00:00",
  "fim": "2026-06-22T10:00:00",
  "modalidade": "MENSAL",
  "utilizador": { "id": 1 },
  "zonas": [
    { "id": 1, "nome": "Zona 1", "num": 1 },
    { "id": 2, "nome": "Zona 2", "num": 2 }
  ]
}
```

#### GET /api/tickets/meus-bilhetes — Obter os meus bilhetes

```
Headers: Authorization: Bearer <token>
Resposta 200: [ /* lista de Bilhete */ ]
```

#### GET /api/tickets/meu-passe — Obter o meu passe

```
Headers: Authorization: Bearer <token>
Resposta 200: { /* objeto Passe */ } ou null
```

---

### 4.4 Rede de Transporte (`/api/network`) — Leitura pública

#### GET /api/network/paragens — Listar todas as paragens

```
Resposta 200:
[
  {
    "id": 1,
    "nome": "Paragem Centro",
    "localizacao": { "latitude": 32.6669, "longitude": -16.9241 }
  }
]
```

#### GET /api/network/paragens/{id}

```
Resposta 200: { /* Paragem */ }
Resposta 404
```

#### GET /api/network/linhas — Listar todas as linhas

```
Resposta 200:
[
  {
    "id": 1,
    "nome": "Linha 1 - Azul",
    "identificadorServico": "L1",
    "trajetos": [
      { "id": 1, "direcao": "IDA" },
      { "id": 2, "direcao": "VOLTA" }
    ]
  }
]
```

#### GET /api/network/linhas/{id}

```
Resposta 200: { /* Linha com trajetos */ }
Resposta 404
```

#### GET /api/network/linhas/{linhaId}/trajetos

```
Resposta 200: [ /* lista de Trajeto para esta Linha */ ]
```

#### GET /api/network/trajetos — Listar todos os trajetos

```
Resposta 200: [ /* lista de Trajeto */ ]
```

#### GET /api/network/trajetos/{id}

```
Resposta 200:
{
  "id": 1,
  "direcao": "IDA",
  "linha": { "id": 1, "nome": "Linha 1 - Azul" },
  "pontosDePassagem": [
    {
      "id": 1,
      "ordem": 1,
      "horaChegada": "2026-05-22T08:00:00",
      "tempoDesdeInicio": "PT0S",
      "paragem": { "id": 1, "nome": "Terminal" }
    },
    {
      "id": 2,
      "ordem": 2,
      "horaChegada": "2026-05-22T08:10:00",
      "tempoDesdeInicio": "PT10M",
      "paragem": { "id": 2, "nome": "Centro" }
    }
  ]
}
```

#### GET /api/network/trajetos/{trajetoId}/pontos

```
Resposta 200: [ /* PontosDePassagem ordenados por ordem */ ]
```

---

### 4.5 Zonas (`/api/zonas`)

#### GET /api/zonas — Listar todas as zonas

```
Resposta 200:
[
  {
    "id": 1,
    "num": 1,
    "nome": "Zona 1 - Centro",
    "paragens": [{ "id": 1, "nome": "Paragem Centro" }]
  },
  {
    "id": 2,
    "num": 2,
    "nome": "Zona 2 - Periferia",
    "paragens": [{ "id": 3, "nome": "Paragem Subúrbio" }]
  }
]
```

#### GET /api/zonas/{id}

```
Resposta 200: { /* Zona com paragens */ }
Resposta 404
```

#### GET /api/zonas/{id}/paragens — Obter paragens na zona

```
Resposta 200: [ /* lista de Paragem */ ]
```

---

### 4.6 Tarifas (`/api/tarifas`)

#### GET /api/tarifas — Listar todas as tarifas

```
Resposta 200:
[
  {
    "id": 1,
    "valor": 30.00,
    "tipoUtilizador": "ADULTO",
    "modalidade": "MENSAL",
    "nrZonas": 1
  },
  {
    "id": 2,
    "valor": 45.00,
    "tipoUtilizador": "ADULTO",
    "modalidade": "MENSAL",
    "nrZonas": 2
  }
]
```

#### GET /api/tarifas/{id}

```
Resposta 200: { /* Tarifa */ }
Resposta 404
```

#### GET /api/tarifas/perfil/{perfil}

```
// perfil = CRIANCA | ESTUDANTE | ADULTO | SENIOR
Resposta 200: [ /* Tarifas para este perfil */ ]
```

#### GET /api/tarifas/modalidade/{modalidade}

```
// modalidade = MENSAL | ANUAL | H24 | H48 | H72 | SEMANAL
Resposta 200: [ /* Tarifas para esta modalidade */ ]
```

#### GET /api/tarifas/calculadora — Calcular tarifa

```
Parâmetros de consulta:
  tipoUtilizador : CRIANCA | ESTUDANTE | ADULTO | SENIOR  (obrigatório para passe)
  modalidade     : MENSAL | ANUAL | H24 | ...              (opcional)
  nrZonas        : 1 | 2 | 3                               (obrigatório)

Resposta 200:
{
  "valor": 45.00,
  "tipoUtilizador": "ADULTO",
  "modalidade": "MENSAL",
  "nrZonas": 2,
  "tipo": "PASSE"
}

Resposta 404: "Nenhuma tarifa encontrada para os parâmetros indicados"
```

---

### 4.7 Veículos (`/api/veiculos`)

#### GET /api/veiculos — Listar todos os veículos

```
Resposta 200:
[
  {
    "id": 1,
    "matricula": "AB-12-CD",
    "nLugares": 50,
    "lotacaoAtual": 23,
    "localizacaoAtual": { "latitude": 32.6669, "longitude": -16.9241 }
  }
]
// Usa @JsonTypeInfo/@JsonSubTypes para polimorfismo de Veiculo
```

#### GET /api/veiculos/{id}

```
Resposta 200: { /* Veiculo (Autocarro) */ }
Resposta 404
```

#### GET /api/veiculos/matricula/{matricula}

```
Resposta 200: { /* Veiculo */ }
Resposta 404
```

---

### 4.8 Viagens (`/api/viagens`)

#### POST /api/viagens/utilizador/iniciar — Iniciar viagem do utilizador

```
Headers: Authorization: Bearer <token>
Pedido:
{
  "tituloId": 10,
  "paragemEntradaId": 1,
  "viagemVeiculoId": 5
}

Resposta 200:
{
  "id": 20,
  "inicio": "2026-05-22T08:30:00",
  "fim": null,
  "estado": "ATIVA",
  "titulo": { "id": 10 },
  "viagemVeiculo": { "id": 5 },
  "paragemEntrada": { "id": 1, "nome": "Terminal" },
  "paragemSaida": null,
  "coima": null
}
```

#### PUT /api/viagens/utilizador/{id}/terminar — Terminar viagem do utilizador

```
Headers: Authorization: Bearer <token>
Pedido:
{
  "paragemSaidaId": 5
}

Resposta 200:
{
  "id": 20,
  "inicio": "2026-05-22T08:30:00",
  "fim": "2026-05-22T08:55:00",
  "estado": "CONCLUIDA",
  "paragemSaida": { "id": 5, "nome": "Centro" }
}
// Atribui pontos ao utilizador ao completar
```

#### GET /api/viagens/utilizador — Listar todas as viagens de utilizador

```
Resposta 200: [ /* lista de ViagemUtilizador */ ]
```

#### GET /api/viagens/utilizador/{id}

```
Resposta 200: { /* ViagemUtilizador */ }
Resposta 404
```

#### GET /api/viagens/veiculo — Listar todas as viagens de veículo

```
Resposta 200: [ /* lista de ViagemVeiculo */ ]
```

#### GET /api/viagens/veiculo/{id}

```
Resposta 200: { /* ViagemVeiculo com viagensUtilizador */ }
Resposta 404
```

#### GET /api/viagens/veiculo/por-veiculo/{veiculoId}

```
Resposta 200: [ /* lista de ViagemVeiculo */ ]
```

#### GET /api/viagens/veiculo/por-trajeto/{trajetoId}

```
Resposta 200: [ /* lista de ViagemVeiculo */ ]
```

---

### 4.9 Validação (`/api/validacao`)

#### POST /api/validacao/{tituloId} — Validar título

```
Headers: Authorization: Bearer <token>
Resposta 200:
{
  "valido": true,
  "tipo": "PASSE",
  "mensagem": "Passe válido até 2026-06-22T10:00:00"
}

Resposta 200 (inválido):
{
  "valido": false,
  "tipo": "BILHETE",
  "mensagem": "Bilhete já foi utilizado"
}
```

#### POST /api/validacao/{tituloId}/usar — Validar e consumir

```
Headers: Authorization: Bearer <token>
// Para bilhetes: marca como usado
// Para passes: apenas valida (passes não são consumidos por viagem)
Resposta 200:
{
  "valido": true,
  "consumido": true,
  "mensagem": "Bilhete validado e marcado como usado"
}
```

---

### 4.10 Transações (`/api/transacoes`)

#### GET /api/transacoes — Listar todas as transações `[ADMINISTRADOR]`

```
Resposta 200: [ /* lista de Transacao */ ]
```

#### GET /api/transacoes/{id}

```
Resposta 200: { /* Transacao */ }
Resposta 404
```

#### GET /api/transacoes/minhas — Minhas transações

```
Headers: Authorization: Bearer <token>
Resposta 200: [ /* lista de Transacao do utilizador atual */ ]
```

#### GET /api/transacoes/estado/{estado}

```
// estado = EM_CURSO | CANCELADO | CONCLUIDO | REJEITADO
Resposta 200: [ /* lista de Transacao */ ]
```

#### POST /api/transacoes — Criar transação

```
Headers: Authorization: Bearer <token>
Pedido:
{
  "tituloId": 10,
  "referenciaExterna": "PAYPAL-REF-12345"
}
Resposta 200: { /* Transacao criada com estado EM_CURSO */ }
```

#### PUT /api/transacoes/{id}/estado — Atualizar estado do pagamento

```
Pedido:
{
  "estado": "CONCLUIDO"
}
Resposta 200: { /* Transacao atualizada */ }
```

---

### 4.11 Pontos (`/api/pontos`)

#### GET /api/pontos/saldo — Obter saldo de pontos

```
Headers: Authorization: Bearer <token>
Resposta 200:
{
  "nrPontos": 150
}
```

#### GET /api/pontos/historico — Obter histórico de pontos

```
Headers: Authorization: Bearer <token>
Resposta 200:
[
  {
    "id": 1,
    "pontos": 10,
    "tipo": "VIAGEM",
    "descricao": "Pontos ganhos por viagem concluída",
    "dataHora": "2026-05-22T09:00:00"
  },
  {
    "id": 2,
    "pontos": -50,
    "tipo": "RESGATE",
    "descricao": "Pontos utilizados para desconto",
    "dataHora": "2026-05-22T10:00:00"
  }
]
```

#### POST /api/pontos/utilizar — Utilizar pontos

```
Headers: Authorization: Bearer <token>
Pedido:
{
  "pontos": 50,
  "descricao": "Desconto no passe mensal"
}
Resposta 200:
{
  "nrPontos": 100,
  "mensagem": "50 pontos utilizados com sucesso"
}
Resposta 400: "Pontos insuficientes"
```

---

### 4.12 Endpoints de Administração (`/api/admin`) — Requerem papel ADMINISTRADOR

#### Admin - Gestão da Rede

```
POST   /api/admin/network/paragens              Criar paragem
PUT    /api/admin/network/paragens/{id}         Atualizar paragem
DELETE /api/admin/network/paragens/{id}         Eliminar paragem

POST   /api/admin/network/linhas                Criar linha
PUT    /api/admin/network/linhas/{id}           Atualizar linha
DELETE /api/admin/network/linhas/{id}           Eliminar linha

POST   /api/admin/network/trajetos              Criar trajeto
PUT    /api/admin/network/trajetos/{id}         Atualizar trajeto
DELETE /api/admin/network/trajetos/{id}         Eliminar trajeto

POST   /api/admin/network/trajetos/{id}/pontos  Adicionar ponto de passagem
PUT    /api/admin/network/pontos/{id}           Atualizar ponto de passagem
DELETE /api/admin/network/pontos/{id}           Eliminar ponto de passagem
```

#### Admin - Gestão de Zonas

```
POST   /api/admin/zonas                         Criar zona
PUT    /api/admin/zonas/{id}                    Atualizar zona
DELETE /api/admin/zonas/{id}                    Eliminar zona
POST   /api/admin/zonas/{id}/paragens/{pid}     Adicionar paragem à zona
DELETE /api/admin/zonas/{id}/paragens/{pid}     Remover paragem da zona
```

#### Admin - Gestão de Tarifas

```
POST   /api/admin/tarifas                       Criar tarifa
PUT    /api/admin/tarifas/{id}                  Atualizar tarifa
DELETE /api/admin/tarifas/{id}                  Eliminar tarifa
```

#### Admin - Gestão de Veículos

```
POST   /api/admin/veiculos                      Criar veículo (Autocarro)
PUT    /api/admin/veiculos/{id}                 Atualizar veículo
DELETE /api/admin/veiculos/{id}                 Eliminar veículo
PUT    /api/admin/veiculos/{id}/localizacao     Atualizar localização GPS
PUT    /api/admin/veiculos/{id}/lotacao         Atualizar lotação
```

#### Admin - Gestão de Viagens de Veículo

```
POST   /api/admin/viagens/veiculo               Criar viagem de veículo
PUT    /api/admin/viagens/veiculo/{id}          Atualizar viagem de veículo
DELETE /api/admin/viagens/veiculo/{id}          Eliminar viagem de veículo
```

#### Admin - Estatísticas do Painel

```
GET    /api/admin/stats                         Visão geral do sistema

Resposta 200:
{
  "totalUtilizadores": 1250,
  "totalViagensHoje": 340,
  "totalBilhetesVendidos": 890,
  "totalPassesAtivos": 420,
  "receitaMensal": 45600.50,
  "veiculosAtivos": 15,
  "viagensEmCurso": 8
}
```

---

## 5. Microsserviço de Email

### 5.1 Propósito

O microsserviço de email tem exatamente **duas responsabilidades**:

1. **Email de Boas-vindas** — enviado quando um novo utilizador se regista
2. **Email de Recuperação de Palavra-passe** — enviado quando o utilizador pede para redefinir a password

### 5.2 Arquitetura

```
Backend (produtor)             RabbitMQ              Serviço de Email (consumidor)
─────────────────             ─────────             ─────────────────────────────
UTILIZADOR_CRIADO     ──►   notub.email.fila  ──►  ProcessadorEmailBoasVindas
RECUPERACAO_PASSWORD  ──►   notub.email.fila  ──►  ProcessadorRecuperacaoPassword
```

### 5.3 Configuração RabbitMQ

**Exchange:** `notub.email.exchange` (tipo: direct, durável)

**Fila:** `notub.email.fila` (durável)

**Bindings:**
- `notub.email.exchange` + routing key `UTILIZADOR_CRIADO` → `notub.email.fila`
- `notub.email.exchange` + routing key `RECUPERACAO_PASSWORD_PEDIDA` → `notub.email.fila`

### 5.4 Contratos de Eventos

#### Evento UTILIZADOR_CRIADO

```json
{
  "tipoEvento": "UTILIZADOR_CRIADO",
  "carimboTemporal": "2026-05-22T10:00:00",
  "dados": {
    "utilizadorId": 1,
    "email": "joao@email.com",
    "primeiroNome": "Joao",
    "ultimoNome": "Silva"
  }
}
```

#### Evento RECUPERACAO_PASSWORD_PEDIDA

```json
{
  "tipoEvento": "RECUPERACAO_PASSWORD_PEDIDA",
  "carimboTemporal": "2026-05-22T10:30:00",
  "dados": {
    "utilizadorId": 1,
    "email": "joao@email.com",
    "primeiroNome": "Joao",
    "tokenRecuperacao": "550e8400-e29b-41d4-a716-446655440000",
    "urlRecuperacao": "https://localhost/redefinir-password?token=550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### 5.5 Configuração SMTP Mailtrap

```yaml
# servico-email application.yml
spring:
  mail:
    host: sandbox.smtp.mailtrap.io
    port: 587
    username: ${MAILTRAP_USERNAME}
    password: ${MAILTRAP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

### 5.6 Templates de Email

#### Email de Boas-vindas

```
Assunto: Bem-vindo ao NoTUB!

De: noreply@notub.pt
Para: {email}

Corpo (HTML):
─────────────────────────────────────────
  Logo NoTUB

  Bem-vindo, {primeiroNome}!

  A sua conta foi criada com sucesso.

  Com o NoTUB pode:
    - Comprar bilhetes e passes
    - Acompanhar as suas viagens
    - Consultar rotas e horários

  Comece já: {FRONTEND_URL}/home

  ──
  NoTUB - Transporte Público
─────────────────────────────────────────
```

#### Email de Recuperação de Palavra-passe

```
Assunto: NoTUB - Recuperação de Palavra-passe

De: noreply@notub.pt
Para: {email}

Corpo (HTML):
─────────────────────────────────────────
  Logo NoTUB

  Olá, {primeiroNome}

  Recebemos um pedido para redefinir a sua palavra-passe.
  Clique no botão abaixo para definir uma nova palavra-passe:

  [REDEFINIR PALAVRA-PASSE] ← link: {urlRecuperacao}

  Este link expira em 30 minutos.

  Se não fez este pedido, ignore este email.

  ──
  NoTUB - Transporte Público
─────────────────────────────────────────
```

### 5.7 Estrutura de Pastas do Serviço de Email

```
servico-email/
├── pom.xml
├── Dockerfile
└── src/
    └── main/
        ├── java/pt/notub/email/
        │   ├── ServicoEmailApplication.java
        │   ├── config/
        │   │   ├── RabbitMQConfig.java
        │   │   └── MailConfig.java
        │   ├── consumidor/
        │   │   └── ConsumidorEventosEmail.java
        │   ├── dto/
        │   │   ├── EventoUtilizadorCriado.java
        │   │   └── EventoRecuperacaoPassword.java
        │   └── servico/
        │       └── ServicoEnvioEmail.java
        └── resources/
            ├── application.yml
            └── templates/
                ├── email-boas-vindas.html
                └── email-recuperacao-password.html
```

---

## 6. Arquitetura do Frontend

### 6.1 Stack Tecnológica

- **Vue 3** (Composition API + `<script setup>`)
- **Quasar Framework** v2 (componentes UI)
- **Pinia** (gestão de estado)
- **Vue Router** v5 (modo history)
- **PWA** (service worker + manifest)
- **Capacitor** (opcional, para mobile nativo)

### 6.2 Mapa de Rotas

#### Rotas Públicas

| Caminho | Página | Layout | Descrição |
|---------|--------|--------|-----------|
| `/login` | PaginaLogin | LayoutAuth | Formulário de login + Google OAuth |
| `/registar` | PaginaRegistar | LayoutAuth | Formulário de registo |
| `/oauth2/redirect` | PaginaOAuth2Redirect | nenhum | Callback OAuth2 |
| `/esqueceu-password` | PaginaEsqueceuPassword | LayoutAuth | Introduzir email para recuperação |
| `/redefinir-password` | PaginaRedefinirPassword | LayoutAuth | Nova palavra-passe (via link email) |

#### Rotas de Utilizador (requer autenticação, papel: UTILIZADOR ou ADMINISTRADOR)

| Caminho | Página | Layout | Tab | Descrição |
|---------|--------|--------|-----|-----------|
| `/home` | PaginaInicial | LayoutPrincipal | home | Painel, estado viagem ativa |
| `/viagens` | PaginaViagens | LayoutPrincipal | viagens | Histórico de viagens |
| `/scan` | PaginaScannerQR | LayoutPrincipal | scan | Scanner QR para embarque |
| `/bilhetes` | PaginaBilhetes | LayoutPrincipal | bilhetes | Comprar/gerir bilhetes e passes |
| `/conta` | PaginaEditarConta | LayoutPrincipal | conta | Editar perfil |
| `/viajando` | PaginaViajando | LayoutPrincipal | scan | Vista de viagem ativa |
| `/linhas` | PaginaLinhas | LayoutPrincipal | - | Ver todas as linhas/rotas |
| `/linhas/:id` | PaginaDetalheLinha | LayoutPrincipal | - | Detalhe da linha com paragens |
| `/zonas` | PaginaZonas | LayoutPrincipal | - | Vista do mapa de zonas |

#### Rotas de Administrador (requer autenticação, papel: ADMINISTRADOR)

| Caminho | Página | Layout | Descrição |
|---------|--------|--------|-----------|
| `/admin` | PaginaPainelAdmin | LayoutAdmin | Visão geral de estatísticas |
| `/admin/utilizadores` | PaginaAdminUtilizadores | LayoutAdmin | Gerir utilizadores + papéis |
| `/admin/utilizadores/:id` | PaginaAdminDetalheUtilizador | LayoutAdmin | Detalhe do utilizador / editar papéis |
| `/admin/rede` | PaginaAdminRede | LayoutAdmin | Gerir linhas, trajetos, paragens |
| `/admin/rede/linhas/nova` | PaginaFormularioLinha | LayoutAdmin | Criar/editar linha |
| `/admin/rede/trajetos/novo` | PaginaFormularioTrajeto | LayoutAdmin | Criar/editar trajeto |
| `/admin/rede/paragens/nova` | PaginaFormularioParagem | LayoutAdmin | Criar/editar paragem |
| `/admin/zonas` | PaginaAdminZonas | LayoutAdmin | Gerir zonas |
| `/admin/veiculos` | PaginaAdminVeiculos | LayoutAdmin | Gerir veículos |
| `/admin/tarifas` | PaginaAdminTarifas | LayoutAdmin | Gerir tarifas |
| `/admin/viagens` | PaginaAdminViagens | LayoutAdmin | Gerir viagens de veículo |

### 6.3 Especificações de Páginas

#### PaginaLogin (`/login`)

```
- Campo de email (obrigatório)
- Campo de palavra-passe (obrigatório, mascarado)
- Botão "Continuar" (submeter)
- Link "Esqueceu a palavra-passe?" → /esqueceu-password
- Divisor "ou"
- Botão "Continuar com o Google" → /oauth2/authorization/google
- Link "Não tem conta? Registe-se" → /registar
```

#### PaginaEsqueceuPassword (`/esqueceu-password`)

```
- Título: "Recuperar palavra-passe"
- Subtítulo: "Insira o seu email para receber o link de recuperação"
- Campo de email (obrigatório)
- Botão "Enviar" (submeter)
- Mensagem de sucesso: "Se o email existir, enviámos o link de recuperação."
- Link "Voltar ao login" → /login
```

#### PaginaRedefinirPassword (`/redefinir-password`)

```
- Título: "Nova palavra-passe"
- Lê o token do parâmetro URL: ?token=<uuid>
- Campo nova palavra-passe (obrigatório, mascarado)
- Campo confirmar palavra-passe (obrigatório, mascarado)
- Botão "Redefinir palavra-passe" (submeter)
- Sucesso: redireciona para /login com mensagem flash
- Erro: "Link inválido ou expirado"
```

#### PaginaInicial (`/home`)

```
- Mensagem de boas-vindas: "Olá, {primeiroNome}!"
- Cartão de viagem ativa (se o utilizador tem ViagemUtilizador ATIVA):
  - Mostra paragemEntrada, info veículo, duração
  - Botão "Terminar viagem" → chama terminarViagem
- Se sem viagem ativa:
  - Botão circular grande "Iniciar" → /scan
- Estatísticas rápidas: bilhetes disponíveis, estado do passe atual, saldo de pontos
- Mostrador de lotação do autocarro (do veículo ativo)
```

#### PaginaBilhetes (`/bilhetes`)

```
- Tabs: "Bilhetes" | "Passe"

Tab Bilhetes:
  - Botão "Comprar bilhetes" → abre modal de compra
    - Seletor de quantidade (1-10)
    - Seletor de zonas (checkboxes, mínimo 1)
    - Pré-visualização do preço (via /api/tarifas/calculadora)
    - Botão "Confirmar compra"
  - Lista de bilhetes possuídos:
    - Badge de estado: "Disponível" (verde) ou "Usado" (cinzento)
    - Info da zona
    - QR code para validação

Tab Passe:
  - Se tem passe ativo:
    - Cartão de passe mostrando tipo, zonas, datas válidas, estado
    - Barra de progresso do período de validade
  - Se sem passe:
    - Cartão "Comprar passe"
    - Selecionar modalidade (MENSAL, SEMANAL, etc.)
    - Selecionar zonas (checkboxes)
    - Pré-visualização do preço
    - Botão "Confirmar compra"
```

#### PaginaViagens (`/viagens`)

```
- Filtro: intervalo de datas, estado (ATIVA/CONCLUIDA)
- Lista de ViagemUtilizador:
  - Data/hora
  - Paragem entrada → Paragem saída
  - Duração
  - Badge de estado (ATIVA=azul, CONCLUIDA=verde)
  - Info do veículo
  - Link para detalhes da linha/trajeto
- Estado vazio: "Sem viagens registadas"
```

#### PaginaLinhas (`/linhas`)

```
- Barra de pesquisa (filtrar por nome)
- Lista de Linha:
  - Nome + identificador de serviço
  - Contagem de trajetos
  - Indicadores de direção (IDA / VOLTA)
- Clique → /linhas/:id
```

#### PaginaDetalheLinha (`/linhas/:id`)

```
- Nome da linha + identificador
- Tabs para trajetos IDA / VOLTA
- Para cada trajeto:
  - Lista ordenada de paragens (PontosDePassagem)
  - Horas de chegada
  - Timeline visual do trajeto
- Veículos ativos nesta linha (se houver)
```

#### PaginaZonas (`/zonas`)

```
- Mapa visual de zonas (representação de círculos concêntricos)
- Para cada zona:
  - Nome e número da zona
  - Número de paragens
  - Lista de paragens
  - Info de tarifas (links para calculadora)
```

#### PaginaScannerQR (`/scan`)

```
- Visor da câmara com moldura sobreposta
- Instruções de leitura
- Ao ler QR (contém viagemVeiculoId):
  - Valida que utilizador tem título válido
  - Chama POST /api/viagens/utilizador/iniciar
  - Redireciona para /viajando em caso de sucesso
  - Mostra erro se não tem título válido
```

#### PaginaViajando (`/viajando`)

```
- Cabeçalho "A viajar..."
- Info da viagem ativa:
  - Nome da paragem de entrada
  - Contador de duração (em tempo real)
  - Lotação do veículo
- Botão "Validar viagem" (re-ler QR)
- Botão "Sair" → chama terminarViagem → /home
- Notificação de pontos atribuídos ao completar viagem
```

#### PaginaPainelAdmin (`/admin`)

```
- Cartões de estatísticas: total utilizadores, viagens hoje, bilhetes vendidos, receita
- Gráficos: viagens por dia, receita mensal, linhas populares
- Links rápidos para páginas de gestão
```

#### PaginaAdminUtilizadores (`/admin/utilizadores`)

```
- Barra de pesquisa (por email/nome)
- Tabela: nome, email, tipoUtilizador, papéis, nrPontos, ações
- Ações: editar papéis, eliminar
- Paginação
```

#### PaginaAdminRede (`/admin/rede`)

```
- Tabs: Linhas | Trajetos | Paragens
- Tabela CRUD para cada um com edição inline
- Botão "Adicionar novo" por tab
```

#### PaginaAdminTarifas (`/admin/tarifas`)

```
- Tabela: tipoUtilizador, modalidade, nrZonas, valor
- Campo valor editável (inline)
- Botão "Adicionar tarifa"
- Vista de matriz de tarifas por zona
```

### 6.4 Stores Pinia

```
stores/
├── index.js              // Configuração Pinia
├── auth.js               // token, utilizador, login, registar, logout, fetchUtilizador
├── tickets.js            // meusBilhetes, meuPasse, comprarBilhetes, comprarPasse
├── viagens.js            // historicoViagens, viagemAtiva, iniciarViagem, terminarViagem
├── rede.js               // linhas, paragens, trajetos, pontosDePassagem
├── zonas.js              // lista de zonas
├── tarifas.js            // lista de tarifas, calcularTarifa
├── pontos.js             // saldo, historico, utilizarPontos
├── notificacoes.js       // fila de toasts/notificações
└── admin.js              // estatísticas admin, dados de gestão
```

### 6.5 Cliente API

Todos os pedidos à API passam por `src/boot/api.js` (ficheiro boot Quasar):

```javascript
const API_BASE = '/api'

async function apiFetch(caminho, opcoes = {}) {
  const token = localStorage.getItem('token')
  const headers = {
    'Content-Type': 'application/json',
    ...(token && { Authorization: `Bearer ${token}` }),
    ...opcoes.headers,
  }
  const resposta = await fetch(`${API_BASE}${caminho}`, { ...opcoes, headers })
  if (resposta.status === 401) {
    localStorage.removeItem('token')
    window.location.href = '/login'
    return
  }
  if (!resposta.ok) {
    const texto = await resposta.text()
    throw new Error(texto || `Erro ${resposta.status}`)
  }
  const texto = await resposta.text()
  return texto ? JSON.parse(texto) : null
}

export const api = {
  get: (caminho) => apiFetch(caminho, { method: 'GET' }),
  post: (caminho, corpo) => apiFetch(caminho, { method: 'POST', body: JSON.stringify(corpo) }),
  put: (caminho, corpo) => apiFetch(caminho, { method: 'PUT', body: JSON.stringify(corpo) }),
  delete: (caminho) => apiFetch(caminho, { method: 'DELETE' }),
}
```

### 6.6 Configuração PWA

```javascript
// quasar.config.js - secção pwa
pwa: {
  workboxMode: 'GenerateSW',
  manifest: {
    name: 'NoTUB',
    short_name: 'NoTUB',
    description: 'Gestão de Transporte Público',
    display: 'standalone',
    orientation: 'portrait',
    background_color: '#ffffff',
    theme_color: '#1876d2',
    icons: [
      { src: 'icons/android-chrome-192x192.png', sizes: '192x192', type: 'image/png' },
      { src: 'icons/android-chrome-512x512.png', sizes: '512x512', type: 'image/png' }
    ]
  }
}
```

---

## 7. Estrutura de Pastas

### 7.1 Backend (`backend/notub`)

```
backend/notub/
├── Dockerfile
├── pom.xml
├── mvnw
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
└── src/
    └── main/
        ├── java/pt/notub/
        │   ├── NotubApplication.java
        │   ├── config/
        │   │   ├── RabbitMQConfig.java
        │   │   └── CorsConfig.java
        │   ├── models/
        │   │   ├── Autocarro.java
        │   │   ├── Bilhete.java
        │   │   ├── Coima.java
        │   │   ├── Direcao.java
        │   │   ├── EstadoPagamento.java
        │   │   ├── EstadoViagem.java
        │   │   ├── HistoricoPontos.java
        │   │   ├── Linha.java
        │   │   ├── ModalidadePasse.java
        │   │   ├── Paragem.java
        │   │   ├── Passe.java
        │   │   ├── Papel.java
        │   │   ├── TipoPapel.java
        │   │   ├── Point.java
        │   │   ├── PontosDePassagem.java
        │   │   ├── Tarifa.java
        │   │   ├── TipoUtilizador.java
        │   │   ├── TituloTransporte.java
        │   │   ├── TokenRecuperacaoSenha.java
        │   │   ├── Transacao.java
        │   │   ├── Trajeto.java
        │   │   ├── Utilizador.java
        │   │   ├── Veiculo.java
        │   │   ├── ViagemUtilizador.java
        │   │   ├── ViagemVeiculo.java
        │   │   └── Zona.java
        │   ├── dto/
        │   │   ├── pedido/
        │   │   │   ├── PedidoLogin.java
        │   │   │   ├── PedidoRegisto.java
        │   │   │   ├── PedidoComprarBilhete.java
        │   │   │   ├── PedidoComprarPasse.java
        │   │   │   ├── PedidoEsqueceuPassword.java
        │   │   │   └── PedidoRedefinirPassword.java
        │   │   └── resposta/
        │   │       ├── RespostaJwt.java
        │   │       ├── ResultadoValidacao.java
        │   │       ├── ResultadoCalculoTarifa.java
        │   │       └── RespostaSaldoPontos.java
        │   ├── repositories/
        │   │   ├── BilheteRepository.java
        │   │   ├── CoimaRepository.java
        │   │   ├── HistoricoPontosRepository.java
        │   │   ├── LinhaRepository.java
        │   │   ├── ParagemRepository.java
        │   │   ├── PapelRepository.java
        │   │   ├── PasseRepository.java
        │   │   ├── PontosDePassagemRepository.java
        │   │   ├── TarifaRepository.java
        │   │   ├── TituloTransporteRepository.java
        │   │   ├── TokenRecuperacaoSenhaRepository.java
        │   │   ├── TransacaoRepository.java
        │   │   ├── TrajetoRepository.java
        │   │   ├── UtilizadorRepository.java
        │   │   ├── VeiculoRepository.java
        │   │   ├── ViagemUtilizadorRepository.java
        │   │   ├── ViagemVeiculoRepository.java
        │   │   └── ZonaRepository.java
        │   ├── services/
        │   │   ├── EstrategiaValidacao.java
        │   │   ├── ValidacaoQRCode.java
        │   │   ├── GestorValidacao.java
        │   │   ├── ServicoAuth.java
        │   │   ├── ServicoBilhetes.java
        │   │   ├── ServicoTransacao.java
        │   │   ├── ServicoRedeTransporte.java
        │   │   ├── ServicoTarifa.java
        │   │   ├── ServicoUtilizador.java
        │   │   ├── ServicoVeiculo.java
        │   │   ├── ServicoViagem.java
        │   │   ├── ServicoZona.java
        │   │   ├── ServicoPontos.java
        │   │   ├── ServicoAdmin.java
        │   │   └── PublicadorEventosEmail.java
        │   ├── controllers/
        │   │   ├── AuthController.java
        │   │   ├── TestController.java
        │   │   ├── TransportNetworkController.java
        │   │   ├── TarifaController.java
        │   │   ├── ZonaController.java
        │   │   ├── TicketController.java
        │   │   ├── TransacaoController.java
        │   │   ├── UtilizadorController.java
        │   │   ├── VeiculoController.java
        │   │   ├── ViagemController.java
        │   │   ├── ValidacaoController.java
        │   │   ├── PontosController.java
        │   │   ├── AdminRedeController.java
        │   │   ├── AdminZonaController.java
        │   │   ├── AdminTarifaController.java
        │   │   ├── AdminVeiculoController.java
        │   │   ├── AdminViagemController.java
        │   │   ├── AdminUtilizadorController.java
        │   │   └── AdminStatsController.java
        │   ├── security/
        │   │   ├── SecurityConfig.java
        │   │   ├── JwtUtils.java
        │   │   ├── AuthTokenFilter.java
        │   │   ├── AuthEntryPointJwt.java
        │   │   ├── OAuth2AuthenticationSuccessHandler.java
        │   │   ├── UserDetailsImpl.java
        │   │   └── UserDetailsServiceImpl.java
        │   └── exception/
        │       ├── GlobalExceptionHandler.java
        │       └── RecursoNaoEncontradoException.java
        └── resources/
            └── application.properties
```

### 7.2 Serviço de Email (`servico-email`)

```
servico-email/
├── Dockerfile
├── pom.xml
└── src/
    └── main/
        ├── java/pt/notub/email/
        │   ├── ServicoEmailApplication.java
        │   ├── config/
        │   │   ├── RabbitMQConfig.java
        │   │   └── MailConfig.java
        │   ├── consumidor/
        │   │   └── ConsumidorEventosEmail.java
        │   ├── dto/
        │   │   ├── EventoUtilizadorCriado.java
        │   │   └── EventoRecuperacaoPassword.java
        │   └── servico/
        │       └── ServicoEnvioEmail.java
        └── resources/
            ├── application.yml
            └── templates/
                ├── email-boas-vindas.html
                └── email-recuperacao-password.html
```

### 7.3 Frontend (`frontend/NoTUB_Frontend`)

```
frontend/NoTUB_Frontend/
├── index.html
├── package.json
├── quasar.config.js
├── capacitor.config.json
├── postcss.config.js
├── jsconfig.json
├── eslint.config.js
├── .prettierrc.json
├── .editorconfig
├── public/
│   ├── favicon.ico
│   ├── icons/
│   │   ├── favicon-16x16.png
│   │   ├── favicon-32x32.png
│   │   ├── apple-touch-icon.png
│   │   ├── android-chrome-192x192.png
│   │   └── android-chrome-512x512.png
│   └── assets/
│       ├── logo.png
│       ├── google-logo.svg
│       ├── icon-home.svg
│       ├── icon-bus.svg
│       ├── icon-qr.svg
│       ├── icon-qr2.svg
│       ├── icon-cart.svg
│       ├── icon-person.svg
│       ├── icon-exit.svg
│       ├── icon-calendar.svg
│       ├── icon-heart.svg
│       ├── corner-*.svg
│       ├── scanner-bg.png
│       ├── scanner-subtract.svg
│       ├── mask-group.svg
│       └── union*.svg
└── src/
    ├── App.vue
    ├── boot/
    │   └── api.js
    ├── css/
    │   └── app.scss
    ├── router/
    │   ├── index.js
    │   └── routes.js
    ├── stores/
    │   ├── index.js
    │   ├── auth.js
    │   ├── tickets.js
    │   ├── viagens.js
    │   ├── rede.js
    │   ├── zonas.js
    │   ├── tarifas.js
    │   ├── pontos.js
    │   ├── notificacoes.js
    │   └── admin.js
    ├── layouts/
    │   ├── MainLayout.vue
    │   ├── AuthLayout.vue
    │   └── AdminLayout.vue
    ├── components/
    │   ├── BrandHeader.vue
    │   ├── AppTabBar.vue
    │   ├── AdminSidebar.vue
    │   ├── CartaoBilhete.vue
    │   ├── CartaoPasse.vue
    │   ├── CartaoViagem.vue
    │   ├── CartaoLinha.vue
    │   ├── MapaZonas.vue
    │   ├── CalculadoraTarifa.vue
    │   ├── BadgePontos.vue
    │   └── CartaoEstatistica.vue
    ├── pages/
    │   ├── PaginaLogin.vue
    │   ├── PaginaRegistar.vue
    │   ├── PaginaOAuth2Redirect.vue
    │   ├── PaginaEsqueceuPassword.vue
    │   ├── PaginaRedefinirPassword.vue
    │   ├── PaginaInicial.vue
    │   ├── PaginaBilhetes.vue
    │   ├── PaginaViagens.vue
    │   ├── PaginaScannerQR.vue
    │   ├── PaginaViajando.vue
    │   ├── PaginaEditarConta.vue
    │   ├── PaginaLinhas.vue
    │   ├── PaginaDetalheLinha.vue
    │   ├── PaginaZonas.vue
    │   ├── PaginaNaoEncontrada.vue
    │   └── admin/
    │       ├── PaginaPainelAdmin.vue
    │       ├── PaginaAdminUtilizadores.vue
    │       ├── PaginaAdminDetalheUtilizador.vue
    │       ├── PaginaAdminRede.vue
    │       ├── PaginaFormularioLinha.vue
    │       ├── PaginaFormularioTrajeto.vue
    │       ├── PaginaFormularioParagem.vue
    │       ├── PaginaAdminZonas.vue
    │       ├── PaginaAdminVeiculos.vue
    │       ├── PaginaAdminTarifas.vue
    │       └── PaginaAdminViagens.vue
    └── composables/
        ├── usarApi.js
        └── usarAuth.js
```

---

## 8. Docker e Infraestrutura

### 8.1 docker-compose.yml

```yaml
services:
  # ─── Base de Dados ────────────────────────────
  postgres:
    image: postgres:18.3
    container_name: postgresdb
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_HOST_PORT}:5432"
    volumes:
      - pgdata:/var/lib/postgresql
    networks:
      - notub
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ─── Broker de Mensagens ──────────────────────
  rabbitmq:
    image: rabbitmq:4-management-alpine
    container_name: rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports:
      - "${RABBITMQ_HOST_PORT}:5672"
      - "${RABBITMQ_MGMT_PORT}:15672"
    volumes:
      - rabbitdata:/var/lib/rabbitmq
    networks:
      - notub
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 15s
      timeout: 10s
      retries: 5

  # ─── Backend API ──────────────────────────────
  backend:
    build: ./backend/notub
    deploy:
      replicas: 1
    depends_on:
      postgres:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}
      GOOGLE_CLIENT_SECRET: ${GOOGLE_CLIENT_SECRET}
      FRONTEND_URL: ${FRONTEND_URL}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: ${RABBITMQ_USER}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
    networks:
      - notub

  # ─── Microsserviço de Email ───────────────────
  servico-email:
    build: ./servico-email
    depends_on:
      rabbitmq:
        condition: service_healthy
    environment:
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: ${RABBITMQ_USER}
      RABBITMQ_PASSWORD: ${RABBITMQ_PASSWORD}
      MAILTRAP_USERNAME: ${MAILTRAP_USERNAME}
      MAILTRAP_PASSWORD: ${MAILTRAP_PASSWORD}
      FRONTEND_URL: ${FRONTEND_URL}
    networks:
      - notub

  # ─── Frontend SPA ─────────────────────────────
  frontend:
    build: ./frontend
    deploy:
      replicas: 1
    networks:
      - notub

  # ─── Reverse Proxy ────────────────────────────
  nginx:
    image: nginx:stable
    container_name: nginx
    entrypoint: ["/bin/sh", "-c"]
    command: ["sh /docker-entrypoint.d/99-certs.sh && /docker-entrypoint.sh nginx -g 'daemon off;'"]
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/generate-certs.sh:/docker-entrypoint.d/99-certs.sh
    depends_on:
      - frontend
      - backend
    networks:
      - notub

volumes:
  pgdata:
  rabbitdata:

networks:
  notub:
    driver: bridge
```

### 8.2 .env (atualizado)

```env
# ─── Base de Dados ────────────────────────────
POSTGRES_DB=notub
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_HOST_PORT=5433

# ─── JWT ──────────────────────────────────────
JWT_SECRET=u4qpwiBtWjdzvlKmYB8GpXP9/s82qWJcJvVnwstb8kh4n0OJUJY45y3ePSfj4J5BMmZ8kMwXItPjeu1NS++Rmw==

# ─── Google OAuth2 ────────────────────────────
GOOGLE_CLIENT_ID=o-seu-google-client-id
GOOGLE_CLIENT_SECRET=o-seu-google-client-secret
FRONTEND_URL=https://localhost

# ─── RabbitMQ ─────────────────────────────────
RABBITMQ_USER=notub
RABBITMQ_PASSWORD=notub123
RABBITMQ_HOST_PORT=5673
RABBITMQ_MGMT_PORT=15672

# ─── Mailtrap ─────────────────────────────────
MAILTRAP_USERNAME=o-seu-username-mailtrap
MAILTRAP_PASSWORD=a-sua-password-mailtrap
```

### 8.3 Dockerfile do Backend (inalterado)

```dockerfile
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
CMD java -jar target/*.jar
```

### 8.4 Dockerfile do Serviço de Email

```dockerfile
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw clean package -DskipTests
EXPOSE 8081
CMD java -jar target/*.jar
```

### 8.5 Dockerfile do Frontend (inalterado)

```dockerfile
FROM node:24.14.1-alpine AS build
WORKDIR /app
COPY NoTUB_Frontend/ .
RUN npm install
RUN npm run build

FROM nginx:stable
COPY --from=build /app/dist/spa /usr/share/nginx/html
COPY default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 8.6 Configuração Nginx (atualizada)

```nginx
events {
    worker_connections 2048;
}

worker_processes auto;

http {
    server_tokens off;

    open_file_cache max=1000 inactive=20s;
    open_file_cache_valid 30s;
    open_file_cache_min_uses 2;
    open_file_cache_errors on;

    upstream backend {
        server backend:8080;
    }

    upstream frontend {
        server frontend:80;
    }

    server {
        listen 80;
        server_name localhost;
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl;
        http2 on;
        server_name localhost;

        ssl_certificate /etc/nginx/certs/server.crt;
        ssl_certificate_key /etc/nginx/certs/server.key;

        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers off;

        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;

        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
        add_header X-Content-Type-Options nosniff always;
        add_header X-Frame-Options DENY always;
        add_header X-XSS-Protection "1; mode=block" always;

        # SPA
        location / {
            proxy_pass http://frontend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
        }

        # API
        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
        }

        # OAuth2 - iniciar fluxo
        location /oauth2/authorization/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
        }

        # OAuth2 - callback
        location /login/oauth2/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto https;
        }
    }
}
```

---

## 9. Exemplos de Código Chave

### 9.1 Configuração RabbitMQ (Backend - Produtor)

```java
package pt.notub.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_EXCHANGE = "notub.email.exchange";
    public static final String EMAIL_FILA = "notub.email.fila";
    public static final String ROUTING_KEY_UTILIZADOR_CRIADO = "UTILIZADOR_CRIADO";
    public static final String ROUTING_KEY_RECUPERACAO_PASSWORD = "RECUPERACAO_PASSWORD_PEDIDA";

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public Queue emailFila() {
        return QueueBuilder.durable(EMAIL_FILA).build();
    }

    @Bean
    public Binding bindingUtilizadorCriado(Queue emailFila, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailFila).to(emailExchange).with(ROUTING_KEY_UTILIZADOR_CRIADO);
    }

    @Bean
    public Binding bindingRecuperacaoPassword(Queue emailFila, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailFila).to(emailExchange).with(ROUTING_KEY_RECUPERACAO_PASSWORD);
    }
}
```

### 9.2 Publicador de Eventos (Backend)

```java
package pt.notub.services;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import pt.notub.config.RabbitMQConfig;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PublicadorEventosEmail {

    private final RabbitTemplate rabbitTemplate;

    public PublicadorEventosEmail(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarUtilizadorCriado(Long utilizadorId, String email,
                                          String primeiroNome, String ultimoNome) {
        Map<String, Object> evento = Map.of(
            "tipoEvento", "UTILIZADOR_CRIADO",
            "carimboTemporal", LocalDateTime.now().toString(),
            "dados", Map.of(
                "utilizadorId", utilizadorId,
                "email", email,
                "primeiroNome", primeiroNome,
                "ultimoNome", ultimoNome != null ? ultimoNome : ""
            )
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EMAIL_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_UTILIZADOR_CRIADO,
            evento
        );
    }

    public void publicarRecuperacaoPassword(Long utilizadorId, String email,
                                             String primeiroNome, String token,
                                             String urlRecuperacao) {
        Map<String, Object> evento = Map.of(
            "tipoEvento", "RECUPERACAO_PASSWORD_PEDIDA",
            "carimboTemporal", LocalDateTime.now().toString(),
            "dados", Map.of(
                "utilizadorId", utilizadorId,
                "email", email,
                "primeiroNome", primeiroNome != null ? primeiroNome : "",
                "tokenRecuperacao", token,
                "urlRecuperacao", urlRecuperacao
            )
        );
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EMAIL_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_RECUPERACAO_PASSWORD,
            evento
        );
    }
}
```

### 9.3 Consumidor RabbitMQ (Serviço de Email)

```java
package pt.notub.email.consumidor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.notub.email.config.RabbitMQConfig;
import pt.notub.email.servico.ServicoEnvioEmail;
import java.util.Map;

@Component
public class ConsumidorEventosEmail {

    private final ServicoEnvioEmail servicoEnvioEmail;

    public ConsumidorEventosEmail(ServicoEnvioEmail servicoEnvioEmail) {
        this.servicoEnvioEmail = servicoEnvioEmail;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_FILA)
    public void processarEventoEmail(Map<String, Object> evento) {
        String tipoEvento = (String) evento.get("tipoEvento");
        @SuppressWarnings("unchecked")
        Map<String, Object> dados = (Map<String, Object>) evento.get("dados");

        switch (tipoEvento) {
            case "UTILIZADOR_CRIADO" -> servicoEnvioEmail.enviarEmailBoasVindas(
                (String) dados.get("email"),
                (String) dados.get("primeiroNome"),
                (String) dados.get("ultimoNome")
            );
            case "RECUPERACAO_PASSWORD_PEDIDA" -> servicoEnvioEmail.enviarEmailRecuperacaoPassword(
                (String) dados.get("email"),
                (String) dados.get("primeiroNome"),
                (String) dados.get("urlRecuperacao")
            );
            default -> System.err.println("Tipo de evento desconhecido: " + tipoEvento);
        }
    }
}
```

### 9.4 Serviço de Envio de Email (Serviço de Email)

```java
package pt.notub.email.servico;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class ServicoEnvioEmail {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public ServicoEnvioEmail(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEmailBoasVindas(String destinatario, String primeiroNome, String ultimoNome) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("Bem-vindo ao NoTUB!");
            helper.setFrom("noreply@notub.pt");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h1 style="color: #1876d2;">NoTUB</h1>
                    <h2>Bem-vindo, %s!</h2>
                    <p>A sua conta foi criada com sucesso.</p>
                    <p>Com o NoTUB pode:</p>
                    <ul>
                        <li>Comprar bilhetes e passes</li>
                        <li>Acompanhar as suas viagens</li>
                        <li>Consultar rotas e horários</li>
                    </ul>
                    <a href="%s/home" style="background: #1876d2; color: white; padding: 12px 24px;
                       text-decoration: none; border-radius: 8px; display: inline-block;">
                       Começar
                    </a>
                    <hr style="margin-top: 30px;" />
                    <p style="color: #828282; font-size: 12px;">NoTUB - Transporte Público</p>
                </div>
                """.formatted(primeiroNome, frontendUrl);

            helper.setText(html, true);
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            System.err.println("Falha ao enviar email de boas-vindas: " + e.getMessage());
        }
    }

    public void enviarEmailRecuperacaoPassword(String destinatario, String primeiroNome,
                                                String urlRecuperacao) {
        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject("NoTUB - Recuperação de Palavra-passe");
            helper.setFrom("noreply@notub.pt");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h1 style="color: #1876d2;">NoTUB</h1>
                    <h2>Olá, %s</h2>
                    <p>Recebemos um pedido para redefinir a sua palavra-passe.</p>
                    <p>Clique no botão abaixo para definir uma nova palavra-passe:</p>
                    <a href="%s" style="background: #1876d2; color: white; padding: 12px 24px;
                       text-decoration: none; border-radius: 8px; display: inline-block;">
                       Redefinir Palavra-passe
                    </a>
                    <p style="color: #828282; margin-top: 20px;">
                       Este link expira em 30 minutos.
                    </p>
                    <p>Se não fez este pedido, ignore este email.</p>
                    <hr />
                    <p style="color: #828282; font-size: 12px;">NoTUB - Transporte Público</p>
                </div>
                """.formatted(primeiroNome != null ? primeiroNome : "Utilizador", urlRecuperacao);

            helper.setText(html, true);
            mailSender.send(mensagem);
        } catch (MessagingException e) {
            System.err.println("Falha ao enviar email de recuperação: " + e.getMessage());
        }
    }
}
```

### 9.5 AuthController - Esqueceu/Redefinir Password

```java
@PostMapping("/esqueceu-password")
public ResponseEntity<?> esqueceuPassword(@RequestBody PedidoEsqueceuPassword pedido) {
    utilizadorRepository.findByEmail(pedido.getEmail()).ifPresent(utilizador -> {
        TokenRecuperacaoSenha token = new TokenRecuperacaoSenha();
        token.setUtilizador(utilizador);
        token.setToken(UUID.randomUUID().toString());
        token.setDataExpiracao(LocalDateTime.now().plusMinutes(30));
        token.setUtilizado(false);
        tokenRecuperacaoSenhaRepository.save(token);

        String urlRecuperacao = frontendUrl + "/redefinir-password?token=" + token.getToken();
        publicadorEventosEmail.publicarRecuperacaoPassword(
            utilizador.getId(), utilizador.getEmail(), utilizador.getPrimeiroNome(),
            token.getToken(), urlRecuperacao
        );
    });
    return ResponseEntity.ok("Se o email existir, foi enviado um link de recuperação.");
}

@PostMapping("/redefinir-password")
public ResponseEntity<?> redefinirPassword(@RequestBody PedidoRedefinirPassword pedido) {
    TokenRecuperacaoSenha token = tokenRecuperacaoSenhaRepository
        .findByTokenAndUtilizadoFalse(pedido.getToken())
        .orElseThrow(() -> new RuntimeException("Token inválido ou expirado"));

    if (token.getDataExpiracao().isBefore(LocalDateTime.now())) {
        return ResponseEntity.badRequest().body("Token de recuperação expirado");
    }

    Utilizador utilizador = token.getUtilizador();
    utilizador.setPassword(passwordEncoder.encode(pedido.getNovaPassword()));
    utilizadorRepository.save(utilizador);

    token.setUtilizado(true);
    tokenRecuperacaoSenhaRepository.save(token);

    return ResponseEntity.ok("Palavra-passe redefinida com sucesso");
}
```

### 9.6 Serviço de Pontos

```java
package pt.notub.services;

import org.springframework.stereotype.Service;
import pt.notub.models.*;
import pt.notub.repositories.*;
import java.time.LocalDateTime;

@Service
public class ServicoPontos {

    private final UtilizadorRepository utilizadorRepository;
    private final HistoricoPontosRepository historicoPontosRepository;

    private static final int PONTOS_POR_VIAGEM = 10;
    private static final int PONTOS_POR_COMPRA = 5;

    public ServicoPontos(UtilizadorRepository utilizadorRepository,
                         HistoricoPontosRepository historicoPontosRepository) {
        this.utilizadorRepository = utilizadorRepository;
        this.historicoPontosRepository = historicoPontosRepository;
    }

    public void atribuirPontosViagem(Long utilizadorId) {
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
            .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        utilizador.setNrPontos(utilizador.getNrPontos() + PONTOS_POR_VIAGEM);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, PONTOS_POR_VIAGEM, "VIAGEM",
            "Pontos ganhos por viagem concluída");
    }

    public void atribuirPontosCompra(Long utilizadorId) {
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
            .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        utilizador.setNrPontos(utilizador.getNrPontos() + PONTOS_POR_COMPRA);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, PONTOS_POR_COMPRA, "COMPRA",
            "Pontos ganhos por compra");
    }

    public void utilizarPontos(Long utilizadorId, int pontos, String descricao) {
        Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
            .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        if (utilizador.getNrPontos() < pontos) {
            throw new RuntimeException("Pontos insuficientes");
        }
        utilizador.setNrPontos(utilizador.getNrPontos() - pontos);
        utilizadorRepository.save(utilizador);
        registarHistorico(utilizador, -pontos, "RESGATE", descricao);
    }

    private void registarHistorico(Utilizador utilizador, int pontos,
                                    String tipo, String descricao) {
        HistoricoPontos hp = new HistoricoPontos();
        hp.setUtilizador(utilizador);
        hp.setPontos(pontos);
        hp.setTipo(tipo);
        hp.setDescricao(descricao);
        hp.setDataHora(LocalDateTime.now());
        historicoPontosRepository.save(hp);
    }
}
```

### 9.7 Cálculo de Tarifa (baseado em zonas)

```java
// Em ServicoTarifa.java
public Optional<Tarifa> calcularTarifa(TipoUtilizador tipoUtilizador,
                                        ModalidadePasse modalidade,
                                        int nrZonas) {
    if (tipoUtilizador == null) {
        // Preço de bilhete: tipoUtilizador é null na tabela de tarifas
        return tarifaRepository.findByTipoUtilizadorIsNullAndModalidadeAndNrZonas(
            modalidade, nrZonas);
    }
    return tarifaRepository.findByTipoUtilizadorAndModalidadeAndNrZonas(
        tipoUtilizador, modalidade, nrZonas);
}
```

### 9.8 Handler Global de Exceções

```java
package pt.notub.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> tratarRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            "carimboTemporal", LocalDateTime.now().toString(),
            "status", 400,
            "erro", "Pedido Inválido",
            "mensagem", ex.getMessage()
        ));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "carimboTemporal", LocalDateTime.now().toString(),
            "status", 404,
            "erro", "Não Encontrado",
            "mensagem", ex.getMessage()
        ));
    }
}
```

### 9.9 Entidade Papel + UserDetailsImpl

```java
package pt.notub.models;

import jakarta.persistence.*;

@Entity
public class Papel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoPapel nome;

    public Papel() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoPapel getNome() { return nome; }
    public void setNome(TipoPapel nome) { this.nome = nome; }
}
```

```java
// security/UserDetailsImpl.java (atualizado)
// No método getAuthorities():
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return papeis.stream()
        .map(papel -> new SimpleGrantedAuthority("PAPEL_" + papel.getNome().name()))
        .collect(Collectors.toList());
}
```

### 9.10 pom.xml do Serviço de Email

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.5</version>
    </parent>

    <groupId>pt.notub</groupId>
    <artifactId>servico-email</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>servico-email</name>

    <properties>
        <java.version>25</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 9.11 application.yml do Serviço de Email

```yaml
server:
  port: 8081

spring:
  application:
    name: servico-email
  rabbitmq:
    host: ${RABBITMQ_HOST:rabbitmq}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:notub}
    password: ${RABBITMQ_PASSWORD:notub123}
  mail:
    host: sandbox.smtp.mailtrap.io
    port: 587
    username: ${MAILTRAP_USERNAME}
    password: ${MAILTRAP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

app:
  frontend-url: ${FRONTEND_URL:https://localhost}

rabbitmq:
  fila: notub.email.fila
  exchange: notub.email.exchange
```

### 9.12 Adições ao pom.xml do Backend

```xml
<!-- Adicionar ao pom.xml existente do backend -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 9.13 Adições ao application.properties do Backend

```properties
# Configuração RabbitMQ
spring.rabbitmq.host=${RABBITMQ_HOST:rabbitmq}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:notub}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:notub123}
```

---

## 10. Escalabilidade e Melhorias

### 10.1 Curto Prazo (Próximo Sprint)

- **Migrações de BD**: Substituir `ddl-auto=update` por Flyway ou Liquibase para mudanças de schema versionadas
- **Validação de input**: Adicionar anotações `@Valid` + Bean Validation em todos os DTOs
- **Paginação**: Adicionar `Pageable` a todos os endpoints de listagem
- **Cache**: Adicionar Redis para cache de linhas/paragens/tarifas (raramente mudam)
- **Limitação de taxa**: Adicionar rate limiting nos endpoints de autenticação (login, registar, esqueceu-password)
- **Testes**: Testes unitários para serviços, testes de integração para controladores

### 10.2 Médio Prazo

- **Versionamento da API**: Adicionar prefixo `/api/v1/` para compatibilidade retroativa
- **WebSocket**: Atualizações de localização de veículos em tempo real via STOMP sobre WebSocket
- **Upload de ficheiros**: Fotos de perfil, upload de documentos para verificação de estudante/sénior
- **Notificações push**: Via Firebase Cloud Messaging para PWA mobile
- **Registo de auditoria**: Registar todas as ações de administrador com entidade RegistoAuditoria
- **Health checks**: Endpoints Spring Actuator para monitorização

### 10.3 Longo Prazo

- **Service discovery**: Eureka ou Consul se forem adicionados mais microsserviços
- **API Gateway**: Substituir proxy nginx por Spring Cloud Gateway para encaminhamento inteligente
- **Circuit breaker**: Resilience4j para tolerância a falhas do serviço de email
- **Observabilidade**: Prometheus + Grafana para métricas, ELK para logs
- **CI/CD**: Pipeline GitHub Actions para testes e deployment automatizados
- **Kubernetes**: Migrar de Docker Compose para K8s para orquestração em produção
- **Base de Dados**: Réplicas de leitura, otimização de connection pooling (HikariCP)
- **CDN**: Servir assets estáticos do frontend via CloudFlare ou similar

### 10.4 Reforço de Segurança

- **Apenas HTTPS**: Redirecionar todo o HTTP para HTTPS (já na config nginx)
- **CORS**: Restringir ao domínio específico do frontend em produção
- **Refresh tokens JWT**: Implementar rotação de refresh tokens para melhor segurança
- **Política de passwords**: Impor comprimento mínimo, complexidade, verificação de breach
- **CSRF**: Embora JWT seja stateless, adicionar proteção CSRF para fluxos OAuth2
- **Injeção SQL**: Usar queries parametrizadas (JPA já trata disto)
- **XSS**: Sanitizar todo o input do utilizador, usar headers Content-Security-Policy
- **Gestão de segredos**: Usar HashiCorp Vault ou Docker secrets em vez de .env

---

## Apêndice A: Checklist de Migração (Código Existente → Nova Spec)

### Renomeações no Backend

| Atual (Código) | Novo (Spec/PIM) | Ficheiros afetados |
|-----------------|------------------|-------------------|
| Carreira | Linha | Modelo, Repositório, Serviço, Controlador |
| SequenciaParagem | PontosDePassagem | Modelo, Repositório, Serviço |
| TipoPerfil | TipoUtilizador | Enum, modelo Utilizador, referências |
| EM_CURSO | ATIVA | Enum EstadoViagem |
| CONCLUIDA | CONCLUIDA | Enum EstadoViagem (mantém-se) |
| EstadoPagamento valores | EM_CURSO, CANCELADO, CONCLUIDO, REJEITADO | Enum (mantém-se) |

### Novas Entidades no Backend

| Entidade | Estado |
|----------|--------|
| Papel | NOVA - criar modelo, repositório |
| TokenRecuperacaoSenha | NOVA - criar modelo, repositório |
| HistoricoPontos | NOVA - criar modelo, repositório |
| nrPontos no Utilizador | ADICIONAR campo |
| nrZonas na Tarifa | ADICIONAR campo |
| num na Zona | ADICIONAR campo |
| utilizador na Transacao | ADICIONAR relação |

### Novos Controladores/Serviços no Backend

| Componente | Estado |
|-----------|--------|
| ValidacaoController | NOVO |
| PontosController | NOVO |
| AdminRedeController | NOVO |
| AdminZonaController | NOVO |
| AdminTarifaController | NOVO |
| AdminVeiculoController | NOVO |
| AdminViagemController | NOVO |
| AdminUtilizadorController | NOVO |
| AdminStatsController | NOVO |
| PublicadorEventosEmail | NOVO |
| ServicoPontos | NOVO |
| ServicoAuth (esqueceu/redefinir) | NOVO |
| GlobalExceptionHandler | NOVO |

### Novas Páginas no Frontend

| Página | Estado |
|--------|--------|
| PaginaEsqueceuPassword | NOVA |
| PaginaRedefinirPassword | NOVA |
| PaginaLinhas | NOVA |
| PaginaDetalheLinha | NOVA |
| PaginaZonas | NOVA |
| LayoutAdmin | NOVO |
| Todas as páginas /admin/* | NOVAS (10 páginas) |
| PaginaBilhetes | EXPANDIR de placeholder |
| PaginaViagens | EXPANDIR de placeholder |
| PaginaInicial | ATUALIZAR com dados dinâmicos |

### Novidades na Infraestrutura

| Componente | Estado |
|-----------|--------|
| RabbitMQ | ADICIONAR ao docker-compose |
| servico-email | CRIAR serviço completo |
| Atualizações .env | ADICIONAR vars RabbitMQ + Mailtrap |
| nginx.conf | ADICIONAR redireção HTTP→HTTPS |

---

## Apêndice B: Configuração Inicial de Papéis

Quando a aplicação arranca, garantir que estes papéis existem na base de dados:

```java
@Bean
CommandLineRunner initPapeis(PapelRepository papelRepository) {
    return args -> {
        if (papelRepository.count() == 0) {
            Papel utilizador = new Papel();
            utilizador.setNome(TipoPapel.UTILIZADOR);
            papelRepository.save(utilizador);

            Papel administrador = new Papel();
            administrador.setNome(TipoPapel.ADMINISTRADOR);
            papelRepository.save(administrador);
        }
    };
}
```

No registo, todos os utilizadores recebem o papel `UTILIZADOR` por padrão.
Administradores podem promover utilizadores via `PUT /api/utilizadores/{id}/papeis`.
