package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CriarFilmeRequest extends RequestBase {

    @SerializedName("filme")
    public FilmeData filme;

    @SerializedName("token")
    public String token;

    public CriarFilmeRequest() {}

    public CriarFilmeRequest(FilmeData filme, String token) {
        super("CRIAR_FILME");
        this.filme = filme;
        this.token = token;
    }

    public static class FilmeData {
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

        public FilmeData() {}

        public FilmeData(String titulo, String diretor, String ano, List<String> genero, String sinopse) {
            this.titulo = titulo;
            this.diretor = diretor;
            this.ano = ano;
            this.genero = genero;
            this.sinopse = sinopse;
        }
    }
}