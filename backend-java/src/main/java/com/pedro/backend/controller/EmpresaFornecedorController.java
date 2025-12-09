package com.pedro.backend.controller;

import com.pedro.backend.model.Empresa;
import com.pedro.backend.model.EmpresaFornecedor;
import com.pedro.backend.model.Fornecedor;
import com.pedro.backend.model.TipoPessoa;
import com.pedro.backend.repository.EmpresaFornecedorRepository;
import com.pedro.backend.repository.EmpresaRepository;
import com.pedro.backend.repository.FornecedorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@RestController
@RequestMapping("/empresas")
public class EmpresaFornecedorController {

    private final EmpresaRepository empresaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final EmpresaFornecedorRepository empresaFornecedorRepository;

    public EmpresaFornecedorController(EmpresaRepository empresaRepository,
                                       FornecedorRepository fornecedorRepository,
                                       EmpresaFornecedorRepository empresaFornecedorRepository) {
        this.empresaRepository = empresaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.empresaFornecedorRepository = empresaFornecedorRepository;
    }

    @PostMapping("/{empresaId}/fornecedores/{fornecedorId}")
    public ResponseEntity<?> vincular(@PathVariable Long empresaId,
                                      @PathVariable Long fornecedorId) {

        // 1. Busca empresa
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            return ResponseEntity.notFound().build();
        }

        // 2. Busca fornecedor
        Fornecedor fornecedor = fornecedorRepository.findById(fornecedorId).orElse(null);
        if (fornecedor == null) {
            return ResponseEntity.notFound().build();
        }

        // 3. Regra do Paraná:
        // Empresa do PR não pode ter fornecedor PF menor de 18 anos
        if ("PR".equalsIgnoreCase(empresa.getEstado())
                && fornecedor.getTipoPessoa() == TipoPessoa.PF
                && fornecedor.getDataNascimento() != null) {

            int idade = Period.between(
                    fornecedor.getDataNascimento(),
                    LocalDate.now()
            ).getYears();

            if (idade < 18) {
                return ResponseEntity.badRequest()
                        .body("Não é permitido vincular fornecedor pessoa física menor de idade a empresa do Paraná.");
            }
        }

        // 4. Impede vínculo duplicado
        if (empresaFornecedorRepository.existsByEmpresaAndFornecedor(empresa, fornecedor)) {
            return ResponseEntity.badRequest()
                    .body("Esse fornecedor já está vinculado a essa empresa.");
        }

        // 5. Cria vínculo
        EmpresaFornecedor vinculo = new EmpresaFornecedor();
        vinculo.setEmpresa(empresa);
        vinculo.setFornecedor(fornecedor);
        vinculo = empresaFornecedorRepository.save(vinculo);

        URI location = URI.create(
                String.format("/empresas/%d/fornecedores/%d", empresaId, fornecedorId)
        );

        return ResponseEntity.created(location).body(vinculo);
    }

    @GetMapping("/{empresaId}/fornecedores")
    public ResponseEntity<List<Fornecedor>> listarFornecedores(@PathVariable Long empresaId) {
        if (!empresaRepository.existsById(empresaId)) {
            return ResponseEntity.notFound().build();
        }

        List<Fornecedor> fornecedores =
                empresaFornecedorRepository.findFornecedoresByEmpresaId(empresaId);

        return ResponseEntity.ok(fornecedores);
    }

    // 👉 NOVO: DESVINCULAR fornecedor de empresa
    @Transactional
    @DeleteMapping("/{empresaId}/fornecedores/{fornecedorId}")
    public ResponseEntity<?> desvincular(@PathVariable Long empresaId,
                                         @PathVariable Long fornecedorId) {

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            return ResponseEntity.notFound().build();
        }

        Fornecedor fornecedor = fornecedorRepository.findById(fornecedorId).orElse(null);
        if (fornecedor == null) {
            return ResponseEntity.notFound().build();
        }

        if (!empresaFornecedorRepository.existsByEmpresaAndFornecedor(empresa, fornecedor)) {
            return ResponseEntity.badRequest()
                    .body("Vínculo entre essa empresa e fornecedor não existe.");
        }

        try {
            empresaFornecedorRepository.deleteByEmpresaAndFornecedor(empresa, fornecedor);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Não foi possível desvincular o fornecedor desta empresa.");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Erro inesperado ao desvincular o fornecedor.");
        }
    }
}
