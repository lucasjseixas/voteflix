package voteflix.service;

import com.google.gson.Gson;
import voteflix.auth.Autenticacao;
import voteflix.dto.FilmeDTO;
import voteflix.dto.UsuarioDTO;
import voteflix.dto.request.*;
import voteflix.dto.response.*;
import voteflix.entity.*;
import voteflix.repository.FilmeRepository;
import voteflix.repository.UsuarioRepository;
import voteflix.session.SessionManager;
import voteflix.util.GeneroFilme;
import voteflix.util.HttpStatus;

import java.util.List;

public class ServerService {

    private final Gson GSON = new Gson();
    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final FilmeRepository filmeRepository = new FilmeRepository();
    private final Autenticacao AUTH = new Autenticacao();

    public String createStatusResponse(String statusCode) {
        HttpStatus status = HttpStatus.fromCode(statusCode);
        return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
    }

    public String handleLogin(String jsonRequest) {
        try {
            LoginRequest req = GSON.fromJson(jsonRequest, LoginRequest.class);
            String usuarioNome = req.usuario;
            String senha = req.senha;

            // Busca o usuario no repositorio
            Usuario usuario = usuarioRepository.findByUsuario(usuarioNome);

            // Validacao de credenciais
            if (usuario == null || !usuario.getSenha().equals(senha)) {
                System.out.println("-> Login falhou: Senha invalida (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new LoginResponse(status.getCode(), status.getMessage()));
            }

            // Geração do token
            String jwtToken = AUTH.generateToken(usuario.getId(), usuario.getUsuario(), usuario.getFuncao());
            System.out.println("-> Login BEM-SUCEDIDO: JWT: " + jwtToken.substring(0, 20) + "...");

            // Adiciona sessao
            SessionManager.getInstance().addSession(usuarioNome);

            // Usa o construtor de sucesso (status e token)
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new LoginResponse(status.getCode(), status.getMessage(), jwtToken));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar Login: " + e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST;
            return GSON.toJson(new LoginResponse(status.getCode(), status.getMessage()));
        }
    }

    public String handleCadastrarUsuario(String jsonRequest) {
        try {
            CadastrarUsuarioRequest req = GSON.fromJson(jsonRequest, CadastrarUsuarioRequest.class);

            // Acessa dados aninhados
            String usuario = req.usuario.nome;
            String senha = req.usuario.senha;

            // Validacao de tamanho e formato
            if (usuario == null || senha == null ||
                    usuario.length() < 3 || usuario.length() > 20 ||
                    senha.length() < 3 || senha.length() > 20) {

                System.out.println("-> Cadastro falhou: Dados fora do padrão (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Validacao de unicidade
            Usuario novoUsuario = usuarioRepository.addComum(usuario, senha);

            if (novoUsuario == null) {
                System.out.println("-> Cadastro falhou: Usuário '" + usuario + "' já existe (409).");
                HttpStatus status = HttpStatus.CONFLICT;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            System.out.println("-> Cadastro BEM-SUCEDIDO: ID " + novoUsuario.getId() + ", Usuario: " + novoUsuario.getUsuario());
            HttpStatus status = HttpStatus.CREATED;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar Cadastro: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleLogout(String jsonRequest) {
        try {
            LogoutRequest req = GSON.fromJson(jsonRequest, LogoutRequest.class);

            // Valida o token para extrair o usuário
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT != null) {
                String usuario = decodedJWT.getClaim("usuario").asString();
                // Remove a session
                SessionManager.getInstance().removeSession(usuario);
            }

            System.out.println("-> Processando Logout...");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro ao desserializar Logout: " + e.getMessage());
            HttpStatus status = HttpStatus.BAD_REQUEST;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleListarProprioUsuario(String jsonRequest) {
        try {
            ListarProprioUsuarioRequest req = GSON.fromJson(jsonRequest, ListarProprioUsuarioRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> LISTAR_PROPRIO_USUARIO falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("-> LISTAR_PROPRIO_USUARIO falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Extrai o nome do usuário do token
            String usuario = decodedJWT.getClaim("usuario").asString();

            System.out.println("-> LISTAR_PROPRIO_USUARIO BEM-SUCEDIDO: Usuario: " + usuario);
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ListarProprioUsuarioResponse(status.getCode(), status.getMessage(), usuario));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar LISTAR_PROPRIO_USUARIO: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleEditarProprioUsuario(String jsonRequest) {
        try {
            EditarProprioUsuarioRequest req = GSON.fromJson(jsonRequest, EditarProprioUsuarioRequest.class);

            // Validação de campos obrigatórios
            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> EDITAR_PROPRIO_USUARIO falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.usuario == null || req.usuario.senha == null || req.usuario.senha.isEmpty()) {
                System.out.println("-> EDITAR_PROPRIO_USUARIO falhou: Senha ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Validação de formato da senha (3-20 caracteres)
            if (req.usuario.senha.length() < 3 || req.usuario.senha.length() > 20) {
                System.out.println("-> EDITAR_PROPRIO_USUARIO falhou: Senha fora do padrão (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("-> EDITAR_PROPRIO_USUARIO falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Extrai o nome do usuário do token
            String nomeUsuario = decodedJWT.getClaim("usuario").asString();

            // Atualiza a senha no repositório
            boolean sucesso = usuarioRepository.updateSenha(nomeUsuario, req.usuario.senha);

            if (!sucesso) {
                System.out.println("-> EDITAR_PROPRIO_USUARIO falhou: Usuário não encontrado (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            System.out.println("-> EDITAR_PROPRIO_USUARIO BEM-SUCEDIDO: Usuario '" + nomeUsuario + "' atualizou sua senha.");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar EDITAR_PROPRIO_USUARIO: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleExcluirProprioUsuario(String jsonRequest) {
        try {
            ExcluirProprioUsuarioRequest req = GSON.fromJson(jsonRequest, ExcluirProprioUsuarioRequest.class);

            // Validação de token obrigatório
            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> EXCLUIR_PROPRIO_USUARIO falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("-> EXCLUIR_PROPRIO_USUARIO falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Extrai o nome do usuário do token
            String nomeUsuario = decodedJWT.getClaim("usuario").asString();

            // Previne exclusão do admin
            if ("admin".equals(nomeUsuario)) {
                System.out.println("-> EXCLUIR_PROPRIO_USUARIO falhou: Não é possível excluir o administrador (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Remove o usuário do repositório
            boolean sucesso = usuarioRepository.deleteUsuario(nomeUsuario);

            if (!sucesso) {
                System.out.println("-> EXCLUIR_PROPRIO_USUARIO falhou: Usuário não encontrado (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Remove sessão
            SessionManager.getInstance().removeSession(nomeUsuario);

            System.out.println("-> EXCLUIR_PROPRIO_USUARIO BEM-SUCEDIDO: Usuario '" + nomeUsuario + "' foi excluído.");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar EXCLUIR_PROPRIO_USUARIO: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleListarUsuarios(String jsonRequest) {
        try {
            ListarUsuariosRequest req = GSON.fromJson(jsonRequest, ListarUsuariosRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> LISTAR_USUARIOS falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("-> LISTAR_USUARIOS falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Verifica se e admin
            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("-> LISTAR_USUARIOS falhou: Usuário não é admin (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Obtem lista de usuários
            List<UsuarioDTO> usuarios = usuarioRepository.getAllUsuarios();

            System.out.println("-> LISTAR_USUARIOS BEM-SUCEDIDO: " + usuarios.size() + " usuário(s) retornado(s).");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ListarUsuariosResponse(status.getCode(), status.getMessage(), usuarios));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar LISTAR_USUARIOS: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleAdminEditarUsuario(String jsonRequest) {
        try {
            AdminEditarUsuarioRequest req = GSON.fromJson(jsonRequest, AdminEditarUsuarioRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.id == null || req.id.isEmpty()) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: ID ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.usuario == null || req.usuario.senha == null || req.usuario.senha.isEmpty()) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: Senha ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: Usuário não é admin (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idUsuario;
            try {
                idUsuario = Integer.parseInt(req.id);
            } catch (NumberFormatException e) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: ID inválido (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // MUDANÇA: extrai ID do claim ao invés do subject
            int idAdmin = decodedJWT.getClaim("id").asInt();
            if (idAdmin == idUsuario) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: Admin não pode editar a si mesmo por esta operação (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.usuario.senha.length() < 3 || req.usuario.senha.length() > 20) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: Senha fora do padrão (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            boolean sucesso = usuarioRepository.updateSenhaById(idUsuario, req.usuario.senha);

            if (!sucesso) {
                System.out.println("-> ADMIN_EDITAR_USUARIO falhou: Usuário não encontrado (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            System.out.println("-> ADMIN_EDITAR_USUARIO BEM-SUCEDIDO: Senha do usuário ID " + idUsuario + " atualizada.");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar ADMIN_EDITAR_USUARIO: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleAdminExcluirUsuario(String jsonRequest) {
        try {
            AdminExcluirUsuarioRequest req = GSON.fromJson(jsonRequest, AdminExcluirUsuarioRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.id == null || req.id.isEmpty()) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: ID ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: Usuário não é admin (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idUsuario;
            try {
                idUsuario = Integer.parseInt(req.id);
            } catch (NumberFormatException e) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: ID inválido (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idAdmin = decodedJWT.getClaim("id").asInt();
            if (idAdmin == idUsuario) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: Admin não pode excluir a si mesmo (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (idUsuario == 0) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: Não é possível excluir administrador (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            Usuario usuarioExcluir = usuarioRepository.findById(idUsuario);

            if (usuarioExcluir == null) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: Usuário não encontrado (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            boolean sucesso = usuarioRepository.deleteUsuarioById(idUsuario);

            if (!sucesso) {
                System.out.println("-> ADMIN_EXCLUIR_USUARIO falhou: Erro ao excluir (500).");
                HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            SessionManager.getInstance().removeSession(usuarioExcluir.getUsuario());

            System.out.println("-> ADMIN_EXCLUIR_USUARIO BEM-SUCEDIDO: Usuário ID " + idUsuario + " excluído.");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar ADMIN_EXCLUIR_USUARIO: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleCriarFilme(String jsonRequest) {
        try {
            CriarFilmeRequest req = GSON.fromJson(jsonRequest, CriarFilmeRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> CRIAR_FILME falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme == null) {
                System.out.println("-> CRIAR_FILME falhou: Dados do filme ausentes (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> CRIAR_FILME falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("-> CRIAR_FILME falhou: Usuário não é admin (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.titulo == null || req.filme.titulo.trim().isEmpty() ||
                    req.filme.diretor == null || req.filme.diretor.trim().isEmpty() ||
                    req.filme.ano == null || req.filme.ano.trim().isEmpty() ||
                    req.filme.genero == null || req.filme.genero.isEmpty() ||
                    req.filme.sinopse == null || req.filme.sinopse.trim().isEmpty()) {

                System.out.println("-> CRIAR_FILME falhou: Campos obrigatórios ausentes (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.titulo.length() > 30) {
                System.out.println("-> CRIAR_FILME falhou: Título excede 30 caracteres (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.ano.length() != 4) {
                System.out.println("-> CRIAR_FILME falhou: Ano deve ter 4 dígitos (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.sinopse.length() > 250) {
                System.out.println("-> CRIAR_FILME falhou: Sinopse excede 250 caracteres (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            try {
                int ano = Integer.parseInt(req.filme.ano);
                if (ano < 1800 || ano > 2100) {
                    System.out.println("-> CRIAR_FILME falhou: Ano inválido (405).");
                    HttpStatus status = HttpStatus.INVALID_FIELDS;
                    return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
                }
            } catch (NumberFormatException e) {
                System.out.println("-> CRIAR_FILME falhou: Ano deve ser numérico (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (!GeneroFilme.validateGeneros(req.filme.genero)) {
                System.out.println("-> CRIAR_FILME falhou: Gênero(s) inválido(s) (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            Filme novoFilme = filmeRepository.addFilme(
                    req.filme.titulo,
                    req.filme.diretor,
                    req.filme.ano,
                    req.filme.genero,
                    req.filme.sinopse
            );

            if (novoFilme == null) {
                System.out.println("-> CRIAR_FILME falhou: Filme já existe (409).");
                HttpStatus status = HttpStatus.CONFLICT;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            System.out.println("-> CRIAR_FILME BEM-SUCEDIDO: ID " + novoFilme.getId() + ", Título: " + novoFilme.getTitulo());
            HttpStatus status = HttpStatus.CREATED;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar CRIAR_FILME: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleListarFilmes(String jsonRequest) {
        try {
            ListarFilmesRequest req =  GSON.fromJson(jsonRequest, ListarFilmesRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> LISTAR_FILMES falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("-> LISTAR_FILMES falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            System.out.println("chegou no filme");
            List<FilmeDTO> filmes = filmeRepository.getAllFilmes();

            System.out.println("-> LISTAR_FILMES BEM-SUCEDIDO: " + filmes.size() + " filme(s) retornado(s).");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ListarFilmesResponse(status.getCode(), status.getMessage(), filmes));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar LISTAR_FILMES: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleEditarFilme(String jsonRequest) {
        try {
            EditarFilmeRequest req = GSON.fromJson(jsonRequest, EditarFilmeRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> EDITAR_FILME falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme == null || req.filme.id == null || req.filme.id.isEmpty()) {
                System.out.println("-> EDITAR_FILME falhou: ID do filme ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> EDITAR_FILME falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("-> EDITAR_FILME falhou: Usuário não é admin (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idFilme;
            try {
                idFilme = Integer.parseInt(req.filme.id);
            } catch (NumberFormatException e) {
                System.out.println("-> EDITAR_FILME falhou: ID inválido (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.titulo == null || req.filme.titulo.trim().isEmpty() ||
                    req.filme.diretor == null || req.filme.diretor.trim().isEmpty() ||
                    req.filme.ano == null || req.filme.ano.trim().isEmpty() ||
                    req.filme.genero == null || req.filme.genero.isEmpty() ||
                    req.filme.sinopse == null || req.filme.sinopse.trim().isEmpty()) {

                System.out.println("-> EDITAR_FILME falhou: Campos obrigatórios ausentes (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.titulo.length() > 30 || req.filme.ano.length() != 4 ||
                    req.filme.sinopse.length() > 250) {
                System.out.println("-> EDITAR_FILME falhou: Dados fora do padrão (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            try {
                int ano = Integer.parseInt(req.filme.ano);
                if (ano < 1800 || ano > 2100) {
                    System.out.println("-> EDITAR_FILME falhou: Ano inválido (405).");
                    HttpStatus status = HttpStatus.INVALID_FIELDS;
                    return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
                }
            } catch (NumberFormatException e) {
                System.out.println("-> EDITAR_FILME falhou: Ano deve ser numérico (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (!GeneroFilme.validateGeneros(req.filme.genero)) {
                System.out.println("-> EDITAR_FILME falhou: Gênero(s) inválido(s) (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            boolean sucesso = filmeRepository.updateFilme(
                    idFilme,
                    req.filme.titulo,
                    req.filme.diretor,
                    req.filme.ano,
                    req.filme.genero,
                    req.filme.sinopse
            );

            if (!sucesso) {
                System.out.println("-> EDITAR_FILME falhou: Filme não encontrado ou conflito (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            System.out.println("-> EDITAR_FILME BEM-SUCEDIDO: ID " + idFilme);
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar EDITAR_FILME: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleExcluirFilme(String jsonRequest) {
        try {
            ExcluirFilmeRequest req = GSON.fromJson(jsonRequest, ExcluirFilmeRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> EXCLUIR_FILME falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.id == null || req.id.isEmpty()) {
                System.out.println("-> EXCLUIR_FILME falhou: ID ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> EXCLUIR_FILME falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("-> EXCLUIR_FILME falhou: Usuário não é admin (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idFilme;
            try {
                idFilme = Integer.parseInt(req.id);
            } catch (NumberFormatException e) {
                System.out.println("-> EXCLUIR_FILME falhou: ID inválido (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            boolean sucesso = filmeRepository.deleteFilme(idFilme);

            if (!sucesso) {
                System.out.println("-> EXCLUIR_FILME falhou: Filme não encontrado (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            System.out.println("-> EXCLUIR_FILME BEM-SUCEDIDO: ID " + idFilme);
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar EXCLUIR_FILME: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }
}