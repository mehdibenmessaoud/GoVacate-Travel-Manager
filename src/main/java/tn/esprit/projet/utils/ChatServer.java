package tn.esprit.projet.utils;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.io.*;

public class ChatServer extends WebSocketServer {
    private static final String ADMIN_HISTORY_FILE = "chat_history_admin_global.txt";

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("🚀 [Server] Nouvelle connexion : " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message == null || message.trim().isEmpty()) return;

        // 1. Logic d'effacement
        if (message.equals("[EFFACER_TOUT]")) {
            new File(ADMIN_HISTORY_FILE).delete();
            broadcast(message);
            return;
        }

        // 2. Protocol Parsing: ID|ROLE|NAME|CONTENT|TIME|DATE
        String[] parts = message.split("\\|");

        if (parts.length >= 4) {
            String userId = parts[0];
            String role = parts[1];

            // 3. SEJJEL DIMA (Hata ken l'admin tafi)
            // Save to Global History (bech l'Admin yelqah ki i-7ell)
            saveMessageToFile(ADMIN_HISTORY_FILE, message);

            // Save to Specific User History (bech Ali yelqa klemou dima)
            saveMessageToFile("chat_history_user_" + userId + ".txt", message);
        }

        // 4. RELAIS (Broadcast)
        // Hna el server yabaath lel ness el kol el connectés tawwa.
        // Ken Moez ma7loul, bech yestlem. Ken tafi, dima 3anna el file msejjla.
        broadcast(message);
        System.out.println("📩 [Server Relay]: " + message);
    }

    private synchronized void saveMessageToFile(String fileName, String message) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)))) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("❌ Erreur fichier (" + fileName + ") : " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean rem) {
        System.out.println("🔌 Connexion fermée : " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Erreur WebSocket : " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("✅ SERVER CHAT ONLINE - Port: " + getPort());
        System.out.println("📢 Attente des messages des clients...");
    }

    // --- AHAM 7AJA: MAIN METHOD ---
    // Bech t-ranni el Server wa7dou 9bal kol chay!
    public static void main(String[] args) {
        int port = 8887;
        ChatServer s = new ChatServer(port);
        s.start();
    }
}