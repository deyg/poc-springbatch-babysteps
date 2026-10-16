package br.com.poc.batch.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class BatchLoggingListener implements JobExecutionListener, StepExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(BatchLoggingListener.class);

    @Override
    public void beforeJob(JobExecution execution) {
        log.info("Início Job={} executionId={}",
                execution.getJobInstance().getJobName(), execution.getId());
    }

    @Override
    public void afterJob(JobExecution execution) {
        long lidos = execution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long escritos = execution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        log.info("Fim Job={} executionId={} status={} lidos={} escritos={} tempoMs={}",
                execution.getJobInstance().getJobName(), execution.getId(), execution.getStatus(),
                lidos, escritos, tempoMs(execution.getStartTime(), execution.getEndTime()));
    }

    @Override
    public void beforeStep(StepExecution execution) {
        log.info("Início Step={} executionId={}", execution.getStepName(), execution.getId());
    }

    @Override
    public ExitStatus afterStep(StepExecution execution) {
        log.info("Fim Step={} executionId={} status={} lidos={} escritos={} tempoMs={}",
                execution.getStepName(), execution.getId(), execution.getStatus(),
                execution.getReadCount(), execution.getWriteCount(),
                tempoMs(execution.getStartTime(), execution.getEndTime()));
        return null;
    }

    private long tempoMs(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) {
            return 0;
        }
        return Duration.between(inicio, fim == null ? LocalDateTime.now() : fim).toMillis();
    }
}
