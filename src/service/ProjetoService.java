package service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dao.ProjetoCSV;
import model.Projeto;

// Os métodos são "synchronized" porque a API (HttpServer) pode atender
// várias requisições ao mesmo tempo, em threads diferentes.
public class ProjetoService {

    private List<Projeto> projetos;

    private ProjetoCSV dao;

    public ProjetoService() {

        projetos = new ArrayList<>();

        dao = new ProjetoCSV();
    }

    public synchronized void carregar()
            throws IOException {

        projetos = dao.listar();
    }

    public synchronized void salvar()
            throws IOException {

        dao.salvar(projetos);
    }

    // Usado pela tela Swing, que não tem campo de ID no formulário.
    public synchronized int proximoId() {

        int maior = 0;

        for (Projeto projeto : projetos) {

            if (projeto.getId() > maior) {
                maior = projeto.getId();
            }
        }

        return maior + 1;
    }

    public synchronized boolean adicionar(Projeto projeto) {

        if (projeto == null) {

            return false;
        }

        if (projeto.getNome() == null ||
                projeto.getNome().isBlank()) {

            return false;
        }

        if (buscarPorId(projeto.getId()) != null) {

            return false;
        }

        projetos.add(projeto);

        return true;
    }

    // Devolve uma cópia: quem chama não consegue alterar
    // a lista interna do serviço sem passar pelos métodos dele.
    public synchronized List<Projeto> listar() {

        return new ArrayList<>(projetos);
    }

    public synchronized boolean alterar(Projeto projetoAtualizado) {

        if (projetoAtualizado == null) {

            return false;
        }

        if (projetoAtualizado.getNome() == null ||
                projetoAtualizado.getNome().isBlank()) {

            return false;
        }

        Projeto projeto = buscarPorId(projetoAtualizado.getId());

        if (projeto == null) {

            return false;
        }

        projeto.setNome(projetoAtualizado.getNome());
        projeto.setDescricao(projetoAtualizado.getDescricao());
        projeto.setCategoria(projetoAtualizado.getCategoria());
        projeto.setStatus(projetoAtualizado.getStatus());

        return true;
    }

    public synchronized Projeto buscarPorId(int id) {

        for (Projeto projeto : projetos) {

            if (projeto.getId() == id) {

                return projeto;
            }
        }

        return null;
    }

    public synchronized List<Projeto> buscarPorCategoria(
            String categoria
    ) {

        List<Projeto> resultado = new ArrayList<>();

        if (categoria == null) {

            return resultado;
        }

        for (Projeto projeto : projetos) {

            // O null-check está no parâmetro: se a categoria do
            // projeto for null, equalsIgnoreCase devolve false.
            if (categoria.equalsIgnoreCase(projeto.getCategoria())) {

                resultado.add(projeto);
            }
        }

        return resultado;
    }

    public synchronized List<Projeto> buscarPorStatus(
            String status
    ) {

        List<Projeto> resultado = new ArrayList<>();

        if (status == null) {

            return resultado;
        }

        for (Projeto projeto : projetos) {

            if (status.equalsIgnoreCase(projeto.getStatus())) {

                resultado.add(projeto);
            }
        }

        return resultado;
    }

    public synchronized boolean removerPorId(int id) {

        Projeto projeto = buscarPorId(id);

        if (projeto != null) {

            projetos.remove(projeto);

            return true;
        }

        return false;
    }
}
