package voteflix.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO para serialização/desserialização de filmes em JSON.
 * Usado tanto para enviar dados ao cliente quanto salvar em arquivo.
 */
public class FilmeDTO {
    @SerializedName("id")
    public String id;  // String no JSON, int na entidade

    @SerializedName("titulo")
    public String titulo;

    @SerializedName("diretor")
    public String diretor;

    @SerializedName("ano")
    public String ano;

    @SerializedName("genero")
    public List<String> genero;

    @SerializedName("sinopse")
    public String sinopse;

    @SerializedName("nota")
    public String nota;  // String no JSON, double na entidade

    @SerializedName("qtd_avaliacoes")
    public String qtdAvaliacoes;  // String no JSON, int na entidade

    public FilmeDTO() {}

    public FilmeDTO(String id, String titulo, String diretor, String ano,
                    List<String> genero, String sinopse, String nota, String qtdAvaliacoes) {
        this.id = id;
        this.titulo = titulo;
        this.diretor = diretor;
        this.ano = ano;
        this.genero = genero;
        this.sinopse = sinopse;
        this.nota = nota;
        this.qtdAvaliacoes = qtdAvaliacoes;
    }
}