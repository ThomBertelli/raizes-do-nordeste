# Raízes do Nordeste API

API Spring Boot do projeto **Raízes do Nordeste**, com autenticação JWT, JPA, PostgreSQL e migrations via Flyway.

## Requisitos

- Java 21
- PostgreSQL
- Maven Wrapper (`mvnw.cmd` já incluído no projeto)

## Como rodar localmente

### 1. Crie um banco vazio no PostgreSQL

Exemplo de nome usado hoje no projeto:

- `raizes_do_nordeste`

> O banco pode ter outro nome. Basta ajustar a URL em `src/main/resources/application.properties`.

### 2. Ajuste a conexão com o banco

Arquivo: `src/main/resources/application.properties`

Propriedades principais:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`

Exemplo atual:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5455/raizes_do_nordeste
spring.datasource.username=postgres
spring.datasource.password=123456
```

### 3. Suba a aplicação

```powershell
Set-Location "C:\Users\Thomas\Desktop\Nova pasta\raizes_do_nordeste_api"
.\mvnw.cmd spring-boot:run
```

Ou rode os testes de contexto:

```powershell
Set-Location "C:\Users\Thomas\Desktop\Nova pasta\raizes_do_nordeste_api"
.\mvnw.cmd -q test
```

## Como o banco é criado

O projeto usa Flyway com migration inicial em:

- `src/main/resources/db/migration/V1__create_table_usuarios.sql`

Apesar do nome do arquivo, essa migration cria o schema inicial completo do sistema:

- `usuarios`
- `produtos`
- `lojas`
- `pedidos`
- `pagamentos`
- `itens_pedido`
- `fidelidades`
- `estoques`

### Banco vazio

Se o banco estiver vazio, o Flyway executa a `V1` e cria toda a estrutura automaticamente.

### Banco legado já existente

O projeto está com:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

Isso permite adotar Flyway em um banco já existente, marcando o schema atual como versão `1`.

> Recomendação: depois que todos os ambientes legados estiverem baselinados, desligue `spring.flyway.baseline-on-migrate` para evitar baseline automático em banco errado.

## Seed inicial

Na inicialização, a classe `com.raizesdonordeste.config.AdminDataInitializer` garante a criação de um usuário admin caso ainda não exista.

As propriedades usadas são:

```properties
admin.nome=Administrador
admin.email=admin@raizesdonordeste.com
admin.senha=Admin@2026
```

Troque esses valores antes de usar o projeto em ambientes reais.

## Próximas migrations

Como o baseline e a migration inicial estão na versão `1`, a próxima alteração de banco deve ser criada como:

- `V2__descricao_da_mudanca.sql`

Exemplo:

- `V2__adicionar_coluna_telefone_usuarios.sql`

## Observações

- `spring.jpa.hibernate.ddl-auto=none`: o schema não é criado pelo Hibernate.
- O schema deve ser versionado apenas por migrations Flyway.
- Para novos desenvolvedores, o fluxo recomendado é sempre começar com um banco vazio.

