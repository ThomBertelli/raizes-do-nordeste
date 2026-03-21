package com.raizesdonordeste.service;

import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SecurityContextService {

    public UsuarioAutenticado getRequiredPrincipal() {
        Authentication authentication = getAuthenticationOrThrow();
        Object principal = authentication.getPrincipal();
        if (principal instanceof UsuarioAutenticado usuarioAutenticado) {
            return usuarioAutenticado;
        }
        throw new AccessDeniedException("Principal autenticado inválido");
    }

    public PerfilUsuario getRequiredPerfil() {
        Authentication authentication = getAuthenticationOrThrow();
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .map(PerfilUsuario::valueOf)
                .orElseThrow(() -> new AccessDeniedException("Perfil do usuário autenticado não identificado"));
    }

    public Long getActorIdOrNull() {
        Authentication authentication = getAuthenticationOrNull();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UsuarioAutenticado usuarioAutenticado) {
            return usuarioAutenticado.getId();
        }
        return null;
    }

    public PerfilUsuario getActorPerfilOrNull() {
        Authentication authentication = getAuthenticationOrNull();
        if (authentication == null) {
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
                .map(authority -> {
                    try {
                        return PerfilUsuario.valueOf(authority);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Authentication getAuthenticationOrThrow() {
        Authentication authentication = getAuthenticationOrNull();
        if (authentication == null) {
            throw new AccessDeniedException("Usuário não autenticado");
        }
        return authentication;
    }

    private Authentication getAuthenticationOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication;
    }
}

