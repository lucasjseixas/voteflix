package voteflix.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import voteflix.auth.AuthResponse;
import voteflix.dto.request.*;
import voteflix.dto.response.ListarProprioUsuarioResponse;
import voteflix.dto.response.ResponsePadrao;

public class ClientService {

    private final Gson GSON = new Gson();

    private final PrintWriter out;
    private final BufferedReader in;
    private final BufferedReader stdIn;

    // Variáveis de Estado da Sessão
    private String currentToken = null;
    private String currentFuncao = null;
    private String currentUsuario = null;
    private int currentIdUsuario = -1;

    // Construtor: Recebe os objetos de I/O criados no main do cliente
    public ClientService(PrintWriter out, BufferedReader in, BufferedReader stdIn) {
        this.out = out;
        this.in = in;
        this.stdIn = stdIn;
    }

    // Getters para a classe do loop principal (para controle de menu)
    public String getCurrentToken() {
        return currentToken;
    }

    public String getCurrentFuncao() {
        return currentFuncao;
    }

    public void handleLogin() throws IOException {
        System.out.print("Usuário: ");
        String usuario = stdIn.readLine();
        System.out.print("Senha: ");
        String senha = stdIn.readLine();

        LoginRequest req = new LoginRequest(usuario, senha);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                AuthResponse res = GSON.fromJson(jsonResponse, AuthResponse.class);

                if ("200".equals(res.getStatus())) {
                    currentToken = res.getToken();
                } else {
                    System.out.println("Login falhou. Status retornado: " + res.getStatus());
                    currentToken = null;
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar o JSON de resposta do servidor: " + e.getMessage());
                currentToken = null;
            }
        }
    }

    public void handleLogout() throws IOException {
        if (currentToken == null) {
            System.out.println("Você já está deslogado.");
            return;
        }

        LogoutRequest req = new LogoutRequest(currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);

                if ("200".equals(res.getStatus())) {
                    System.out.println("Logout bem-sucedido.");
                    currentToken = null;
                    currentFuncao = null;
                    currentUsuario = null;
                    currentIdUsuario = -1;
                } else {
                    System.err.println("Logout falhou no servidor (Status: " + res.getStatus() + "). Removendo token local por segurança.");
                    currentToken = null; currentFuncao = null; currentUsuario = null; currentIdUsuario = -1;
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar o JSON de resposta do servidor: " + e.getMessage());
            }
        }
    }

    public void handleCadastrarUsuario() throws IOException {
        System.out.println("\n--- CADASTRO DE NOVO USUÁRIO COMUM ---");
        System.out.print("Novo Usuário (3-20 caracteres, letras e números): ");
        String usuario = stdIn.readLine();
        System.out.print("Nova Senha (3-20 caracteres, letras e números): ");
        String senha = stdIn.readLine();

        if (usuario.length() < 3 || usuario.length() > 20 || senha.length() < 3 || senha.length() > 20) {
            System.err.println("Erro: Usuário e Senha devem ter entre 3 e 20 caracteres.");
            return;
        }

        CadastrarUsuarioRequest req = new CadastrarUsuarioRequest(usuario, senha);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                String status = res.getStatus();

                switch (status) {
                    case "201" ->  // **SUCESSO (CREATED)**
                            System.out.println("Cadastro de usuário BEM-SUCEDIDO. 201");
                    case "400" -> System.out.println("400 Bad Request");
                    case "409" -> System.out.println("409 Already exists");
                    case "422" -> System.out.println("422 Unprocessable Entity");
                    case "500" -> System.out.println("500 Internal Server Error");
                    case null, default -> System.out.println("Status retornado: " + status);
                }
            } catch (Exception e) {
                System.err.println("ERRO: Falha na desserialização da resposta do servidor: " + e.getMessage());
            }
        }
    }

    public void handleListarProprioUsuario() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado para executar esta operação.");
            return;
        }

        ListarProprioUsuarioRequest req = new ListarProprioUsuarioRequest(currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ListarProprioUsuarioResponse res = GSON.fromJson(jsonResponse, ListarProprioUsuarioResponse.class);

                if ("200".equals(res.status)) {
                    System.out.println("\n--- DADOS DO USUÁRIO ---");
                    System.out.println("Usuário: " + res.usuario);
                } else {
                    System.out.println("Falha ao listar dados. Status: " + res.status);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }
    public void handleEditarProprioUsuario() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado para executar esta operação.");
            return;
        }

        System.out.println("\n--- ATUALIZAR SENHA ---");
        System.out.print("Nova Senha (3-20 caracteres): ");
        String novaSenha = stdIn.readLine();

        // Validação no cliente
        if (novaSenha.length() < 3 || novaSenha.length() > 20) {
            System.err.println("Erro: A senha deve ter entre 3 e 20 caracteres.");
            return;
        }

        EditarProprioUsuarioRequest req = new EditarProprioUsuarioRequest(novaSenha, currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                String status = res.getStatus();

                switch (status) {
                    case "200" -> System.out.println("✓ Senha atualizada com sucesso!");
                    case "400" -> System.out.println("✗ Erro: Requisição inválida (400).");
                    case "401" -> System.out.println("✗ Erro: Token inválido ou expirado (401).");
                    case "422" -> System.out.println("✗ Erro: Senha fora do padrão (422).");
                    case "500" -> System.out.println("✗ Erro interno do servidor (500).");
                    default -> System.out.println("Status retornado: " + status);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    public void handleExcluirProprioUsuario() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado para executar esta operação.");
            return;
        }

        System.out.println("\n⚠️  ATENÇÃO: EXCLUSÃO DE CONTA ⚠️");
        System.out.println("Esta ação é IRREVERSÍVEL e irá excluir sua conta permanentemente.");
        System.out.print("Tem certeza que deseja continuar? (S/N): ");
        String confirmacao = stdIn.readLine();

        if (!confirmacao.equalsIgnoreCase("S")) {
            System.out.println("Operação cancelada.");
            return;
        }

        ExcluirProprioUsuarioRequest req = new ExcluirProprioUsuarioRequest(currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                String status = res.getStatus();

                switch (status) {
                    case "200" -> {
                        System.out.println("✓ Conta excluída com sucesso!");
                        System.out.println("Você foi desconectado. Retornando ao menu principal...");
                        // Limpa o token e os dados da sessão
                        currentToken = null;
                        currentFuncao = null;
                        currentUsuario = null;
                        currentIdUsuario = -1;
                    }
                    case "400" -> System.out.println("✗ Erro: Requisição inválida (400).");
                    case "401" -> {
                        System.out.println("✗ Erro: Token inválido ou expirado (401).");
                        // Limpa o token local por segurança
                        currentToken = null;
                        currentFuncao = null;
                        currentUsuario = null;
                        currentIdUsuario = -1;
                    }
                    case "403" -> System.out.println("✗ Erro: Não é possível excluir o administrador (403).");
                    case "404" -> {
                        System.out.println("✗ Erro: Usuário não encontrado (404).");
                        // Limpa o token local pois o usuário não existe mais
                        currentToken = null;
                        currentFuncao = null;
                        currentUsuario = null;
                        currentIdUsuario = -1;
                    }
                    case "500" -> System.out.println("✗ Erro interno do servidor (500).");
                    default -> System.out.println("Status retornado: " + status);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

}