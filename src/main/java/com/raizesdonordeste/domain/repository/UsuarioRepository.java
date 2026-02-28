package com.raizesdonordeste.domain.repository;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Usuario> findByAtivo(boolean ativo, Pageable pageable);

    Page<Usuario> findByPerfil(PerfilUsuario perfil, Pageable pageable);

    Page<Usuario> findByPerfilAndAtivoTrue(PerfilUsuario perfil, Pageable pageable);

    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Usuario> findByConsentimentoProgramaFidelidadeTrue(Pageable pageable);


}