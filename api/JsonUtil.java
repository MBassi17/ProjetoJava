package api;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Projeto;

public class JsonUtil {

    public static String paraJson(Projeto projeto) {

        StringBuilder json = new StringBuilder();

        json.append("{");
        json.append("\"id\":").append(projeto.getId()).append(",");
        json.append("\"nome\":").append(escrever(projeto.getNome())).append(",");
        json.append("\"descricao\":").append(escrever(projeto.getDescricao())).append(",");
        json.append("\"categoria\":").append(escrever(projeto.getCategoria())).append(",");
        json.append("\"status\":").append(escrever(projeto.getStatus()));
        json.append("}");

        return json.toString();
    }

    public static String listaParaJson(List<Projeto> projetos) {

        StringBuilder json = new StringBuilder();

        json.append("[");

        for (int i = 0; i < projetos.size(); i++) {

            if (i > 0) {
                json.append(",");
            }

            json.append(paraJson(projetos.get(i)));
        }

        json.append("]");

        return json.toString();
    }

    // Lê um objeto JSON simples (como o enviado por POST/PUT) e monta um
    // Projeto. Campos ausentes viram null (texto) ou 0 (id).
    public static Projeto jsonParaProjeto(String json) {

        int id = extrairInt(json, "id");
        String nome = extrairString(json, "nome");
        String descricao = extrairString(json, "descricao");
        String categoria = extrairString(json, "categoria");
        String status = extrairString(json, "status");

        return new Projeto(id, nome, descricao, categoria, status);
    }

    private static String escrever(String valor) {

        if (valor == null) {
            return "null";
        }

        String escapado = valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        return "\"" + escapado + "\"";
    }

    private static String extrairString(String json, String campo) {

        // Aceita \"" e \\ dentro do valor, para não quebrar em textos
        // que contenham aspas.
        Pattern padrao = Pattern.compile(
                "\"" + campo + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );

        Matcher m = padrao.matcher(json);

        if (!m.find()) {
            return null;
        }

        return m.group(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r");
    }

    private static int extrairInt(String json, String campo) {

        Pattern padrao = Pattern.compile(
                "\"" + campo + "\"\\s*:\\s*(-?\\d+)"
        );

        Matcher m = padrao.matcher(json);

        if (!m.find()) {
            return 0;
        }

        return Integer.parseInt(m.group(1));
    }
}
