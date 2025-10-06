package voteflix.service;

import com.google.gson.Gson;
import voteflix.auth.AuthResponse;
import voteflix.dto.UsuarioDTO;
import voteflix.dto.request.*;
import voteflix.dto.response.ListarProprioUsuarioResponse;
import voteflix.dto.response.ListarUsuariosResponse;
import voteflix.dto.response.ResponsePadrao;
import voteflix.util.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

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

    public void handleLogin() throws IOException {
        System.out.print("Usuário: ");
        String usuario = stdIn.readLine();
        System.out.print("Senha: ");
        String senha = stdIn.readLine();

        LoginRequest req = new LoginRequest(usuario, senha);
        String jsonRequest = GSON.toJson(req);

        // O que está sendo enviado ao SERVIDOR
        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        // O que está sendo recebido no CLIENTE
        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                AuthResponse res = GSON.fromJson(jsonResponse, AuthResponse.class);
                HttpStatus status = HttpStatus.fromCode(res.getStatus());

                if (status == HttpStatus.OK) {
                    currentToken = res.getToken();

                    // Extrai claims do token
                    com.auth0.jwt.interfaces.DecodedJWT decodedJWT =
                            com.auth0.jwt.JWT.decode(currentToken);

                    // MUDANÇA: extrai ID do claim ao invés do subject
                    currentIdUsuario = decodedJWT.getClaim("id").asInt();
                    currentFuncao = decodedJWT.getClaim("funcao").asString();
                    currentUsuario = decodedJWT.getClaim("usuario").asString();

                    System.out.println(status.getFormattedMessage());
                    System.out.println("Bem-vindo, " + currentUsuario + " (ID: " + currentIdUsuario + ", " + currentFuncao + ")");
                } else {
                    System.err.println(status.getFormattedMessage());
                    currentToken = null;
                    currentFuncao = null;
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
                currentToken = null;
                currentFuncao = null;
            }
        }


    }

    public String getCurrentFuncao() {
        return currentFuncao;
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
                HttpStatus status = HttpStatus.fromCode(res.getStatus());

                if (status == HttpStatus.OK) {
                    System.out.println(status.getFormattedMessage());
                } else {
                    System.err.println(status.getFormattedMessage() + " Removendo token local por segurança.");
                }
                currentToken = null;
                currentFuncao = null;
                currentUsuario = null;
                currentIdUsuario = -1;
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
                HttpStatus status = HttpStatus.fromCode(res.getStatus());
                if (status.isSuccess()) {
                    System.out.println(status.getFormattedMessage());
                } else {
                    System.err.println(status.getFormattedMessage());
                }
            } catch (Exception e) {
                System.err.println("ERRO: Falha ao processar resposta: " + e.getMessage());
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
                HttpStatus status = HttpStatus.fromCode(res.status);

                if (status == HttpStatus.OK) {
                    System.out.println("\n--- DADOS DO USUÁRIO ---");
                    System.out.println("Usuário: " + res.usuario);
                } else {
                    System.err.println(status.getFormattedMessage());
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
                HttpStatus status = HttpStatus.fromCode(res.getStatus());

                if (status.isSuccess()) {
                    System.out.println(status.getFormattedMessage());
                } else {
                    System.err.println(status.getFormattedMessage());
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
                HttpStatus status = HttpStatus.fromCode(res.getStatus());

                switch (status) {
                    case OK:
                        System.out.println(status.getFormattedMessage());
                        System.out.println("Você foi desconectado.");
                        currentToken = null;
                        currentFuncao = null;
                        currentUsuario = null;
                        currentIdUsuario = -1;
                        break;
                    case UNAUTHORIZED:
                    case NOT_FOUND:
                        System.err.println(status.getFormattedMessage());
                        // Limpa o token local por segurança
                        currentToken = null;
                        currentFuncao = null;
                        currentUsuario = null;
                        currentIdUsuario = -1;
                        break;
                    case FORBIDDEN:
                        System.err.println(status.getFormattedMessage());
                        // NÃO limpa token - admin não pode se excluir mas continua logado
                        break;
                    default:
                        System.err.println(status.getFormattedMessage());
                        // Outros erros não limpam token
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * ADMIN: Lista todos os usuários
     */
    public void handleListarUsuarios() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado para executar esta operação.");
            return;
        }

        ListarUsuariosRequest req = new ListarUsuariosRequest(currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ListarUsuariosResponse res = GSON.fromJson(jsonResponse, ListarUsuariosResponse.class);
                HttpStatus status = HttpStatus.fromCode(res.status);

                if (status == HttpStatus.OK && res.usuarios != null) {
                    System.out.println("\n=== LISTA DE USUÁRIOS ===");
                    for (UsuarioDTO user : res.usuarios) {
                        System.out.println("ID: " + user.id + " | Usuário: " + user.usuario);
                    }
                    System.out.println("Total: " + res.usuarios.size() + " usuário(s)");
                    System.out.println("========================\n");
                } else {
                    System.err.println(status.getFormattedMessage());
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * ADMIN: Edita senha de outro usuário
     */
    public void handleAdminEditarUsuario() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado para executar esta operação.");
            return;
        }

        System.out.println("\n--- ADMIN: EDITAR USUÁRIO ---");
        System.out.print("ID do usuário a editar: ");
        String idUsuario = stdIn.readLine();

        System.out.print("Nova senha (3-20 caracteres): ");
        String novaSenha = stdIn.readLine();

        if (novaSenha.length() < 3 || novaSenha.length() > 20) {
            System.err.println("Erro: A senha deve ter entre 3 e 20 caracteres.");
            return;
        }

        AdminEditarUsuarioRequest req = new AdminEditarUsuarioRequest(novaSenha, currentToken, idUsuario);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(res.getStatus());

                if (status.isSuccess()) {
                    System.out.println(status.getFormattedMessage());
                } else {
                    System.err.println(status.getFormattedMessage());
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    /**
     * ADMIN: Exclui usuário comum
     */
    public void handleAdminExcluirUsuario() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado para executar esta operação.");
            return;
        }

        System.out.println("\n--- ADMIN: EXCLUIR USUÁRIO ---");
        System.out.print("ID do usuário a excluir: ");
        String idUsuario = stdIn.readLine();

        System.out.print("Tem certeza? (S/N): ");
        String confirmacao = stdIn.readLine();

        if (!confirmacao.equalsIgnoreCase("S")) {
            System.out.println("Operação cancelada.");
            return;
        }

        AdminExcluirUsuarioRequest req = new AdminExcluirUsuarioRequest(idUsuario, currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(res.getStatus());

                if (status.isSuccess()) {
                    System.out.println(status.getFormattedMessage());
                } else {
                    System.err.println(status.getFormattedMessage());
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

}