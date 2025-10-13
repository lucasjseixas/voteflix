package voteflix.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;

//public class SessionManager {
//    private static final SessionManager INSTANCE = new SessionManager();
//    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
//    private final List<SessionChangeListener> listeners = new CopyOnWriteArrayList<>();
//
//    // Map<username, loginTime>
//    private final ConcurrentHashMap<String, LocalDateTime> activeSessions = new ConcurrentHashMap<>();
//
//    private SessionManager() {}
//
//    public static SessionManager getInstance() {
//        return INSTANCE;
//    }
//
//    public void addSession(String username) {
//        activeSessions.put(username, LocalDateTime.now());
//
//        // Log no console
//        System.out.println("\n╔════════════════════════════════════════╗");
//        System.out.println("║  NOVO USUÁRIO CONECTADO                ║");
//        System.out.println("╠════════════════════════════════════════╣");
//        System.out.println("║  Usuario: " + String.format("%-28s", username) + "║");
//        System.out.println("║  Hora: " + String.format("%-31s", LocalDateTime.now().format(TIME_FORMATTER)) + "║");
//        System.out.println("╠════════════════════════════════════════╣");
//        System.out.println("║  USUÁRIOS ATIVOS: " + String.format("%-20d", activeSessions.size()) + "║");
//        System.out.println("╚════════════════════════════════════════╝");
//
//        listarUsuariosAtivos();
//        notifyListeners();
//    }
//
//    public void removeSession(String username) {
//        LocalDateTime loginTime = activeSessions.remove(username);
//
//        if (loginTime != null) {
//            // Log no console
//            System.out.println("\n╔════════════════════════════════════════╗");
//            System.out.println("║  USUÁRIO DESCONECTADO                  ║");
//            System.out.println("╠════════════════════════════════════════╣");
//            System.out.println("║  Usuario: " + String.format("%-28s", username) + "║");
//            System.out.println("║  Hora: " + String.format("%-31s", LocalDateTime.now().format(TIME_FORMATTER)) + "║");
//            System.out.println("╠════════════════════════════════════════╣");
//            System.out.println("║  USUÁRIOS ATIVOS: " + String.format("%-20d", activeSessions.size()) + "║");
//            System.out.println("╚════════════════════════════════════════╝");
//            if (activeSessions.size() > 0) {
//                listarUsuariosAtivos();
//                notifyListeners();
//            }
//        }
//    }
//
//    public Map<String, LocalDateTime> getActiveSessions() {
//        return new ConcurrentHashMap<>(activeSessions);
//    }
//
//    public int getActiveCount() {
//        return activeSessions.size();
//    }
//
//    public boolean isUserOnline(String username) {
//        return activeSessions.containsKey(username);
//    }
//
//    private void listarUsuariosAtivos() {
//        if (activeSessions.isEmpty()) {
//            System.out.println("Nenhum usuário ativo no momento.\n");
//            return;
//        }
//
//        System.out.println("\n--- LISTA DE USUÁRIOS ATIVOS ---");
//        int count = 1;
//        for (Map.Entry<String, LocalDateTime> entry : activeSessions.entrySet()) {
//            System.out.println(count + ". " + entry.getKey() +
//                    " (conectado às " + entry.getValue().format(TIME_FORMATTER) + ")");
//            count++;
//        }
//        System.out.println("--------------------------------\n");
//    }
//
//    // Metodo para exibir status quando solicitado
//    public void exibirStatus() {
//        System.out.println("\n╔════════════════════════════════════════╗");
//        System.out.println("║  STATUS DO SERVIDOR                    ║");
//        System.out.println("╠════════════════════════════════════════╣");
//        System.out.println("║  USUÁRIOS ATIVOS: " + String.format("%-20d", activeSessions.size()) + "║");
//        System.out.println("╚════════════════════════════════════════╝");
//
//        listarUsuariosAtivos();
//    }
//
//    public void addListener(SessionChangeListener listener) {
//        listeners.add(listener);
//    }
//
//    public void removeListener(SessionChangeListener listener) {
//        listeners.remove(listener);
//    }
//
//    private void notifyListeners() {
//        for (SessionChangeListener listener : listeners) {
//            listener.onSessionChange();
//        }
//    }
//
//}



public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Map<username, loginTime>
    private final ConcurrentHashMap<String, LocalDateTime> activeSessions = new ConcurrentHashMap<>();

    // Lista de listeners para notificar mudanças
    private final List<SessionChangeListener> listeners = new CopyOnWriteArrayList<>();

    private SessionManager() {}

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void addListener(SessionChangeListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SessionChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (SessionChangeListener listener : listeners) {
            try {
                listener.onSessionChange();
            } catch (Exception e) {
                System.err.println("Erro ao notificar listener: " + e.getMessage());
            }
        }
    }

    public void addSession(String username) {
        activeSessions.put(username, LocalDateTime.now());

        // Log no console
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  NOVO USUÁRIO CONECTADO                ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║  Usuario: " + String.format("%-28s", username) + "║");
        System.out.println("║  Hora: " + String.format("%-31s", LocalDateTime.now().format(TIME_FORMATTER)) + "║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║  USUÁRIOS ATIVOS: " + String.format("%-20d", activeSessions.size()) + "║");
        System.out.println("╚════════════════════════════════════════╝");

        listarUsuariosAtivos();

        // Notifica listeners (GUI)
        notifyListeners();
    }

    public void removeSession(String username) {
        LocalDateTime loginTime = activeSessions.remove(username);

        if (loginTime != null) {
            // Log no console
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  USUÁRIO DESCONECTADO                  ║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║  Usuario: " + String.format("%-28s", username) + "║");
            System.out.println("║  Hora: " + String.format("%-31s", LocalDateTime.now().format(TIME_FORMATTER)) + "║");
            System.out.println("╠════════════════════════════════════════╣");
            System.out.println("║  USUÁRIOS ATIVOS: " + String.format("%-20d", activeSessions.size()) + "║");
            System.out.println("╚════════════════════════════════════════╝");

            if (activeSessions.size() > 0) {
                listarUsuariosAtivos();
            }

            // Notifica listeners (GUI)
            notifyListeners();
        }
    }

    public Map<String, LocalDateTime> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    public int getActiveCount() {
        return activeSessions.size();
    }

    public boolean isUserOnline(String username) {
        return activeSessions.containsKey(username);
    }

    private void listarUsuariosAtivos() {
        if (activeSessions.isEmpty()) {
            System.out.println("Nenhum usuário ativo no momento.\n");
            return;
        }

        System.out.println("\n--- LISTA DE USUÁRIOS ATIVOS ---");
        int count = 1;
        for (Map.Entry<String, LocalDateTime> entry : activeSessions.entrySet()) {
            System.out.println(count + ". " + entry.getKey() +
                    " (conectado às " + entry.getValue().format(TIME_FORMATTER) + ")");
            count++;
        }
        System.out.println("--------------------------------\n");
    }

    // Metodo para exibir status quando solicitado
    public void exibirStatus() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║  STATUS DO SERVIDOR                    ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║  USUÁRIOS ATIVOS: " + String.format("%-20d", activeSessions.size()) + "║");
        System.out.println("╚════════════════════════════════════════╝");

        listarUsuariosAtivos();
    }
}
