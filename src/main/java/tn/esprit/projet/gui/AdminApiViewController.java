package tn.esprit.projet.gui;

import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;
import tn.esprit.projet.API.hotels.GeoapifyPlacesApiClient;
import tn.esprit.projet.API.hotels.NominatimHotelApiClient;
import tn.esprit.projet.services.HotelService;
import tn.esprit.projet.utils.DialogHelper;
import tn.esprit.projet.utils.GuiUtils;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles all external API UI logic:
 * - Geoapify hotel search + results table
 * - OSM map preview dialog (opened from hotel detail or table row)
 */
public class AdminApiViewController {

    private static final int    OSM_PREVIEW_MIN_ZOOM     = 3;
    private static final int    OSM_PREVIEW_MAX_ZOOM     = 18;
    private static final int    OSM_PREVIEW_INITIAL_ZOOM = 11;
    private static final int    OSM_TILE_SIZE            = 256;
    private static final String OSM_TILE_URL_TEMPLATE    = "https://tile.openstreetmap.org/%d/%d/%d.png";

    private final AdminController3 admin;
    private final HotelService       hotelService;
    private final Map<String, Image> osmTileCache         = new HashMap<>();
    // Stores the last Geoapify search query so the import cell factory can
    // pass it to importGeoapifyHotel for reliable Latin city matching
    private String                   lastGeoapifyQuery    = "";
    private       boolean            apiRequestInProgress = false;

    public AdminApiViewController(AdminController3 admin, HotelService hotelService) {
        this.admin        = admin;
        this.hotelService = hotelService;
    }

    public boolean isApiRequestInProgress() { return apiRequestInProgress; }

    // -----------------------------------------------------------------------
    // GEOAPIFY
    // -----------------------------------------------------------------------

    public void executeGeoapifySearchAsync(String query,
                                           TableView<Object> mainTable,
                                           Label pageTitle,
                                           Label pageSubtitle,
                                           Button addButton,
                                           ProgressIndicator apiLoadingIndicator,
                                           Button geoapifyApiButton) {
        Task<List<GeoapifyPlacesApiClient.HotelPlace>> task = new Task<>() {
            @Override
            protected List<GeoapifyPlacesApiClient.HotelPlace> call() throws Exception {
                return hotelService.fetchGeoapifyHotels(query, 20);
            }
        };
        task.setOnSucceeded(ev -> {
            setApiLoadingState(false, apiLoadingIndicator, geoapifyApiButton);
            List<GeoapifyPlacesApiClient.HotelPlace> places = task.getValue();
            showGeoapifyPlacesInTable(query, places, mainTable, pageTitle, pageSubtitle, addButton);
        });
        task.setOnFailed(ev -> {
            setApiLoadingState(false, apiLoadingIndicator, geoapifyApiButton);
            handleApiFailure("Geoapify", task.getException());
        });
        setApiLoadingState(true, apiLoadingIndicator, geoapifyApiButton);
        Thread worker = new Thread(task, "gv-admin-geoapify-search");
        worker.setDaemon(true);
        worker.start();
    }

    private void showGeoapifyPlacesInTable(String query,
                                           List<GeoapifyPlacesApiClient.HotelPlace> places,
                                           TableView<Object> mainTable,
                                           Label pageTitle,
                                           Label pageSubtitle,
                                           Button addButton) {
        addButton.setDisable(false);
        lastGeoapifyQuery = (query == null ? "" : query.trim());
        admin.configureGeoapifyColumns();
        mainTable.setItems(javafx.collections.FXCollections.observableArrayList(places));
        pageTitle.setText("Hotels Geoapify");
        pageSubtitle.setText(places.size() + " hotel(s) trouve(s) pour \"" + query + "\"");
    }

    public void setupGeoapifyColumns(TableColumn<Object, String> col1,
                                     TableColumn<Object, String> col2,
                                     TableColumn<Object, String> col3,
                                     TableColumn<Object, String> col4,
                                     TableColumn<Object, String> col5,
                                     TableColumn<Object, Object> col6) {
        col1.setVisible(true);  col2.setVisible(true);  col3.setVisible(true);
        col4.setVisible(false); col5.setVisible(false); col6.setVisible(true);

        // Unbind any percentage bindings set by hotel/room table setup
        col1.prefWidthProperty().unbind(); col2.prefWidthProperty().unbind();
        col3.prefWidthProperty().unbind(); col4.prefWidthProperty().unbind();
        col5.prefWidthProperty().unbind(); col6.prefWidthProperty().unbind();
        col1.setResizable(true); col2.setResizable(true);
        col3.setResizable(true); col6.setResizable(true);

        col4.setMinWidth(0); col4.setPrefWidth(0); col4.setMaxWidth(0);
        col5.setMinWidth(0); col5.setPrefWidth(0); col5.setMaxWidth(0);

        // Constrained flex for Geoapify (3 visible cols fill 100% of table width)
        col1.getTableView().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        col1.setText("Nom");       col1.setMinWidth(160); col1.setPrefWidth(220);
        col2.setText("Site web");  col2.setMinWidth(180); col2.setPrefWidth(240);
        col3.setText("Téléphone"); col3.setMinWidth(140); col3.setPrefWidth(180);
        col6.setText("Actions");   col6.setMinWidth(260); col6.setPrefWidth(290); col6.setMaxWidth(320);

        // col1: name — wrapping
        col1.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setText(null); setGraphic(null); return; }
                Object row = getTableView().getItems().get(getIndex());
                String val = row instanceof GeoapifyPlacesApiClient.HotelPlace p ? p.name() : "";
                Label l = wrapLabel(val, col1.getPrefWidth() - 16);
                setGraphic(l); setText(null); setAlignment(Pos.CENTER_LEFT);
            }
        });
        col1.setCellValueFactory(d -> d.getValue() instanceof GeoapifyPlacesApiClient.HotelPlace o
                ? new SimpleStringProperty(o.name()) : new SimpleStringProperty(""));

        // col2: clickable website — wrapping
        col2.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setGraphic(null); setText(null); return; }
                Object row = getTableView().getItems().get(getIndex());
                if (!(row instanceof GeoapifyPlacesApiClient.HotelPlace p) || p.website().isBlank()) {
                    setText("N/A"); setGraphic(null);
                    getStyleClass().add("gv-wrap-label");
                    return;
                }
                Label link = new Label(p.website());
                link.getStyleClass().add("gv-link");
                link.setWrapText(true);
                link.setMaxWidth(col2.getPrefWidth() - 16);
                link.setOnMouseClicked(e -> openUrlInBrowser(p.website()));
                setGraphic(link); setText(null);
            }
        });
        col2.setCellValueFactory(d -> d.getValue() instanceof GeoapifyPlacesApiClient.HotelPlace o
                ? new SimpleStringProperty(o.website()) : new SimpleStringProperty(""));

        // col3: phone + hours — wrapping
        // col2: clickable website
        col2.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setGraphic(null); setText(null); return; }
                Object row = getTableView().getItems().get(getIndex());
                if (!(row instanceof GeoapifyPlacesApiClient.HotelPlace p) || p.website().isBlank()) {
                    setText("N/A"); setGraphic(null);
                    getStyleClass().add("gv-wrap-label");
                    return;
                }
                Label link = new Label(p.website());
                link.getStyleClass().add("gv-link");
                link.setWrapText(true);
                link.setMaxWidth(col2.getPrefWidth() - 16);
                link.setOnMouseClicked(e -> openUrlInBrowser(p.website()));
                setGraphic(link); setText(null);
            }
        });
        col2.setCellValueFactory(d -> d.getValue() instanceof GeoapifyPlacesApiClient.HotelPlace o
                ? new SimpleStringProperty(o.website()) : new SimpleStringProperty(""));

        // col3: phone only
        col3.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setGraphic(null); setText(null); return; }
                Object row = getTableView().getItems().get(getIndex());
                if (!(row instanceof GeoapifyPlacesApiClient.HotelPlace p)) { setText(""); setGraphic(null); return; }
                String phone = p.phone().isBlank() ? "N/A" : "📞 " + p.phone();
                Label l = wrapLabel(phone, col3.getPrefWidth() - 16);
                setGraphic(l); setText(null); setAlignment(Pos.CENTER_LEFT);
            }
        });
        col3.setCellValueFactory(d -> new SimpleStringProperty(""));

        // col6: Détails + Carte + Importer buttons
        col6.setCellFactory(tc -> new TableCell<>() {
            private final Button mapBtn      = buildActionBtn("🗺  Carte",     "#0ea5e9", "#0284c7");
            private final Button detailBtn   = buildActionBtn("ℹ  Détails",   "#6366f1", "#4f46e5");
            private final Button importBtn   = buildActionBtn("＋ Importer",   "#16a34a", "#15803d");
            private final HBox box = new HBox(6, detailBtn, mapBtn, importBtn);
            {
                box.setAlignment(Pos.CENTER);
                mapBtn.setOnAction(e -> {
                    Object item = getTableView().getItems().get(getIndex());
                    if (item instanceof GeoapifyPlacesApiClient.HotelPlace p && p.hasCoords()) {
                        NominatimHotelApiClient.LocationSummary loc = new NominatimHotelApiClient.LocationSummary(
                                p.name(), String.valueOf(p.lat()), String.valueOf(p.lon()),
                                "node", 0, "", "", 1.0);
                        openOpenStreetMapLocation(loc);
                    } else {
                        DialogHelper.showNotification("Coordonnées indisponibles pour cet hôtel.", "warning", getClass());
                    }
                });
                detailBtn.setOnAction(e -> {
                    Object item = getTableView().getItems().get(getIndex());
                    if (item instanceof GeoapifyPlacesApiClient.HotelPlace p) showGeoapifyDetailDialog(p);
                });
                importBtn.setOnAction(e -> {
                    Object item = getTableView().getItems().get(getIndex());
                    if (item instanceof GeoapifyPlacesApiClient.HotelPlace p) importGeoapifyHotel(p, lastGeoapifyQuery);
                });
            }
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    Object row = getTableView().getItems().get(getIndex());
                    mapBtn.setDisable(!(row instanceof GeoapifyPlacesApiClient.HotelPlace p && p.hasCoords()));
                    setGraphic(box);
                }
            }
        });
    }

    /** Creates a Label with word-wrap enabled, suitable for table cells. */
    private Label wrapLabel(String text, double maxWidth) {
        Label l = new Label(text == null ? "" : text);
        l.setWrapText(true);
        l.setMaxWidth(maxWidth > 0 ? maxWidth : Double.MAX_VALUE);
        l.getStyleClass().add("gv-wrap-label");
        return l;
    }

    private Button buildActionBtn(String text, String color, String hoverColor) {
        Button b = new Button(text);
        b.getStyleClass().add("gv-res-action-btn");
        b.setStyle("-fx-background-color: " + color + ";");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: " + hoverColor + ";"));
        b.setOnMouseExited(e  -> b.setStyle("-fx-background-color: " + color + ";"));
        return b;
    }

    /**
     * @param place  the hotel returned by Geoapify
     * @param query  the city string the user originally typed (e.g. "nabeul", "london")
     *               — used as a reliable Latin fallback when place.city() is in Arabic
     *                 or another non-Latin script
     */
    private void importGeoapifyHotel(GeoapifyPlacesApiClient.HotelPlace place, String query) {
        // Fresh load from DB — only existing destinations are offered
        admin.getState().refreshLocalisationLookup();
        List<String> locLabels = admin.getState().getSortedLocalisationLabels();

        // Try to smart-match against existing destinations.
        // We try three sources in order:
        //   1. place.city()   — may be Arabic/non-Latin for some regions (Geoapify OSM data)
        //   2. query          — what the user typed; always Latin, reliable for matching
        //   3. place.country()— last resort country-level match
        String bestMatch = findBestLocalisationMatch(locLabels, place.city(), query, place.country());

        // Build clean description
        StringBuilder cleanDesc = new StringBuilder();
        if (!place.categoryLabel().isBlank()) cleanDesc.append(place.categoryLabel());
        if (!place.address().isBlank()) {
            if (cleanDesc.length() > 0) cleanDesc.append(" — ");
            cleanDesc.append(place.address());
        }
        if (!place.phone().isBlank())   cleanDesc.append("  |  ☎ ").append(place.phone());
        if (!place.website().isBlank()) cleanDesc.append("  |  🌐 ").append(place.website());

        // ── Dialog ────────────────────────────────────────────────────────────
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Importer — " + place.name());
        ButtonType importType =
                new ButtonType("Importer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importType, ButtonType.CANCEL);

        GridPane grid = DialogHelper.createDialogFormGrid();

        TextField nameField = new TextField(place.name());
        DialogHelper.applyDialogFieldSizing(nameField);

        TextArea descField =
                new TextArea(cleanDesc.toString().trim());
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        DialogHelper.applyDialogFieldSizing(descField);

        Spinner<Integer> starsSpinner =
                new Spinner<>(1, 5, 3);
        starsSpinner.setEditable(false);
        DialogHelper.configureDialogSpinner(starsSpinner, 0);

        ComboBox<String> statusCombo = new ComboBox<>(
                javafx.collections.FXCollections.observableArrayList("AVAILABLE", "OCCUPIED", "MAINTENANCE"));
        statusCombo.setValue("AVAILABLE");
        DialogHelper.applyDialogFieldSizing(statusCombo);

        // Localisation — existing destinations only, no DB writes
        ComboBox<String> locCombo = new ComboBox<>();
        locCombo.getItems().addAll(locLabels);
        locCombo.setPromptText("Sélectionner une destination");
        locCombo.setValue(bestMatch); // null when no match → user must pick
        DialogHelper.applyDialogFieldSizing(locCombo);

        // ── "➕" button to create a new destination on the fly ─────────────
        // Pre-fill city and country from the Geoapify result (or query as fallback)
        String prefCity    = (place.city() != null && !place.city().isBlank()) ? place.city() : query;
        String prefCountry = place.country();
        Button newDestBtn =
                admin.getHotelViewController().buildNewDestinationButton(locCombo, prefCity, prefCountry, dialog.getDialogPane());

        HBox locRow = new HBox(8, locCombo, newDestBtn);
        locRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(locCombo, Priority.ALWAYS);

        // Warning when no destination matches — show the user's original search query
        // (Latin, readable) NOT the Arabic place.city() from Geoapify
        Label hintLabel = new Label();
        if (bestMatch == null) {
            String hint = (query != null && !query.isBlank()) ? query : place.locationLine();
            hintLabel.setText("⚠ \"" + hint + "\" n'existe pas dans vos destinations — "
                    + "sélectionnez ou créez une destination (➕).");
            hintLabel.getStyleClass().add("gv-hint-label");
        }

        grid.add(new Label("Nom *"),          0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Description"),    0, 1); grid.add(descField,    1, 1);
        grid.add(new Label("Étoiles"),        0, 2); grid.add(starsSpinner, 1, 2);
        grid.add(new Label("Statut"),         0, 3); grid.add(statusCombo,  1, 3);
        grid.add(new Label("Localisation *"), 0, 4); grid.add(locRow,       1, 4);
        if (!hintLabel.getText().isBlank()) grid.add(hintLabel, 1, 5);

        // Save disabled until both name and localisation are filled.
        // Use Platform.runLater so lookupButton resolves after dialog is shown.
        Node importNode = dialog.getDialogPane().lookupButton(importType);
        Runnable syncState = () -> importNode.setDisable(
                nameField.getText().isBlank() || GuiUtils.isBlank(locCombo.getValue()));
        javafx.application.Platform.runLater(syncState); // initial state after show
        nameField.textProperty().addListener((o, old, v) -> syncState.run());
        locCombo.valueProperty().addListener((o, old, v) -> syncState.run());

        dialog.getDialogPane().setContent(grid);
        DialogHelper.applyDialogPaneSizing(dialog.getDialogPane(), 700, 440);
        DialogHelper.styleDialog(dialog, false, getClass());
        javafx.application.Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(btn -> btn == importType);
        dialog.showAndWait().ifPresent(confirmed -> {
            if (!confirmed) return;
            // Extra guard: if somehow combo is still blank, abort rather than
            // silently saving with the wrong default destination
            String selectedLoc = locCombo.getValue();
            if (GuiUtils.isBlank(selectedLoc)) {
                DialogHelper.showNotification(
                        "Veuillez sélectionner une destination avant d'importer.", "warning", getClass());
                return;
            }
            int locationId = admin.getState().resolveLocalisationId(
                    selectedLoc, admin.getState().getDefaultDestinationId());
            tn.esprit.projet.entities.Hotel h = new tn.esprit.projet.entities.Hotel(
                    0, nameField.getText().trim(), descField.getText().trim(),
                    starsSpinner.getValue(), statusCombo.getValue(), locationId);
            try {
                hotelService.create(h);
                admin.loadAllData();
                // Show notification first (import dialog is already closed, this is safe).
                // After the user clicks OK, ALL showAndWait calls are fully resolved so
                // goToHotels() can be called synchronously with no modal state to interfere.
                DialogHelper.showNotification(
                        "\"" + h.getName() + "\" importé avec succès!", "success", getClass());
                admin.goToHotels();
            } catch (Exception ex) {
                DialogHelper.showNotification(
                        "Erreur import: " + ex.getMessage(), "error", getClass());
            }
        });
    }

    /**
     * Find the best matching localisation label among existing DB destinations.
     *
     * Tries in order:
     *  1. place.city()   — may be in Arabic/non-Latin for some regions
     *  2. searchQuery    — what the user typed (always Latin, most reliable)
     *  3. country        — last resort country-level match
     *
     * Returns null when nothing matches — caller leaves combo blank.
     */
    private String findBestLocalisationMatch(
            List<String> labels, String city, String searchQuery, String country) {

        if (labels == null || labels.isEmpty()) return null;

        String cityLc    = city        == null ? "" : city.trim().toLowerCase();
        String queryLc   = searchQuery == null ? "" : searchQuery.trim().toLowerCase();
        String countryLc = country     == null ? "" : country.trim().toLowerCase();

        if (cityLc.isBlank() && queryLc.isBlank() && countryLc.isBlank()) return null;

        String byCity = null, byQuery = null, byCountry = null;
        for (String label : labels) {
            String lbl = label.toLowerCase();
            // Word-boundary match prevents "tunis" from matching inside "tunisie"
            if (!cityLc.isBlank()    && containsWord(lbl, cityLc)    && byCity    == null) byCity    = label;
            if (!queryLc.isBlank()   && containsWord(lbl, queryLc)   && byQuery   == null) byQuery   = label;
            if (!countryLc.isBlank() && containsWord(lbl, countryLc) && byCountry == null) byCountry = label;
        }
        if (byCity    != null) return byCity;
        if (byQuery   != null) return byQuery;
        return byCountry;
    }

    /**
     * True when word appears as a complete word inside text (non-letter boundaries).
     * Examples:
     *   containsWord("sfax - sfax (tunisie)", "tunis")   -> false
     *   containsWord("sfax - sfax (tunisie)", "tunisie") -> true
     *   containsWord("tunis - tunis (tunisie)", "tunis") -> true
     *   containsWord("paris - paris (france)", "paris")  -> true
     */
    private boolean containsWord(String text, String word) {
        int idx = text.indexOf(word);
        while (idx >= 0) {
            boolean beforeOk = (idx == 0) || !Character.isLetter(text.charAt(idx - 1));
            boolean afterOk  = (idx + word.length() >= text.length())
                    || !Character.isLetter(text.charAt(idx + word.length()));
            if (beforeOk && afterOk) return true;
            idx = text.indexOf(word, idx + 1);
        }
        return false;
    }

    private void showGeoapifyDetailDialog(GeoapifyPlacesApiClient.HotelPlace place) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + place.name());
        ButtonType closeType =
                new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        VBox root = new VBox(0);
        root.getStyleClass().add("gv-detail-dialog-root");

        // Header
        HBox hbar = new HBox(14);
        hbar.setAlignment(Pos.CENTER_LEFT);
        hbar.setPadding(new Insets(18, 24, 18, 24));
        hbar.getStyleClass().add("gv-detail-dialog-header");
        Label iconLbl = new Label("🌍");
        iconLbl.getStyleClass().add("gv-detail-dialog-icon");
        VBox titleBox = new VBox(3);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLbl = new Label(place.name());
        titleLbl.getStyleClass().add("gv-detail-dialog-title");
        Label subLbl = new Label("Geoapify Places API  ·  " + place.categoryLabel());
        subLbl.getStyleClass().add("gv-detail-dialog-subtitle");
        titleBox.getChildren().addAll(titleLbl, subLbl);
        hbar.getChildren().addAll(iconLbl, titleBox);

        // Body rows
        VBox body = new VBox(0);
        body.setPadding(new Insets(20, 26, 24, 26));
        String[][] rows = {
                {"🏨", "Type",        place.categoryLabel().isBlank()  ? "N/A" : place.categoryLabel()},
                {"📍", "Adresse",     place.address().isBlank()        ? "N/A" : place.address()},
                {"🏙", "Ville",       place.locationLine().isBlank()   ? "N/A" : place.locationLine()},
                {"📮", "Code postal", place.postcode().isBlank()       ? "N/A" : place.postcode()},
                {"🌐", "Site web",    place.website().isBlank()        ? "N/A" : place.website()},
                {"📞", "Téléphone",   place.phone().isBlank()          ? "N/A" : place.phone()},
                {"🕒", "Horaires",    place.openingHours().isBlank()   ? "N/A" : place.openingHours()},
                {"📍", "Lat / Lon",   place.hasCoords()
                        ? place.latStr() + " / " + place.lonStr() : "N/A"},
                {"🔗", "Source",      "Geoapify Places API (données OpenStreetMap)"},
        };
        for (String[] row : rows) body.getChildren().add(detailDialogRow(row[0], row[1], row[2]));

        if (place.hasCoords()) {
            Button mapBtn2 = new Button("🗺  Voir sur la carte");
            mapBtn2.getStyleClass().add("gv-detail-map-btn");
            mapBtn2.setOnAction(e -> {
                dialog.close();
                NominatimHotelApiClient.LocationSummary loc = new NominatimHotelApiClient.LocationSummary(
                        place.name(), String.valueOf(place.lat()), String.valueOf(place.lon()),
                        "node", 0, "", "", 1.0);
                openOpenStreetMapLocation(loc);
            });
            HBox mapRow = new HBox(mapBtn2);
            mapRow.setPadding(new Insets(14, 0, 0, 0));
            body.getChildren().add(mapRow);
        }

        root.getChildren().addAll(hbar, body);
        DialogPane pane = dialog.getDialogPane();
        pane.setContent(root); pane.setPadding(Insets.EMPTY);
        pane.getStyleClass().add("gv-detail-dialog-pane");
        pane.setMinWidth(520); pane.setPrefWidth(560); pane.setMaxWidth(620);

        java.net.URL cssUrl = getClass().getResource("/css/admin-hotel-style.css");
        if (cssUrl != null && !pane.getStylesheets().contains(cssUrl.toExternalForm()))
            pane.getStylesheets().add(cssUrl.toExternalForm());

        javafx.application.Platform.runLater(() -> {
            Node closeNode = pane.lookupButton(closeType);
            if (closeNode instanceof Button cb) {
                cb.setMinWidth(100);
                cb.getStyleClass().add("gv-detail-close-btn");
            }
        });
        dialog.showAndWait();
    }

    private HBox detailDialogRow(String icon, String key, String value) {
        HBox r = new HBox(0);
        r.setAlignment(Pos.CENTER_LEFT);
        r.setPadding(new Insets(11, 0, 11, 0));
        r.getStyleClass().add("gv-detail-row");
        Label ic = new Label(icon); ic.getStyleClass().add("gv-detail-row-icon");
        Label k  = new Label(key + " :"); k.getStyleClass().add("gv-detail-row-key");
        Label v  = new Label(value); v.getStyleClass().add("gv-detail-row-value");
        v.setWrapText(true); HBox.setHgrow(v, Priority.ALWAYS);
        r.getChildren().addAll(ic, k, v);
        return r;
    }

    // -----------------------------------------------------------------------
    // NOMINATIM
    // -----------------------------------------------------------------------

    public void executeNominatimSearchAsync(String query,
                                            TableView<Object> mainTable,
                                            Label pageTitle,
                                            Label pageSubtitle,
                                            Button addButton,
                                            ProgressIndicator apiLoadingIndicator,
                                            Button nominatimApiButton) {
        Task<List<NominatimHotelApiClient.LocationSummary>> task = new Task<>() {
            @Override
            protected List<NominatimHotelApiClient.LocationSummary> call() throws Exception {
                return hotelService.fetchOsmLocations(query, 20);
            }
        };
        task.setOnSucceeded(ev -> {
            setApiLoadingStateNominatim(false, apiLoadingIndicator, nominatimApiButton);
            List<NominatimHotelApiClient.LocationSummary> locations = task.getValue();
            showNominatimLocationsInTable(query, locations, mainTable, pageTitle, pageSubtitle, addButton);
        });
        task.setOnFailed(ev -> {
            setApiLoadingStateNominatim(false, apiLoadingIndicator, nominatimApiButton);
            handleApiFailure("Nominatim", task.getException());
        });
        setApiLoadingStateNominatim(true, apiLoadingIndicator, nominatimApiButton);
        Thread worker = new Thread(task, "gv-admin-nominatim-search");
        worker.setDaemon(true);
        worker.start();
    }

    private void showNominatimLocationsInTable(String query,
                                               List<NominatimHotelApiClient.LocationSummary> locations,
                                               TableView<Object> mainTable,
                                               Label pageTitle,
                                               Label pageSubtitle,
                                               Button addButton) {
        addButton.setDisable(false);
        admin.configureNominatimColumns();
        mainTable.setItems(javafx.collections.FXCollections.observableArrayList(locations));
        pageTitle.setText("Lieux Nominatim OSM");
        pageSubtitle.setText(locations.size() + " résultat(s) pour \"" + query + "\"");
    }

    public void setupNominatimColumns(TableColumn<Object, String> col1,
                                      TableColumn<Object, String> col2,
                                      TableColumn<Object, String> col3,
                                      TableColumn<Object, String> col4,
                                      TableColumn<Object, String> col5,
                                      TableColumn<Object, Object> col6) {
        col1.setVisible(true); col2.setVisible(true); col3.setVisible(true);
        col4.setVisible(true); col5.setVisible(true); col6.setVisible(false);
        col1.setText("Nom / Adresse");
        col2.setText("Latitude");
        col3.setText("Longitude");
        col4.setText("Catégorie");
        col5.setText("Type OSM");

        col1.setCellValueFactory(d -> d.getValue() instanceof NominatimHotelApiClient.LocationSummary o
                ? new SimpleStringProperty(o.displayName()) : new SimpleStringProperty(""));
        DialogHelper.applyPlainTextCellFactory(col1);
        col2.setCellValueFactory(d -> d.getValue() instanceof NominatimHotelApiClient.LocationSummary o
                ? new SimpleStringProperty(o.lat() == null ? "N/A" : o.lat()) : new SimpleStringProperty(""));
        DialogHelper.applyPlainTextCellFactory(col2);
        col3.setCellValueFactory(d -> d.getValue() instanceof NominatimHotelApiClient.LocationSummary o
                ? new SimpleStringProperty(o.lon() == null ? "N/A" : o.lon()) : new SimpleStringProperty(""));
        DialogHelper.applyPlainTextCellFactory(col3);
        col4.setCellValueFactory(d -> d.getValue() instanceof NominatimHotelApiClient.LocationSummary o
                ? new SimpleStringProperty(o.category().isBlank() ? "N/A" : o.category()) : new SimpleStringProperty(""));
        DialogHelper.applyPlainTextCellFactory(col4);
        col5.setCellValueFactory(d -> d.getValue() instanceof NominatimHotelApiClient.LocationSummary o
                ? new SimpleStringProperty(o.placeType().isBlank() ? "N/A" : o.placeType()) : new SimpleStringProperty(""));
        DialogHelper.applyPlainTextCellFactory(col5);

        // col6: Map button
        col6.setVisible(true);
        col6.setText("Actions");
        col6.setMinWidth(130); col6.setPrefWidth(140); col6.setMaxWidth(160);
        col6.setCellFactory(tc -> new TableCell<>() {
            private final Button mapBtn = buildActionBtn("🗺  Carte", "#0ea5e9", "#0284c7");
            private final HBox box = new HBox(mapBtn);
            {
                box.setAlignment(Pos.CENTER);
                mapBtn.setOnAction(e -> {
                    Object item = getTableView().getItems().get(getIndex());
                    if (item instanceof NominatimHotelApiClient.LocationSummary loc) {
                        openOpenStreetMapLocation(loc);
                    }
                });
            }
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { setGraphic(null); return; }
                Object row = getTableView().getItems().get(getIndex());
                boolean hasCoords = row instanceof NominatimHotelApiClient.LocationSummary loc2
                        && loc2.lat() != null && !loc2.lat().isBlank()
                        && loc2.lon() != null && !loc2.lon().isBlank();
                mapBtn.setDisable(!hasCoords);
                setGraphic(box);
            }
        });

        // Double-click on a row opens the OSM map preview
        col1.getTableView().setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                Object sel = col1.getTableView().getSelectionModel().getSelectedItem();
                if (sel instanceof NominatimHotelApiClient.LocationSummary loc) {
                    openOpenStreetMapLocation(loc);
                }
            }
        });
    }

    private void setApiLoadingStateNominatim(boolean loading,
                                             ProgressIndicator apiLoadingIndicator,
                                             Button nominatimApiButton) {
        apiRequestInProgress = loading;
        if (apiLoadingIndicator != null) {
            apiLoadingIndicator.setVisible(loading);
            apiLoadingIndicator.setManaged(loading);
        }
        if (nominatimApiButton != null) nominatimApiButton.setDisable(loading);
    }

    // -----------------------------------------------------------------------
    // OSM MAP PREVIEW  (opened from hotel detail pill or table double-click)
    // -----------------------------------------------------------------------

    public void openOpenStreetMapLocation(NominatimHotelApiClient.LocationSummary location) {
        String url = buildOpenStreetMapUrl(location);
        if ("N/A".equals(url)) {
            DialogHelper.showNotification("Lien OpenStreetMap indisponible pour ce resultat.", "warning", getClass());
            return;
        }
        showOpenStreetMapPreviewDialog(location, url);
    }

    private void showOpenStreetMapPreviewDialog(NominatimHotelApiClient.LocationSummary location,
                                                String fallbackUrl) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Apercu carte - OpenStreetMap");
        dialog.setResizable(true);

        ButtonType openBrowserType = new ButtonType("Itineraires", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(openBrowserType, ButtonType.CLOSE);

        VBox content = new VBox(0);
        content.getStyleClass().add("gv-detail-dialog-content");

        String displayName   = location == null ? null : location.displayName();
        String locationTitle = extractLocationTitle(displayName);
        String fullAddr      = GuiUtils.trimToMaxLength(GuiUtils.formatApiValue(displayName, "N/A"), 100);
        String coordText     = GuiUtils.formatCoordinates(
                location == null ? null : location.lat(),
                location == null ? null : location.lon());
        Double lat = GuiUtils.parseCoordinate(location == null ? null : location.lat());
        Double lon = GuiUtils.parseCoordinate(location == null ? null : location.lon());

        // Header bar
        HBox headerBar = new HBox(16);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(16, 20, 16, 20));
        headerBar.getStyleClass().add("gv-detail-dialog-header");

        Label iconCircle = new Label("P");
        iconCircle.getStyleClass().add("gv-detail-dialog-icon-green");

        VBox headerText = new VBox(3);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        Label titleLabel = new Label(locationTitle);
        titleLabel.getStyleClass().add("gv-detail-dialog-title");
        Label subtitleLbl = new Label(fullAddr);
        subtitleLbl.getStyleClass().add("gv-detail-dialog-subtitle");
        subtitleLbl.setWrapText(false);
        headerText.getChildren().addAll(titleLabel, subtitleLbl);

        Label coordChip = new Label(coordText);
        coordChip.getStyleClass().add("gv-map-coord-chip");
        VBox coordBox = new VBox(coordChip);
        coordBox.setAlignment(Pos.CENTER);
        headerBar.getChildren().addAll(iconCircle, headerText, coordBox);

        // Map viewport
        StackPane mapViewport = new StackPane();
        mapViewport.setMinHeight(520);
        mapViewport.setPrefHeight(560);
        mapViewport.getStyleClass().add("gv-map-viewport");
        VBox.setVgrow(mapViewport, Priority.ALWAYS);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(mapViewport.widthProperty());
        clip.heightProperty().bind(mapViewport.heightProperty());
        mapViewport.setClip(clip);

        Pane tileLayer = new Pane();
        tileLayer.setPickOnBounds(false);
        tileLayer.prefWidthProperty().bind(mapViewport.widthProperty());
        tileLayer.prefHeightProperty().bind(mapViewport.heightProperty());
        tileLayer.setMinSize(0, 0);
        tileLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label mapStatusLabel = new Label("Chargement de la carte...");
        mapStatusLabel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        mapStatusLabel.getStyleClass().add("gv-map-status-label");
        mapStatusLabel.setVisible(false);
        mapStatusLabel.setMouseTransparent(true);

        Label  zoomLabel  = new Label("Z" + OSM_PREVIEW_INITIAL_ZOOM);
        Button zoomInBtn  = buildMapControlBtn("+");
        Button zoomOutBtn = buildMapControlBtn("-");

        Region zoomSep = new Region();
        zoomSep.setMinHeight(1); zoomSep.setPrefHeight(1); zoomSep.setMaxHeight(1);
        zoomSep.getStyleClass().add("gv-map-zoom-sep");

        VBox zoomBox = new VBox(0, zoomInBtn, zoomSep, zoomOutBtn);
        zoomBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        zoomBox.getStyleClass().add("gv-map-zoom-box");
        StackPane.setAlignment(zoomBox, Pos.TOP_LEFT);
        StackPane.setMargin(zoomBox, new Insets(14, 0, 0, 14));

        zoomLabel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        zoomLabel.getStyleClass().add("gv-map-zoom-label");
        StackPane.setAlignment(zoomLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(zoomLabel, new Insets(0, 0, 12, 14));

        Label attribution = new Label("OpenStreetMap contributors");
        attribution.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        attribution.getStyleClass().add("gv-map-attribution");
        StackPane.setAlignment(attribution, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(attribution, new Insets(0, 10, 10, 0));

        mapViewport.getChildren().addAll(tileLayer, mapStatusLabel, zoomBox, zoomLabel, attribution);
        StackPane.setAlignment(mapStatusLabel, Pos.CENTER);
        content.getChildren().addAll(headerBar, mapViewport);

        // Map state
        final boolean  hasCoords = lat != null && lon != null;
        final double   tgtLat    = hasCoords ? GuiUtils.clampLatitude(lat)  : 0.0;
        final double   tgtLon    = hasCoords ? GuiUtils.clampLongitude(lon) : 0.0;
        final double[] cLat      = {tgtLat};
        final double[] cLon      = {tgtLon};
        final int[]    zoom      = {OSM_PREVIEW_INITIAL_ZOOM};

        Runnable renderMap = () -> {
            tileLayer.getChildren().clear();
            if (!hasCoords) {
                mapStatusLabel.setText("Coordonnees indisponibles. Utilisez Itineraires.");
                mapStatusLabel.setVisible(true);
                zoomLabel.setText("Z-");
                return;
            }
            double w = Math.max(360, mapViewport.getWidth());
            double h = Math.max(280, mapViewport.getHeight());
            zoomLabel.setText("Z" + zoom[0]);
            double cpx = longitudeToWorldPixelX(cLon[0], zoom[0]);
            double cpy = latitudeToWorldPixelY(cLat[0], zoom[0]);
            double tlx = cpx - w / 2.0;
            double tly = cpy - h / 2.0;
            int tileCount = 1 << zoom[0];
            int x0 = (int) Math.floor(tlx / OSM_TILE_SIZE) - 1;
            int x1 = (int) Math.floor((tlx + w) / OSM_TILE_SIZE) + 1;
            int y0 = (int) Math.floor(tly / OSM_TILE_SIZE) - 1;
            int y1 = (int) Math.floor((tly + h) / OSM_TILE_SIZE) + 1;
            for (int ty = y0; ty <= y1; ty++) {
                if (ty < 0 || ty >= tileCount) continue;
                for (int tx = x0; tx <= x1; tx++) {
                    int wx = wrapTileIndex(tx, tileCount);
                    ImageView iv = new ImageView(getOsmTileImage(zoom[0], wx, ty));
                    iv.setFitWidth(OSM_TILE_SIZE); iv.setFitHeight(OSM_TILE_SIZE);
                    iv.setSmooth(true); iv.setPreserveRatio(false);
                    iv.setLayoutX(tx * OSM_TILE_SIZE - tlx);
                    iv.setLayoutY(ty * OSM_TILE_SIZE - tly);
                    tileLayer.getChildren().add(iv);
                }
            }
            double mpx = longitudeToWorldPixelX(tgtLon, zoom[0]);
            double mpy = latitudeToWorldPixelY(tgtLat, zoom[0]);
            Node marker = createMapMarker();
            marker.setMouseTransparent(true);
            marker.setLayoutX(mpx - tlx - 11);
            marker.setLayoutY(mpy - tly - 31);
            tileLayer.getChildren().add(marker);
            mapStatusLabel.setVisible(false);
        };

        final double[] dragX  = {0}, dragY  = {0};
        final double[] dragCX = {0}, dragCY = {0};

        mapViewport.setOnMousePressed(e -> {
            if (!hasCoords) return;
            dragX[0]  = e.getX(); dragY[0]  = e.getY();
            dragCX[0] = longitudeToWorldPixelX(cLon[0], zoom[0]);
            dragCY[0] = latitudeToWorldPixelY(cLat[0], zoom[0]);
            mapViewport.setCursor(javafx.scene.Cursor.CLOSED_HAND);
        });
        mapViewport.setOnMouseReleased(e -> mapViewport.setCursor(javafx.scene.Cursor.OPEN_HAND));
        mapViewport.setOnMouseDragged(e -> {
            if (!hasCoords) return;
            double ws = OSM_TILE_SIZE * (double)(1 << zoom[0]);
            cLon[0] = worldPixelXToLongitude(wrapPixelValue(dragCX[0] - (e.getX() - dragX[0]), ws), zoom[0]);
            cLat[0] = worldPixelYToLatitude(Math.max(0, Math.min(ws, dragCY[0] - (e.getY() - dragY[0]))), zoom[0]);
            renderMap.run();
        });
        mapViewport.setOnScroll(e -> {
            if (!hasCoords) return;
            if (e.getDeltaY() > 0 && zoom[0] < OSM_PREVIEW_MAX_ZOOM)  { zoom[0]++; renderMap.run(); }
            else if (e.getDeltaY() < 0 && zoom[0] > OSM_PREVIEW_MIN_ZOOM) { zoom[0]--; renderMap.run(); }
            e.consume();
        });
        mapViewport.setCursor(javafx.scene.Cursor.OPEN_HAND);

        zoomInBtn.setOnAction(e  -> { if (zoom[0] < OSM_PREVIEW_MAX_ZOOM)  { zoom[0]++; renderMap.run(); } });
        zoomOutBtn.setOnAction(e -> { if (zoom[0] > OSM_PREVIEW_MIN_ZOOM) { zoom[0]--; renderMap.run(); } });
        zoomInBtn.setDisable(!hasCoords);
        zoomOutBtn.setDisable(!hasCoords);

        PauseTransition resizeDebounce = new PauseTransition(Duration.millis(120));
        resizeDebounce.setOnFinished(e -> renderMap.run());
        mapViewport.widthProperty().addListener((o, ov, nv)  -> resizeDebounce.playFromStart());
        mapViewport.heightProperty().addListener((o, ov, nv) -> resizeDebounce.playFromStart());
        dialog.setOnShown(e -> renderMap.run());

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        pane.setPadding(Insets.EMPTY);
        pane.setMinWidth(860); pane.setPrefWidth(980);
        pane.setMinHeight(680);
        pane.getStyleClass().add("gv-detail-dialog-pane");

        java.net.URL cssUrl = getClass().getResource("/css/admin-hotel-style.css");
        if (cssUrl != null && !pane.getStylesheets().contains(cssUrl.toExternalForm()))
            pane.getStylesheets().add(cssUrl.toExternalForm());

        String googleMapsUrl = (lat != null && lon != null)
                ? "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon + "&travelmode=driving"
                : fallbackUrl;

        Node openBrowserNode = pane.lookupButton(openBrowserType);
        if (openBrowserNode instanceof Button openBrowserButton) {
            javafx.application.Platform.runLater(() -> {
                openBrowserButton.setMinWidth(165);
                openBrowserButton.getStyleClass().add("gv-itinerary-btn");
            });
            openBrowserButton.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
                openUrlInBrowser(googleMapsUrl);
                e.consume();
            });
        }
        Node closeNode = pane.lookupButton(ButtonType.CLOSE);
        if (closeNode instanceof Button closeButton) {
            javafx.application.Platform.runLater(() -> {
                closeButton.setMinWidth(110);
                closeButton.getStyleClass().add("gv-detail-close-btn");
            });
        }

        dialog.showAndWait();
    }

    private Button buildMapControlBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("gv-map-control-btn");
        return b;
    }

    // -----------------------------------------------------------------------
    // API STATE HELPERS
    // -----------------------------------------------------------------------

    public void updateApiControlsForView(boolean hotelsView, Button geoapifyApiButton) {
        boolean enable = hotelsView && !apiRequestInProgress;
        if (geoapifyApiButton != null) geoapifyApiButton.setDisable(!enable);
    }

    private void setApiLoadingState(boolean loading,
                                    ProgressIndicator apiLoadingIndicator,
                                    Button geoapifyApiButton) {
        apiRequestInProgress = loading;
        if (apiLoadingIndicator != null) {
            apiLoadingIndicator.setVisible(loading);
            apiLoadingIndicator.setManaged(loading);
        }
        updateApiControlsForView(!loading, geoapifyApiButton);
    }

    private void handleApiFailure(String provider, Throwable error) {
        DialogHelper.showNotification(
                provider + " API: " + GuiUtils.extractErrorMessage(error), "error", getClass());
    }

    // -----------------------------------------------------------------------
    // OSM TILE / MATH HELPERS
    // -----------------------------------------------------------------------

    private Image getOsmTileImage(int zoom, int tileX, int tileY) {
        String key    = zoom + "/" + tileX + "/" + tileY;
        Image  cached = osmTileCache.get(key);
        if (cached != null) return cached;
        String tileUrl = String.format(Locale.ROOT, OSM_TILE_URL_TEMPLATE, zoom, tileX, tileY);
        try {
            java.net.URL url  = new java.net.URL(tileUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "GoVacate/1.0 (contact@youremail.com)");
            conn.connect();
            java.io.InputStream is = conn.getInputStream();
            Image loaded = new Image(is);
            osmTileCache.put(key, loaded);
            is.close();
            conn.disconnect();
            return loaded;
        } catch (Exception e) {
            return new Image(tileUrl, true);
        }
    }

    private Node createMapMarker() {
        Circle top   = new Circle(8.5, Color.web("#3B8EDB"));
        top.setStroke(Color.web("#2E6FAE")); top.setStrokeWidth(1.0);
        top.setCenterX(10.0); top.setCenterY(10.0);
        Circle inner = new Circle(3.0, Color.WHITE);
        inner.setCenterX(10.0); inner.setCenterY(10.0);
        Polygon tail = new Polygon(10.0, 30.0, 5.2, 17.0, 14.8, 17.0);
        tail.setFill(Color.web("#3B8EDB")); tail.setStroke(Color.web("#2E6FAE")); tail.setStrokeWidth(1.0);
        Pane p = new Pane(tail, top, inner);
        p.setPrefSize(20, 31); p.setMinSize(20, 31); p.setMaxSize(20, 31);
        return p;
    }

    private String buildOpenStreetMapUrl(NominatimHotelApiClient.LocationSummary location) {
        if (location == null) return "N/A";
        String lat = location.lat() == null ? "" : location.lat().trim();
        String lon = location.lon() == null ? "" : location.lon().trim();
        if (!lat.isBlank() && !lon.isBlank())
            return "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon
                    + "#map=16/" + lat + "/" + lon;
        String type = location.osmType() == null ? "" : location.osmType().trim().toLowerCase(Locale.ROOT);
        if (location.osmId() > 0 && !type.isBlank()) {
            String prefix = switch (type) {
                case "node"     -> "node";
                case "way"      -> "way";
                case "relation" -> "relation";
                default         -> "";
            };
            if (!prefix.isBlank())
                return "https://www.openstreetmap.org/" + prefix + "/" + location.osmId();
        }
        return "N/A";
    }

    private String extractLocationTitle(String displayName) {
        String safe = GuiUtils.formatApiValue(displayName, "Lieu inconnu");
        String[] seg = safe.split(",");
        if (seg.length == 0) return safe;
        return GuiUtils.trimToMaxLength(seg[0].trim(), 36);
    }

    private void openUrlInBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                DialogHelper.showNotification("Ouverture navigateur non supportee.", "warning", getClass());
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            DialogHelper.showNotification(
                    "Impossible d'ouvrir la carte: " + GuiUtils.extractErrorMessage(e), "error", getClass());
        }
    }

    private double longitudeToWorldPixelX(double lon, int zoom) {
        return ((GuiUtils.clampLongitude(lon) + 180.0) / 360.0) * OSM_TILE_SIZE * (double)(1 << zoom);
    }

    private double latitudeToWorldPixelY(double lat, int zoom) {
        double latRad = Math.toRadians(GuiUtils.clampLatitude(lat));
        double ws     = OSM_TILE_SIZE * (double)(1 << zoom);
        return (1.0 - Math.log(Math.tan(Math.PI / 4.0 + latRad / 2.0)) / Math.PI) * ws / 2.0;
    }

    private double worldPixelXToLongitude(double px, int zoom) {
        return (px / (OSM_TILE_SIZE * (double)(1 << zoom))) * 360.0 - 180.0;
    }

    private double worldPixelYToLatitude(double py, int zoom) {
        double ws = OSM_TILE_SIZE * (double)(1 << zoom);
        double n  = Math.PI - (2.0 * Math.PI * Math.max(0, Math.min(ws, py)) / ws);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private int    wrapTileIndex(int v, int count)    { int    w = v % count; return w < 0 ? w + count : w; }
    private double wrapPixelValue(double v, double m) { double w = v % m;     return w < 0 ? w + m     : w; }
}
