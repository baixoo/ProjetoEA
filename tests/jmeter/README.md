# Testes de Carga - NoTUB

## Requisitos

- [Apache JMeter 5.6+](https://jmeter.apache.org/download_jmeter.cgi)
- Java 17+

## Plano de Teste

O ficheiro `notub_load_test.jmx` contem 5 grupos de threads (patamares):

| # | Grupo | Threads | Ramp-up | Loops | Endpoint | Cenario |
|---|-------|---------|---------|-------|----------|---------|
| 1 | Autenticacao | 50 | 10s | 10 | `POST /api/auth/login` | 500 logins consecutivos |
| 2 | Consulta Paragens | 100 | 15s | 20 | `GET /api/network/*` | 2000 requests publicos (paragens, linhas, trajetos) |
| 3 | Consulta Viagens | 30 | 10s | 15 | `GET /api/viagens/*` | 450 requests autenticados |
| 4 | Validacao Pico | 200 | 5s | 5 | `POST /api/validacao/1` | Pico de 1000 validacoes simultaneas |
| 5 | Planeamento Rota | 50 | 10s | 10 | `GET /api/network/route` | 500 planeamentos de rota |

### Parametrizacao

Variaveis definidas no Test Plan:
- `HOST` - Dominio ou IP (default: `localhost`)
- `PORT` - Porto (default: `443`)
- `PROTOCOL` - Protocolo (default: `https`)

Para testar localmente com HTTP: mudar `PORT` para `80` e `PROTOCOL` para `http`.

### Credenciais de teste

O plano usa `admin@notub.pt` / `admin123` (conta default criada pelo `DataInitializer`).

## Como Executar

### GUI (recomendado para analise visual)

```bash
cd tests/jmeter
jmeter -t notub_load_test.jmx
```

### CLI (non-GUI, para CI/servidor)

```bash
mkdir -p results
jmeter -n -t notub_load_test.jmx -l results/notub_results.csv -e -o results/report
```

### Com parametros customizados

```bash
jmeter -n -t notub_load_test.jmx \
  -JHOST=notub.bounceme.net \
  -JPORT=443 \
  -JPROTOCOL=https \
  -l results/notub_prod.csv \
  -e -o results/report_prod
```

## Metricas a Analisar

1. **Throughput** (requests/segundo) - Deve ser > 100 req/s para endpoints publicos
2. **Tempo de resposta medio** - Deve ser < 500ms para 95% das requests
3. **Taxa de erro** - Deve ser < 1%
4. **P99 latency** - Tempo de resposta no percentil 99

## Resultados

Os resultados sao guardados em `results/notub_load_test_results.csv` (configurado no Summary Report).
