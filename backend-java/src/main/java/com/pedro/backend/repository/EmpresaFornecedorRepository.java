package com.pedro.backend.repository;

import com.pedro.backend.model.Empresa;
import com.pedro.backend.model.EmpresaFornecedor;
import com.pedro.backend.model.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmpresaFornecedorRepository extends JpaRepository<EmpresaFornecedor, Long> {

    // verifica se já existe um vínculo específico empresa ↔ fornecedor
    boolean existsByEmpresaAndFornecedor(Empresa empresa, Fornecedor fornecedor);

    // lista todos os fornecedores de uma empresa
    @Query("SELECT ef.fornecedor FROM EmpresaFornecedor ef WHERE ef.empresa.id = :empresaId")
    List<Fornecedor> findFornecedoresByEmpresaId(@Param("empresaId") Long empresaId);

    // usados na hora de excluir em cascata (empresa ou fornecedor)
    boolean existsByEmpresa(Empresa empresa);
    boolean existsByFornecedor(Fornecedor fornecedor);

    void deleteAllByEmpresa(Empresa empresa);
    void deleteAllByFornecedor(Fornecedor fornecedor);

    
    void deleteByEmpresaAndFornecedor(Empresa empresa, Fornecedor fornecedor);
}
