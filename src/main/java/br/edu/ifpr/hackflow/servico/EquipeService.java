package br.edu.ifpr.hackflow.servico;

import br.edu.ifpr.hackflow.dto.CadastroEquipeRequest;
import br.edu.ifpr.hackflow.dto.EquipeCriadaResponse;
import br.edu.ifpr.hackflow.dto.EquipeResumo;
import br.edu.ifpr.hackflow.dto.NotaRequest;
import br.edu.ifpr.hackflow.dto.NotaResponse;
import br.edu.ifpr.hackflow.dto.ProjetoRequest;
import br.edu.ifpr.hackflow.dto.ProjetoResponse;
import br.edu.ifpr.hackflow.erro.ApiException;
import br.edu.ifpr.hackflow.repositorio.EquipeRepository;
import br.edu.ifpr.hackflow.util.Tempo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipeService {

    private final EquipeRepository repositorio;
    private final PasswordEncoder codificador;

    public EquipeService(EquipeRepository repositorio, PasswordEncoder codificador) {
        this.repositorio = repositorio;
        this.codificador = codificador;
    }

    // ---------- consultas ----------

    public List<EquipeResumo> listar() {
        return repositorio.listar();
    }

    public EquipeResumo buscar(long id) {
        return repositorio.buscar(id).orElseThrow(EquipeService::equipeNaoEncontrada);
    }

    // ---------- cadastro ----------

    @Transactional
    public EquipeCriadaResponse cadastrar(CadastroEquipeRequest req) {
        // As anotações do DTO já validaram o formato; aqui normalizamos e conferimos de novo após o trim().
        String nome = req.nome().trim();
        String responsavel = req.responsavel().trim();
        String email = req.email().trim().toLowerCase(Locale.ROOT); // e-mail sempre em minúsculas
        String celular = normalizarCelular(req.celular());
        validarSenha(req.senha(), email);

        if (nome.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O nome da equipe deve ter de 3 a 60 caracteres.");
        }
        if (responsavel.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "O nome do responsável deve ter de 3 a 80 caracteres.");
        }
        if (repositorio.existeEmail(email)) {
            throw emailJaInscrito();
        }

        String codigo = gerarCodigoUnico();
        // Só o hash BCrypt vai para o banco: a senha em si nunca é guardada.
        String senhaHash = codificador.encode(req.senha());
        try {
            long id = repositorio.inserir(nome, responsavel, email, senhaHash, celular,
                    req.integrantes(), codigo, Tempo.agora());
            return new EquipeCriadaResponse(id, nome, codigo);
        } catch (DataAccessException e) {
            // Duas inscrições simultâneas com o mesmo e-mail: a restrição UNIQUE do banco é a garantia final.
            if (violouRestricaoUnica(e)) {
                throw emailJaInscrito();
            }
            throw e;
        }
    }

    // ---------- projeto ----------

    @Transactional
    public ProjetoResponse enviarProjeto(long equipeId, ProjetoRequest req) {
        // SEGURANÇA (fase futura): só o responsável da equipe pode enviar o projeto.
        buscar(equipeId); // 404 se a equipe não existir

        String repo = req.repositorio().trim();
        if (!ehUrlHttps(repo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Informe um link https:// válido para o repositório.");
        }
        String descricao = req.descricao() == null ? "" : req.descricao().trim();
        String enviadoEm = Tempo.agora();

        repositorio.salvarProjeto(equipeId, repo, descricao, enviadoEm);
        return new ProjetoResponse(equipeId, repo, descricao, enviadoEm);
    }

    // ---------- avaliação ----------

    @Transactional
    public NotaResponse registrarNota(long equipeId, NotaRequest req) {
        // SEGURANÇA (fase futura): só jurados; jurado não avalia a própria equipe; bloquear após o fechamento.
        EquipeResumo equipe = buscar(equipeId);
        if (!equipe.temProjeto()) {
            throw new ApiException(HttpStatus.CONFLICT, "A equipe ainda não enviou o projeto.");
        }

        // BigDecimal evita erros de arredondamento do double (ex.: 0.1 + 0.2).
        BigDecimal soma = BigDecimal.valueOf(req.inovacao())
                .add(BigDecimal.valueOf(req.execucaoTecnica()))
                .add(BigDecimal.valueOf(req.apresentacao()));
        double media = soma.divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP).doubleValue();

        repositorio.salvarAvaliacao(equipeId, req.inovacao(), req.execucaoTecnica(),
                req.apresentacao(), media, Tempo.agora());
        return new NotaResponse(equipeId, media);
    }

    // ---------- apoio ----------

    private static ApiException equipeNaoEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND, "Equipe não encontrada.");
    }

    private static ApiException emailJaInscrito() {
        // Propositalmente NÃO informa qual equipe nem quando: qualquer pessoa pode testar um e-mail.
        return new ApiException(HttpStatus.CONFLICT, "Já existe uma inscrição com este e-mail.");
    }

    /** O BCrypt só considera os 72 primeiros bytes da senha, então senhas maiores são recusadas. */
    private static void validarSenha(String senha, String email) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A senha deve ter de 8 a 72 caracteres.");
        }
        if (senha.equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A senha não pode ser igual ao e-mail.");
        }
    }

    /** Celular é opcional. Se vier, guarda só os dígitos e exige DDD + número (10 ou 11 dígitos). */
    private static String normalizarCelular(String celular) {
        if (celular == null || celular.isBlank()) {
            return null;
        }
        String digitos = celular.replaceAll("\\D", "");
        if (digitos.length() != 10 && digitos.length() != 11) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Celular inválido. Use DDD + número.");
        }
        return digitos;
    }

    /** Só aceita https:// com host e sem usuário/senha no endereço (bloqueia javascript:, data:, etc.). */
    static boolean ehUrlHttps(String texto) {
        try {
            URI uri = new URI(texto);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /** Gera HF-123 (e só passa a 4 dígitos se houver muitas colisões). */
    private String gerarCodigoUnico() {
        for (int tentativa = 0; tentativa < 200; tentativa++) {
            int base = tentativa < 30 ? 100 : 1000;
            int faixa = tentativa < 30 ? 900 : 9000;
            String codigo = "HF-" + (base + ThreadLocalRandom.current().nextInt(faixa));
            if (!repositorio.existeCodigo(codigo)) {
                return codigo;
            }
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível gerar o código da equipe.");
    }

    /** O driver do SQLite não é traduzido pelo Spring para DuplicateKeyException, então olhamos a mensagem. */
    private static boolean violouRestricaoUnica(Throwable erro) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            String mensagem = t.getMessage();
            if (mensagem != null && mensagem.contains("UNIQUE constraint failed")) {
                return true;
            }
        }
        return false;
    }
}
