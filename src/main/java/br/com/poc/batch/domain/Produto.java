package br.com.poc.batch.domain;

import java.math.BigDecimal;

public record Produto(Long id, String nome, BigDecimal preco) {}
