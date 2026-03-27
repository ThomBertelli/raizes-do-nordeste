package com.raizesdonordeste.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raizesdonordeste.api.dto.usuario.UsuarioCreateDTO;
import com.raizesdonordeste.api.dto.usuario.UsuarioResponseDTO;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest {

    private final UsuarioService usuarioService = mock(UsuarioService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new UsuarioController(usuarioService))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void criarDeveRetornarRespostaEnxuta() throws Exception {
        when(usuarioService.criar(any(UsuarioCreateDTO.class))).thenReturn(
                UsuarioResponseDTO.builder()
                        .id(8L)
                        .nome("Marcos Paulo Bezerra")
                        .email("marcospaulo@raizesnordeste.com.br")
                        .perfil(PerfilUsuario.FUNCIONARIO)
                        .lojaId(1L)
                        .ativo(true)
                        .consentimentoProgramaFidelidade(false)
                        .build()
        );

        UsuarioCreateDTO request = new UsuarioCreateDTO(
                "Marcos Paulo Bezerra",
                "marcospaulo@raizesnordeste.com.br",
                "Senha@123",
                PerfilUsuario.FUNCIONARIO,
                1L,
                null
        );

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").value("Marcos Paulo Bezerra"))
                .andExpect(jsonPath("$.mensagem").value("criado com sucesso"))
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.lojaId").doesNotExist());
    }
}
