# Raízes do Nordeste API

API REST para gestão de lojas, produtos, estoque, usuários, pedidos, pagamentos e programa de fidelidade. O projeto foi construído com Spring Boot, Spring Security, JWT, JPA, Flyway e PostgreSQL.

## O que este projeto entrega

- Autenticação com JWT
- Controle de acesso por perfil
- CRUD de lojas
- CRUD de produtos
- Gestão de estoque por loja
- Catálogo público de produtos disponíveis por loja
- Criação e acompanhamento de pedidos
- Pagamento mock de pedidos
- Programa de fidelidade com acúmulo, resgate, saldo, extrato e taxa de conversão configurável
- Migrations com Flyway
- Seed automático de usuários e dados para testes manuais
- Documentação Swagger/OpenAPI

## Stack

- Java 21
- Spring Boot 4
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Maven Wrapper

## Funcionalidades principais

### Autenticação e perfis

- Login em `/auth/login`
- Cadastro público de cliente em `/auth/cadastro`
- Perfis disponíveis:
  - `ADMIN`
  - `GERENCIA_MATRIZ`
  - `GERENTE`
  - `FUNCIONARIO`
  - `CLIENTE`

### Fidelidade

- O cliente pode entrar no programa de fidelidade no momento do cadastro
- Pedidos pagos podem acumular moedas automaticamente
- O cliente pode usar moedas como desconto ao criar pedidos
- O cliente pode consultar saldo em `/api/fidelidade/saldo`
- O cliente pode consultar extrato em `/api/fidelidade/extrato`
- `GERENCIA_MATRIZ` pode consultar e alterar a taxa de conversão em `/api/fidelidade/taxa`
- Existe persistência separada para saldo, transações e configuração de fidelidade

### Pedidos e pagamentos

- Cliente pode criar e consultar os próprios pedidos
- Funcionário e gerente podem operar pedidos da loja
- Pagamento é processado pelo endpoint `/pagamentos/{pedidoId}`
- Criação de pedido e pagamento suportam `Idempotency-Key`

### Estoque

- Consulta de estoque por loja
- Histórico de movimentações
- Entrada e saída de estoque
- Proteções contra estoque negativo e movimentações inválidas

### Catálogo por loja

- O cliente pode consultar produtos disponíveis para compra em uma loja específica
- A listagem pública considera apenas loja ativa
- A listagem pública considera apenas produto ativo
- Produtos sem estoque ou não trabalhados pela loja não aparecem
- A resposta da vitrine não expõe estoque da loja nem identificadores internos do produto
- Endpoint público disponível em `/api/lojas/{id}/produtos-disponiveis`

## Regras de negócio importantes

- Apenas `ADMIN` pode criar outro `ADMIN`
- `GERENCIA_MATRIZ` pode operar em qualquer loja
- `GERENTE` atua na própria loja
- `FUNCIONARIO` não cria usuários
- `CLIENTE` se cadastra apenas pelo endpoint público
- `GERENTE` e `FUNCIONARIO` precisam estar vinculados a uma loja
- Não é possível criar pedido com estoque insuficiente
- Não é possível movimentar estoque com quantidade menor ou igual a zero
- Apenas clientes participam do programa de fidelidade

## Estrutura básica do projeto

```text
src/main/java       codigo fonte
src/main/resources  configuracoes e migrations Flyway
postman/            colecoes e apoio para testes manuais
.mvn/               wrapper do Maven
```

## Passo a passo para rodar localmente

Esta seção foi escrita para quem clonar o projeto do zero, inclusive sem experiência prévia com Java.

### 1. Instale os pré-requisitos

Você precisa ter instalado na máquina:

- Java 21
- PostgreSQL

Para validar:

```powershell
java -version
psql --version
```

### 2. Clone o projeto

```powershell
git clone <url-do-repositorio>
cd raizes_do_nordeste_api
```

### 3. Crie o banco de dados

Crie um banco vazio no PostgreSQL. Exemplo:

```sql
CREATE DATABASE raizes_do_nordeste;
```

Se você usa outra porta, nome de banco, usuário ou senha, tudo bem. Só precisa refletir isso no arquivo de configuração.

### 4. Crie o arquivo `application.properties`

Depois edite os valores conforme o seu ambiente.

Exemplo mínimo:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5455/raizes_do_nordeste
spring.datasource.username=postgres
spring.datasource.password=123456

server.port=8080

jwt.secret=troque-esta-chave-por-uma-chave-longa-e-segura
jwt.expiration=86400000

admin.nome=Administrador
admin.email=admin@raizesdonordeste.com
admin.senha=Admin@2026

postman.seed.enabled=true
```

### 5. Suba a aplicação

Na raiz do projeto, rode:

```powershell
.\mvnw.cmd spring-boot:run
```

O Maven Wrapper já vem no projeto, então você não precisa instalar Maven manualmente.

Quando a aplicação subir:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 6. Entenda o que acontece na primeira subida

Na inicialização, o projeto:

- Executa as migrations do Flyway
- Cria um usuário admin padrão se ainda não existir `ADMIN`
- Pode popular lojas, produtos, estoques e usuários de apoio para Postman quando `postman.seed.enabled=true`

### 7. Rode os testes

```powershell
.\mvnw.cmd test
```

## Usuários e seeds úteis

Se `postman.seed.enabled=true`, a aplicação cria dados de apoio para testes manuais.

### Admin inicial

Criado automaticamente se ainda não existir nenhum `ADMIN`:

- Email: `admin@raizesdonordeste.com`
- Senha: `Admin@2026`

Esses valores vêm do `application.properties` e devem ser alterados antes de qualquer uso real.

### Usuários de seed para testes

- `gerenciamatriz@raizesnordeste.com.br` / `Gerencia@2026`
- `gerente.centro@raizesnordeste.com.br` / `Gerente@2026`
- `funcionario.centro@raizesnordeste.com.br` / `Funcionario@2026`
- `cliente.postman@raizes.com` / `Cliente@2026`

## Fluxo rápido para testar a API

### 1. Faça login

Use `POST /auth/login` com:

```json
{
  "email": "cliente.postman@raizes.com",
  "senha": "Cliente@2026"
}
```

Copie o token JWT retornado.

### 2. Autorize no Swagger

No Swagger UI:

1. Clique em `Authorize`
2. Cole o token no formato `Bearer SEU_TOKEN`
3. Confirme

### 3. Consulte dados públicos

Exemplos:

- `GET /api/lojas`
- `GET /api/lojas/1/produtos-disponiveis`
- `GET /api/produtos`
- `GET /api/produtos/ativos`

Esse endpoint de loja é o recomendado para montar a vitrine do cliente sem exibir itens indisponíveis.
Ele foi pensado para experiência de compra e retorna apenas dados públicos do produto.

### 4. Crie um pedido usando fidelidade se quiser

Exemplo de payload:

```json
{
  "lojaId": 1,
  "canalPedido": "APP",
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 1
    }
  ],
  "moedasFidelidade": 5.00
}
```

### 5. Pague o pedido

Exemplo:

```json
{
  "valor": 13.00
}
```

Envie esse corpo para `POST /pagamentos/{pedidoId}`.

### 6. Consulte o saldo e o extrato

- `GET /api/fidelidade/saldo`
- `GET /api/fidelidade/extrato`

## Endpoints principais

### Públicos

- `POST /auth/login`
- `POST /auth/cadastro`
- `GET /api/lojas`
- `GET /api/lojas/{id}`
- `GET /api/lojas/{id}/produtos-disponiveis`
- `GET /api/lojas/ativas`
- `GET /api/lojas/buscar`
- `GET /api/produtos`
- `GET /api/produtos/{id}`
- `GET /api/produtos/ativos`
- `GET /api/produtos/buscar`
- `GET /api/produtos/buscar-descricao`
- `GET /api/produtos/faixa-preco`

### Autenticados

- `POST /api/usuarios`
- `GET /api/usuarios`
- `GET /api/usuarios/{id}`
- `PUT /api/usuarios/{id}`
- `PATCH /api/usuarios/{id}/ativar`
- `PATCH /api/usuarios/{id}/desativar`
- `DELETE /api/usuarios/{id}`
- `POST /api/lojas`
- `PUT /api/lojas/{id}`
- `PATCH /api/lojas/{id}/ativar`
- `PATCH /api/lojas/{id}/desativar`
- `DELETE /api/lojas/{id}`
- `POST /api/produtos`
- `PUT /api/produtos/{id}`
- `PATCH /api/produtos/{id}/ativar`
- `PATCH /api/produtos/{id}/desativar`
- `DELETE /api/produtos/{id}`
- `GET /api/estoques`
- `GET /api/estoques/movimentacoes`
- `POST /api/estoques/entrada`
- `POST /api/estoques/saida`
- `POST /api/pedidos`
- `GET /api/pedidos`
- `GET /api/pedidos/{id}`
- `GET /api/pedidos/meus`
- `PUT /api/pedidos/{id}/status`
- `POST /pagamentos/{pedidoId}`
- `GET /api/fidelidade/saldo`
- `GET /api/fidelidade/extrato`
- `GET /api/fidelidade/taxa`
- `PATCH /api/fidelidade/taxa`


## Observações para outros devs

- O projeto usa Flyway, então o banco deve estar acessível antes de subir a aplicação
- O Swagger está liberado sem autenticação
- CORS está preparado para `http://localhost:3000`, `http://localhost:4200` e `http://localhost:8081`
- O pagamento é mock, então não existe integração real com gateway externo
- Se quiser subir a aplicação sem dados de apoio, defina `postman.seed.enabled=false`
- Para a vitrine do cliente, prefira `GET /api/lojas/{id}/produtos-disponiveis` em vez do catálogo global de produtos
- Esse endpoint de vitrine filtra por estoque internamente, mas não expõe quantidade disponível nem `produtoId`

## Próximo passo recomendado

Depois de clonar:

1. criar `application.properties` a partir do arquivo exemplo
2. subir o PostgreSQL
3. rodar `.\mvnw.cmd spring-boot:run`
4. abrir o Swagger
5. fazer login com um usuário de seed
