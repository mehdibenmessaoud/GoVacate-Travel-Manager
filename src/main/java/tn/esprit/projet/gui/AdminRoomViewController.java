package tn.esprit.projet.gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import tn.esprit.projet.entities.*;
import tn.esprit.projet.services.RoomImageService;
import tn.esprit.projet.services.RoomService;

import java.sql.SQLException;
import java.util.List;

/**
 * Handles all Room-related UI logic:
 * – Table setup / filtering
 * – Detail view population
 * – Add / Edit / Delete / Status-change dialogs
 * – Room images (gallery + table)
 */
public class AdminRoomViewController {

    private final AdminSharedState state;
    private final AdminController3 admin;
    private final RoomService roomService;
    private final RoomImageService roomImageService;

    public AdminRoomViewController(AdminSharedState state, AdminController3 admin,
                                   RoomService roomService, RoomImageService roomImageService) {
        this.state = state;
        this.admin = admin;
        this.roomService = roomService;
        this.roomImageService = roomImageService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TABLE
    // ═══════════════════════════════════════════════════════════════════════

    public void setupRoomsTable(TableView<Object> mainTable,
                                TableColumn<Object, String> col1,
                                TableColumn<Object, String> col2,
                                TableColumn<Object, String> col3,
                                TableColumn<Object, String> col4,
                                TableColumn<Object, String> col5,
                                TableColumn<Object, Object> col6) {
        col1.setVisible(true); col2.setVisible(true); col3.setVisible(true);
        col4.setVisible(true); col5.setVisible(true); col6.setVisible(true);

        // ── Professional best practice: bind column widths as % of table width ──
        // Percentages: No Chambre 9% | Hotel 24% | Type 10% | Prix 10% | Statut 13% | Actions 34%
        if (mainTable != null) {
            mainTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            col1.prefWidthProperty().unbind(); col2.prefWidthProperty().unbind();
            col3.prefWidthProperty().unbind(); col4.prefWidthProperty().unbind();
            col5.prefWidthProperty().unbind(); col6.prefWidthProperty().unbind();

            // Responsive minima to keep the Action column fully visible.
            col1.setMinWidth(90);  col2.setMinWidth(180); col3.setMinWidth(90);
            col4.setMinWidth(90);  col5.setMinWidth(110); col6.setMinWidth(240);

            col1.setMaxWidth(Double.MAX_VALUE); col2.setMaxWidth(Double.MAX_VALUE);
            col3.setMaxWidth(Double.MAX_VALUE); col4.setMaxWidth(Double.MAX_VALUE);
            col5.setMaxWidth(Double.MAX_VALUE); col6.setMaxWidth(Double.MAX_VALUE);

            col1.setResizable(false); col2.setResizable(false); col3.setResizable(false);
            col4.setResizable(false); col5.setResizable(false); col6.setResizable(false);

            final TableView<Object> tbl = mainTable;
            javafx.application.Platform.runLater(() -> {
                javafx.beans.binding.DoubleBinding usable = tbl.widthProperty().subtract(18);
                col1.prefWidthProperty().bind(usable.multiply(0.09));
                col2.prefWidthProperty().bind(usable.multiply(0.24));
                col3.prefWidthProperty().bind(usable.multiply(0.10));
                col4.prefWidthProperty().bind(usable.multiply(0.10));
                col5.prefWidthProperty().bind(usable.multiply(0.13));
                col6.prefWidthProperty().bind(usable.multiply(0.34));
                // 0.09+0.24+0.10+0.10+0.13+0.34 = 1.00
            });
        }

        col1.setText("No Chambre"); col2.setText("Hotel"); col3.setText("Type");
        col4.setText("Prix"); col5.setText("Statut");
        col6.setText("Actions");

        col1.setCellValueFactory(data ->
                data.getValue() instanceof Room r ? new SimpleStringProperty(r.getRoomNumber()) : new SimpleStringProperty(""));
        col2.setCellValueFactory(data ->
                data.getValue() instanceof Room r ? new SimpleStringProperty(state.getHotelName(r.getHotelId())) : new SimpleStringProperty(""));
        col3.setCellValueFactory(data ->
                data.getValue() instanceof Room r ? new SimpleStringProperty(r.getRoomType()) : new SimpleStringProperty(""));
        DialogHelper.applyPlainTextCellFactory(col3);
        col4.setCellValueFactory(data ->
                data.getValue() instanceof Room r ? new SimpleStringProperty(GuiUtils.formatPrice(r.getPricePerNight()) + " DT") : new SimpleStringProperty(""));
        DialogHelper.applyPlainTextCellFactory(col4);

        col5.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                if (getTableRow().getItem() instanceof Room room) {
                    Label badge = new Label(GuiUtils.getStatusLabel(room.getStatus()));
                    badge.getStyleClass().add(GuiUtils.getStatusStyleClass(room.getStatus()));
                    setGraphic(badge);
                } else { setGraphic(null); }
            }
        });
        col5.setCellValueFactory(data ->
                data.getValue() instanceof Room r ? new SimpleStringProperty(r.getStatus()) : new SimpleStringProperty(""));

        col6.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col6.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = DialogHelper.createTableActionButton("Details", "gv-btn-blue", 84);
            private final Button editBtn = DialogHelper.createTableActionButton("Modifier", "gv-btn-orange", 92);
            private final Button deleteBtn = DialogHelper.createTableActionButton("Supprimer", "gv-btn-red", 98);
            private final HBox box = new HBox(4, viewBtn, editBtn, deleteBtn);
            {
                viewBtn.setTooltip(new Tooltip("Voir les details de cette chambre"));
                editBtn.setTooltip(new Tooltip("Modifier les informations"));
                deleteBtn.setTooltip(new Tooltip("Supprimer cette chambre"));
                box.setAlignment(Pos.CENTER_LEFT);
            }

            private void styleActionsForWidth() {
                double w = getTableColumn() == null ? 0 : getTableColumn().getWidth();
                boolean compact = w > 0 && w < 300;
                if (compact) {
                    viewBtn.setText("Voir");
                    editBtn.setText("Edit");
                    deleteBtn.setText("Supp.");
                    box.setSpacing(4);
                    viewBtn.setMinWidth(70); viewBtn.setPrefWidth(70); viewBtn.setMaxWidth(70);
                    editBtn.setMinWidth(72); editBtn.setPrefWidth(72); editBtn.setMaxWidth(72);
                    deleteBtn.setMinWidth(72); deleteBtn.setPrefWidth(72); deleteBtn.setMaxWidth(72);
                } else {
                    viewBtn.setText("Details");
                    editBtn.setText("Modifier");
                    deleteBtn.setText("Supprimer");
                    box.setSpacing(6);
                    viewBtn.setMinWidth(84); viewBtn.setPrefWidth(84); viewBtn.setMaxWidth(84);
                    editBtn.setMinWidth(92); editBtn.setPrefWidth(92); editBtn.setMaxWidth(92);
                    deleteBtn.setMinWidth(98); deleteBtn.setPrefWidth(98); deleteBtn.setMaxWidth(98);
                }
            }

            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || !(item instanceof Room room)) { setGraphic(null); return; }
                styleActionsForWidth();
                viewBtn.setOnAction(e -> admin.showRoomDetail(room));
                editBtn.setOnAction(e -> showEditRoomDialog(room));
                deleteBtn.setOnAction(e -> DialogHelper.confirmDelete("chambre", room.getRoomNumber(), () -> handleDeleteRoom(room), getClass()));
                setGraphic(box);
            }
        });
    }

    public void loadRoomsTable(TableView<Object> mainTable,
                               TextField searchField,
                               ComboBox<String> hotelFilterCombo,
                               ComboBox<String> roomTypeFilterCombo,
                               ComboBox<String> roomStatusFilterCombo,
                               Label pageSubtitle) {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String hotelFilter = hotelFilterCombo.getValue();
        String typeFilter = roomTypeFilterCombo != null ? roomTypeFilterCombo.getValue() : null;
        String statusFilter = roomStatusFilterCombo != null ? roomStatusFilterCombo.getValue() : null;
        ObservableList<Object> filtered = FXCollections.observableArrayList();

        for (Room r : state.getRoomsList()) {
            String hotelName = state.getHotelName(r.getHotelId());
            boolean matchSearch = search.isEmpty() || r.getRoomNumber().toLowerCase().contains(search)
                    || r.getRoomType().toLowerCase().contains(search) || hotelName.toLowerCase().contains(search);
            boolean matchHotel = hotelFilter == null || hotelFilter.equals("Tous hotels") || hotelFilter.equals(hotelName);
            boolean matchType = typeFilter == null || typeFilter.equals("Tous") || r.getRoomType().equals(typeFilter);
            boolean matchStatus = statusFilter == null || statusFilter.equals("Tous") || r.getStatus().equals(statusFilter);
            if (matchSearch && matchHotel && matchType && matchStatus) filtered.add(r);
        }
        mainTable.setItems(filtered);
        pageSubtitle.setText(filtered.size() + " chambre(s) trouvee(s)  -  Double-cliquez pour details");
    }

    public void setupHotelFilter(ComboBox<String> hotelFilterCombo) {
        hotelFilterCombo.getItems().clear();
        hotelFilterCombo.getItems().add("Tous hotels");
        for (Hotel h : state.getHotelsList()) hotelFilterCombo.getItems().add(h.getName());
        hotelFilterCombo.setValue("Tous hotels");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DETAIL VIEW
    // ═══════════════════════════════════════════════════════════════════════

    public void populateRoomDetail(Room room,
                                   Label detailName, Label detailSubInfo, Label detailDesc,
                                   FlowPane detailBadges, FlowPane detailActionButtons,
                                   VBox detailServicesSection, FlowPane detailServicesPane,
                                   VBox reviewsSection, VBox imagesSection, VBox roomImagesSection,
                                   TableView<RoomImage> roomImagesTable,
                                   StackPane mainImageContainer, HBox thumbnailsContainer) {
        String hotelName = state.getHotelName(room.getHotelId());
        detailName.setText("Chambre " + room.getRoomNumber());
        detailSubInfo.setText(hotelName);
        detailSubInfo.getStyleClass().add("detail-room-subinfo");
        detailDesc.setText("Type: " + room.getRoomType() + "\nCapacite: " + room.getCapacity() + " personnes\nPrix par nuit: " + room.getPricePerNight() + " DT");

        detailBadges.getChildren().clear();
        Label typeBadge = new Label(room.getRoomType());
        typeBadge.getStyleClass().add("badge-room-type");
        Label priceBadge = new Label(room.getPricePerNight() + " DT/nuit");
        priceBadge.getStyleClass().add("badge-room-price");
        Label statusBadge = new Label(GuiUtils.getStatusLabel(room.getStatus()));
        statusBadge.getStyleClass().add(GuiUtils.getStatusStyleClass(room.getStatus()));
        statusBadge.getStyleClass().add("badge-font-override");
        Label capacityBadge = new Label(room.getCapacity() + " personnes");
        capacityBadge.getStyleClass().add("badge-room-capacity");
        detailBadges.getChildren().addAll(typeBadge, priceBadge, statusBadge, capacityBadge);

        if (detailServicesSection != null) { detailServicesSection.setVisible(false); detailServicesSection.setManaged(false); }
        if (detailServicesPane != null) detailServicesPane.getChildren().clear();

        detailActionButtons.getChildren().clear();
        detailActionButtons.setHgap(12); detailActionButtons.setVgap(12);
        detailActionButtons.setPrefWrapLength(620); detailActionButtons.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = DialogHelper.createDetailActionButton("Modifier la chambre", "#FF8210");
        editBtn.setOnAction(e -> showEditRoomDialog(room));
        Button deleteBtn = DialogHelper.createDetailActionButton("Supprimer", "#dc3545");
        deleteBtn.setOnAction(e -> DialogHelper.confirmDelete("chambre", room.getRoomNumber(), () -> {
            handleDeleteRoom(room); admin.showRoomsView();
        }, getClass()));
        detailActionButtons.getChildren().addAll(editBtn, deleteBtn);

        reviewsSection.setVisible(false); reviewsSection.setManaged(false);
        imagesSection.setVisible(false); imagesSection.setManaged(false);
        roomImagesSection.setVisible(true); roomImagesSection.setManaged(true);

        loadRoomImages(room.getId(), roomImagesTable, mainImageContainer, thumbnailsContainer);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROOM CRUD DIALOGS
    // ═══════════════════════════════════════════════════════════════════════

    public void showAddRoomDialog() {
        if (state.getHotelsList().isEmpty()) {
            DialogHelper.showNotification("Creez d'abord un hotel!", "warning", getClass()); return;
        }
        Dialog<Room> dialog = createRoomDialog(null);
        dialog.showAndWait().ifPresent(r -> runSqlAction(() -> {
            roomService.create(r); admin.loadAllData(); admin.loadRoomsTable();
            DialogHelper.showNotification("Chambre creee!", "success", getClass());
        }));
    }

    public void showEditRoomDialog(Room room) {
        Dialog<Room> dialog = createRoomDialog(room);
        dialog.showAndWait().ifPresent(r -> runSqlAction(() -> {
            r.setId(room.getId()); roomService.update(r); admin.loadAllData();
            if (state.getSelectedRoom() != null && state.getSelectedRoom().getId() == room.getId()) admin.showRoomDetail(r);
            else admin.loadRoomsTable();
            DialogHelper.showNotification("Chambre modifiee!", "success", getClass());
        }));
    }

    private Dialog<Room> createRoomDialog(Room room) {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle(room == null ? "Nouvelle Chambre" : "Modifier la Chambre");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = DialogHelper.createDialogFormGrid();
        TextField numField = new TextField(room != null ? room.getRoomNumber() : "");
        numField.setPromptText("Ex: 101"); DialogHelper.applyDialogFieldSizing(numField);
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("SINGLE", "DOUBLE", "SUITE", "DELUXE", "FAMILY"));
        typeCombo.setValue(room != null ? room.getRoomType() : "SINGLE"); DialogHelper.applyDialogFieldSizing(typeCombo);
        Spinner<Integer> capSpinner = new Spinner<>(1, 10, room != null ? room.getCapacity() : 2);
        capSpinner.setEditable(false); DialogHelper.configureDialogSpinner(capSpinner, 0);
        TextField priceField = new TextField(room != null ? String.valueOf(room.getPricePerNight()) : "100");
        priceField.setPromptText("Prix par nuit"); DialogHelper.applyDialogFieldSizing(priceField);
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));
        statusCombo.setValue(room != null ? room.getStatus() : "AVAILABLE"); DialogHelper.applyDialogFieldSizing(statusCombo);
        ComboBox<Hotel> hotelCombo = new ComboBox<>();
        hotelCombo.setItems(FXCollections.observableArrayList(state.getHotelsList()));
        hotelCombo.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(Hotel item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : item.getName()); }
        });
        hotelCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Hotel item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : item.getName()); }
        });
        if (room != null) {
            state.getHotelsList().stream().filter(h -> h.getId() == room.getHotelId()).findFirst().ifPresent(hotelCombo::setValue);
        } else if (!state.getHotelsList().isEmpty()) {
            hotelCombo.setValue(state.getHotelsList().get(0));
        }
        DialogHelper.applyDialogFieldSizing(hotelCombo);

        grid.add(new Label("No Chambre *"), 0, 0); grid.add(numField, 1, 0);
        grid.add(new Label("Type"), 0, 1); grid.add(typeCombo, 1, 1);
        grid.add(new Label("Capacite"), 0, 2); grid.add(capSpinner, 1, 2);
        grid.add(new Label("Prix/Nuit *"), 0, 3); grid.add(priceField, 1, 3);
        grid.add(new Label("Statut"), 0, 4); grid.add(statusCombo, 1, 4);
        grid.add(new Label("Hotel *"), 0, 5); grid.add(hotelCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);
        DialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 740, 500);
        DialogHelper.styleDialog(dialog, false, getClass());

        Node saveButtonNode = dialog.getDialogPane().lookupButton(saveBtn);
        Runnable validate = () -> saveButtonNode.setDisable(
                GuiUtils.isBlank(numField.getText()) || hotelCombo.getValue() == null || GuiUtils.parsePrice(priceField.getText()) == null);
        validate.run();
        numField.textProperty().addListener((obs, oldValue, newValue) -> validate.run());
        priceField.textProperty().addListener((obs, oldValue, newValue) -> validate.run());
        hotelCombo.valueProperty().addListener((obs, oldValue, newValue) -> validate.run());

        dialog.setResultConverter(btn -> {
            if (btn != saveBtn) return null;
            Hotel selected = hotelCombo.getValue();
            Double price = GuiUtils.parsePrice(priceField.getText());
            if (selected == null || price == null) return null;
            return new Room(0, GuiUtils.trimToEmpty(numField.getText()), typeCombo.getValue(), capSpinner.getValue(), price, statusCombo.getValue(), selected.getId());
        });
        return dialog;
    }

    private void showChangeStatusDialog(Room room) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le statut");
        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        GridPane grid = DialogHelper.createDialogFormGrid();
        Label roomValue = new Label("Chambre " + room.getRoomNumber());
        roomValue.getStyleClass().add("gv-dialog-value-label");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));
        statusCombo.setValue(room.getStatus()); DialogHelper.applyDialogFieldSizing(statusCombo);
        grid.add(new Label("Chambre"), 0, 0); grid.add(roomValue, 1, 0);
        grid.add(new Label("Nouveau statut"), 0, 1); grid.add(statusCombo, 1, 1);
        dialog.getDialogPane().setContent(grid);
        DialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 680, 300);
        DialogHelper.styleDialog(dialog, false, getClass());
        dialog.setResultConverter(btn -> btn == saveBtn ? statusCombo.getValue() : null);
        dialog.showAndWait().ifPresent(newStatus -> {
            try {
                room.setStatus(newStatus); roomService.update(room); admin.loadAllData(); admin.showRoomDetail(room);
                DialogHelper.showNotification("Statut mis a jour!", "success", getClass());
            } catch (SQLException e) { DialogHelper.showNotification("Erreur: " + e.getMessage(), "error", getClass()); }
        });
    }

    public void handleDeleteRoom(Room room) {
        runSqlAction(() -> {
            roomService.delete(room.getId()); admin.loadAllData(); admin.loadRoomsTable();
            DialogHelper.showNotification("Chambre supprimee!", "success", getClass());
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROOM IMAGES
    // ═══════════════════════════════════════════════════════════════════════

    public void setupRoomImagesTable(TableColumn<RoomImage, String> colRoomImagePath,
                                     TableColumn<RoomImage, Void> colRoomImagePreview,
                                     TableColumn<RoomImage, Void> colRoomImageActions) {
        colRoomImagePath.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImageUrl()));
        colRoomImagePreview.setCellFactory(col -> new TableCell<>() {
            private final ImageView preview = new ImageView();
            { preview.setFitWidth(60); preview.setFitHeight(40); preview.setPreserveRatio(true); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) { setGraphic(null); return; }
                Image image = ImageLoader.load(getTableView().getItems().get(getIndex()).getImageUrl(), getClass());
                if (image != null) { preview.setImage(image); setGraphic(preview); } else { setGraphic(new Label("!")); }
            }
        });
        colRoomImageActions.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = DialogHelper.createTableActionButton("Supprimer", "gv-btn-red", 96);
            { deleteBtn.setTooltip(new Tooltip("Supprimer cette image")); deleteBtn.setOnAction(e -> DialogHelper.confirmDelete("image", "", () -> handleDeleteRoomImage(getTableView().getItems().get(getIndex())), getClass())); }
            @Override protected void updateItem(Void item, boolean empty) { super.updateItem(item, empty); setGraphic(empty ? null : deleteBtn); }
        });
    }

    public void loadRoomImages(int roomId, TableView<RoomImage> roomImagesTable,
                               StackPane mainImageContainer, HBox thumbnailsContainer) {
        mainImageContainer.getChildren().clear(); thumbnailsContainer.getChildren().clear();
        try {
            List<RoomImage> images = roomImageService.getByRoomId(roomId);
            roomImagesTable.setItems(FXCollections.observableArrayList(images));
            ImageGalleryBuilder.build(images, img -> img.getImageUrl(),
                    mainImageContainer, thumbnailsContainer, "#679AC1", "R", getClass());
        } catch (SQLException e) {
            ImageGalleryBuilder.showPlaceholder("!", mainImageContainer);
        }
    }

    public void saveRoomImage(String imagePath, int roomId,
                              TableView<RoomImage> roomImagesTable,
                              StackPane mainImageContainer, HBox thumbnailsContainer) throws SQLException {
        roomImageService.createWithImagePipeline(imagePath, roomId);
        loadRoomImages(roomId, roomImagesTable, mainImageContainer, thumbnailsContainer);
    }

    private void handleDeleteRoomImage(RoomImage image) {
        runSqlAction(() -> {
            roomImageService.delete(image.getId());
            Room sel = state.getSelectedRoom();
            if (sel != null) DialogHelper.showNotification("Image supprimee!", "success", getClass());
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    @FunctionalInterface
    private interface SqlAction { void run() throws SQLException; }

    private void runSqlAction(SqlAction action) {
        try { action.run(); }
        catch (SQLException e) { DialogHelper.showNotification("Erreur: " + e.getMessage(), "error", getClass()); }
    }
}
