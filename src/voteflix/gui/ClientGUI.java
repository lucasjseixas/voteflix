package voteflix.gui;

import voteflix.dto.UsuarioDTO;
import voteflix.dto.request.*;
import voteflix.dto.response.*;
import voteflix.util.HttpStatus;
import com.google.gson.Gson;

import voteflix.dto.FilmeDTO;
import voteflix.dto.request.CriarFilmeRequest;
import voteflix.dto.request.EditarFilmeRequest;
import voteflix.dto.request.ExcluirFilmeRequest;
import voteflix.dto.request.ListarFilmesRequest;
import voteflix.dto.response.ListarFilmesResponse;
import java.util.ArrayList;

import voteflix.dto.ReviewDTO;
import voteflix.dto.request.CriarReviewRequest;
import voteflix.dto.request.EditarReviewRequest;
import voteflix.dto.request.ExcluirReviewRequest;
import voteflix.dto.request.ListarReviewsUsuarioRequest;
import voteflix.dto.request.BuscarFilmeIdRequest;
import voteflix.dto.response.ListarReviewsUsuarioResponse;
import voteflix.dto.response.BuscarFilmeIdResponse;

import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientGUI extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new Gson();

    // Componentes de Conexão
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;

    // Componentes de Autenticação
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JButton registerButton;

    // Componentes de Interface Principal
    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel userInfoLabel;
    private JPanel operationsPanel;

    // Networking
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    // Sessão
    private String currentToken = null;
    private String currentUsuario = null;
    private String currentFuncao = null;

    public ClientGUI() {
        setTitle("VoteFlix - Cliente");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Painel Superior - Conexão
        JPanel topPanel = createConnectionPanel();
        add(topPanel, BorderLayout.NORTH);

        // Painel Central - Split entre Operações e Logs
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.4);

        // Painel Esquerdo - Operações
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(new EmptyBorder(5, 10, 5, 5));

        // Painel de Login/Registro
        JPanel authPanel = createAuthPanel();
        leftPanel.add(authPanel, BorderLayout.NORTH);

        // Painel de Operações (inicialmente vazio)
        operationsPanel = new JPanel();
        operationsPanel.setLayout(new BoxLayout(operationsPanel, BoxLayout.Y_AXIS));
        operationsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Operações Disponíveis",
                0, 0, new Font("Arial", Font.BOLD, 12), new Color(70, 130, 180)
        ));

        JScrollPane operationsScroll = new JScrollPane(operationsPanel);
        leftPanel.add(operationsScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // Painel Direito - Logs
        JPanel logPanel = createLogPanel();
        splitPane.setRightComponent(logPanel);

        add(splitPane, BorderLayout.CENTER);

        // Painel Inferior - Status
        JPanel bottomPanel = createStatusPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Conexão com Servidor",
                0, 0, new Font("Arial", Font.BOLD, 12), new Color(70, 130, 180)
        ));

        panel.add(new JLabel("IP:"));
        ipField = new JTextField("127.0.0.1", 12);
        panel.add(ipField);

        panel.add(new JLabel("Porta:"));
        portField = new JTextField("20000", 8);
        panel.add(portField);

        connectButton = new JButton("Conectar");
        connectButton.setBackground(new Color(34, 139, 34));
        connectButton.setForeground(Color.BLACK);
        connectButton.setFont(new Font("Arial", Font.BOLD, 11));
        connectButton.setFocusPainted(false);
        connectButton.addActionListener(e -> toggleConnection());
        panel.add(connectButton);

        return panel;
    }

    private JPanel createAuthPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                "Autenticação",
                0, 0, new Font("Arial", Font.BOLD, 12), new Color(100, 149, 237)
        ));

        // Campo Usuário
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.add(new JLabel("Usuário:"));
        userField = new JTextField(15);
        userPanel.add(userField);
        panel.add(userPanel);

        // Campo Senha
        JPanel passPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passPanel.add(new JLabel("Senha:   "));
        passField = new JPasswordField(15);
        passPanel.add(passField);
        panel.add(passPanel);

        // Botões
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        loginButton = new JButton("Login");
        loginButton.setEnabled(false);
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.BLACK);
        loginButton.addActionListener(e -> handleLogin());
        buttonPanel.add(loginButton);

        registerButton = new JButton("Registrar");
        registerButton.setEnabled(false);
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.BLACK);
        registerButton.addActionListener(e -> handleRegister());
        buttonPanel.add(registerButton);

        panel.add(buttonPanel);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                "Log de Comunicação",
                0, 0, new Font("Arial", Font.BOLD, 12)
        ));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 10));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JButton clearButton = new JButton("Limpar Logs");
        clearButton.addActionListener(e -> logArea.setText(""));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        statusLabel = new JLabel("● Status: Desconectado");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(Color.RED);

        userInfoLabel = new JLabel("");
        userInfoLabel.setFont(new Font("Arial", Font.BOLD, 11));
        userInfoLabel.setForeground(new Color(70, 130, 180));

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(userInfoLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel criarCardFilme(FilmeDTO filme) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                new EmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        // Painel esquerdo - Info principal
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        JLabel tituloLabel = new JLabel(filme.titulo);
        tituloLabel.setFont(new Font("Arial", Font.BOLD, 16));
        tituloLabel.setForeground(new Color(70, 130, 180));

        JLabel detalhesLabel = new JLabel("Diretor: " + filme.diretor + " | Ano: " + filme.ano);
        detalhesLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel generosLabel = new JLabel("Gêneros: " + String.join(", ", filme.genero));
        generosLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        generosLabel.setForeground(Color.GRAY);

        JLabel sinopseLabel = new JLabel("<html><p style='width:500px'>" + filme.sinopse + "</p></html>");
        sinopseLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        infoPanel.add(tituloLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(detalhesLabel);
        infoPanel.add(generosLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(sinopseLabel);

        // Painel direito - Nota
        JPanel notaPanel = new JPanel();
        notaPanel.setLayout(new BoxLayout(notaPanel, BoxLayout.Y_AXIS));
        notaPanel.setBackground(Color.WHITE);
        notaPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel notaLabel = new JLabel(filme.nota);
        notaLabel.setFont(new Font("Arial", Font.BOLD, 24));
        notaLabel.setForeground(new Color(255, 165, 0));
        notaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel avalLabel = new JLabel("(" + filme.qtdAvaliacoes + " avaliações)");
        avalLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        avalLabel.setForeground(Color.GRAY);
        avalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel idLabel = new JLabel("ID: " + filme.id);
        idLabel.setFont(new Font("Monospaced", Font.PLAIN, 9));
        idLabel.setForeground(Color.LIGHT_GRAY);
        idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        notaPanel.add(notaLabel);
        notaPanel.add(avalLabel);
        notaPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        notaPanel.add(idLabel);

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(notaPanel, BorderLayout.EAST);

        return card;
    }

    private void toggleConnection() {
        if (!connected) {
            connect();
        } else {
            disconnect();
        }
    }

    private void connect() {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();

        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite o IP do servidor!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);

            if (port < 20000 || port > 25000) {
                JOptionPane.showMessageDialog(this, "Porta inválida! Use valores entre 20000 e 25000.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            addLog("=".repeat(50));
            addLog("INICIANDO CONEXÃO");
            addLog("Servidor: " + ip + ":" + port);

            socket = new Socket(ip, port);

            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8),
                    true
            );
            in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)
            );

            connected = true;
            ipField.setEnabled(false);
            portField.setEnabled(false);
            connectButton.setText("Desconectar");
            connectButton.setBackground(new Color(178, 34, 34));
            statusLabel.setText("● Status: Conectado");
            statusLabel.setForeground(new Color(34, 139, 34));

            loginButton.setEnabled(true);
            registerButton.setEnabled(true);

            addLog("CONECTADO COM SUCESSO!");
            addLog("=".repeat(50));

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porta inválida! Digite apenas números.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            addLog("✗ FALHA NA CONEXÃO: " + e.getMessage());
        }
    }

    private void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();

            connected = false;
            ipField.setEnabled(true);
            portField.setEnabled(true);
            connectButton.setText("Conectar");
            connectButton.setBackground(new Color(34, 139, 34));
            statusLabel.setText("● Status: Desconectado");
            statusLabel.setForeground(Color.RED);

            loginButton.setEnabled(false);
            registerButton.setEnabled(false);

            clearOperationsPanel();
            clearSession();

            addLog("=".repeat(50));
            addLog("DESCONECTADO");
            addLog("=".repeat(50));

        } catch (IOException e) {
            addLog("ERRO ao desconectar: " + e.getMessage());
        }
    }

    private void handleLogin() {
        if (currentToken != null) {
            JOptionPane.showMessageDialog(this,
                    "Você já está logado como " + currentUsuario + "!\nFaça logout antes de logar com outra conta.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String usuario = userField.getText().trim();
        String senha = new String(passField.getPassword());

        if (usuario.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha usuário e senha!", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                LoginRequest request = new LoginRequest(usuario, senha);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: LOGIN");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA LOGIN");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                LoginResponse response = GSON.fromJson(jsonResponse, LoginResponse.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                if (status == HttpStatus.OK && response.token != null) {
                    currentToken = response.token;
                    currentUsuario = usuario;

                    currentFuncao = usuario.equals("admin") ? "admin" : "user";

                    SwingUtilities.invokeLater(() -> {
                        userField.setText("");
                        passField.setText("");
                        loginButton.setEnabled(false);
                        registerButton.setEnabled(false);

                        userInfoLabel.setText("👤 " + currentUsuario + " (" + currentFuncao + ")");
                        loadUserOperations();
                        JOptionPane.showMessageDialog(this,
                                "Login realizado com sucesso!\nBem-vindo, " + currentUsuario,
                                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                status.getMessage(),
                                "Erro no Login", JOptionPane.ERROR_MESSAGE);
                    });
                }

            } catch (IOException e) {
                addLog("ERRO ao fazer login: " + e.getMessage());
            }
        }).start();
    }

    private void handleRegister() {
        String usuario = userField.getText().trim();
        String senha = new String(passField.getPassword());

        if (usuario.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha usuário e senha!", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (usuario.length() < 3 || usuario.length() > 20 || senha.length() < 3 || senha.length() > 20) {
            JOptionPane.showMessageDialog(this, "Usuário e senha devem ter entre 3 e 20 caracteres!", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                CadastrarUsuarioRequest request = new CadastrarUsuarioRequest(usuario, senha);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: CRIAR_USUARIO");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA CRIAR_USUARIO");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    if (status == HttpStatus.CREATED) {
                        JOptionPane.showMessageDialog(this,
                                status.getMessage(),
                                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        passField.setText("");
                    } else {
                        JOptionPane.showMessageDialog(this,
                                status.getMessage(),
                                "Erro no Cadastro", JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (IOException e) {
                addLog("ERRO ao cadastrar: " + e.getMessage());
            }
        }).start();
    }

    private void loadUserOperations() {
        operationsPanel.removeAll();

        addOperationButton("Ver Catálogo de Filmes", this::handleVerFilmes);

        if ("admin".equals(currentFuncao)) {
            addOperationButton("Listar Todos os Usuários", this::handleListUsers);
            addOperationButton("Editar Usuário (por ID)", this::handleEditUser);
            addOperationButton("Excluir Usuário (por ID)", this::handleDeleteUser);
            addSeparator("GERENCIAR FILMES");
            addOperationButton("Adicionar Filme", this::handleAdicionarFilme);
            addOperationButton("Buscar Filme por ID", this::handleBuscarFilmeComReviews);
            addOperationButton("Editar Filme", this::handleEditarFilme);
            addOperationButton("Excluir Filme", this::handleExcluirFilme);
            addOperationButton("Criar Review", this::handleCriarReview);
            addSeparator("GERENCIAR REVIEWS");
            addOperationButton("Listar Minhas Reviews", this::handleListarMinhasReviews);
            addOperationButton("Editar Review", this::handleEditarReview);
            addOperationButton("Excluir Review", this::handleExcluirReview);
            addSeparator("MINHA CONTA");
            addOperationButton("Listar Meus Dados", this::handleListMyself);
            addOperationButton("Atualizar Senha", this::handleUpdatePassword);
            addOperationButton("Excluir Conta", this::handleDeleteAccount);
        } else {
            addSeparator("GERENCIAR FILMES");
            addOperationButton("Buscar Filme por ID", this::handleBuscarFilmeComReviews);
            addSeparator("MINHAS REVIEWS");
            addOperationButton("Criar Review", this::handleCriarReview);
            addOperationButton("Listar Minhas Reviews", this::handleListarMinhasReviews);
            addOperationButton("Editar Review", this::handleEditarReview);
            addOperationButton("Excluir Review", this::handleExcluirReview);
            addSeparator("MINHA CONTA");
            addOperationButton("Listar Meus Dados", this::handleListMyself);
            addOperationButton("Atualizar Senha", this::handleUpdatePassword);
            addOperationButton("Excluir Conta", this::handleDeleteAccount);
        }

        addOperationButton("Logout", this::handleLogout);

        operationsPanel.revalidate();
        operationsPanel.repaint();
    }

    private void mostrarFormularioEdicao(FilmeDTO filmeOriginal) {
        JDialog dialog = new JDialog(this, "Editar Filme ID " + filmeOriginal.id, true);
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField tituloField = new JTextField(filmeOriginal.titulo);
        JTextField diretorField = new JTextField(filmeOriginal.diretor);
        JTextField anoField = new JTextField(filmeOriginal.ano);
        JTextArea sinopseArea = new JTextArea(filmeOriginal.sinopse, 3, 20);
        sinopseArea.setLineWrap(true);
        sinopseArea.setWrapStyleWord(true);

        String[] generosDisponiveis = {
                "Ação", "Aventura", "Comédia", "Drama", "Fantasia",
                "Ficção Científica", "Terror", "Romance", "Documentário", "Musical", "Animação"
        };

        JPanel generoPanel = new JPanel(new GridLayout(0, 2));
        java.util.List<JCheckBox> generoCheckboxes = new ArrayList<>();
        for (String genero : generosDisponiveis) {
            JCheckBox cb = new JCheckBox(genero);
            cb.setSelected(filmeOriginal.genero.contains(genero));
            generoCheckboxes.add(cb);
            generoPanel.add(cb);
        }

        formPanel.add(new JLabel("Título (max 30):"));
        formPanel.add(tituloField);
        formPanel.add(new JLabel("Diretor:"));
        formPanel.add(diretorField);
        formPanel.add(new JLabel("Ano (YYYY):"));
        formPanel.add(anoField);
        formPanel.add(new JLabel("Sinopse (max 250):"));
        formPanel.add(new JScrollPane(sinopseArea));
        formPanel.add(new JLabel("Gêneros:"));
        formPanel.add(new JScrollPane(generoPanel));

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.setBorder(new EmptyBorder(0, 20, 10, 20));
        JLabel infoLabel = new JLabel("⭐ Nota atual: " + filmeOriginal.nota +
                " (" + filmeOriginal.qtdAvaliacoes + " avaliações) - não pode ser editada");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        infoLabel.setForeground(Color.GRAY);
        infoPanel.add(infoLabel);
        dialog.add(infoPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        JButton salvarButton = new JButton("Salvar Alterações");
        JButton cancelarButton = new JButton("Cancelar");

        salvarButton.addActionListener(e -> {
            String titulo = tituloField.getText().trim();
            String diretor = diretorField.getText().trim();
            String ano = anoField.getText().trim();
            String sinopse = sinopseArea.getText().trim();

            java.util.List<String> generosSelecionados = new ArrayList<>();
            for (JCheckBox cb : generoCheckboxes) {
                if (cb.isSelected()) {
                    generosSelecionados.add(cb.getText());
                }
            }

            if (titulo.isEmpty() || diretor.isEmpty() || ano.isEmpty() ||
                    sinopse.isEmpty() || generosSelecionados.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Todos os campos são obrigatórios!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (titulo.length() > 30 || ano.length() != 4 || sinopse.length() > 250) {
                JOptionPane.showMessageDialog(dialog,
                        "Verifique os limites de caracteres!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dialog.dispose();
            enviarEditarFilme(filmeOriginal.id, titulo, diretor, ano, generosSelecionados, sinopse);
        });

        cancelarButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(salvarButton);
        buttonPanel.add(cancelarButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void addSeparator(String texto) {
        JLabel separator = new JLabel("─── " + texto + " ───");
        separator.setFont(new Font("Arial", Font.BOLD, 11));
        separator.setForeground(new Color(100, 100, 100));
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);

        operationsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        operationsPanel.add(separator);
        operationsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    private void addOperationButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.addActionListener(e -> new Thread(action).start());

        operationsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        operationsPanel.add(button);
    }

    private void clearOperationsPanel() {
        operationsPanel.removeAll();
        operationsPanel.revalidate();
        operationsPanel.repaint();
    }

    private void clearSession() {
        currentToken = null;
        currentUsuario = null;
        currentFuncao = null;
        userInfoLabel.setText("");
        userField.setText("");
        passField.setText("");

        if (connected) {
            loginButton.setEnabled(true);
            registerButton.setEnabled(true);
        }
    }

    private void handleListUsers() {
        if (currentToken == null) {
            JOptionPane.showMessageDialog(this, "Você precisa estar logado!", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                ListarUsuariosRequest request = new ListarUsuariosRequest(currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: LISTAR_USUARIOS");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA LISTAR_USUARIOS");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ListarUsuariosResponse response = GSON.fromJson(jsonResponse, ListarUsuariosResponse.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    if (status == HttpStatus.OK && response.usuarios != null) {
                        StringBuilder sb = new StringBuilder("=== LISTA DE USUÁRIOS ===\n\n");
                        for (UsuarioDTO user : response.usuarios) {
                            sb.append("ID: ").append(user.id)
                                    .append(" | Nome: ").append(user.nome)
                                    .append("\n");
                        }
                        sb.append("\nTotal: ").append(response.usuarios.size()).append(" usuário(s)");

                        JTextArea textArea = new JTextArea(sb.toString());
                        textArea.setEditable(false);
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(400, 300));

                        JOptionPane.showMessageDialog(this, scrollPane, "Lista de Usuários", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, status.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleEditUser() {
        if (currentToken == null) return;

        String id = JOptionPane.showInputDialog(this, "Digite o ID do usuário:");
        if (id == null || id.isEmpty()) return;

        String novaSenha = JOptionPane.showInputDialog(this, "Digite a nova senha (3-20 caracteres):");
        if (novaSenha == null || novaSenha.isEmpty()) return;

        if (novaSenha.length() < 3 || novaSenha.length() > 20) {
            JOptionPane.showMessageDialog(this, "Senha deve ter entre 3 e 20 caracteres!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                AdminEditarUsuarioRequest request = new AdminEditarUsuarioRequest(novaSenha, currentToken, id);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: ADMIN_EDITAR_USUARIO");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA ADMIN_EDITAR_USUARIO");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status.getMessage(),
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleDeleteUser() {
        if (currentToken == null) return;

        String id = JOptionPane.showInputDialog(this, "Digite o ID do usuário a excluir:");
        if (id == null || id.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja excluir o usuário ID " + id + "?",
                "Confirmar Exclusão",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                AdminExcluirUsuarioRequest request = new AdminExcluirUsuarioRequest(id, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: ADMIN_EXCLUIR_USUARIO");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA ADMIN_EXCLUIR_USUARIO");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status.getMessage(),
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleListMyself() {
        if (currentToken == null) return;

        new Thread(() -> {
            try {
                ListarProprioUsuarioRequest request = new ListarProprioUsuarioRequest(currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: LISTAR_PROPRIO_USUARIO");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA LISTAR_PROPRIO_USUARIO");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ListarProprioUsuarioResponse response = GSON.fromJson(jsonResponse, ListarProprioUsuarioResponse.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    if (status == HttpStatus.OK) {
                        String info = "=== MEUS DADOS ===\n\n" +
                                "Usuário: " + response.usuario + "\n" +
                                "Função: " + currentFuncao;
                        JOptionPane.showMessageDialog(this, info, "Meus Dados", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, status.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleUpdatePassword() {
        if (currentToken == null) return;

        String novaSenha = JOptionPane.showInputDialog(this, "Digite a nova senha (3-20 caracteres):");
        if (novaSenha == null || novaSenha.isEmpty()) return;

        if (novaSenha.length() < 3 || novaSenha.length() > 20) {
            JOptionPane.showMessageDialog(this, "Senha deve ter entre 3 e 20 caracteres!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                EditarProprioUsuarioRequest request = new EditarProprioUsuarioRequest(novaSenha, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: EDITAR_PROPRIO_USUARIO");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA EDITAR_PROPRIO_USUARIO");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status,
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleDeleteAccount() {
        if (currentToken == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "ATENÇÃO: Esta ação é IRREVERSÍVEL!\n\nTem certeza que deseja excluir sua conta?\nVocê será desconectado após a exclusão.",
                "Confirmar Exclusão de Conta",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                ExcluirProprioUsuarioRequest request = new ExcluirProprioUsuarioRequest(currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: EXCLUIR_PROPRIO_USUARIO");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA EXCLUIR_PROPRIO_USUARIO");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    if (status == HttpStatus.OK) {
                        JOptionPane.showMessageDialog(this,
                                "Conta excluída com sucesso!\nVocê será desconectado.",
                                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        disconnect();
                    } else if (status == HttpStatus.FORBIDDEN) {
                        JOptionPane.showMessageDialog(this, status.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                status.getMessage() + "\nVocê será desconectado por segurança.",
                                "Erro", JOptionPane.ERROR_MESSAGE);
                        disconnect();
                    }
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    disconnect();
                });
            }
        }).start();
    }

    private void handleLogout() {
        if (currentToken == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Deseja fazer logout e desconectar do servidor?",
                "Confirmar Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                LogoutRequest request = new LogoutRequest(currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: LOGOUT");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);

                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA LOGOUT");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    if (status == HttpStatus.OK) {
                        JOptionPane.showMessageDialog(this,
                                "Logout realizado com sucesso!\nVocê será desconectado.",
                                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                status.getMessage() + "\nVocê será desconectado por segurança.",
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                    }

                    disconnect();
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    disconnect();
                });
            }
        }).start();
    }

    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // Operações de filmes

    private void handleVerFilmes() {
        new Thread(() -> {
            try {
                ListarFilmesRequest request = new ListarFilmesRequest(currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: LISTAR_FILMES");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│  RECEBIDO: RESPOSTA LISTAR_FILMES");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ListarFilmesResponse response = GSON.fromJson(jsonResponse, ListarFilmesResponse.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    if (status == HttpStatus.OK && response.filmes != null) {
                        mostrarCatalogoFilmes(response.filmes);
                    } else {
                        JOptionPane.showMessageDialog(this,
                                status.getMessage(),
                                "Erro", JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleAdicionarFilme() {
        if (currentToken == null) return;

        JDialog dialog = new JDialog(this, "➕ Adicionar Novo Filme", true);
        dialog.setSize(500, 550);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField tituloField = new JTextField();
        JTextField diretorField = new JTextField();
        JTextField anoField = new JTextField();
        JTextArea sinopseArea = new JTextArea(3, 20);
        sinopseArea.setLineWrap(true);
        sinopseArea.setWrapStyleWord(true);

        String[] generosDisponiveis = {
                "Ação", "Aventura", "Comédia", "Drama", "Fantasia",
                "Ficção Científica", "Terror", "Romance", "Documentário", "Musical", "Animação"
        };

        JPanel generoPanel = new JPanel(new GridLayout(0, 2));
        java.util.List<JCheckBox> generoCheckboxes = new ArrayList<>();
        for (String genero : generosDisponiveis) {
            JCheckBox cb = new JCheckBox(genero);
            generoCheckboxes.add(cb);
            generoPanel.add(cb);
        }

        formPanel.add(new JLabel("Título (max 30):"));
        formPanel.add(tituloField);
        formPanel.add(new JLabel("Diretor:"));
        formPanel.add(diretorField);
        formPanel.add(new JLabel("Ano (YYYY):"));
        formPanel.add(anoField);
        formPanel.add(new JLabel("Sinopse (max 250):"));
        formPanel.add(new JScrollPane(sinopseArea));
        formPanel.add(new JLabel("Gêneros:"));
        formPanel.add(new JScrollPane(generoPanel));

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton salvarButton = new JButton("Salvar");
        JButton cancelarButton = new JButton("Cancelar");

        salvarButton.addActionListener(e -> {
            String titulo = tituloField.getText().trim();
            String diretor = diretorField.getText().trim();
            String ano = anoField.getText().trim();
            String sinopse = sinopseArea.getText().trim();

            java.util.List<String> generosSelecionados = new ArrayList<>();
            for (JCheckBox cb : generoCheckboxes) {
                if (cb.isSelected()) {
                    generosSelecionados.add(cb.getText());
                }
            }

            if (titulo.isEmpty() || diretor.isEmpty() || ano.isEmpty() ||
                    sinopse.isEmpty() || generosSelecionados.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Todos os campos são obrigatórios!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (titulo.length() > 30 || ano.length() != 4 || sinopse.length() > 250) {
                JOptionPane.showMessageDialog(dialog,
                        "Verifique os limites de caracteres!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dialog.dispose();
            enviarCriarFilme(titulo, diretor, ano, generosSelecionados, sinopse);
        });

        cancelarButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(salvarButton);
        buttonPanel.add(cancelarButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void enviarCriarFilme(String titulo, String diretor, String ano,
                                  java.util.List<String> generos, String sinopse) {
        new Thread(() -> {
            try {
                CriarFilmeRequest.FilmeData filmeData = new CriarFilmeRequest.FilmeData(
                        titulo, diretor, ano, generos, sinopse
                );
                CriarFilmeRequest request = new CriarFilmeRequest(filmeData, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: CRIAR_FILME");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA CRIAR_FILME");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status.getMessage(),
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleEditarFilme() {
        if (currentToken == null) return;

        new Thread(() -> {
            try {
                ListarFilmesRequest request = new ListarFilmesRequest(currentToken);
                String jsonRequest = GSON.toJson(request);

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                ListarFilmesResponse response = GSON.fromJson(jsonResponse, ListarFilmesResponse.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                if (status == HttpStatus.OK && response.filmes != null && !response.filmes.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        String[] opcoes = new String[response.filmes.size()];
                        for (int i = 0; i < response.filmes.size(); i++) {
                            FilmeDTO filme = response.filmes.get(i);
                            opcoes[i] = "ID " + filme.id + " - " + filme.titulo;
                        }

                        String escolha = (String) JOptionPane.showInputDialog(
                                this,
                                "Selecione o filme a editar:",
                                "Editar Filme",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                opcoes,
                                opcoes[0]
                        );

                        if (escolha != null) {
                            String idStr = escolha.substring(3, escolha.indexOf(" -"));

                            FilmeDTO filmeSelecionado = null;
                            for (FilmeDTO f : response.filmes) {
                                if (f.id.equals(idStr)) {
                                    filmeSelecionado = f;
                                    break;
                                }
                            }

                            if (filmeSelecionado != null) {
                                mostrarFormularioEdicao(filmeSelecionado);
                            }
                        }
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Nenhum filme disponível para edição.",
                                "Aviso", JOptionPane.INFORMATION_MESSAGE);
                    });
                }

            } catch (IOException e) {
                addLog("ERRO ao buscar filmes: " + e.getMessage());
            }
        }).start();
    }

    private void handleExcluirFilme() {
        if (currentToken == null) return;

        String idStr = JOptionPane.showInputDialog(this, "Digite o ID do filme a excluir:");
        if (idStr == null || idStr.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja excluir o filme ID " + idStr + "?\n" +
                        "Esta ação também excluirá todas as avaliações deste filme!",
                "Confirmar Exclusão",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                ExcluirFilmeRequest request = new ExcluirFilmeRequest(idStr, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: EXCLUIR_FILME");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA EXCLUIR_FILME");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status.getMessage(),
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void mostrarCatalogoFilmes(java.util.List<FilmeDTO> filmes) {
        JDialog dialog = new JDialog(this, "Catálogo de Filmes", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        if (filmes.isEmpty()) {
            JLabel emptyLabel = new JLabel("Nenhum filme cadastrado ainda.", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.BOLD, 16));
            emptyLabel.setForeground(Color.GRAY);
            dialog.add(emptyLabel, BorderLayout.CENTER);
        } else {
            JPanel filmesPanel = new JPanel();
            filmesPanel.setLayout(new BoxLayout(filmesPanel, BoxLayout.Y_AXIS));
            filmesPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            for (FilmeDTO filme : filmes) {
                JPanel filmeCard = criarCardFilme(filme);
                filmesPanel.add(filmeCard);
                filmesPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }

            JScrollPane scrollPane = new JScrollPane(filmesPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            dialog.add(scrollPane, BorderLayout.CENTER);
        }

        JButton closeButton = new JButton("Fechar");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void enviarEditarFilme(String id, String titulo, String diretor, String ano,
                                   java.util.List<String> generos, String sinopse) {
        new Thread(() -> {
            try {
                EditarFilmeRequest.FilmeUpdate filmeUpdate = new EditarFilmeRequest.FilmeUpdate(
                        id, titulo, diretor, ano, generos, sinopse
                );
                EditarFilmeRequest request = new EditarFilmeRequest(filmeUpdate, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: EDITAR_FILME");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA EDITAR_FILME");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status.getMessage(),
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    // operacoes de reviews

    private void handleCriarReview() {
        if (currentToken == null) return;

        JDialog dialog = new JDialog(this, "Criar Review", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField idFilmeField = new JTextField();
        JSpinner notaSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 5, 1));
        JTextField tituloField = new JTextField();
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setLineWrap(true);

        formPanel.add(new JLabel("ID do Filme:"));
        formPanel.add(idFilmeField);
        formPanel.add(new JLabel("Nota (1-5):"));
        formPanel.add(notaSpinner);
        formPanel.add(new JLabel("Título:"));
        formPanel.add(tituloField);
        formPanel.add(new JLabel("Descrição (max 250):"));
        formPanel.add(new JScrollPane(descArea));

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton salvarButton = new JButton("Criar Review");
        JButton cancelarButton = new JButton("Cancelar");

        salvarButton.addActionListener(e -> {
            String idFilme = idFilmeField.getText().trim();
            String nota = notaSpinner.getValue().toString();
            String titulo = tituloField.getText().trim();
            String descricao = descArea.getText().trim();

            if (idFilme.isEmpty() || titulo.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "ID do filme e título são obrigatórios!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (descricao.length() > 250) {
                JOptionPane.showMessageDialog(dialog,
                        "Descrição não pode exceder 250 caracteres!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dialog.dispose();
            enviarCriarReview(idFilme, nota, titulo, descricao);
        });

        cancelarButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(salvarButton);
        buttonPanel.add(cancelarButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void mostrarFormularioCriarReview(String idFilme) {
        JDialog dialog = new JDialog(this, "✍️ Criar Review - Filme ID " + idFilme, true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel notaLabel = new JLabel("Nota (1-5):");
        JSpinner notaSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));

        JLabel tituloLabel = new JLabel("Título:");
        JTextField tituloField = new JTextField();

        JLabel descricaoLabel = new JLabel("Descrição (max 250):");
        JTextArea descricaoArea = new JTextArea(5, 20);
        descricaoArea.setLineWrap(true);
        descricaoArea.setWrapStyleWord(true);

        formPanel.add(notaLabel);
        formPanel.add(notaSpinner);
        formPanel.add(tituloLabel);
        formPanel.add(tituloField);
        formPanel.add(descricaoLabel);
        formPanel.add(new JScrollPane(descricaoArea));

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton salvarButton = new JButton("Salvar Review");
        JButton cancelarButton = new JButton("Cancelar");

        salvarButton.addActionListener(e -> {
            String titulo = tituloField.getText().trim();
            String descricao = descricaoArea.getText().trim();
            String nota = String.valueOf(notaSpinner.getValue());

            if (titulo.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "O título é obrigatório!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (descricao.length() > 250) {
                JOptionPane.showMessageDialog(dialog,
                        "A descrição não pode exceder 250 caracteres!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dialog.dispose();
            enviarCriarReview(idFilme, titulo, descricao, nota);
        });

        cancelarButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(salvarButton);
        buttonPanel.add(cancelarButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void enviarCriarReview(String idFilme, String nota, String titulo, String descricao) {
        new Thread(() -> {
            try {
                CriarReviewRequest.ReviewData reviewData = new CriarReviewRequest.ReviewData(
                        idFilme, titulo, descricao, nota
                );
                CriarReviewRequest request = new CriarReviewRequest(reviewData, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("ENVIANDO: CRIAR_REVIEW");
                out.println(jsonRequest);
                String jsonResponse = in.readLine();
                addLog("RECEBIDO: " + jsonResponse);

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status.getMessage(),
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }
    private void handleListarMinhasReviews() {
        if (currentToken == null) return;

        new Thread(() -> {
            try {
                ListarReviewsUsuarioRequest request = new ListarReviewsUsuarioRequest(currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("ENVIANDO: LISTAR_REVIEWS_USUARIO");
                out.println(jsonRequest);
                String jsonResponse = in.readLine();
                addLog("RECEBIDO: " + jsonResponse);

                ListarReviewsUsuarioResponse response = GSON.fromJson(jsonResponse, ListarReviewsUsuarioResponse.class);

                if ("200".equals(response.status)) {
                    SwingUtilities.invokeLater(() -> {
                        mostrarMinhasReviews(response.reviews);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, response.mensagem,
                                "Erro", JOptionPane.ERROR_MESSAGE);
                    });
                }
            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }
    private void mostrarMinhasReviews(List<ReviewDTO> reviews) {
        JDialog dialog = new JDialog(this, "Minhas Reviews", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);

        if (reviews.isEmpty()) {
            JLabel emptyLabel = new JLabel("Você ainda não criou nenhuma review.", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.BOLD, 16));
            dialog.add(emptyLabel, BorderLayout.CENTER);
        } else {
            JPanel reviewsPanel = new JPanel();
            reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));
            reviewsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            for (ReviewDTO review : reviews) {
                JPanel reviewCard = criarCardReview(review);
                reviewsPanel.add(reviewCard);
                reviewsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }

            JScrollPane scrollPane = new JScrollPane(reviewsPanel);
            dialog.add(scrollPane, BorderLayout.CENTER);
        }

        JButton closeButton = new JButton("Fechar");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void mostrarDialogoMinhasReviews(java.util.List<ReviewDTO> reviews) {
        JDialog dialog = new JDialog(this, "📝 Minhas Reviews", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        if (reviews.isEmpty()) {
            JLabel emptyLabel = new JLabel("Você ainda não criou nenhuma review.", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.BOLD, 16));
            emptyLabel.setForeground(Color.GRAY);
            dialog.add(emptyLabel, BorderLayout.CENTER);
        } else {
            JPanel reviewsPanel = new JPanel();
            reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));
            reviewsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            for (ReviewDTO review : reviews) {
                JPanel reviewCard = criarCardReview(review);
                reviewsPanel.add(reviewCard);
                reviewsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }

            JScrollPane scrollPane = new JScrollPane(reviewsPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            dialog.add(scrollPane, BorderLayout.CENTER);
        }

        JButton closeButton = new JButton("Fechar");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel criarCardReview(ReviewDTO review) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(10, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                new EmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        JLabel userLabel = new JLabel("👤 " + review.nomeUsuario);
        userLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel tituloLabel = new JLabel(review.titulo);
        tituloLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel descLabel = new JLabel("<html>" + review.descricao + "</html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 11));

        // ✅ CAMPOS NOVOS
        JLabel dataLabel = new JLabel("📅 " + review.data +
                ("true".equals(review.editado) ? " (editado)" : ""));
        dataLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        dataLabel.setForeground(Color.GRAY);

        infoPanel.add(userLabel);
        infoPanel.add(tituloLabel);
        infoPanel.add(descLabel);
        infoPanel.add(dataLabel);

        JLabel notaLabel = new JLabel("⭐ " + review.nota);
        notaLabel.setFont(new Font("Arial", Font.BOLD, 20));
        notaLabel.setForeground(new Color(255, 165, 0));

        card.add(infoPanel, BorderLayout.CENTER);
        card.add(notaLabel, BorderLayout.EAST);

        return card;
    }

    private void handleEditarReview() {
        if (currentToken == null) return;

        new Thread(() -> {
            try {
                // 1. Listar reviews do usuário
                ListarReviewsUsuarioRequest reqListar = new ListarReviewsUsuarioRequest(currentToken);
                out.println(GSON.toJson(reqListar));
                String jsonResponseListar = in.readLine();

                ListarReviewsUsuarioResponse resListar = GSON.fromJson(jsonResponseListar, ListarReviewsUsuarioResponse.class);

                if (!"200".equals(resListar.status) || resListar.reviews == null || resListar.reviews.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Você não tem reviews para editar.",
                                "Aviso", JOptionPane.INFORMATION_MESSAGE);
                    });
                    return;
                }

                // 2. Mostrar diálogo de seleção
                SwingUtilities.invokeLater(() -> {
                    String[] opcoes = new String[resListar.reviews.size()];
                    for (int i = 0; i < resListar.reviews.size(); i++) {
                        ReviewDTO r = resListar.reviews.get(i);
                        opcoes[i] = "ID " + r.id + " - " + r.titulo + " (Filme ID: " + r.idFilme + ")";
                    }

                    String escolha = (String) JOptionPane.showInputDialog(
                            this,
                            "Selecione a review para editar:",
                            "Editar Review",
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            opcoes,
                            opcoes[0]
                    );

                    if (escolha != null) {
                        String idReview = escolha.substring(3, escolha.indexOf(" -"));

                        // Encontrar a review selecionada
                        ReviewDTO reviewSelecionada = null;
                        for (ReviewDTO r : resListar.reviews) {
                            if (r.id.equals(idReview)) {
                                reviewSelecionada = r;
                                break;
                            }
                        }

                        if (reviewSelecionada != null) {
                            mostrarFormularioEditarReview(reviewSelecionada);
                        }
                    }
                });

            } catch (IOException e) {
                addLog("ERRO ao buscar reviews: " + e.getMessage());
            }
        }).start();
    }

    private void mostrarFormularioEditarReview(ReviewDTO reviewOriginal) {
        JDialog dialog = new JDialog(this, "✏️ Editar Review ID " + reviewOriginal.id, true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Info de data (read-only)
        JLabel dataInfoLabel = new JLabel("Data original:");
        JLabel dataValueLabel = new JLabel(reviewOriginal.data);
        dataValueLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        dataValueLabel.setForeground(Color.GRAY);

        JLabel notaLabel = new JLabel("Nota (1-5):");
        JSpinner notaSpinner = new JSpinner(new SpinnerNumberModel(
                Integer.parseInt(reviewOriginal.nota), 1, 5, 1));

        JLabel tituloLabel = new JLabel("Título:");
        JTextField tituloField = new JTextField(reviewOriginal.titulo);

        JLabel descricaoLabel = new JLabel("Descrição (max 250):");
        JTextArea descricaoArea = new JTextArea(reviewOriginal.descricao, 5, 20);
        descricaoArea.setLineWrap(true);
        descricaoArea.setWrapStyleWord(true);

        formPanel.add(dataInfoLabel);
        formPanel.add(dataValueLabel);
        formPanel.add(notaLabel);
        formPanel.add(notaSpinner);
        formPanel.add(tituloLabel);
        formPanel.add(tituloField);
        formPanel.add(descricaoLabel);
        formPanel.add(new JScrollPane(descricaoArea));

        dialog.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton salvarButton = new JButton("Salvar Alterações");
        JButton cancelarButton = new JButton("Cancelar");

        salvarButton.addActionListener(e -> {
            String titulo = tituloField.getText().trim();
            String descricao = descricaoArea.getText().trim();
            String nota = String.valueOf(notaSpinner.getValue());

            if (titulo.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "O título é obrigatório!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (descricao.length() > 250) {
                JOptionPane.showMessageDialog(dialog,
                        "A descrição não pode exceder 250 caracteres!",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dialog.dispose();
            enviarEditarReview(reviewOriginal.id, titulo, descricao, nota);
        });

        cancelarButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(salvarButton);
        buttonPanel.add(cancelarButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void enviarEditarReview(String idReview, String titulo, String descricao, String nota) {
        new Thread(() -> {
            try {
                EditarReviewRequest.ReviewUpdate reviewUpdate = new EditarReviewRequest.ReviewUpdate(
                        idReview, titulo, descricao, nota
                );
                EditarReviewRequest request = new EditarReviewRequest(reviewUpdate, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: EDITAR_REVIEW");
                addLog("│ " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);
                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA EDITAR_REVIEW");
                addLog("│ " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(response.status);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, status.getMessage(),
                            status.isSuccess() ? "Sucesso" : "Erro",
                            status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });

            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void handleExcluirReview() {
        if (currentToken == null) return;

        new Thread(() -> {
            try {
                // 1. Listar reviews do usuário
                ListarReviewsUsuarioRequest reqListar = new ListarReviewsUsuarioRequest(currentToken);
                out.println(GSON.toJson(reqListar));
                String jsonResponseListar = in.readLine();

                ListarReviewsUsuarioResponse resListar = GSON.fromJson(jsonResponseListar, ListarReviewsUsuarioResponse.class);

                if (!"200".equals(resListar.status) || resListar.reviews == null || resListar.reviews.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Você não tem reviews para excluir.",
                                "Aviso", JOptionPane.INFORMATION_MESSAGE);
                    });
                    return;
                }

                // 2. Mostrar diálogo de seleção
                SwingUtilities.invokeLater(() -> {
                    String[] opcoes = new String[resListar.reviews.size()];
                    for (int i = 0; i < resListar.reviews.size(); i++) {
                        ReviewDTO r = resListar.reviews.get(i);
                        opcoes[i] = "ID " + r.id + " - " + r.titulo + " (Filme ID: " + r.idFilme + ")";
                    }

                    String escolha = (String) JOptionPane.showInputDialog(
                            this,
                            "Selecione a review para excluir:",
                            "Excluir Review",
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            opcoes,
                            opcoes[0]
                    );

                    if (escolha != null) {
                        String idReview = escolha.substring(3, escolha.indexOf(" -"));

                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Tem certeza que deseja excluir esta review?\n" +
                                        "Esta ação é irreversível!",
                                "Confirmar Exclusão",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);

                        if (confirm == JOptionPane.YES_OPTION) {
                            new Thread(() -> enviarExcluirReview(idReview)).start();
                        }
                    }
                });

            } catch (IOException e) {
                addLog("ERRO ao buscar reviews: " + e.getMessage());
            }
        }).start();
    }

    private void enviarExcluirReview(String idReview) {
        try {
            ExcluirReviewRequest request = new ExcluirReviewRequest(idReview, currentToken);
            String jsonRequest = GSON.toJson(request);

            addLog("\n┌" + "─".repeat(48) + "┐");
            addLog("│ ENVIANDO: EXCLUIR_REVIEW");
            addLog("│ " + jsonRequest);
            addLog("└" + "─".repeat(48) + "┘");

            out.println(jsonRequest);
            String jsonResponse = in.readLine();

            addLog("\n┌" + "─".repeat(48) + "┐");
            addLog("│ RECEBIDO: RESPOSTA EXCLUIR_REVIEW");
            addLog("│ " + jsonResponse);
            addLog("└" + "─".repeat(48) + "┘");

            ResponsePadrao response = GSON.fromJson(jsonResponse, ResponsePadrao.class);
            HttpStatus status = HttpStatus.fromCode(response.status);

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, status.getMessage(),
                        status.isSuccess() ? "Sucesso" : "Erro",
                        status.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            });

        } catch (IOException e) {
            addLog("ERRO: " + e.getMessage());
        }
    }

    private void handleBuscarFilmeComReviews() {
        if (currentToken == null) return;

        String idFilme = JOptionPane.showInputDialog(this, "Digite o ID do filme:");
        if (idFilme == null || idFilme.isEmpty()) return;

        new Thread(() -> {
            try {
                BuscarFilmeIdRequest request = new BuscarFilmeIdRequest(idFilme, currentToken);
                String jsonRequest = GSON.toJson(request);

                addLog("ENVIANDO: BUSCAR_FILME_ID");
                out.println(jsonRequest);
                String jsonResponse = in.readLine();
                addLog("RECEBIDO: " + jsonResponse);

                BuscarFilmeIdResponse response = GSON.fromJson(jsonResponse, BuscarFilmeIdResponse.class);

                if ("200".equals(response.status)) {
                    SwingUtilities.invokeLater(() -> {
                        mostrarFilmeDetalhado(response.filme, response.reviews);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, response.mensagem, "Erro", JOptionPane.ERROR_MESSAGE);
                    });
                }
            } catch (IOException e) {
                addLog("ERRO: " + e.getMessage());
            }
        }).start();
    }

    private void mostrarFilmeDetalhado(FilmeDTO filme, List<ReviewDTO> reviews) {
        JDialog dialog = new JDialog(this, "Detalhes do Filme", true);
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Card do filme (já existe: criarCardFilme)
        mainPanel.add(criarCardFilme(filme), BorderLayout.NORTH);

        // Painel de reviews
        JPanel reviewsPanel = new JPanel();
        reviewsPanel.setLayout(new BoxLayout(reviewsPanel, BoxLayout.Y_AXIS));

        JLabel reviewsTitle = new JLabel("═══ REVIEWS (" + reviews.size() + ") ═══");
        reviewsTitle.setFont(new Font("Arial", Font.BOLD, 16));
        reviewsTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        reviewsPanel.add(reviewsTitle);
        reviewsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        for (ReviewDTO review : reviews) {
            JPanel reviewCard = criarCardReview(review);
            reviewsPanel.add(reviewCard);
            reviewsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scrollPane = new JScrollPane(reviewsPanel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Fechar");
        closeButton.addActionListener(e -> dialog.dispose());
        mainPanel.add(closeButton, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ClientGUI().setVisible(true);
        });
    }
}