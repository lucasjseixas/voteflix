package voteflix.gui;

import voteflix.network.ServerConnection;
import voteflix.session.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerGUI extends JFrame {
    private JTextField portField;
    private JButton startButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel connectedUsersLabel;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private JTable sessionsTable;
    private DefaultTableModel tableModel;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ServerGUI() {
        setTitle("VoteFlix - Servidor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        // Painel Superior - Configuração e Controle
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Painel de configuração
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                "Configuração do Servidor",
                0, 0, new Font("Arial", Font.BOLD, 12), new Color(70, 130, 180)
        ));

        configPanel.add(new JLabel("Porta:"));
        portField = new JTextField("5000", 8);
        portField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        configPanel.add(portField);

        startButton = new JButton("▶ Iniciar Servidor");
        startButton.setBackground(new Color(34, 139, 34));
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Arial", Font.BOLD, 12));
        startButton.setFocusPainted(false);
        startButton.addActionListener(e -> toggleServer());
        configPanel.add(startButton);

        topPanel.add(configPanel, BorderLayout.WEST);

        // Painel de status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5));
        statusLabel = new JLabel("● Status: Parado");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.RED);

        connectedUsersLabel = new JLabel("Usuários Conectados: 0");
        connectedUsersLabel.setFont(new Font("Arial", Font.BOLD, 12));
        connectedUsersLabel.setForeground(new Color(70, 130, 180));

        statusPanel.add(connectedUsersLabel);
        statusPanel.add(statusLabel);
        topPanel.add(statusPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Painel Central - Split entre Logs e Sessões
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);

        // Painel de Logs (esquerda)
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                "Log de Comunicação do Servidor",
                0, 0, new Font("Arial", Font.BOLD, 12)
        ));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(200, 200, 200));
        logArea.setLineWrap(false);

        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Botão para limpar logs
        JButton clearLogsButton = new JButton("🗑 Limpar Logs");
        clearLogsButton.addActionListener(e -> logArea.setText(""));
        JPanel logButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logButtonPanel.add(clearLogsButton);

        logPanel.add(logScrollPane, BorderLayout.CENTER);
        logPanel.add(logButtonPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(logPanel);

        // Painel de Sessões Ativas (direita)
        JPanel sessionsPanel = new JPanel(new BorderLayout());
        sessionsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                "Usuários Conectados",
                0, 0, new Font("Arial", Font.BOLD, 12)
        ));

        tableModel = new DefaultTableModel(new String[]{"Usuário", "Hora de Login"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        sessionsTable = new JTable(tableModel);
        sessionsTable.setFont(new Font("Arial", Font.PLAIN, 12));
        sessionsTable.setRowHeight(25);
        sessionsTable.getTableHeader().setReorderingAllowed(false);
        sessionsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        sessionsTable.getTableHeader().setBackground(new Color(70, 130, 180));
        sessionsTable.getTableHeader().setForeground(Color.WHITE);

        JScrollPane tableScrollPane = new JScrollPane(sessionsTable);
        sessionsPanel.add(tableScrollPane, BorderLayout.CENTER);

        splitPane.setRightComponent(sessionsPanel);

        add(splitPane, BorderLayout.CENTER);

        // Registrar listener do SessionManager para atualizar a GUI
        SessionManager.getInstance().addListener(this::updateSessionsTable);
    }

    private void toggleServer() {
        if (!running) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText().trim());

            if (port < 1024 || port > 65535) {
                JOptionPane.showMessageDialog(this,
                        "Porta inválida! Use valores entre 1024 e 65535.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            serverSocket = new ServerSocket(port);
            running = true;

            portField.setEnabled(false);
            startButton.setText("■ Parar Servidor");
            startButton.setBackground(new Color(178, 34, 34));
            statusLabel.setText("● Status: Rodando na porta " + port);
            statusLabel.setForeground(new Color(34, 139, 34));

            addLog("=".repeat(60));
            addLog("SERVIDOR INICIADO");
            addLog("Porta: " + port);
            addLog("Timestamp: " + LocalDateTime.now().format(DATETIME_FORMATTER));
            addLog("Aguardando conexões...");
            addLog("=".repeat(60));

            // Registra callback para receber logs do ServerConnection
            ServerConnection.setLogCallback(this::addLog);

            new Thread(this::acceptConnections).start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Porta inválida! Digite apenas números.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao iniciar servidor: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            addLog("ERRO: Falha ao iniciar servidor - " + e.getMessage());
        }
    }

    private void stopServer() {
        try {
            running = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            portField.setEnabled(true);
            startButton.setText("▶ Iniciar Servidor");
            startButton.setBackground(new Color(34, 139, 34));
            statusLabel.setText("● Status: Parado");
            statusLabel.setForeground(Color.RED);

            addLog("=".repeat(60));
            addLog("SERVIDOR PARADO");
            addLog("Timestamp: " + LocalDateTime.now().format(DATETIME_FORMATTER));
            addLog("=".repeat(60));

        } catch (IOException e) {
            addLog("ERRO ao parar servidor: " + e.getMessage());
        }
    }

    private void acceptConnections() {
        while (running) {
            try {
                var clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();

                addLog("");
                addLog("┌" + "─".repeat(58) + "┐");
                addLog("│ NOVA CONEXÃO ACEITA");
                addLog("│ IP: " + clientIP);
                addLog("│ Porta: " + clientSocket.getPort());
                addLog("│ Timestamp: " + LocalDateTime.now().format(TIME_FORMATTER));
                addLog("└" + "─".repeat(58) + "┘");

                new ServerConnection(clientSocket);

            } catch (IOException e) {
                if (running) {
                    addLog("ERRO ao aceitar conexão: " + e.getMessage());
                }
            }
        }
    }

    public void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void updateSessionsTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            var sessions = SessionManager.getInstance().getActiveSessions();

            for (var entry : sessions.entrySet()) {
                String username = entry.getKey();
                LocalDateTime loginTime = entry.getValue();

                tableModel.addRow(new Object[]{
                        username,
                        loginTime.format(TIME_FORMATTER)
                });
            }

            int count = sessions.size();
            connectedUsersLabel.setText("Usuários Conectados: " + count);

            if (running) {
                statusLabel.setText("● Status: Rodando | " + count + " conexão(ões) ativa(s)");
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ServerGUI().setVisible(true);
        });
    }
}