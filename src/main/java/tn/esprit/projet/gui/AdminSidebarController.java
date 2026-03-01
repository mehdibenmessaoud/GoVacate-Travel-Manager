package tn.esprit.projet.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * FXML controller for AdminSidebar.fxml.
 * Holds @FXML-injected sidebar nodes so AdminController
 * no longer needs to walk the tree manually.
 */
public class AdminSidebarController {

    @FXML public Pane   slidingPane;
    @FXML public VBox   menuContainer;
    @FXML public Button btnHotels;
    @FXML public Button btnChambres;
    @FXML public Button btnReservations;
    @FXML public Button btnLogout;
}
