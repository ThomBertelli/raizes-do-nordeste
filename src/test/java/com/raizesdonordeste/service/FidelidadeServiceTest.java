package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.fidelidade.ExtratoFidelidadeResponseDTO;
import com.raizesdonordeste.api.dto.fidelidade.SaldoFidelidadeResponseDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.enums.TipoTransacaoFidelidade;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.model.SaldoFidelidade;
import com.raizesdonordeste.domain.model.TransacaoFidelidade;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.SaldoFidelidadeRepository;
import com.raizesdonordeste.domain.repository.TransacaoFidelidadeRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FidelidadeServiceTest {

    @Mock
    private SaldoFidelidadeRepository saldoFidelidadeRepository;

    @Mock
    private TransacaoFidelidadeRepository transacaoFidelidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private FidelidadeService fidelidadeService;

    @Test
    @DisplayName("Cliente consulta saldo com consentimento")
    void clienteConsultaSaldoComConsentimento() {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                10L,
                null,
                PerfilUsuario.CLIENTE,
                "cliente@teste.com",
                "senha",
                true,
                List.of()
        );
        Usuario usuario = Usuario.builder()
                .id(10L)
                .consentimentoProgramaFidelidade(true)
                .build();

        when(securityContextService.getRequiredPrincipal()).thenReturn(principal);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(saldoFidelidadeRepository.findByUsuarioId(10L))
                .thenReturn(Optional.of(SaldoFidelidade.builder()
                        .id(1L)
                        .usuario(usuario)
                        .moedas(new BigDecimal("5.50"))
                        .build()));

        SaldoFidelidadeResponseDTO resposta = fidelidadeService.consultarSaldoCliente();

        assertThat(resposta.getSaldo()).isEqualTo(new BigDecimal("5.50"));
        assertThat(resposta.isConsentimentoProgramaFidelidade()).isTrue();
    }

    @Test
    @DisplayName("Cliente consulta extrato com saldo")
    void clienteConsultaExtratoComSaldo() {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                10L,
                null,
                PerfilUsuario.CLIENTE,
                "cliente@teste.com",
                "senha",
                true,
                List.of()
        );
        Usuario usuario = Usuario.builder()
                .id(10L)
                .consentimentoProgramaFidelidade(true)
                .build();

        TransacaoFidelidade transacao = TransacaoFidelidade.builder()
                .id(100L)
                .usuario(usuario)
                .pedido(Pedido.builder().id(200L).build())
                .tipo(TipoTransacaoFidelidade.GANHO)
                .moedas(new BigDecimal("3.00"))
                .dataCriacao(LocalDateTime.now())
                .build();

        Page<TransacaoFidelidade> page = new PageImpl<>(List.of(transacao));

        when(securityContextService.getRequiredPrincipal()).thenReturn(principal);
        when(transacaoFidelidadeRepository.findByUsuarioIdOrderByDataCriacaoDesc(10L, PageRequest.of(0, 20)))
                .thenReturn(page);
        when(saldoFidelidadeRepository.findByUsuarioId(10L))
                .thenReturn(Optional.of(SaldoFidelidade.builder()
                        .id(2L)
                        .usuario(usuario)
                        .moedas(new BigDecimal("3.00"))
                        .build()));

        ExtratoFidelidadeResponseDTO resposta = fidelidadeService.consultarExtratoCliente(PageRequest.of(0, 20));

        assertThat(resposta.getSaldo()).isEqualTo(new BigDecimal("3.00"));
        assertThat(resposta.getTransacoes().getTotalElements()).isEqualTo(1);
        assertThat(resposta.getTransacoes().getContent().get(0).getPedidoId()).isEqualTo(200L);
        assertThat(resposta.getTransacoes().getContent().get(0).getTipo()).isEqualTo(TipoTransacaoFidelidade.GANHO);
    }

    @Test
    @DisplayName("Nao cliente nao pode consultar saldo")
    void naoClienteNaoPodeConsultarSaldo() {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                11L,
                1L,
                PerfilUsuario.GERENTE,
                "gerente@teste.com",
                "senha",
                true,
                List.of()
        );
        when(securityContextService.getRequiredPrincipal()).thenReturn(principal);

        assertThatThrownBy(() -> fidelidadeService.consultarSaldoCliente())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("clientes");
    }
}


