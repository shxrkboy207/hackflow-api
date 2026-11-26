package br.edu.ifpr.hackflow.repositorio;

import br.edu.ifpr.hackflow.dto.PainelResponse.BarraEquipe;
import br.edu.ifpr.hackflow.dto.PainelResponse.PontoDia;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Consultas de contagem e agrupamento usadas no painel. Tudo sai das tabelas do banco. */
@Repository
public class PainelRepository {

    private final JdbcTemplate jdbc;

    public PainelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int contarEquipes() {
        return contar("SELECT COUNT(*) FROM equipes");
    }

    public int contarProjetos() {
        return contar("SELECT COUNT(*) FROM projetos");
    }

    public int contarAvaliadas() {
        return contar("SELECT COUNT(*) FROM avaliacoes");
    }

    /** Equipes que enviaram projeto e ainda não têm avaliação. */
    public int contarAguardando() {
        return contar("""
                SELECT COUNT(*)
                  FROM projetos p
                  LEFT JOIN avaliacoes a ON a.equipe_id = p.equipe_id
                 WHERE a.equipe_id IS NULL
                """);
    }

    /** Nota final das equipes avaliadas, da maior para a menor (no máximo 10). */
    public List<BarraEquipe> notasPorEquipe() {
        return jdbc.query("""
                SELECT e.nome, a.nota_final
                  FROM avaliacoes a
                  JOIN equipes e ON e.id = a.equipe_id
                 ORDER BY a.nota_final DESC, e.nome
                 LIMIT 10
                """, (rs, linha) -> new BarraEquipe(rs.getString("nome"), rs.getDouble("nota_final")));
    }

    /** Inscrições por dia nos últimos 14 dias com inscrição, do mais antigo para o mais recente. */
    public List<PontoDia> inscricoesPorDia() {
        List<PontoDia> dias = new ArrayList<>(jdbc.query("""
                SELECT substr(criada_em, 1, 10) AS dia, COUNT(*) AS total
                  FROM equipes
                 GROUP BY dia
                 ORDER BY dia DESC
                 LIMIT 14
                """, (rs, linha) -> new PontoDia(formatarDia(rs.getString("dia")), rs.getInt("total"))));
        Collections.reverse(dias); // a consulta traz do mais novo para o mais antigo
        return dias;
    }

    private int contar(String sql) {
        Integer total = jdbc.queryForObject(sql, Integer.class);
        return total == null ? 0 : total;
    }

    /** 2026-11-23 vira 23/11. */
    private static String formatarDia(String iso) {
        return iso.substring(8, 10) + "/" + iso.substring(5, 7);
    }
}
