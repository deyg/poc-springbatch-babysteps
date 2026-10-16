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
