package org.danial.chatapp.controller;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import org.danial.chatapp.service.AvalAIService;
import org.danial.chatapp.model.ChatMessage;
public class MainController {
    @FXML private ListView<String> chatList;
    @FXML private VBox chatArea;
    @FXML private TextField messageField;
    private AvalAIService avalAIService = new AvalAIService();

    @FXML
    private void SendMessage() {
        String userText = messageField.getText();
        if (userText.isEmpty()) return;
        // اضافه کردن پیام کاربر به UI
        chatArea.getChildren().add(new Label(userText));
        // گرفتن پاسخ از AvalAI
       ChatMessage aiResponse = avalAIService.sendToAvalAI(userText, "gpt-5");
       chatArea.getChildren().add(new Label(aiResponse.getContent()));
       messageField.clear();
    }
    @FXML
    private void newChat() {
        chatArea.getChildren().clear();
    }
    @FXML
    private void openSettings(){
        System.out.println("Settings window will open here...");
    }
}
