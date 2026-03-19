package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.usuario.UsuarioUpdateDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioCreateDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final LojaRepository lojaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO criar(UsuarioCreateDTO dto) {
        validarPermissaoCriacao(dto.getPerfil());

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        boolean consentimentoFidelidade = resolverConsentimentoFidelidadeCriacao(dto);
        Loja lojaVinculada = resolverLojaPorPerfil(dto.getPerfil(), dto.getLojaId());

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                .senha(passwordEncoder.encode(dto.getSenha()))
                .perfil(dto.getPerfil())
                .loja(lojaVinculada)
                .ativo(true)
                .consentimentoProgramaFidelidade(consentimentoFidelidade)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuario criado: usuarioId={}, perfil={}, lojaId={}, actorId={}, actorPerfil={}",
                salvo.getId(),
                salvo.getPerfil(),
                salvo.getLoja() != null ? salvo.getLoja().getId() : null,
                obterIdAtor(),
                obterPerfilAtor());

        return converterParaDTO(salvo);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        PerfilUsuario perfilFinal = dto.getPerfil() != null ? dto.getPerfil() : usuario.getPerfil();
        Long lojaIdFinal = dto.getLojaId() != null
                ? dto.getLojaId()
                : (usuario.getLoja() != null ? usuario.getLoja().getId() : null);

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

        if (dto.getLojaId() != null || dto.getPerfil() != null) {
            usuario.setLoja(resolverLojaPorPerfil(perfilFinal, lojaIdFinal));
        }

        if (dto.getAtivo() != null) {
            usuario.setAtivo(dto.getAtivo());
        }

        if (dto.getConsentimentoProgramaFidelidade() != null) {
            usuario.setConsentimentoProgramaFidelidade(dto.getConsentimentoProgramaFidelidade());
        }

        Usuario atualizado = usuarioRepository.save(usuario);
        log.info("Usuario atualizado: usuarioId={}, perfil={}, ativo={}, actorId={}, actorPerfil={}",
                atualizado.getId(),
                atualizado.getPerfil(),
                atualizado.isAtivo(),
                obterIdAtor(),
                obterPerfilAtor());

        return converterParaDTO(atualizado);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return converterParaDTO(usuario);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listarTodos(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> buscarPorNome(String nome, Pageable pageable) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome, pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> buscarPorPerfil(String perfil, Pageable pageable) {
        return usuarioRepository.findByPerfil(
                com.raizesdonordeste.domain.enums.PerfilUsuario.valueOf(perfil.toUpperCase()), 
                pageable
        ).map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> buscarAtivos(Pageable pageable) {
        return usuarioRepository.findByAtivo(true, pageable)
                .map(this::converterParaDTO);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> buscarComConsentimentoFidelidade(Pageable pageable) {
        return usuarioRepository.findByConsentimentoProgramaFidelidadeTrue(pageable)
                .map(this::converterParaDTO);
    }

    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
        log.info("Usuario deletado: usuarioId={}, actorId={}, actorPerfil={}", id, obterIdAtor(), obterPerfilAtor());
    }

    @Transactional
    public void desativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        log.info("Usuario desativado: usuarioId={}, actorId={}, actorPerfil={}", id, obterIdAtor(), obterPerfilAtor());
    }

    @Transactional
    public void ativar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
        log.info("Usuario ativado: usuarioId={}, actorId={}, actorPerfil={}", id, obterIdAtor(), obterPerfilAtor());
    }

    private UsuarioResponseDTO converterParaDTO(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .perfil(usuario.getPerfil())
                .lojaId(usuario.getLoja() != null ? usuario.getLoja().getId() : null)
                .ativo(usuario.isAtivo())
                .consentimentoProgramaFidelidade(usuario.isConsentimentoProgramaFidelidade())
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }

    private Loja resolverLojaPorPerfil(PerfilUsuario perfil, Long lojaId) {
        boolean exigeLoja = perfil == PerfilUsuario.GERENTE || perfil == PerfilUsuario.FUNCIONARIO;

        if (exigeLoja && lojaId == null) {
            throw new IllegalArgumentException("lojaId é obrigatório para GERENTE e FUNCIONARIO");
        }

        if (!exigeLoja && lojaId != null) {
            throw new IllegalArgumentException("lojaId deve ser nulo para CLIENTE, ADMIN e GERENCIA_MATRIZ");
        }

        if (!exigeLoja) {
            return null;
        }

        return lojaRepository.findById(lojaId)
                .orElseThrow(() -> new IllegalArgumentException("Loja não encontrada"));
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
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .map(PerfilUsuario::valueOf)
                .orElse(null);
    }

    private boolean resolverConsentimentoFidelidadeCriacao(UsuarioCreateDTO dto) {
        if (dto.getPerfil() == PerfilUsuario.CLIENTE) {
            if (dto.getConsentimentoProgramaFidelidade() == null) {
                throw new IllegalArgumentException("Consentimento do programa de fidelidade é obrigatório para perfil CLIENTE");
            }
            return dto.getConsentimentoProgramaFidelidade();
        }

        // Não cliente não participa do programa de fidelidade no cadastro administrativo.
        return false;
    }
}
