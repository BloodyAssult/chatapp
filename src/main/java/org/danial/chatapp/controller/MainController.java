package org.danial.chatapp.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.danial.chatapp.model.ChatMessageModel;
import org.danial.chatapp.service.AvalAIService;
import javafx.scene.text.Font;
import javafx.scene.input.KeyEvent;

public class MainController {
    @FXML private ListView<String> chatList;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatArea;
    @FXML private TextArea messageField;

    private final AvalAIService avalAIService = new AvalAIService();
    private String selectedModel = "gpt-5";

    @FXML
    public void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/fonts/Vazirmatn.ttf"), 14);

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    // خط جدید با Shift+Enter
                } else {
                    sendMessage();
                    event.consume();
                }
            }
        });
    }

    @FXML
    private void sendMessage() {
        String userText = messageField.getText();
        if (userText == null || userText.trim().isEmpty()) return;

        TextArea userTextArea = createDynamicTextArea("شما: " + userText, "chat-bubble-user");
        chatArea.getChildren().add(userTextArea);
        addFadeInAnimation(userTextArea);
        scrollToBottom();

        messageField.clear();

        new Thread(() -> {
            try {
                ChatMessageModel aiResponse = avalAIService.sendToAvalAI(userText, selectedModel);
                Platform.runLater(() -> {
                    TextArea aiTextArea = createDynamicTextArea("AI: " + aiResponse.getContent(), "chat-bubble-ai");
                    chatArea.getChildren().add(aiTextArea);
                    addFadeInAnimation(aiTextArea);
                    scrollToBottom();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    TextArea errorTextArea = createDynamicTextArea("خطا: " + e.getMessage(), "chat-bubble-ai");
                    chatArea.getChildren().add(errorTextArea);
                    scrollToBottom();
                });
            }
        }).start();
    }

    // ایجاد TextArea با محاسبه اولیه و listener برای افزایش height بر اساس سطر جدید
    private TextArea createDynamicTextArea(String text, String styleClass) {
        TextArea textArea = new TextArea();
        textArea.getStyleClass().add(styleClass);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setOpacity(0);
        textArea.setMinHeight(50); // حداقل ارتفاع
        textArea.setPrefWidth(400); // عرض محدود
        textArea.setMaxWidth(400);

        // ست متن اولیه
        textArea.setText(text);

        // محاسبه و تنظیم مستقیم برای متن اولیه (برای فعال شدن بدون listener)
        if (text != null && !text.isEmpty()) {
            int rowCount = text.split("\n").length + 1; // محاسبه ردیف‌ها (برای grow ارتفاع)
            textArea.setPrefRowCount(rowCount); // تنظیم اولیه ردیف‌ها

            int columnCount = Math.max(20, text.length() / 10); // محاسبه اولیه ستون‌ها
            textArea.setPrefColumnCount(columnCount); // تنظیم اولیه ستون‌ها

            double computedHeight = rowCount * 18 + 20; // افزایش height بر اساس ردیف‌ها (18px per row + padding)
            textArea.setPrefHeight(computedHeight); // تنظیم مستقیم height برای grow

            // دیباگ اولیه: در console چک کن
            System.out.println("دیباگ اولیه: rowCount=" + rowCount + ", columnCount=" + columnCount + ", computedHeight=" + computedHeight + ", متن طول=" + text.length());
        } else {
            textArea.setPrefRowCount(1);
            textArea.setPrefColumnCount(40);
            textArea.setPrefHeight(50); // پیش‌فرض کوچک
        }

        // listener برای تغییرات آینده (اگر متن تغییر کرد)
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) return;

            int rowCount = newVal.split("\n").length + 1;
            textArea.setPrefRowCount(rowCount);

            int columnCount = Math.max(20, newVal.length() / 10);
            textArea.setPrefColumnCount(columnCount);

            double computedHeight = rowCount * 18 + 20; // افزایش height
            textArea.setPrefHeight(computedHeight);

            textArea.requestLayout(); // force اعمال

            // دیباگ listener: اگر فعال شد، این رو می‌بینی
            System.out.println("دیباگ listener: rowCount=" + rowCount + ", columnCount=" + columnCount + ", computedHeight=" + computedHeight + ", متن جدید طول=" + newVal.length());
        });

        return textArea;
    }

    private void addFadeInAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setCycleCount(1);
        fadeIn.setAutoReverse(false);
        fadeIn.play();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatArea.layout();
            chatScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void newChat() {
        chatArea.getChildren().clear();
    }

    @FXML
    private void openSettings() {
        System.out.println("Settings window will open here...");
    }

    public void shutdown() {
        avalAIService.shutdown();
    }

    public void testConnection() {
        String testPrompt = "سلام، یک تست ساده برای چک اتصال.";
        try {
            ChatMessageModel response = avalAIService.sendToAvalAI(testPrompt, selectedModel);
            System.out.println("تست موفق: " + response.getContent());
        } catch (Exception e) {
            System.out.println("تست ناموفق: " + e.getMessage());
        }
    }
}