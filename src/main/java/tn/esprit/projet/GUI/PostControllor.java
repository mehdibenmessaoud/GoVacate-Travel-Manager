package tn.esprit.projet.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import tn.esprit.projet.models.Post;
import tn.esprit.projet.models.Reclamation;
import tn.esprit.projet.services.BlogueService;
import tn.esprit.projet.services.ReclamationService;
import tn.esprit.projet.utils.GlassEffectUtils;
import tn.esprit.projet.utils.govacate_connect;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PostControllor {

    @FXML private StackPane rootStack;
    @FXML private ScrollPane mainScrollPane;
    @FXML private StackPane detailContainer;

    @FXML private TextField inputTitre;
    @FXML private TextArea postContent;
    @FXML private FlowPane postsContainer;
    @FXML private Label lblCompteur;
    @FXML private Label lblImageName;

    private List<String> selectedImagesPaths = new ArrayList<>();
    private final BlogueService blogueService = new BlogueService();
    private final Connection cnx = govacate_connect.getInstance().getConnection();

    @FXML
    public void initialize() {
        loadPostsFromDatabase();
    }

    @FXML
    private void importerImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner des images");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            selectedImagesPaths.clear();
            for (File file : selectedFiles) {
                selectedImagesPaths.add(file.toURI().toString());
            }
            lblImageName.setText(selectedImagesPaths.size() + " images sélectionnées");
        }
    }

    @FXML
    private void publishPost() {
        String titre = inputTitre.getText();
        String content = postContent.getText();

        if (titre.isEmpty() || content.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Champs obligatoires !").show();
            return;
        }

        try {
            Post nouveauPost = new Post(0, titre, content, "Publié", null, null, 1, 0);
            blogueService.ajouterBlogueAvecPlusieursImages(nouveauPost, selectedImagesPaths);
            loadPostsFromDatabase();

            inputTitre.clear();
            postContent.clear();
            selectedImagesPaths.clear();
            lblImageName.setText("Aucun fichier sélectionné");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPostsFromDatabase() {
        postsContainer.getChildren().clear();
        String query = "SELECT b.id_blogue, u.nom, b.title, b.content, b.createdat, GROUP_CONCAT(i.image_url) as all_images " +
                "FROM blogue b " +
                "LEFT JOIN utilisateur u ON b.userid = u.id " +
                "LEFT JOIN blogue_images i ON b.id_blogue = i.id_blogue " +
                "GROUP BY b.id_blogue ORDER BY b.createdat DESC";

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Post post = new Post();
                post.setId(rs.getInt("id_blogue"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setStatus("Publié");

                String imgsRaw = rs.getString("all_images");
                List<String> imagesList = new ArrayList<>();
                if (imgsRaw != null && !imgsRaw.isEmpty()) {
                    imagesList = Arrays.stream(imgsRaw.split(","))
                            .map(String::trim).collect(Collectors.toList());
                }

                String auteur = rs.getString("nom");
                String date = rs.getString("createdat");

                createPostCard(post, imagesList, auteur, date);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Cette méthode attend (Post, List, String, String)
    private void showPostDetail(Post post, List<String> images, String auteur, String date) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/detailedpost.fxml"));
            Parent root = loader.load();

            DetailedPostControllor controller = loader.getController();

            // FIX: Appel cohérent avec la signature attendue
            controller.setPostData(post, images, auteur, date);

            controller.setOnClose(() -> {
                rootStack.getChildren().remove(root);
                GlassEffectUtils.transitionBlur(mainScrollPane, 0);
            });

            rootStack.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showLiquidGlassPreview(Post post, List<String> images, String auteur, String date) {
        GlassEffectUtils.transitionBlur(mainScrollPane, 15);

        Pane blocker = new Pane();
        blocker.setPrefSize(rootStack.getWidth(), rootStack.getHeight());
        blocker.setStyle("-fx-background-color: rgba(0,0,0,0.1);");

        VBox glassPanel = new VBox(15);
        glassPanel.getStyleClass().add("glass-preview-panel");
        glassPanel.setMaxSize(300, 200);
        glassPanel.setAlignment(Pos.CENTER);
        glassPanel.setPadding(new Insets(20));

        Label title = new Label(post.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-alignment: center;");
        title.setWrapText(true);

        Label hint = new Label("Cliquez pour ouvrir le détail");
        hint.setStyle("-fx-text-fill: rgba(255,255,255,0.7); -fx-font-size: 12px; -fx-font-style: italic;");

        glassPanel.getChildren().addAll(title, hint);

        glassPanel.setOnMouseClicked(e -> {
            e.consume();
            rootStack.getChildren().removeAll(blocker, glassPanel);

            // CORRECTION CRITIQUE : Tu passais (String, String, List, String, String)
            // au lieu de passer l'objet 'post' complet (Post, List, String, String).
            showPostDetail(post, images, auteur, date);
        });

        blocker.setOnMouseClicked(e -> {
            rootStack.getChildren().removeAll(blocker, glassPanel);
            GlassEffectUtils.transitionBlur(mainScrollPane, 0);
        });

        rootStack.getChildren().addAll(blocker, glassPanel);
    }

    private void createPostCard(Post post, List<String> images, String auteur, String date) {
        StackPane cardFrame = new StackPane();
        cardFrame.getStyleClass().add("water-card");

        Region overlay = new Region();
        overlay.getStyleClass().add("glass-overlay");
        Region specular = new Region();
        specular.getStyleClass().add("glass-specular");

        VBox contentBox = new VBox(0);
        contentBox.getStyleClass().add("card-content-box");
        contentBox.setPadding(new Insets(10));

        Pane headerPane = new Pane();
        headerPane.setPrefHeight(180.0);
        String previewUrl = (images != null && !images.isEmpty()) ? images.get(0) : null;
        if (previewUrl != null) {
            try {
                Image img = new Image(previewUrl, 330, 180, true, true);
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(330);
                imageView.setFitHeight(180);
                applyRectangleClip(imageView, 330, 180);
                headerPane.getChildren().add(imageView);
                contentBox.getChildren().add(headerPane);
            } catch (Exception e) { System.out.println("Erreur image: " + previewUrl); }
        }

        VBox textData = new VBox(5);
        textData.setPadding(new Insets(10, 15, 15, 15));
        Label lblTitre = new Label(post.getTitle());
        lblTitre.getStyleClass().add("hero-title");
        lblTitre.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");
        Label lblDesc = new Label(post.getContent());
        lblDesc.getStyleClass().add("text-sub");
        lblDesc.setWrapText(true);
        lblDesc.setMaxHeight(45);
        lblDesc.setStyle("-fx-text-fill: rgba(255,255,255,0.7);");
        Label lblMeta = new Label("Par " + auteur + " le " + date);
        lblMeta.setStyle("-fx-text-fill: rgba(255,255,255,0.4); -fx-font-size: 10px;");
        textData.getChildren().addAll(lblTitre, lblDesc, lblMeta);
        contentBox.getChildren().add(textData);

        Button btnOptions = new Button("⋮");
        btnOptions.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 22px; -fx-cursor: hand;");
        btnOptions.setUserData(post);
        StackPane.setAlignment(btnOptions, Pos.TOP_RIGHT);
        StackPane.setMargin(btnOptions, new Insets(5, 10, 0, 0));
        btnOptions.setOnAction(e -> showOptionsMenu(btnOptions, (Post) btnOptions.getUserData()));

        cardFrame.getChildren().addAll(overlay, specular, contentBox, btnOptions);

        cardFrame.setOnMouseClicked(event -> {
            if (!(event.getTarget() instanceof Button)) {
                showLiquidGlassPreview(post, images, auteur, date);
            }
        });

        postsContainer.getChildren().add(cardFrame);
    }

    private void showOptionsMenu(Button anchor, Post post) {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("glass-context-menu");

        MenuItem updateItem = new MenuItem("✏️ Modifier");
        MenuItem deleteItem = new MenuItem("🗑️ Supprimer");
        MenuItem reportItem = new MenuItem("⚠️ Signaler");

        updateItem.setOnAction(event -> openUpdateBlogForm(post));

        deleteItem.setOnAction(event -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce blog ?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    try {
                        blogueService.delete(post);
                        loadPostsFromDatabase();
                    } catch (SQLException e) { e.printStackTrace(); }
                }
            });
        });

        reportItem.setOnAction(event -> openReclamationForm(post.getId()));

        contextMenu.getItems().addAll(updateItem, deleteItem, new SeparatorMenuItem(), reportItem);
        contextMenu.show(anchor, Side.BOTTOM, 0, 0);
    }

    private void openUpdateBlogForm(Post post) {
        GlassEffectUtils.transitionBlur(mainScrollPane, 20);

        VBox formContainer = new VBox(15);
        formContainer.getStyleClass().add("glass-panel");
        formContainer.setMaxSize(400, 350);
        formContainer.setPadding(new Insets(25));
        formContainer.setAlignment(Pos.CENTER);

        TextField titleField = new TextField(post.getTitle());
        titleField.getStyleClass().add("search-field");

        TextArea contentArea = new TextArea(post.getContent());
        contentArea.getStyleClass().add("search-field");
        contentArea.setPrefHeight(100);

        Button btnSave = new Button("Enregistrer");
        btnSave.getStyleClass().add("btn-orange-glow");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: white;");

        HBox actions = new HBox(10, btnCancel, btnSave);
        actions.setAlignment(Pos.CENTER);
        formContainer.getChildren().addAll(new Label("Modifier le Blog"), titleField, contentArea, actions);
        rootStack.getChildren().add(formContainer);

        btnCancel.setOnAction(e -> {
            rootStack.getChildren().remove(formContainer);
            GlassEffectUtils.transitionBlur(mainScrollPane, 0);
        });

        btnSave.setOnAction(e -> {
            try {
                post.setTitle(titleField.getText());
                post.setContent(contentArea.getText());
                post.setStatus("Publié");
                blogueService.update(post);
                btnCancel.fire();
                loadPostsFromDatabase();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private Button createGlassButton(String text, boolean isPrimary) {
        Button b = new Button(text);
        if(isPrimary) b.getStyleClass().add("btn-orange-glow");
        else b.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
        return b;
    }

    private void openReclamationForm(int blogId) {
        GlassEffectUtils.transitionBlur(mainScrollPane, 20);

        VBox formContainer = new VBox(15);
        formContainer.getStyleClass().add("glass-panel");
        formContainer.setMaxSize(400, 350);
        formContainer.setPadding(new Insets(25));
        formContainer.setAlignment(Pos.CENTER);

        Label head = new Label("Signaler ce contenu");
        head.getStyleClass().add("hero-title");

        TextField subjectField = new TextField();
        subjectField.setPromptText("Sujet...");
        subjectField.getStyleClass().add("search-field");

        TextArea descField = new TextArea();
        descField.setPromptText("Détails du problème...");
        descField.getStyleClass().add("search-field");
        descField.setPrefHeight(100);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button btnSubmit = new Button("Envoyer");
        btnSubmit.getStyleClass().add("btn-orange-glow");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");

        actions.getChildren().addAll(btnCancel, btnSubmit);
        formContainer.getChildren().addAll(head, subjectField, descField, actions);

        rootStack.getChildren().add(formContainer);

        btnCancel.setOnAction(e -> {
            rootStack.getChildren().remove(formContainer);
            GlassEffectUtils.transitionBlur(mainScrollPane, 0);
        });

        btnSubmit.setOnAction(e -> {
            try {
                Reclamation r = new Reclamation();
                r.setSubject(subjectField.getText());
                r.setDescription(descField.getText());
                r.setUserid(1);
                r.setPostid(blogId);
                new ReclamationService().insert(r);
                btnCancel.fire();
                new Alert(Alert.AlertType.INFORMATION, "Signalement envoyé avec succès.").show();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void applyRectangleClip(ImageView imageView, double width, double height) {
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        imageView.setClip(clip);
    }
}