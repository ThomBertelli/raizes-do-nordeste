package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.fidelidade.TaxaFidelidadeResponseDTO;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.ConfiguracaoFidelidade;
import com.raizesdonordeste.domain.repository.ConfiguracaoFidelidadeRepository;
import com.raizesdonordeste.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguracaoFidelidadeServiceTest {

    @Mock
    private ConfiguracaoFidelidadeRepository configuracaoFidelidadeRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private ConfiguracaoFidelidadeService configuracaoFidelidadeService;

    @Test
    @DisplayName("Gerencia matriz atualiza taxa de conversao")
    void gerenciaMatrizAtualizaTaxaConversao() {
        ConfiguracaoFidelidade configuracao = ConfiguracaoFidelidade.builder()
                .id(1L)
                .taxaConversao(new BigDecimal("0.0300"))
                .build();

        when(securityContextService.getRequiredPerfil()).thenReturn(PerfilUsuario.GERENCIA_MATRIZ);
        when(configuracaoFidelidadeRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(configuracao));
        when(configuracaoFidelidadeRepository.save(configuracao)).thenReturn(configuracao);

        TaxaFidelidadeResponseDTO response = configuracaoFidelidadeService.atualizarTaxaConversao(new BigDecimal("0.0500"));

        assertThat(response.getTaxaConversao()).isEqualTo(new BigDecimal("0.0500"));
    }

    @Test
    @DisplayName("Nao permite atualizar taxa com perfil diferente")
    void naoPermiteAtualizarTaxaComPerfilDiferente() {
        when(securityContextService.getRequiredPerfil()).thenReturn(PerfilUsuario.GERENTE);

        assertThatThrownBy(() -> configuracaoFidelidadeService.atualizarTaxaConversao(new BigDecimal("0.0500")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Nao permite taxa negativa")
    void naoPermiteTaxaNegativa() {
        when(securityContextService.getRequiredPerfil()).thenReturn(PerfilUsuario.GERENCIA_MATRIZ);

        assertThatThrownBy(() -> configuracaoFidelidadeService.atualizarTaxaConversao(new BigDecimal("-0.0100")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("taxaConversao");
    }
}

