package com.raizesdonordeste.api.dto.auth;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDTO {

    private String accessToken;
    private Long expiresIn;
    @Builder.Default
    private final String tipo = "Bearer";
    private Long id;
    private String nome;
    private String email;
    private PerfilUsuario perfil;
}

