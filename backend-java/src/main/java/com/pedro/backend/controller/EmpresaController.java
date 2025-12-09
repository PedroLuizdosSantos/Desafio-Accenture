package com.pedro.backend.controller;

import com.pedro.backend.model.Empresa;
import com.pedro.backend.repository.EmpresaFornecedorRepository;
import com.pedro.backend.repository.EmpresaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; 
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController // indica que essa classe expõe endpoints REST
@RequestMapping("/empresas") // prefixo padrão de todas as rotas daqui
public class EmpresaController {

    // repos que conversam direto com o banco
    private final EmpresaRepository empresaRepository;
    private final EmpresaFornecedorRepository empresaFornecedorRepository;

    // injeção dos repositórios via construtor
    public EmpresaController(EmpresaRepository empresaRepository,
                             EmpresaFornecedorRepository empresaFornecedorRepository) {
        this.empresaRepository = empresaRepository;
        this.empresaFornecedorRepository = empresaFornecedorRepository;
    }

    // LISTAR TODAS
    @GetMapping
    public ResponseEntity<List<Empresa>> listar() {
        // busca tudo no banco e devolve 200 OK com a lista
        List<Empresa> empresas = empresaRepository.findAll();
        return ResponseEntity.ok(empresas);
    }

    // BUSCAR POR ID
    @GetMapping("/{id}")
    public ResponseEntity<Empresa> buscar(@PathVariable Long id) {
        // se achar devolve 200 com a empresa, se não 404
        return empresaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CRIAR
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Empresa empresa) {

        // só pra garantir que o CNPJ não venha com espaços a mais
        if (empresa.getCnpj() != null) {
            empresa.setCnpj(empresa.getCnpj().trim());
        }

        // validação de unicidade de CNPJ antes de salvar
        if (empresa.getCnpj() != null && !empresa.getCnpj().isEmpty()) {
            if (empresaRepository.existsByCnpj(empresa.getCnpj())) {
                return ResponseEntity.badRequest()
                        .body("Já existe empresa cadastrada com esse CNPJ.");
            }
        }

        try {
            // salva no banco
            Empresa salva = empresaRepository.save(empresa);
            
            return ResponseEntity
                    .created(URI.create("/empresas/" + salva.getId()))
                    .body(salva);
        } catch (DataIntegrityViolationException e) {
            // pega erro de constraint única no banco e devolve mensagem amigável
            return ResponseEntity.badRequest()
                    .body("Não foi possível salvar a empresa. Verifique se o CNPJ já não está cadastrado.");
        }
    }

    // ATUALIZAR
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Empresa dados) {

        // garante que a empresa existe antes de atualizar
        Optional<Empresa> opt = empresaRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Empresa existente = opt.get();

        // tratativa específica pro CNPJ (trim + checagem de duplicidade)
        String novoCnpj = dados.getCnpj();
        if (novoCnpj != null) {
            novoCnpj = novoCnpj.trim();
            // só checa duplicidade se CNPJ realmente mudou
            if (!novoCnpj.equals(existente.getCnpj())
                    && empresaRepository.existsByCnpj(novoCnpj)) {
                return ResponseEntity.badRequest()
                        .body("Já existe empresa cadastrada com esse CNPJ.");
            }
            existente.setCnpj(novoCnpj);
        }

        
        if (dados.getNomeFantasia() != null) {
            existente.setNomeFantasia(dados.getNomeFantasia());
        }
        if (dados.getCep() != null) {
            existente.setCep(dados.getCep());
        }
        if (dados.getEstado() != null) {
            existente.setEstado(dados.getEstado());
        }

        try {
            Empresa salva = empresaRepository.save(existente);
            return ResponseEntity.ok(salva);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Não foi possível atualizar a empresa. Verifique os dados enviados.");
        }
    }

    // DELETE
    @Transactional 
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {

        // checa se a empresa existe
        Optional<Empresa> opt = empresaRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Empresa empresa = opt.get();

        try {
            // 1) remove todos os vínculos dessa empresa na tabela de junção
            empresaFornecedorRepository.deleteAllByEmpresa(empresa);

            // 2) exclui a empresa em si
            empresaRepository.delete(empresa);

        
            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {
            
            return ResponseEntity.badRequest()
                    .body("Não foi possível excluir a empresa. Verifique se não há vínculos com fornecedores ou outros registros dependentes.");
        } catch (Exception e) {
          
            return ResponseEntity.status(500)
                    .body("Erro inesperado ao excluir a empresa.");
        }
    }

}
