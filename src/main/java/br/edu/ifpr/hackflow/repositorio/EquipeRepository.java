package br.edu.ifpr.hackflow.repositorio;

import br.edu.ifpr.hackflow.dto.EquipeResumo;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao SQLite para equipes, projetos e avaliações.
 * Todo SQL usa parâmetros "?" (PreparedStatement): nunca concatene texto vindo do usuário no SQL,
 * isso evita SQL injection.
 */
@Repository
public class EquipeRepository {

    // Junta equipe + projeto + avaliação. (p.equipe_id IS NOT NULL) vira 1/0 = tem projeto.
    private static final String SELECT_RESUMO = """
            SELECT e.id,
                   e.nome,
                   (p.equipe_id IS NOT NULL) AS tem_projeto,
                   a.nota_final
              FROM equipes e
              LEFT JOIN projetos p   ON p.equipe_id = e.id
              LEFT JOIN avaliacoes a ON a.equipe_id = e.id
            """;

    private static final RowMapper<EquipeResumo> MAPEADOR_RESUMO = (rs, linha) -> {
        double nota = rs.getDouble("nota_final");
        Double notaOuNula = rs.wasNull() ? null : nota; // wasNull() informa se a última coluna lida era NULL
        return new EquipeResumo(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getBoolean("tem_projeto"),
                notaOuNula);
    };

    private final JdbcTemplate jdbc;

    public EquipeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- consultas ----------

    public List<EquipeResumo> listar() {
        return jdbc.query(SELECT_RESUMO + " ORDER BY e.id", MAPEADOR_RESUMO);
    }

    public Optional<EquipeResumo> buscar(long id) {
        return jdbc.query(SELECT_RESUMO + " WHERE e.id = ?", MAPEADOR_RESUMO, id).stream().findFirst();
    }

    public int contar() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM equipes", Integer.class);
        return total == null ? 0 : total;
    }

    public boolean existeEmail(String email) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM equipes WHERE email = ?", Integer.class, email);
        return total != null && total > 0;
    }

    public boolean existeCodigo(String codigo) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM equipes WHERE codigo = ?", Integer.class, codigo);
        return total != null && total > 0;
    }

    // ---------- escrita ----------

    /** Insere a equipe e devolve o id gerado pelo banco. A senha chega aqui já como hash BCrypt. */
    public long inserir(String nome, String responsavel, String email, String senhaHash, String celular,
                        int integrantes, String codigo, String criadaEm) {
        String sql = """
                INSERT INTO equipes (nome, responsavel, email, senha_hash, celular, integrantes, codigo, criada_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement ps = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, nome);
            ps.setString(2, responsavel);
            ps.setString(3, email);
            ps.setString(4, senhaHash);
            ps.setString(5, celular); // null vira NULL no banco
            ps.setInt(6, integrantes);
            ps.setString(7, codigo);
            ps.setString(8, criadaEm);
            return ps;
        }, chave);
        return Objects.requireNonNull(chave.getKey(), "O banco não devolveu o id da equipe").longValue();
    }

    /** Cria o projeto da equipe ou, se já existir, substitui (upsert do SQLite: ON CONFLICT ... DO UPDATE). */
    public void salvarProjeto(long equipeId, String repositorio, String descricao, String enviadoEm) {
        jdbc.update("""
                INSERT INTO projetos (equipe_id, repositorio, descricao, enviado_em)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (equipe_id) DO UPDATE SET
                    repositorio = excluded.repositorio,
                    descricao   = excluded.descricao,
                    enviado_em  = excluded.enviado_em
                """, equipeId, repositorio, descricao, enviadoEm);
    }

    /** Cria a avaliação da equipe ou substitui a anterior ("Revisar"). */
    public void salvarAvaliacao(long equipeId, double inovacao, double execucaoTecnica,
                                double apresentacao, double notaFinal, String avaliadoEm) {
        jdbc.update("""
                INSERT INTO avaliacoes (equipe_id, inovacao, execucao_tecnica, apresentacao, nota_final, avaliado_em)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (equipe_id) DO UPDATE SET
                    inovacao         = excluded.inovacao,
                    execucao_tecnica = excluded.execucao_tecnica,
                    apresentacao     = excluded.apresentacao,
                    nota_final       = excluded.nota_final,
                    avaliado_em      = excluded.avaliado_em
                """, equipeId, inovacao, execucaoTecnica, apresentacao, notaFinal, avaliadoEm);
    }

    // ---------- login e perfil ----------

    /** Dados para conferir a senha no login. */
    public record Credencial(long equipeId, String senhaHash) {
    }

    public Optional<Credencial> buscarCredencialPorEmail(String email) {
        return jdbc.query(
                "SELECT id, senha_hash FROM equipes WHERE email = ?",
                (rs, linha) -> new Credencial(rs.getLong("id"), rs.getString("senha_hash")),
                email).stream().findFirst();
    }

    /** Equipe + responsável + projeto (se houver). Os campos do projeto são null quando não há projeto. */
    public record EquipeCompleta(long id, String nome, String responsavel, String email, String celular,
                                 int integrantes, String codigo, String criadaEm,
                                 String repositorio, String descricao, String enviadoEm) {
    }

    public Optional<EquipeCompleta> buscarCompleta(long id) {
        return jdbc.query("""
                SELECT e.id, e.nome, e.responsavel, e.email, e.celular, e.integrantes, e.codigo, e.criada_em,
                       p.repositorio, p.descricao, p.enviado_em
                  FROM equipes e
                  LEFT JOIN projetos p ON p.equipe_id = e.id
                 WHERE e.id = ?
                """, (rs, linha) -> new EquipeCompleta(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("responsavel"),
                rs.getString("email"),
                rs.getString("celular"),
                rs.getInt("integrantes"),
                rs.getString("codigo"),
                rs.getString("criada_em"),
                rs.getString("repositorio"),
                rs.getString("descricao"),
                rs.getString("enviado_em")), id).stream().findFirst();
    }
}
