package tn.esprit.projet.gui;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.shape.Circle;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientController {

    @FXML private ImageView bgView1, bgView2;
    @FXML private HBox topDealsContainer;
    @FXML private VBox socialFeed;
    @FXML private HBox navBar;
    @FXML private TextField searchField;
    @FXML private Button categoryBtn;
    @FXML private StackPane megaMenu;
    @FXML private Circle avatarPlaceholder;
    @FXML private ScrollPane rootScroll;
    @FXML private ImageView cartIcon;
    @FXML private Label profileName;
    @FXML private Label cartCount;
    @FXML private Button quickRestaurants;
    @FXML private VBox megaRestaurantsCol;

    private List<Image> images = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isView1Active = true;

    public void initialize() {
        loadBackgroundImages();
        startSlideshow();
        loadSQLContent();
        setupNav();
        showNavBarWithDelay();
        attachCategoryBehavior();
    }

    private void setupNav() {
        // load icons if present
        try {
            var res = getClass().getResource("/images/cart.png");
            if (res != null) cartIcon.setImage(new Image(res.toExternalForm()));
        } catch (Exception ignored) {}

        try {
            var res2 = getClass().getResource("/images/avatar_default.png");
            if (res2 != null && avatarPlaceholder != null) {
                Image av = new Image(res2.toExternalForm(), 36, 36, true, true);
                avatarPlaceholder.setFill(new ImagePattern(av));
            } else if (avatarPlaceholder != null) {
                avatarPlaceholder.setFill(Color.web("#FF8210"));
            }
        } catch (Exception ignored) { if (avatarPlaceholder != null) avatarPlaceholder.setFill(Color.web("#FF8210")); }

        try { if (cartCount != null) cartCount.setText("0"); } catch (Exception ignored) {}
        try { if (profileName != null) profileName.setText("Invité"); } catch (Exception ignored) {}
    }

    private void attachCategoryBehavior() {
        if (categoryBtn != null) categoryBtn.setOnAction(e -> toggleMegaMenu());
        if (quickRestaurants != null) quickRestaurants.setOnAction(e -> openRestaurantPage(null));
        // attach handlers to mega menu restaurant items
        if (megaRestaurantsCol != null) {
            if (megaRestaurantsCol.getChildren().size() > 1 && megaRestaurantsCol.getChildren().get(1) instanceof VBox) {
                VBox inner = (VBox) megaRestaurantsCol.getChildren().get(1);
                for (var node : inner.getChildren()) {
                    if (node instanceof Button) {
                        Button b = (Button) node;
                        b.setOnAction(ev -> openRestaurantPage(b.getText()));
                    }
                }
            }
        }
        if (rootScroll != null) {
            rootScroll.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
                if (megaMenu != null && megaMenu.isVisible()) {
                    Node target = (Node) ev.getTarget();
                    if (!isDescendant(target, megaMenu) && target != categoryBtn) {
                        hideMegaMenu();
                    }
                }
            });
        }
    }

    private boolean isDescendant(Node target, Node parent) {
        Node cur = target;
        while (cur != null) {
            if (cur == parent) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private void toggleMegaMenu() {
        if (megaMenu == null) return;
        if (megaMenu.isVisible()) hideMegaMenu(); else showMegaMenu();
    }

    private void showMegaMenu() {
        megaMenu.setManaged(true);
        megaMenu.setVisible(true);
        megaMenu.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.seconds(0.22), megaMenu);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void hideMegaMenu() {
        if (megaMenu == null) return;
        FadeTransition ft = new FadeTransition(Duration.seconds(0.18), megaMenu);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> { megaMenu.setVisible(false); megaMenu.setManaged(false); });
        ft.play();
    }

    private void openRestaurantPage(String category) {
        try {
            var res = getClass().getResource("/RestaurantPage.fxml");
            if (res == null) return;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(res);
            javafx.scene.Parent page = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof RestaurantController && category != null) {
                ((RestaurantController) ctrl).applyInitialCategory(category);
            }

            // replace the scene root so back navigation works correctly
            javafx.scene.Scene scene = (navBar != null) ? navBar.getScene() : (rootScroll != null ? rootScroll.getScene() : null);
            if (scene != null) {
                scene.setRoot(page);
            } else if (rootScroll != null) {
                rootScroll.setContent(page);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        hideMegaMenu();
    }

    private void showNavBarWithDelay() {
        if (navBar == null) return;
        navBar.setOpacity(0);
        navBar.setTranslateY(-10);
        PauseTransition wait = new PauseTransition(Duration.seconds(0.7));
        wait.setOnFinished(evt -> {
            FadeTransition fade = new FadeTransition(Duration.seconds(0.55), navBar);
            fade.setFromValue(0); fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.seconds(0.55), navBar);
            slide.setFromY(-10); slide.setToY(0);
            new ParallelTransition(fade, slide).play();
        });
        wait.play();
    }

    private void loadBackgroundImages() {
        for (int i = 1; i <= 5; i++) {
            var res = getClass().getResource("/images/background_" + i + ".png");
            if (res != null) images.add(new Image(res.toExternalForm()));
        }
        if (!images.isEmpty()) bgView1.setImage(images.get(0));
    }

    private void startSlideshow() {
        Timeline slideTimer = new Timeline(new KeyFrame(Duration.seconds(6), e -> transitionNext()));
        slideTimer.setCycleCount(Timeline.INDEFINITE);
        slideTimer.play();
    }

    private void transitionNext() {
        currentIndex = (currentIndex + 1) % images.size();
        ImageView visible = isView1Active ? bgView1 : bgView2;
        ImageView hidden = isView1Active ? bgView2 : bgView1;

        hidden.setImage(images.get(currentIndex));

        FadeTransition out = new FadeTransition(Duration.seconds(2), visible);
        out.setToValue(0);
        FadeTransition in = new FadeTransition(Duration.seconds(2), hidden);
        in.setToValue(1);

        new ParallelTransition(out, in).play();
        isView1Active = !isView1Active;
    }

    private void loadSQLContent() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/govacate", "root", "")) {
            // 1. Charger Top Packs
            ResultSet rsPack = conn.createStatement().executeQuery("SELECT name, prix FROM pack LIMIT 3");
            while(rsPack.next()) {
                topDealsContainer.getChildren().add(createCard(rsPack.getString("name"), rsPack.getDouble("prix") + "€", "PACK"));
            }

            // 2. Charger Social Feed
            String postSql = "SELECT p.*, u.nom FROM post p JOIN utilisateur u ON p.userid = u.id ORDER BY p.createdat DESC";
            ResultSet rsPost = conn.createStatement().executeQuery(postSql);
            while(rsPost.next()) {
                socialFeed.getChildren().add(createSocialPost(rsPost.getString("nom"), rsPost.getString("title"), rsPost.getString("content")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox createCard(String title, String price, String type) {
        VBox card = new VBox(12);
        card.getStyleClass().add("deal-card");
        card.setPadding(new Insets(12));

        StackPane imageWrap = new StackPane();
        imageWrap.getStyleClass().add("deal-image");
        imageWrap.setPrefSize(260, 180);

        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("deal-type");
        StackPane.setAlignment(typeLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(typeLabel, new Insets(10));

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("deal-title");

        Label priceLbl = new Label(price);
        priceLbl.getStyleClass().add("deal-price");
        imageWrap.getChildren().add(typeLabel);

        // rating + actions
        HBox meta = new HBox(10);
        meta.setAlignment(Pos.CENTER_LEFT);

        HBox stars = new HBox(4);
        stars.getChildren().addAll(new Label("★"), new Label("★"), new Label("★"), new Label("☆"));
        stars.getStyleClass().add("deal-stars");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("Ajouter");
        addBtn.getStyleClass().addAll("add-btn");

        meta.getChildren().addAll(stars, spacer, priceLbl, addBtn);

        card.getChildren().addAll(imageWrap, titleLbl, meta);
        return card;
    }

    private VBox createSocialPost(String user, String title, String content) {
        VBox post = new VBox(12);
        post.getStyleClass().add("insta-post");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Circle avatar = new Circle(22);
        try {
            var res = getClass().getResource("/images/avatar_default.png");
            if (res != null) {
                Image av = new Image(res.toExternalForm(), 44, 44, true, true);
                avatar.setFill(new ImagePattern(av));
            } else {
                avatar.setFill(Color.web("#FF8210"));
            }
        } catch (Exception ex) { avatar.setFill(Color.web("#FF8210")); }

        VBox headerText = new VBox(2);
        Label nameLbl = new Label(user);
        nameLbl.getStyleClass().add("post-username");
        Label locationLbl = new Label("— Localisation inconnue");
        locationLbl.getStyleClass().add("post-location");
        headerText.getChildren().addAll(nameLbl, locationLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label timeLbl = new Label("2h");
        timeLbl.getStyleClass().add("post-time");

        header.getChildren().addAll(avatar, headerText, spacer, timeLbl);

        Text caption = new Text((title == null ? "" : title) + "\n" + (content == null ? "" : content));
        caption.getStyleClass().add("post-caption");
        caption.setWrappingWidth(650);

        HBox actions = new HBox(18);
        actions.getStyleClass().add("post-actions");
        Label like = new Label("❤️ 124");
        Label comment = new Label("💬 21");
        actions.getChildren().addAll(like, comment);

        post.getChildren().addAll(header, caption, actions);
        return post;
    }

    // helper not used right now but useful for future image loads
    private Image tryLoad(String path, double w, double h) {
        try {
            var res = getClass().getResource(path);
            if (res != null) return new Image(res.toExternalForm(), w, h, true, true);
        } catch (Exception ignored) {}
        return null;
    }
}