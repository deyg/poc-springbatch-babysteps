package br.com.poc.batch.config;

import br.com.poc.batch.domain.Produto;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ReaderConfig {
    @Bean
    public FlatFileItemReader<Produto> produtoReader() {
        return new FlatFileItemReaderBuilder<Produto>()
                .name("produtoReader")
                .resource(new ClassPathResource("produtos.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "nome", "preco")
                .fieldSetMapper(fields -> new Produto(
                        fields.readLong("id"), fields.readString("nome"),
                        fields.readBigDecimal("preco")))
                .build();
    }
}
