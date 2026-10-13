package dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import model.Projeto;

public class ProjetoCSV {

    private static final String CABECALHO =
            "id;nome;descricao;categoria;status";

    private static final int TOTAL_COLUNAS = 5;

    private final Path caminho;

    public ProjetoCSV() {

        this(Path.of("dados/projetos.csv"));
    }

    // Permite informar outro arquivo (útil para testes)
    public ProjetoCSV(Path caminho) {

        this.caminho = caminho;
    }

    public void salvar(List<Projeto> projetos)
            throws IOException {

        List<String> linhas = new ArrayList<>();

        linhas.add(CABECALHO);

        for (Projeto projeto : projetos) {

            String linha =
                    projeto.getId() + ";" +
                            limpar(projeto.getNome()) + ";" +
                            limpar(projeto.getDescricao()) + ";" +
                            limpar(projeto.getCategoria()) + ";" +
                            limpar(projeto.getStatus());

            linhas.add(linha);
        }

        // Cria a pasta "dados" caso ela ainda não exista
        Path pasta = caminho.toAbsolutePath().getParent();

        if (pasta != null) {
            Files.createDirectories(pasta);
        }

        // Grava primeiro em um arquivo temporário e só depois substitui
        // o original: se algo falhar no meio, o CSV antigo não é corrompido.
        Path temporario =
                caminho.resolveSibling(caminho.getFileName() + ".tmp");

        Files.write(temporario, linhas, StandardCharsets.UTF_8);

        Files.move(
                temporario,
                caminho,
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    public List<Projeto> listar()
            throws IOException {

        List<Projeto> projetos = new ArrayList<>();

        if (!Files.exists(caminho)) {

            return projetos;
        }

        List<String> linhas =
                Files.readAllLines(caminho, StandardCharsets.UTF_8);

        if (linhas.isEmpty()) {

            // Arquivo existe mas está vazio: não há nem cabeçalho.
            // Tratamos como "sem projetos ainda".
            return projetos;
        }

        // A linha 0 deve ser o cabeçalho. Sem essa checagem, um CSV
        // editado à mão sem o cabeçalho faria o primeiro projeto real
        // ser descartado em silêncio, como se fosse o cabeçalho.
        String cabecalho = linhas.get(0).trim();

        if (!cabecalho.equalsIgnoreCase(CABECALHO)) {

            throw new IOException(
                    "Cabeçalho do CSV inválido. Esperado \"" + CABECALHO
                            + "\", encontrado \"" + cabecalho + "\"."
            );
        }

        for (int i = 1; i < linhas.size(); i++) {

            String linha = linhas.get(i);

            // Ignora linhas em branco (ex.: linha vazia no fim do arquivo)
            if (linha.isBlank()) {
                continue;
            }

            // O -1 mantém campos vazios no final da linha
            String[] dados = linha.split(";", -1);

            // Uma linha inválida gera erro em vez de ser ignorada em silêncio.
            // Se fosse ignorada, o próximo salvar() apagaria essa linha do CSV.
            if (dados.length != TOTAL_COLUNAS) {

                throw new IOException(
                        "Linha " + (i + 1) + " do CSV inválida: esperado "
                                + TOTAL_COLUNAS + " colunas, encontrado "
                                + dados.length + "."
                );
            }

            int id;

            try {

                id = Integer.parseInt(dados[0].trim());

            } catch (NumberFormatException e) {

                throw new IOException(
                        "Linha " + (i + 1) + " do CSV: ID inválido '"
                                + dados[0] + "'.",
                        e
                );
            }

            Projeto projeto =
                    new Projeto(
                            id,
                            dados[1],
                            dados[2],
                            dados[3],
                            dados[4]
                    );

            projetos.add(projeto);
        }

        return projetos;
    }

    // Evita que o texto digitado quebre o formato do CSV
    private String limpar(String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace(";", ",")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }
}
