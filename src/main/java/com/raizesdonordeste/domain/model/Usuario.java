package com.raizesdonordeste.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Column(nullable = false, length = 150, unique = true)
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @JsonIgnore
    private String senha;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Perfil é obrigatório")
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "consentimento_programa_fidelidade", nullable = false)
    private boolean consentimentoProgramaFidelidade = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime atualizadoEm;



}
