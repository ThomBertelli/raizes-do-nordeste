package com.raizesdonordeste.api.dto.usuario;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAtualizacaoDTO {

    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @Email(message = "Email deve ser válido")
    private String email;

    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String senha;

    private PerfilUsuario perfil;

    private Long lojaId;

    private Boolean ativo;

    private Boolean consentimentoProgramaFidelidade;
}

