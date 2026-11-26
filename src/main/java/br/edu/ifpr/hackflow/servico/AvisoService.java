package br.edu.ifpr.hackflow.servico;

import br.edu.ifpr.hackflow.dto.AvisoRequest;
import br.edu.ifpr.hackflow.dto.AvisoResponse;
import br.edu.ifpr.hackflow.erro.ApiException;
import br.edu.ifpr.hackflow.repositorio.AvisoRepository;
import br.edu.ifpr.hackflow.util.Tempo;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvisoService {

    private final AvisoRepository repositorio;

    public AvisoService(AvisoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public List<AvisoResponse> listar() {
        return repositorio.listar();
    }

    @Transactional
    public AvisoResponse publicar(AvisoRequest req) {
        // SEGURANÇA (fase futura): só a comissão organizadora pode publicar avisos.
        String texto = req.texto().trim();
        if (texto.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escreva a mensagem do aviso.");
        }
        // O servidor define a hora (o front não pode escolher), para o mural ser confiável.
        String criadoEm = Tempo.agora();
        long id = repositorio.inserir(texto, req.urgente(), criadoEm);
        return new AvisoResponse(id, texto, req.urgente(), criadoEm);
    }
}
