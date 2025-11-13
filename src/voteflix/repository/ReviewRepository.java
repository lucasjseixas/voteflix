package voteflix.repository;

import voteflix.entity.Review;
import voteflix.dto.ReviewDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReviewRepository {

    private static final String DATA_FILE = "src/data/reviews.json";
    private static final Gson GSON = new Gson();

    // Map<id, Review>
    private final ConcurrentHashMap<Integer, Review> reviews = new ConcurrentHashMap<>();

    // Contador de IDs para novas reviews
    private final AtomicInteger nextId = new AtomicInteger(1);

    public ReviewRepository() {
        try {
            Path dataDir = Paths.get("src/data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("-> Diretório 'src/data/' criado.");
            }
        } catch (IOException e) {
            System.err.println("-> ERRO ao criar diretório: " + e.getMessage());
        }

        if (carregarReviews()) {
            System.out.println("-> Reviews carregadas do JSON.");
        } else {
            System.out.println("-> Arquivo reviews.json não existe. Será criado ao adicionar reviews.");
        }
    }

    /**
     * Carrega reviews do arquivo JSON.
     */
    private boolean carregarReviews() {
        try {
            Path filePath = Paths.get(DATA_FILE);

            if (!Files.exists(filePath)) {
                return false;
            }

            String json = Files.readString(filePath);
            Type listType = new TypeToken<List<ReviewDTO>>(){}.getType();
            List<ReviewDTO> reviewsDTO = GSON.fromJson(json, listType);

            if (reviewsDTO == null || reviewsDTO.isEmpty()) {
                return false;
            }

            int maxId = 0;
            for (ReviewDTO dto : reviewsDTO) {
                int id = Integer.parseInt(dto.id);
                int idFilme = Integer.parseInt(dto.idFilme);
                int nota = Integer.parseInt(dto.nota);

                Review review = new Review(
                        id,
                        idFilme,
                        dto.nomeUsuario,
                        nota,
                        dto.titulo,
                        dto.descricao
                );

                reviews.put(id, review);

                if (id > maxId) {
                    maxId = id;
                }
            }

            nextId.set(maxId + 1);
            Review.setNextId(maxId + 1);
            System.out.println("-> " + reviews.size() + " review(s) carregada(s). Próximo ID: " + nextId.get());

            return true;

        } catch (IOException e) {
            System.err.println("Erro ao carregar reviews.json: " + e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter dados numéricos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Salva reviews no arquivo JSON.
     */
    private void salvarReviews() {
        try {
            List<ReviewDTO> reviewsDTO = new ArrayList<>();

            for (Review review : reviews.values()) {
                reviewsDTO.add(new ReviewDTO(
                        String.valueOf(review.getId()),
                        String.valueOf(review.getIdFilme()),
                        review.getNomeUsuario(),
                        String.valueOf(review.getNota()),
                        review.getTitulo(),
                        review.getDescricao()
                ));
            }

            String json = GSON.toJson(reviewsDTO);
            Path filePath = Paths.get(DATA_FILE);
            Files.writeString(filePath, json);

            System.out.println("-> Reviews salvas em " + DATA_FILE);

        } catch (IOException e) {
            System.err.println("ERRO ao salvar reviews.json: " + e.getMessage());
        }
    }

    /**
     * Adiciona uma nova review.
     * @return A review criada ou null se o usuário já tem review para este filme.
     */
    public Review addReview(int idFilme, String nomeUsuario, int nota, String titulo, String descricao) {
        // Verifica se usuário já tem review para este filme
        for (Review r : reviews.values()) {
            if (r.getIdFilme() == idFilme && r.getNomeUsuario().equals(nomeUsuario)) {
                return null; // Usuário já tem review (409)
            }
        }

        Review novaReview = new Review(idFilme, nomeUsuario, nota, titulo, descricao);
        reviews.put(novaReview.getId(), novaReview);
        salvarReviews();
        return novaReview;
    }

    /**
     * Busca review por ID.
     */
    public Review findById(int id) {
        return reviews.get(id);
    }

    /**
     * Retorna todas as reviews de um usuário.
     */
    public List<ReviewDTO> getReviewsByUsuario(String nomeUsuario) {
        return reviews.values().stream()
                .filter(r -> r.getNomeUsuario().equals(nomeUsuario))
                .map(r -> new ReviewDTO(
                        String.valueOf(r.getId()),
                        String.valueOf(r.getIdFilme()),
                        r.getNomeUsuario(),
                        String.valueOf(r.getNota()),
                        r.getTitulo(),
                        r.getDescricao()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Retorna todas as reviews de um filme.
     */
    public List<ReviewDTO> getReviewsByFilme(int idFilme) {
        return reviews.values().stream()
                .filter(r -> r.getIdFilme() == idFilme)
                .map(r -> new ReviewDTO(
                        String.valueOf(r.getId()),
                        String.valueOf(r.getIdFilme()),
                        r.getNomeUsuario(),
                        String.valueOf(r.getNota()),
                        r.getTitulo(),
                        r.getDescricao()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Atualiza uma review existente.
     */
    public boolean updateReview(int id, int nota, String titulo, String descricao) {
        Review reviewAntiga = reviews.get(id);
        if (reviewAntiga == null) {
            return false;
        }

        Review reviewAtualizada = reviewAntiga.comAtualizacao(nota, titulo, descricao);
        reviews.put(id, reviewAtualizada);
        salvarReviews();
        return true;
    }

    /**
     * Remove review pelo ID.
     */
    public boolean deleteReview(int id) {
        Review review = reviews.remove(id);
        if (review != null) {
            salvarReviews();
            return true;
        }
        return false;
    }

    /**
     * Remove todas as reviews de um filme.
     * Usado quando um filme é excluído.
     */
    public void deleteReviewsByFilme(int idFilme) {
        List<Integer> idsToRemove = reviews.values().stream()
                .filter(r -> r.getIdFilme() == idFilme)
                .map(Review::getId)
                .collect(Collectors.toList());

        for (Integer id : idsToRemove) {
            reviews.remove(id);
        }

        if (!idsToRemove.isEmpty()) {
            salvarReviews();
            System.out.println("-> " + idsToRemove.size() + " review(s) do filme ID " + idFilme + " excluída(s).");
        }
    }

    /**
     * Remove todas as reviews de um usuário.
     * Usado quando um usuário é excluído.
     */
    public void deleteReviewsByUsuario(String nomeUsuario) {
        List<Integer> idsToRemove = reviews.values().stream()
                .filter(r -> r.getNomeUsuario().equals(nomeUsuario))
                .map(Review::getId)
                .collect(Collectors.toList());

        for (Integer id : idsToRemove) {
            reviews.remove(id);
        }

        if (!idsToRemove.isEmpty()) {
            salvarReviews();
            System.out.println("-> " + idsToRemove.size() + " review(s) do usuário '" + nomeUsuario + "' excluída(s).");
        }
    }

    /**
     * Retorna a média e quantidade de avaliações de um filme.
     * Usado para recalcular a nota após exclusão de review.
     */
    public double[] getFilmeStats(int idFilme) {
        List<Review> reviewsDoFilme = reviews.values().stream()
                .filter(r -> r.getIdFilme() == idFilme)
                .collect(Collectors.toList());

        if (reviewsDoFilme.isEmpty()) {
            return new double[]{0.0, 0.0};
        }

        double soma = reviewsDoFilme.stream()
                .mapToInt(Review::getNota)
                .sum();

        double media = soma / reviewsDoFilme.size();
        return new double[]{media, reviewsDoFilme.size()};
    }
}
