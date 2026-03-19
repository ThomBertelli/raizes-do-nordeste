package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.loja.LojaUpdateDTO;
import com.raizesdonordeste.api.dto.loja.LojaCreateDTO;
import com.raizesdonordeste.api.dto.loja.LojaResponseDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
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

        Loja salva = lojaRepository.save(loja);
        log.info("Loja criada: lojaId={}, ativa={}, actorId={}, actorPerfil={}",
                salva.getId(),
                salva.isAtiva(),
                obterIdAtor(),
                obterPerfilAtor());

        return toDTO(salva);
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

        Loja atualizada = lojaRepository.save(loja);
        log.info("Loja atualizada: lojaId={}, ativa={}, actorId={}, actorPerfil={}",
                atualizada.getId(),
                atualizada.isAtiva(),
                obterIdAtor(),
                obterPerfilAtor());

        return toDTO(atualizada);
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
        log.info("Loja ativada: lojaId={}, actorId={}, actorPerfil={}", id, obterIdAtor(), obterPerfilAtor());
    }

    @Transactional
    public void desativar(Long id) {
        validarAutorizacaoGerenciaMatriz();
        Loja loja = buscarEntidade(id);
        loja.setAtiva(false);
        lojaRepository.save(loja);
        log.info("Loja desativada: lojaId={}, actorId={}, actorPerfil={}", id, obterIdAtor(), obterPerfilAtor());
    }

    @Transactional
    public void deletar(Long id) {
        validarAutorizacaoGerenciaMatriz();
        if (!lojaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Loja não encontrada");
        }
        lojaRepository.deleteById(id);
        log.info("Loja deletada: lojaId={}, actorId={}, actorPerfil={}", id, obterIdAtor(), obterPerfilAtor());
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

    private Long obterIdAtor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UsuarioAutenticado usuarioAutenticado) {
            return usuarioAutenticado.getId();
        }
        return null;
    }

    private PerfilUsuario obterPerfilAtor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UsuarioAutenticado usuarioAutenticado) {
            return usuarioAutenticado.getPerfil();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(java.util.Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .map(PerfilUsuario::valueOf)
                .orElse(null);
    }
}
