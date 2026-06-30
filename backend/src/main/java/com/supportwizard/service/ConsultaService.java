package com.supportwizard.service;

import com.supportwizard.dto.ConsultaResponse;
import com.supportwizard.model.Consulta;
import com.supportwizard.repository.ConsultaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConsultaService {

    private final KnowledgeService knowledgeService;
    private final OllamaService ollamaService;
    private final ConsultaRepository consultaRepository;

    public ConsultaService(
            KnowledgeService knowledgeService,
            OllamaService ollamaService,
            ConsultaRepository consultaRepository) {
        this.knowledgeService = knowledgeService;
        this.ollamaService = ollamaService;
        this.consultaRepository = consultaRepository;
    }

    public ConsultaResponse processarConsulta(String problema) {
        String knowledgeBase = knowledgeService.getKnowledgeBase(problema);
        String prompt = montarPrompt(knowledgeBase, problema);
        String resposta = ollamaService.gerarResposta(prompt);

        Consulta consulta = new Consulta();
        consulta.setProblema(problema);
        consulta.setResposta(resposta);
        consulta.setCriadoEm(LocalDateTime.now());

        consulta = consultaRepository.save(consulta);

        return new ConsultaResponse(resposta, consulta.getId());
    }

    public void registrarFeedback(Long id, boolean util) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada: " + id));
        consulta.setUtil(util);
        consultaRepository.save(consulta);
    }

    private String montarPrompt(String knowledgeBase, String problema) {
        return """
                Você é um assistente especializado em suporte técnico de TI.
                Use APENAS as soluções abaixo como referência para responder.
                Não invente informações que não estejam na base.

                Copy the answer EXACTLY as written. Do not explain, do not add steps, do not invent procedures. If the information is a simple fact like a password, respond with just that fact.
                Preserve backslashes exactly as in the source (e.g. UNC paths like \\\\srv-print must keep both backslashes).

                === BASE DE CONHECIMENTO ===
                %s

                === PROBLEMA RELATADO ===
                %s

                Responda copiando exatamente o trecho relevante da base de conhecimento, sem reformular.
                """.formatted(knowledgeBase, problema);
    }
}
