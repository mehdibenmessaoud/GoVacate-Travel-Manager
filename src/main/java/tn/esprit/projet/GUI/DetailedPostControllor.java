package tn.esprit.projet.GUI;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import tn.esprit.projet.models.Comment;
import tn.esprit.projet.models.Like;
import tn.esprit.projet.models.Post;
import tn.esprit.projet.services.CommentService;
import tn.esprit.projet.services.LikeService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DetailedPostControllor {

    @FXML private ImageView currentImageView;
    @FXML private Label lblTitre, lblMeta, lblImageCount, lblLikeCount;
    @FXML private Text txtContent;
    @FXML private Button btnPrev, btnNext, btnLike;
    @FXML private StackPane carouselContainer;
    @FXML private VBox commentsListContainer;
    @FXML private TextField commentInput;

    private final CommentService commentService = new CommentService();
    private final LikeService likeService = new LikeService();

    private int currentPostId;
    private final int currentUserId = 1; // À lier à votre session utilisateur
    private List<String> imagePaths = new ArrayList<>();
    private int currentIndex = 0;
    private Runnable onClose;

    @FXML
    public void initialize() {
        commentInput.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                handleAddComment();
            }
        });
    }

    public void setPostData(Post post, List<String> images, String auteur, String date) {
        this.currentPostId = post.getId();
        this.lblTitre.setText(post.getTitle());
        this.txtContent.setText(post.getContent());
        this.lblMeta.setText("Par " + auteur + " le " + date);

        // Carousel
        this.imagePaths = (images != null) ? images : new ArrayList<>();
        this.currentIndex = 0;
        if (imagePaths.isEmpty()) {
            carouselContainer.setVisible(false);
            carouselContainer.setManaged(false);
        } else {
            carouselContainer.setVisible(true);
            carouselContainer.setManaged(true);
            updateImage();
        }

        // Chargement des données sociales
        refreshLikes();
        refreshComments();
    }

    @FXML
    private void handleLike() {
        try {
            Like likeObj = new Like(currentPostId, currentUserId);
            if (hasUserLiked()) {
                likeService.delete(likeObj);
            } else {
                likeService.insert(likeObj);
            }
            refreshLikes();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddComment() {
        String content = commentInput.getText().trim();
        if (content.isEmpty()) return;

        try {
            Comment newComment = new Comment();
            newComment.setPostid(currentPostId);
            newComment.setUserid(currentUserId);
            newComment.setContent(content);

            commentService.insert(newComment);
            commentInput.clear();
            refreshComments();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshLikes() {
        try {
            Like filter = new Like();
            filter.setPostid(currentPostId);
            int count = likeService.selectAll(filter).size();
            lblLikeCount.setText(count + (count > 1 ? " likes" : " like"));

            if (hasUserLiked()) {
                btnLike.setText("💖 Liké");
                btnLike.setStyle("-fx-background-color: #ff4757; -fx-text-fill: white; -fx-background-radius: 20;");
            } else {
                btnLike.setText("❤️ J'aime");
                btnLike.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: white; -fx-background-radius: 20;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshComments() {
        try {
            commentsListContainer.getChildren().clear();
            Comment filter = new Comment();
            filter.setPostid(currentPostId);
            List<Comment> comments = commentService.selectAll(filter);

            for (Comment c : comments) {
                VBox commentBox = new VBox(5);
                commentBox.setPadding(new Insets(12));
                commentBox.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-background-radius: 12;");

                Label userLbl = new Label("👤 Utilisateur " + c.getUserid());
                userLbl.setStyle("-fx-text-fill: #ff9f43; -fx-font-weight: bold; -fx-font-size: 12px;");

                Label contentLbl = new Label(c.getContent());
                contentLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
                contentLbl.setWrapText(true);

                commentBox.getChildren().addAll(userLbl, contentLbl);
                commentsListContainer.getChildren().add(commentBox);
                VBox.setMargin(commentBox, new Insets(0, 0, 8, 0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean hasUserLiked() throws SQLException {
        Like filter = new Like(currentPostId, currentUserId);
        return likeService.selectAll(filter).stream()
                .anyMatch(l -> l.getUser_id() == currentUserId);
    }

    // --- CAROUSEL & NAVIGATION ---
    private void updateImage() {
        if (imagePaths.isEmpty()) return;
        Image img = new Image(imagePaths.get(currentIndex), true);
        currentImageView.setImage(img);
        lblImageCount.setText((currentIndex + 1) + " / " + imagePaths.size());
        btnPrev.setVisible(imagePaths.size() > 1);
        btnNext.setVisible(imagePaths.size() > 1);
    }

    @FXML private void showPreviousImage() {
        currentIndex = (currentIndex > 0) ? currentIndex - 1 : imagePaths.size() - 1;
        updateImage();
    }

    @FXML private void showNextImage() {
        currentIndex = (currentIndex < imagePaths.size() - 1) ? currentIndex + 1 : 0;
        updateImage();
    }

    public void setOnClose(Runnable onClose) { this.onClose = onClose; }
    @FXML private void handleClose() { if (onClose != null) onClose.run(); }
}