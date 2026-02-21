package tn.esprit.projet.test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.projet.utils.ChatServer;
import org.java_websocket.WebSocket;
import java.io.*;
import java.net.URL;

public class GlobalTestApp extends Application {
    private ChatServer chatServer;
    private static TextArea adminDisplayGlobal;
    private static final String HISTORY_FILE = "chat_history.txt";

    @Override
    public void start(Stage clientStage) {
        try {
            chatServer = new ChatServer(8887) {
                @Override
                public void onMessage(WebSocket conn, String message) {
                    // 1. On vérifie si c'est un ordre de suppression venant du client
                    if (message.equals("[EFFACER_TOUT]")) {
                        Platform.runLater(() -> {
                            adminDisplayGlobal.clear();
                            adminDisplayGlobal.appendText("[Système] Le Client a réinitialisé l'historique.\n");
                        });
                        // Le broadcast vers les autres clients est déjà géré par super.onMessage
                        // si ton ChatServer appelle broadcast()
                        super.onMessage(conn, message);
                    } else {
                        super.onMessage(conn, message);
                        Platform.runLater(() -> {
                            if (adminDisplayGlobal != null) {
                                adminDisplayGlobal.appendText(message + "\n");
                            }
                        });
                    }
                }
            };
            chatServer.start();

            URL clientFxml = getClass().getResource("/ClientDashboard.fxml");
            if (clientFxml != null) {
                FXMLLoader loader = new FXMLLoader(clientFxml);
                clientStage.setScene(new Scene(loader.load(), 1100, 750));
                clientStage.setTitle("GoVacate - ESPACE CLIENT");
                clientStage.show();
            }

            createAdminWindow();

            clientStage.setOnCloseRequest(e -> {
                try { if (chatServer != null) chatServer.stop(); } catch (Exception ex) {}
                System.exit(0);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createAdminWindow() {
        Stage adminStage = new Stage();
        adminDisplayGlobal = new TextArea();
        adminDisplayGlobal.setEditable(false);
        adminDisplayGlobal.setPrefHeight(400);
        adminDisplayGlobal.setStyle("-fx-control-inner-background: #1e272e; -fx-text-fill: #00ff00; -fx-font-family: 'Consolas';");

        loadHistoryIntoAdmin();

        TextField adminInput = new TextField();
        adminInput.setPromptText("Répondre au client...");

        Button clearBtn = new Button("🗑 Effacer l'historique");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");
        clearBtn.setOnAction(e -> handleClearHistory());

        adminInput.setOnAction(e -> {
            String text = adminInput.getText();
            if (!text.isEmpty()) {
                String formattedMsg = "ADMIN: " + text;
                chatServer.broadcast(formattedMsg);
                saveMessageToFile(formattedMsg);
                adminDisplayGlobal.appendText("Moi (Admin): " + text + "\n");
                adminInput.clear();
            }
        });

        VBox layout = new VBox(10, new Label("PANEL SUPPORT ADMIN (Live)"), adminDisplayGlobal, adminInput, clearBtn);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #2c3e50;");

        adminStage.setTitle("GoVacate - Admin Chat Monitor");
        adminStage.setScene(new Scene(layout, 400, 580));
        adminStage.setX(20);
        adminStage.show();
    }

    private void handleClearHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            if (file.delete()) {
                adminDisplayGlobal.clear();
                adminDisplayGlobal.appendText("[Système] Historique supprimé du disque.\n");
                chatServer.broadcast("[EFFACER_TOUT]");
            }
        } else {
            adminDisplayGlobal.clear();
            chatServer.broadcast("[EFFACER_TOUT]");
        }
    }

    private void saveMessageToFile(String message) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(HISTORY_FILE, true)))) {
            out.println(message);
        } catch (IOException e) {
            System.err.println("Erreur sauvegarde Admin: " + e.getMessage());
        }
    }

    private void loadHistoryIntoAdmin() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    adminDisplayGlobal.appendText(line + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}