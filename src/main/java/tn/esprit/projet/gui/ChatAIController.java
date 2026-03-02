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

    private final AIService aiService = new AIService();
    private PackService packService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        packService = new PackService(MyDBConnexion.getInstance().getConnection());

        addMessage("Bonjour ! Je suis votre assistant GoVacate. Je connais tous nos packs actuels. Comment puis-je vous aider ?", false);

        chatBox.heightProperty().addListener((obs, oldVal, newVal) ->
                scrollPane.setVvalue(1.0));
    }

    @FXML
    private void handleSendMessage() {
        String query = userInputField.getText().trim();
        if (query.isEmpty()) return;

        addMessage(query, true);
        userInputField.clear();

        Label loadingLabel = addMessage("L'IA reflechit...", false);

        String packsContext = packService.getPacksForAI();

        aiService.getRecommendationAsync(query, packsContext).thenAccept(response -> {
            Platform.runLater(() -> loadingLabel.setText(response));
        }).exceptionally(ex -> {
            Platform.runLater(() -> loadingLabel.setText("Desole, une erreur est survenue lors de la connexion a l'IA."));
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
