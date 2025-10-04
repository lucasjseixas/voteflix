//package testevoteflix.gui;
//
//import javax.swing.*;
//import java.awt.*;
//import java.io.*;
//import java.net.Socket;
//
//public class ClientGUI extends JFrame {
//    private JTextField ipField;
//    private JTextField portField;
//    private JButton connectButton;
//    private JTextArea logArea;
//    private JLabel statusLabel;
//
//    private Socket socket;
//    private PrintWriter out;
//    private BufferedReader in;
//    private boolean connected = false;
//
//    public ClientGUI() {
//        setTitle("VoteFlix - Cliente");
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
//        // Painel superior - Configuração de Conexão
//        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
//        configPanel.setBorder(BorderFactory.createTitledBorder("Conexão"));
//
//        configPanel.add(new JLabel("IP do Servidor:"));
//        ipField = new JTextField("127.0.0.1", 12);
//        configPanel.add(ipField);
//
//        configPanel.add(new JLabel("Porta:"));
//        portField = new JTextField("5000", 8);
//        configPanel.add(portField);
//
//        connectButton = new JButton("Conectar");
//        connectButton.addActionListener(e -> toggleConnection());
//        configPanel.add(connectButton);
//
//        add(configPanel, BorderLayout.NORTH);
//
//        // Painel central - Log
//        JPanel logPanel = new JPanel(new BorderLayout());
//        logPanel.setBorder(BorderFactory.createTitledBorder("Log de Conexão"));
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
//        statusLabel = new JLabel("Status: Desconectado");
//        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
//        statusLabel.setForeground(Color.RED);
//        statusPanel.add(statusLabel);
//
//        add(statusPanel, BorderLayout.SOUTH);
//    }
//
//    private void toggleConnection() {
//        if (!connected) {
//            connect();
//        } else {
//            disconnect();
//        }
//    }
//
//    private void connect() {
//        String ip = ipField.getText().trim();
//        String portStr = portField.getText().trim();
//
//        if (ip.isEmpty()) {
//            JOptionPane.showMessageDialog(this,
//                    "Digite o IP do servidor!",
//                    "Erro",
//                    JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        try {
//            int port = Integer.parseInt(portStr);
//
//            if (port < 1024 || port > 65535) {
//                JOptionPane.showMessageDialog(this,
//                        "Porta inválida! Use valores entre 1024 e 65535.",
//                        "Erro",
//                        JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            addLog("Tentando conectar em " + ip + ":" + port + "...");
//
//            socket = new Socket(ip, port);
//            out = new PrintWriter(socket.getOutputStream(), true);
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//
//            connected = true;
//            ipField.setEnabled(false);
//            portField.setEnabled(false);
//            connectButton.setText("Desconectar");
//            statusLabel.setText("Status: Conectado");
//            statusLabel.setForeground(new Color(0, 150, 0));
//
//            addLog("Conectado com sucesso!");
//            addLog("Você pode fechar esta janela e usar o console para as operações.");
//
//            // Aqui você pode:
//            // 1. Fechar a GUI e continuar no console
//            // 2. Abrir uma nova janela com o menu de operações
//            // 3. Transformar esta janela no menu principal
//
//        } catch (NumberFormatException e) {
//            JOptionPane.showMessageDialog(this,
//                    "Porta inválida! Digite apenas números.",
//                    "Erro",
//                    JOptionPane.ERROR_MESSAGE);
//        } catch (IOException e) {
//            JOptionPane.showMessageDialog(this,
//                    "Erro ao conectar: " + e.getMessage(),
//                    "Erro de Conexão",
//                    JOptionPane.ERROR_MESSAGE);
//            addLog("Falha na conexão: " + e.getMessage());
//        }
//    }
//
//    private void disconnect() {
//        try {
//            if (out != null) out.close();
//            if (in != null) in.close();
//            if (socket != null) socket.close();
//
//            connected = false;
//            ipField.setEnabled(true);
//            portField.setEnabled(true);
//            connectButton.setText("Conectar");
//            statusLabel.setText("Status: Desconectado");
//            statusLabel.setForeground(Color.RED);
//
//            addLog("Desconectado.");
//
//        } catch (IOException e) {
//            addLog("Erro ao desconectar: " + e.getMessage());
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
//    // Getters para integração com o código existente
//    public Socket getSocket() { return socket; }
//    public PrintWriter getOut() { return out; }
//    public BufferedReader getIn() { return in; }
//    public boolean isConnected() { return connected; }
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            new ClientGUI().setVisible(true);
//        });
//    }
//}