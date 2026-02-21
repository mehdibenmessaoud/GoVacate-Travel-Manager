package tn.esprit.projet.utils;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.InetSocketAddress;
import java.io.*;

public class ChatServer extends WebSocketServer {
    private static final String HISTORY_FILE = "chat_history.txt";

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("🚀 Nouvelle connexion : " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // 1. Sauvegarde immédiate dans le fichier
        saveMessageToFile(message);
        // 2. Envoi à tout le monde
        broadcast(message);
    }

    private void saveMessageToFile(String message) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("❌ Erreur sauvegarde : " + e.getMessage());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {}

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Erreur Serveur : " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("✅ Serveur WebSocket actif sur le port : " + getPort());
    }
}