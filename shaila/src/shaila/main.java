package shaila;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class main extends Application {

    private TableView<Cliente> tableClientes;
    private ObservableList<Cliente> clientes;

    private TextField tfNome;
    private TextField tfTelefone;
    private TextField tfEmail;

    private TextField tfPesquisaId;
    private TextField tfPesquisaNome;
    private TextField tfPesquisaTelefone;
    private TextField tfPesquisaEmail;

    private TableView<HistoricoContato> tableHistorico;
    private ObservableList<HistoricoContato> historicos;

    private int proximoId = 1;

    private final DateTimeFormatter formatterDataHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void start(Stage primaryStage) {
        clientes = FXCollections.observableArrayList();
        historicos = FXCollections.observableArrayList();

        //cadastro clientes
        tfNome = new TextField();
        tfTelefone = new TextField();
        tfEmail = new TextField();

        tfNome.setPromptText("Nome");
        tfTelefone.setPromptText("Telefone");
        tfEmail.setPromptText("Email");

        Button btnCadastrar = new Button("Cadastrar");
        btnCadastrar.setOnAction(e -> cadastrarCliente());

        HBox formCadastro = new HBox(10, tfNome, tfTelefone, tfEmail, btnCadastrar);
        formCadastro.setPadding(new Insets(10));

        // Campos separados para pesquisa
        tfPesquisaId = new TextField();
        tfPesquisaNome = new TextField();
        tfPesquisaTelefone = new TextField();
        tfPesquisaEmail = new TextField();

        tfPesquisaId.setPromptText("Pesquisar por ID");
        tfPesquisaNome.setPromptText("Pesquisar por Nome");
        tfPesquisaTelefone.setPromptText("Pesquisar por Telefone");
        tfPesquisaEmail.setPromptText("Pesquisar por Email");

        // Add listeners para filtrar ao digitar
        tfPesquisaId.textProperty().addListener((obs, oldV, newV) -> aplicarFiltro());
        tfPesquisaNome.textProperty().addListener((obs, oldV, newV) -> aplicarFiltro());
        tfPesquisaTelefone.textProperty().addListener((obs, oldV, newV) -> aplicarFiltro());
        tfPesquisaEmail.textProperty().addListener((obs, oldV, newV) -> aplicarFiltro());

        HBox boxPesquisa = new HBox(10, tfPesquisaId, tfPesquisaNome, tfPesquisaTelefone, tfPesquisaEmail);
        boxPesquisa.setPadding(new Insets(10));

        Button btnEditar = new Button("Editar");
        btnEditar.setOnAction(e -> editarClienteSelecionado());

        Button btnExcluir = new Button("Excluir");
        btnExcluir.setOnAction(e -> excluirClienteSelecionado());

        Button btnHistorico = new Button("Histórico de Contato");
        btnHistorico.setOnAction(e -> abrirHistoricoContato());

        Button btnExportar = new Button("Exportar Cadastro");
        btnExportar.setOnAction(e -> exportarCadastro(primaryStage));

        Button btnImportar = new Button("Importar Cadastro");
        btnImportar.setOnAction(e -> importarCadastro(primaryStage));

        HBox hboxBotoes = new HBox(10, btnEditar, btnExcluir, btnHistorico, btnExportar, btnImportar);
        hboxBotoes.setPadding(new Insets(10));

        // Tabela de clientes
        tableClientes = new TableView<>();
        TableColumn<Cliente, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Cliente, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Cliente, String> colTelefone = new TableColumn<>("Telefone");
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));

        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        tableClientes.getColumns().addAll(colId, colNome, colTelefone, colEmail);
        tableClientes.setItems(clientes);
        tableClientes.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        VBox root = new VBox(10, formCadastro, boxPesquisa, tableClientes, hboxBotoes);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("Cadastro de Clientes");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void cadastrarCliente() {
        String nome = tfNome.getText().trim();
        String telefone = tfTelefone.getText().trim();
        String email = tfEmail.getText().trim();

        if (nome.isEmpty() || telefone.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erro ao cadastrar", "Preencha todos os campos para cadastrar o cliente.");
            return;
        }

        // Verifica email válido básico
        if (!email.contains("@") || !email.contains(".")) {
            showAlert(Alert.AlertType.ERROR, "Erro ao cadastrar", "Informe um email válido.");
            return;
        }

        Cliente novoCliente = new Cliente(proximoId++, nome, telefone, email);
        clientes.add(novoCliente);

        tfNome.clear();
        tfTelefone.clear();
        tfEmail.clear();

        limparFiltrosPesquisa();

        aplicarFiltro();
    }

    private void limparFiltrosPesquisa() {
        tfPesquisaId.clear();
        tfPesquisaNome.clear();
        tfPesquisaTelefone.clear();
        tfPesquisaEmail.clear();
    }

    private void aplicarFiltro() {
        String filtroId = tfPesquisaId.getText().trim();
        String filtroNome = tfPesquisaNome.getText().toLowerCase().trim();
        String filtroTelefone = tfPesquisaTelefone.getText().toLowerCase().trim();
        String filtroEmail = tfPesquisaEmail.getText().toLowerCase().trim();

        ObservableList<Cliente> filtrados = clientes.filtered(c -> {
            boolean matchesId = true;
            if (!filtroId.isEmpty()) {
                try {
                    int idPesquisa = Integer.parseInt(filtroId);
                    matchesId = c.getId() == idPesquisa;
                } catch (NumberFormatException e) {
                    // se nao for numero, filtro de id não bate com ninguem
                    matchesId = false;
                }
            }
            boolean matchesNome = filtroNome.isEmpty() || c.getNome().toLowerCase().contains(filtroNome);
            boolean matchesTelefone = filtroTelefone.isEmpty() || c.getTelefone().toLowerCase().contains(filtroTelefone);
            boolean matchesEmail = filtroEmail.isEmpty() || c.getEmail().toLowerCase().contains(filtroEmail);

            return matchesId && matchesNome && matchesTelefone && matchesEmail;
        });

        tableClientes.setItems(filtrados);
    }

    private void editarClienteSelecionado() {
        Cliente selecionado = tableClientes.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            showAlert(Alert.AlertType.WARNING, "Aviso", "Selecione um cliente para editar.");
            return;
        }

        Dialog<Cliente> dialog = new Dialog<>();
        dialog.setTitle("Editar Cliente");

        Label labelNome = new Label("Nome:");
        TextField tfNomeEdit = new TextField(selecionado.getNome());

        Label labelTelefone = new Label("Telefone:");
        TextField tfTelefoneEdit = new TextField(selecionado.getTelefone());

        Label labelEmail = new Label("Email:");
        TextField tfEmailEdit = new TextField(selecionado.getEmail());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(labelNome, 0, 0);
        grid.add(tfNomeEdit, 1, 0);
        grid.add(labelTelefone, 0, 1);
        grid.add(tfTelefoneEdit, 1, 1);
        grid.add(labelEmail, 0, 2);
        grid.add(tfEmailEdit, 1, 2);
        grid.setPadding(new Insets(20, 150, 10, 10));

        dialog.getDialogPane().setContent(grid);

        ButtonType btnSalvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSalvar, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSalvar) {
                return new Cliente(selecionado.getId(), tfNomeEdit.getText().trim(),
                        tfTelefoneEdit.getText().trim(), tfEmailEdit.getText().trim());
            }
            return null;
        });

        Optional<Cliente> resultado = dialog.showAndWait();

        resultado.ifPresent(edited -> {
            // Valida campos
            if (edited.getNome().isEmpty() || edited.getTelefone().isEmpty() || edited.getEmail().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erro", "Todos os campos devem estar preenchidos.");
                return;
            }
            if (!edited.getEmail().contains("@") || !edited.getEmail().contains(".")) {
                showAlert(Alert.AlertType.ERROR, "Erro", "Informe um email válido.");
                return;
            }
            // Atualiza cliente
            selecionado.setNome(edited.getNome());
            selecionado.setTelefone(edited.getTelefone());
            selecionado.setEmail(edited.getEmail());
            tableClientes.refresh();
            aplicarFiltro();
        });
    }

    private void excluirClienteSelecionado() {
        Cliente selecionado = tableClientes.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            showAlert(Alert.AlertType.WARNING, "Aviso", "Selecione um cliente para excluir.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmação");
        confirm.setHeaderText(null);
        confirm.setContentText("Confirma a exclusão do cliente ID " + selecionado.getId() + " - " + selecionado.getNome() + "?");

        Optional<ButtonType> resposta = confirm.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.OK) {
            clientes.remove(selecionado);
            // Remove históricos associados
            historicos.removeIf(h -> h.getClienteId() == selecionado.getId());
            aplicarFiltro();
        }
    }

    private void abrirHistoricoContato() {
        Cliente selecionado = tableClientes.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            showAlert(Alert.AlertType.WARNING, "Aviso", "Selecione um cliente para adicionar/consultar histórico.");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Histórico de Contatos - Cliente: " + selecionado.getNome());

        // Form para adicionar histórico
        TextField tfMeioContato = new TextField();
        tfMeioContato.setPromptText("Meio de contato (ex: telefone, email)");

        TextField tfSolicitacao = new TextField();
        tfSolicitacao.setPromptText("O que foi solicitado/registrado");

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(120);

        TextField tfHora = new TextField();
        tfHora.setPromptText("HH:mm");
        tfHora.setPrefWidth(60);

        CheckBox cbDataHoraAtual = new CheckBox("Usar Data e Hora Atual (Horário de Brasília)");
        cbDataHoraAtual.setSelected(true);

        datePicker.setDisable(true);
        tfHora.setDisable(true);

        cbDataHoraAtual.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // Se marcado, desabilita campos de data e hora
            datePicker.setDisable(newVal);
            tfHora.setDisable(newVal);
        });

        Button btnAdicionarHist = new Button("Adicionar Contato");
        btnAdicionarHist.setOnAction(e -> {
            String meio = tfMeioContato.getText().trim();
            String sol = tfSolicitacao.getText().trim();
            if (meio.isEmpty() || sol.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erro", "Preencha todos os campos do histórico.");
                return;
            }
            LocalDateTime dataHora;
            if (cbDataHoraAtual.isSelected()) {
                ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
                dataHora = zonedDateTime.toLocalDateTime();
            } else {
                LocalDate data = datePicker.getValue();
                String horaStr = tfHora.getText().trim();
                if (data == null) {
                    showAlert(Alert.AlertType.ERROR, "Erro", "Selecione a data.");
                    return;
                }
                if (!horaStr.matches("^([01]?\\d|2[0-3]):[0-5]\\d$")) {
                    showAlert(Alert.AlertType.ERROR, "Erro", "Informe a hora no formato HH:mm (ex: 14:30).");
                    return;
                }
                LocalTime hora = LocalTime.parse(horaStr);
                dataHora = LocalDateTime.of(data, hora);
            }
            historicos.add(new HistoricoContato(selecionado.getId(), meio, sol, dataHora));
            tfMeioContato.clear();
            tfSolicitacao.clear();
            if (!cbDataHoraAtual.isSelected()) {
                datePicker.setValue(LocalDate.now());
                tfHora.clear();
            }
            atualizarTabelaHistorico(selecionado.getId());
        });

        HBox formHistorico = new HBox(10,
                tfMeioContato,
                tfSolicitacao,
                datePicker,
                tfHora,
                cbDataHoraAtual,
                btnAdicionarHist);
        formHistorico.setPadding(new Insets(10));

        // tabela histórico de contatos
        tableHistorico = new TableView<>();

        TableColumn<HistoricoContato, String> colMeio = new TableColumn<>("Meio de Contato");
        colMeio.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMeioContato()));

        TableColumn<HistoricoContato, String> colSol = new TableColumn<>("Solicitação");
        colSol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSolicitacao()));

        TableColumn<HistoricoContato, String> colDataHora = new TableColumn<>("Data e Hora");
        colDataHora.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDataHoraFormatada()));

        tableHistorico.getColumns().addAll(colMeio, colSol, colDataHora);

        atualizarTabelaHistorico(selecionado.getId());

        VBox layout = new VBox(10, formHistorico, tableHistorico);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 950, 400);
        stage.setScene(scene);
        stage.show();
    }

    private void atualizarTabelaHistorico(int clienteId) {
        List<HistoricoContato> filtrado = historicos.stream()
                .filter(h -> h.getClienteId() == clienteId)
                .sorted(Comparator.comparing(HistoricoContato::getDataHora).reversed())
                .collect(Collectors.toList());
        tableHistorico.setItems(FXCollections.observableArrayList(filtrado));
    }

    private void exportarCadastro(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar Cadastro");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV file (*.csv)", "*.csv"));
        File arquivo = fileChooser.showSaveDialog(stage);
        if (arquivo != null) {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8))) {
                // Escreve clientes e históricos
                writer.println("CLIENTES");
                writer.println("id;nome;telefone;email");
                for (Cliente c : clientes) {
                    writer.printf("%d;%s;%s;%s%n",
                            c.getId(),
                            escapeCSV(c.getNome()),
                            escapeCSV(c.getTelefone()),
                            escapeCSV(c.getEmail()));
                }
                writer.println("HISTORICO");
                writer.println("clienteId;meioContato;solicitacao;dataHora");
                for (HistoricoContato h : historicos) {
                    writer.printf("%d;%s;%s;%s%n",
                            h.getClienteId(),
                            escapeCSV(h.getMeioContato()),
                            escapeCSV(h.getSolicitacao()),
                            h.getDataHora().format(formatterDataHora));
                }
                showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Exportação concluída.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao exportar o arquivo.\n" + ex.getMessage());
            }
        }
    }

    private void importarCadastro(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Importar Cadastro");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV file (*.csv)", "*.csv"));
        File arquivo = fileChooser.showOpenDialog(stage);
        if (arquivo != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8))) {
                List<Cliente> clientesImportados = new ArrayList<>();
                List<HistoricoContato> historicosImportados = new ArrayList<>();
                String linha;
                boolean lerClientes = false;
                boolean lerHistorico = false;

                while ((linha = reader.readLine()) != null) {
                    linha = linha.trim();
                    if (linha.equalsIgnoreCase("CLIENTES")) {
                        lerClientes = true;
                        lerHistorico = false;
                        reader.readLine(); // pula cabeçalho
                        continue;
                    }
                    if (linha.equalsIgnoreCase("HISTORICO")) {
                        lerHistorico = true;
                        lerClientes = false;
                        reader.readLine(); // pula cabeçalho
                        continue;
                    }
                    if (lerClientes) {
                        // linha formato: id;nome;telefone;email
                        String[] partes = linha.split(";", -1);
                        if (partes.length == 4) {
                            int id = Integer.parseInt(partes[0]);
                            String nome = partes[1];
                            String telefone = partes[2];
                            String email = partes[3];
                            clientesImportados.add(new Cliente(id, nome, telefone, email));
                        }
                    } else if (lerHistorico) {
                        // linha formato: clienteId;meioContato;solicitacao;dataHora
                        String[] partes = linha.split(";", -1);
                        if (partes.length >= 4) {
                            int clienteId = Integer.parseInt(partes[0]);
                            String meio = partes[1];
                            String sol = partes[2];
                            String dataHoraStr = partes[3];
                            LocalDateTime dt;
                            try {
                                dt = LocalDateTime.parse(dataHoraStr, formatterDataHora);
                            } catch (Exception ex) {
                                dt = LocalDateTime.now();
                            }
                            historicosImportados.add(new HistoricoContato(clienteId, meio, sol, dt));
                        }
                    }
                }

                if (!clientesImportados.isEmpty()) {
                    clientes.clear();
                    clientes.addAll(clientesImportados);
                    // Recalcula proximoId
                    proximoId = clientes.stream().mapToInt(Cliente::getId).max().orElse(0) + 1;
                }
                if (!historicosImportados.isEmpty()) {
                    historicos.clear();
                    historicos.addAll(historicosImportados);
                }
                aplicarFiltro();
                showAlert(Alert.AlertType.INFORMATION, "Sucesso", "Importação concluída.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erro", "Erro ao importar o arquivo.\n" + ex.getMessage());
            }
        }
    }

    private String escapeCSV(String s) {
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            s = "\"" + s + "\"";
        }
        return s;
    }

    private void showAlert(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

// Classes internas para modelo Cliente e Histórico
    public static class Cliente {
        private final int id;
        private String nome;
        private String telefone;
        private String email;

        public Cliente(int id, String nome, String telefone, String email) {
            this.id = id;
            this.nome = nome;
            this.telefone = telefone;
            this.email = email;
        }

        public int getId() {
            return id;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class HistoricoContato {
        private final int clienteId;
        private final String meioContato;
        private final String solicitacao;
        private final LocalDateTime dataHora;

        public HistoricoContato(int clienteId, String meioContato, String solicitacao, LocalDateTime dataHora) {
            this.clienteId = clienteId;
            this.meioContato = meioContato;
            this.solicitacao = solicitacao;
            this.dataHora = dataHora;
        }

        public int getClienteId() {
            return clienteId;
        }

        public String getMeioContato() {
            return meioContato;
        }

        public String getSolicitacao() {
            return solicitacao;
        }

        public LocalDateTime getDataHora() {
            return dataHora;
        }

        public String getDataHoraFormatada() {
            return dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

