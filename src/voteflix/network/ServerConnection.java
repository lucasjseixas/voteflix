package voteflix.network;

import java.net.*;
import java.io.*;
import com.google.gson.Gson; // Importação necessária
import voteflix.service.ServerService;
import voteflix.dto.request.RequestBase; // Importação necessária
import voteflix.dto.response.ResponsePadrao; // Importação necessária
import voteflix.util.JsonValidator;

public class ServerConnection extends Thread {

    protected Socket clientSocket;
    private static final Gson GSON = new Gson(); // Instancia de Gson
    private static final ServerService SERVICE = new ServerService();

    /**
     * Do LogCallBack até private void log
     * Assim como remover toda e qualquer linha que contenha LOG
     * Tambem fazer a remocao dos listeners do sessionManager
     * Tambem observar o retorno ao System.out.println(mensagem);
     * Remover as INTERFACES para retornar ao sistema antes da implementacao da GUI
     */
    // Interface funcional para callback de logging
    public interface LogCallback {
        void log(String message);
    }

    private static LogCallback logCallback = null;

    // Método estático para registrar callback da GUI
    public static void setLogCallback(LogCallback callback) {
        logCallback = callback;
    }

    // Método auxiliar para logar tanto no console quanto na GUI
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
            serverSocket = new ServerSocket(porta);  // instancia o socket do servidor na porta especificada
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
        System.out.println("Nova thread de comunicacao iniciada.\n");
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
                log("║ 📥 JSON RECEBIDO DE " + String.format("%-46s", clientIP + ":" + clientPort) + "║");
                log("╠" + "═".repeat(70) + "╣");

                // Quebra o JSON em linhas para melhor visualização
                String[] jsonLines = inputLine.split("(?<=\\{)|(?=\\})|(?<=,)");
                for (String line : jsonLines) {
                    if (!line.trim().isEmpty()) {
                        log("║ " + String.format("%-68s", line.trim()) + "║");
                    }
                }
                log("╚" + "═".repeat(70) + "╝");

                // --- 1. INICIALIZAÇÃO E RECEBIMENTO ---
                System.out.println("Servidor recebeu JSON: " + inputLine);

                // Inicializamos o status como nulo ou um erro genérico (500) para garantir que
                // qualquer falha não tratada retorne um erro.

                // Variável para a resposta JSON específica (token, dados, etc.)
                String finalResponse = SERVICE.createStatusResponse("500");
                String statusFinal = "500";
                RequestBase reqBase = null;

                // *** VALIDAÇÃO DO JSON ***
                if (!JsonValidator.validateComplete(inputLine)) {
                    System.err.println(">>> JSON INVÁLIDO - Violação do padrão de nomenclatura");
                    finalResponse = SERVICE.createStatusResponse("400");
                    out.println(finalResponse);
                    continue; // Pula para próxima iteração, mantém conexão
                }

                try {
                    // --- 2. DESSERIALIZAÇÃO PARA CLASSE BASE (IDENTIFICAR OPERAÇÃO) ---
                    reqBase = GSON.fromJson(inputLine, RequestBase.class);
                    String operacao = reqBase.getOperacao();

                    System.out.println(">>> Operacao Identificada: " + operacao);

                    // 2. ROTEAMENTO PARA O HANDLER DE SERVIÇO
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
                        System.err.println("ALERTA: Handler falhou em definir finalResponse. Retornando 500.");
                        finalResponse = SERVICE.createStatusResponse("500");
                    }

                    // Extrai o status da resposta do handler para uso no controle de loop
                    ResponsePadrao res = GSON.fromJson(finalResponse, ResponsePadrao.class);
                    statusFinal = res.getStatus();

                } catch (Exception e) {
                    // --- 4. TRATAMENTO DE ERRO DE PARSING ---
                    statusFinal = "400";
                    System.err.println("ERRO de Parsing JSON (Status 400): " + e.getMessage());
                    // e.printStackTrace(); // Descomentar para debug
                }

                // --- CRIAÇÃO E ENVIO DA RESPOSTA FINAL ---
                System.out.println("Servidor enviou JSON: " + finalResponse);
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
                        System.out.println(">>> LOGOUT BEM-SUCEDIDO - Encerrando conexão com cliente.");
                    }
                    if (isExcluirSucesso) {
                        System.out.println(">>> EXCLUSÃO DE CONTA BEM-SUCEDIDA - Encerrando conexão com cliente.");
                    }
                    break; // Encerra o loop e a thread
                }
            }

            out.close();
            in.close();
            clientSocket.close();
            System.out.println("Thread de comunicacao encerrada.\n");
        } catch (IOException e) {
            System.err.println("Problema com Servidor de Communicacao!");
        }
    }
}

