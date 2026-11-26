package br.edu.ifpr.hackflow.servico;

import br.edu.ifpr.hackflow.dto.LoginRequest;
import br.edu.ifpr.hackflow.dto.SessaoResponse;
import br.edu.ifpr.hackflow.erro.ApiException;
import br.edu.ifpr.hackflow.repositorio.EquipeRepository;
import br.edu.ifpr.hackflow.repositorio.EquipeRepository.Credencial;
import br.edu.ifpr.hackflow.repositorio.SessaoRepository;
import br.edu.ifpr.hackflow.util.Tempo;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Login, sessões e proteção contra tentativas repetidas de adivinhar senha. */
@Service
public class AuthService {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int MAXIMO_DE_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private record Falhas(int quantidade, Instant desde) {
    }

    /** Resultado do login: o token vai para o cookie, a sessão vai no corpo da resposta. */
    public record ResultadoLogin(String token, SessaoResponse sessao) {
    }

    // Falhas de login por e-mail (em memória: zera quando o servidor reinicia).
    // Limitação conhecida: alguém pode bloquear o login de um e-mail alheio por 15 minutos.
    private final ConcurrentHashMap<String, Falhas> falhas = new ConcurrentHashMap<>();

    private final EquipeRepository equipes;
    private final SessaoRepository sessoes;
    private final PasswordEncoder codificador;
    private final long horasDeSessao;
    // Hash de uma senha qualquer. É conferido quando o e-mail não existe, para o tempo de resposta
    // ser o mesmo nos dois casos (senão daria para descobrir quais e-mails estão cadastrados).
    private final String hashFalso;

    public AuthService(EquipeRepository equipes, SessaoRepository sessoes, PasswordEncoder codificador,
                       @Value("${hackflow.sessao.horas:12}") long horasDeSessao) {
        this.equipes = equipes;
        this.sessoes = sessoes;
        this.codificador = codificador;
        this.horasDeSessao = horasDeSessao;
        this.hashFalso = codificador.encode("senha-falsa-para-igualar-o-tempo");
    }

    // ---------- login ----------

    public ResultadoLogin login(LoginRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        exigirLiberado(email);

        Optional<Credencial> credencial = equipes.buscarCredencialPorEmail(email);
        // A conferência de senha roda sempre, mesmo sem conta, para igualar o tempo de resposta.
        boolean senhaCorreta = codificador.matches(req.senha(), credencial.map(Credencial::senhaHash).orElse(hashFalso));

        if (credencial.isEmpty() || !senhaCorreta) {
            registrarFalha(email);
            // Mesma mensagem nos dois casos: não revela se o e-mail existe.
            throw new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        falhas.remove(email);

        long equipeId = credencial.get().equipeId();
        String token = criarSessao(equipeId);
        return new ResultadoLogin(token, sessaoDe(equipeId).orElseThrow());
    }

    // ---------- sessões ----------

    /** Cria uma sessão e devolve o token (que só existe aqui e no cookie: o banco guarda apenas o hash). */
    public String criarSessao(long equipeId) {
        sessoes.apagarExpiradas(Tempo.agora());
        byte[] bytes = new byte[32]; // 256 bits aleatórios
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.inserir(hash(token), equipeId, Tempo.agora(), Tempo.daquiAHoras(horasDeSessao));
        return token;
    }

    /** Id da equipe dona do token, se a sessão existir e não tiver vencido. */
    public Optional<Long> equipeDoToken(String token) {
        if (token == null || token.isBlank() || token.length() > 100) {
            return Optional.empty();
        }
        return sessoes.buscarEquipeId(hash(token), Tempo.agora());
    }

    public void encerrarSessao(String token) {
        if (token != null && !token.isBlank() && token.length() <= 100) {
            sessoes.apagar(hash(token));
        }
    }

    public Optional<SessaoResponse> sessaoDe(long equipeId) {
        return equipes.buscarCompleta(equipeId)
                .map(e -> new SessaoResponse(e.responsavel(), e.nome(), e.id()));
    }

    // ---------- limite de tentativas ----------

    private void exigirLiberado(String chave) {
        Falhas atual = falhas.get(chave);
        if (atual != null && !expirou(atual) && atual.quantidade() >= MAXIMO_DE_FALHAS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde alguns minutos e tente de novo.");
        }
    }

    private void registrarFalha(String chave) {
        if (falhas.size() > 5000) { // evita crescer sem fim se alguém testar milhares de e-mails
            falhas.entrySet().removeIf(entrada -> expirou(entrada.getValue()));
        }
        falhas.merge(chave, new Falhas(1, Instant.now()),
                (atual, nova) -> expirou(atual) ? nova : new Falhas(atual.quantidade() + 1, atual.desde()));
    }

    private static boolean expirou(Falhas f) {
        return f.desde().plus(JANELA).isBefore(Instant.now());
    }

    private static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
