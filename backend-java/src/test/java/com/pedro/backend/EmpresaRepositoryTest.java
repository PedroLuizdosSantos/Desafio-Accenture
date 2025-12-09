package com.pedro.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaRepositoryTest {

    // Teste simples só pra garantir que o JUnit está rodando
    @Test
    void cnpjDeveTerAte14Caracteres() {
        String cnpj = "12345678000199";
        assertEquals(14, cnpj.length(), "CNPJ deve ter 14 caracteres");
    }

    @Test
    void deveValidarSeStringENumerica() {
        String cnpj = "12345678000199";
        assertTrue(cnpj.matches("\\d+"), "CNPJ deve conter apenas dígitos");
    }
}
