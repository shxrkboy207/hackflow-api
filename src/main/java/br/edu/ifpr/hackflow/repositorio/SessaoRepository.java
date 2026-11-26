package br.edu.ifpr.hackflow.repositorio;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Tabela sessoes: guarda o hash do token de login, a equipe dona e a data de vencimento. */
@Repository
public class SessaoRepository {

    private final JdbcTemplate jdbc;

    public SessaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void inserir(String tokenHash, long equipeId, String criadaEm, String expiraEm) {
        jdbc.update("INSERT INTO sessoes (token_hash, equipe_id, criada_em, expira_em) VALUES (?, ?, ?, ?)",
                tokenHash, equipeId, criadaEm, expiraEm);
    }

    /** Só devolve a equipe se a sessão existir e ainda não tiver vencido (datas ISO comparam como texto). */
    public Optional<Long> buscarEquipeId(String tokenHash, String agora) {
        return jdbc.query(
                "SELECT equipe_id FROM sessoes WHERE token_hash = ? AND expira_em > ?",
                (rs, linha) -> rs.getLong("equipe_id"),
                tokenHash, agora).stream().findFirst();
    }

    public void apagar(String tokenHash) {
        jdbc.update("DELETE FROM sessoes WHERE token_hash = ?", tokenHash);
    }

    public void apagarExpiradas(String agora) {
        jdbc.update("DELETE FROM sessoes WHERE expira_em <= ?", agora);
    }
}
