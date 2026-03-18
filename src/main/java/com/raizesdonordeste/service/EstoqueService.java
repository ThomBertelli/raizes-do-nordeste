package com.raizesdonordeste.service;

import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.MovimentacaoEstoque;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.MovimentacaoEstoqueRepository;
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
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional(readOnly = true)
    public Page<Estoque> listarEstoquesPorLoja(Long lojaId, Pageable pageable) {
        Long lojaAutorizadaId = validarAcessoEstoque(lojaId);

        if (lojaAutorizadaId == null) {
            return estoqueRepository.findAll(pageable);
        }

        return estoqueRepository.findByLojaId(lojaAutorizadaId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> listarMovimentacoesPorLoja(Long lojaId, Long produtoId, Pageable pageable) {
        Long lojaAutorizadaId = validarAcessoEstoque(lojaId);

        if (lojaAutorizadaId == null) {
            if (produtoId != null) {
                return movimentacaoEstoqueRepository.findByProdutoId(produtoId, pageable);
            }
            return movimentacaoEstoqueRepository.findAll(pageable);
        }

        if (produtoId != null) {
            return movimentacaoEstoqueRepository.findByLojaIdAndProdutoId(lojaAutorizadaId, produtoId, pageable);
        }

        return movimentacaoEstoqueRepository.findByLojaId(lojaAutorizadaId, pageable);
    }

    public Long validarAcessoEstoque(Long lojaId) {
        UsuarioAutenticado usuarioAutenticado = obterUsuarioAutenticado();
        PerfilUsuario perfil = usuarioAutenticado.getPerfil();

        if (perfil == PerfilUsuario.GERENCIA_MATRIZ) {
            return lojaId;
        }

        if (perfil == PerfilUsuario.GERENTE) {
            if (usuarioAutenticado.getLojaId() == null) {
                throw new AccessDeniedException("Gerente sem loja vinculada não pode acessar estoque");
            }

            Long lojaDoGerenteId = usuarioAutenticado.getLojaId();

            if (lojaId != null && !lojaDoGerenteId.equals(lojaId)) {
                throw new AccessDeniedException("Gerente não pode acessar estoque de outra loja");
            }

            return lojaDoGerenteId;
        }

        throw new AccessDeniedException("Perfil sem permissão para acessar estoque");
    }

    private UsuarioAutenticado obterUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UsuarioAutenticado usuarioAutenticado)) {
            throw new AccessDeniedException("Principal autenticado inválido para acesso de estoque");
        }

        return usuarioAutenticado;
    }
}

