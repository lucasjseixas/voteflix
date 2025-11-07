package voteflix.service;

import com.google.gson.Gson;
import voteflix.dto.UsuarioDTO;
import voteflix.dto.request.*;
import voteflix.dto.response.ListarProprioUsuarioResponse;
import voteflix.dto.response.ListarUsuariosResponse;
import voteflix.dto.response.LoginResponse;
import voteflix.dto.response.ResponsePadrao;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import voteflix.dto.FilmeDTO;
import voteflix.dto.request.CriarFilmeRequest;
import voteflix.dto.request.EditarFilmeRequest;
import voteflix.dto.request.ExcluirFilmeRequest;
import voteflix.dto.response.ListarFilmesResponse;
import java.util.ArrayList;
import java.util.List;

public class ClientService {

    private final Gson GSON = new Gson();

    private final PrintWriter out;
    private final BufferedReader in;
    private final BufferedReader stdIn;

    private String currentToken = null;
    private String currentFuncao = null;
    private String currentUsuario = null;

    public ClientService(PrintWriter out, BufferedReader in, BufferedReader stdIn) {
        this.out = out;
        this.in = in;
        this.stdIn = stdIn;
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public String getCurrentFuncao() {
        return currentFuncao;
    }

//    public String createErrorResponse(String statusCode) {
//        HttpStatus status = HttpStatus.fromCode(statusCode);
//        return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
//    }

    public void handleLogin() throws IOException {
        if (currentToken != null) {
            System.out.println("Você já está logado como " + currentUsuario + "!");
            return;
        }

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
                LoginResponse res = GSON.fromJson(jsonResponse, LoginResponse.class);

                if ("200".equals(res.status) && res.token != null) {
                    currentToken = res.token;
                    currentUsuario = usuario;
                    currentFuncao = usuario.equals("admin") ? "admin" : "user";

                    System.out.println("\n");
                    System.out.println("Bem-vindo, " + currentUsuario +
                            (currentFuncao.equals("admin") ? " (Administrador)" : ""));
                } else {
                    System.err.println(res.mensagem);
                    clearSession();
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
                clearSession();
            }
        }
    }

    private void clearSession() {
        currentToken = null;
        currentFuncao = null;
        currentUsuario = null;
    }

    public void handleLogout() throws IOException {
        if (currentToken == null) {
            System.out.println("Você ja esta deslogado.");
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

                clearSession();
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

                if ("201".equals(res.status)) {
                    System.out.println(res.mensagem);
                } else {
                    System.err.println(res.mensagem);
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

                if ("200".equals(res.status)) {
                    System.out.println("\n--- DADOS DO USUÁRIO ---");
                    System.out.println("Usuário: " + res.usuario);
                } else {
                    System.err.println(res.mensagem);
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

                if ("200".equals(res.status)) {
                    System.out.println(res.mensagem);
                } else {
                    System.err.println(res.mensagem);
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

        System.out.println("\nATENÇÃO: EXCLUSÃO DE CONTA");
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

                System.out.println(res.mensagem);

                if ("200".equals(res.status)) {
                    System.out.println("Você foi desconectado.");
                    clearSession();
                } else if ("401".equals(res.status) || "404".equals(res.status)) {
                    clearSession();
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

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

                if ("200".equals(res.status) && res.usuarios != null) {
                    System.out.println("\n=== LISTA DE USUÁRIOS ===");
                    for (UsuarioDTO user : res.usuarios) {
                        System.out.println("ID: " + user.id + " | Nome: " + user.nome);
                    }
                    System.out.println("Total: " + res.usuarios.size() + " usuário(s)");
                    System.out.println("========================\n");
                } else {
                    System.err.println(res.mensagem);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

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

                if ("200".equals(res.status)) {
                    System.out.println(res.mensagem);
                } else {
                    System.err.println(res.mensagem);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

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

                if ("200".equals(res.status)) {
                    System.out.println(res.mensagem);
                } else {
                    System.err.println(res.mensagem);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    public void handleVerFilmes() throws IOException {
        if (currentToken == null) {
            System.out.println("Você precisa estar logado para executar esta operação.");
            return;
        }

        System.out.println("\n--- CATÁLOGO DE FILMES ---");

        ListarFilmesRequest req = new ListarFilmesRequest(currentToken);
        String jsonRequest = GSON.toJson(req);

        System.out.println("Enviando: " + jsonRequest);
        out.println(jsonRequest);

        String jsonResponse = in.readLine();
        System.out.println("Servidor retornou: " + jsonResponse);

        if (jsonResponse != null) {
            try {
                ListarFilmesResponse res = GSON.fromJson(jsonResponse, ListarFilmesResponse.class);

                if ("200".equals(res.status) && res.filmes != null) {
                    if (res.filmes.isEmpty()) {
                        System.out.println("\n>>> Nenhum filme cadastrado ainda.");
                    } else {
                        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
                        System.out.println("║                    CATÁLOGO DE FILMES                         ║");
                        System.out.println("╠═══════════════════════════════════════════════════════════════╣");

                        for (FilmeDTO filme : res.filmes) {
                            System.out.println("║");
                            System.out.println("║" + filme.titulo);
                            System.out.println("║    ID: " + filme.id + " | Diretor: " + filme.diretor + " | Ano: " + filme.ano);
                            System.out.println("║    Gêneros: " + String.join(", ", filme.genero));
                            System.out.println("║    Nota: " + filme.nota + " (" + filme.qtdAvaliacoes + " avaliações)");
                            System.out.println("║    Sinopse: " + filme.sinopse);
                            System.out.println("║");
                            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
                        }

                        System.out.println("║ Total: " + res.filmes.size() + " filme(s)");
                        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
                    }
                } else {
                    System.err.println(res.mensagem);
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

        if (titulo.isEmpty() || diretor.isEmpty() || ano.isEmpty()) {
            System.err.println("Erro: Os campos titulo, diretor e ano sao obrigatorios");
            return;
        }

        if (titulo.length() < 3 || titulo.length() > 30) {
            System.err.println("Erro: Titulo não pode ser menor que 3 e maior que 30 caracteres.");
            return;
        }

        if (diretor.length() < 3 || diretor.length() > 30) {
            System.err.println("Erro: Diretor não pode ser menor que 3 e maior que 30 caracteres.");
            return;
        }

        if (ano.length() < 3 || ano.length() > 4) {
            System.err.println("Erro: O Ano deve possuir 3 algarismos.");
            return;
        }

        if (sinopse.length() > 250) {
            System.err.println("Erro: Sinopse não pode exceder 250 caracteres. Mas pode ser vazia");
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

                if ("201".equals(res.status)) {
                    System.out.println(res.mensagem);
                } else {
                    System.err.println(res.mensagem);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }

    public void handleEditarFilme() throws IOException {

        //Para editar os filmes eu terei que relistar os filmes para obter os dados do servidor, senao nao tem como implementar esse tipo de operação
        if (currentToken == null) {
            System.out.println("Você precisa estar logado como admin para executar esta operação.");
            return;
        }

        // ===== 1. LISTAR FILMES =====
        ListarFilmesRequest reqListar = new ListarFilmesRequest(currentToken);
        out.println(GSON.toJson(reqListar));
        String jsonResponseListar = in.readLine();

        ListarFilmesResponse resListar = GSON.fromJson(jsonResponseListar, ListarFilmesResponse.class);

        if (!"200".equals(resListar.status) || resListar.filmes == null || resListar.filmes.isEmpty()) {
            System.err.println("Nenhum filme disponível.");
            return;
        }

        // ===== 2. ESCOLHER FILME =====
        System.out.println("\n=== FILMES ===");
        for (int i = 0; i < resListar.filmes.size(); i++) {
            FilmeDTO f = resListar.filmes.get(i);
            System.out.println((i + 1) + ". [ID: " + f.id + "] " + f.titulo);
        }

        System.out.print("\nEscolha o filme: ");
        int escolha = Integer.parseInt(stdIn.readLine()) - 1;

        if (escolha < 0 || escolha >= resListar.filmes.size()) {
            System.err.println("Opção inválida.");
            return;
        }

        // ===== 3. GUARDAR DADOS ORIGINAIS =====
        FilmeDTO original = resListar.filmes.get(escolha);

        System.out.println("\n=== EDITANDO: " + original.titulo + " ===");
        System.out.println("(Deixe em branco para manter o valor atual)\n");

        //System.out.println("\n--- EDITAR FILME ---");

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

        if (titulo.isEmpty() || diretor.isEmpty() || ano.isEmpty() || generos.isEmpty()) {
            System.err.println("Erro: Os campos Titulo, Diretor e Ano são obrigatorios.");
            return;
        }

        if (titulo.length() < 3 || titulo.length() > 30 || ano.length() < 3 || ano.length() > 4 || sinopse.length() > 250) {
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

                if ("200".equals(res.status)) {
                    System.out.println(res.mensagem);
                } else {
                    System.err.println(res.mensagem);
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

        System.out.print("ATENÇÃO: Esta ação excluirá o filme e todas as suas avaliações!\nTem certeza? (S/N): ");
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

                if ("200".equals(res.status)) {
                    System.out.println(res.mensagem);
                } else {
                    System.err.println(res.mensagem);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar resposta: " + e.getMessage());
            }
        }
    }
}