package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EditarFilmeRequest extends RequestBase {

    @SerializedName("filme")
    public FilmeUpdate filme;

    @SerializedName("token")
    public String token;

    public EditarFilmeRequest() {}

    public EditarFilmeRequest(FilmeUpdate filme, String token) {
        super("EDITAR_FILME");
        this.filme = filme;
        this.token = token;
    }

    public static class FilmeUpdate {
        @SerializedName("id")
        public String id;

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

        public FilmeUpdate() {}

        public FilmeUpdate(String id, String titulo, String diretor, String ano,
                           List<String> genero, String sinopse) {
            this.id = id;
            this.titulo = titulo;
            this.diretor = diretor;
            this.ano = ano;
            this.genero = genero;
            this.sinopse = sinopse;
        }
    }
}
