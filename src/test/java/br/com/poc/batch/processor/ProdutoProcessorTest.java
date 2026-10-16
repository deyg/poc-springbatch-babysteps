package br.com.poc.batch.processor;

import java.math.BigDecimal;
import br.com.poc.batch.domain.Produto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class ProdutoProcessorTest {
    @ParameterizedTest
    @CsvSource({"100.00,110.00", "50.00,55.00"})
    void reajustaSemMutarOriginal(String preco, String esperado) {
        var original = new Produto(1L, "Produto", new BigDecimal(preco));
        var resultado = new ProdutoProcessor().process(original);
        assertThat(resultado).isEqualTo(new Produto(1L, "Produto", new BigDecimal(esperado)));
        assertThat(resultado).isNotSameAs(original);
        assertThat(original.preco()).isEqualTo(new BigDecimal(preco));
    }
}
