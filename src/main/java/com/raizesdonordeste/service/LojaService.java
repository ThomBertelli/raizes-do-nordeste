package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.loja.LojaUpdateDTO;
import com.raizesdonordeste.api.dto.loja.LojaCreateDTO;
import com.raizesdonordeste.api.dto.loja.LojaResponseDTO;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LojaService {

    private final LojaRepository lojaRepository;

    @Transactional
    public LojaResponseDTO criar(LojaCreateDTO dto) {
        validarAutorizacaoGerenciaMatriz();
        if (lojaRepository.existsByCnpj(dto.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        Loja loja = Loja.builder()
                .nome(dto.getNome())
                .cnpj(dto.getCnpj())
                .endereco(dto.getEndereco())
                .ativa(true)
                .build();

        return toDTO(lojaRepository.save(loja));
    }

    @Transactional
    public LojaResponseDTO atualizar(Long id, LojaUpdateDTO dto) {
        validarAutorizacaoGerenciaMatriz();
        Loja loja = buscarEntidade(id);

        if (dto.getNome() != null) {
            loja.setNome(dto.getNome());
        }

        if (dto.getCnpj() != null && !dto.getCnpj().equals(loja.getCnpj())) {
            if (lojaRepository.existsByCnpj(dto.getCnpj())) {
                throw new IllegalArgumentException("CNPJ já cadastrado");
            }
            loja.setCnpj(dto.getCnpj());
        }

        if (dto.getEndereco() != null) {
            loja.setEndereco(dto.getEndereco());
        }

        if (dto.getAtiva() != null) {
            loja.setAtiva(dto.getAtiva());
        }

        return toDTO(lojaRepository.save(loja));
    }

    @Transactional(readOnly = true)
    public LojaResponseDTO buscarPorId(Long id) {
        return toDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Page<LojaResponseDTO> listarTodos(Pageable pageable) {
        return lojaRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LojaResponseDTO> buscarAtivas(Pageable pageable) {
        return lojaRepository.findByAtiva(true, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LojaResponseDTO> buscarPorNome(String nome, Pageable pageable) {
        return lojaRepository.findByNomeContainingIgnoreCase(nome, pageable).map(this::toDTO);
    }

    @Transactional
    public void ativar(Long id) {
        validarAutorizacaoGerenciaMatriz();
        Loja loja = buscarEntidade(id);
        loja.setAtiva(true);
        lojaRepository.save(loja);
    }

    @Transactional
    public void desativar(Long id) {
        validarAutorizacaoGerenciaMatriz();
        Loja loja = buscarEntidade(id);
        loja.setAtiva(false);
        lojaRepository.save(loja);
    }

    @Transactional
    public void deletar(Long id) {
        validarAutorizacaoGerenciaMatriz();
        if (!lojaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Loja não encontrada");
        }
        lojaRepository.deleteById(id);
    }

    private Loja buscarEntidade(Long id) {
        return lojaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja não encontrada"));
    }

    private LojaResponseDTO toDTO(Loja loja) {
        return LojaResponseDTO.builder()
                .id(loja.getId())
                .nome(loja.getNome())
                .cnpj(loja.getCnpj())
                .endereco(loja.getEndereco())
                .ativa(loja.isAtiva())
                .dataCriacao(loja.getDataCriacao())
                .dataAtualizacao(loja.getDataAtualizacao())
                .build();
    }

    private void validarAutorizacaoGerenciaMatriz() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        boolean temPermissao = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + PerfilUsuario.GERENCIA_MATRIZ.name()));

        if (!temPermissao) {
            throw new AccessDeniedException("Usuário não tem permissão para realizar esta operação");
        }
    }
}
