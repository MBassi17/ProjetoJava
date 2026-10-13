package api;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import model.Projeto;
import service.ProjetoService;

public class Api {

    private static final ProjetoService service = new ProjetoService();

    public static void main(String[] args) throws Exception {

        service.carregar();

        HttpServer server =
                HttpServer.create(new InetSocketAddress(7070), 0);

        server.createContext("/", Api::raiz);
        server.createContext("/api/projetos", Api::projetos);

        // null = Java usa a própria thread do start() para atender as
        // requisições, uma de cada vez. Para o projeto da disciplina isso
        // é suficiente; não precisamos de um pool de threads aqui.
        server.setExecutor(null);
        server.start();

        System.out.println("Servidor iniciado em http://localhost:7070");
    }

    private static void raiz(HttpExchange exchange) throws IOException {

        responderTexto(exchange, 200, "API Sistema de Projetos");
    }

    // Atende GET, POST, PUT e DELETE tanto para /api/projetos
    // quanto para /api/projetos/{id}.
    private static void projetos(HttpExchange exchange) throws IOException {

        try {

            String metodo = exchange.getRequestMethod();
            String caminho = exchange.getRequestURI().getPath();

            // "/api/projetos"      -> ["", "api", "projetos"]        (3 partes)
            // "/api/projetos/5"    -> ["", "api", "projetos", "5"]   (4 partes)
            String[] partes = caminho.split("/");

            Integer id = null;

            if (partes.length == 4) {

                try {

                    id = Integer.parseInt(partes[3]);

                } catch (NumberFormatException e) {

                    responderErro(exchange, 400, "ID inválido na URL.");
                    return;
                }

            } else if (partes.length != 3) {

                responderErro(exchange, 404, "Rota não encontrada.");
                return;
            }

            switch (metodo) {

                case "GET":

                    if (id == null) {
                        listar(exchange);
                    } else {
                        buscarPorId(exchange, id);
                    }
                    break;

                case "POST":

                    if (id != null) {
                        responderErro(exchange, 400,
                                "Use POST em /api/projetos, sem ID na URL.");
                    } else {
                        cadastrar(exchange);
                    }
                    break;

                case "PUT":

                    if (id == null) {
                        responderErro(exchange, 400,
                                "Informe o ID na URL: /api/projetos/{id}");
                    } else {
                        alterar(exchange, id);
                    }
                    break;

                case "DELETE":

                    if (id == null) {
                        responderErro(exchange, 400,
                                "Informe o ID na URL: /api/projetos/{id}");
                    } else {
                        excluir(exchange, id);
                    }
                    break;

                default:

                    responderErro(exchange, 405, "Método não permitido.");
            }

        } catch (Exception e) {

            // Protege o servidor: um erro inesperado não pode derrubar a
            // thread sem responder ao cliente.
            responderErro(exchange, 500, "Erro interno: " + e.getMessage());
        }
    }

    private static void listar(HttpExchange exchange) throws IOException {

        String json = JsonUtil.listaParaJson(service.listar());
        responderJson(exchange, 200, json);
    }

    private static void buscarPorId(HttpExchange exchange, int id)
            throws IOException {

        Projeto projeto = service.buscarPorId(id);

        if (projeto == null) {
            responderErro(exchange, 404, "Projeto não encontrado.");
            return;
        }

        responderJson(exchange, 200, JsonUtil.paraJson(projeto));
    }

    private static void cadastrar(HttpExchange exchange) throws IOException {

        Projeto projeto = JsonUtil.jsonParaProjeto(lerCorpo(exchange));

        if (projeto.getNome() == null || projeto.getNome().isBlank()) {
            responderErro(exchange, 400, "Nome obrigatório.");
            return;
        }

        if (!service.adicionar(projeto)) {
            responderErro(exchange, 400,
                    "Não foi possível cadastrar (ID " + projeto.getId()
                            + " já existe).");
            return;
        }

        try {

            service.salvar();

        } catch (IOException e) {

            // Desfaz o cadastro em memória se não conseguiu gravar no CSV
            service.removerPorId(projeto.getId());
            responderErro(exchange, 500, "Não foi possível salvar: " + e.getMessage());
            return;
        }

        responderJson(exchange, 201, JsonUtil.paraJson(projeto));
    }

    private static void alterar(HttpExchange exchange, int id)
            throws IOException {

        Projeto projeto = JsonUtil.jsonParaProjeto(lerCorpo(exchange));
        projeto.setId(id);

        if (projeto.getNome() == null || projeto.getNome().isBlank()) {
            responderErro(exchange, 400, "Nome obrigatório.");
            return;
        }

        if (!service.alterar(projeto)) {
            responderErro(exchange, 404, "Projeto não encontrado.");
            return;
        }

        try {

            service.salvar();

        } catch (IOException e) {

            responderErro(exchange, 500, "Não foi possível salvar: " + e.getMessage());
            return;
        }

        responderJson(exchange, 200, JsonUtil.paraJson(service.buscarPorId(id)));
    }

    private static void excluir(HttpExchange exchange, int id)
            throws IOException {

        if (!service.removerPorId(id)) {
            responderErro(exchange, 404, "Projeto não encontrado.");
            return;
        }

        try {

            service.salvar();

        } catch (IOException e) {

            responderErro(exchange, 500, "Não foi possível salvar: " + e.getMessage());
            return;
        }

        responderSemConteudo(exchange, 204);
    }

    private static String lerCorpo(HttpExchange exchange) throws IOException {

        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    private static void responderErro(HttpExchange exchange, int status, String mensagem)
            throws IOException {

        String json = "{\"erro\":" + '"' + mensagem.replace("\"", "\\\"") + '"' + "}";
        responderJson(exchange, status, json);
    }

    private static void responderJson(HttpExchange exchange, int status, String json)
            throws IOException {

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);

        OutputStream corpo = exchange.getResponseBody();
        corpo.write(bytes);
        exchange.close();
    }

    private static void responderTexto(HttpExchange exchange, int status, String texto)
            throws IOException {

        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);

        OutputStream corpo = exchange.getResponseBody();
        corpo.write(bytes);
        exchange.close();
    }

    private static void responderSemConteudo(HttpExchange exchange, int status)
            throws IOException {

        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
