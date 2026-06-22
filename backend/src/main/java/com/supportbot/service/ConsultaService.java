package com.supportbot.service;

import com.supportbot.dto.ConsultaResponse;
import com.supportbot.model.Consulta;
import com.supportbot.repository.ConsultaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConsultaService {

    private final KnowledgeService knowledgeService;
    private final GroqService groqService;
    private final ConsultaRepository consultaRepository;

    public ConsultaService(
            KnowledgeService knowledgeService,
            GroqService groqService,
            ConsultaRepository consultaRepository) {
        this.knowledgeService = knowledgeService;
        this.groqService = groqService;
        this.consultaRepository = consultaRepository;
    }

    public ConsultaResponse processarConsulta(String problema) {
        String knowledgeBase = knowledgeService.getKnowledgeBase();
        String prompt = montarPrompt(knowledgeBase, problema);
        String resposta = groqService.gerarResposta(prompt);

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

                === BASE DE CONHECIMENTO ===
                %s

                === PROBLEMA RELATADO ===
                %s

                Responda com:
                1. Diagnóstico provável
                2. Passo a passo da solução
                3. O que fazer se não resolver
                """.formatted(knowledgeBase, problema);
    }
}
