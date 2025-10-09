package voteflix.repository;

import voteflix.entity.Filme;
import voteflix.dto.FilmeDTO;
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

public class FilmeRepository {

    private static final String DATA_FILE = "src/data/filmes.json";
    private static final Gson GSON = new Gson();

    // Map<chaveUnica, Filme> onde chaveUnica = "titulo|diretor|ano"
    private final ConcurrentHashMap<String, Filme> filmes = new ConcurrentHashMap<>();

    // Contador de IDs para novos filmes
    private final AtomicInteger nextId = new AtomicInteger(1);

    public FilmeRepository() {
        try {
            Path dataDir = Paths.get("src/data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("-> Diretório 'src/data/' criado.");
            }
        } catch (IOException e) {
            System.err.println("-> ERRO ao criar diretório: " + e.getMessage());
        }

        if (carregarFilmes()) {
            System.out.println("-> Filmes carregados do JSON.");
        } else {
            System.out.println("-> Arquivo filmes.json não existe. Será criado ao adicionar filmes.");
        }
    }

    private double parseNota(String notaStr) {
        if (notaStr == null || notaStr.isEmpty()) {
            return 0.0;
        }

        // Substitui vírgula por ponto para parsing correto
        String notaNormalizada = notaStr.replace(",", ".");

        try {
            return Double.parseDouble(notaNormalizada);
        } catch (NumberFormatException e) {
            System.err.println("AVISO: Não foi possível converter nota '" + notaStr + "', usando 0.0");
            return 0.0;
        }
    }

    /**
     * Carrega filmes do arquivo JSON.
     */
    private boolean carregarFilmes() {
        try {
            Path filePath = Paths.get(DATA_FILE);

            if (!Files.exists(filePath)) {
                return false;
            }

            String json = Files.readString(filePath);
            Type listType = new TypeToken<List<FilmeDTO>>(){}.getType();
            List<FilmeDTO> filmesDTO = GSON.fromJson(json, listType);

            if (filmesDTO == null || filmesDTO.isEmpty()) {
                return false;
            }

            int maxId = 0;
            for (FilmeDTO dto : filmesDTO) {
                int id = Integer.parseInt(dto.id);

                // FIX: Usa o método parseNota que aceita vírgula ou ponto
                double nota = parseNota(dto.nota);

                int qtdAval = dto.qtdAvaliacoes != null && !dto.qtdAvaliacoes.isEmpty() ?
                        Integer.parseInt(dto.qtdAvaliacoes) : 0;

                Filme filme = new Filme(
                        id,
                        dto.titulo,
                        dto.diretor,
                        dto.ano,
                        dto.genero,
                        dto.sinopse,
                        nota,
                        qtdAval
                );

                filmes.put(filme.getChaveUnica(), filme);

                if (id > maxId) {
                    maxId = id;
                }
            }

            nextId.set(maxId + 1);
            Filme.setNextId(maxId + 1);
            System.out.println("-> " + filmes.size() + " filme(s) carregado(s). Próximo ID: " + nextId.get());

            return true;

        } catch (IOException e) {
            System.err.println("Erro ao carregar filmes.json: " + e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter dados numéricos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Salva filmes no arquivo JSON.
     */
    private void salvarFilmes() {
        try {
            List<FilmeDTO> filmesDTO = new ArrayList<>();

            for (Filme filme : filmes.values()) {
                filmesDTO.add(new FilmeDTO(
                        String.valueOf(filme.getId()),
                        filme.getTitulo(),
                        filme.getDiretor(),
                        filme.getAno(),
                        filme.getGenero(),
                        filme.getSinopse(),
                        String.format("%.1f", filme.getNota()).replace(",", "."), // FIX: Garante ponto
                        String.valueOf(filme.getQtdAvaliacoes())
                ));
            }

            String json = GSON.toJson(filmesDTO);
            Path filePath = Paths.get(DATA_FILE);
            Files.writeString(filePath, json);

            System.out.println("-> Filmes salvos em " + DATA_FILE);

        } catch (IOException e) {
            System.err.println("ERRO ao salvar filmes.json: " + e.getMessage());
        }
    }

    /**
     * Adiciona um novo filme.
     * @return O filme criado ou null se já existe.
     */
    public Filme addFilme(String titulo, String diretor, String ano,
                          List<String> genero, String sinopse) {
        Filme novoFilme = new Filme(titulo, diretor, ano, genero, sinopse);
        String chave = novoFilme.getChaveUnica();

        if (filmes.containsKey(chave)) {
            return null; // Filme já existe (409)
        }

        filmes.put(chave, novoFilme);
        salvarFilmes();
        return novoFilme;
    }

    /**
     * Busca filme por ID.
     */
    public Filme findById(int id) {
        for (Filme filme : filmes.values()) {
            if (filme.getId() == id) {
                return filme;
            }
        }
        return null;
    }

    /**
     * Atualiza um filme existente.
     */
    public boolean updateFilme(int id, String titulo, String diretor, String ano,
                               List<String> genero, String sinopse) {
        Filme filmeAntigo = findById(id);
        if (filmeAntigo == null) {
            return false;
        }

        // Remove filme antigo
        filmes.remove(filmeAntigo.getChaveUnica());

        // Cria filme atualizado (mantém nota e qtdAvaliacoes)
        Filme filmeAtualizado = new Filme(
                id, titulo, diretor, ano, genero, sinopse,
                filmeAntigo.getNota(),
                filmeAntigo.getQtdAvaliacoes()
        );

        // Verifica se a nova chave já existe (outro filme)
        String novaChave = filmeAtualizado.getChaveUnica();
        if (!novaChave.equals(filmeAntigo.getChaveUnica()) && filmes.containsKey(novaChave)) {
            // Conflito: outro filme já tem essa combinação
            filmes.put(filmeAntigo.getChaveUnica(), filmeAntigo); // Restaura
            return false;
        }

        filmes.put(novaChave, filmeAtualizado);
        salvarFilmes();
        return true;
    }

    /**
     * Remove filme pelo ID.
     */
    public boolean deleteFilme(int id) {
        Filme filme = findById(id);
        if (filme == null) {
            return false;
        }

        filmes.remove(filme.getChaveUnica());
        salvarFilmes();
        return true;
    }

    /**
     * Retorna todos os filmes.
     */
    public List<FilmeDTO> getAllFilmes() {
        List<FilmeDTO> lista = new ArrayList<>();
        for (Filme filme : filmes.values()) {
            lista.add(new FilmeDTO(
                    String.valueOf(filme.getId()),
                    filme.getTitulo(),
                    filme.getDiretor(),
                    filme.getAno(),
                    filme.getGenero(),
                    filme.getSinopse(),
                    String.format("%.1f", filme.getNota()).replace(",", "."), // FIX: Garante ponto
                    String.valueOf(filme.getQtdAvaliacoes())
            ));
        }
        return lista;
    }


    /**
     * Atualiza a nota do filme após uma avaliação.
     */
    public boolean atualizarNota(int idFilme, int novaNota) {
        Filme filmeAntigo = findById(idFilme);
        if (filmeAntigo == null) {
            return false;
        }

        Filme filmeAtualizado = filmeAntigo.comNovaAvaliacao(novaNota);

        filmes.put(filmeAtualizado.getChaveUnica(), filmeAtualizado);
        salvarFilmes();
        return true;
    }
}