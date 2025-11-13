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
            displayMenu(service.getCurrentToken());
            //getCurrentFuncao não está vindo pelo JWT do servidor, simplesmente é uma verificação na hora do login
            //se a reposta for 200 e o token for != de null, e o usuario for == admin, ele seta a funcao para 'admin', senao, seta para 'user'
            System.out.print("Digite a opcao: ");
            choice = stdIn.readLine();

            if (service.getCurrentToken() == null) {
                // USUÁRIO DESLOGADO
                switch (choice) {
                    case "1":
                        service.handleLogin();
                        break;
                    case "2":
                        service.handleCadastrarUsuario();
                        break;
                    case "3":
                        service.handleVerFilmes();
                        break;
                    case "4":
                        running = false;
                        break;
                    default:
                        System.out.println("Opcao invalida. Tente novamente.");
                }
            } else {
                // ADMIN LOGADO --> Qualquer usuário logado --> mudanca dia 12/11
                switch (choice) {
                    case "1":
                        service.handleListarUsuarios();
                        break;
                    case "2":
                        service.handleAdminEditarUsuario();
                        break;
                    case "3":
                        service.handleAdminExcluirUsuario();
                        break;
                    case "4":
                        service.handleVerFilmes();
                        break;
                    case "5":
                        service.handleAdicionarFilme();
                        break;
                    case "6":
                        service.handleEditarFilme();
                        break;
                    case "7":
                        service.handleExcluirFilme();
                        break;
                    case "8":
                        service.handleBuscarFilmeComReviews();
                        break;
                    case "9":
                        service.handleListarProprioUsuario();
                        break;
                    case "10":
                        service.handleEditarProprioUsuario();
                        break;
                    case "11":
                        service.handleExcluirProprioUsuario();
                        if (service.getCurrentToken() == null) {
                            System.out.println("\n>>> Conta excluída. Encerrando...");
                            running = false;
                        }
                        break;
                    case "12":
                        service.handleCriarReview();
                        break;
                    case "13":
                        service.handleListarMinhasReviews();
                        break;
                    case "14":
                        service.handleEditarReview();
                        break;
                    case "15":
                        service.handleExcluirReview();
                        break;
                    case "16":
                        service.handleLogout();
                        if (service.getCurrentToken() == null) {
                            System.out.println("\n>>> Logout bem-sucedido. Encerrando...");
                            running = false;
                        }
                        break;
                    default:
                        System.out.println("Opcao invalida. Tente novamente.");
                }
//            } else {
//                // USUÁRIO COMUM LOGADO
//                switch (choice) {
//                    case "1":
//                        service.handleListarProprioUsuario();
//                        break;
//                    case "2":
//                        service.handleEditarProprioUsuario();
//                        break;
//                    case "3":
//                        service.handleExcluirProprioUsuario();
//                        if (service.getCurrentToken() == null) {
//                            System.out.println("\n>>> Conta excluída. Encerrando...");
//                            running = false;
//                        }
//                        break;
//                    case "4":
//                        service.handleVerFilmes();
//                        break;
//                    case "5":
//                        service.handleLogout();
//                        if (service.getCurrentToken() == null) {
//                            System.out.println("\n>>> Logout bem-sucedido. Encerrando...");
//                            running = false;
//                        }
//                        break;
//                    default:
//                        System.out.println("Opcao invalida. Tente novamente.");
//                }
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
            System.out.println("3. Ver Catálogo de Filmes");
            System.out.println("4. Desconectar");
        } else {
            System.out.println("\n--- MENU ---");
            System.out.println("\n--- OPERACOES ADMINISTRATIVAS ---");
            System.out.println("1. Listar Todos os Usuários");
            System.out.println("2. Editar Usuário (por ID)");
            System.out.println("3. Excluir Usuário (por ID)");
            System.out.println("");
            System.out.println("\n--- OPERACOES DE FILMES ---");
            System.out.println("4. Ver Catálogo de Filmes");
            System.out.println("5. Adicionar Filme");
            System.out.println("6. Editar Filme");
            System.out.println("7. Excluir Filme");
            System.out.println("8. Buscar Filme por ID");
            System.out.println("");
            System.out.println("9. Listar Meus Dados");
            System.out.println("10. Atualizar Senha");
            System.out.println("11. Excluir Conta");
            System.out.println("\n--- OPERACOES DE REVIEW ---");
            System.out.println("12. Criar Review");
            System.out.println("13. Listar Minhas Reviews");
            System.out.println("14. Editar Reviews");
            System.out.println("15. Excluir Reviews");
            System.out.println("16. Logout");

        }
//        } else {
//            System.out.println("\n--- MENU USUÁRIO (LOGADO)---");
//            System.out.println("1. Listar Meus Dados");
//            System.out.println("2. Atualizar Senha");
//            System.out.println("3. Excluir Conta");
//            System.out.println("4. Ver Catálogo de Filmes");
//            System.out.println("5. Logout");
//        }
    }
}