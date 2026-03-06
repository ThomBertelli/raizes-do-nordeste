package com.raizesdonordeste.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fidelidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fidelidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    @NotNull(message = "Usuário é obrigatório")
    private Usuario usuario;

    @Column(name = "pontos_atuais", nullable = false)
    @Min(value = 0, message = "Pontos atuais não pode ser negativo")
    private int pontosAtuais = 0;

    @Column(name = "pontos_totais_acumulados", nullable = false)
    @Min(value = 0, message = "Pontos totais acumulados não pode ser negativo")
    private int pontosTotaisAcumulados = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public void adicionarPontos(int quantidade) {
        if (quantidade > 0) {
            this.pontosAtuais += quantidade;
            this.pontosTotaisAcumulados += quantidade;
        }
    }

    public void resgatarPontos(int quantidade) {
        if (quantidade > 0 && quantidade <= this.pontosAtuais) {
            this.pontosAtuais -= quantidade;
        }
    }

    public void zerarPontos() {
        this.pontosAtuais = 0;
    }
}
