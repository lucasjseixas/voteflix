package voteflix.dto.request;
import com.google.gson.annotations.SerializedName;

public class RequestBase {

    @SerializedName("operacao")
    public String operacao;

    public RequestBase(){}

    public RequestBase(String operacao) {
        this.operacao = operacao;
    }

    public String getOperacao() {
        return operacao;
    }
}
