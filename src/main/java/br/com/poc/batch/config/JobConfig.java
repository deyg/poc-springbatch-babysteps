package br.com.poc.batch.config;

import br.com.poc.batch.listener.BatchLoggingListener;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobConfig {
    @Bean
    public Job reajusteProdutoJob(JobRepository jobRepository, Step reajusteProdutoStep, BatchLoggingListener loggingListener) {
        return new JobBuilder("reajusteProdutoJob", jobRepository)
                .start(reajusteProdutoStep)
                .listener(loggingListener)
                .build();
    }
}
