package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.pagamento.PagamentoRequestDTO;
import com.raizesdonordeste.api.dto.pagamento.PagamentoResponseDTO;
import com.raizesdonordeste.config.UsuarioAutenticado;
import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.enums.StatusPedido;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Pedido;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.PedidoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.infra.request.IdempotentResponse;
import com.raizesdonordeste.infra.request.RequestDeduplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private MockPaymentGateway mockPaymentGateway;

    @Mock
    private RequestDeduplicationService requestDeduplicationService;

    @Mock
    private SecurityContextService securityContextService;

    @Mock
    private JsonMapper objectMapper;

    @InjectMocks
    private PagamentoService pagamentoService;

    @Test
    @DisplayName("Processa pagamento aprovado e confirma o pedido")
    void processaPagamentoAprovado() {
        Pedido pedido = pedidoExemplo(1L, StatusPedido.CRIADO);
        when(pedidoRepository.findByIdWithRelacionamentos(1L)).thenReturn(Optional.of(pedido));

        MockPaymentGateway.MockPaymentResult resultado = new MockPaymentGateway.MockPaymentResult(
                "tx-123",
                true,
                "APROVADO"
        );
        when(mockPaymentGateway.processarPagamento(eq(new BigDecimal("100.00")), isNull())).thenReturn(resultado);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0, Pedido.class));

        PagamentoResponseDTO response = pagamentoService.processarPagamento(1L, new PagamentoRequestDTO(new BigDecimal("100.00")));

        assertThat(response.getPedidoId()).isEqualTo(1L);
        assertThat(response.isAprovado()).isTrue();
        assertThat(response.getStatusPagamento()).isEqualTo("APROVADO");
        assertThat(response.getStatusPedido()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(response.getTransacaoId()).isEqualTo("tx-123");
        assertThat(pedido.getStatusPedido()).isEqualTo(StatusPedido.CONFIRMADO);
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Processa pagamento recusado e cancela o pedido")
    void processaPagamentoRecusado() {
        Pedido pedido = pedidoExemplo(2L, StatusPedido.CRIADO);
        when(pedidoRepository.findByIdWithRelacionamentos(2L)).thenReturn(Optional.of(pedido));

        MockPaymentGateway.MockPaymentResult resultado = new MockPaymentGateway.MockPaymentResult(
                "tx-456",
                false,
                "RECUSADO"
        );
        when(mockPaymentGateway.processarPagamento(eq(new BigDecimal("1500.00")), isNull())).thenReturn(resultado);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0, Pedido.class));

        PagamentoResponseDTO response = pagamentoService.processarPagamento(2L, new PagamentoRequestDTO(new BigDecimal("1500.00")));

        assertThat(response.getPedidoId()).isEqualTo(2L);
        assertThat(response.isAprovado()).isFalse();
        assertThat(response.getStatusPagamento()).isEqualTo("RECUSADO");
        assertThat(response.getStatusPedido()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(response.getTransacaoId()).isEqualTo("tx-456");
        assertThat(pedido.getStatusPedido()).isEqualTo(StatusPedido.CANCELADO);
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Rejeita pagamento quando pedido nao esta em CRIADO")
    void rejeitaPagamentoQuandoPedidoNaoCriado() {
        Pedido pedido = pedidoExemplo(3L, StatusPedido.CONFIRMADO);
        when(pedidoRepository.findByIdWithRelacionamentos(3L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pagamentoService.processarPagamento(3L, new PagamentoRequestDTO(new BigDecimal("50.00"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRIADO");
        verify(mockPaymentGateway, never()).processarPagamento(any(), anyString());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Falha quando pedido nao existe")
    void falhaQuandoPedidoNaoExiste() {
        when(pedidoRepository.findByIdWithRelacionamentos(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagamentoService.processarPagamento(4L, new PagamentoRequestDTO(new BigDecimal("50.00"))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Pedido não encontrado");
        verify(mockPaymentGateway, never()).processarPagamento(any(), anyString());
    }

    @Test
    @DisplayName("Processa pagamento com idempotencia delegando para RequestDeduplicationService")
    void processaPagamentoComIdempotenciaDelegando() throws Exception {
        String idempotencyKey = "idem-123";
        Long pedidoId = 5L;
        PagamentoRequestDTO request = new PagamentoRequestDTO(new BigDecimal("70.00"));

        UsuarioAutenticado principal = new UsuarioAutenticado(
                10L,
                1L,
                PerfilUsuario.CLIENTE,
                "cliente@teste.com",
                "senha",
                true,
                List.of()
        );
        when(securityContextService.getRequiredPrincipal()).thenReturn(principal);

        String requestJson = "{\"valor\":70.00}";
        when(objectMapper.writeValueAsString(request)).thenReturn(requestJson);

        PagamentoResponseDTO responseBody = PagamentoResponseDTO.builder()
                .pedidoId(pedidoId)
                .aprovado(true)
                .statusPagamento("APROVADO")
                .statusPedido(StatusPedido.CONFIRMADO)
                .transacaoId("tx-789")
                .build();
        IdempotentResponse<PagamentoResponseDTO> expected = new IdempotentResponse<>(responseBody, HttpStatus.OK.value());

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        when(requestDeduplicationService.execute(
                eq(idempotencyKey),
                eq(principal.getId()),
                hashCaptor.capture(),
                eq(PagamentoResponseDTO.class),
                any(),
                eq(HttpStatus.OK.value())
        )).thenReturn(expected);

        IdempotentResponse<PagamentoResponseDTO> resultado = pagamentoService.processarPagamentoComIdempotencia(pedidoId, request, idempotencyKey);

        String expectedHash = sha256(pedidoId + "|" + requestJson);
        assertThat(hashCaptor.getValue()).isEqualTo(expectedHash);
        assertThat(resultado.body().getTransacaoId()).isEqualTo("tx-789");
        assertThat(resultado.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    private Pedido pedidoExemplo(Long id, StatusPedido statusPedido) {
        return Pedido.builder()
                .id(id)
                .loja(Loja.builder().id(1L).build())
                .cliente(Usuario.builder().id(10L).build())
                .statusPedido(statusPedido)
                .build();
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível", ex);
        }
    }
}
