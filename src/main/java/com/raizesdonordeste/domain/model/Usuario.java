package com.raizesdonordeste.domain.model;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 150)
    private String nome;

    @Column(nullable = false,length = 150, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PerfilUsuario perfil;

    private boolean ativo = true;

    @Column(name = "consentimento_programa_fidelidade")
    private boolean consentimentoProgramaFidelidade = false;






}
