package voteflix.network;

import java.net.*;
import java.io.*;
import com.google.gson.Gson;
import voteflix.service.ServerService;
import voteflix.dto.request.RequestBase;
import voteflix.dto.response.ResponsePadrao;
import voteflix.util.JsonValidator;

public class ServerConnection extends Thread {

    protected Socket clientSocket;
    private static final Gson GSON = new Gson();
    private static final ServerService SERVICE = new ServerService();

    // Interface funcional para callback de logging
    public interface LogCallback {
        void log(String message);
    }

    private static LogCallback logCallback = null;

    // Metodo estático para registrar callback da GUI
    public static void setLogCallback(LogCallback callback) {
        logCallback = callback;
    }

    // Metodo auxiliar para logar tanto no console quanto na GUI
    private void log(String message) {
        System.out.println(message);
        if (logCallback != null) {
            logCallback.log(message);
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;

        System.out.println("Qual porta o servidor deve usar? ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int porta = Integer.parseInt(br.readLine());

        System.out.println("Servidor carregado na porta " + porta);
        System.out.println("Aguardando conexao....\n ");

        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Criado Socket de Conexao.\n");
            try {
                while (true) {
                    new ServerConnection(serverSocket.accept());
                    System.out.println("Accept ativado. Esperando por uma conexao...\n");
                }
            } catch (IOException e) {
                System.err.println("Accept falhou!");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Nao foi possivel ouvir a porta " + porta);
            System.exit(1);
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Nao foi possivel fechar a porta " + porta);
                System.exit(1);
            }
        }
    }

    // Constructor
    public ServerConnection(Socket clientSoc) {
        clientSocket = clientSoc;
        start();
    }

    @Override
    public void run() {
        String clientIP = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getPort();

        log("Nova thread de comunicacao iniciada para " + clientIP + ":" + clientPort);

        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null) {

                // --- 1. RECEBIMENTO E LOG DO JSON RECEBIDO ---
                log("");
                log("╔" + "═".repeat(70) + "╗");
                log("║ JSON RECEBIDO DE " + String.format("%-46s", clientIP + ":" + clientPort) + "║");
                log("╠" + "═".repeat(70) + "╣");

                // Quebra o JSON em linhas para melhor visualização
                String[] jsonLines = inputLine.split("(?<=\\{)|(?=\\})|(?<=,)");
                for (String line : jsonLines) {
                    if (!line.trim().isEmpty()) {
                        log("║ " + String.format("%-68s", line.trim()) + "║");
                    }
                }
                log("╚" + "═".repeat(70) + "╝");

                String finalResponse = SERVICE.createStatusResponse("500");
                String statusFinal = "500";
                RequestBase reqBase = null;

                // *** VALIDAÇÃO DO JSON ***
                if (!JsonValidator.validateComplete(inputLine)) {
                    log("JSON INVÁLIDO - Violação do padrão de nomenclatura");
                    finalResponse = SERVICE.createStatusResponse("400");

                    // LOG DO JSON ENVIADO (ERRO)
                    logJsonEnviado(finalResponse, clientIP, clientPort);
                    out.println(finalResponse);
                    continue;
                }

                try {
                    // --- 2. DESSERIALIZAÇÃO E IDENTIFICAÇÃO DA OPERAÇÃO ---
                    reqBase = GSON.fromJson(inputLine, RequestBase.class);
                    String operacao = reqBase.getOperacao();

                    log("Operacao Identificada: " + operacao);

                    // 3. ROTEAMENTO PARA O HANDLER DE SERVIÇO
                    switch (operacao) {
                        case "LOGIN":
                            finalResponse = SERVICE.handleLogin(inputLine);
                            break;

                        case "CRIAR_USUARIO":
                            finalResponse = SERVICE.handleCadastrarUsuario(inputLine);
                            break;

                        case "LOGOUT":
                            finalResponse = SERVICE.handleLogout(inputLine);
                            break;

                        case "LISTAR_PROPRIO_USUARIO":
                            finalResponse = SERVICE.handleListarProprioUsuario(inputLine);
                            break;

                        case "EDITAR_PROPRIO_USUARIO":
                            finalResponse = SERVICE.handleEditarProprioUsuario(inputLine);
                            break;

                        case "EXCLUIR_PROPRIO_USUARIO":
                            finalResponse = SERVICE.handleExcluirProprioUsuario(inputLine);
                            break;

                        case "LISTAR_USUARIOS":
                            finalResponse = SERVICE.handleListarUsuarios(inputLine);
                            break;

                        case "ADMIN_EDITAR_USUARIO":
                            finalResponse = SERVICE.handleAdminEditarUsuario(inputLine);
                            break;

                        case "ADMIN_EXCLUIR_USUARIO":
                            finalResponse = SERVICE.handleAdminExcluirUsuario(inputLine);
                            break;

                        default:
                            finalResponse = SERVICE.createStatusResponse("400");
                            break;
                    }

                    if (finalResponse == null) {
                        log("ALERTA: Handler falhou em definir finalResponse. Retornando 500.");
                        finalResponse = SERVICE.createStatusResponse("500");
                    }

                    // Extrai o status da resposta
                    ResponsePadrao res = GSON.fromJson(finalResponse, ResponsePadrao.class);
                    statusFinal = res.getStatus();

                } catch (Exception e) {
                    statusFinal = "400";
                    log("ERRO de Parsing JSON (Status 400): " + e.getMessage());
                }

                // --- 4. LOG DO JSON ENVIADO ---
                logJsonEnviado(finalResponse, clientIP, clientPort);

                // --- 5. ENVIO DA RESPOSTA ---
                out.println(finalResponse);

                // Condição para fechar a thread
                boolean isLogoutSucesso = reqBase != null &&
                        "LOGOUT".equals(reqBase.getOperacao()) &&
                        "200".equals(statusFinal);

                boolean isExcluirSucesso = reqBase != null &&
                        "EXCLUIR_PROPRIO_USUARIO".equals(reqBase.getOperacao()) &&
                        "200".equals(statusFinal);

                if (isLogoutSucesso || isExcluirSucesso) {
                    if (isLogoutSucesso) {
                        log("🚪 LOGOUT BEM-SUCEDIDO - Encerrando conexão com " + clientIP);
                    }
                    if (isExcluirSucesso) {
                        log("🗑️ EXCLUSÃO DE CONTA BEM-SUCEDIDA - Encerrando conexão com " + clientIP);
                    }
                    break;
                }
            }

            out.close();
            in.close();
            clientSocket.close();
            log("Thread de comunicacao encerrada para " + clientIP + ":" + clientPort);

        } catch (IOException e) {
            log("Problema com Servidor de Comunicacao: " + e.getMessage());
        }
    }

    // Metodo auxiliar para logar JSON enviado
    private void logJsonEnviado(String json, String clientIP, int clientPort) {
        log("");
        log("╔" + "═".repeat(70) + "╗");
        log("║ JSON ENVIADO PARA " + String.format("%-47s", clientIP + ":" + clientPort) + "║");
        log("╠" + "═".repeat(70) + "╣");

        // Exibe JSON completo sem quebras desnecessárias
        if (json.length() <= 66) {
            log("║ " + String.format("%-68s", json) + "║");
        } else {
            // Quebra JSON em múltiplas linhas se for muito longo
            int chunkSize = 66;
            for (int i = 0; i < json.length(); i += chunkSize) {
                int end = Math.min(i + chunkSize, json.length());
                String chunk = json.substring(i, end);
                log("║ " + String.format("%-68s", chunk) + "║");
            }
        }

        log("╚" + "═".repeat(70) + "╝");
    }
}