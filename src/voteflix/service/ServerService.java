package voteflix.service;

import com.google.gson.Gson;
import voteflix.auth.Autenticacao;
import voteflix.dto.UsuarioDTO;
import voteflix.dto.request.*;
import voteflix.dto.response.ListarProprioUsuarioResponse;
import voteflix.dto.response.ListarUsuariosResponse;
import voteflix.dto.response.LoginResponse;
import voteflix.dto.response.ResponsePadrao;
import voteflix.entity.*;
import voteflix.repository.UsuarioRepository;
import voteflix.session.SessionManager;

import java.util.List;

public class ServerService {

    private final Gson GSON = new Gson();
    private final UsuarioRepository usuarioRepository = new UsuarioRepository();
    private final Autenticacao AUTH = new Autenticacao();

    /**
     * Gera uma resposta JSON simples de status.
     * @param status O código de status a ser retornado (ex: "201", "400").
     * @return A string JSON serializada.
     */
    public String createStatusResponse(String status) {
        return GSON.toJson(new ResponsePadrao(status));
    }

    /**
     * Manipula o LOGIN: Tenta desserializar o objeto completo e retorna a AuthResponse.
     * @param jsonRequest A string JSON completa da requisição.
     * @return O JSON de AuthResponse ou o JSON de erro (ResponsePadrao).
     */
    public String handleLogin(String jsonRequest) {
        try {
            LoginRequest req = GSON.fromJson(jsonRequest, LoginRequest.class);
            String usuarioNome = req.usuario;
            String senha = req.senha;

            // 1. BUSCA O USUÁRIO NO REPOSITÓRIO
            Usuario usuario = usuarioRepository.findByUsuario(usuarioNome);

            // 2. VALIDAÇÃO DE CREDENCIAIS
            if (usuario == null || !usuario.getSenha().equals(senha)) {
                System.out.println("  -> Login falhou: Credenciais inválidas (401).");
                // 401: Unauthorized: invalid token (usado para credenciais inválidas)
                return createStatusResponse("401");
            }

            // Geração do token
            String jwtToken = AUTH.generateToken(usuario.getId(), usuario.getUsuario(), usuario.getFuncao());
            System.out.println("  -> Login BEM-SUCEDIDO: JWT: " + jwtToken.substring(0, 20) + "...");

            // Adiciona sessão
            SessionManager.getInstance().addSession(usuarioNome);

            // Usa o construtor de sucesso (status e token)
            return GSON.toJson(new LoginResponse("200", jwtToken));

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar Login: " + e.getMessage());
            return GSON.toJson(new LoginResponse("400")); // Bad Request
        }
    }

    /**
     * Manipula o CADASTRO: Tenta desserializar o objeto completo e retorna o status.
     * @param jsonRequest A string JSON completa da requisição.
     * @return O JSON de ResponsePadrao (201 ou 4xx).
     */
    public String handleCadastrarUsuario(String jsonRequest) {
        try {
            CadastrarUsuarioRequest req = GSON.fromJson(jsonRequest, CadastrarUsuarioRequest.class);

            String usuario = req.usuario;
            String senha = req.senha;

            // --- VALIDAÇÃO DE TAMANHO E FORMATO (Requisito) ---
            // O Cliente já valida, mas o Servidor DEVE repetir a validação.
            if (usuario == null || senha == null ||
                    usuario.length() < 3 || usuario.length() > 20 ||
                    senha.length() < 3 || senha.length() > 20) {

                System.out.println("  -> Cadastro falhou: Dados fora do padrão (422).");
                return createStatusResponse("422"); // Unprocessable Entity
            }

            // --- VALIDAÇÃO DE UNICIDADE (Lógica de Negócio) ---
            Usuario novoUsuario = usuarioRepository.addComum(req.usuario, req.senha);

            if (novoUsuario == null) {
                // Usuário já existe
                System.out.println("  -> Cadastro falhou: Usuário '" + req.usuario + "' já existe (409).");
                return createStatusResponse("409"); // Already exists
            }

            System.out.println("  -> Cadastro BEM-SUCEDIDO: ID " + novoUsuario.getId() + ", Usuario: " + novoUsuario.getUsuario());
            return createStatusResponse("201"); // Created

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar Cadastro: " + e.getMessage());
            // Qualquer outra falha (parsing, I/O)
            return createStatusResponse("500"); // Internal Server Error (se for falha de lógica/sistema)
        }
    }

    /**
     * Manipula o LOGOUT: Tenta desserializar o objeto completo e retorna o status.
     * @param jsonRequest A string JSON completa da requisição.
     * @return O JSON de ResponsePadrao (200 ou 4xx).
     */
    public String handleLogout(String jsonRequest) {
        try {
            LogoutRequest req = GSON.fromJson(jsonRequest, LogoutRequest.class);

            // Valida o token para extrair o usuário
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT != null) {
                String usuario = decodedJWT.getClaim("usuario").asString();
                // *** REMOVER SESSÃO ***
                SessionManager.getInstance().removeSession(usuario);
            }

            System.out.println("  -> Processando Logout...");
            return createStatusResponse("200");

        } catch (Exception e) {
            System.err.println("  -> Erro ao desserializar Logout: " + e.getMessage());
            return createStatusResponse("400");
        }
    }

    /**
     * Manipula LISTAR_PROPRIO_USUARIO: Valida o token e retorna os dados do usuário.
     * * @param jsonRequest A string JSON completa da requisição.
     * @return O JSON de ListarProprioUsuarioResponse ou ResponsePadrao (erro).
     */
    public String handleListarProprioUsuario(String jsonRequest) {
        try {
            ListarProprioUsuarioRequest req = GSON.fromJson(jsonRequest, ListarProprioUsuarioRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("  -> LISTAR_PROPRIO_USUARIO falhou: Token ausente (400).");
                return createStatusResponse("400"); // Bad Request
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("  -> LISTAR_PROPRIO_USUARIO falhou: Token inválido ou expirado (401).");
                return createStatusResponse("401"); // Unauthorized
            }

            // Extrai o nome do usuário do token
            String usuario = decodedJWT.getClaim("usuario").asString();

            System.out.println("  -> LISTAR_PROPRIO_USUARIO BEM-SUCEDIDO: Usuario: " + usuario);
            return GSON.toJson(new ListarProprioUsuarioResponse("200", usuario));

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar LISTAR_PROPRIO_USUARIO: " + e.getMessage());
            return createStatusResponse("500"); // Internal Server Error
        }
    }

    /**
    * Manipula EDITAR_PROPRIO_USUARIO: Valida o token e atualiza a senha do usuário.
    * @param jsonRequest A string JSON completa da requisição.
    * @return O JSON de ResponsePadrao.
    */
    public String handleEditarProprioUsuario(String jsonRequest) {
        try {
            EditarProprioUsuarioRequest req = GSON.fromJson(jsonRequest, EditarProprioUsuarioRequest.class);

            // Validação de campos obrigatórios
            if (req.token == null || req.token.isEmpty()) {
                System.out.println("  -> EDITAR_PROPRIO_USUARIO falhou: Token ausente (400).");
                return createStatusResponse("400"); // Bad Request
            }

            if (req.usuario == null || req.usuario.senha == null || req.usuario.senha.isEmpty()) {
                System.out.println("  -> EDITAR_PROPRIO_USUARIO falhou: Senha ausente (400).");
                return createStatusResponse("400"); // Bad Request
            }

            // Validação de formato da senha (3-20 caracteres)
            if (req.usuario.senha.length() < 3 || req.usuario.senha.length() > 20) {
                System.out.println("  -> EDITAR_PROPRIO_USUARIO falhou: Senha fora do padrão (422).");
                return createStatusResponse("422"); // Unprocessable Entity
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("  -> EDITAR_PROPRIO_USUARIO falhou: Token inválido ou expirado (401).");
                return createStatusResponse("401"); // Unauthorized
            }

            // Extrai o nome do usuário do token
            String nomeUsuario = decodedJWT.getClaim("usuario").asString();

            // Atualiza a senha no repositório
            boolean sucesso = usuarioRepository.updateSenha(nomeUsuario, req.usuario.senha);

            if (!sucesso) {
                System.out.println("  -> EDITAR_PROPRIO_USUARIO falhou: Usuário não encontrado (404).");
                return createStatusResponse("404"); // Not Found (improvável, mas possível)
            }

            System.out.println("  -> EDITAR_PROPRIO_USUARIO BEM-SUCEDIDO: Usuario '" + nomeUsuario + "' atualizou sua senha.");
            return createStatusResponse("200"); // Success

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar EDITAR_PROPRIO_USUARIO: " + e.getMessage());
            return createStatusResponse("500"); // Internal Server Error
        }
    }

    /**
     * Manipula EXCLUIR_PROPRIO_USUARIO: Valida o token e remove o usuário.
     * @param jsonRequest A string JSON completa da requisição.
     * @return O JSON de ResponsePadrao.
     */
    public String handleExcluirProprioUsuario(String jsonRequest) {
        try {
            ExcluirProprioUsuarioRequest req = GSON.fromJson(jsonRequest, ExcluirProprioUsuarioRequest.class);

            // Validação de token obrigatório
            if (req.token == null || req.token.isEmpty()) {
                System.out.println("  -> EXCLUIR_PROPRIO_USUARIO falhou: Token ausente (400).");
                return createStatusResponse("400"); // Bad Request
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("  -> EXCLUIR_PROPRIO_USUARIO falhou: Token inválido ou expirado (401).");
                return createStatusResponse("401"); // Unauthorized
            }

            // Extrai o nome do usuário do token
            String nomeUsuario = decodedJWT.getClaim("usuario").asString();

            // Previne exclusão do admin
            if ("admin".equals(nomeUsuario)) {
                System.out.println("  -> EXCLUIR_PROPRIO_USUARIO falhou: Não é possível excluir o administrador (403).");
                return createStatusResponse("403"); // Forbidden
            }

            // Remove o usuário do repositório
            boolean sucesso = usuarioRepository.deleteUsuario(nomeUsuario);

            if (!sucesso) {
                System.out.println("  -> EXCLUIR_PROPRIO_USUARIO falhou: Usuário não encontrado (404).");
                return createStatusResponse("404"); // Not Found
            }

            // Remove sessão
            SessionManager.getInstance().removeSession(nomeUsuario);

            System.out.println("  -> EXCLUIR_PROPRIO_USUARIO BEM-SUCEDIDO: Usuario '" + nomeUsuario + "' foi excluído.");
            return createStatusResponse("200"); // Success

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar EXCLUIR_PROPRIO_USUARIO: " + e.getMessage());
            return createStatusResponse("500"); // Internal Server Error
        }
    }

    /**
     * Manipula LISTAR_USUARIOS: Apenas admin pode listar todos os usuários.
     */
    public String handleListarUsuarios(String jsonRequest) {
        try {
            ListarUsuariosRequest req = GSON.fromJson(jsonRequest, ListarUsuariosRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("  -> LISTAR_USUARIOS falhou: Token ausente (400).");
                return createStatusResponse("400");
            }

            // Valida o token
            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("  -> LISTAR_USUARIOS falhou: Token inválido ou expirado (401).");
                return createStatusResponse("401");
            }

            // Verifica se é admin
            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("  -> LISTAR_USUARIOS falhou: Usuário não é admin (403).");
                return createStatusResponse("403");
            }

            // Obtém lista de usuários
            List<UsuarioDTO> usuarios = usuarioRepository.getAllUsuarios();

            System.out.println("  -> LISTAR_USUARIOS BEM-SUCEDIDO: " + usuarios.size() + " usuário(s) retornado(s).");
            return GSON.toJson(new ListarUsuariosResponse("200", usuarios));

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar LISTAR_USUARIOS: " + e.getMessage());
            return createStatusResponse("500");
        }
    }

    public String handleAdminEditarUsuario(String jsonRequest) {
        try {
            AdminEditarUsuarioRequest req = GSON.fromJson(jsonRequest, AdminEditarUsuarioRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: Token ausente (400).");
                return createStatusResponse("400");
            }

            if (req.id == null || req.id.isEmpty()) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: ID ausente (400).");
                return createStatusResponse("400");
            }

            if (req.usuario == null || req.usuario.senha == null || req.usuario.senha.isEmpty()) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: Senha ausente (400).");
                return createStatusResponse("400");
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: Token inválido ou expirado (401).");
                return createStatusResponse("401");
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: Usuário não é admin (403).");
                return createStatusResponse("403");
            }

            int idUsuario;
            try {
                idUsuario = Integer.parseInt(req.id);
            } catch (NumberFormatException e) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: ID inválido (400).");
                return createStatusResponse("400");
            }

            // MUDANÇA: extrai ID do claim ao invés do subject
            int idAdmin = decodedJWT.getClaim("id").asInt();
            if (idAdmin == idUsuario) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: Admin não pode editar a si mesmo por esta operação (403).");
                return createStatusResponse("403");
            }

            if (req.usuario.senha.length() < 3 || req.usuario.senha.length() > 20) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: Senha fora do padrão (422).");
                return createStatusResponse("422");
            }

            boolean sucesso = usuarioRepository.updateSenhaById(idUsuario, req.usuario.senha);

            if (!sucesso) {
                System.out.println("  -> ADMIN_EDITAR_USUARIO falhou: Usuário não encontrado (404).");
                return createStatusResponse("404");
            }

            System.out.println("  -> ADMIN_EDITAR_USUARIO BEM-SUCEDIDO: Senha do usuário ID " + idUsuario + " atualizada.");
            return createStatusResponse("200");

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar ADMIN_EDITAR_USUARIO: " + e.getMessage());
            return createStatusResponse("500");
        }
    }

    public String handleAdminExcluirUsuario(String jsonRequest) {
        try {
            AdminExcluirUsuarioRequest req = GSON.fromJson(jsonRequest, AdminExcluirUsuarioRequest.class);

            if (req.token == null || req.token.isEmpty()) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: Token ausente (400).");
                return createStatusResponse("400");
            }

            if (req.id == null || req.id.isEmpty()) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: ID ausente (400).");
                return createStatusResponse("400");
            }

            com.auth0.jwt.interfaces.DecodedJWT decodedJWT = AUTH.validateToken(req.token);

            if (decodedJWT == null) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: Token inválido ou expirado (401).");
                return createStatusResponse("401");
            }

            String funcao = decodedJWT.getClaim("funcao").asString();
            if (!"admin".equals(funcao)) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: Usuário não é admin (403).");
                return createStatusResponse("403");
            }

            int idUsuario;
            try {
                idUsuario = Integer.parseInt(req.id);
            } catch (NumberFormatException e) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: ID inválido (400).");
                return createStatusResponse("400");
            }

            // MUDANÇA: extrai ID do claim ao invés do subject
            int idAdmin = decodedJWT.getClaim("id").asInt();
            if (idAdmin == idUsuario) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: Admin não pode excluir a si mesmo (403).");
                return createStatusResponse("403");
            }

            if (idUsuario == 0) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: Não é possível excluir administrador (403).");
                return createStatusResponse("403");
            }

            Usuario usuarioExcluir = usuarioRepository.findById(idUsuario);

            if (usuarioExcluir == null) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: Usuário não encontrado (404).");
                return createStatusResponse("404");
            }

            boolean sucesso = usuarioRepository.deleteUsuarioById(idUsuario);

            if (!sucesso) {
                System.out.println("  -> ADMIN_EXCLUIR_USUARIO falhou: Erro ao excluir (500).");
                return createStatusResponse("500");
            }

            SessionManager.getInstance().removeSession(usuarioExcluir.getUsuario());

            System.out.println("  -> ADMIN_EXCLUIR_USUARIO BEM-SUCEDIDO: Usuário ID " + idUsuario + " excluído.");
            return createStatusResponse("200");

        } catch (Exception e) {
            System.err.println("  -> Erro interno ao processar ADMIN_EXCLUIR_USUARIO: " + e.getMessage());
            return createStatusResponse("500");
        }
    }

}

