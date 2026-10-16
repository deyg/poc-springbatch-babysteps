package br.com.poc.batch.config;

import br.com.poc.batch.domain.Produto;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class WriterConfig {
    @Bean
    public FlatFileItemWriter<Produto> produtoWriter() {
        return new FlatFileItemWriterBuilder<Produto>()
                .name("produtoWriter")
                .resource(new FileSystemResource("produtos-reajustados.csv"))
                .lineAggregator(produto -> produto.id() + "," + produto.nome() + ","
                        + produto.preco().toPlainString())
                .build();
    }
}
