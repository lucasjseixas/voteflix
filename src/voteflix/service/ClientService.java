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

import voteflix.dto.FilmeDTO;
import voteflix.dto.request.CriarFilmeRequest;
import voteflix.dto.request.EditarFilmeRequest;
import voteflix.dto.request.ExcluirFilmeRequest;
import voteflix.dto.response.ListarFilmesResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public void handleVerFilmes() throws IOException {
        System.out.println("\n--- CATÁLOGO DE FILMES ---");

        String jsonRequest = "{\"operacao\":\"LISTAR_FILMES\"}";

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ListarFilmesResponse res = GSON.fromJson(jsonResponse, ListarFilmesResponse.class);
                HttpStatus status = HttpStatus.fromCode(res.status);

                if (status == HttpStatus.OK && res.filmes != null) {
                    if (res.filmes.isEmpty()) {
                        System.out.println("\n>>> Nenhum filme cadastrado ainda.");
                    } else {
                        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
                        System.out.println("║                    CATÁLOGO DE FILMES                         ║");
                        System.out.println("╠═══════════════════════════════════════════════════════════════╣");

                        for (FilmeDTO filme : res.filmes) {
                            System.out.println("║");
                            System.out.println("║ 🎬 " + filme.titulo);
                            System.out.println("║    ID: " + filme.id + " | Diretor: " + filme.diretor + " | Ano: " + filme.ano);
                            System.out.println("║    Gêneros: " + String.join(", ", filme.genero));
                            System.out.println("║    ⭐ Nota: " + filme.nota + " (" + filme.qtdAvaliacoes + " avaliações)");
                            System.out.println("║    Sinopse: " + filme.sinopse);
                            System.out.println("║");
                            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
                        }

                        System.out.println("║ Total: " + res.filmes.size() + " filme(s)");
                        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
                    }
                } else {
                    System.err.println(status.getFormattedMessage());
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    public void handleAdicionarFilme() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado como admin para executar esta operação.");
            return;
        }

        System.out.println("\n--- ADICIONAR NOVO FILME ---");

        System.out.print("Título (max 30 caracteres): ");
        String titulo = stdIn.readLine().trim();

        System.out.print("Diretor: ");
        String diretor = stdIn.readLine().trim();

        System.out.print("Ano (YYYY): ");
        String ano = stdIn.readLine().trim();

        System.out.println("\nGêneros disponíveis:");
        System.out.println("1. Ação          2. Aventura       3. Comédia");
        System.out.println("4. Drama         5. Fantasia       6. Ficção Científica");
        System.out.println("7. Terror        8. Romance        9. Documentário");
        System.out.println("10. Musical      11. Animação");

        String[] generosDisponiveis = {
                "Ação", "Aventura", "Comédia", "Drama", "Fantasia",
                "Ficção Científica", "Terror", "Romance", "Documentário", "Musical", "Animação"
        };

        System.out.print("Digite os números dos gêneros separados por vírgula (ex: 1,3,6): ");
        String generosInput = stdIn.readLine().trim();

        List<String> generos = new ArrayList<>();
        try {
            String[] indices = generosInput.split(",");
            for (String idx : indices) {
                int index = Integer.parseInt(idx.trim()) - 1;
                if (index >= 0 && index < generosDisponiveis.length) {
                    generos.add(generosDisponiveis[index]);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar gêneros. Operação cancelada.");
            return;
        }

        if (generos.isEmpty()) {
            System.err.println("Nenhum gênero válido selecionado. Operação cancelada.");
            return;
        }

        System.out.print("Sinopse (max 250 caracteres): ");
        String sinopse = stdIn.readLine().trim();

        // Validações
        if (titulo.isEmpty() || diretor.isEmpty() || ano.isEmpty() || sinopse.isEmpty()) {
            System.err.println("Erro: Todos os campos são obrigatórios.");
            return;
        }

        if (titulo.length() > 30) {
            System.err.println("Erro: Título não pode exceder 30 caracteres.");
            return;
        }

        if (ano.length() != 4) {
            System.err.println("Erro: Ano deve ter exatamente 4 dígitos.");
            return;
        }

        if (sinopse.length() > 250) {
            System.err.println("Erro: Sinopse não pode exceder 250 caracteres.");
            return;
        }

        CriarFilmeRequest.FilmeData filmeData = new CriarFilmeRequest.FilmeData(
                titulo, diretor, ano, generos, sinopse
        );
        CriarFilmeRequest req = new CriarFilmeRequest(filmeData, currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(res.status);

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

    public void handleEditarFilme() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado como admin para executar esta operação.");
            return;
        }

        System.out.println("\n--- EDITAR FILME ---");

        System.out.print("ID do filme a editar: ");
        String id = stdIn.readLine().trim();

        System.out.println("\n>>> Digite os novos dados (mantenha os dados atuais se não quiser alterar)");

        System.out.print("Novo Título (max 30): ");
        String titulo = stdIn.readLine().trim();

        System.out.print("Novo Diretor: ");
        String diretor = stdIn.readLine().trim();

        System.out.print("Novo Ano (YYYY): ");
        String ano = stdIn.readLine().trim();

        System.out.println("\nGêneros disponíveis:");
        System.out.println("1. Ação          2. Aventura       3. Comédia");
        System.out.println("4. Drama         5. Fantasia       6. Ficção Científica");
        System.out.println("7. Terror        8. Romance        9. Documentário");
        System.out.println("10. Musical      11. Animação");

        String[] generosDisponiveis = {
                "Ação", "Aventura", "Comédia", "Drama", "Fantasia",
                "Ficção Científica", "Terror", "Romance", "Documentário", "Musical", "Animação"
        };

        System.out.print("Digite os números dos gêneros separados por vírgula: ");
        String generosInput = stdIn.readLine().trim();

        List<String> generos = new ArrayList<>();
        try {
            String[] indices = generosInput.split(",");
            for (String idx : indices) {
                int index = Integer.parseInt(idx.trim()) - 1;
                if (index >= 0 && index < generosDisponiveis.length) {
                    generos.add(generosDisponiveis[index]);
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar gêneros. Operação cancelada.");
            return;
        }

        System.out.print("Nova Sinopse (max 250): ");
        String sinopse = stdIn.readLine().trim();

        // Validações
        if (titulo.isEmpty() || diretor.isEmpty() || ano.isEmpty() || generos.isEmpty() || sinopse.isEmpty()) {
            System.err.println("Erro: Todos os campos são obrigatórios.");
            return;
        }

        if (titulo.length() > 30 || ano.length() != 4 || sinopse.length() > 250) {
            System.err.println("Erro: Verifique os limites de caracteres.");
            return;
        }

        EditarFilmeRequest.FilmeUpdate filmeUpdate = new EditarFilmeRequest.FilmeUpdate(
                id, titulo, diretor, ano, generos, sinopse
        );
        EditarFilmeRequest req = new EditarFilmeRequest(filmeUpdate, currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(res.status);

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

    public void handleExcluirFilme() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado como admin para executar esta operação.");
            return;
        }

        System.out.println("\n--- EXCLUIR FILME ---");
        System.out.print("ID do filme a excluir: ");
        String id = stdIn.readLine().trim();

        System.out.print("⚠️  ATENÇÃO: Esta ação excluirá o filme e todas as suas avaliações!\nTem certeza? (S/N): ");
        String confirmacao = stdIn.readLine().trim();

        if (!confirmacao.equalsIgnoreCase("S")) {
            System.out.println("Operação cancelada.");
            return;
        }

        ExcluirFilmeRequest req = new ExcluirFilmeRequest(id, currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ResponsePadrao res = GSON.fromJson(jsonResponse, ResponsePadrao.class);
                HttpStatus status = HttpStatus.fromCode(res.status);

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