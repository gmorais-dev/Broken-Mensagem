# API Mensagem - Mensageria de Fretes

## Visao geral

A `apiMensagem` e uma aplicacao Spring Boot responsavel por receber eventos de frete do sistema legado, validar o contrato recebido, controlar idempotencia em banco, publicar os eventos no RabbitMQ e notificar dashboards conectados por WebSocket/STOMP.

No ecossistema do projeto, esta API ocupa o papel de integracao entre:

- legado operacional
- mensageria assicrona
- dashboard em tempo real

## Arquitetura tecnica

O projeto segue a seguinte composicao:

- `Spring Web MVC` para entrada HTTP
- `Bean Validation` para validar payloads
- `Spring Data JPA` para persistencia de idempotencia
- `RabbitMQ` para redistribuicao de eventos
- `Spring WebSocket/STOMP` para notificacao em tempo real
- `PostgreSQL` como banco principal em runtime
- `H2` para os testes automatizados

Fluxo principal:

1. O legado faz `POST` para `/api/mensageria/eventos`.
2. A API valida o payload e o tipo de evento.
3. A API gera a chave de idempotencia.
4. A API consulta a tabela `evento_processado`.
5. Se o evento for novo, registra como `PROCESSANDO`.
6. Publica o evento no RabbitMQ.
7. Notifica o dashboard em `/topic/fretes`.
8. Atualiza o registro para `PROCESSADO`.
9. Se houver falha, atualiza o registro para `ERRO`.

## Papel desta API no ecossistema

O legado continua sendo a fonte de verdade do frete e do outbox `evento_sistema`.

Esta API nao grava frete nem ocorrencia operacional. Ela:

- recebe o payload pronto do legado
- protege contra processamento duplicado
- redistribui o evento para mensageria e dashboard
- devolve resposta HTTP para o produtor legado

## Estrutura do projeto

Pacotes principais:

- `config`: RabbitMQ, WebSocket e properties tipadas
- `controller`: endpoints REST de evento unico e lote
- `dto`: contratos de request e response
- `entity`: entidade de controle de idempotencia
- `exception`: excecoes de validacao, duplicidade e publicacao
- `handler`: tratamento global de erros HTTP
- `repository`: acesso JPA a `evento_processado`
- `service`: regra principal de mensageria, idempotencia, RabbitMQ e WebSocket

Arquivos centrais:

- `src/main/java/Mensageria_frete/apiMensagem/controller/MensageriaEventoController.java`
- `src/main/java/Mensageria_frete/apiMensagem/controller/MensageriaLoteController.java`
- `src/main/java/Mensageria_frete/apiMensagem/service/MensageriaEventoService.java`
- `src/main/java/Mensageria_frete/apiMensagem/service/IdempotenciaEventoService.java`
- `src/main/java/Mensageria_frete/apiMensagem/service/RabbitPublisherService.java`
- `src/main/java/Mensageria_frete/apiMensagem/service/DashboardNotifierService.java`

## Eventos suportados

Eventos aceitos hoje:

- `FRETE_CRIADO`
- `FRETE_SAIDA_CONFIRMADA`
- `FRETE_EM_TRANSITO`
- `FRETE_ENTREGUE`
- `FRETE_NAO_ENTREGUE`
- `FRETE_CANCELADO`
- `OCORRENCIA_FRETE_REGISTRADA`

Qualquer valor fora dessa lista gera `400 Bad Request`.

## Contrato de entrada

O contrato principal esta em `EventoFreteRequest`.

Campos do evento:

- `versao`
- `evento`
- `origem`
- `endpointMensageria`
- `mensageriaHabilitada`
- `dataEvento`
- `frete`
- `ocorrencia` opcional

### Bloco `frete`

Campos modelados:

- `id`
- `numero`
- `status`
- `idRemetente`
- `idDestinatario`
- `idMotorista`
- `idVeiculo`
- `origem`
- `destino`
- `pesoKg`
- `valorTotal`

### Bloco `ocorrencia`

O bloco `ocorrencia` e opcional e pode acompanhar:

- `OCORRENCIA_FRETE_REGISTRADA`
- `FRETE_SAIDA_CONFIRMADA`
- `FRETE_EM_TRANSITO`
- `FRETE_ENTREGUE`

Campos modelados:

- `tipo`
- `descricaoTipo`
- `dataHora`
- `municipio`
- `uf`
- `descricao`
- `nomeRecebedor`
- `documentoRecebedor`

Exemplo de payload:

```json
{
  "versao": "1.0",
  "evento": "OCORRENCIA_FRETE_REGISTRADA",
  "origem": "SISTEMA_FRETES_WEB",
  "endpointMensageria": "http://localhost:8082/api/mensageria/eventos",
  "mensageriaHabilitada": true,
  "dataEvento": "2026-05-10T14:37:12.345678901",
  "frete": {
    "id": 12,
    "numero": "FRT-2026-00012",
    "status": "EM_TRANSITO"
  },
  "ocorrencia": {
    "tipo": "AVARIA",
    "descricaoTipo": "Avaria",
    "dataHora": "2026-05-10T14:35:00",
    "municipio": "Fortaleza",
    "uf": "CE",
    "descricao": "Embalagem lateral danificada.",
    "nomeRecebedor": "",
    "documentoRecebedor": ""
  }
}
```

## Endpoints HTTP

### `POST /api/mensageria/eventos`

Recebe um unico evento e retorna `202 Accepted` quando o processamento e aceito.

Respostas relevantes:

- `202`: evento aceito
- `400`: payload invalido ou evento fora do contrato
- `409`: evento duplicado ja processado
- `500`: falha de publicacao no RabbitMQ

### `POST /api/mensageria/lotes`

Recebe uma lista de eventos e aplica a mesma regra do endpoint individual para cada item.

## Idempotencia persistida

A API usa a tabela `evento_processado` para controlar duplicidade.

Chave atual:

```text
evento + ":" + frete.id + ":" + dataEvento
```

Estados de processamento:

- `PROCESSANDO`
- `PROCESSADO`
- `ERRO`

Regras:

- `PROCESSADO`: bloqueia republicacao
- `PROCESSANDO`: bloqueia novo processamento concorrente
- `ERRO`: permite retentativa controlada

Observacao:

- a chave atual e operacional e compativel com o legado
- ela ainda nao usa um identificador unico do `evento_sistema`
- se o legado passar a enviar esse identificador no payload, ele deve substituir ou compor a chave de idempotencia

## RabbitMQ

Recursos declarados pela aplicacao:

- exchange direta: `fretes.exchange`
- fila duravel: `fretes.eventos.queue`
- routing key: `fretes.eventos`

O publicador usa `JacksonJsonMessageConverter`, entao o objeto `EventoFreteRequest` e serializado como JSON no envio ao broker.

## WebSocket/STOMP

Configuracao atual:

- endpoint: `/ws-fretes`
- broker simples: `/topic`
- topico de fretes: `/topic/fretes`
- application prefix: `/app`

Depois da publicacao no RabbitMQ, a API tambem envia o evento para dashboards conectados ao topico `/topic/fretes`.

## Configuracao

Configuracoes principais em `src/main/resources/application.yaml`:

```yaml
server:
  port: 8082

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/postgres}
    driver-class-name: org.postgresql.Driver
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:1234}
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

app:
  mensageria:
    rabbit:
      exchange: fretes.exchange
      queue: fretes.eventos.queue
      routing-key: fretes.eventos
    websocket:
      topic-fretes: /topic/fretes
```

## Como executar

Pre-requisitos:

- Java 21
- PostgreSQL ativo
- RabbitMQ ativo em `localhost:5672`

Executar a aplicacao:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw spring-boot:run
```

Executar os testes:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test
```

## Fluxo de integracao ponta a ponta

1. O legado grava o evento no outbox `evento_sistema`.
2. O legado envia o `payload` bruto para esta API.
3. A API valida o contrato.
4. A API registra idempotencia em `evento_processado`.
5. A API publica o evento no RabbitMQ.
6. A API notifica dashboards por STOMP/WebSocket.
7. O dashboard recebe o evento e decide se reidrata os dados no legado.

## Testes automatizados

Os testes atuais cobrem:

- processamento de evento valido
- bloqueio de evento duplicado
- retentativa apos falha de publicacao
- rejeicao de evento fora do contrato
- aceite de `OCORRENCIA_FRETE_REGISTRADA`
- aceite de bloco opcional `ocorrencia` em outro evento de frete
- transicao atomica de retentativa em idempotencia
- subida do contexto Spring

## Observacoes finais

- esta API nao substitui o legado; ela complementa o fluxo de integracao
- o contrato de mensageria e mais rico que o minimo necessario para o dashboard
- a idempotencia persistida e parte central da arquitetura
- respostas `409` para duplicados precisam ser tratadas corretamente pelo produtor legado para nao prender o outbox em retentativas desnecessarias
