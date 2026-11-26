# HackFlow API (Spring Boot + SQLite)

API REST que atende o front-end React do HackFlow. Java 21, Spring Boot 3.5, JDBC com SQL escrito à mão
(`JdbcTemplate`), banco SQLite em arquivo, senhas com BCrypt e login por cookie de sessão.

## Como rodar

Você precisa do **JDK 21** e do **Maven 3.9+** (`java -version` e `mvn -v` devem funcionar).
No IntelliJ, abra a pasta `hackflow-api` (a que tem o `pom.xml`), espere o Maven baixar as dependências
(na primeira vez precisa de internet) e execute a classe `HackflowApplication` (botão ▶ ao lado do `main`).
Pelo terminal: `mvn spring-boot:run`.

- A API sobe em `http://127.0.0.1:8080` (somente acessível do próprio computador).
- Na primeira execução é criado o arquivo `hackflow.db` na pasta de onde você rodou, só com as tabelas (vazio).
- Para recomeçar do zero, pare o servidor e apague `hackflow.db`.
- **Se você veio de uma versão antiga, apague o `hackflow.db`**: a tabela `equipes` ganhou a coluna `senha_hash`.
- `hackflow.seed=true` em `application.properties` cria 4 equipes de exemplo (sem senha utilizável). O padrão é `false`:
  o banco só tem o que for cadastrado.

## Ligar o front

No `vite.config.js` do front, use o endereço numérico no proxy (o `localhost` pode resolver para IPv6):

```js
target: 'http://127.0.0.1:8080',
```

## Endpoints

| Método | Rota | Login? | Resposta |
|---|---|---|---|
| POST | `/api/equipes` | não | 201 `{id, nome, codigo}` + cookie de login · 400 · 409 (e-mail já inscrito) |
| GET | `/api/equipes` | não* | 200 `[{id, nome, temProjeto, nota}]` |
| GET | `/api/equipes/{id}` | não* | 200 · 404 |
| POST | `/api/equipes/{id}/projeto` | não* | 201 · 400 (link precisa ser https) · 404 |
| PUT | `/api/equipes/{id}/nota` | não* | 200 `{id, nota}` · 400 · 404 · 409 (sem projeto) |
| GET | `/api/avisos` | não | 200 `[{id, texto, urgente, criadoEm}]` |
| POST | `/api/avisos` | não* | 201 · 400 |
| GET | `/api/resultado` | não | 200 `{ranking, aguardando}` |
| GET | `/api/painel/resumo` | não | 200 (contagens e agrupamentos do banco) |
| POST | `/api/auth/login` | não | 200 `{nome, equipe, equipeId}` + cookie · 401 · 429 |
| POST | `/api/auth/logout` | não | 204, apaga o cookie |
| GET | `/api/auth/sessao` | não | 200 quem está logado · 204 se ninguém |
| GET | `/api/perfil` | **sim** | 200 · 401 |

\* Ainda abertas (próxima etapa: exigir login e papéis).

Todo erro tem o formato `{ "mensagem": "texto" }`.

## Estrutura

```
src/main/java/br/edu/ifpr/hackflow/
├── HackflowApplication.java
├── controle/        Rotas HTTP (Equipe, Aviso, Resultado, Painel, Auth, Perfil)
├── servico/         Regras (cadastro, projeto, nota, ranking, painel, login e sessões, perfil)
├── repositorio/     SQL com JdbcTemplate (Equipe, Aviso, Painel, Sessao)
├── seguranca/       CookieSessao e AutenticacaoInterceptor (confere o login)
├── dto/             Formatos do JSON (records) e validações
├── erro/            ApiException e tratador global ({"mensagem": ...})
├── config/          CORS, BCrypt, rotas protegidas e dados de exemplo
└── util/Tempo.java  Data/hora em ISO-8601 (horário de Brasília)
src/main/resources/
├── application.properties
└── schema.sql       Tabelas: equipes, projetos, avaliacoes, avisos, sessoes
```

## Banco de dados

```
equipes (id, nome, responsavel, email UNIQUE, senha_hash, celular, integrantes 3..5, codigo UNIQUE, criada_em)
projetos (equipe_id PK/FK, repositorio, descricao, enviado_em)
avaliacoes (equipe_id PK/FK, inovacao, execucao_tecnica, apresentacao, nota_final, avaliado_em)
avisos (id, texto, urgente 0/1, criado_em)
sessoes (token_hash PK, equipe_id FK, criada_em, expira_em)
```

Cada equipe tem uma pessoa responsável, que é quem se cadastra e faz login (e-mail + senha).
Para olhar o conteúdo, abra `hackflow.db` no **DB Browser for SQLite**:

```sql
SELECT id, nome, responsavel, email, codigo FROM equipes;
SELECT substr(senha_hash, 1, 7) FROM equipes;   -- aparece $2a$12$, nunca a senha
SELECT * FROM projetos;  SELECT * FROM avaliacoes;
SELECT substr(token_hash, 1, 8), equipe_id, expira_em FROM sessoes;
```

## Login e segurança

Já incluído:
- Senha guardada só como hash **BCrypt** (custo 12). Mínimo de 8 caracteres, máximo de 72 bytes.
- Login por cookie `HttpOnly` + `SameSite=Strict` (o JavaScript não lê o token). O banco guarda só o **hash SHA-256** do token.
  Sessão dura 12 h (`hackflow.sessao.horas`). Mude `hackflow.cookie.seguro` para `true` quando houver HTTPS.
- Mensagem de erro igual para e-mail ou senha incorretos, com tempo de resposta equalizado, e limite de 5 falhas por e-mail
  em 15 minutos (429). O contador fica na memória e zera quando o servidor reinicia.
- `/api/perfil` pega o id da **sessão**, nunca da URL: ninguém vê o perfil de outra equipe.
- Todo SQL usa parâmetros (`?`). Validação no servidor + restrições do banco (UNIQUE, CHECK, chaves estrangeiras).
- JSON com campos desconhecidos é recusado. Erros não revelam detalhes internos.
- O servidor só escuta em `127.0.0.1`. O CORS só libera `http://localhost:5173` (nunca `*`).

Falta (marcado com `SEGURANÇA` no código):
- Exigir login nas rotas de escrita (projeto só da equipe logada; nota só de jurado; aviso só da comissão) e papéis.
  Ao proteger rotas de escrita, tratar também CSRF.
- Recuperação de senha, verificação de e-mail, HTTPS e limite de requisições por IP.

## Problemas comuns

- **`PasswordEncoder` em vermelho**: falta a dependência `spring-security-crypto` no `pom.xml` ou recarregar o Maven (aba Maven > 🔄).
- **`Port 8080 was already in use`**: já há um back rodando. Pare o antigo (■ na aba Run) ou troque `server.port`
  (e o `target` do proxy no front).
- **`database is locked`**: só ocorre se `hackflow.db` estiver aberto em outro programa gravando. Feche-o.
- **Maven não acha a versão do Spring Boot**: troque `3.5.7` no `pom.xml` pela última 3.5.x de start.spring.io.
- **O front mostra "Não foi possível conectar ao servidor"**: a API não está rodando, ou o proxy do Vite ainda aponta para `localhost`.
