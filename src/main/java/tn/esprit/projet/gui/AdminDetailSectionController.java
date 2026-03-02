package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.projet.entities.HotelImage;
import tn.esprit.projet.entities.HotelReview;
import tn.esprit.projet.entities.RoomImage;

/**
 * FXML controller for RoomView.fxml.
 * Holds @FXML-injected detail view nodes so AdminController
 * no longer needs to walk the tree manually.
 */
public class AdminDetailSectionController {

    @FXML public VBox      detailViewSection;
    @FXML public Button    backButton;
    @FXML public StackPane mainImageContainer;
    @FXML public ImageView mainImageView;
    @FXML public HBox      thumbnailsContainer;
    @FXML public Label     detailName;
    @FXML public Label     detailSubInfo;
    @FXML public HBox      detailApiRow;
    @FXML public FlowPane  detailBadges;
    @FXML public Label     detailDesc;
    @FXML public FlowPane  detailActionButtons;
    @FXML public VBox      detailServicesSection;
    @FXML public Label     detailServicesTitle;
    @FXML public Button    detailManageServicesBtn;
    @FXML public FlowPane  detailServicesPane;
    @FXML public VBox      reviewsSection;
    @FXML public TableView<HotelReview> reviewsTable;
    @FXML public TableColumn<HotelReview, String> colReviewRating;
    @FXML public TableColumn<HotelReview, String> colReviewComment;
    @FXML public TableColumn<HotelReview, String> colReviewUser;
    @FXML public TableColumn<HotelReview, Void>   colReviewActions;
    @FXML public VBox      imagesSection;
    @FXML public Button    addImageBtn;
    @FXML public TableView<HotelImage> imagesTable;
    @FXML public TableColumn<HotelImage, String> colImagePath;
    @FXML public TableColumn<HotelImage, Void>   colImagePreview;
    @FXML public TableColumn<HotelImage, Void>   colImageActions;
    @FXML public VBox      roomImagesSection;
    @FXML public Button    addRoomImageBtn;
    @FXML public TableView<RoomImage> roomImagesTable;
    @FXML public TableColumn<RoomImage, String> colRoomImagePath;
    @FXML public TableColumn<RoomImage, Void>   colRoomImagePreview;
    @FXML public TableColumn<RoomImage, Void>   colRoomImageActions;
}
