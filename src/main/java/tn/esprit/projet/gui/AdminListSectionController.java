package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * FXML controller for HotelView.fxml.
 * Holds @FXML-injected nodes so AdminController
 * no longer needs to walk the tree manually.
 */
public class AdminListSectionController {

    @FXML public Label     pageTitle;
    @FXML public Label     pageSubtitle;
    @FXML public Label     sectionTitle;
    @FXML public Label     searchPanelTitle;
    @FXML public TextField searchField;
    @FXML public VBox      filterContainer;
    @FXML public Label     filterLabel;
    @FXML public ComboBox<String> starsFilterCombo;
    @FXML public ComboBox<String> hotelFilterCombo;
    @FXML public Pane      roomFilterSeparator;
    @FXML public VBox      roomTypeFilterContainer;
    @FXML public Label     roomTypeFilterLabel;
    @FXML public ComboBox<String> roomTypeFilterCombo;
    @FXML public VBox      roomStatusFilterContainer;
    @FXML public Label     roomStatusFilterLabel;
    @FXML public ComboBox<String> roomStatusFilterCombo;
    @FXML public Button    searchButton;
    @FXML public Button    addButton;
    @FXML public VBox      apiToolbar;
    @FXML public Button    geoapifyApiButton;
    @FXML public Button    nominatimApiButton;
    @FXML public TextField apiSearchField;
    @FXML public ProgressIndicator apiLoadingIndicator;
    @FXML public Label     apiStatusLabel;
    @SuppressWarnings("rawtypes")
    @FXML public TableView mainTable;
    @FXML public TableColumn col1;
    @FXML public TableColumn col2;
    @FXML public TableColumn col3;
    @FXML public TableColumn col4;
    @FXML public TableColumn col5;
    @FXML public TableColumn col6;
}
