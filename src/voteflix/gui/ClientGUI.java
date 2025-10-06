package voteflix.gui;

import voteflix.service.ClientService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientGUI extends JFrame {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
    private BufferedReader stdIn;
    private ClientService clientService;
    private boolean connected = false;

    public ClientGUI() {
        setTitle("VoteFlix - Cliente");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            stdIn = new BufferedReader(new InputStreamReader(System.in));
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        portField = new JTextField("5000", 8);
        panel.add(portField);

        connectButton = new JButton("🔌 Conectar");
        connectButton.setBackground(new Color(34, 139, 34));
        connectButton.setForeground(Color.WHITE);
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

        loginButton = new JButton("🔓 Login");
        loginButton.setEnabled(false);
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.addActionListener(e -> handleLogin());
        buttonPanel.add(loginButton);

        registerButton = new JButton("📝 Registrar");
        registerButton.setEnabled(false);
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.WHITE);
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

        JButton clearButton = new JButton("🗑 Limpar Logs");
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

            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this, "Porta inválida! Use valores entre 1024 e 65535.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            addLog("=".repeat(50));
            addLog("INICIANDO CONEXÃO");
            addLog("Servidor: " + ip + ":" + port);

            socket = new Socket(ip, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Cria ClientService com um BufferedReader simulado para GUI
            clientService = new ClientService(out, in, stdIn);

            connected = true;
            ipField.setEnabled(false);
            portField.setEnabled(false);
            connectButton.setText("🔌 Desconectar");
            connectButton.setBackground(new Color(178, 34, 34));
            statusLabel.setText("● Status: Conectado");
            statusLabel.setForeground(new Color(34, 139, 34));

            loginButton.setEnabled(true);
            registerButton.setEnabled(true);

            addLog("✓ CONECTADO COM SUCESSO!");
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
            connectButton.setText("🔌 Conectar");
            connectButton.setBackground(new Color(34, 139, 34));
            statusLabel.setText("● Status: Desconectado");
            statusLabel.setForeground(Color.RED);

            loginButton.setEnabled(false);
            registerButton.setEnabled(false);

            clearOperationsPanel();
            userInfoLabel.setText("");

            addLog("=".repeat(50));
            addLog("DESCONECTADO");
            addLog("=".repeat(50));

        } catch (IOException e) {
            addLog("ERRO ao desconectar: " + e.getMessage());
        }
    }

    private void handleLogin() {
        String usuario = userField.getText().trim();
        String senha = new String(passField.getPassword());

        if (usuario.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha usuário e senha!", "Erro", JOptionPane.WARNING_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: LOGIN");
                String jsonRequest = String.format("{\"operacao\":\"LOGIN\",\"usuario\":\"%s\",\"senha\":\"%s\"}", usuario, senha);
                addLog("│ JSON: " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);

                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA LOGIN");
                addLog("│ JSON: " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                // Processar resposta (simplificado - idealmente usar ClientService)
                if (jsonResponse.contains("\"status\":\"200\"")) {
                    SwingUtilities.invokeLater(() -> {
                        userInfoLabel.setText("👤 Usuário: " + usuario);
                        loadUserOperations(usuario.equals("admin"));
                        JOptionPane.showMessageDialog(this, "Login realizado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Falha no login! Credenciais inválidas.", "Erro", JOptionPane.ERROR_MESSAGE);
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
                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ ENVIANDO: CRIAR_USUARIO");
                String jsonRequest = String.format("{\"operacao\":\"CRIAR_USUARIO\",\"usuario\":\"%s\",\"senha\":\"%s\"}", usuario, senha);
                addLog("│ JSON: " + jsonRequest);
                addLog("└" + "─".repeat(48) + "┘");

                out.println(jsonRequest);

                String jsonResponse = in.readLine();

                addLog("\n┌" + "─".repeat(48) + "┐");
                addLog("│ RECEBIDO: RESPOSTA CRIAR_USUARIO");
                addLog("│ JSON: " + jsonResponse);
                addLog("└" + "─".repeat(48) + "┘");

                if (jsonResponse.contains("\"status\":\"201\"")) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Usuário cadastrado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        passField.setText("");
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Falha no cadastro! Usuário pode já existir.", "Erro", JOptionPane.ERROR_MESSAGE);
                    });
                }

            } catch (IOException e) {
                addLog("ERRO ao cadastrar: " + e.getMessage());
            }
        }).start();
    }

    private void loadUserOperations(boolean isAdmin) {
        operationsPanel.removeAll();

        if (isAdmin) {
            addOperationButton("📋 Listar Todos os Usuários", this::handleListUsers);
            addOperationButton("✏️ Editar Usuário (por ID)", this::handleEditUser);
            addOperationButton("🗑️ Excluir Usuário (por ID)", this::handleDeleteUser);
        } else {
            addOperationButton("👤 Listar Meus Dados", this::handleListMyself);
            addOperationButton("🔑 Atualizar Senha", this::handleUpdatePassword);
            addOperationButton("❌ Excluir Conta", this::handleDeleteAccount);
        }

        addOperationButton("🚪 Logout", this::handleLogout);

        operationsPanel.revalidate();
        operationsPanel.repaint();
    }

    private void addOperationButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFont(new Font("Arial", Font.PLAIN, 12));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
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

    private void handleListUsers() {
        // Implementação simplificada - usar token real em produção
        addLog("\n[OPERAÇÃO] Listar Todos os Usuários");
        JOptionPane.showMessageDialog(this, "Funcionalidade: Listar Usuários\nImplementar com token JWT", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleEditUser() {
        String id = JOptionPane.showInputDialog(this, "Digite o ID do usuário:");
        if (id != null && !id.isEmpty()) {
            String novaSenha = JOptionPane.showInputDialog(this, "Digite a nova senha:");
            if (novaSenha != null && !novaSenha.isEmpty()) {
                addLog("\n[OPERAÇÃO] Editar Usuário ID: " + id);
                JOptionPane.showMessageDialog(this, "Funcionalidade: Editar Usuário\nImplementar com token JWT", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void handleDeleteUser() {
        String id = JOptionPane.showInputDialog(this, "Digite o ID do usuário a excluir:");
        if (id != null && !id.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Tem certeza que deseja excluir o usuário ID " + id + "?",
                    "Confirmar Exclusão",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                addLog("\n[OPERAÇÃO] Excluir Usuário ID: " + id);
                JOptionPane.showMessageDialog(this, "Funcionalidade: Excluir Usuário\nImplementar com token JWT", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void handleListMyself() {
        addLog("\n[OPERAÇÃO] Listar Meus Dados");
        JOptionPane.showMessageDialog(this, "Funcionalidade: Listar Próprio Usuário\nImplementar com token JWT", "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleUpdatePassword() {
        String novaSenha = JOptionPane.showInputDialog(this, "Digite a nova senha (3-20 caracteres):");
        if (novaSenha != null && !novaSenha.isEmpty()) {
            if (novaSenha.length() >= 3 && novaSenha.length() <= 20) {
                addLog("\n[OPERAÇÃO] Atualizar Senha");
                JOptionPane.showMessageDialog(this, "Funcionalidade: Atualizar Senha\nImplementar com token JWT", "Info", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Senha deve ter entre 3 e 20 caracteres!", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteAccount() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠️ ATENÇÃO: Esta ação é IRREVERSÍVEL!\n\nTem certeza que deseja excluir sua conta?",
                "Confirmar Exclusão de Conta",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            addLog("\n[OPERAÇÃO] Excluir Própria Conta");
            JOptionPane.showMessageDialog(this, "Funcionalidade: Excluir Conta\nImplementar com token JWT", "Info", JOptionPane.INFORMATION_MESSAGE);
            // Após exclusão bem-sucedida, desconectar
            disconnect();
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Deseja fazer logout?",
                "Confirmar Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            addLog("\n[OPERAÇÃO] Logout");
            clearOperationsPanel();
            userInfoLabel.setText("");
            userField.setText("");
            passField.setText("");
            JOptionPane.showMessageDialog(this, "Logout realizado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
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