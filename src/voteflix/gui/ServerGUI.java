//package testevoteflix.gui;
//
//import testevoteflix.network.ServerConnection;
//
//import javax.swing.*;
//import java.awt.*;
//import java.io.IOException;
//import java.net.ServerSocket;
//import testevoteflix.session.SessionManager;
//import javax.swing.table.DefaultTableModel;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//public class ServerGUI extends JFrame {
//    private JTextField portField;
//    private JButton startButton;
//    private JTextArea logArea;
//    private JLabel statusLabel;
//    private ServerSocket serverSocket;
//    private volatile boolean running = false;
//    private JTable sessionsTable;
//    private DefaultTableModel tableModel;
//    private static final DateTimeFormatter TIME_FORMATTER =
//            DateTimeFormatter.ofPattern("HH:mm:ss");
//
//    public ServerGUI() {
//        setTitle("VoteFlix - Servidor");
//        setSize(600, 400);
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setLocationRelativeTo(null);
//
//        initComponents();
//    }
//
//    private void initComponents() {
//        setLayout(new BorderLayout(10, 10));
//
//        // Painel superior - Configuração
//        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
//        configPanel.setBorder(BorderFactory.createTitledBorder("Configuração"));
//
//        configPanel.add(new JLabel("Porta:"));
//        portField = new JTextField("5000", 8);
//        configPanel.add(portField);
//
//        startButton = new JButton("Iniciar Servidor");
//        startButton.addActionListener(e -> toggleServer());
//        configPanel.add(startButton);
//
//        add(configPanel, BorderLayout.NORTH);
//
//        // Painel central - Log
//        JPanel logPanel = new JPanel(new BorderLayout());
//        logPanel.setBorder(BorderFactory.createTitledBorder("Log do Servidor"));
//
//        logArea = new JTextArea();
//        logArea.setEditable(false);
//        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
//        JScrollPane scrollPane = new JScrollPane(logArea);
//        logPanel.add(scrollPane, BorderLayout.CENTER);
//
//        add(logPanel, BorderLayout.CENTER);
//
//        // Painel inferior - Status
//        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//        statusLabel = new JLabel("Status: Parado");
//        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
//        statusPanel.add(statusLabel);
//
//        add(statusPanel, BorderLayout.SOUTH);
//
//        // Painel de sessões ativas
//        JPanel sessionsPanel = new JPanel(new BorderLayout());
//        sessionsPanel.setBorder(BorderFactory.createTitledBorder("Usuários Conectados"));
//
//        tableModel = new DefaultTableModel(
//                new String[]{"Usuário", "Hora de Login"}, 0) {
//            @Override
//            public boolean isCellEditable(int row, int column) {
//                return false; // Tabela não editável
//            }
//        };
//        sessionsTable = new JTable(tableModel);
//        sessionsTable.getTableHeader().setReorderingAllowed(false);
//        JScrollPane tableScrollPane = new JScrollPane(sessionsTable);
//        sessionsPanel.add(tableScrollPane, BorderLayout.CENTER);
//
//        add(sessionsPanel, BorderLayout.EAST);
//
//        // Registrar listener do SessionManager
//        SessionManager.getInstance().addListener(() -> updateSessionsTable());
//
//    }
//
//    private void toggleServer() {
//        if (!running) {
//            startServer();
//        } else {
//            stopServer();
//        }
//    }
//
//    private void startServer() {
//        try {
//            int port = Integer.parseInt(portField.getText().trim());
//
//            if (port < 1024 || port > 65535) {
//                JOptionPane.showMessageDialog(this,
//                        "Porta inválida! Use valores entre 1024 e 65535.",
//                        "Erro",
//                        JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            serverSocket = new ServerSocket(port);
//            running = true;
//
//            portField.setEnabled(false);
//            startButton.setText("Parar Servidor");
//            statusLabel.setText("Status: Rodando na porta " + port);
//
//            addLog("Servidor iniciado na porta " + port);
//            addLog("Aguardando conexões...");
//
//            // Inicia thread para aceitar conexões
//            new Thread(() -> acceptConnections()).start();
//
//        } catch (NumberFormatException e) {
//            JOptionPane.showMessageDialog(this,
//                    "Porta inválida! Digite apenas números.",
//                    "Erro",
//                    JOptionPane.ERROR_MESSAGE);
//        } catch (IOException e) {
//            JOptionPane.showMessageDialog(this,
//                    "Erro ao iniciar servidor: " + e.getMessage(),
//                    "Erro",
//                    JOptionPane.ERROR_MESSAGE);
//        }
//    }
//
//    private void stopServer() {
//        try {
//            running = false;
//            if (serverSocket != null && !serverSocket.isClosed()) {
//                serverSocket.close();
//            }
//
//            portField.setEnabled(true);
//            startButton.setText("Iniciar Servidor");
//            statusLabel.setText("Status: Parado");
//
//            addLog("Servidor parado.");
//
//        } catch (IOException e) {
//            addLog("Erro ao parar servidor: " + e.getMessage());
//        }
//    }
//
//    private void acceptConnections() {
//        while (running) {
//            try {
//                var clientSocket = serverSocket.accept();
//                addLog("Nova conexão aceita de: " + clientSocket.getInetAddress().getHostAddress());
//
//                // Tornei public a chamada pro server, vamos ver.
//                new ServerConnection(clientSocket);
//
//            } catch (IOException e) {
//                if (running) {
//                    addLog("Erro ao aceitar conexão: " + e.getMessage());
//                }
//            }
//        }
//    }
//
//    private void addLog(String message) {
//        SwingUtilities.invokeLater(() -> {
//            logArea.append("[" + java.time.LocalTime.now().format(
//                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
//            logArea.setCaretPosition(logArea.getDocument().getLength());
//        });
//    }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            new ServerGUI().setVisible(true);
//        });
//    }
//
//    // Adicionar método para atualizar a tabela:
//    private void updateSessionsTable() {
//        SwingUtilities.invokeLater(() -> {
//            tableModel.setRowCount(0);
//            var sessions = SessionManager.getInstance().getActiveSessions();
//
//            for (var entry : sessions.entrySet()) {
//                String username = entry.getKey();
//                LocalDateTime loginTime = entry.getValue();
//
//                tableModel.addRow(new Object[]{
//                        username,
//                        loginTime.format(TIME_FORMATTER)
//                });
//            }
//
//            // Atualiza o status
//            int count = sessions.size();
//            if (running) {
//                statusLabel.setText("Status: Rodando | Usuários conectados: " + count);
//            }
//        });
//    }
//}