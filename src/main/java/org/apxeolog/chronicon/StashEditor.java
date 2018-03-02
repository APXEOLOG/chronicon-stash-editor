package org.apxeolog.chronicon;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author APXEOLOG (Artyom Melnikov), at 01.03.2018
 */
@Slf4j
public class StashEditor extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    public static class AttributeModel {

        private final SimpleStringProperty name;
        private final SimpleStringProperty value;

        private Stash.Item.Attribute attribute;

        AttributeModel(Stash.Item.Attribute attribute) {
            this.attribute = attribute;
            this.name = new SimpleStringProperty(attribute.getName());
            this.value = new SimpleStringProperty(String.valueOf(attribute.getValue()));
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public String getValue() {
            return value.get();
        }

        public void setValue(String value) {
            if (attribute.getValue() instanceof String) {
                attribute.setValue(value);
            } else if (attribute.getValue() instanceof Double) {
                attribute.setValue(Double.valueOf(value));
            }
            this.value.set(value);
        }
    }

    private Stash stash;

    @FXML
    private TextField editFilePath;

    @FXML
    private ListView<String> itemsList;

    @FXML
    private TableView itemTable;

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        try {
            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("window.fxml"));
            primaryStage.setTitle("Chronicon Stash Editor");
            primaryStage.setScene(new Scene(root, 600, 600));
            primaryStage.show();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception. Stash format version is probably incompatible");
        alert.setHeaderText(throwable.getMessage());
        alert.setContentText(Arrays.stream(ExceptionUtils.getRootCauseStackTrace(throwable))
                .limit(10).collect(Collectors.joining("\n")));
        alert.showAndWait();
    }

    public void showFileChooserDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(getChroniconFolder().toFile());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Stash file", "*.stash"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            editFilePath.setText(file.getAbsolutePath());
        }
    }

    public void loadStash() {
        if (editFilePath.getText() != null) {
            File file = new File(editFilePath.getText());
            if (file.exists()) {
                try {
                    stash = Stash.fromFile(file);
                    itemsList.getItems().clear();
                    itemsList.getItems().addAll(
                            stash.getItemList().stream()
                                    .map(Stash.Item::getName)
                                    .collect(Collectors.toList()));
                    itemsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    itemsList.getSelectionModel().selectedItemProperty()
                            .addListener((observable, oldValue, newValue) -> selectItem());
                } catch (Exception ex) {
                    showError(ex);
                }
            }
        }
    }

    void selectItem() {
        int index = itemsList.getSelectionModel().getSelectedIndex();
        Stash.Item item = stash.getItemList().get(index);
        itemTable.setEditable(true);
        TableColumn<AttributeModel, String> nameColumn = (TableColumn) itemTable.getColumns().get(0);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setEditable(false);
        TableColumn<AttributeModel, String> valueColumn = (TableColumn) itemTable.getColumns().get(1);
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setEditable(true);
        valueColumn.setOnEditCommit(event -> event.getRowValue().setValue(event.getNewValue()));
        itemTable.getItems().clear();
        itemTable.getItems().addAll(item.getAttributeList().stream()
                .map(AttributeModel::new).collect(Collectors.toList()));
    }

    public void saveStash() throws IOException {
        if (editFilePath.getText() != null && stash != null) {
            File file = new File(editFilePath.getText());
            stash.toFile(file);
        }
    }

    private Path getChroniconFolder() {
        String appDataFolder = System.getenv("LOCALAPPDATA");
        if (appDataFolder != null) {
            return Paths.get(appDataFolder).resolve("Chronicon").resolve("save");
        }
        return Paths.get("");
    }
}
