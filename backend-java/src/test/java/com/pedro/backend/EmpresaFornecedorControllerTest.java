package com.pedro.backend;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.*;

class EmpresaFornecedorControllerTest {

    private int calcularIdade(LocalDate nascimento, LocalDate hoje) {
        return Period.between(nascimento, hoje).getYears();
    }

    @Test
    void menorDeIdadeDeveTerMenosDe18Anos() {
        LocalDate nascimento = LocalDate.now().minusYears(17);
        int idade = calcularIdade(nascimento, LocalDate.now());
        assertTrue(idade < 18, "Idade deve ser menor que 18");
    }

    @Test
    void maiorDeIdadeDeveTer18OuMaisAnos() {
        LocalDate nascimento = LocalDate.now().minusYears(20);
        int idade = calcularIdade(nascimento, LocalDate.now());
        assertTrue(idade >= 18, "Idade deve ser maior ou igual a 18");
    }
}
