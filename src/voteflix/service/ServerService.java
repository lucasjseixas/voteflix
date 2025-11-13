package voteflix.service;

import com.google.gson.Gson;
import voteflix.auth.Autenticacao;
import voteflix.dto.FilmeDTO;
import voteflix.dto.ReviewDTO;
import voteflix.dto.UsuarioDTO;
import voteflix.dto.request.*;
import voteflix.dto.response.*;
import voteflix.entity.*;
import voteflix.repository.FilmeRepository;
import voteflix.repository.ReviewRepository;
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
    private final ReviewRepository reviewRepository = new ReviewRepository();

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
                System.out.println("-> Login falhou: Senha invalida (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
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

            if("admin".equals(nomeUsuario)) {
                System.out.println("-> EDITAR_PROPRIO_USUARIO falhou: Usuário admin não pode alterar sua senha (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

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

            // Remove todas as reviews do usuário antes de exclui-lo
            reviewRepository.deleteReviewsByUsuario(nomeUsuario);

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

            // Remove todas as reviews do usuário antes de exclui-lo
            reviewRepository.deleteReviewsByUsuario(usuarioExcluir.getUsuario());

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
                    req.filme.genero == null || req.filme.genero.isEmpty()) {

                System.out.println("-> CRIAR_FILME falhou: Campos obrigatórios ausentes (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.titulo.length() < 3 || req.filme.titulo.length() > 30) {
                System.out.println("-> CRIAR_FILME falhou: Título excede 30 caracteres (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.ano.length() < 3 || req.filme.ano.length() > 4) {
                System.out.println("-> CRIAR_FILME falhou: Ano deve ter valores entre 3 e 4 dígitos (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.sinopse.length() > 250) {
                System.out.println("-> CRIAR_FILME falhou: Sinopse excede 250 caracteres (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

/*          Valores entre 1800 e 2100 removidos
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
*/

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
                    req.filme.genero == null || req.filme.genero.isEmpty()) {

                System.out.println("-> EDITAR_FILME falhou: Campos obrigatórios ausentes (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.filme.titulo.length() < 3 || req.filme.titulo.length() > 30 || req.filme.ano.length() < 3 ||req.filme.ano.length() > 4 ||
                    req.filme.sinopse.length() > 250) {
                System.out.println("-> EDITAR_FILME falhou: Dados fora do padrão (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

/*
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
*/
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

            // Remove todas as reviews do filme antes de exclui-lo
            reviewRepository.deleteReviewsByFilme(idFilme);

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

    public String handleCriarReview(String jsonRequest) {
        try {
            CriarReviewRequest req = GSON.fromJson(jsonRequest, CriarReviewRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> CRIAR_REVIEW falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.review == null) {
                System.out.println("-> CRIAR_REVIEW falhou: Dados da review ausentes (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> CRIAR_REVIEW falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if ("admin".equals(funcao)) {
                System.out.println("-> CRIAR_REVIEW falhou: Admin não pode criar reviews (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String nomeUsuario = decodedJWT.getClaim("usuario").asString();

            if (req.review.idFilme == null || req.review.idFilme.isEmpty() ||
                    req.review.nota == null || req.review.nota.isEmpty() ||
                    req.review.titulo == null || req.review.titulo.isEmpty()) {
                System.out.println("-> CRIAR_REVIEW falhou: Campos obrigatórios ausentes (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idFilme;
            int nota;
            try {
                idFilme = Integer.parseInt(req.review.idFilme);
                nota = Integer.parseInt(req.review.nota);
            } catch (NumberFormatException e) {
                System.out.println("-> CRIAR_REVIEW falhou: ID ou nota inválidos (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (nota < 1 || nota > 5) {
                System.out.println("-> CRIAR_REVIEW falhou: Nota deve estar entre 1 e 5 (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.review.descricao != null && req.review.descricao.length() > 250) {
                System.out.println("-> CRIAR_REVIEW falhou: Descrição excede 250 caracteres (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            Filme filme = filmeRepository.findById(idFilme);
            if (filme == null) {
                System.out.println("-> CRIAR_REVIEW falhou: Filme não encontrado (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String descricao = req.review.descricao != null ? req.review.descricao : "";

            Review novaReview = reviewRepository.addReview(
                    idFilme, nomeUsuario, nota, req.review.titulo, descricao
            );

            if (novaReview == null) {
                System.out.println("-> CRIAR_REVIEW falhou: Usuário já tem review para este filme (409).");
                HttpStatus status = HttpStatus.CONFLICT;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Atualiza nota do filme usando a fórmula: (M × n + x) / (n + 1)
            double notaAtual = filme.getNota();
            int qtdAtual = filme.getQtdAvaliacoes();
            double novaMedia = (notaAtual * qtdAtual + nota) / (qtdAtual + 1.0);

            filmeRepository.recalcularNota(idFilme, novaMedia, qtdAtual + 1);

            System.out.println("-> CRIAR_REVIEW BEM-SUCEDIDO: ID " + novaReview.getId() +
                    ", Filme: " + idFilme + ", Usuario: " + nomeUsuario);
            HttpStatus status = HttpStatus.CREATED;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar CRIAR_REVIEW: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleListarReviewsUsuario(String jsonRequest) {
        try {
            ListarReviewsUsuarioRequest req = GSON.fromJson(jsonRequest, ListarReviewsUsuarioRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> LISTAR_REVIEWS_USUARIO falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> LISTAR_REVIEWS_USUARIO falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String nomeUsuario = decodedJWT.getClaim("usuario").asString();
            List<ReviewDTO> reviews = reviewRepository.getReviewsByUsuario(nomeUsuario);

            System.out.println("-> LISTAR_REVIEWS_USUARIO BEM-SUCEDIDO: " + reviews.size() +
                    " review(s) do usuário '" + nomeUsuario + "'");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ListarReviewsUsuarioResponse(
                    status.getCode(), status.getMessage(), reviews
            ));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar LISTAR_REVIEWS_USUARIO: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleBuscarFilmeId(String jsonRequest) {
        try {
            BuscarFilmeIdRequest req = GSON.fromJson(jsonRequest, BuscarFilmeIdRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> BUSCAR_FILME_ID falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.idFilme == null || req.idFilme.isEmpty()) {
                System.out.println("-> BUSCAR_FILME_ID falhou: ID do filme ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> BUSCAR_FILME_ID falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idFilme;
            try {
                idFilme = Integer.parseInt(req.idFilme);
            } catch (NumberFormatException e) {
                System.out.println("-> BUSCAR_FILME_ID falhou: ID inválido (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            Filme filme = filmeRepository.findById(idFilme);
            if (filme == null) {
                System.out.println("-> BUSCAR_FILME_ID falhou: Filme não encontrado (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            FilmeDTO filmeDTO = new FilmeDTO(
                    String.valueOf(filme.getId()),
                    filme.getTitulo(),
                    filme.getDiretor(),
                    filme.getAno(),
                    filme.getGenero(),
                    filme.getSinopse(),
                    String.format("%.1f", filme.getNota()).replace(",", "."),
                    String.valueOf(filme.getQtdAvaliacoes())
            );

            List<ReviewDTO> reviews = reviewRepository.getReviewsByFilme(idFilme);

            System.out.println("-> BUSCAR_FILME_ID BEM-SUCEDIDO: Filme ID " + idFilme +
                    " com " + reviews.size() + " review(s)");
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new BuscarFilmeIdResponse(
                    status.getCode(), status.getMessage(), filmeDTO, reviews
            ));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar BUSCAR_FILME_ID: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleEditarReview(String jsonRequest) {
        try {
            EditarReviewRequest req = GSON.fromJson(jsonRequest, EditarReviewRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> EDITAR_REVIEW falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.review == null || req.review.id == null || req.review.id.isEmpty()) {
                System.out.println("-> EDITAR_REVIEW falhou: ID da review ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> EDITAR_REVIEW falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idReview;
            int nota;
            try {
                idReview = Integer.parseInt(req.review.id);
                nota = Integer.parseInt(req.review.nota);
            } catch (NumberFormatException e) {
                System.out.println("-> EDITAR_REVIEW falhou: ID ou nota inválidos (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (nota < 1 || nota > 5) {
                System.out.println("-> EDITAR_REVIEW falhou: Nota deve estar entre 1 e 5 (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.review.descricao != null && req.review.descricao.length() > 250) {
                System.out.println("-> EDITAR_REVIEW falhou: Descrição excede 250 caracteres (405).");
                HttpStatus status = HttpStatus.INVALID_FIELDS;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            Review reviewAntiga = reviewRepository.findById(idReview);
            if (reviewAntiga == null) {
                System.out.println("-> EDITAR_REVIEW falhou: Review não encontrada (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String nomeUsuario = decodedJWT.getClaim("usuario").asString();
            String funcao = decodedJWT.getClaim("funcao").asString();

            // Verifica permissão: usuário deve ser dono ou admin
            if (!reviewAntiga.getNomeUsuario().equals(nomeUsuario) && !"admin".equals(funcao)) {
                System.out.println("-> EDITAR_REVIEW falhou: Usuário não é dono da review (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String titulo = req.review.titulo != null ? req.review.titulo : reviewAntiga.getTitulo();
            String descricao = req.review.descricao != null ? req.review.descricao : reviewAntiga.getDescricao();

            boolean sucesso = reviewRepository.updateReview(idReview, nota, titulo, descricao);
            if (!sucesso) {
                System.out.println("-> EDITAR_REVIEW falhou: Erro ao atualizar (500).");
                HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Recalcula nota do filme
            int idFilme = reviewAntiga.getIdFilme();
            double[] stats = reviewRepository.getFilmeStats(idFilme);
            filmeRepository.recalcularNota(idFilme, stats[0], (int)stats[1]);

            System.out.println("-> EDITAR_REVIEW BEM-SUCEDIDO: ID " + idReview);
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar EDITAR_REVIEW: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

    public String handleExcluirReview(String jsonRequest) {
        try {
            ExcluirReviewRequest req = GSON.fromJson(jsonRequest, ExcluirReviewRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("-> EXCLUIR_REVIEW falhou: Token ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            if (req.id == null || req.id.isEmpty()) {
                System.out.println("-> EXCLUIR_REVIEW falhou: ID ausente (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);
            if (decodedJWT == null) {
                System.out.println("-> EXCLUIR_REVIEW falhou: Token inválido ou expirado (401).");
                HttpStatus status = HttpStatus.UNAUTHORIZED;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idReview;
            try {
                idReview = Integer.parseInt(req.id);
            } catch (NumberFormatException e) {
                System.out.println("-> EXCLUIR_REVIEW falhou: ID inválido (422).");
                HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            Review review = reviewRepository.findById(idReview);
            if (review == null) {
                System.out.println("-> EXCLUIR_REVIEW falhou: Review não encontrada (404).");
                HttpStatus status = HttpStatus.NOT_FOUND;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            String nomeUsuario = decodedJWT.getClaim("usuario").asString();
            String funcao = decodedJWT.getClaim("funcao").asString();

            // Verifica permissão: usuário deve ser dono ou admin
            if (!review.getNomeUsuario().equals(nomeUsuario) && !"admin".equals(funcao)) {
                System.out.println("-> EXCLUIR_REVIEW falhou: Usuário não é dono da review (403).");
                HttpStatus status = HttpStatus.FORBIDDEN;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            int idFilme = review.getIdFilme();

            boolean sucesso = reviewRepository.deleteReview(idReview);
            if (!sucesso) {
                System.out.println("-> EXCLUIR_REVIEW falhou: Erro ao excluir (500).");
                HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
            }

            // Recalcula nota do filme
            double[] stats = reviewRepository.getFilmeStats(idFilme);
            filmeRepository.recalcularNota(idFilme, stats[0], (int)stats[1]);

            System.out.println("-> EXCLUIR_REVIEW BEM-SUCEDIDO: ID " + idReview);
            HttpStatus status = HttpStatus.OK;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));

        } catch (Exception e) {
            System.err.println("-> Erro interno ao processar EXCLUIR_REVIEW: " + e.getMessage());
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return GSON.toJson(new ResponsePadrao(status.getCode(), status.getMessage()));
        }
    }

}