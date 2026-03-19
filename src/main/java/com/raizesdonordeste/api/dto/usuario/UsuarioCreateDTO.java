package com.raizesdonordeste.api.dto.usuario;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCreateDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 150, message = "Nome deve ter entre 3 e 150 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    private String senha;

    @NotNull(message = "Perfil é obrigatório")
    private PerfilUsuario perfil;

    private Long lojaId;

    private Boolean consentimentoProgramaFidelidade;

    @AssertTrue(message = "Consentimento do programa de fidelidade é obrigatório para perfil CLIENTE")
    public boolean isConsentimentoValidoParaPerfil() {
        if (perfil == PerfilUsuario.CLIENTE) {
            return consentimentoProgramaFidelidade != null;
        }
        return true;
    }
}

