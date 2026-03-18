package com.raizesdonordeste.config;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

public class UsuarioAutenticado implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long lojaId;
    private final PerfilUsuario perfil;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UsuarioAutenticado(Long id,
                              Long lojaId,
                              PerfilUsuario perfil,
                              String username,
                              String password,
                              boolean enabled,
                              Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.lojaId = lojaId;
        this.perfil = perfil;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static UsuarioAutenticado fromUsuario(Usuario usuario) {
        Long lojaId = usuario.getLoja() != null ? usuario.getLoja().getId() : null;
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())
        );

        return new UsuarioAutenticado(
                usuario.getId(),
                lojaId,
                usuario.getPerfil(),
                usuario.getEmail(),
                usuario.getSenha(),
                usuario.isAtivo(),
                authorities
        );
    }

    public Long getId() {
        return id;
    }

    public Long getLojaId() {
        return lojaId;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
