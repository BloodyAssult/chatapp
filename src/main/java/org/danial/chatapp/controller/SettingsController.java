package org.danial.chatapp.controller;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.danial.chatapp.service.AvalAIService;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SettingsController {
    @FXML
    private ComboBox<String> modelComboBox;
    @FXML
    private TextField apiKeyfield;
    @FXML
    private TextField googleKeyField;

    private AvalAIService avalAIService;

    public void setAvalAIService(AvalAIService service) {
        this.avalAIService = service;
    }

    @FXML
    public void initialize() {
        // مدل‌های موجود (از memories: gpt-5, gpt-oss-120b, می‌تونی اضافه کنی)
        modelComboBox.setItems(FXCollections.observableArrayList("gpt-oss-120b", "gpt-5"));
        // لود تنظیمات قبلی اگر فایل وجود داشت (بعداً پیاده می‌شه)
    }
    @FXML
    private void saveSettings(){
        String selectedModel = modelComboBox.getValue();
        String apiKey = apiKeyfield.getText();
        String googleKey = googleKeyField.getText();

        if (selectedModel != null && !apiKey.isEmpty() && !googleKey.isEmpty()) {
            // بروزرسانی مدل در سرویس
            avalAIService.setModel(selectedModel);

            // ذخیره در settings.json
            Map<String, String> settings = new HashMap<>();
            settings.put("model", selectedModel);
            settings.put("apiKey", apiKey);
            settings.put("googleKey", googleKey);


            try (FileWriter writer = new FileWriter("settings.json")) {
                new Gson().toJson(settings, writer);
                System.out.println("تنظیمات ذخیره شد!");
            } catch (IOException e) {
                System.out.println("خطا در ذخیره تنطیمات" + e.getMessage());
            }

            // بستن پنجره
            Stage stage = (Stage) modelComboBox.getScene().getWindow();
            stage.close();
        } else {
            // هشدار اگر فیلد خالی باشه
            Platform.runLater(() -> new Alert(Alert.AlertType.WARNING, "لطفا همه فیلدها را پر کنید!").show());
        }
    }
}
