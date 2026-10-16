# Spring Batch POC

Java 21, Maven, Spring Boot 3.5.7, Spring Batch e H2.

```sh
mvn clean test
mvn spring-boot:run
```

O Job lê `src/main/resources/produtos.csv`, aplica 10% de reajuste e escreve
`produtos-reajustados.csv`, sem cabeçalho. Valores são arredondados para duas
casas decimais com HALF_UP. Execute os comandos na raiz do projeto.

## Passo 8 — execução ponta a ponta

H2 persiste somente os metadados em `data/batch`. O Boot inicializa as tabelas
com `spring.batch.jdbc.initialize-schema=always`.

Resultado validado:

```csv
1,Teclado,110.00
2,Mouse,55.00
3,Monitor,1100.00
```

| STATUS | READ_COUNT | WRITE_COUNT | COMMIT_COUNT | ROLLBACK_COUNT |
| --- | --- | --- | --- | --- |
| COMPLETED | 3 | 3 | 2 | 0 |

O primeiro chunk processa Teclado e Mouse e faz COMMIT. O segundo processa
Monitor e faz COMMIT. O arquivo gerado e o banco local são ignorados pelo Git.

## Passo 9 — metadados

Consultas executadas com a aplicação encerrada, abrindo o mesmo H2 local:

```sql
SELECT * FROM BATCH_JOB_INSTANCE;
SELECT * FROM BATCH_JOB_EXECUTION;
SELECT STEP_NAME, STATUS, READ_COUNT, WRITE_COUNT, COMMIT_COUNT, ROLLBACK_COUNT
FROM BATCH_STEP_EXECUTION;
```

Resultado inicial: JobInstance 1 (`reajusteProdutoJob`), JobExecution 1
(`COMPLETED`) e StepExecution 1 (`reajusteProdutoStep`, 3 lidos, 3 escritos,
2 commits, 0 rollbacks).

`JobInstance → JobExecution → StepExecution`: a instância identifica o Job
pelo nome e pelos parâmetros identificadores; uma execução registra uma tentativa;
a execução do Step registra contagens, status e contexto para retomada.
O JobRepository persiste esses dados. Nenhuma tabela de negócio foi criada.

Para consultar com o cliente incluído no H2 (ajuste o caminho do JAR à versão local):

```sh
java -cp ~/.m2/repository/com/h2database/h2/2.3.232/h2-2.3.232.jar \
  org.h2.tools.Shell -url jdbc:h2:file:./data/batch -user sa -password ''
```

Use esse cliente com a aplicação encerrada para evitar disputa pelo arquivo H2.

## Passo 10 — falha proposital

O commit deste passo lança `RuntimeException("Erro proposital ao processar Mouse")`
no Processor. Comando utilizado:

```sh
mvn spring-boot:run -Dspring-boot.run.arguments=caso=restart-demo
```

| JobInstance | JobExecution | STATUS | READ_COUNT | WRITE_COUNT | COMMIT_COUNT | ROLLBACK_COUNT |
| --- | --- | --- | --- | --- | --- | --- |
| 2 | 2 | FAILED | 2 | 0 | 0 | 1 |

Teclado e Mouse foram lidos no mesmo chunk de tamanho 2. O Processor falhou
antes de o Writer receber o chunk; nenhum item foi escrito e a transação sofreu
rollback. O arquivo ficou vazio. READ_COUNT=2 registra as leituras observadas
na tentativa, não dois itens confirmados. O código de saída do Maven foi zero:
`BUILD SUCCESS` sozinho não comprova sucesso do Job; consulte seu STATUS.

O Writer de arquivo participa por buffering e estado de restart; um arquivo
não oferece a mesma atomicidade física de uma tabela transacional em todos os
cenários de queda. Aqui a falha aconteceu antes da escrita do primeiro chunk.

## Passo 11 — restart

Removida a falha, executei exatamente o mesmo comando e parâmetro do passo 10.
Não apaguei tabelas, banco, contextos nem o arquivo vazio.

| JobInstance | JobExecution | STATUS | READ_COUNT | WRITE_COUNT | COMMIT_COUNT | ROLLBACK_COUNT |
| --- | --- | --- | --- | --- | --- | --- |
| 2 | 2 | FAILED | 2 | 0 | 0 | 1 |
| 2 | 3 | COMPLETED | 3 | 3 | 2 | 0 |

As execuções 2 e 3 têm `caso=restart-demo`, `IDENTIFYING=Y`, e apontam à mesma
JobInstance 2: isso é restart. Cada tentativa tem seu próprio JobExecution e
StepExecution; o histórico FAILED é preservado. Como nenhum chunk tinha sido
confirmado, o restart leu os três registros desde o início. O CSV final foi
validado novamente, sem duplicatas.

Mudar um parâmetro identificador cria outra JobInstance, não um restart.
Repetir uma instância já COMPLETED é rejeitado. Para uma nova execução manual:

```sh
mvn spring-boot:run -Dspring-boot.run.arguments=caso=nova-execucao-01
```

Use outro valor a cada nova instância desejada. Evite timestamps automáticos
quando quiser demonstrar restart: eles mudariam a identidade.

Consulta antes e depois:

```sql
SELECT JOB_EXECUTION_ID, JOB_INSTANCE_ID, STATUS FROM BATCH_JOB_EXECUTION;
SELECT JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_VALUE, IDENTIFYING
FROM BATCH_JOB_EXECUTION_PARAMS;
SELECT JOB_EXECUTION_ID, STEP_NAME, STATUS, READ_COUNT, WRITE_COUNT,
       COMMIT_COUNT, ROLLBACK_COUNT FROM BATCH_STEP_EXECUTION;
```

## Passo 12 — observabilidade mínima

`BatchLoggingListener` registra início e fim de Job e Step, IDs de execução,
status, quantidade lida/escrita e duração em milissegundos. Não há log por item.
Exemplo observado em `caso=observabilidade` (JobExecution 4):

```text
Fim Step=reajusteProdutoStep executionId=4 status=COMPLETED lidos=3 escritos=3 tempoMs=42
Fim Job=reajusteProdutoJob executionId=4 status=COMPLETED lidos=3 escritos=3 tempoMs=71
```

As contagens do Job são a soma dos Steps executados nessa tentativa; os tempos
variam a cada execução. Os metadados continuam sendo a fonte para o histórico.
