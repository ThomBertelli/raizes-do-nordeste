package com.raizesdonordeste.api.dto.usuario;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioRespostaDTO {

    private Long id;
    private String nome;
    private String email;
    private PerfilUsuario perfil;
    private Long lojaId;
    private boolean ativo;
    private boolean consentimentoProgramaFidelidade;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}

