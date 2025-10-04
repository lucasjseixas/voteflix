package voteflix.repository;

import voteflix.entity.Usuario;

import java.util.concurrent.ConcurrentHashMap;

public class UsuarioRepository {

    // Simula a tabela de usuários
    private final ConcurrentHashMap<String, Usuario> usuarios = new ConcurrentHashMap<>();

    // O admin pré-cadastrado deve ser inicializado aqui
    public UsuarioRepository() {
        // Requisito: administrador pré-cadastrado
        // Usuário: "admin", Senha: "admin"
        Usuario admin = Usuario.createAdmin("admin");
        usuarios.put(admin.getUsuario(), admin);
    }

    /**
     * Tenta adicionar um novo usuário comum.
     * @return O objeto Usuario se for bem-sucedido, ou null se o usuário já existe.
     */
    public Usuario addComum(String nome, String senha) {
        // Requisito: O usuário será único pelo seu nome.
        if (usuarios.containsKey(nome)) {
            return null; // Usuário já existe (erro 409)
        }

        Usuario novoUsuario = new Usuario(nome, senha, "user");
        usuarios.put(nome, novoUsuario);
        return novoUsuario;
    }

    public Usuario findByUsuario(String nome) {
        return usuarios.get(nome);
    }

    /**
     * Atualiza a senha de um usuário existente.
     * @param nomeUsuario Nome do usuário
     * @param novaSenha Nova senha
     * @return true se atualizado com sucesso, false se usuário não encontrado
     */
    public boolean updateSenha(String nomeUsuario, String novaSenha) {
        Usuario usuario = usuarios.get(nomeUsuario);

        if (usuario == null) {
            return false; // Usuário não encontrado
        }

        // Como Usuario é imutável (campos final), precisamos criar um novo objeto
        Usuario usuarioAtualizado = new Usuario(
                usuario.getId(),
                usuario.getUsuario(),
                novaSenha,
                usuario.getFuncao()
        );

        usuarios.put(nomeUsuario, usuarioAtualizado);
        return true;
    }

    /**
     * Remove um usuário do repositório.
     * @param nomeUsuario Nome do usuário a ser removido
     * @return true se removido com sucesso, false se usuário não encontrado
     */
    public boolean deleteUsuario(String nomeUsuario) {
        Usuario usuario = usuarios.remove(nomeUsuario);
        return usuario != null; // Retorna true se removeu, false se não encontrou
    }
}
