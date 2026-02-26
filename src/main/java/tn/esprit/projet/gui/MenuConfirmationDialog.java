package tn.esprit.projet.gui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.BigDecimalStringConverter;
import tn.esprit.projet.entities.Menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MenuConfirmationDialog extends Dialog<ButtonType> {
    private final TableView<Menu> tableView = new TableView<>();
    private final ObservableList<Menu> items;

    public MenuConfirmationDialog(List<Menu> parsedMenus) {
        // Initialize with the list sent from Gemini
        items = FXCollections.observableArrayList(parsedMenus);

        setTitle("Review Extracted Menu");
        setHeaderText("Gemini AI has extracted these items. Double-click any cell to edit if needed.");

        // --- Name Column ---
        TableColumn<Menu, String> nameCol = new TableColumn<>("Dish Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(event -> event.getRowValue().setName(event.getNewValue()));
        nameCol.setPrefWidth(150);

        // --- Price Column ---
        TableColumn<Menu, BigDecimal> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setCellFactory(col -> new TextFieldTableCell<>(new BigDecimalStringConverter()));
        priceCol.setOnEditCommit(event -> event.getRowValue().setPrice(event.getNewValue()));
        priceCol.setPrefWidth(80);

        // --- Description Column ---
        TableColumn<Menu, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));
        descCol.setPrefWidth(300);

        // --- Status Column (Read-only for now) ---
        TableColumn<Menu, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        // Setup Table
        tableView.getColumns().addAll(nameCol, priceCol, descCol, statusCol);
        tableView.setEditable(true);
        tableView.setItems(items);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Context Menu for bulk actions
        setupContextMenu();

        // Layout
        getDialogPane().setContent(tableView);
        getDialogPane().setPrefWidth(700);
        getDialogPane().setPrefHeight(500);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Add custom styling class if you have a CSS file
        getDialogPane().getStyleClass().add("confirmation-dialog");
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem mergeItem = new MenuItem("Merge Selected (Combine names)");
        mergeItem.setOnAction(e -> mergeSelected());

        MenuItem deleteItem = new MenuItem("Delete Selected");
        deleteItem.setStyle("-fx-text-fill: red;");
        deleteItem.setOnAction(e -> deleteSelected());

        contextMenu.getItems().addAll(mergeItem, new SeparatorMenuItem(), deleteItem);
        tableView.setContextMenu(contextMenu);
    }

    private void mergeSelected() {
        ObservableList<Menu> selected = tableView.getSelectionModel().getSelectedItems();
        if (selected.size() < 2) {
            showAlert("Please select at least two items to merge.");
            return;
        }

        StringBuilder combinedName = new StringBuilder();
        StringBuilder combinedDesc = new StringBuilder();
        BigDecimal price = selected.get(0).getPrice();

        for (Menu m : selected) {
            if (combinedName.length() > 0) combinedName.append(" / ");
            combinedName.append(m.getName());

            if (m.getDescription() != null && !m.getDescription().isEmpty()) {
                if (combinedDesc.length() > 0) combinedDesc.append(" ");
                combinedDesc.append(m.getDescription());
            }
        }

        Menu merged = new Menu();
        merged.setName(combinedName.toString());
        merged.setDescription(combinedDesc.toString());
        merged.setPrice(price);
        merged.setStatus("AVAILABLE");

        items.removeAll(selected);
        items.add(merged);
        tableView.getSelectionModel().clearSelection();
    }

    private void deleteSelected() {
        List<Menu> selected = new java.util.ArrayList<>(tableView.getSelectionModel().getSelectedItems());
        items.removeAll(selected);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING, msg);
        alert.showAndWait();
    }

    public List<Menu> getConfirmedItems() {
        return items;
    }
}