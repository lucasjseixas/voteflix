package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;

public class ResponsePadrao {
    @SerializedName("status")
    public String status;

    @SerializedName("mensagem")
    public String mensagem;

    public ResponsePadrao(){}

    public ResponsePadrao(String status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
    }

    public String getStatus() {
        return status;
    }

    public String getMensagem() {
        return mensagem;
    }
}