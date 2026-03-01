package tn.esprit.projet.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.projet.services.AIService;
import tn.esprit.projet.services.PackService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.net.URL;
import java.util.ResourceBundle;

public class ChatAIController implements Initializable {

    @FXML private VBox chatBox;
    @FXML private TextField userInputField;
    @FXML private ScrollPane scrollPane;

    private AIService aiService = new AIService();
    private PackService packService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialisation du service avec la connexion existante
        packService = new PackService(MyDBConnexion.getInstance().getConnection());

        addMessage("Bonjour ! Je suis votre assistant GoVacate. Je connais tous nos packs actuels. Comment puis-je vous aider ?", false);

        chatBox.heightProperty().addListener((obs, oldVal, newVal) ->
                scrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleSendMessage() {
        String query = userInputField.getText().trim();
        if (query.isEmpty()) return;

        // 1. Afficher le message de l'utilisateur
        addMessage(query, true);
        userInputField.clear();

        // 2. Afficher un message de chargement temporaire
        Label loadingLabel = addMessage("L'IA réfléchit...", false);

        // 3. Récupérer les données de la base de données
        String packsContext = packService.getPacksForAI();

        // 4. Appeler l'IA de manière asynchrone (pour ne pas bloquer l'écran)
        aiService.getRecommendationAsync(query, packsContext).thenAccept(response -> {
            Platform.runLater(() -> {
                // Remplacer le message de chargement par la vraie réponse
                loadingLabel.setText(response);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                loadingLabel.setText("Désolé, une erreur est survenue lors de la connexion à l'IA.");
            });
            return null;
        });
    }

    private Label addMessage(String text, boolean isUser) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(350);

        HBox hBox = new HBox();
        if (isUser) {
            label.getStyleClass().add("message-user");
            hBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            label.getStyleClass().add("message-ai");
            hBox.setAlignment(Pos.CENTER_LEFT);
        }

        hBox.getChildren().add(label);
        chatBox.getChildren().add(hBox);
        return label;
    }

    @FXML
    private void handleClearChat() {
        chatBox.getChildren().clear();
    }
}