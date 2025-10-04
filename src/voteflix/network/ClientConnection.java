package voteflix.network;

import java.io.*;
import java.net.*;

import voteflix.service.ClientService;

public class ClientConnection {
    public static void main(String[] args) throws IOException {

        System.out.println("Qual o IP do servidor? ");
        BufferedReader brIP = new BufferedReader(new InputStreamReader(System.in));
        String serverIP = brIP.readLine();

        System.out.println("Qual a Porta do servidor? ");
        BufferedReader brPort = new BufferedReader(new InputStreamReader(System.in));
        int serverPort = Integer.parseInt(brPort.readLine());

        System.out.println("Tentando conectar com host " + serverIP + " na porta " + serverPort);

        Socket echoSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            echoSocket = new Socket(serverIP, serverPort);
            out = new PrintWriter(echoSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host " + serverIP + " nao encontrado!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Nao foi possivel reservar I/O para conectar com " + serverIP);
            System.exit(1);
        }

        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        // Instancia a classe de serviço, passando os objetos de I/O
        ClientService service = new ClientService(out, in, stdIn);

        String choice;

        System.out.println("Conectado. Escolha uma opcao no menu abaixo:");

        boolean running = true;
        while (running) {
            // Usa o service para obter o estado
            displayMenu(service.getCurrentToken());
            System.out.print("Digite a opcao: ");
            choice = stdIn.readLine();

            if (service.getCurrentToken() == null) { // Usuário Deslogado
                switch (choice) {
                    case "1":
                        service.handleLogin();
                        break;
                    case "2":
                        service.handleCadastrarUsuario();
                        break;
                    case "3":
                        running = false;
                        break;
                    default:
                        System.out.println("Opcao invalida. Tente novamente.");
                }
            } else { // Usuário Logado
                switch (choice) {
                    case "1":
                        service.handleLogout();
                        // Após logout, encerra o cliente
                        System.out.println("\n>>> Conexão encerrada pelo servidor após logout.");
                        running = false;
                        break;
                    case "2":
                        service.handleListarProprioUsuario();
                        break;
                    case "3":
                        service.handleEditarProprioUsuario();
                        break;
                    case "4":
                        service.handleExcluirProprioUsuario();
                        // Após exclusão, encerra o cliente
                        System.out.println("\n>>> Conexão encerrada pelo servidor após exclusão de conta.");
                        running = false;

                        break;
                    case "5":
                        running = false;
                        break;
                    default:
                        System.out.println("Opcao invalida. Tente novamente.");
                }
            }
        }

        out.close();
        in.close();
        stdIn.close();
        echoSocket.close();
    }

    private static void displayMenu(String token) {
        if (token == null) {
            System.out.println("\n--- MENU PRINCIPAL (DESLOGADO) ---");
            System.out.println("1. Login");
            System.out.println("2. Cadastrar Novo Usuario");
            System.out.println("3. Desconectar");
        } else {
            System.out.println("\n--- MENU PRINCIPAL (LOGADO)  ---");
            System.out.println("1. Logout");
            System.out.println("2. Listar Dados Proprios Usuario");
            System.out.println("3. Atualizar Senha");
            System.out.println("4. Excluir Conta");
            System.out.println("5. Desconectar");
        }
    }
}