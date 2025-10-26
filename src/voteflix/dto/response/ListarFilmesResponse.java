package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;
import voteflix.dto.FilmeDTO;
import java.util.List;

public class ListarFilmesResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("mensagem")
    public String mensagem;

    @SerializedName("filmes")
    public List<FilmeDTO> filmes;

    public ListarFilmesResponse(String status, String mensagem, List<FilmeDTO> filmes) {
        this.status = status;
        this.mensagem = mensagem;
        this.filmes = filmes;
    }

    public ListarFilmesResponse(String status) {
        this.status = status;
        this.filmes = null;
    }

    public ListarFilmesResponse() {}
}
