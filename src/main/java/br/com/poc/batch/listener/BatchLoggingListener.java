package br.com.poc.batch.listener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import br.com.poc.batch.domain.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Component
public class BatchLoggingListener implements JobExecutionListener, StepExecutionListener,
        ChunkListener, ItemReadListener<Produto>, ItemProcessListener<Produto, Produto>,
        ItemWriteListener<Produto>, SkipListener<Produto, Produto> {

    private static final Logger log = LoggerFactory.getLogger(BatchLoggingListener.class);
    private static final String CHUNK_NUMBER_KEY = "observabilidade.chunk.numero";

    @Override
    public void beforeJob(JobExecution execution) {
        log.info("[JOB][INICIO][nome={}][execucao={}][parametros={}]",
                execution.getJobInstance().getJobName(), execution.getId(), execution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution execution) {
        long lidos = execution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long escritos = execution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        long filtrados = execution.getStepExecutions().stream().mapToLong(StepExecution::getFilterCount).sum();
        long ignorados = execution.getStepExecutions().stream().mapToLong(StepExecution::getSkipCount).sum();

        log.info("[JOB][RESUMO][nome={}][execucao={}][status={}][lidos={}][escritos={}]"
                        + "[filtrados={}][ignorados={}][tempoMs={}]",
                execution.getJobInstance().getJobName(), execution.getId(), execution.getStatus(),
                lidos, escritos, filtrados, ignorados,
                tempoMs(execution.getStartTime(), execution.getEndTime()));

        execution.getAllFailureExceptions().forEach(erro ->
                log.error("[JOB][ERRO][tipo={}][mensagem={}]",
                        erro.getClass().getSimpleName(), erro.getMessage(), erro));
    }

    @Override
    public void beforeStep(StepExecution execution) {
        execution.getExecutionContext().putInt(CHUNK_NUMBER_KEY, 0);
        log.info("[STEP][INICIO][nome={}][execucao={}][jobExecucao={}]",
                execution.getStepName(), execution.getId(), execution.getJobExecutionId());
    }

    @Override
    public ExitStatus afterStep(StepExecution execution) {
        log.info("[STEP][RESUMO][nome={}][execucao={}][status={}][exitStatus={}]"
                        + "[lidos={}][escritos={}][filtrados={}][readSkips={}][processSkips={}]"
                        + "[writeSkips={}][commits={}][rollbacks={}][tempoMs={}]",
                execution.getStepName(), execution.getId(), execution.getStatus(), execution.getExitStatus(),
                execution.getReadCount(), execution.getWriteCount(), execution.getFilterCount(),
                execution.getReadSkipCount(), execution.getProcessSkipCount(), execution.getWriteSkipCount(),
                execution.getCommitCount(), execution.getRollbackCount(),
                tempoMs(execution.getStartTime(), execution.getEndTime()));
        return null;
    }

    @Override
    public void beforeChunk(ChunkContext context) {
        StepExecution step = context.getStepContext().getStepExecution();
        int numero = step.getExecutionContext().getInt(CHUNK_NUMBER_KEY, 0) + 1;
        step.getExecutionContext().putInt(CHUNK_NUMBER_KEY, numero);
        log.info("[CHUNK][INICIO][numero={}][step={}]", numero, step.getStepName());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        StepExecution step = context.getStepContext().getStepExecution();
        log.info("[CHUNK][COMMIT][numero={}][lidosAcumulados={}][escritosAcumulados={}]"
                        + "[commitsAcumulados={}]",
                numeroChunk(step), step.getReadCount(), step.getWriteCount(), step.getCommitCount());
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        StepExecution step = context.getStepContext().getStepExecution();
        log.error("[CHUNK][ERRO][numero={}][step={}][rollbacksAcumulados={}]",
                numeroChunk(step), step.getStepName(), step.getRollbackCount());
    }

    @Override
    public void beforeRead() {
        log.debug("[READER][AGUARDANDO_ITEM]");
    }

    @Override
    public void afterRead(Produto produto) {
        log.info("[READER][ITEM_LIDO][id={}][nome={}][preco={}]",
                produto.id(), produto.nome(), produto.preco());
    }

    @Override
    public void onReadError(Exception erro) {
        log.error("[READER][ERRO][tipo={}][mensagem={}]",
                erro.getClass().getSimpleName(), erro.getMessage(), erro);
    }

    @Override
    public void beforeProcess(Produto produto) {
        log.debug("[PROCESSOR][INICIO][id={}]", produto.id());
    }

    @Override
    public void afterProcess(Produto original, Produto processado) {
        if (processado == null) {
            log.info("[PROCESSOR][FILTRADO][id={}][nome={}]", original.id(), original.nome());
            return;
        }

        String percentual = percentualReajuste(original.preco(), processado.preco());
        log.info("[PROCESSOR][CALCULO][id={}][nome={}][precoOriginal={}]"
                        + "[reajustePercentual={}][precoFinal={}]",
                original.id(), original.nome(), original.preco(),
                percentual, processado.preco());
    }

    @Override
    public void onProcessError(Produto produto, Exception erro) {
        log.error("[PROCESSOR][ERRO][id={}][tipo={}][mensagem={}]",
                produto.id(), erro.getClass().getSimpleName(), erro.getMessage(), erro);
    }

    @Override
    public void beforeWrite(Chunk<? extends Produto> itens) {
        log.info("[WRITER][INICIO][quantidade={}]", itens.size());
    }

    @Override
    public void afterWrite(Chunk<? extends Produto> itens) {
        itens.forEach(produto -> log.info(
                "[WRITER][ITEM_ESCRITO][id={}][nome={}][preco={}]",
                produto.id(), produto.nome(), produto.preco()));
        log.info("[WRITER][FIM][quantidade={}]", itens.size());
    }

    @Override
    public void onWriteError(Exception erro, Chunk<? extends Produto> itens) {
        log.error("[WRITER][ERRO][quantidade={}][tipo={}][mensagem={}]",
                itens.size(), erro.getClass().getSimpleName(), erro.getMessage(), erro);
    }

    @Override
    public void onSkipInRead(Throwable erro) {
        log.warn("[SKIP][LEITURA][tipo={}][mensagem={}]",
                erro.getClass().getSimpleName(), erro.getMessage());
    }

    @Override
    public void onSkipInProcess(Produto produto, Throwable erro) {
        log.warn("[SKIP][PROCESSAMENTO][id={}][tipo={}][mensagem={}]",
                produto.id(), erro.getClass().getSimpleName(), erro.getMessage());
    }

    @Override
    public void onSkipInWrite(Produto produto, Throwable erro) {
        log.warn("[SKIP][ESCRITA][id={}][tipo={}][mensagem={}]",
                produto.id(), erro.getClass().getSimpleName(), erro.getMessage());
    }

    private int numeroChunk(StepExecution step) {
        return step.getExecutionContext().getInt(CHUNK_NUMBER_KEY, 0);
    }

    private String percentualReajuste(BigDecimal precoOriginal, BigDecimal precoFinal) {
        if (precoOriginal.signum() == 0) {
            return "indefinido";
        }
        return precoFinal.subtract(precoOriginal)
                .divide(precoOriginal, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .stripTrailingZeros()
                .toPlainString();
    }

    private long tempoMs(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) {
            return 0;
        }
        return Duration.between(inicio, fim == null ? LocalDateTime.now() : fim).toMillis();
    }
}
