package com.raizesdonordeste.config;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Estoque;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.model.Produto;
import com.raizesdonordeste.domain.model.SaldoFidelidade;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.EstoqueRepository;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.domain.repository.ProdutoRepository;
import com.raizesdonordeste.domain.repository.SaldoFidelidadeRepository;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostmanSeedDataInitializer implements ApplicationRunner {

    private final LojaRepository lojaRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final UsuarioRepository usuarioRepository;
    private final SaldoFidelidadeRepository saldoFidelidadeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${postman.seed.enabled:true}")
    private boolean postmanSeedEnabled;

    @Override
    @Transactional
    @SuppressWarnings("NullableProblems")
    public void run(ApplicationArguments args) {
        if (!postmanSeedEnabled) {
            log.info("Seed Postman desabilitado por propriedade.");
            return;
        }

        Loja matriz = obterOuCriarLoja(
                "Loja Matriz Centro",
                "11222333000181",
                "Rua das Mangueiras, 100 - Centro"
        );
        Loja praia = obterOuCriarLoja(
                "Loja Praia",
                "11222333000182",
                "Avenida Beira Mar, 250 - Praia"
        );

        Produto buchada = obterOuCriarProduto(
                "Buchada de Bode",
                "Prato tipico com preparo tradicional nordestino.",
                new BigDecimal("18.00")
        );
        Produto cuscuz = obterOuCriarProduto(
                "Cuscuz Nordestino",
                "Cuscuz de milho servido com manteiga da terra.",
                new BigDecimal("12.50")
        );
        Produto carneDeSol = obterOuCriarProduto(
                "Carne de Sol na Nata",
                "Carne de sol servida com creme e acompanhamentos.",
                new BigDecimal("32.90")
        );

        obterOuCriarEstoque(matriz, buchada, 30);
        obterOuCriarEstoque(matriz, cuscuz, 40);
        obterOuCriarEstoque(matriz, carneDeSol, 25);
        obterOuCriarEstoque(praia, buchada, 15);
        obterOuCriarEstoque(praia, cuscuz, 20);
        obterOuCriarEstoque(praia, carneDeSol, 10);

        Usuario gerenciaMatriz = obterOuCriarUsuario(
                "Gerencia Matriz",
                "gerenciamatriz@raizesnordeste.com.br",
                "Gerencia@2026",
                PerfilUsuario.GERENCIA_MATRIZ,
                null,
                false
        );
        Usuario gerente = obterOuCriarUsuario(
                "Gerente Centro",
                "gerente.centro@raizesnordeste.com.br",
                "Gerente@2026",
                PerfilUsuario.GERENTE,
                matriz,
                false
        );
        Usuario funcionario = obterOuCriarUsuario(
                "Funcionario Centro",
                "funcionario.centro@raizesnordeste.com.br",
                "Funcionario@2026",
                PerfilUsuario.FUNCIONARIO,
                matriz,
                false
        );
        Usuario cliente = obterOuCriarUsuario(
                "Cliente Postman",
                "cliente.postman@raizes.com",
                "Cliente@2026",
                PerfilUsuario.CLIENTE,
                null,
                true
        );

        obterOuCriarSaldo(cliente, new BigDecimal("25.00"));

        log.info(
                """
                Seed Postman pronto.
                Lojas: matrizId={}, praiaId={}
                Produtos: buchadaId={}, cuscuzId={}, carneDeSolId={}
                Usuarios: gerenciaMatrizId={}, gerenteId={}, funcionarioId={}, clienteId={}
                Credenciais:
                - gerenciamatriz@raizesnordeste.com.br / Gerencia@2026
                - gerente.centro@raizesnordeste.com.br / Gerente@2026
                - funcionario.centro@raizesnordeste.com.br / Funcionario@2026
                - cliente.postman@raizes.com / Cliente@2026
                """,
                matriz.getId(),
                praia.getId(),
                buchada.getId(),
                cuscuz.getId(),
                carneDeSol.getId(),
                gerenciaMatriz.getId(),
                gerente.getId(),
                funcionario.getId(),
                cliente.getId()
        );
    }

    private Loja obterOuCriarLoja(String nome, String cnpj, String endereco) {
        return lojaRepository.findByCnpj(cnpj)
                .map(existente -> atualizarLoja(existente, nome, endereco))
                .orElseGet(() -> lojaRepository.save(Loja.builder()
                        .nome(nome)
                        .cnpj(cnpj)
                        .endereco(endereco)
                        .ativa(true)
                        .build()));
    }

    private Loja atualizarLoja(Loja loja, String nome, String endereco) {
        loja.setNome(nome);
        loja.setEndereco(endereco);
        loja.setAtiva(true);
        return lojaRepository.save(loja);
    }

    private Produto obterOuCriarProduto(String nome, String descricao, BigDecimal preco) {
        return produtoRepository.findByNome(nome)
                .map(existente -> atualizarProduto(existente, descricao, preco))
                .orElseGet(() -> produtoRepository.save(Produto.builder()
                        .nome(nome)
                        .descricao(descricao)
                        .preco(preco)
                        .ativo(true)
                        .build()));
    }

    private Produto atualizarProduto(Produto produto, String descricao, BigDecimal preco) {
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produtoRepository.save(produto);
    }

    private Estoque obterOuCriarEstoque(Loja loja, Produto produto, int quantidade) {
        return estoqueRepository.findByLojaIdAndProdutoId(loja.getId(), produto.getId())
                .map(existente -> atualizarEstoque(existente, quantidade))
                .orElseGet(() -> estoqueRepository.save(Estoque.builder()
                        .loja(loja)
                        .produto(produto)
                        .quantidade(quantidade)
                        .versao(0L)
                        .build()));
    }

    private Estoque atualizarEstoque(Estoque estoque, int quantidade) {
        estoque.setQuantidade(quantidade);
        return estoqueRepository.save(estoque);
    }

    private Usuario obterOuCriarUsuario(String nome,
                                        String email,
                                        String senha,
                                        PerfilUsuario perfil,
                                        Loja loja,
                                        boolean consentimentoProgramaFidelidade) {
        return usuarioRepository.findByEmail(email)
                .map(existente -> atualizarUsuario(existente, nome, senha, perfil, loja, consentimentoProgramaFidelidade))
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .nome(nome)
                        .email(email)
                        .senha(passwordEncoder.encode(senha))
                        .perfil(perfil)
                        .loja(loja)
                        .ativo(true)
                        .consentimentoProgramaFidelidade(consentimentoProgramaFidelidade)
                        .build()));
    }

    private Usuario atualizarUsuario(Usuario usuario,
                                     String nome,
                                     String senha,
                                     PerfilUsuario perfil,
                                     Loja loja,
                                     boolean consentimentoProgramaFidelidade) {
        usuario.setNome(nome);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setLoja(loja);
        usuario.setAtivo(true);
        usuario.setConsentimentoProgramaFidelidade(consentimentoProgramaFidelidade);
        return usuarioRepository.save(usuario);
    }

    private SaldoFidelidade obterOuCriarSaldo(Usuario usuario, BigDecimal moedas) {
        return saldoFidelidadeRepository.findByUsuarioId(usuario.getId())
                .map(existente -> atualizarSaldo(existente, moedas))
                .orElseGet(() -> saldoFidelidadeRepository.save(SaldoFidelidade.builder()
                        .usuario(usuario)
                        .moedas(moedas)
                        .version(0L)
                        .build()));
    }

    private SaldoFidelidade atualizarSaldo(SaldoFidelidade saldo, BigDecimal moedas) {
        saldo.setMoedas(moedas);
        return saldoFidelidadeRepository.save(saldo);
    }
}
