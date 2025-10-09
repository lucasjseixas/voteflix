package voteflix.entity;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Filme {

    private static final AtomicInteger nextId = new AtomicInteger(1);

    public final int id;
    public final String titulo;
    public final String diretor;
    public final String ano;
    public final List<String> genero;
    public final String sinopse;
    public final double nota;           // Média das avaliações
    public final int qtdAvaliacoes;     // Quantidade de avaliações

    // Construtor para novo filme (sem avaliações)
    public Filme(String titulo, String diretor, String ano, List<String> genero, String sinopse) {
        this.id = nextId.getAndIncrement();
        this.titulo = titulo;
        this.diretor = diretor;
        this.ano = ano;
        this.genero = genero;
        this.sinopse = sinopse;
        this.nota = 0.0;
        this.qtdAvaliacoes = 0;
    }

    // Construtor completo (para carregar do arquivo ou atualizar)
    public Filme(int id, String titulo, String diretor, String ano, List<String> genero,
                 String sinopse, double nota, int qtdAvaliacoes) {
        this.id = id;
        this.titulo = titulo;
        this.diretor = diretor;
        this.ano = ano;
        this.genero = genero;
        this.sinopse = sinopse;
        this.nota = nota;
        this.qtdAvaliacoes = qtdAvaliacoes;
    }

    // Getters
    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDiretor() { return diretor; }
    public String getAno() { return ano; }
    public List<String> getGenero() { return genero; }
    public String getSinopse() { return sinopse; }
    public double getNota() { return nota; }
    public int getQtdAvaliacoes() { return qtdAvaliacoes; }

    /**
     * Cria um novo filme com nota atualizada após uma avaliação.
     * Fórmula: novaMedia = (notaAtual * qtdAtual + novaNota) / (qtdAtual + 1)
     */
    public Filme comNovaAvaliacao(int novaNota) {
        double novaMedia = (this.nota * this.qtdAvaliacoes + novaNota) / (this.qtdAvaliacoes + 1.0);
        int novaQtd = this.qtdAvaliacoes + 1;

        return new Filme(
                this.id, this.titulo, this.diretor, this.ano,
                this.genero, this.sinopse, novaMedia, novaQtd
        );
    }

    /**
     * Retorna chave única do filme (titulo + diretor + ano).
     */
    public String getChaveUnica() {
        return (titulo + "|" + diretor + "|" + ano).toLowerCase();
    }

    /**
     * Define o próximo ID (usado ao carregar do arquivo).
     */
    public static void setNextId(int id) {
        nextId.set(id);
    }
}
