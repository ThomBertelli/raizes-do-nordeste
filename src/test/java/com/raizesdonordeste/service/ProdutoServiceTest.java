package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.produto.ProdutoUpdateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoCreateDTO;
import com.raizesdonordeste.api.dto.produto.ProdutoResponseDTO;
import com.raizesdonordeste.domain.model.Produto;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProdutoService Tests")
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private ProdutoService produtoService;

    // -------------------------------------------------------------------------
    // Testes de Criação - Casos Positivos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve criar produto com sucesso")
    void deveCriarProdutoComSucesso() {
        // Arrange
        ProdutoCreateDTO dto = novoProdutoCriacaoDTO();
        when(produtoRepository.existsByNome(dto.getNome())).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
            Produto p = inv.getArgument(0, Produto.class);
            p.setId(1L);
            p.setDataCriacao(LocalDateTime.now());
            p.setDataAtualizacao(LocalDateTime.now());
            return p;
        });

        ProdutoResponseDTO resposta = produtoService.criar(dto);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getId()).isEqualTo(1L);
        assertThat(resposta.getNome()).isEqualTo("Produto Teste");
        assertThat(resposta.getDescricao()).isEqualTo("Descrição do Produto");
        assertThat(resposta.getPreco()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        assertThat(resposta.isAtivo()).isTrue();
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    @DisplayName("Produto deve ser criado como ativo por padrão")
    void produtoDeveSerciadoComoAtivoPorPadrao() {
        // Arrange
        ProdutoCreateDTO dto = novoProdutoCriacaoDTO();
        when(produtoRepository.existsByNome(anyString())).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
            Produto p = inv.getArgument(0, Produto.class);
            p.setId(1L);
            return p;
        });

        // Act
        ProdutoResponseDTO resposta = produtoService.criar(dto);

        // Assert
        assertThat(resposta.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve criar produto com preço válido")
    void deveCriarProdutoComPrecoValido() {
        // Arrange
        BigDecimal preco = BigDecimal.valueOf(50.00);
        ProdutoCreateDTO dto = new ProdutoCreateDTO("Produto Premium", "Descrição", preco);
        when(produtoRepository.existsByNome(anyString())).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
            Produto p = inv.getArgument(0, Produto.class);
            p.setId(1L);
            return p;
        });

        // Act
        ProdutoResponseDTO resposta = produtoService.criar(dto);

        // Assert
        assertThat(resposta.getPreco()).isEqualByComparingTo(preco);
        verify(produtoRepository).save(any(Produto.class));
    }

    // -------------------------------------------------------------------------
    // Testes de Criação - Casos Negativos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Não deve criar produto com nome duplicado")
    void naoDeveCriarProdutoComNomeDuplicado() {
        // Arrange
        ProdutoCreateDTO dto = novoProdutoCriacaoDTO();
        when(produtoRepository.existsByNome(dto.getNome())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> produtoService.criar(dto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Nome de produto já cadastrado");

        verify(produtoRepository, never()).save(any());
    }


    // -------------------------------------------------------------------------
    // Testes de Atualização - Casos Positivos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve atualizar produto com sucesso")
    void deveAtualizarProdutoComSucesso() {
        // Arrange
        Produto produtoExistente = produtoExistente();
        ProdutoUpdateDTO dto = new ProdutoUpdateDTO("Novo Nome", "Nova Descrição", BigDecimal.valueOf(150.00));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));
        when(produtoRepository.existsByNome("Novo Nome")).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0, Produto.class));

        // Act
        ProdutoResponseDTO resposta = produtoService.atualizar(1L, dto);

        // Assert
        assertThat(resposta.getNome()).isEqualTo("Novo Nome");
        assertThat(resposta.getDescricao()).isEqualTo("Nova Descrição");
        assertThat(resposta.getPreco()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    @DisplayName("Deve atualizar apenas o nome do produto")
    void deveAtualizarApenasNome() {
        // Arrange
        Produto produtoExistente = produtoExistente();
        ProdutoUpdateDTO dto = new ProdutoUpdateDTO("Novo Nome", null, null);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));
        when(produtoRepository.existsByNome("Novo Nome")).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0, Produto.class));

        // Act
        ProdutoResponseDTO resposta = produtoService.atualizar(1L, dto);

        // Assert
        assertThat(resposta.getNome()).isEqualTo("Novo Nome");
        assertThat(resposta.getDescricao()).isEqualTo("Descrição Original");
        assertThat(resposta.getPreco()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    @DisplayName("Deve atualizar apenas o preço do produto")
    void deveAtualizarApenasPreco() {
        // Arrange
        Produto produtoExistente = produtoExistente();
        ProdutoUpdateDTO dto = new ProdutoUpdateDTO(null, null, BigDecimal.valueOf(199.99));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produtoExistente));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0, Produto.class));

        // Act
        ProdutoResponseDTO resposta = produtoService.atualizar(1L, dto);

        // Assert
        assertThat(resposta.getNome()).isEqualTo("Produto Original");
        assertThat(resposta.getPreco()).isEqualByComparingTo(BigDecimal.valueOf(199.99));
        verify(produtoRepository).save(any(Produto.class));
    }

    // -------------------------------------------------------------------------
    // Testes de Atualização - Casos Negativos
    // -------------------------------------------------------------------------


    @Test
    @DisplayName("Não deve atualizar produto inexistente")
    void naoDeveAtualizarProdutoInexistente() {
        // Arrange
        ProdutoUpdateDTO dto = new ProdutoUpdateDTO("Novo Nome", null, null);
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produtoService.atualizar(99L, dto))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Produto não encontrado");

        verify(produtoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Testes de Busca - Casos Positivos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve buscar produto por ID com sucesso")
    void deveBuscarProdutoPorIdComSucesso() {
        // Arrange
        Produto produto = produtoExistente();
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        // Act
        ProdutoResponseDTO resposta = produtoService.buscarPorId(1L);

        // Assert
        assertThat(resposta).isNotNull();
        assertThat(resposta.getId()).isEqualTo(1L);
        assertThat(resposta.getNome()).isEqualTo("Produto Original");
    }

    @Test
    @DisplayName("Deve listar todos os produtos")
    void deveListarTodosProdutos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        List<Produto> produtos = List.of(produtoExistente());
        Page<Produto> page = new PageImpl<>(produtos, pageable, 1);
        when(produtoRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<ProdutoResponseDTO> resposta = produtoService.listarTodos(pageable);

        // Assert
        assertThat(resposta).isNotEmpty();
        assertThat(resposta.getTotalElements()).isEqualTo(1);
        assertThat(resposta.getContent().get(0).getNome()).isEqualTo("Produto Original");
    }

    @Test
    @DisplayName("Deve buscar produtos ativos")
    void deveBuscarProdutosAtivos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Produto produto = produtoExistente();
        produto.setAtivo(true);
        List<Produto> produtos = List.of(produto);
        Page<Produto> page = new PageImpl<>(produtos, pageable, 1);
        when(produtoRepository.findByAtivo(true, pageable)).thenReturn(page);

        // Act
        Page<ProdutoResponseDTO> resposta = produtoService.buscarAtivos(pageable);

        // Assert
        assertThat(resposta).isNotEmpty();
        assertThat(resposta.getContent().get(0).isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve buscar produtos por nome")
    void deveBuscarProdutosPorNome() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Produto produto = produtoExistente();
        List<Produto> produtos = List.of(produto);
        Page<Produto> page = new PageImpl<>(produtos, pageable, 1);
        when(produtoRepository.findByNomeContainingIgnoreCase("Original", pageable)).thenReturn(page);

        // Act
        Page<ProdutoResponseDTO> resposta = produtoService.buscarPorNome("Original", pageable);

        // Assert
        assertThat(resposta).isNotEmpty();
        assertThat(resposta.getContent().get(0).getNome()).isEqualTo("Produto Original");
    }

    @Test
    @DisplayName("Deve buscar produtos por descrição")
    void deveBuscarProdutosPorDescricao() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Produto produto = produtoExistente();
        List<Produto> produtos = List.of(produto);
        Page<Produto> page = new PageImpl<>(produtos, pageable, 1);
        when(produtoRepository.findByDescricaoContainingIgnoreCase("Descrição", pageable)).thenReturn(page);

        // Act
        Page<ProdutoResponseDTO> resposta = produtoService.buscarPorDescricao("Descrição", pageable);

        // Assert
        assertThat(resposta).isNotEmpty();
        assertThat(resposta.getContent().get(0).getDescricao()).contains("Descrição");
    }

    @Test
    @DisplayName("Deve buscar produtos por faixa de preço")
    void deveBuscarProdutosPorFaixaDePreco() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Produto produto = produtoExistente();
        List<Produto> produtos = List.of(produto);
        Page<Produto> page = new PageImpl<>(produtos, pageable, 1);
        BigDecimal min = BigDecimal.valueOf(50.00);
        BigDecimal max = BigDecimal.valueOf(150.00);
        when(produtoRepository.findByPrecoBetween(min, max, pageable)).thenReturn(page);

        // Act
        Page<ProdutoResponseDTO> resposta = produtoService.buscarPorFaixaDePreco(min, max, pageable);

        // Assert
        assertThat(resposta).isNotEmpty();
        assertThat(resposta.getContent().get(0).getPreco()).isBetween(min, max);
    }

    // -------------------------------------------------------------------------
    // Testes de Busca - Casos Negativos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve lançar exceção ao buscar produto inexistente por ID")
    void deveLancarExcecaoAoBuscarProdutoInexistente() {
        // Arrange
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produtoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Produto não encontrado");
    }

    @Test
    @DisplayName("Deve retornar página vazia ao buscar produtos por nome inexistente")
    void deveRetornarPaginaVaziaAoBuscarProdutoInexistente() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Produto> page = Page.empty(pageable);
        when(produtoRepository.findByNomeContainingIgnoreCase("Inexistente", pageable)).thenReturn(page);

        // Act
        Page<ProdutoResponseDTO> resposta = produtoService.buscarPorNome("Inexistente", pageable);

        // Assert
        assertThat(resposta).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Testes de Ativação/Desativação
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve ativar produto com sucesso")
    void deveAtivarProdutoComSucesso() {
        // Arrange
        Produto produto = produtoExistente();
        produto.setAtivo(false);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0, Produto.class));

        // Act
        produtoService.ativar(1L);

        // Assert
        assertThat(produto.isAtivo()).isTrue();
        verify(produtoRepository).save(produto);
    }

    @Test
    @DisplayName("Deve desativar produto com sucesso")
    void deveDesativarProdutoComSucesso() {
        // Arrange
        Produto produto = produtoExistente();
        produto.setAtivo(true);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0, Produto.class));

        // Act
        produtoService.desativar(1L);

        // Assert
        assertThat(produto.isAtivo()).isFalse();
        verify(produtoRepository).save(produto);
    }

    @Test
    @DisplayName("Não deve ativar produto inexistente")
    void naoDeveAtivarProdutoInexistente() {
        // Arrange
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produtoService.ativar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(produtoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve desativar produto inexistente")
    void naoDeveDesativarProdutoInexistente() {
        // Arrange
        when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> produtoService.desativar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(produtoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Testes de Deleção
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve deletar produto com sucesso")
    void deveDeletarProdutoComSucesso() {
        // Arrange
        when(produtoRepository.existsById(1L)).thenReturn(true);

        // Act
        produtoService.deletar(1L);

        // Assert
        verify(produtoRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Não deve deletar produto inexistente")
    void naoDeveDeletarProdutoInexistente() {
        // Arrange
        when(produtoRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> produtoService.deletar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Produto não encontrado");

        verify(produtoRepository, never()).deleteById(anyLong());
    }

    // -------------------------------------------------------------------------
    // Testes de Integridade e Validações
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Deve manter mesmo nome ao atualizar sem fornecer novo nome")
    void deveManterMesmoNomeAoAtualizarSemFornecerNovoNome() {
        // Arrange
        Produto produto = produtoExistente();
        ProdutoUpdateDTO dto = new ProdutoUpdateDTO(null, "Nova Desc", null);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0, Produto.class));

        // Act
        ProdutoResponseDTO resposta = produtoService.atualizar(1L, dto);

        // Assert
        assertThat(resposta.getNome()).isEqualTo("Produto Original");
        assertThat(resposta.getDescricao()).isEqualTo("Nova Desc");
    }

    @Test
    @DisplayName("Deve validar que campos nulos não afetam atualização")
    void deveValidarQueNulosNaoAfetamAtualizacao() {
        // Arrange
        Produto produto = produtoExistente();
        ProdutoUpdateDTO dto = new ProdutoUpdateDTO(null, null, null);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0, Produto.class));

        // Act
        ProdutoResponseDTO resposta = produtoService.atualizar(1L, dto);

        // Assert
        assertThat(resposta.getNome()).isEqualTo("Produto Original");
        assertThat(resposta.getDescricao()).isEqualTo("Descrição Original");
        assertThat(resposta.getPreco()).isEqualByComparingTo(BigDecimal.valueOf(99.99));
    }

    @Test
    @DisplayName("Deve converter Produto para DTO corretamente")
    void deveConverterProdutoParaDTOCorretamente() {
        // Arrange
        Produto produto = produtoExistente();
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        // Act
        ProdutoResponseDTO resposta = produtoService.buscarPorId(1L);

        // Assert
        assertThat(resposta.getId()).isEqualTo(produto.getId());
        assertThat(resposta.getNome()).isEqualTo(produto.getNome());
        assertThat(resposta.getDescricao()).isEqualTo(produto.getDescricao());
        assertThat(resposta.getPreco()).isEqualByComparingTo(produto.getPreco());
        assertThat(resposta.isAtivo()).isEqualTo(produto.isAtivo());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProdutoCreateDTO novoProdutoCriacaoDTO() {
        return new ProdutoCreateDTO(
                "Produto Teste",
                "Descrição do Produto",
                BigDecimal.valueOf(99.99)
        );
    }

    private Produto produtoExistente() {
        return Produto.builder()
                .id(1L)
                .nome("Produto Original")
                .descricao("Descrição Original")
                .preco(BigDecimal.valueOf(99.99))
                .ativo(true)
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();
    }
}

