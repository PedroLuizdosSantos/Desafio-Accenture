package com.pedro.backend.repository;

import com.pedro.backend.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    List<Fornecedor> findByNomeContainingIgnoreCase(String nome);

    List<Fornecedor> findByCpfCnpj(String cpfCnpj);

    List<Fornecedor> findByNomeContainingIgnoreCaseAndCpfCnpj(String nome, String cpfCnpj);

    // usado para validar CPF/CNPJ duplicado
    boolean existsByCpfCnpj(String cpfCnpj);
    boolean existsById(Long id);

    
}
