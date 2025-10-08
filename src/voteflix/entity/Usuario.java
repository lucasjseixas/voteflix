package voteflix.entity;
import java.util.concurrent.atomic.AtomicInteger;

public class Usuario {

    private static final AtomicInteger nextId = new AtomicInteger(1);

    public final int id;
    public final String usuario;
    public final String senha;
    public final String funcao;

    public Usuario(String usuario, String senha, String funcao) {
        this.id = nextId.getAndIncrement();
        this.usuario = usuario;
        this.senha = senha;
        this.funcao = funcao;
    }

    // Metodo para o usuário administrador pré-cadastrado
    public static Usuario createAdmin(String senha) {
        return new Usuario(0, "admin", senha, "admin");
    }

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