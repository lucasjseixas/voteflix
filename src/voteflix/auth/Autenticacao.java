package voteflix.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;

public class Autenticacao {

    private static final String SECRET_KEY = "";

    public String generateToken(int id, String usuario, String funcao) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            return JWT.create()
                    .withIssuer("auth0")
                    //.withSubject(String.valueOf(id)) // checar se utilizaremos o withSubject ou um claim
                    .withClaim("id", id)
                    .withClaim("usuario", usuario)
                    .withClaim("funcao", funcao)
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + 3600000)) // 1 hour expiration
                    .sign(algorithm);

        } catch (JWTCreationException exception) {
            return null;
        }
    }

    /**
     * Valida e decodifica um token JWT.
     *
     * @param token O token JWT a ser validado
     * @return DecodedJWT se válido, null se inválido ou expirado
     */
    public DecodedJWT validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build();
            return verifier.verify(token);
        } catch (JWTVerificationException exception) {
            // Token inválido ou expirado
            return null;
        }
    }
}