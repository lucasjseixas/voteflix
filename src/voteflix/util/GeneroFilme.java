package voteflix.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gêneros de filmes pré-cadastrados no sistema.
 */
public enum GeneroFilme {
    ACAO("Ação"),
    AVENTURA("Aventura"),
    COMEDIA("Comédia"),
    DRAMA("Drama"),
    FANTASIA("Fantasia"),
    FICCAO_CIENTIFICA("Ficção Científica"),
    TERROR("Terror"),
    ROMANCE("Romance"),
    DOCUMENTARIO("Documentário"),
    MUSICAL("Musical"),
    ANIMACAO("Animação");

    private final String nome;

    GeneroFilme(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    /**
     * Valida se um gênero existe.
     */
    public static boolean isValid(String genero) {
        if (genero == null || genero.isEmpty()) {
            return false;
        }

        for (GeneroFilme g : values()) {
            if (g.nome.equalsIgnoreCase(genero)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retorna lista de todos os gêneros disponíveis.
     */
    public static List<String> getAllGeneros() {
        return Arrays.stream(values())
                .map(GeneroFilme::getNome)
                .collect(Collectors.toList());
    }

    /**
     * Valida uma lista de gêneros.
     */
    public static boolean validateGeneros(List<String> generos) {
        if (generos == null || generos.isEmpty()) {
            return false;
        }

        for (String genero : generos) {
            if (!isValid(genero)) {
                return false;
            }
        }
        return true;
    }
}
