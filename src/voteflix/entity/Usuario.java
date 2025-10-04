package voteflix.entity;
import java.util.concurrent.atomic.AtomicInteger;

// Não precisa de anotações Gson, pois não será serializada/desserializada diretamente
public class Usuario {

    private static final AtomicInteger nextId = new AtomicInteger(1); // Thread-safe auto-incremento

    public final int id;
    public final String usuario;
    public final String senha;
    public final String funcao;

    public Usuario(String usuario, String senha, String funcao) {
        this.id = nextId.getAndIncrement(); // Operação atômica
        this.usuario = usuario;
        this.senha = senha;
        this.funcao = funcao;
    }

    // Método para o usuário administrador pré-cadastrado
    public static Usuario createAdmin(String senha) {
        // ID estático garantido para o admin
        Usuario admin = new Usuario(0, "admin", senha, "admin");
        nextId.set(1); // Reinicia o contador para que os usuários comuns comecem em 1
        return admin;
    }

    // Construtor privado para o admin pré-cadastrado
    public Usuario(int id, String usuario, String senha, String funcao) {
        this.id = id;
        this.usuario = usuario;
        this.senha = senha;
        this.funcao = funcao;
    }

    public int getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getSenha() { return senha; }
    public String getFuncao() { return funcao; }
}