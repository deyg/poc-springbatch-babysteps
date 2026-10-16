package br.com.poc.batch.config;

import br.com.poc.batch.listener.BatchLoggingListener;

import br.com.poc.batch.domain.Produto;
import br.com.poc.batch.processor.ProdutoProcessor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class StepConfig {
    @Bean
    public Step reajusteProdutoStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<Produto> produtoReader, ProdutoProcessor processor,
            FlatFileItemWriter<Produto> produtoWriter, BatchLoggingListener loggingListener) {
        return new StepBuilder("reajusteProdutoStep", jobRepository)
                .<Produto, Produto>chunk(2, transactionManager)
                .reader(produtoReader)
                .processor(processor)
                .writer(produtoWriter)
                .listener(loggingListener)
                .build();
    }
}
