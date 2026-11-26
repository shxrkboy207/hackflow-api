-- Tabelas do HackFlow (SQLite). O IF NOT EXISTS permite rodar a cada inicialização.
-- Datas ficam em TEXT no formato ISO-8601 (ex.: 2026-10-23T09:12:00), como a API devolve.

-- Cada equipe tem uma pessoa responsável, que é quem faz login (e-mail + senha).
CREATE TABLE IF NOT EXISTS equipes (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    nome         TEXT    NOT NULL,
    responsavel  TEXT    NOT NULL,
    email        TEXT    NOT NULL UNIQUE,   -- sempre em minúsculas, uma inscrição por e-mail
    senha_hash   TEXT    NOT NULL,          -- hash BCrypt, a senha em si nunca é guardada
    celular      TEXT,                      -- só dígitos (DDD + número) ou vazio
    integrantes  INTEGER NOT NULL CHECK (integrantes BETWEEN 3 AND 5),
    codigo       TEXT    NOT NULL UNIQUE,   -- ex.: HF-482
    criada_em    TEXT    NOT NULL
);

-- No máximo um projeto por equipe (enviar de novo substitui o anterior).
CREATE TABLE IF NOT EXISTS projetos (
    equipe_id    INTEGER PRIMARY KEY REFERENCES equipes (id) ON DELETE CASCADE,
    repositorio  TEXT    NOT NULL,
    descricao    TEXT,
    enviado_em   TEXT    NOT NULL
);

-- Uma avaliação por equipe (avaliar de novo, a tela chama de Revisar, substitui a anterior).
CREATE TABLE IF NOT EXISTS avaliacoes (
    equipe_id         INTEGER PRIMARY KEY REFERENCES equipes (id) ON DELETE CASCADE,
    inovacao          REAL NOT NULL CHECK (inovacao BETWEEN 0 AND 10),
    execucao_tecnica  REAL NOT NULL CHECK (execucao_tecnica BETWEEN 0 AND 10),
    apresentacao      REAL NOT NULL CHECK (apresentacao BETWEEN 0 AND 10),
    nota_final        REAL NOT NULL,        -- média das três, calculada pelo servidor
    avaliado_em       TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS avisos (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    texto      TEXT    NOT NULL,
    urgente    INTEGER NOT NULL DEFAULT 0 CHECK (urgente IN (0, 1)),
    criado_em  TEXT    NOT NULL
);

-- Sessões de login. Guarda só o hash (SHA-256) do token que vai no cookie,
-- então, se o banco vazar, os tokens não podem ser usados.
CREATE TABLE IF NOT EXISTS sessoes (
    token_hash  TEXT    PRIMARY KEY,
    equipe_id   INTEGER NOT NULL REFERENCES equipes (id) ON DELETE CASCADE,
    criada_em   TEXT    NOT NULL,
    expira_em   TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessoes_expira ON sessoes (expira_em);
