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
