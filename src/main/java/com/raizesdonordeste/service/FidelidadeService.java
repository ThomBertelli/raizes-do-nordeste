package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.fidelidade.ExtratoFidelidadeItemDTO;
import com.raizesdonordeste.api.dto.fidelidade.ExtratoFidelidadeResponseDTO;
import com.raizesdonordeste.api.dto.fidelidade.SaldoFidelidadeResponseDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.SaldoFidelidade;
import com.raizesdonordeste.domain.model.TransacaoFidelidade;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.SaldoFidelidadeRepository;
import com.raizesdonordeste.domain.repository.TransacaoFidelidadeRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class FidelidadeService {

    private final SaldoFidelidadeRepository saldoFidelidadeRepository;
    private final TransacaoFidelidadeRepository transacaoFidelidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityContextService securityContextService;

    @Transactional(readOnly = true)
    public SaldoFidelidadeResponseDTO consultarSaldoCliente() {
        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();
        if (principal.getPerfil() != PerfilUsuario.CLIENTE) {
            throw new AccessDeniedException("Apenas clientes podem consultar saldo de fidelidade");
        }

        BigDecimal saldo = obterSaldo(principal.getId());
        log.info("Saldo de fidelidade consultado: usuarioId={}, saldo={}", principal.getId(), saldo);

        Usuario usuario = usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não encontrado"));

        return SaldoFidelidadeResponseDTO.builder()
                .saldo(saldo)
                .consentimentoProgramaFidelidade(usuario.isConsentimentoProgramaFidelidade())
                .build();
    }

    @Transactional(readOnly = true)
    public ExtratoFidelidadeResponseDTO consultarExtratoCliente(Pageable pageable) {
        UsuarioAutenticado principal = securityContextService.getRequiredPrincipal();
        if (principal.getPerfil() != PerfilUsuario.CLIENTE) {
            throw new AccessDeniedException("Apenas clientes podem consultar extrato de fidelidade");
        }

        Page<ExtratoFidelidadeItemDTO> transacoes = transacaoFidelidadeRepository
                .findByUsuarioIdOrderByDataCriacaoDesc(principal.getId(), pageable)
                .map(this::converterParaItem);

        BigDecimal saldo = obterSaldo(principal.getId());
        log.info("Extrato de fidelidade consultado: usuarioId={}, totalTransacoes={}",
                principal.getId(),
                transacoes.getTotalElements());

        return ExtratoFidelidadeResponseDTO.builder()
                .saldo(saldo)
                .transacoes(transacoes)
                .build();
    }

    private BigDecimal obterSaldo(Long usuarioId) {
        return saldoFidelidadeRepository.findByUsuarioId(usuarioId)
                .map(SaldoFidelidade::getMoedas)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.DOWN);
    }

    private ExtratoFidelidadeItemDTO converterParaItem(TransacaoFidelidade transacao) {
        return ExtratoFidelidadeItemDTO.builder()
                .id(transacao.getId())
                .pedidoId(transacao.getPedido() != null ? transacao.getPedido().getId() : null)
                .tipo(transacao.getTipo())
                .moedas(transacao.getMoedas())
                .dataCriacao(transacao.getDataCriacao())
                .build();
    }
}


