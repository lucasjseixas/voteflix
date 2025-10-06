package voteflix.repository;

import voteflix.dto.request.AdminExcluirUsuarioRequest;
import voteflix.entity.Usuario;
import voteflix.dto.UsuarioDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;

public class UsuarioRepository {

    private static final String DATA_FILE = "src/data/usuarios.json";
    private static final Gson GSON = new Gson();

    // Simula a tabela de usuários
    private final ConcurrentHashMap<String, Usuario> usuarios = new ConcurrentHashMap<>();

    // Contador de IDs para novos usuários
    private final AtomicInteger nextId = new AtomicInteger(1);

//    public UsuarioRepository() {
//        // Cria diretório data/ se não existir
//        try {
//            Path dataDir = Paths.get("data");
//            if (!Files.exists(dataDir)) {
//                Files.createDirectories(dataDir);
//                System.out.println("-> Diretório 'data/' criado.");
//            }
//        } catch (IOException e) {
//            System.err.println("Erro ao criar diretório data/: " + e.getMessage());
//        }
//
//        // Tenta carregar usuários do arquivo
//        if (carregarUsuarios()) {
//            System.out.println("-> Usuários carregados do arquivo JSON.");
//        } else {
//            // Se não conseguiu carregar, cria admin padrão
//            Usuario admin = Usuario.createAdmin("admin");
//            usuarios.put(admin.getUsuario(), admin);
//            System.out.println("-> Administrador pré-cadastrado: ID 0, Usuario: admin");
//            salvarUsuarios();
//        }
//    }

    public UsuarioRepository() {
        try {
            Path dataDir = Paths.get("src/data");
            System.out.println("-> Caminho do diretório: " + dataDir.toAbsolutePath());

            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("-> Diretório criado.");
            } else {
                System.out.println("-> Diretório já existe.");
            }
        } catch (IOException e) {
            System.err.println("-> ERRO ao criar diretório: " + e.getMessage());
        }

        if (carregarUsuarios()) {
            System.out.println("-> Usuários carregados do JSON.");
        } else {
            System.out.println("-> Arquivo não existe. Criando admin...");
            Usuario admin = Usuario.createAdmin("admin");
            usuarios.put(admin.getUsuario(), admin);
            System.out.println("-> Admin criado. Chamando salvarUsuarios()...");
            salvarUsuarios();
            System.out.println("-> salvarUsuarios() concluído.");
        }
    }

    /**
     * Carrega usuários do arquivo JSON.
     * @return true se carregou com sucesso, false caso contrário
     */
    private boolean carregarUsuarios() {
        try {
            Path filePath = Paths.get(DATA_FILE);

            if (!Files.exists(filePath)) {
                System.out.println("-> Arquivo usuarios.json não encontrado. Criando novo.");
                return false;
            }

            String json = Files.readString(filePath);
            Type listType = new TypeToken<List<UsuarioDTO>>(){}.getType();
            List<UsuarioDTO> usuariosDTO = GSON.fromJson(json, listType);

            if (usuariosDTO == null || usuariosDTO.isEmpty()) {
                return false;
            }

            int maxId = 0;
            for (UsuarioDTO dto : usuariosDTO) {
                System.out.println("   -> Carregando usuario: ID " + dto.id + ", Usuario: " + dto.usuario);

                // Determina a função baseado no ID
                String funcao = (dto.id == 0) ? "admin" : "user";

                // Recria o usuário completo
                Usuario usuario = new Usuario(dto.id, dto.usuario, dto.senha, funcao);
                usuarios.put(usuario.getUsuario(), usuario);

                if (dto.id > maxId) {
                    maxId = dto.id;
                }
            }

            // Ajusta o contador de IDs para começar após o maior ID encontrado
            nextId.set(maxId + 1);
            System.out.println("-> " + usuarios.size() + " usuário(s) carregado(s). Próximo ID: " + nextId.get());

            return true;

        } catch (IOException e) {
            System.err.println("Erro ao carregar usuarios.json: " + e.getMessage());
            return false;
        }
    }

    /**
     * Salva usuários no arquivo JSON (id, usuario e senha).
     */
    private void salvarUsuarios() {
        try {
            // Converte HashMap para lista de DTOs
            List<UsuarioDTO> usuariosDTO = new ArrayList<>();

            for (Usuario usuario : usuarios.values()) {
                usuariosDTO.add(new UsuarioDTO(
                        usuario.getId(),
                        usuario.getUsuario(),
                        usuario.getSenha()
                ));
            }

            // Serializa e salva
            String json = GSON.toJson(usuariosDTO);
            Path filePath = Paths.get(DATA_FILE);
            Files.writeString(filePath, json);

            System.out.println("-> Usuários salvos em " + DATA_FILE);

        } catch (IOException e) {
            System.err.println("ERRO ao salvar usuarios.json: " + e.getMessage());
        }
    }

    /**
     * Tenta adicionar um novo usuário comum.
     * @return O objeto Usuario se for bem-sucedido, ou null se o usuário já existe.
     */
    public Usuario addComum(String nome, String senha) {
        if (usuarios.containsKey(nome)) {
            return null; // Usuário já existe (erro 409)
        }

        // Usa o contador thread-safe para gerar ID
        int id = nextId.getAndIncrement();
        Usuario novoUsuario = new Usuario(id, nome, senha, "user");
        usuarios.put(nome, novoUsuario);

        // Salva no arquivo
        salvarUsuarios();

        return novoUsuario;
    }

    public Usuario findByUsuario(String nome) {
        return usuarios.get(nome);
    }

    /**
     * Atualiza a senha de um usuário existente.
     */
    public boolean updateSenha(String nomeUsuario, String novaSenha) {
        Usuario usuario = usuarios.get(nomeUsuario);

        if (usuario == null) {
            return false;
        }

        Usuario usuarioAtualizado = new Usuario(
                usuario.getId(),
                usuario.getUsuario(),
                novaSenha,
                usuario.getFuncao()
        );

        usuarios.put(nomeUsuario, usuarioAtualizado);

        // Salva no arquivo (agora inclui senha)
        salvarUsuarios();

        return true;
    }

    /**
     * Remove um usuário do repositório.
     */
    public boolean deleteUsuario(String nomeUsuario) {
        Usuario usuario = usuarios.remove(nomeUsuario);

        if (usuario != null) {
            // Salva no arquivo
            salvarUsuarios();
            return true;
        }

        return false;
    }

    /**
     * Retorna todos os usuários (sem senha).
     */
    public List<UsuarioDTO> getAllUsuarios() {
        List<UsuarioDTO> lista = new ArrayList<>();
        for (Usuario usuario : usuarios.values()) {
            // Sem senha por segurança
            lista.add(new UsuarioDTO(usuario.getId(), usuario.getUsuario(), null));
        }
        return lista;
    }

    /**
     * Busca usuário por ID.
     */
    public Usuario findById(int id) {
        for (Usuario usuario : usuarios.values()) {
            if (usuario.getId() == id) {
                return usuario;
            }
        }
        return null;
    }

    /**
     * Atualiza senha de um usuário pelo ID.
     */
    public boolean updateSenhaById(int id, String novaSenha) {
        Usuario usuario = findById(id);

        if (usuario == null) {
            return false;
        }

        Usuario usuarioAtualizado = new Usuario(
                usuario.getId(),
                usuario.getUsuario(),
                novaSenha,
                usuario.getFuncao()
        );

        usuarios.put(usuario.getUsuario(), usuarioAtualizado);
        salvarUsuarios();
        return true;
    }

    /**
     * Remove usuário pelo ID.
     */
    public boolean deleteUsuarioById(int id) {
        Usuario usuario = findById(id);

        if (usuario == null) {
            return false;
        }

        usuarios.remove(usuario.getUsuario());
        salvarUsuarios();
        return true;
    }

}
