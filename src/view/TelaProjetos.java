package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import model.Projeto;
import service.ProjetoService;

public class TelaProjetos extends JFrame {

    private JTextField campoNome;
    private JTextField campoDescricao;
    private JComboBox<String> comboCategoria;
    private JComboBox<String> comboStatus;

    private JTable tabela;
    private DefaultTableModel modelo;

    private JButton botaoCadastrar;
    private JButton botaoLimpar;
    private JButton botaoExcluir;

    private ProjetoService service;

    public TelaProjetos() {

        service = new ProjetoService();

        String erroCarga = null;

        try {
            service.carregar();
        } catch (IOException e) {
            erroCarga = e.getMessage();
        }

        setTitle("Sistema de Projetos");
        setSize(900, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        criarComponentes();
        criarEventos();
        carregarTabela();

        if (erroCarga != null) {

            // Se o CSV não pôde ser lido, bloqueamos cadastro e exclusão.
            // Caso contrário, o próximo "salvar" sobrescreveria o arquivo
            // apenas com os dados novos e os antigos seriam perdidos.
            botaoCadastrar.setEnabled(false);
            botaoExcluir.setEnabled(false);

            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível carregar os projetos:\n" + erroCarga
                            + "\n\nCorrija o arquivo dados/projetos.csv e abra o sistema novamente.",
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void criarComponentes() {

        campoNome = new JTextField(20);
        campoDescricao = new JTextField(20);

        comboCategoria = new JComboBox<>();
        comboCategoria.addItem("Web");
        comboCategoria.addItem("Software");
        comboCategoria.addItem("Mobile");
        comboCategoria.addItem("Outro");

        comboStatus = new JComboBox<>();
        comboStatus.addItem("Planejado");
        comboStatus.addItem("Em desenvolvimento");
        comboStatus.addItem("Concluído");

        botaoCadastrar = new JButton("Cadastrar");
        botaoLimpar = new JButton("Limpar");
        botaoExcluir = new JButton("Excluir selecionado");

        // Tabela somente leitura: sem isso, um duplo clique deixaria
        // editar as células, mudando só a tela e não os dados.
        modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        modelo.addColumn("ID");
        modelo.addColumn("Nome");
        modelo.addColumn("Categoria");
        modelo.addColumn("Status");

        tabela = new JTable(modelo);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Formulário em grade: os campos acompanham a largura da janela
        JPanel painelFormulario = new JPanel(new GridBagLayout());
        painelFormulario.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 0: Nome | Descrição
        adicionarRotulo(painelFormulario, gbc, "Nome:", 0, 0);
        adicionarCampo(painelFormulario, gbc, campoNome, 1, 0);
        adicionarRotulo(painelFormulario, gbc, "Descrição:", 2, 0);
        adicionarCampo(painelFormulario, gbc, campoDescricao, 3, 0);

        // Linha 1: Categoria | Status
        adicionarRotulo(painelFormulario, gbc, "Categoria:", 0, 1);
        adicionarCampo(painelFormulario, gbc, comboCategoria, 1, 1);
        adicionarRotulo(painelFormulario, gbc, "Status:", 2, 1);
        adicionarCampo(painelFormulario, gbc, comboStatus, 3, 1);

        // Linha 2: botões
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelBotoes.add(botaoCadastrar);
        painelBotoes.add(botaoLimpar);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painelFormulario.add(painelBotoes, gbc);

        JPanel painelAcoesTabela = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelAcoesTabela.add(botaoExcluir);

        // BorderLayout: formulário em cima, tabela ocupa o espaço restante
        setLayout(new BorderLayout());

        add(painelFormulario, BorderLayout.NORTH);
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        add(painelAcoesTabela, BorderLayout.SOUTH);
    }

    private void adicionarRotulo(JPanel painel, GridBagConstraints gbc,
                                 String texto, int coluna, int linha) {

        gbc.gridx = coluna;
        gbc.gridy = linha;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;

        painel.add(new JLabel(texto), gbc);
    }

    private void adicionarCampo(JPanel painel, GridBagConstraints gbc,
                                java.awt.Component campo,
                                int coluna, int linha) {

        gbc.gridx = coluna;
        gbc.gridy = linha;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        painel.add(campo, gbc);
    }

    private void criarEventos() {

        botaoLimpar.addActionListener(e -> limparFormulario());
        botaoCadastrar.addActionListener(e -> cadastrar());
        botaoExcluir.addActionListener(e -> excluirSelecionado());
    }

    private void limparFormulario() {

        campoNome.setText("");
        campoDescricao.setText("");
        comboCategoria.setSelectedIndex(0);
        comboStatus.setSelectedIndex(0);
        campoNome.requestFocus();
    }

    private void cadastrar() {

        String nome = campoNome.getText().trim();
        String descricao = campoDescricao.getText().trim();
        String categoria = comboCategoria.getSelectedItem().toString();
        String status = comboStatus.getSelectedItem().toString();

        if (nome.isBlank()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Informe o nome do projeto."
            );
            return;
        }

        Projeto projeto = new Projeto(
                service.proximoId(),
                nome,
                descricao,
                categoria,
                status
        );

        if (!service.adicionar(projeto)) {

            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível cadastrar o projeto."
            );
            return;
        }

        try {
            service.salvar();
        } catch (IOException e) {

            // Desfaz o cadastro em memória, para a tela não mostrar
            // um projeto que não foi gravado no arquivo.
            service.removerPorId(projeto.getId());

            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível salvar o projeto: " + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Projeto cadastrado com sucesso!"
        );

        limparFormulario();
        carregarTabela();
    }

    private void excluirSelecionado() {

        int linhaVisual = tabela.getSelectedRow();

        if (linhaVisual == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Selecione um projeto na tabela."
            );
            return;
        }

        int linha = tabela.convertRowIndexToModel(linhaVisual);

        int id = (int) modelo.getValueAt(linha, 0);

        Projeto projeto = service.buscarPorId(id);

        if (projeto == null) {

            carregarTabela();
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(
                this,
                "Deseja realmente excluir o projeto \""
                        + projeto.getNome() + "\"?",
                "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmacao != JOptionPane.YES_OPTION) {
            return;
        }

        service.removerPorId(id);

        try {
            service.salvar();
        } catch (IOException e) {

            // Desfaz a exclusão em memória
            service.adicionar(projeto);
            carregarTabela();

            JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível salvar a exclusão: " + e.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        carregarTabela();
    }

    private void carregarTabela() {

        modelo.setRowCount(0);

        for (Projeto projeto : service.listar()) {

            modelo.addRow(
                    new Object[]{
                            projeto.getId(),
                            projeto.getNome(),
                            projeto.getCategoria(),
                            projeto.getStatus()
                    }
            );
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            TelaProjetos tela = new TelaProjetos();
            tela.setVisible(true);
        });
    }
}
