package com.raizesdonordeste.api.dto.usuario;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioCreateResponseDTO {

    private String usuario;
    private String mensagem;
    private PerfilUsuario perfil;
}
