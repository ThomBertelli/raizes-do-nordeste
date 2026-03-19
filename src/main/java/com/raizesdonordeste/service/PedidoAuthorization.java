package com.raizesdonordeste.service;

import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Pedido;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class PedidoAuthorization {

    public Long podeListarPedidos(UsuarioAutenticado principal, Long lojaId) {
        if (principal.getPerfil().equals(PerfilUsuario.GERENCIA_MATRIZ)) {
            return null;
        }

        if (principal.getPerfil().equals(PerfilUsuario.GERENTE)) {
            if (lojaId != null && !lojaId.equals(principal.getLojaId())) {
                throw new AccessDeniedException("Acesso negado: você não tem permissão para acessar pedidos de outra loja");
            }
            return principal.getLojaId();
        }

        throw new AccessDeniedException("Perfil não autorizado para listar pedidos");
    }

    public void podeVisualizarPedido(UsuarioAutenticado principal, Pedido pedido) {
        if (principal.getPerfil().equals(PerfilUsuario.GERENCIA_MATRIZ)) {
            return;
        }

        if (principal.getPerfil().equals(PerfilUsuario.GERENTE)) {
            if (!pedido.getLoja().getId().equals(principal.getLojaId())) {
                throw new AccessDeniedException("Acesso negado ao pedido de outra loja");
            }
            return;
        }

        if (principal.getPerfil().equals(PerfilUsuario.CLIENTE)) {
            if (!pedido.getCliente().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Acesso negado ao pedido de outro cliente");
            }
            return;
        }

        throw new AccessDeniedException("Perfil não autorizado: " + principal.getPerfil());
    }

    public void exigirCliente(UsuarioAutenticado principal) {
        if (!principal.getPerfil().equals(PerfilUsuario.CLIENTE)) {
            throw new AccessDeniedException("Apenas clientes podem listar seus próprios pedidos");
        }
    }
}

