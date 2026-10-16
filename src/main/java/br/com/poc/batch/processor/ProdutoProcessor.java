package br.com.poc.batch.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import br.com.poc.batch.domain.Produto;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class ProdutoProcessor implements ItemProcessor<Produto, Produto> {
    @Override
    public Produto process(Produto produto) {
        if ("Mouse".equals(produto.nome())) {
            throw new RuntimeException("Erro proposital ao processar Mouse");
        }
        var novoPreco = produto.preco().multiply(new BigDecimal("1.10"))
                .setScale(2, RoundingMode.HALF_UP);
        return new Produto(produto.id(), produto.nome(), novoPreco);
    }
}
