# Guia Para o Agente no IntelliJ - API de Mensageria

## Objetivo deste guia

Este guia existe para dar contexto completo ao agente que vai trabalhar em outra IDE, fora deste repositório legado, criando a API Spring Boot de mensageria que vai receber, publicar e retransmitir eventos de frete.

O agente deve assumir que:

- o sistema legado atual ja produz eventos de frete;
- o legado e o producer inicial;
- a API Spring Boot sera o servico de mensageria consumidor desses eventos HTTP;
- essa API sera responsavel por publicar em broker e repassar para WebSocket/dashboard.

Este documento descreve:

- como o legado funciona hoje;
- o que ja esta pronto no producer;
- o que falta construir na API nova;
- quais classes precisam existir;
- qual contrato a API deve atender;
- quais cuidados de compatibilidade devem ser respeitados.

---

## 1. Contexto do sistema atual

O projeto atual e um sistema legado Java EE MVC classico, com esta separacao:

- `JSP + JavaScript` na view
- `Servlet Controller` para receber a requisicao
- `BO` com regra de negocio
- `DAO` com JDBC direto no PostgreSQL

Regra importante do legado:

- o frete e a origem de negocio;
- quando um frete e criado ou muda de status, o sistema gera um evento;
- esse evento e persistido em banco junto com a transacao do frete;
- a publicacao externa da mensageria ainda nao esta implementada no legado;
- portanto o legado hoje funciona como `producer/outbox producer`, nao como publisher final.

---

## 2. O que o legado ja faz hoje

### 2.1 Cria frete e registra evento no mesmo commit

No BO de frete, quando um frete e cadastrado, o sistema:

1. gera o numero do frete;
2. valida motorista, veiculo, remetente, destinatario e dados de carga;
3. cria um `EventoSistema`;
4. grava frete e evento na mesma transacao.

Referencia:

- `src/main/java/BO/FreteBO.java`
- `src/main/java/DAO/FreteDAO.java`

### 2.2 Cria evento em mudanca de status

Quando o frete muda de status, o legado:

1. valida a transicao permitida;
2. atualiza o frete;
3. atualiza o status do veiculo;
4. cria um novo evento;
5. grava tudo no mesmo commit.

Status atualmente suportados:

- `EMITIDO`
- `SAIDA_CONFIRMADA`
- `EM_TRANSITO`
- `ENTREGUE`
- `NAO_ENTREGUE`
- `CANCELADO`

Tipos de evento atualmente produzidos:

- `FRETE_CRIADO`
- `FRETE_SAIDA_CONFIRMADA`
- `FRETE_EM_TRANSITO`
- `FRETE_ENTREGUE`
- `FRETE_NAO_ENTREGUE`
- `FRETE_CANCELADO`

### 2.3 Usa tabela de outbox

O legado ja possui a tabela `evento_sistema`, usada como outbox transacional.

Estrutura logica:

- `id`
- `tipo`
- `entidade`
- `entidade_id`
- `payload`
- `status`
- `tentativas`
- `data_criacao`
- `data_publicacao`
- `mensagem_erro`

Status inicial do evento:

- `PENDENTE`

Isso significa que o legado ja garante o mais importante:

- se o frete foi confirmado em banco, o evento correspondente tambem foi salvo;
- nao existe risco de salvar frete e perder o evento dentro da mesma transacao.

---

## 3. Papel do legado como producer

O agente deve entender o legado como `producer funcional de eventos de negocio`, mas ainda sem entrega externa automatica.

### Producer legado atual

O producer legado:

- detecta acontecimentos de negocio;
- constroi o payload do evento;
- grava o evento no outbox `evento_sistema`;
- parametriza a futura URL da API de mensageria;
- marca o evento como `PENDENTE`.

### O que ele ainda nao faz

O producer legado ainda nao:

- consulta eventos pendentes para envio;
- monta lote HTTP;
- faz `POST` para API Spring Boot;
- atualiza `evento_sistema` para `ENVIADO`, `ERRO` ou `PROCESSANDO`;
- faz retry automatico.

Em outras palavras:

- o legado hoje e `producer + outbox`;
- a API Spring Boot nova sera `consumer HTTP + publisher de broker + broadcaster WebSocket`.

---

## 4. Configuracao de mensageria ja existente no legado

O legado ja possui configuracao parametrizada:

- `mensageria.habilitada`
- `mensageria.api.baseUrl`
- `mensageria.api.eventosPath`
- `mensageria.origem`

Valores padrao atuais:

- `mensageria.habilitada=false`
- `mensageria.api.baseUrl=http://localhost:8082`
- `mensageria.api.eventosPath=/api/mensageria/eventos`
- `mensageria.origem=SISTEMA_FRETES_WEB`

Observacao importante:

- a API nova deve, por padrao, subir na porta `8082`, para casar com o legado sem exigir mudanca imediata.

---

## 5. Contrato que a API nova precisa respeitar

O payload do legado e montado manualmente no `FreteBO`. A API Spring Boot deve estar preparada para receber um JSON no formato aproximado abaixo:

```json
{
  "versao": "1.0",
  "evento": "FRETE_CRIADO",
  "origem": "SISTEMA_FRETES_WEB",
  "endpointMensageria": "http://localhost:8082/api/mensageria/eventos",
  "mensageriaHabilitada": false,
  "dataEvento": "2026-05-02T10:30:00",
  "frete": {
    "id": 12,
    "numero": "FRT-2026-00012",
    "status": "EMITIDO",
    "idRemetente": 1,
    "idDestinatario": 2,
    "idMotorista": 3,
    "idVeiculo": 4,
    "origem": "Campinas/SP",
    "destino": "Curitiba/PR",
    "pesoKg": 9900.0,
    "valorTotal": 9900.0
  }
}
```

Ponto importante:

- o legado envia um evento por payload;
- a API pode receber esse evento unitario e internamente agrupar/publicar;
- se o agente quiser suportar lote tambem, pode criar um endpoint adicional de lote, mas sem quebrar o endpoint unitario do legado.

Contrato minimo recomendado:

- `POST /api/mensageria/eventos`

Contrato opcional recomendado:

- `POST /api/mensageria/lotes`

Resposta HTTP recomendada:

- `202 Accepted` quando o evento for aceito para processamento;
- `400 Bad Request` quando o payload estiver invalido;
- `409 Conflict` quando o evento ja tiver sido processado e a API tratar idempotencia;
- `500` apenas em falhas internas reais.

---

## 6. O que a API Spring Boot precisa construir

A API nova precisa cobrir 4 responsabilidades:

### 6.1 Receber evento do legado

- expor endpoint REST;
- validar payload;
- registrar logs tecnicos;
- responder rapidamente para o producer.

### 6.2 Publicar em broker

Broker recomendado:

- RabbitMQ

Responsabilidade:

- publicar o evento recebido em exchange/fila;
- manter chave de roteamento previsivel;
- isolar a logica de publicacao em um servico proprio.

### 6.3 Repassar para WebSocket

Responsabilidade:

- notificar o dashboard em tempo real;
- expor endpoint WebSocket/STOMP, por exemplo:
    - `/ws-fretes`
    - `/topic/fretes`

### 6.4 Garantir resiliencia basica

Responsabilidade:

- tratar duplicidade de evento;
- permitir reprocessamento controlado;
- manter padrao de idempotencia por `evento + frete.id + dataEvento`, ou chave equivalente;
- registrar falhas de publicacao.

Se houver persistencia na API nova, ela deve guardar pelo menos:

- identificador logico do evento;
- payload bruto;
- status de processamento;
- data de recebimento;
- erro de publicacao, quando existir.

---

## 7. Dependencias recomendadas no Spring Initializr

Dependencias recomendadas para a API nova:

- `Spring Web`
- `Spring for RabbitMQ`
- `WebSocket`
- `Validation`
- `Actuator`
- `Lombok`

Dependencias opcionais, caso a API tambem persista eventos:

- `Spring Data JPA`
- `PostgreSQL Driver`

Versao sugerida:

- `Spring Boot 3.5.x`
- `Java 17`

---

## 8. Estrutura de classes que o agente deve criar

O agente no IntelliJ deve criar a API com uma estrutura parecida com esta:

### Pacote `controller`

- `MensageriaEventoController`
    - recebe `POST /api/mensageria/eventos`
    - recebe DTO do evento
    - delega para service

- `MensageriaLoteController`
    - opcional
    - recebe `POST /api/mensageria/lotes`
    - delega para service

### Pacote `dto`

- `EventoFreteRequest`
- `FretePayloadRequest`
- `LoteEventoFreteRequest`
- `EventoFreteResponse`
- `LoteEventoFreteResponse`

### Pacote `service`

- `MensageriaEventoService`
    - orquestra validacao, deduplicacao, publicacao e notificacao

- `RabbitPublisherService`
    - encapsula `RabbitTemplate`
    - publica em exchange/routing key

- `DashboardNotifierService`
    - encapsula `SimpMessagingTemplate`
    - envia para `/topic/fretes`

- `IdempotenciaEventoService`
    - decide se evento ja foi processado
    - pode consultar banco ou cache local

### Pacote `config`

- `RabbitConfig`
    - exchange
    - queue
    - binding
    - conversor JSON

- `WebSocketConfig`
    - endpoint STOMP/WebSocket
    - topicos do dashboard

- `JacksonConfig`
    - opcional
    - configuracao de serializacao de data/hora

### Pacote `entity`

Se houver persistencia:

- `EventoRecebido`
- `EventoRecebidoStatus`

### Pacote `repository`

Se houver persistencia:

- `EventoRecebidoRepository`

### Pacote `exception`

- `EventoInvalidoException`
- `EventoDuplicadoException`
- `PublicacaoMensageriaException`

### Pacote `handler`

- `ApiExceptionHandler`
    - `@RestControllerAdvice`
    - padroniza erros HTTP

---

## 9. Fluxo esperado da API nova

Fluxo alvo:

1. Legado chama `POST /api/mensageria/eventos`.
2. Controller recebe JSON.
3. DTO e validado.
4. Service verifica idempotencia.
5. Service publica no RabbitMQ.
6. Service notifica WebSocket.
7. Service responde `202 Accepted`.

Fluxo com persistencia opcional:

1. Legado chama endpoint.
2. API salva evento recebido.
3. API publica no broker.
4. API marca evento como `PROCESSADO`.
5. API envia para WebSocket.

Fluxo com erro:

1. Legado chama endpoint.
2. API tenta publicar.
3. Falha ao publicar.
4. API registra erro tecnico.
5. API responde conforme estrategia definida:
    - `500`, se quiser que o producer saiba da falha
    - ou `202`, se houver fila interna/local para retentativa

Para o inicio, o mais simples e transparente e:

- retornar erro quando a publicacao falhar de verdade;
- depois evoluir para resiliencia maior.

---

## 10. Regras importantes para o agente respeitar

### Compatibilidade com o legado

- nao mudar o contrato esperado pelo legado sem necessidade;
- manter suporte ao endpoint `POST /api/mensageria/eventos`;
- aceitar o payload bruto vindo do producer atual;
- aceitar datas no formato ISO-8601;
- aceitar os codigos de status exatamente como o legado envia.

### Idempotencia

- a API deve ser preparada para receber evento duplicado;
- o legado pode reenviar no futuro, quando a rotina de outbox for concluida;
- por isso o consumer nao deve assumir unicidade cega por chamada HTTP.

### Observabilidade

- expor `Actuator`;
- logar evento recebido com cuidado para nao poluir logs demais;
- registrar tipo do evento, numero do frete e status.

### WebSocket

- o dashboard do projeto foi desenhado para tempo real;
- a API deve ser capaz de empurrar atualizacoes para o front;
- o payload enviado para WebSocket pode ser o mesmo evento recebido ou uma versao simplificada.

---

## 11. O que nao precisa ser feito agora

O agente nao precisa, neste primeiro passo:

- mexer no legado Java EE;
- criar consumer complexo de RabbitMQ;
- implementar saga;
- criar arquitetura distribuida com varios microservicos;
- introduzir Kafka;
- criar autenticacao pesada se o ambiente ainda for local.

O foco inicial e:

- receber evento do producer legado;
- publicar no RabbitMQ;
- notificar dashboard por WebSocket;
- manter o contrato simples e funcional.

---

## 12. Definicao objetiva da entrega esperada

O agente no IntelliJ deve entregar uma API Spring Boot que:

1. suba na porta `8082`;
2. exponha `POST /api/mensageria/eventos`;
3. valide o evento recebido;
4. publique no RabbitMQ;
5. notifique `/topic/fretes` via WebSocket;
6. tenha classes separadas de controller, service e config;
7. esteja pronta para evoluir para idempotencia e persistencia;
8. preserve compatibilidade com o producer legado atual.

---

## 13. Resumo executivo para o agente

O projeto legado atual ja produz eventos de frete usando outbox transacional. Ele grava registros em `evento_sistema` quando um frete e criado ou muda de status. O que falta e a API Spring Boot de mensageria que vai receber esses eventos por HTTP, publicar no RabbitMQ e repassar as atualizacoes para WebSocket, atendendo o dashboard em tempo real.

O legado deve ser tratado como producer. A API nova deve ser tratada como consumer HTTP + publisher de broker + broadcaster WebSocket. O contrato mais importante a respeitar e `POST /api/mensageria/eventos`, aceitando o payload que o `FreteBO` ja monta hoje.
