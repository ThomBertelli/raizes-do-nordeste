# Raízes do Nordeste API

API moderna para gestão de lojas, produtos, pedidos, pagamentos e programa de fidelidade, desenvolvida em Spring Boot, com autenticação JWT, JPA, PostgreSQL e migrations via Flyway.

## Requisitos

- Java 21
- PostgreSQL
- Maven Wrapper (`mvnw.cmd` já incluído)

## Como rodar localmente

1. **Crie um banco vazio no PostgreSQL**
   - Exemplo de nome: `raizes_do_nordeste`
   - Ajuste a URL em `src/main/resources/application.properties` se necessário.

2. **Configure a conexão**
   - Edite `src/main/resources/application.properties`:
     ```properties
     spring.datasource.url=jdbc:postgresql://localhost:5455/raizes_do_nordeste
     spring.datasource.username=postgres
     spring.datasource.password=123456
     ```

3. **Suba a aplicação**
   ```powershell
   Set-Location "C:\Users\Thomas\Desktop\Nova pasta\raizes_do_nordeste_api"
   .\mvnw.cmd spring-boot:run
   ```

4. **Testes**
   ```powershell
   .\mvnw.cmd -q test
   ```

## Features principais

- Cadastro e autenticação de usuários (JWT)
- Perfis: ADMIN, GERENCIA_MATRIZ, GERENTE, FUNCIONARIO, CLIENTE
- Gestão de lojas (CRUD, ativação/desativação)
- Gestão de produtos (CRUD, ativação/desativação, busca por nome, descrição, faixa de preço)
- Gestão de estoques por loja/produto, movimentação de entrada/saída
- Pedidos: criação, listagem, detalhamento, histórico do cliente
- Pagamentos integrados (mock)
- Programa de fidelidade: acúmulo e resgate de moedas/pontos
- Controle de permissões por perfil
- Migrations Flyway e seed automático de admin

## Regras de negócio

- **Usuários**:
  - ADMIN: pode criar outros ADMIN, GERENCIA_MATRIZ, GERENTE, FUNCIONARIO
  - GERENCIA_MATRIZ: pode criar GERENCIA_MATRIZ, GERENTE, FUNCIONARIO
  - GERENTE: pode criar FUNCIONARIO vinculado à sua loja
  - FUNCIONARIO: não pode criar usuários
  - CLIENTE: só pode se cadastrar via endpoint público
  - Só ADMIN pode criar outro ADMIN
  - Nenhum perfil pode criar CLIENTE via API autenticada
  - GERENTE/FUNCIONARIO obrigatoriamente vinculados a uma loja

- **Acesso aos recursos**:
  - Endpoints de lojas/produtos/estoques: restritos a perfis administrativos
  - Pedidos:
    - CLIENTE: pode criar e listar apenas seus próprios pedidos
    - FUNCIONARIO/GERENTE: podem criar pedidos para clientes e listar pedidos da própria loja
    - GERENCIA_MATRIZ: pode listar pedidos de qualquer loja
  - Estoque: movimentação apenas por GERENTE/GERENCIA_MATRIZ

- **Produtos**:
  - CRUD restrito a ADMIN, GERENTE, GERENCIA_MATRIZ
  - Busca pública por nome, descrição, faixa de preço

- **Fidelidade**:
  - CLIENTE pode aderir ao programa no cadastro
  - Acúmulo automático de moedas a cada pedido pago
  - Resgate de moedas como desconto em pedidos
  - Saldo de moedas controlado por usuário
  - Não clientes não participam do programa

- **Regras de validação**:
  - Quantidade de estoque não pode ser negativa
  - Não é possível movimentar estoque com quantidade <= 0
  - Não é possível criar pedido com estoque insuficiente
  - Consentimento de fidelidade obrigatório para CLIENTE

## Perfis e regras de acesso

- **ADMIN**: acesso total, exceto criar CLIENTE
- **GERENCIA_MATRIZ**: gerencia todas as lojas, cria GERENTE/FUNCIONARIO, movimenta estoque de qualquer loja
- **GERENTE**: gerencia sua loja, cria FUNCIONARIO, movimenta estoque da própria loja
- **FUNCIONARIO**: pode criar pedidos para clientes na loja vinculada
- **CLIENTE**: pode se cadastrar, criar e listar seus próprios pedidos, aderir ao programa de fidelidade

## Endpoints principais

- `/auth/login` — Login (público)
- `/auth/cadastro` — Cadastro de cliente (público)
- `/api/usuarios` — CRUD de usuários (restrito)
- `/api/lojas` — CRUD de lojas (restrito)
- `/api/produtos` — CRUD e busca de produtos
- `/api/estoques` — Movimentação e consulta de estoques
- `/api/pedidos` — Criação e consulta de pedidos
- `/pagamentos` — Pagamentos de pedidos

## Observações

- O schema do banco é criado e versionado via Flyway.
- O projeto já cria um usuário admin padrão na inicialização (troque a senha antes de usar em produção).
- Para novos desenvolvedores, recomenda-se sempre começar com banco vazio.

Consulte a documentação dos endpoints (Swagger/OpenAPI) para detalhes de payloads e respostas.
