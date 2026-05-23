# NoTUB

## TODO
- Fazer todo o sistema de paragens, rotas,linhas etc a funcionar
- Pagamentos a funcionar(melhorar) so que o callback, o que retorna do sucesso do stripe, é um true e transactionid que não é ideal e usa o ip localhost que pode dar problemas a testar no telemovel. Ideal era webhooks mas precisam de IPs fixos (talvez maquinas dos profs meias más)
- Fazer parte de inciar viagem no autocarro real ao ler o qr code.
- Dá para adicionar a app como PWA no telemovel. Atualizar o logo no manifest.json para ser o da NoTUB.
- Ver se o forget password está a funcionar. (Welcome email (Mailtrap) funciona na sandbox) nas APIS substituir pelas vossas credenciais de preferencia.
- TicketController bypasses payment — POST /api/tickets/comprar and /passe/comprar create titles directly, no transacaoId required
Pode faltar implementar mais coisas é verr conforme

Este projeto corre sobre uma stack que utiliza **Vue/Quasar** no frontend, **Spring Boot**
no backend e **PostgreSQL** para a base de dados, tudo orquestrado através de
**Docker Compose** e protegido por um reverse proxy **Nginx** com SSL auto-gerado que pode ser
facilmente usado um certificado SSL seguro como os do "Let's Encrypt".

## Destaques da Arquitetura

- **HTTPS:** O ambiente cria automaticamente um certificado SSL auto-assinado no arranque através do script `generate-certs.sh`.
- **Serviço de Frontend Otimizado:** O frontend utiliza um Dockerfile que compila a aplicação e serve os ficheiros através de um container nginx.
- **Configuração Parametrizada:** As credenciais da base de dados e estados principais da aplicação são geridos através de variáveis no ficheiro `.env`
que como utilizamos docker são injetadas nas variáveis de ambiente e como o Postgres e o backend procuram por essas variaveis de ambiente fica mais fácil de configurar.
O spring boot
- **Escalar:** Através do docker compose podemos definir o numero de replicas e o nginx funciona como LB também (como reverse proxy tambem) para distribuir pelas replicas. 

## Como Executar

### 1. Verificar portas
Garante que as portas `443` (https) e `5433` (mapeamento do PostgreSQL) estão livres no seu sistema.

> Nota: Se já tiveres uma instalação local do PostgreSQL na porta `5432`, podes usar a porta `5433` definida no `.env` para evitar conflitos com a base de dados em Docker.

Criar o .env na root do projeto com:

```bash
# Database configuration
# Database configuration
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/notub

POSTGRES_DB=notub
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_HOST_PORT=5433

## JWT Secret
JWT_SECRET=u4qpwiBtWjdzvlKmYB8GpXP9/s82qWJcJvVnwstb8kh4n0OJUJY45y3ePSfj4J5BMmZ8kMwXItPjeu1NS++Rmw==
# Map to a different host port to avoid conflict with local postgres in my computer
POSTGRES_HOST_PORT=5433

## Gerem na vossa conta google cloud e configurem o URI redirect correto
# Google OAuth2 Config
GOOGLE_CLIENT_ID=781084617249-gjpk3dpmig6vpmv8rjek438a6mucca63.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=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
FRONTEND_URL=https://localhost

# RabbitMQ
RABBITMQ_USERNAME=notub
RABBITMQ_PASSWORD=notub123
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672

# Stripe
STRIPE_SECRET_KEY=sk_test_51TITKwE1nGtEv7WfxiOuR1qPFgAVzScitXIL3BIv8FoD7DNAAQmNmNet9xMVu1QCPaY0UpV2lwWwbEactnUWNh8g00xkTBGA36
STRIPE_PUBLISHABLE_KEY=pk_test_51TITKwE1nGtEv7Wf1PfPStINwgabTLHXp18Tgdi3Q3XyxeRDPNLiXmMITtADzSsby7fyljxNRB12BdmV4xaSXSuj00kkaLvNle

```

Criar o .env no microserviço servico-email:

```bash
# Mailtrap SMTP
MAILTRAP_USERNAME=053f7b83c94104
MAILTRAP_PASSWORD=d90cc70e66ed89

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=notub
RABBITMQ_PASSWORD=notub123


```
### 2. Arranque do sistema

Reconstrói e inicia toda a stack em modo detached:

```bash
docker-compose up --build -d
```

Reconstrói e inicia toda sem detached para ver o que está a ocorrer nos containers:

```bash
docker-compose up --build
```

Parar e remover containers e apagar volumes
```bash
docker-compose down -v
```

### 3. Acessar

Usar browser e acessar:
```bash
https://localhost
```

Endpoint api teste:
```bash
https://localhost/api/test
```

Para emular no Android Studio app(estar dentro da root do frotend):

Depois de ter feito dentro (NoTUB_Frontend) :
```bash
npm i
```
```bash
npx cap add android
```


### Se quiser capacitor
Nao def. se capacitor ou full quasar

```bash
npm run dev -- -m capacitor -T android
```

Usar pt.notub depois do comando anterior

Escolher primeiro ip

Correr build gradle no Android studio. (Project: android)


