package voteflix.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para serialização/desserialização de reviews em JSON.
 */
public class ReviewDTO {
    @SerializedName("id")
    public String id;

    @SerializedName("id_filme")
    public String idFilme;

    @SerializedName("nome_usuario")
    public String nomeUsuario;

    @SerializedName("nota")
    public String nota;

    @SerializedName("titulo")
    public String titulo;

    @SerializedName("descricao")
    public String descricao;

    @SerializedName("data")
    public String data;

    @SerializedName("editado")
    public String editado;

    public ReviewDTO() {}

    public ReviewDTO(String id, String idFilme, String nomeUsuario, String nota,
                     String titulo, String descricao, String data, String editado) {
        this.id = id;
        this.idFilme = idFilme;
        this.nomeUsuario = nomeUsuario;
        this.nota = nota;
        this.titulo = titulo;
        this.descricao = descricao;
        this.data = data;
        this.editado = editado;
    }
}