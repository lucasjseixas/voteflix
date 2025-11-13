package voteflix.entity;

import java.util.concurrent.atomic.AtomicInteger;

public class Review {
    private static final AtomicInteger nextId = new AtomicInteger(1);

    public final int id;
    public final int idFilme;
    public final String nomeUsuario;
    public final int nota;  // 1-5
    public final String titulo;
    public final String descricao;

    // Construtor para nova review
    public Review(int idFilme, String nomeUsuario, int nota, String titulo, String descricao) {
        this.id = nextId.getAndIncrement();
        this.idFilme = idFilme;
        this.nomeUsuario = nomeUsuario;
        this.nota = nota;
        this.titulo = titulo;
        this.descricao = descricao;
    }

    // Construtor completo (para carregar do arquivo)
    public Review(int id, int idFilme, String nomeUsuario, int nota,
                  String titulo, String descricao) {
        this.id = id;
        this.idFilme = idFilme;
        this.nomeUsuario = nomeUsuario;
        this.nota = nota;
        this.titulo = titulo;
        this.descricao = descricao;
    }

    // Construtor para atualizar review (mantém ID, filme, usuário e data)
    public Review(int id, int idFilme, String nomeUsuario, int nota,
                  String titulo, String descricao, boolean isUpdate) {
        this.id = id;
        this.idFilme = idFilme;
        this.nomeUsuario = nomeUsuario;
        this.nota = nota;
        this.titulo = titulo;
        this.descricao = descricao;
    }

    // Getters
    public int getId() { return id; }
    public int getIdFilme() { return idFilme; }
    public String getNomeUsuario() { return nomeUsuario; }
    public int getNota() { return nota; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }

    /**
     * Define o próximo ID (usado ao carregar do arquivo).
     */
    public static void setNextId(int id) {
        nextId.set(id);
    }

    /**
     * Cria uma review atualizada (para edição).
     */
    public Review comAtualizacao(int novaNota, String novoTitulo, String novaDescricao) {
        return new Review(
                this.id,
                this.idFilme,
                this.nomeUsuario,
                novaNota,
                novoTitulo,
                novaDescricao,
                true
        );
    }
}