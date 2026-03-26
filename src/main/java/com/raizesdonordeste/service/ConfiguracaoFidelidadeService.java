package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.fidelidade.TaxaFidelidadeResponseDTO;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.ConfiguracaoFidelidade;
import com.raizesdonordeste.domain.repository.ConfiguracaoFidelidadeRepository;
import com.raizesdonordeste.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfiguracaoFidelidadeService {

    private static final BigDecimal TAXA_PADRAO = new BigDecimal("0.0300");

    private final ConfiguracaoFidelidadeRepository configuracaoFidelidadeRepository;
    private final SecurityContextService securityContextService;

    @Transactional(readOnly = true)
    public BigDecimal obterTaxaConversao() {
        return carregarConfiguracao().getTaxaConversao();
    }

    @Transactional(readOnly = true)
    public TaxaFidelidadeResponseDTO consultarTaxaConversao() {
        validarGerenciaMatriz();
        BigDecimal taxa = obterTaxaConversao();
        return TaxaFidelidadeResponseDTO.builder()
                .taxaConversao(taxa)
                .build();
    }

    @Transactional
    public TaxaFidelidadeResponseDTO atualizarTaxaConversao(BigDecimal taxaConversao) {
        validarGerenciaMatriz();
        BigDecimal taxa = validarTaxa(taxaConversao);

        ConfiguracaoFidelidade configuracao = carregarConfiguracao();
        configuracao.setTaxaConversao(taxa);
        configuracaoFidelidadeRepository.save(configuracao);

        log.info("Taxa de fidelidade atualizada: taxa={}, actorId={}, actorPerfil={}",
                taxa,
                securityContextService.getActorIdOrNull(),
                securityContextService.getActorPerfilOrNull());

        return TaxaFidelidadeResponseDTO.builder()
                .taxaConversao(taxa)
                .build();
    }

    private void validarGerenciaMatriz() {
        PerfilUsuario perfil = securityContextService.getRequiredPerfil();
        if (perfil != PerfilUsuario.GERENCIA_MATRIZ) {
            throw new AccessDeniedException("Apenas GERENCIA_MATRIZ pode alterar a taxa de fidelidade");
        }
    }

    private ConfiguracaoFidelidade carregarConfiguracao() {
        return configuracaoFidelidadeRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> configuracaoFidelidadeRepository.save(ConfiguracaoFidelidade.builder()
                        .taxaConversao(TAXA_PADRAO)
                        .singleton(true)
                        .build()));
    }

    private BigDecimal validarTaxa(BigDecimal taxaConversao) {
        if (taxaConversao == null) {
            throw new RegraNegocioException("taxaConversao é obrigatória");
        }
        BigDecimal taxa = taxaConversao.setScale(4, RoundingMode.DOWN);
        if (taxa.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("taxaConversao deve ser maior que zero");
        }
        return taxa;
    }
}


