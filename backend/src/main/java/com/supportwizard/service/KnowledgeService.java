package com.supportwizard.service;

import com.supportwizard.dto.KnowledgeItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final String DEBUG_PERGUNTA = "qual a senha da bios";
    private static final String CSV_HEADER = "problema,solucao";

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "o", "as", "os", "um", "uma", "uns", "umas",
            "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
            "por", "para", "com", "sem", "e", "ou", "que", "se", "me", "te",
            "eu", "ele", "ela", "isso", "isto", "qual", "quais", "como",
            "onde", "quando", "porque", "ser", "esta", "este", "essa", "esse"
    );

    private final ResourceLoader resourceLoader;
    private final String knowledgeCsvPath;

    public KnowledgeService(
            ResourceLoader resourceLoader,
            @Value("${knowledge.csv.path}") String knowledgeCsvPath) {
        this.resourceLoader = resourceLoader;
        this.knowledgeCsvPath = knowledgeCsvPath;
    }

    public String getKnowledgeBase(String pergunta) {
        List<KnowledgeRow> todasLinhas = carregarLinhas();
        Set<String> palavrasChave = extrairPalavrasChave(pergunta);

        List<ScoredRow> linhasRanqueadas = todasLinhas.stream()
                .map(linha -> new ScoredRow(linha, calcularPontuacao(palavrasChave, linha)))
                .filter(scored -> scored.pontuacao() > 0)
                .sorted(Comparator.comparingInt(ScoredRow::pontuacao).reversed())
                .toList();

        List<KnowledgeRow> linhasSelecionadas = linhasRanqueadas.isEmpty()
                ? todasLinhas
                : linhasRanqueadas.stream().map(ScoredRow::linha).toList();

        if (deveLogarDebug(pergunta)) {
            String linhasFormatadas = linhasSelecionadas.stream()
                    .map(linha -> "problema=\"" + linha.problema() + "\" | solucao=\"" + resumir(linha.solucao()) + "\"")
                    .collect(Collectors.joining(" || "));
            log.info(
                    "[DEBUG knowledge] pergunta='{}' | palavras-chave={} | total_csv={} | retornadas={} | linhas=[{}]",
                    pergunta,
                    palavrasChave,
                    todasLinhas.size(),
                    linhasSelecionadas.size(),
                    linhasFormatadas
            );
        }

        return formatarCsv(linhasSelecionadas, false);
    }

    public synchronized List<KnowledgeItemDTO> listarTodos() {
        List<KnowledgeRow> linhas = carregarLinhas();
        return IntStream.range(0, linhas.size())
                .mapToObj(i -> toDto(i, linhas.get(i)))
                .toList();
    }

    public synchronized KnowledgeItemDTO adicionar(String problema, String solucao) {
        validarCampos(problema, solucao);
        List<KnowledgeRow> linhas = new ArrayList<>(carregarLinhas());
        KnowledgeRow novo = new KnowledgeRow(problema.trim(), solucao.trim());
        linhas.add(novo);
        salvarLinhas(linhas);
        return toDto(linhas.size() - 1, novo);
    }

    public synchronized KnowledgeItemDTO atualizar(int indice, String problema, String solucao) {
        validarCampos(problema, solucao);
        List<KnowledgeRow> linhas = new ArrayList<>(carregarLinhas());
        validarIndice(indice, linhas.size());
        KnowledgeRow atualizado = new KnowledgeRow(problema.trim(), solucao.trim());
        linhas.set(indice, atualizado);
        salvarLinhas(linhas);
        return toDto(indice, atualizado);
    }

    public synchronized void excluir(int indice) {
        List<KnowledgeRow> linhas = new ArrayList<>(carregarLinhas());
        validarIndice(indice, linhas.size());
        linhas.remove(indice);
        salvarLinhas(linhas);
    }

    private KnowledgeItemDTO toDto(int indice, KnowledgeRow linha) {
        return new KnowledgeItemDTO(indice, linha.problema(), linha.solucao());
    }

    private void validarCampos(String problema, String solucao) {
        if (problema == null || problema.isBlank()) {
            throw new IllegalArgumentException("O campo problema é obrigatório");
        }
        if (solucao == null || solucao.isBlank()) {
            throw new IllegalArgumentException("O campo solucao é obrigatório");
        }
    }

    private void validarIndice(int indice, int tamanho) {
        if (indice < 0 || indice >= tamanho) {
            throw new IllegalArgumentException("Índice inválido: " + indice);
        }
    }

    private List<KnowledgeRow> carregarLinhas() {
        try {
            String conteudo = lerConteudoCsv();
            return parseCsv(conteudo);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível ler o arquivo knowledge.csv em: " + knowledgeCsvPath, e);
        }
    }

    private void salvarLinhas(List<KnowledgeRow> linhas) {
        if (knowledgeCsvPath.startsWith("classpath:")) {
            throw new IllegalArgumentException(
                    "Edição da base de conhecimento requer knowledge.csv.path apontando para um arquivo no sistema de arquivos");
        }
        try {
            Path arquivo = Path.of(knowledgeCsvPath);
            Path diretorio = arquivo.getParent();
            if (diretorio != null) {
                Files.createDirectories(diretorio);
            }
            Files.writeString(arquivo, formatarCsv(linhas, true), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível salvar o arquivo knowledge.csv em: " + knowledgeCsvPath, e);
        }
    }

    private String lerConteudoCsv() throws IOException {
        if (knowledgeCsvPath.startsWith("classpath:")) {
            Resource resource = resourceLoader.getResource(knowledgeCsvPath);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        }
        return Files.readString(Path.of(knowledgeCsvPath), StandardCharsets.UTF_8);
    }

    private List<KnowledgeRow> parseCsv(String conteudo) {
        List<KnowledgeRow> linhas = new ArrayList<>();
        if (conteudo == null || conteudo.isBlank()) {
            return linhas;
        }

        String normalizado = conteudo.replace("\r\n", "\n").replace('\r', '\n');
        int pos = 0;

        if (normalizado.startsWith(CSV_HEADER)) {
            pos = normalizado.indexOf('\n');
            if (pos < 0) {
                return linhas;
            }
            pos++;
        }

        while (pos < normalizado.length()) {
            pos = pularEspacos(normalizado, pos);
            if (pos >= normalizado.length()) {
                break;
            }

            ParsedField problema = lerCampoEntreAspas(normalizado, pos);
            if (problema == null) {
                break;
            }
            pos = problema.proximaPosicao();

            if (pos >= normalizado.length() || normalizado.charAt(pos) != ',') {
                break;
            }
            pos++;

            ParsedField solucao = lerCampoEntreAspas(normalizado, pos);
            if (solucao == null) {
                break;
            }
            pos = solucao.proximaPosicao();

            linhas.add(new KnowledgeRow(
                    deserializarQuebrasLinha(problema.valor()),
                    deserializarQuebrasLinha(solucao.valor())));
            pos = pularEspacos(normalizado, pos);
        }

        return linhas;
    }

    private int pularEspacos(String conteudo, int pos) {
        while (pos < conteudo.length()) {
            char atual = conteudo.charAt(pos);
            if (atual == '\n' || atual == ' ' || atual == '\t') {
                pos++;
                continue;
            }
            break;
        }
        return pos;
    }

    private ParsedField lerCampoEntreAspas(String conteudo, int inicio) {
        if (inicio >= conteudo.length() || conteudo.charAt(inicio) != '"') {
            return null;
        }

        StringBuilder valor = new StringBuilder();
        int i = inicio + 1;

        while (i < conteudo.length()) {
            char c = conteudo.charAt(i);
            if (c == '"') {
                if (i + 1 < conteudo.length() && conteudo.charAt(i + 1) == '"') {
                    valor.append('"');
                    i += 2;
                    continue;
                }
                return new ParsedField(valor.toString(), i + 1);
            }
            valor.append(c);
            i++;
        }

        return null;
    }

    private Set<String> extrairPalavrasChave(String pergunta) {
        String normalizada = normalizar(pergunta);
        return Arrays.stream(normalizada.split("\\s+"))
                .map(String::trim)
                .filter(palavra -> palavra.length() >= 3)
                .filter(palavra -> !STOP_WORDS.contains(palavra))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private int calcularPontuacao(Set<String> palavrasChave, KnowledgeRow linha) {
        String textoProblema = normalizar(linha.problema());
        String textoSolucao = normalizar(linha.solucao());
        int pontuacao = 0;

        for (String palavra : palavrasChave) {
            if (textoProblema.contains(palavra)) {
                pontuacao += 3;
            }
            if (textoSolucao.contains(palavra)) {
                pontuacao += 2;
            }
        }

        return pontuacao;
    }

    private String formatarCsv(List<KnowledgeRow> linhas, boolean paraArquivo) {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (KnowledgeRow linha : linhas) {
            String problema = paraArquivo ? serializarQuebrasLinha(linha.problema()) : linha.problema();
            String solucao = paraArquivo ? serializarQuebrasLinha(linha.solucao()) : linha.solucao();
            csv.append('"').append(escapar(problema)).append("\",\"")
                    .append(escapar(solucao)).append("\"\n");
        }
        return csv.toString();
    }

    private String serializarQuebrasLinha(String valor) {
        if (valor == null || valor.isEmpty()) {
            return valor == null ? "" : valor;
        }
        StringBuilder resultado = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '\n') {
                resultado.append("\\n");
            } else if (c == '\r') {
                continue;
            } else {
                resultado.append(c);
            }
        }
        return resultado.toString();
    }

    private String deserializarQuebrasLinha(String valor) {
        if (valor == null || valor.isEmpty()) {
            return valor;
        }
        StringBuilder resultado = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            if (valor.charAt(i) == '\\' && i + 1 < valor.length() && valor.charAt(i + 1) == 'n') {
                resultado.append('\n');
                i++;
            } else {
                resultado.append(valor.charAt(i));
            }
        }
        return resultado.toString();
    }

    private String escapar(String valor) {
        return valor.replace("\"", "\"\"");
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private boolean deveLogarDebug(String pergunta) {
        return normalizar(pergunta).contains(DEBUG_PERGUNTA);
    }

    private String resumir(String texto) {
        return texto.length() > 120 ? texto.substring(0, 120) + "..." : texto;
    }

    private record KnowledgeRow(String problema, String solucao) {
    }

    private record ScoredRow(KnowledgeRow linha, int pontuacao) {
    }

    private record ParsedField(String valor, int proximaPosicao) {
    }
}
