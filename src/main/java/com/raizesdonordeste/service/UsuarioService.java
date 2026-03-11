package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.usuario.UsuarioAtualizacaoDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioCriacaoDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioRespostaDTO;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioRespostaDTO criar(UsuarioCriacaoDTO dto) {
        // ===== DEBUG: INÍCIO =====
        System.out.println("========================================");
        System.out.println("DEBUG - CRIAR USUÁRIO");
        System.out.println("1. DTO Recebido:");
        System.out.println("   - Nome: " + dto.getNome());
        System.out.println("   - Email: " + dto.getEmail());
        System.out.println("   - Perfil (DTO): " + dto.getPerfil());
        System.out.println("   - Perfil name(): " + (dto.getPerfil() != null ? dto.getPerfil().name() : "NULL"));
        System.out.println("   - Consentimento: " + dto.isConsentimentoProgramaFidelidade());
        System.out.println("========================================");
        // ===== DEBUG: FIM =====
        
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

        // ===== DEBUG: ANTES DE SALVAR =====
        System.out.println("2. Usuario ANTES de salvar:");
        System.out.println("   - Nome: " + usuario.getNome());
        System.out.println("   - Email: " + usuario.getEmail());
        System.out.println("   - Perfil: " + usuario.getPerfil());
        System.out.println("   - Perfil name(): " + (usuario.getPerfil() != null ? usuario.getPerfil().name() : "NULL"));
        System.out.println("========================================");
        // ===== DEBUG: FIM =====

        Usuario salvo = usuarioRepository.save(usuario);
        
        // ===== DEBUG: DEPOIS DE SALVAR =====
        System.out.println("3. Usuario DEPOIS de salvar:");
        System.out.println("   - ID: " + salvo.getId());
        System.out.println("   - Nome: " + salvo.getNome());
        System.out.println("   - Email: " + salvo.getEmail());
        System.out.println("   - Perfil: " + salvo.getPerfil());
        System.out.println("   - Perfil name(): " + (salvo.getPerfil() != null ? salvo.getPerfil().name() : "NULL"));
        System.out.println("========================================");
        // ===== DEBUG: FIM =====
        
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
}
