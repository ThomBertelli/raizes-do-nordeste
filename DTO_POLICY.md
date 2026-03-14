# DTO Policy

## Objetivo
Padronizar o contrato da API por tipo de DTO para evitar vazamento de dados sensiveis e reduzir riscos de seguranca.

## 1) Request DTO
- Aceitar apenas campos enviados pelo cliente para a acao do endpoint.
- Nao aceitar campos de controle interno: `id`, timestamps, flags de auditoria, estado interno de seguranca.
- Validacoes obrigatorias ficam no proprio DTO (`@NotBlank`, `@Email`, `@Size`, etc.) e o controller usa `@Valid`.
- Regra explicita de usuario (etapa 2): em `/api/usuarios` aceitar `perfil`, mas aplicar autorizacao forte por perfil autenticado.
- Matriz de criacao administrativa:
  - `ADMIN` cria `ADMIN`, `FUNCIONARIO`, `GERENTE`, `GERENCIA_MATRIZ` (nao cria `CLIENTE`).
  - `GERENTE` cria apenas `FUNCIONARIO`.
  - `GERENCIA_MATRIZ` cria `FUNCIONARIO`, `GERENTE` e `GERENCIA_MATRIZ`.
  - `CLIENTE` e `FUNCIONARIO` nao criam perfis administrativos.

## 2) Response DTO
- Retornar apenas dados necessarios para consumo da UI/API.
- Nao expor segredos: senha, hash de senha, claims internas, tokens internos de refresh, metadados sensiveis.
- Campos tecnicos devem ser evitados quando nao agregam para o cliente.

## 3) Error DTO
- Formato unico obrigatorio: `status`, `erro`, `mensagem`, `timestamp`, `detalhes`.
- `detalhes` deve existir sempre (lista vazia quando nao houver detalhes especificos).

## Mapeamento atual do projeto
- Request DTOs: `CadastroRequest`, `LoginRequest`, `UsuarioCriacaoDTO`, `UsuarioAtualizacaoDTO`, `LojaCriacaoDTO`, `LojaAtualizacaoDTO`.
- Response DTOs: `LoginResponse`, `UsuarioRespostaDTO`, `LojaRespostaDTO`.
- Error DTO: `ErrorResponse`.

## Criterios objetivos desta etapa
- Todos os DTOs em `src/main/java/com/raizesdonordeste/api/dto` estao classificados em Request/Response/Error.
- `ErrorResponse` mantem contrato unico com `detalhes` presente por padrao.
- Controllers existentes recebem DTO com `@Valid` nos endpoints com corpo JSON.



