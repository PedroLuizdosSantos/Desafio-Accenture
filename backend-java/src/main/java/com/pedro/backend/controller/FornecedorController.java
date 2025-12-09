package com.pedro.backend.controller;

import com.pedro.backend.model.Fornecedor;
import com.pedro.backend.model.TipoPessoa;
import com.pedro.backend.repository.EmpresaFornecedorRepository;
import com.pedro.backend.repository.FornecedorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final FornecedorRepository fornecedorRepository;
    private final EmpresaFornecedorRepository empresaFornecedorRepository;

    public FornecedorController(FornecedorRepository fornecedorRepository,
                                EmpresaFornecedorRepository empresaFornecedorRepository) {
        this.fornecedorRepository = fornecedorRepository;
        this.empresaFornecedorRepository = empresaFornecedorRepository;
    }

    // LISTAR TODOS
    @GetMapping
    public ResponseEntity<List<Fornecedor>> listar() {
        List<Fornecedor> fornecedores = fornecedorRepository.findAll();
        return ResponseEntity.ok(fornecedores);
    }

    // BUSCAR POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Fornecedor> buscar(@PathVariable Long id) {
        return fornecedorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CRIAR
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Fornecedor fornecedor) {

        // regra: se PF precisa RG e dataNascimento
        if (fornecedor.getTipoPessoa() == TipoPessoa.PF) {
            if (fornecedor.getRg() == null || fornecedor.getRg().isBlank()
                    || fornecedor.getDataNascimento() == null) {
                return ResponseEntity.badRequest()
                        .body("Para pessoa física é obrigatório informar RG e data de nascimento.");
            }
        }

        if (fornecedor.getCpfCnpj() != null) {
            fornecedor.setCpfCnpj(fornecedor.getCpfCnpj().trim());
        }

        // CPF/CNPJ duplicado
        if (fornecedor.getCpfCnpj() != null && !fornecedor.getCpfCnpj().isEmpty()) {
            if (fornecedorRepository.existsByCpfCnpj(fornecedor.getCpfCnpj())) {
                return ResponseEntity.badRequest()
                        .body("Já existe fornecedor cadastrado com esse CPF/CNPJ.");
            }
        }

        try {
            Fornecedor salvo = fornecedorRepository.save(fornecedor);
            return ResponseEntity
                    .created(URI.create("/fornecedores/" + salvo.getId()))
                    .body(salvo);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Não foi possível salvar o fornecedor. Verifique se o CPF/CNPJ já não está cadastrado.");
        }
    }

    // ATUALIZAR
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @RequestBody Fornecedor dados) {

        Optional<Fornecedor> opt = fornecedorRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Fornecedor existente = opt.get();

        // regra PF: se for PF depois da alteração, RG e data nascimento obrigatórios
        TipoPessoa novoTipo = dados.getTipoPessoa() != null ? dados.getTipoPessoa() : existente.getTipoPessoa();
        if (novoTipo == TipoPessoa.PF) {
            String rg = dados.getRg() != null ? dados.getRg() : existente.getRg();
            LocalDate nasc = dados.getDataNascimento() != null ? dados.getDataNascimento() : existente.getDataNascimento();
            if (rg == null || rg.isBlank() || nasc == null) {
                return ResponseEntity.badRequest()
                        .body("Para pessoa física é obrigatório informar RG e data de nascimento.");
            }
        }

        // CPF/CNPJ
        String novoCpf = dados.getCpfCnpj();
        if (novoCpf != null) {
            novoCpf = novoCpf.trim();
            if (!novoCpf.equals(existente.getCpfCnpj())
                    && fornecedorRepository.existsByCpfCnpj(novoCpf)) {
                return ResponseEntity.badRequest()
                        .body("Já existe fornecedor cadastrado com esse CPF/CNPJ.");
            }
            existente.setCpfCnpj(novoCpf);
        }

        if (dados.getNome() != null) {
            existente.setNome(dados.getNome());
        }
        if (dados.getEmail() != null) {
            existente.setEmail(dados.getEmail());
        }
        if (dados.getRg() != null) {
            existente.setRg(dados.getRg());
        }
        if (dados.getDataNascimento() != null) {
            existente.setDataNascimento(dados.getDataNascimento());
        }
        if (dados.getCep() != null) {
            existente.setCep(dados.getCep());
        }
        if (dados.getTipoPessoa() != null) {
            existente.setTipoPessoa(dados.getTipoPessoa());
        }

        try {
            Fornecedor salvo = fornecedorRepository.save(existente);
            return ResponseEntity.ok(salvo);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Não foi possível atualizar o fornecedor. Verifique os dados enviados.");
        }
    }

    // EXCLUIR (desvinculando antes)
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {

        Optional<Fornecedor> opt = fornecedorRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Fornecedor fornecedor = opt.get();

        try {
            // remove todos os vínculos desse fornecedor
            empresaFornecedorRepository.deleteAllByFornecedor(fornecedor);

            // agora exclui o fornecedor
            fornecedorRepository.delete(fornecedor);

            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Não foi possível excluir o fornecedor. Verifique se não há vínculos com empresas ou outros registros dependentes.");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Erro inesperado ao excluir o fornecedor.");
        }
    }
}
