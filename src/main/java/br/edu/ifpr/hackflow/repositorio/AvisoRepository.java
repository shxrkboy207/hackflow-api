package br.edu.ifpr.hackflow.repositorio;

import br.edu.ifpr.hackflow.dto.AvisoResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AvisoRepository {

    private static final RowMapper<AvisoResponse> MAPEADOR = (rs, linha) -> new AvisoResponse(
            rs.getLong("id"),
            rs.getString("texto"),
            rs.getInt("urgente") == 1, // o SQLite guarda boolean como 0/1
            rs.getString("criado_em"));

    private final JdbcTemplate jdbc;

    public AvisoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mais recentes primeiro. O LIMIT impede que o mural cresça sem fim na resposta. */
    public List<AvisoResponse> listar() {
        return jdbc.query(
                "SELECT id, texto, urgente, criado_em FROM avisos ORDER BY id DESC LIMIT 200",
                MAPEADOR);
    }

    public long inserir(String texto, boolean urgente, String criadoEm) {
        String sql = "INSERT INTO avisos (texto, urgente, criado_em) VALUES (?, ?, ?)";
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, texto);
            ps.setInt(2, urgente ? 1 : 0);
            ps.setString(3, criadoEm);
            return ps;
        }, chave);
        return Objects.requireNonNull(chave.getKey(), "O banco não devolveu o id do aviso").longValue();
    }
}
