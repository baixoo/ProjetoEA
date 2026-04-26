# ProjetoEA

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
POSTGRES_DB=projetoea
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Map to a different host port to avoid conflict with local postgres in my computer
POSTGRES_HOST_PORT=5433

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

Depois de ter feito dentro (ProjetoEA_Frontend) :
```bash
npm i
```
```bash
npx cap add android
```

```bash
npm run dev -- -m capacitor -T android
```

Usar pt.projetoea depois do comando anterior

Escolher primeiro ip

Correr build gradle no Android studio. (Project: android)


