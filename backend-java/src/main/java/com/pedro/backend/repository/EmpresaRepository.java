package com.pedro.backend.repository;

import com.pedro.backend.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    boolean existsById(Long id);

    boolean existsByCnpj(String cnpj);
}
