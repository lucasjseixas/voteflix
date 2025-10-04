package voteflix.network;

import java.net.*;
import java.io.*;
import com.google.gson.Gson; // Importação necessária
import voteflix.service.ServerService;
import voteflix.dto.request.RequestBase; // Importação necessária
import voteflix.dto.response.ResponsePadrao; // Importação necessária

public class ServerConnection extends Thread {

    protected Socket clientSocket;
    private static final Gson GSON = new Gson(); // Instância de Gson
    private static final ServerService SERVICE = new ServerService();

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

        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null) {

                // --- 1. INICIALIZAÇÃO E RECEBIMENTO ---
                System.out.println("Servidor recebeu JSON: " + inputLine);

                // Inicializamos o status como nulo ou um erro genérico (500) para garantir que
                // qualquer falha não tratada retorne um erro.

                // Variável para a resposta JSON específica (token, dados, etc.)
                String finalResponse = SERVICE.createStatusResponse("500");
                String statusFinal = "500";
                RequestBase reqBase = null;

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

