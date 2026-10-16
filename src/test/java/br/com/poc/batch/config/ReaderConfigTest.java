package br.com.poc.batch.config;

import java.math.BigDecimal;
import br.com.poc.batch.domain.Produto;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;

class ReaderConfigTest {
    @Test
    void leCsvIgnorandoCabecalho() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(ReaderConfig.class)) {
            var reader = context.getBean("produtoReader", org.springframework.batch.item.file.FlatFileItemReader.class);
            reader.open(new ExecutionContext());
            try {
                assertThat(reader.read()).isEqualTo(new Produto(1L, "Teclado", new BigDecimal("100.00")));
                assertThat(reader.read()).isEqualTo(new Produto(2L, "Mouse", new BigDecimal("50.00")));
                assertThat(reader.read()).isEqualTo(new Produto(3L, "Monitor", new BigDecimal("1000.00")));
                assertThat(reader.read()).isNull();
            } finally {
                reader.close();
            }
        }
    }
}
