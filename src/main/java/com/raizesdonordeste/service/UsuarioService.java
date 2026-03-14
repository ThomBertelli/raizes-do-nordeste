package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.usuario.UsuarioAtualizacaoDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioCriacaoDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioRespostaDTO;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioRespostaDTO criar(UsuarioCriacaoDTO dto) {
        validarPermissaoCriacao(dto.getPerfil());

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .perfil(dto.getPerfil())
                .ativo(true)
                .consentimentoProgramaFidelidade(dto.isConsentimentoProgramaFidelidade())
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        
        return converterParaDTO(salvo);
    }

    @Transactional
    public UsuarioRespostaDTO atualizar(Long id, UsuarioAtualizacaoDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (dto.getNome() != null) {
            usuario.setNome(dto.getNome());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(usuario.getEmail())) {
            if (usuarioRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email já cadastrado");
            }
            usuario.setEmail(dto.getEmail());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        if (dto.getPerfil() != null) {
            usuario.setPerfil(dto.getPerfil());
        }

        if (dto.getAtivo() != null) {
            usuario.setAtivo(dto.getAtivo());
        }

        if (dto.getConsentimentoProgramaFidelidade() != null) {
            usuario.setConsentimentoProgramaFidelidade(dto.getConsentimentoProgramaFidelidade());
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        return converterParaDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public UsuarioRespostaDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return converterParaDTO(usuario);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioRespostaDTO> listarTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioRespostaDTO> buscarPorNome(String nome, Pageable pageable) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome, pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioRespostaDTO> buscarPorPerfil(String perfil, Pageable pageable) {
        return usuarioRepository.findByPerfil(
                com.raizesdonordeste.domain.enums.PerfilUsuario.valueOf(perfil.toUpperCase()), 
                pageable
        ).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioRespostaDTO> buscarAtivos(Pageable pageable) {
        return usuarioRepository.findByAtivo(true, pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioRespostaDTO> buscarComConsentimentoFidelidade(Pageable pageable) {
        return usuarioRepository.findByConsentimentoProgramaFidelidadeTrue(pageable)
                .map(this::converterParaDTO);
    }

    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
    }

    @Transactional
    public void desativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void ativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    private UsuarioRespostaDTO converterParaDTO(Usuario usuario) {
        return UsuarioRespostaDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .perfil(usuario.getPerfil())
                .ativo(usuario.isAtivo())
                .consentimentoProgramaFidelidade(usuario.isConsentimentoProgramaFidelidade())
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }

    private void validarPermissaoCriacao(PerfilUsuario perfilDesejado) {
        PerfilUsuario perfilSolicitante = obterPerfilDoSolicitante();

        boolean permitido = switch (perfilSolicitante) {
            case ADMIN -> perfilDesejado != PerfilUsuario.CLIENTE;
            case GERENTE -> perfilDesejado == PerfilUsuario.FUNCIONARIO;
            case GERENCIA_MATRIZ -> perfilDesejado == PerfilUsuario.FUNCIONARIO
                    || perfilDesejado == PerfilUsuario.GERENTE
                    || perfilDesejado == PerfilUsuario.GERENCIA_MATRIZ;
            default -> false;
        };

        if (!permitido) {
            throw new AccessDeniedException("Perfil sem permissão para criar usuário com o perfil informado");
        }
    }

    private PerfilUsuario obterPerfilDoSolicitante() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .map(PerfilUsuario::valueOf)
                .orElseThrow(() -> new AccessDeniedException("Perfil do usuário autenticado não identificado"));
    }
}
