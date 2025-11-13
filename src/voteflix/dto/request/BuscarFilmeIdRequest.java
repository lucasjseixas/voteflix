package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class BuscarFilmeIdRequest extends RequestBase {

    @SerializedName("id_filme")
    public String idFilme;

    @SerializedName("token")
    public String token;

    public BuscarFilmeIdRequest() {}

    public BuscarFilmeIdRequest(String idFilme, String token) {
        super("BUSCAR_FILME_ID");
        this.idFilme = idFilme;
        this.token = token;
    }
}
