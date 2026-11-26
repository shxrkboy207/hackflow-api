package br.edu.ifpr.hackflow.config;

import br.edu.ifpr.hackflow.repositorio.AvisoRepository;
import br.edu.ifpr.hackflow.repositorio.EquipeRepository;
import br.edu.ifpr.hackflow.util.Tempo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "hackflow.seed", havingValue = "true", matchIfMissing = true)
public class DadosIniciais implements ApplicationRunner {

    private final EquipeRepository equipes;
    private final AvisoRepository avisos;
    private final PasswordEncoder codificador;

    public DadosIniciais(EquipeRepository equipes, AvisoRepository avisos, PasswordEncoder codificador) {
        this.equipes = equipes;
        this.avisos = avisos;
        this.codificador = codificador;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (equipes.contar() > 0) {
            return; // já tem dados: não mexe
        }
        String agora = Tempo.agora();
        // As equipes de exemplo recebem uma senha aleatória que ninguém conhece: não dá para entrar nelas.
        String semLogin = codificador.encode(UUID.randomUUID().toString());

        long alfa = equipes.inserir("Equipe Alfa", "Ana Alfa", "alfa@exemplo.com", semLogin, null, 3, "HF-101", agora);
        long beta = equipes.inserir("Equipe Beta", "Bruno Beta", "beta@exemplo.com", semLogin, null, 4, "HF-102", agora);
        long gama = equipes.inserir("Equipe Gama", "Gabi Gama", "gama@exemplo.com", semLogin, null, 5, "HF-103", agora);
        equipes.inserir("Equipe Delta", "Davi Delta", "delta@exemplo.com", semLogin, null, 3, "HF-104", agora);

        // Alfa, Beta e Gama enviaram projeto; Delta não.
        equipes.salvarProjeto(alfa, "https://github.com/exemplo/alfa", "Projeto da equipe Alfa", agora);
        equipes.salvarProjeto(beta, "https://github.com/exemplo/beta", "Projeto da equipe Beta", agora);
        equipes.salvarProjeto(gama, "https://github.com/exemplo/gama", "Projeto da equipe Gama", agora);

        // Alfa (8.7) e Gama (7.9) já avaliadas; Beta aguarda avaliação.
        equipes.salvarAvaliacao(alfa, 9.0, 8.5, 8.5, 8.7, agora);
        equipes.salvarAvaliacao(gama, 8.0, 7.5, 8.2, 7.9, agora);

        avisos.inserir("Abertura do evento no auditório. Check-in por QR code.", false, agora);
    }
}
