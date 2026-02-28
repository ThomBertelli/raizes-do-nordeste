package com.raizesdonordeste.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "lojas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Loja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Column(nullable = false, length = 18, unique = true)
    @NotBlank(message = "CNPJ é obrigatório")
    private String cnpj;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Endereço é obrigatório")
    @Size(min = 5, max = 255, message = "Endereço deve ter entre 5 e 255 caracteres")
    private String endereco;

    @Column(nullable = false)
    private boolean ativa = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

}
