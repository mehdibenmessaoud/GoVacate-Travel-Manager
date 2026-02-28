package tn.esprit.projet.gui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import tn.esprit.projet.API.hotels.GeoapifyPlacesApiClient;
import tn.esprit.projet.API.hotels.NominatimHotelApiClient;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Handles API-related interactions in the client view:
 *
 *  Providers:
 *   - OSM / Overpass  — free, no API key, hotel data from OpenStreetMap
 *   - Geoapify Places — free 3000/day, city name search, rich hotel data
 *   - Nominatim       — used only for in-app map tile preview
 *
 *  Best practices:
 *   - ExecutorService + AtomicReference<Task> for safe async & cancellation
 *   - Debounced live search (350 ms) on destination field
 *   - Geoapify hotel cards: category label, website, phone, opening hours
 *   - Background tile loading for OSM map preview (no FX-thread blocking)
 *   - Inline status pill feedback; popups reserved for hard errors only
 */
public class ClientApiViewController {

    // ── Shared state ──────────────────────────────────────────────────────────
    private final ClientSharedState  state;
    private final ClientDialogHelper dialogs;

    // ── FXML node refs ────────────────────────────────────────────────────────
    private Label             apiStatusLabel;
    private ProgressIndicator apiLoadingIndicator;
    private Button            geoapifyApiButton;
    private TextField         searchField;
    private TextField         apiSearchField;
    private Label             sectionTitle;
    private Label             subtitleLabel;
    private FlowPane          hotelsContainer;

    // ── Concurrency ───────────────────────────────────────────────────────────
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "gv-api-worker");
        t.setDaemon(true);
        return t;
    });
    private final AtomicReference<Task<?>> currentTask = new AtomicReference<>(null);

    // ── State ─────────────────────────────────────────────────────────────────
    private final Map<String, Image> osmTileCache = new HashMap<>();

    public ClientApiViewController(ClientSharedState state, ClientDialogHelper dialogs) {
        this.state   = state;
        this.dialogs = dialogs;
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setApiStatusLabel(Label l)                  { apiStatusLabel       = l; }
    public void setApiLoadingIndicator(ProgressIndicator p) { apiLoadingIndicator  = p; }
    public void setGeoapifyApiButton(Button b)              { geoapifyApiButton    = b; }
    public void setSearchField(TextField f)                 { searchField          = f; }
    public void setApiSearchField(TextField f)                 { apiSearchField = f; }
    public void setSectionTitle(Label l)                    { sectionTitle         = l; }
    public void setSubtitleLabel(Label l)                   { subtitleLabel        = l; }
    public void setHotelsContainer(FlowPane p)              { hotelsContainer      = p; }

    // ── Public handlers ───────────────────────────────────────────────────────

    public void handleGeoapifySearch(boolean isHotelsView) {
        if (!state.databaseAvailable) return;
        if (!ensureHotelsView(isHotelsView, "La recherche Geoapify est disponible uniquement en vue Hotels.")) return;
        resolveQueryAndRun(this::executeGeoapifySearchAsync,
                "Entrez une ville (ex: Paris, Rome, Tokyo).");
    }

    // ── Geoapify async task ──────────────────────────────────────────────────

    private void executeGeoapifySearchAsync(String query) {
        record GeoResult(String query, List<GeoapifyPlacesApiClient.HotelPlace> places) {}

        Task<GeoResult> task = new Task<>() {
            @Override protected GeoResult call() throws Exception {
                List<GeoapifyPlacesApiClient.HotelPlace> places =
                        state.hotelService.fetchGeoapifyHotels(query, 20);
                return new GeoResult(query, places);
            }
        };
        task.setOnSucceeded(ev -> {
            setApiLoadingState(false);
            GeoResult r = task.getValue();
            if (r == null) return;
            showGeoapifyResultsInInterface(r.query(), r.places());
        });
        task.setOnFailed(ev -> { setApiLoadingState(false); handleApiFailure("Geoapify", task.getException()); });
        task.setOnCancelled(ev -> setApiLoadingState(false));
        submitTask(task, "Geoapify en cours...");
    }

    private void submitTask(Task<?> newTask, String initialStatus) {
        Task<?> old = currentTask.getAndSet(newTask);
        if (old != null && old.isRunning()) old.cancel(true);
        setApiStatus(initialStatus, "loading");
        setApiLoadingState(true);
        executor.execute(newTask);
    }

    // ── Geoapify results UI ──────────────────────────────────────────────────

    private void showGeoapifyResultsInInterface(String query,
                                                List<GeoapifyPlacesApiClient.HotelPlace> places) {
        hotelsContainer.getChildren().clear();
        setApiFlowPaneMode();
        sectionTitle.setText("Hôtels Geoapify (" + places.size() + ")");
        subtitleLabel.setText("Résultats pour \"" + query + "\"");

        hotelsContainer.getChildren().add(buildApiSectionHeader(
                "GEOAPIFY", "🌍", "#0ea5e9", "rgba(14,165,233,0.12)",
                "Hôtels — \"" + query + "\"",
                places.size() + " résultat(s)  ·  Geoapify Places API  ·  Données OpenStreetMap"
        ));

        if (places.isEmpty()) {
            showEmptyState("🌍", "Aucun hôtel Geoapify trouvé",
                    "Vérifiez votre GEOAPIFY_API_KEY ou essayez une autre ville.");
            return;
        }
        int limit = Math.min(20, places.size());
        for (int i = 0; i < limit; i++) {
            hotelsContainer.getChildren().add(createGeoapifyCard(places.get(i), i));
        }
    }

    private StackPane createGeoapifyCard(GeoapifyPlacesApiClient.HotelPlace place, int rank) {
        StackPane card = new StackPane();
        card.getStyleClass().add("fsq-card");
        card.setMinWidth(290); card.setPrefWidth(290); card.setMaxWidth(290);
        card.setCursor(Cursor.HAND);
        applyRoundedClip(card, 20);

        VBox content = new VBox(0);
        content.setMinWidth(290); content.setPrefWidth(290); content.setMaxWidth(290);

        // ── Header (teal gradient) ────────────────────────────────────────
        StackPane header = new StackPane();
        header.setMinHeight(165); header.setPrefHeight(165); header.setMaxHeight(165);
        header.setStyle("-fx-background-color: linear-gradient(to bottom right, #0c1a26, #0e4460, #0ea5e9);");
        applyRoundedClip(header, 20);

        // Provider chip
        Label providerChip = new Label("🌍  Geoapify");
        providerChip.setStyle(
                "-fx-background-color: rgba(0,0,0,0.42);" +
                        "-fx-text-fill: #7dd3fc; -fx-font-size: 10px; -fx-font-weight: 800;" +
                        "-fx-padding: 5 12 5 10; -fx-background-radius: 0 0 10 0;");
        StackPane.setAlignment(providerChip, Pos.TOP_LEFT);
        header.getChildren().add(providerChip);

        // Rank badge
        String rankBg  = rank == 0 ? "#b45309" : rank == 1 ? "#475569" : rank == 2 ? "#92400e" : "rgba(0,0,0,0.40)";
        String rankTxt = rank == 0 ? "🥇 #1" : rank == 1 ? "🥈 #2" : rank == 2 ? "🥉 #3" : "#" + (rank + 1);
        Label rankBadge = new Label(rankTxt);
        rankBadge.setStyle(
                "-fx-background-color: " + rankBg + ";" +
                        "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 800;" +
                        "-fx-padding: 4 10; -fx-background-radius: 0 0 0 10;");
        StackPane.setAlignment(rankBadge, Pos.TOP_RIGHT);
        header.getChildren().add(rankBadge);

        // Category pill (center)
        Label catPill = new Label("🏨 " + place.categoryLabel());
        catPill.setStyle(
                "-fx-background-color: rgba(14,165,233,0.30);" +
                        "-fx-text-fill: #e0f2fe; -fx-font-size: 12px; -fx-font-weight: 700;" +
                        "-fx-padding: 6 16; -fx-background-radius: 20;" +
                        "-fx-border-color: rgba(125,211,252,0.40); -fx-border-radius: 20; -fx-border-width: 1;");
        StackPane.setAlignment(catPill, Pos.CENTER);
        header.getChildren().add(catPill);

        // Name + location bottom-left
        VBox heroBox = new VBox(4);
        heroBox.setAlignment(Pos.BOTTOM_LEFT);
        heroBox.setPadding(new Insets(0, 14, 12, 14));
        heroBox.setMouseTransparent(true);

        Label heroName = new Label(place.name());
        heroName.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.70), 8, 0, 0, 2);");
        heroName.setWrapText(true); heroName.setMaxWidth(262);

        String location = place.locationLine();
        if (!location.isBlank()) {
            Label locLabel = new Label("📍 " + location);
            locLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(186,230,253,0.90); -fx-font-weight: 500;");
            heroBox.getChildren().addAll(heroName, locLabel);
        } else {
            heroBox.getChildren().add(heroName);
        }
        StackPane.setAlignment(heroBox, Pos.BOTTOM_LEFT);
        header.getChildren().add(heroBox);

        // ── Body ─────────────────────────────────────────────────────────
        VBox body = new VBox(0);
        body.setStyle("-fx-background-color: #ffffff;");
        body.setMinWidth(290); body.setPrefWidth(290); body.setMaxWidth(290);
        body.setPadding(new Insets(12, 14, 0, 14));

        // Address
        if (!place.address().isBlank()) {
            Label addrLabel = new Label("📍 " + place.address());
            addrLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");
            addrLabel.setWrapText(false); addrLabel.setMaxWidth(262);
            body.getChildren().add(addrLabel);
        }

        // Chips row: website + phone
        HBox chipRow = new HBox(8);
        chipRow.setPadding(new Insets(7, 0, 0, 0));
        chipRow.setAlignment(Pos.CENTER_LEFT);

        if (!place.website().isBlank()) {
            Label wLabel = new Label("🌐 Site web");
            wLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #0369a1;" +
                    "-fx-background-color: #e0f2fe; -fx-background-radius: 7;" +
                    "-fx-border-color: #7dd3fc; -fx-border-radius: 7; -fx-border-width: 1;" +
                    "-fx-padding: 3 9; -fx-cursor: hand;");
            wLabel.setOnMouseClicked(e -> openUrlInBrowser(place.website()));
            chipRow.getChildren().add(wLabel);
        }
        if (!place.phone().isBlank()) {
            Label pLabel = new Label("📞 " + place.phone());
            pLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #374151;");
            chipRow.getChildren().add(pLabel);
        }
        if (!chipRow.getChildren().isEmpty()) body.getChildren().add(chipRow);

        // Opening hours
        if (!place.openingHours().isBlank()) {
            Label hoursLabel = new Label("🕒 " + place.openingHours());
            hoursLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #059669; -fx-font-weight: 600;");
            hoursLabel.setPadding(new Insets(5, 0, 0, 0));
            body.getChildren().add(hoursLabel);
        }

        Region bodySpacer = new Region(); VBox.setVgrow(bodySpacer, Priority.ALWAYS);
        body.getChildren().add(bodySpacer);

        // Actions
        HBox actions = new HBox(8);
        actions.setPadding(new Insets(10, 0, 12, 0));
        actions.setAlignment(Pos.CENTER);

        Button detailBtn = styledButton("Détails", true, "#0ea5e9", "#0284c7");
        detailBtn.setMinWidth(118); detailBtn.setPrefWidth(118); detailBtn.setMinHeight(38);
        detailBtn.setOnAction(e -> showGeoapifyDetailDialog(place));

        Button mapBtn = styledButton("🗺  Carte", false, "#0ea5e9", "#0284c7");
        mapBtn.setMinWidth(108); mapBtn.setPrefWidth(108); mapBtn.setMinHeight(38);
        mapBtn.setDisable(!place.hasCoords());
        mapBtn.setOnAction(e -> showMapForCoords(place.lat(), place.lon(), place.name()));

        actions.getChildren().addAll(detailBtn, mapBtn);
        body.getChildren().add(actions);

        content.getChildren().addAll(header, body);
        card.getChildren().add(content);
        return card;
    }


    // ── Geoapify detail dialog ────────────────────────────────────────────────

    private void showGeoapifyDetailDialog(GeoapifyPlacesApiClient.HotelPlace place) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + place.name());
        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f1923;");

        HBox hbar = new HBox(14);
        hbar.setAlignment(Pos.CENTER_LEFT);
        hbar.setPadding(new Insets(18, 24, 18, 24));
        hbar.setStyle("-fx-background-color: #141f2e;" +
                "-fx-border-color: transparent transparent rgba(255,255,255,0.08) transparent;" +
                "-fx-border-width: 0 0 1 0;");
        Label iconLbl = new Label("🌍");
        iconLbl.setStyle("-fx-font-size: 18px; -fx-background-color: rgba(14,165,233,0.18);" +
                "-fx-background-radius: 50%; -fx-padding: 10; -fx-min-width: 42; -fx-min-height: 42; -fx-alignment: center;");
        VBox titleBox = new VBox(3); HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label titleLbl = new Label(place.name());
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label subLbl = new Label("Geoapify Places API  ·  " + place.categoryLabel());
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.38);");
        titleBox.getChildren().addAll(titleLbl, subLbl);
        hbar.getChildren().addAll(iconLbl, titleBox);

        VBox body = new VBox(0);
        body.setPadding(new Insets(20, 26, 24, 26));
        String[][] rows = {
                {"🏨", "Type",        place.categoryLabel().isBlank() ? "N/A" : place.categoryLabel()},
                {"📍", "Adresse",     place.address().isBlank() ? "N/A" : place.address()},
                {"🏙", "Ville",       place.locationLine().isBlank() ? "N/A" : place.locationLine()},
                {"📮", "Code postal", place.postcode().isBlank() ? "N/A" : place.postcode()},
                {"🌐", "Site web",    place.website().isBlank() ? "N/A" : place.website()},
                {"📞", "Téléphone",   place.phone().isBlank() ? "N/A" : place.phone()},
                {"🕒", "Horaires",    place.openingHours().isBlank() ? "N/A" : place.openingHours()},
                {"📍", "Lat / Lon",   place.hasCoords() ? place.latStr() + " / " + place.lonStr() : "N/A"},
                {"🔗", "Source",      "Geoapify Places API (données OpenStreetMap)"},
        };
        for (String[] row : rows) body.getChildren().add(detailRow(row[0], row[1], row[2]));

        if (place.hasCoords()) {
            Button mapBtn = new Button("🗺  Voir sur la carte");
            mapBtn.setStyle("-fx-background-color: rgba(14,165,233,0.18); -fx-text-fill: #7dd3fc;" +
                    "-fx-font-weight: 700; -fx-font-size: 12px; -fx-background-radius: 10; -fx-padding: 10 20;" +
                    "-fx-border-color: rgba(14,165,233,0.35); -fx-border-radius: 10; -fx-border-width: 1;" +
                    "-fx-cursor: hand;");
            mapBtn.setOnAction(e -> { dialog.close(); showMapForCoords(place.lat(), place.lon(), place.name()); });
            HBox mapRow = new HBox(mapBtn); mapRow.setPadding(new Insets(14, 0, 0, 0));
            body.getChildren().add(mapRow);
        }

        root.getChildren().addAll(hbar, body);
        DialogPane pane = dialog.getDialogPane();
        pane.setContent(root); pane.setPadding(Insets.EMPTY);
        pane.setStyle("-fx-padding: 0; -fx-background-color: #0f1923;");
        pane.setMinWidth(520); pane.setPrefWidth(560); pane.setMaxWidth(620);

        java.net.URL cssUrl = getClass().getResource("/css/client-style.css");
        if (cssUrl != null && !pane.getStylesheets().contains(cssUrl.toExternalForm()))
            pane.getStylesheets().add(cssUrl.toExternalForm());

        styleCloseButton(pane.lookupButton(closeType));
        dialog.showAndWait();
    }


    // ── OSM map preview ─────────────────────────────────────────────────────────

    public void showMapForCoords(double lat, double lon, String locationName) {
        // Wrap in a minimal NominatimHotelApiClient.LocationSummary for the map dialog
        NominatimHotelApiClient.LocationSummary loc = new NominatimHotelApiClient.LocationSummary(
                locationName, String.valueOf(lat), String.valueOf(lon),
                "node", 0, "", "", 1.0
        );
        showOpenStreetMapPreviewDialog(loc, buildOpenStreetMapUrl(loc));
    }

    public void showOpenStreetMapPreviewDialog(NominatimHotelApiClient.LocationSummary location,
                                               String fallbackUrl) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Aperçu carte — " + ClientUtils.extractLocationTitle(location == null ? null : location.displayName()));
        dialog.setResizable(true);

        ButtonType openBrowserType = new ButtonType("🧭  Itinéraires", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(openBrowserType, ButtonType.CLOSE);

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color: #0f1923;");

        String displayName   = location == null ? null : location.displayName();
        String locationTitle = ClientUtils.extractLocationTitle(displayName);
        String fullAddr      = ClientUtils.trimToMaxLength(ClientUtils.formatApiValue(displayName, "N/A"), 100);
        String coordText     = ClientUtils.formatCoordinates(
                location == null ? null : location.lat(),
                location == null ? null : location.lon());
        Double lat = ClientUtils.parseCoordinate(location == null ? null : location.lat());
        Double lon = ClientUtils.parseCoordinate(location == null ? null : location.lon());

        // Header bar
        HBox headerBar = new HBox(16);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(16, 20, 16, 20));
        headerBar.setStyle("-fx-background-color: #141f2e;" +
                "-fx-border-color: transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width: 0 0 1 0;");
        Label iconCircle = new Label("📍");
        iconCircle.setStyle("-fx-font-size: 16px; -fx-background-color: rgba(79,209,179,0.12);" +
                "-fx-background-radius: 50%; -fx-padding: 8; -fx-min-width: 38; -fx-min-height: 38; -fx-alignment: center;");
        VBox headerText = new VBox(3); HBox.setHgrow(headerText, Priority.ALWAYS);
        Label titleLabel = new Label(locationTitle);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");
        Label subtitleLbl = new Label(fullAddr);
        subtitleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.45);");
        headerText.getChildren().addAll(titleLabel, subtitleLbl);
        Label coordChip = new Label(coordText);
        coordChip.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #4fd1b3;" +
                "-fx-background-color: rgba(79,209,179,0.10); -fx-background-radius: 20; -fx-padding: 6 14;" +
                "-fx-border-color: rgba(79,209,179,0.28); -fx-border-radius: 20; -fx-border-width: 1;");
        headerBar.getChildren().addAll(iconCircle, headerText, new VBox(coordChip));

        // Map viewport
        StackPane mapViewport = new StackPane();
        mapViewport.setMinHeight(520); mapViewport.setPrefHeight(560);
        mapViewport.setStyle("-fx-background-color: #1a2a3a;");
        VBox.setVgrow(mapViewport, Priority.ALWAYS);
        Rectangle vClip = new Rectangle();
        vClip.widthProperty().bind(mapViewport.widthProperty());
        vClip.heightProperty().bind(mapViewport.heightProperty());
        mapViewport.setClip(vClip);

        Pane tileLayer = new Pane();
        tileLayer.setStyle("-fx-background-color: transparent;");
        tileLayer.setPickOnBounds(false);
        tileLayer.prefWidthProperty().bind(mapViewport.widthProperty());
        tileLayer.prefHeightProperty().bind(mapViewport.heightProperty());
        tileLayer.setMinSize(0, 0); tileLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label mapStatusLabel = new Label("Chargement de la carte...");
        mapStatusLabel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        mapStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.55);" +
                "-fx-background-color: rgba(15,25,35,0.75); -fx-background-radius: 10; -fx-padding: 10 20;");
        mapStatusLabel.setVisible(false); mapStatusLabel.setMouseTransparent(true);

        Label zoomLabel = new Label("Z" + ClientUtils.OSM_PREVIEW_INITIAL_ZOOM);
        Button zoomInBtn = mapControlBtn("+"), zoomOutBtn = mapControlBtn("−");
        Region zoomSep = new Region();
        zoomSep.setMinHeight(1); zoomSep.setPrefHeight(1); zoomSep.setMaxHeight(1);
        zoomSep.setStyle("-fx-background-color: rgba(255,255,255,0.13);");
        VBox zoomBox = new VBox(0, zoomInBtn, zoomSep, zoomOutBtn);
        zoomBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        zoomBox.setStyle("-fx-background-color: rgba(15,25,35,0.92); -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 10; -fx-border-width: 1;");
        StackPane.setAlignment(zoomBox, Pos.TOP_LEFT); StackPane.setMargin(zoomBox, new Insets(14, 0, 0, 14));

        zoomLabel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        zoomLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: rgba(255,255,255,0.85);" +
                "-fx-background-color: rgba(15,25,35,0.85); -fx-background-radius: 8; -fx-padding: 4 10;" +
                "-fx-border-color: rgba(255,255,255,0.15); -fx-border-radius: 8; -fx-border-width: 1;");
        StackPane.setAlignment(zoomLabel, Pos.BOTTOM_LEFT); StackPane.setMargin(zoomLabel, new Insets(0, 0, 12, 14));

        Label attribution = new Label("© OpenStreetMap contributors");
        attribution.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        attribution.setStyle("-fx-font-size: 10px; -fx-text-fill: rgba(255,255,255,0.55);" +
                "-fx-background-color: rgba(15,25,35,0.75); -fx-background-radius: 4; -fx-padding: 3 8;");
        StackPane.setAlignment(attribution, Pos.BOTTOM_RIGHT); StackPane.setMargin(attribution, new Insets(0, 10, 10, 0));

        mapViewport.getChildren().addAll(tileLayer, mapStatusLabel, zoomBox, zoomLabel, attribution);
        StackPane.setAlignment(mapStatusLabel, Pos.CENTER);
        content.getChildren().addAll(headerBar, mapViewport);

        final boolean hasCoords = lat != null && lon != null;
        final double targetLat = hasCoords ? ClientUtils.clampLatitude(lat) : 0.0;
        final double targetLon = hasCoords ? ClientUtils.clampLongitude(lon) : 0.0;
        final double[] centerLat = {targetLat}, centerLon = {targetLon};
        final int[] zoom = {ClientUtils.OSM_PREVIEW_INITIAL_ZOOM};

        Runnable renderMap = () -> {
            tileLayer.getChildren().clear();
            if (!hasCoords) { mapStatusLabel.setText("Coordonnées indisponibles."); mapStatusLabel.setVisible(true); return; }
            double w = Math.max(360, mapViewport.getWidth()), h = Math.max(280, mapViewport.getHeight());
            zoomLabel.setText("Z" + zoom[0]);
            double cx = ClientUtils.longitudeToWorldPixelX(centerLon[0], zoom[0]);
            double cy = ClientUtils.latitudeToWorldPixelY(centerLat[0], zoom[0]);
            double tlX = cx - w / 2.0, tlY = cy - h / 2.0;
            int tc = 1 << zoom[0];
            for (int ty = (int)Math.floor(tlY / ClientUtils.OSM_TILE_SIZE) - 1; ty <= (int)Math.floor((tlY + h) / ClientUtils.OSM_TILE_SIZE) + 1; ty++) {
                if (ty < 0 || ty >= tc) continue;
                for (int tx = (int)Math.floor(tlX / ClientUtils.OSM_TILE_SIZE) - 1; tx <= (int)Math.floor((tlX + w) / ClientUtils.OSM_TILE_SIZE) + 1; tx++) {
                    loadTileAsync(zoom[0], ClientUtils.wrapTileIndex(tx, tc), ty, tx * ClientUtils.OSM_TILE_SIZE - tlX, ty * ClientUtils.OSM_TILE_SIZE - tlY, tileLayer);
                }
            }
            double mx = ClientUtils.longitudeToWorldPixelX(targetLon, zoom[0]);
            double my = ClientUtils.latitudeToWorldPixelY(targetLat, zoom[0]);
            Node marker = createMapMarker();
            marker.setMouseTransparent(true);
            marker.setLayoutX(mx - tlX - 11); marker.setLayoutY(my - tlY - 31);
            tileLayer.getChildren().add(marker);
            mapStatusLabel.setVisible(false);
        };

        final double[] dragAX = {0}, dragAY = {0}, dragCX = {0}, dragCY = {0};
        mapViewport.setOnMousePressed(ev -> { if (!hasCoords) return; dragAX[0]=ev.getX(); dragAY[0]=ev.getY(); dragCX[0]=ClientUtils.longitudeToWorldPixelX(centerLon[0],zoom[0]); dragCY[0]=ClientUtils.latitudeToWorldPixelY(centerLat[0],zoom[0]); mapViewport.setCursor(javafx.scene.Cursor.CLOSED_HAND); });
        mapViewport.setOnMouseReleased(ev -> mapViewport.setCursor(javafx.scene.Cursor.OPEN_HAND));
        mapViewport.setOnMouseDragged(ev -> { if (!hasCoords) return; double ws=ClientUtils.OSM_TILE_SIZE*(double)(1<<zoom[0]); centerLon[0]=ClientUtils.worldPixelXToLongitude(ClientUtils.wrapPixelValue(dragCX[0]-(ev.getX()-dragAX[0]),ws),zoom[0]); centerLat[0]=ClientUtils.worldPixelYToLatitude(Math.max(0,Math.min(ws,dragCY[0]-(ev.getY()-dragAY[0]))),zoom[0]); renderMap.run(); });
        mapViewport.setOnScroll(ev -> { if (!hasCoords) return; if (ev.getDeltaY()>0&&zoom[0]<ClientUtils.OSM_PREVIEW_MAX_ZOOM){zoom[0]++;renderMap.run();}else if(ev.getDeltaY()<0&&zoom[0]>ClientUtils.OSM_PREVIEW_MIN_ZOOM){zoom[0]--;renderMap.run();} ev.consume(); });
        mapViewport.setCursor(javafx.scene.Cursor.OPEN_HAND);
        zoomInBtn.setOnAction(ev -> { if(zoom[0]<ClientUtils.OSM_PREVIEW_MAX_ZOOM){zoom[0]++;renderMap.run();} });
        zoomOutBtn.setOnAction(ev -> { if(zoom[0]>ClientUtils.OSM_PREVIEW_MIN_ZOOM){zoom[0]--;renderMap.run();} });
        zoomInBtn.setDisable(!hasCoords); zoomOutBtn.setDisable(!hasCoords);

        PauseTransition debounce = new PauseTransition(Duration.millis(120));
        debounce.setOnFinished(ev -> renderMap.run());
        mapViewport.widthProperty().addListener((o,a,b)->debounce.playFromStart());
        mapViewport.heightProperty().addListener((o,a,b)->debounce.playFromStart());
        dialog.setOnShown(ev -> renderMap.run());

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content); pane.setPadding(Insets.EMPTY);
        pane.setMinWidth(860); pane.setPrefWidth(900); pane.setMinHeight(680);
        URL cssUrl = getClass().getResource("/css/client-style.css");
        if (cssUrl != null && !pane.getStylesheets().contains(cssUrl.toExternalForm()))
            pane.getStylesheets().add(cssUrl.toExternalForm());
        pane.getStyleClass().add("gv-map-dialog"); pane.setStyle("-fx-padding: 0;");

        Double latVal = lat, lonVal = lon;
        String googleMapsUrl = (latVal != null && lonVal != null)
                ? "https://www.google.com/maps/dir/?api=1&destination=" + latVal + "," + lonVal + "&travelmode=driving"
                : fallbackUrl;

        Node openBrowserNode = pane.lookupButton(openBrowserType);
        if (openBrowserNode instanceof Button ob) {
            Platform.runLater(() -> { ob.setMinWidth(165); ob.setStyle("-fx-background-color: linear-gradient(to right, #FF8210, #ffaa44);" + "-fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 13px; -fx-background-radius: 10; -fx-padding: 11 24; -fx-cursor: hand;"); });
            ob.addEventFilter(ActionEvent.ACTION, ev -> openUrlInBrowser(googleMapsUrl));
        }
        styleCloseButton(pane.lookupButton(ButtonType.CLOSE));
        dialog.showAndWait();
    }

    // ── OSM tile loading ──────────────────────────────────────────────────────

    private void loadTileAsync(int zoom, int tileX, int tileY, double lx, double ly, Pane tileLayer) {
        String key = zoom + "/" + tileX + "/" + tileY;
        Image cached = osmTileCache.get(key);
        if (cached != null) { tileLayer.getChildren().add(tileImageView(cached, lx, ly)); return; }
        ImageView placeholder = tileImageView(null, lx, ly);
        tileLayer.getChildren().add(placeholder);
        String url = String.format(Locale.ROOT, ClientUtils.OSM_TILE_URL_TEMPLATE, zoom, tileX, tileY);
        Image img = new Image(url, ClientUtils.OSM_TILE_SIZE, ClientUtils.OSM_TILE_SIZE, true, true, true);
        img.progressProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() >= 1.0 && !img.isError()) {
                osmTileCache.put(key, img);
                Platform.runLater(() -> { placeholder.setImage(img); placeholder.setStyle(null); });
            }
        });
        if (img.getProgress() >= 1.0 && !img.isError()) { osmTileCache.put(key, img); placeholder.setImage(img); }
    }

    private ImageView tileImageView(Image img, double lx, double ly) {
        ImageView v = new ImageView(img);
        v.setFitWidth(ClientUtils.OSM_TILE_SIZE); v.setFitHeight(ClientUtils.OSM_TILE_SIZE);
        v.setSmooth(true); v.setPreserveRatio(false);
        v.setLayoutX(lx); v.setLayoutY(ly);
        return v;
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public void setApiStatus(String message, String state) {
        if (apiStatusLabel == null) return;
        String text = (message == null || message.trim().isEmpty()) ? "Prêt" : message.trim();
        apiStatusLabel.setText(text);
        apiStatusLabel.getStyleClass().removeAll("api-status-idle","api-status-loading","api-status-success","api-status-error");
        if (!apiStatusLabel.getStyleClass().contains("api-status-pill")) apiStatusLabel.getStyleClass().add("api-status-pill");
        apiStatusLabel.getStyleClass().add(switch (safe(state, "")) {
            case "loading" -> "api-status-loading";
            case "success" -> "api-status-success";
            case "error"   -> "api-status-error";
            default        -> "api-status-idle";
        });
    }

    public void setApiLoadingState(boolean loading) {
        if (apiLoadingIndicator != null) { apiLoadingIndicator.setVisible(loading); apiLoadingIndicator.setManaged(loading); }
        updateApiButtonState(this.state.databaseAvailable, true);
    }

    public void updateApiButtonState(boolean dbAvailable, boolean hotelsView) {
        boolean enable = dbAvailable && hotelsView;
        if (geoapifyApiButton != null) geoapifyApiButton.setDisable(!enable);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean ensureHotelsView(boolean isHotelsView, String warn) {
        if (isHotelsView) return true;
        setApiStatus("Vue Hotels requise pour les APIs", "idle");
        dialogs.showWarning("API", warn);
        return false;
    }

    private void resolveQueryAndRun(Consumer<String> action, String emptyMsg) {
        TextField pref = (apiSearchField != null && !trimmed(apiSearchField.getText()).isEmpty())
                ? apiSearchField : searchField;
        String q = pref == null ? "" : trimmed(pref.getText());
        if (q.isBlank()) { dialogs.showWarning("API", emptyMsg); return; }
        action.accept(q);
    }

    private void handleApiFailure(String provider, Throwable error) {
        String msg = ClientUtils.extractErrorMessage(error);
        setApiStatus(provider + " — " + msg, "error");
        if (!(error instanceof IllegalArgumentException)) dialogs.showError(provider, msg);
    }

    private void showEmptyState(String icon, String title, String sub) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER); box.setPadding(new Insets(50));
        Label i = new Label(icon); i.setStyle("-fx-font-size: 48px;");
        Label t = new Label(title); t.setStyle("-fx-font-size: 18px; -fx-text-fill: rgba(255,255,255,0.6);");
        Label s = new Label(sub);   s.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.4);");
        box.getChildren().addAll(i, t, s);
        hotelsContainer.getChildren().add(box);
    }

    private HBox buildApiSectionHeader(String provider, String icon, String accentColor,
                                       String bgColor, String title, String subtitle) {
        HBox bar = new HBox(0);
        bar.setMinWidth(Double.MAX_VALUE); bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("api-section-header");
        Region accent = new Region();
        accent.setPrefWidth(5); accent.setMinWidth(5); accent.setMaxWidth(5);
        accent.setStyle("-fx-background-color: " + accentColor + "; -fx-background-radius: 3 0 0 3;");
        VBox textArea = new VBox(4);
        textArea.setPadding(new Insets(14, 20, 14, 18)); HBox.setHgrow(textArea, Priority.ALWAYS);
        HBox titleRow = new HBox(10); titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon); iconLbl.setStyle("-fx-font-size: 18px;");
        Label titleLbl = new Label(title); titleLbl.getStyleClass().add("api-section-title");
        titleRow.getChildren().addAll(iconLbl, titleLbl);
        Label subLbl = new Label(subtitle); subLbl.getStyleClass().add("api-section-subtitle");
        textArea.getChildren().addAll(titleRow, subLbl);
        Label pill = new Label(provider);
        pill.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + accentColor + ";" +
                "-fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 5 14; -fx-background-radius: 20;" +
                "-fx-border-color: " + accentColor + "; -fx-border-width: 1.5; -fx-border-radius: 20;");
        VBox pillBox = new VBox(pill); pillBox.setAlignment(Pos.CENTER_RIGHT);
        pillBox.setPadding(new Insets(0, 20, 0, 0));
        bar.getChildren().addAll(accent, textArea, pillBox);
        return bar;
    }

    private Node createMapMarker() {
        Circle top = new Circle(8.5, Color.web("#3B8EDB"));
        top.setStroke(Color.web("#2E6FAE")); top.setStrokeWidth(1.0); top.setCenterX(10.0); top.setCenterY(10.0);
        Circle inner = new Circle(3.0, Color.WHITE); inner.setCenterX(10.0); inner.setCenterY(10.0);
        Polygon tail = new Polygon(10.0, 30.0, 5.2, 17.0, 14.8, 17.0);
        tail.setFill(Color.web("#3B8EDB")); tail.setStroke(Color.web("#2E6FAE")); tail.setStrokeWidth(1.0);
        Pane marker = new Pane(tail, top, inner);
        marker.setPrefSize(20, 31); marker.setMinSize(20, 31); marker.setMaxSize(20, 31);
        return marker;
    }

    private Button mapControlBtn(String text) {
        Button b = new Button(text); b.getStyleClass().add("gv-map-control-btn"); return b;
    }

    private void applyRoundedClip(Region region, double arc) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(arc); clip.setArcHeight(arc);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    private void setApiFlowPaneMode() {
        if (hotelsContainer == null) return;
        hotelsContainer.setHgap(22); hotelsContainer.setVgap(26);
        hotelsContainer.setPrefWrapLength(1080); hotelsContainer.setAlignment(Pos.TOP_CENTER);
    }

    public String buildOpenStreetMapUrl(NominatimHotelApiClient.LocationSummary location) {
        if (location == null) return "N/A";
        String lat = location.lat() == null ? "" : location.lat().trim();
        String lon = location.lon() == null ? "" : location.lon().trim();
        if (!lat.isBlank() && !lon.isBlank())
            return "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=16/" + lat + "/" + lon;
        String type = location.osmType() == null ? "" : location.osmType().trim().toLowerCase(Locale.ROOT);
        type = switch (type) { case "n","node" -> "node"; case "w","way" -> "way"; case "r","relation" -> "relation"; default -> ""; };
        if (type.isEmpty() || location.osmId() <= 0) return "N/A";
        return "https://www.openstreetmap.org/" + type + "/" + location.osmId();
    }

    private void openUrlInBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                dialogs.showWarning("Navigateur", "Ouverture navigateur non supportée."); return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) { dialogs.showError("Navigateur", ClientUtils.extractErrorMessage(e)); }
    }




    // ── Small UI factories ────────────────────────────────────────────────────

    /** Styled button: filled (primary=true) or outlined. */
    private Button styledButton(String text, boolean filled, String mainColor, String hoverColor) {
        Button b = new Button(text);
        String base = filled
                ? "-fx-background-color: " + mainColor + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: 700; -fx-font-size: 12px; -fx-padding: 9 0; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: " + mainColor + "; -fx-background-radius: 10; -fx-border-color: " + mainColor + "44; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-font-weight: 700; -fx-font-size: 12px; -fx-padding: 8 0; -fx-cursor: hand;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace(mainColor, hoverColor)));
        b.setOnMouseExited(e -> b.setStyle(base));
        return b;
    }

    /** Colored info chip (e.g. room type, board type). */
    private Label infoChip(String text, String bg, String border) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #374151;" +
                "-fx-background-color: " + bg + "; -fx-background-radius: 7;" +
                "-fx-border-color: " + border + "; -fx-border-radius: 7; -fx-border-width: 1;" +
                "-fx-padding: 3 9;");
        return l;
    }

    /** Detail dialog row (icon | key | value). */
    private HBox detailRow(String icon, String key, String value) {
        HBox r = new HBox(0); r.setAlignment(Pos.CENTER_LEFT); r.setPadding(new Insets(11, 0, 11, 0));
        r.setStyle("-fx-border-color: transparent transparent rgba(255,255,255,0.05) transparent; -fx-border-width: 0 0 1 0;");
        Label ic = new Label(icon); ic.setStyle("-fx-font-size: 14px; -fx-min-width: 26;");
        Label k  = new Label(key + " :"); k.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.40); -fx-font-weight: 600; -fx-min-width: 110;");
        Label v  = new Label(value); v.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.85); -fx-font-weight: 600;");
        v.setWrapText(true); HBox.setHgrow(v, Priority.ALWAYS);
        r.getChildren().addAll(ic, k, v);
        return r;
    }

    private void styleCloseButton(Node n) {
        if (!(n instanceof Button cb)) return;
        Platform.runLater(() -> {
            cb.setStyle("-fx-background-color: rgba(255,255,255,0.07); -fx-text-fill: rgba(255,255,255,0.75);" +
                    "-fx-font-weight: 700; -fx-font-size: 13px; -fx-background-radius: 10; -fx-padding: 10 26;" +
                    "-fx-border-color: rgba(255,255,255,0.13); -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;");
            cb.setMinWidth(100);
        });
    }

    private String priceTierDesc(int tier) {
        return switch (tier) { case 1 -> "Budget"; case 2 -> "Modéré"; case 3 -> "Haut de gamme"; case 4 -> "Luxe"; default -> ""; };
    }

    private String formatPrice(String raw) {
        if (raw == null || raw.isBlank()) return "N/A";
        try { double v = Double.parseDouble(raw.trim()); return v == Math.floor(v) ? String.format(Locale.ROOT, "%.0f", v) : String.format(Locale.ROOT, "%.2f", v); }
        catch (NumberFormatException e) { return raw.trim(); }
    }

    private static String trimmed(String s) { return s == null ? "" : s.trim(); }
    private static String safe(String v, String fb) { return v == null || v.isBlank() ? fb : v; }
}