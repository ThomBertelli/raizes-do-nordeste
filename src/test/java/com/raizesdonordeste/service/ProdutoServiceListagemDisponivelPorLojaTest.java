package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.produto.ProdutoDisponivelLojaResponseDTO;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProdutoService - disponibilidade por loja")
class ProdutoServiceListagemDisponivelPorLojaTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private EstoqueRepository estoqueRepository;

    @Mock
    private LojaRepository lojaRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private ProdutoService produtoService;

    @Test
    @DisplayName("Deve listar produtos disponíveis para compra em uma loja específica")
    void deveListarProdutosDisponiveisPorLoja() {
        Pageable pageable = PageRequest.of(0, 20);
        Loja loja = lojaAtiva(1L);
        Estoque estoque = estoqueDisponivel(loja, produtoAtivo(10L, "Bolo de Rolo"), 7);
        Page<Estoque> pagina = new PageImpl<>(List.of(estoque), pageable, 1);

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(estoqueRepository.findProdutosDisponiveisParaVenda(1L, pageable)).thenReturn(pagina);

        Page<ProdutoDisponivelLojaResponseDTO> resposta = produtoService.listarDisponiveisPorLoja(1L, null, pageable);

        assertThat(resposta.getTotalElements()).isEqualTo(1);
        assertThat(resposta.getContent().get(0).getNome()).isEqualTo("Bolo de Rolo");
        assertThat(resposta.getContent().get(0).getPreco()).isEqualByComparingTo("19.90");
        verify(estoqueRepository).findProdutosDisponiveisParaVenda(1L, pageable);
    }

    @Test
    @DisplayName("Deve filtrar produtos disponíveis por nome dentro da loja")
    void deveFiltrarProdutosDisponiveisPorNome() {
        Pageable pageable = PageRequest.of(0, 20);
        Loja loja = lojaAtiva(1L);

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));
        when(estoqueRepository.findProdutosDisponiveisParaVendaPorNome(1L, "bolo", pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProdutoDisponivelLojaResponseDTO> resposta = produtoService.listarDisponiveisPorLoja(1L, "bolo", pageable);

        assertThat(resposta).isEmpty();
        verify(estoqueRepository).findProdutosDisponiveisParaVendaPorNome(1L, "bolo", pageable);
    }

    @Test
    @DisplayName("Não deve listar produtos de loja inexistente")
    void naoDeveListarProdutosDeLojaInexistente() {
        Pageable pageable = PageRequest.of(0, 20);
        when(lojaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.listarDisponiveisPorLoja(99L, null, pageable))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Loja não encontrada");
    }

    @Test
    @DisplayName("Não deve listar produtos de loja inativa")
    void naoDeveListarProdutosDeLojaInativa() {
        Pageable pageable = PageRequest.of(0, 20);
        Loja loja = lojaAtiva(1L);
        loja.setAtiva(false);

        when(lojaRepository.findById(1L)).thenReturn(Optional.of(loja));

        assertThatThrownBy(() -> produtoService.listarDisponiveisPorLoja(1L, null, pageable))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não está ativa");
    }

    private Loja lojaAtiva(Long id) {
        return Loja.builder()
                .id(id)
                .nome("Loja Centro")
                .cnpj("00.000.000/0001-00")
                .endereco("Rua A, 1")
                .ativa(true)
                .build();
    }

    private Produto produtoAtivo(Long id, String nome) {
        return Produto.builder()
                .id(id)
                .nome(nome)
                .descricao("Descricao")
                .preco(BigDecimal.valueOf(19.90))
                .ativo(true)
                .build();
    }

    private Estoque estoqueDisponivel(Loja loja, Produto produto, Integer quantidade) {
        return Estoque.builder()
                .id(1L)
                .loja(loja)
                .produto(produto)
                .quantidade(quantidade)
                .versao(1L)
                .build();
    }
}
