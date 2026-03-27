package com.raizesdonordeste.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new RegraNegocioController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void deveRetornarJsonDeRegraNegocio() throws Exception {
        mockMvc.perform(get("/regra-negocio").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Regra de negócio"))
                .andExpect(jsonPath("$.mensagem").value("lojaId é obrigatório para GERENTE e FUNCIONARIO"))
                .andExpect(jsonPath("$.detalhes").isArray());
    }

    @RestController
    static class RegraNegocioController {

        @GetMapping("/regra-negocio")
        void lancar() {
            throw new RegraNegocioException("lojaId é obrigatório para GERENTE e FUNCIONARIO");
        }
    }
}
