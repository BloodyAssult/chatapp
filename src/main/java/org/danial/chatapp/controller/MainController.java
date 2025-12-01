package org.danial.chatapp.controller;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton; // اضافه برای چک کلیک چپ/راست
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.danial.chatapp.model.ChatMessageModel;
import org.danial.chatapp.service.AvalAIService;
import javafx.animation.TranslateTransition;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.BorderPane;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.danial.chatapp.model.ChatSession;

public class MainController {
    @FXML private ListView<String> chatList;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatArea;
    @FXML private TextArea messageField;
    @FXML private Button hamburgerButton;
    @FXML private VBox sidebar;
    @FXML private BorderPane chatPane;
    @FXML private  ComboBox<String> modelSelector;
    private boolean sidebarVisible = false;

    private final AvalAIService avalAIService = new AvalAIService();
    private String selectedModel = "gpt-oss-120b";

    private Font cachedFont;
    private static final double FONT_SIZE = 15.5;

    private List<ChatSession> sessions = new ArrayList<>();
    private ChatSession currentSession;

    @FXML
    public void initialize() {
        cachedFont = loadBestFont();

        setupAutoResizeTextArea(messageField, 30, 150);
        messageField.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        messageField.setFont(Font.font(cachedFont.getFamily(), FONT_SIZE));

        messageField.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    int caretPosition = messageField.getCaretPosition();
                    messageField.insertText(caretPosition, "\n");
                    event.consume();
                } else {
                    event.consume();
                    String text = messageField.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        sendMessage();
                    }
                }
            }
        });
        loadSessions();
        currentSession = createNewSession();
        updateChatList();

        // جدید: پر کردن لیست مدل‌ها (فاز 3)
        modelSelector.setItems(FXCollections.observableArrayList("gpt-oss-120b", "gpt-5", "grok-4"));// لیست مدل‌ها - می‌تونی اضافه کنی
        modelSelector.setValue(selectedModel);
        modelSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedModel = newVal;// بروزرسانی مدل انتخابی
                System.out.println("مدل انتخابی" + selectedModel); // برای دیباگ
            }
        });
        // فیکس چپ‌کلیک: بارگذاری فقط با کلیک چپ (PRIMARY)
        chatList.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) { // فقط چپ کلیک بارگذاری کنه (فیکس راست کلیک)
                String selected = chatList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadSessionByName(selected);
                }
            }
        });

        // راست‌کلیک: فقط context menu (rename/delete) — بدون بارگذاری
        chatList.setOnContextMenuRequested(event -> {
            String selected = chatList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ContextMenu menu = new ContextMenu();
                MenuItem rename = new MenuItem("تغییر نام");
                rename.setOnAction(e -> renameSession(findSessionByName(selected)));
                MenuItem delete = new MenuItem("حذف");
                delete.setOnAction(e -> deleteSession(findSessionByName(selected)));
                menu.getItems().addAll(rename, delete);
                menu.show(chatList, event.getScreenX(), event.getScreenY());
                event.consume(); // جلوگیری از پخش event به onMouseClicked
            }
        });

        // اول برنامه: پنل کناری unmanaged برای full-width چت
        sidebar.setManaged(false);
        sidebar.setVisible(false);
    }

    private Font loadBestFont() {
        String[] fontPaths = {
                "/fonts/Sahel.ttf",
                "/fonts/IRANSans.ttf",
                "/fonts/Vazirmatn.ttf"
        };

        for (String path : fontPaths) {
            try {
                Font font = Font.loadFont(getClass().getResourceAsStream(path), FONT_SIZE);
                if (font != null) {
                    System.out.println("✅ فونت بارگذاری شد: " + path);
                    return font;
                }
            } catch (Exception e) {
                System.out.println("❌ فونت یافت نشد: " + path);
            }
        }

        System.out.println("⚠️ استفاده از فونت پیش‌فرض سیستم");
        return Font.font("Tahoma", FONT_SIZE);
    }

    @FXML
    private void toggleSidebar() {
        // TranslateTransition transition = new TranslateTransition(Duration.millis(100), sidebar); // فیکس: سریع‌تر (100ms برای جلوگیری از هنگ/کندی)
        //if (sidebarVisible) {
        // transition.setToX(-250);
        // transition.setOnFinished(e -> {
        //   sidebar.setVisible(false);
        // sidebar.setManaged(false);
        //applyBlur(false);
        //});
        // } //else {
        //sidebar.setVisible(true);
        //sidebar.setManaged(true);
        //transition.setToX(0);
        //applyBlur(true);
        //}
        //transition.play();
        // sidebarVisible = !sidebarVisible;
        if (sidebarVisible) {
            sidebar.setVisible(false);
            sidebar.setManaged(false);
            applyBlur(false);
            sidebar.setTranslateX(-250);
        } else {
            sidebar.setVisible(true);
            sidebar.setManaged(true);
            applyBlur(true);
            sidebar.setTranslateX(0);
        }
        sidebarVisible = !sidebarVisible;
    }

    private void applyBlur(boolean enable) {
        if (enable) {
            chatPane.setEffect(new GaussianBlur(4));
            chatPane.setStyle("-fx-background-color: rgba(245, 245, 245, 0.8);");
        } else {
            chatPane.setEffect(null);
            chatPane.setStyle("-fx-background-color: #F5F5F5;");
        }
    }

    private ChatSession createNewSession() {
        ChatSession newSession = new ChatSession();
        String baseName = "مکالمه جدید";
        String newName = baseName;
        int counter = 1;
        // فیکس: چک برای duplicate و اضافه شمارنده برای منحصر به فرد کردن نام
        while (findSessionByName(newName) != null) {
            newName = baseName + "" + counter;
            counter++;
        }
        newSession.setName(newName);
        sessions.add(newSession);
        return newSession;
    }

    private void saveCurrentSession() {
        if (currentSession != null) {
            currentSession.getMessages().clear();
            for (Node node : chatArea.getChildren()) {
                if (node instanceof HBox) {
                    TextFlow textFlow = (TextFlow) ((VBox) ((HBox) node).getChildren().get(0)).getChildren().get(0);
                    String message = (String) textFlow.getUserData();
                    if (message != null) currentSession.addMessage(message);
                }
            }
            saveSessions();
        }
    }

    private void updateChatList() {
        chatList.getItems().clear();
        for (ChatSession session : sessions) {
            chatList.getItems().add(session.getName());
        }
    }

    private void loadSessionByName(String name) {
        ChatSession session = findSessionByName(name);
        if (session != null) {
            saveCurrentSession();
            currentSession = session;
            chatArea.getChildren().clear();
            for (String msg : session.getMessages()) {
                boolean isUser = msg.startsWith("شما:");
                Node bubble = createDynamicTextFlow(msg, isUser);
                chatArea.getChildren().add(bubble);
            }
            scrollToBottom();
            toggleSidebar(); // مخفی کردن پنل بعد از انتخاب
        }
    }

    private ChatSession findSessionByName(String name) {
        for (ChatSession s : sessions) {
            if (s.getName().equals(name)) return s;
        }
        return null;
    }

    private void renameSession(ChatSession session){
        if (session == null) return;
        TextInputDialog dialog = new TextInputDialog(session.getName());
        dialog.setTitle("تغییر نام مکالمه");
        dialog.setHeaderText("نام جدید را وارد کنید");
        dialog.showAndWait().ifPresent(newName -> {
            String uniqueName = newName;
            int counter = 1;
            while (findSessionByName(uniqueName) != null && !uniqueName.equals(session.getName())) {

                uniqueName = newName + "" +counter;
                counter++;
            }
            session.setName(newName);
            updateChatList();
            saveSessions();
        });
    }

    private void deleteSession(ChatSession session) {
        if (session == null) return;
        sessions.remove(session);
        if(currentSession == session) newChat();
        updateChatList();
        saveSessions();
    }

    private void saveSessions() {
        try (FileWriter writer = new FileWriter("sessions.json")){
            new Gson().toJson(sessions, writer);
        } catch (IOException e) {
            System.out.println("خطا در ذخیره" + e.getMessage());
        }
    }

    private void loadSessions(){
        try (FileReader reader = new FileReader("sessions.json")) {
            Type type = new TypeToken<List<ChatSession>>(){}.getType();
            sessions = new Gson().fromJson(reader, type);
            if (sessions == null) sessions = new ArrayList<>();
        } catch (IOException e) {
            sessions = new ArrayList<>();
        }
    }

    @FXML
    private void sendMessage() {
        String userText = messageField.getText();
        if (userText == null || userText.trim().isEmpty()) return;

        String textToSend = userText;
        messageField.clear();
        messageField.setPrefHeight(30);

        PauseTransition pause = new PauseTransition(Duration.millis(50));
        pause.setOnFinished(e -> {
            Node userBubble = createDynamicTextFlow("شما: " + textToSend, true);
            chatArea.getChildren().add(userBubble);
            addFadeInAnimation(userBubble);
            scrollToBottom();
            saveCurrentSession();
            updateChatList();
        });
        pause.play();

        new Thread(() -> {
            try {
                ChatMessageModel aiResponse = avalAIService.sendToAvalAI(textToSend, selectedModel);
                Platform.runLater(() -> {
                    Node aiBubble = createDynamicTextFlow("AI: " + aiResponse.getContent(), false);
                    chatArea.getChildren().add(aiBubble);
                    addFadeInAnimation(aiBubble);
                    scrollToBottom();
                    saveCurrentSession();
                    updateChatList();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    Node errorBubble = createDynamicTextFlow("خطا: " + ex.getMessage(), false);
                    chatArea.getChildren().add(errorBubble);
                    scrollToBottom();
                    saveCurrentSession();
                    updateChatList();
                });
            }
        }).start();
    }

    /**
     * ایجاد حباب با عرض دینامیک - اصلاح شده
     * حباب از سمت راست شروع می‌شود و عرض آن بر اساس محتوا تنظیم می‌شود
     */
    private Node createDynamicTextFlow(String message, boolean isUser) {
        TextFlow textFlow = new TextFlow();
        textFlow.setLineSpacing(3);
        textFlow.setPadding(new Insets(12, 16, 12, 16));
        textFlow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // 🔥 تغییر اصلی: حذف setMaxWidth از TextFlow
        // TextFlow خودش عرض را بر اساس محتوا محاسبه می‌کند

        // پس‌زمینه
        CornerRadii radii = new CornerRadii(12);
        LinearGradient gradient;
        if (isUser) {
            gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#3C8CE7")),
                    new Stop(1, Color.web("#00EAFF")));
        } else {
            gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#A9A9A9")),
                    new Stop(1, Color.web("#696969")));
        }
        textFlow.setBackground(new Background(new BackgroundFill(gradient, radii, Insets.EMPTY)));

        parseAndAddContent(textFlow, message);
        addContextMenu(textFlow);

        // 🔥 Wrapper برای محدود کردن حداکثر عرض
        VBox wrapper = new VBox(textFlow);
        wrapper.setMaxWidth(550); // حداکثر عرض حباب
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        wrapper.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // 🔥 Container اصلی که حباب را از راست نگه می‌دارد
        HBox container = new HBox();
        container.setPadding(new Insets(5));
        container.setAlignment(Pos.CENTER_RIGHT); // چسبیدن به راست
        container.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        // 🔥 فضای خالی در سمت چپ تا حباب به راست برود
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        container.getChildren().addAll(wrapper, spacer);

        return container;
    }

    private void parseAndAddContent(TextFlow textFlow, String originalMessage) {
        if (originalMessage == null || originalMessage.isEmpty()) return;

        int index = 0;
        StringBuilder textBuffer = new StringBuilder();
        int len = originalMessage.length();

        while (index < len) {
            Emoji found = null;
            String matched = null;

            // 🔥 افزایش به 15 کاراکتر برای ایموجی‌های ترکیبی
            int maxCheck = Math.min(15, len - index);
            for (int l = maxCheck; l >= 1; l--) {
                try {
                    String sub = originalMessage.substring(index, index + l);
                    Emoji e = EmojiManager.getByUnicode(sub);
                    if (e != null) {
                        found = e;
                        matched = sub;
                        break;
                    }
                } catch (Exception ignore) { }
            }

            if (found != null && matched != null) {
                if (textBuffer.length() > 0) {
                    addTextNode(textFlow, textBuffer.toString());
                    textBuffer.setLength(0);
                }

                addEmojiImage(textFlow, found);
                index += matched.length();
            } else {
                // 🔥 چک کنیم آیا کاراکتر فعلی جزء ایموجی است
                char currentChar = originalMessage.charAt(index);

                // اگر کاراکتر invisible یا ZWJ باشد، skip کن
                if (isInvisibleOrModifier(currentChar)) {
                    index++;
                    continue;
                }

                textBuffer.append(currentChar);
                index++;
            }
        }

        if (textBuffer.length() > 0) {
            addTextNode(textFlow, textBuffer.toString());
        }

        textFlow.setUserData(originalMessage);
    }

    /**
     * تشخیص کاراکترهای نامرئی و modifier ایموجی
     */
    private boolean isInvisibleOrModifier(char c) {
        // Zero Width Joiner (ZWJ) - برای ترکیب ایموجی‌ها
        if (c == '\u200D') return true;

        // Variation Selectors (VS15, VS16) - برای استایل ایموجی
        if (c == '\uFE0E' || c == '\uFE0F') return true;

        // Skin tone modifiers
        if (c >= '\uD83C' && c <= '\uD83F') return true;

        // Regional Indicator Symbols (پرچم‌ها)
        if (c >= '\uDDE6' && c <= '\uDDFF') return true;

        return false;
    }

    private void addTextNode(TextFlow textFlow, String text) {
        if (text == null || text.isEmpty()) return;

        Text textNode = new Text(text);
        textNode.setFont(cachedFont);
        textNode.setFill(Color.WHITE);
        textNode.setFontSmoothingType(javafx.scene.text.FontSmoothingType.LCD);

        textFlow.getChildren().add(textNode);
    }

    private void addEmojiImage(TextFlow textFlow, Emoji emoji) {
        String hex = emoji.getHtmlHexadecimal()
                .replace("&#x", "")
                .replace(";", "-")
                .toLowerCase();
        if (hex.endsWith("-")) {
            hex = hex.substring(0, hex.length() - 1);
        }

        // 🔥 سعی در یافتن تصویر ایموجی با فرمت‌های مختلف
        String[] possiblePaths = {
                "/emojis/72x72/emoji_u" + hex + ".png",           // Noto format
                "/emojis/72x72/" + hex + ".png",                   // Simple format
                "/emojis/72x72/u" + hex.replace("-", "_") + ".png", // Alternative format
                "/twemoji/72x72/" + hex + ".png"                    // Old twemoji (backup)
        };

        String foundPath = null;
        for (String path : possiblePaths) {
            if (getClass().getResourceAsStream(path) != null) {
                foundPath = path;
                break;
            }
        }

        try {
            if (foundPath == null) {
                // اگر هیچ تصویری پیدا نشد، از فونت سیستم استفاده کن
                addSystemEmoji(textFlow, emoji.getUnicode());
                return;
            }

            Image image = new Image(getClass().getResourceAsStream(foundPath), 22, 22, true, true);
            ImageView iv = new ImageView(image);
            iv.setPreserveRatio(true);
            iv.setFitHeight(22); // کمی بزرگتر برای کیفیت بهتر Noto
            iv.setUserData(emoji.getUnicode());
            textFlow.getChildren().add(iv);
        } catch (Exception e) {
            // در صورت خطا، از فونت سیستم استفاده کن
            addSystemEmoji(textFlow, emoji.getUnicode());
        }
    }

    /**
     * نمایش ایموجی با استفاده از فونت سیستم (بدون تصویر)
     * برای ایموجی‌هایی که تصویرشان در twemoji نیست
     */
    private void addSystemEmoji(TextFlow textFlow, String unicode) {
        if (unicode == null || unicode.isEmpty()) return;

        Text emojiText = new Text(unicode);

        // 🔥 استفاده از فونت‌های سیستم که ایموجی دارند
        // Windows: Segoe UI Emoji
        // macOS: Apple Color Emoji
        // Linux: Noto Color Emoji
        String os = System.getProperty("os.name").toLowerCase();
        String emojiFont;

        if (os.contains("win")) {
            emojiFont = "Segoe UI Emoji";
        } else if (os.contains("mac")) {
            emojiFont = "Apple Color Emoji";
        } else {
            emojiFont = "Noto Color Emoji";
        }

        emojiText.setFont(Font.font(emojiFont, 18)); // کمی بزرگتر از متن عادی
        emojiText.setFill(Color.WHITE);
        emojiText.setUserData(unicode);

        textFlow.getChildren().add(emojiText);
    }

    private void addContextMenu(TextFlow textFlow) {
        ContextMenu ctx = new ContextMenu();
        MenuItem copyItem = new MenuItem("کپی");
        copyItem.setOnAction(ev -> {
            String original = (String) textFlow.getUserData();
            if (original != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(original);
                Clipboard.getSystemClipboard().setContent(content);
            } else {
                StringBuilder sb = new StringBuilder();
                for (Node n : textFlow.getChildren()) {
                    if (n instanceof Text) {
                        sb.append(((Text) n).getText());
                    } else if (n instanceof ImageView) {
                        Object ud = n.getUserData();
                        if (ud != null) sb.append(ud.toString());
                    }
                }
                ClipboardContent content = new ClipboardContent();
                content.putString(sb.toString());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        ctx.getItems().add(copyItem);
        textFlow.setOnContextMenuRequested(e -> ctx.show(textFlow, e.getScreenX(), e.getScreenY()));
    }

    private void setupAutoResizeTextArea(TextArea textArea, double minHeight, double maxHeight) {
        textArea.setMinHeight(minHeight);
        textArea.setMaxHeight(maxHeight);
        textArea.setPrefHeight(minHeight);

        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                double newHeight = calculateTextHeight(newVal, textArea.getPrefWidth());
                newHeight = Math.max(minHeight, Math.min(newHeight, maxHeight));
                textArea.setPrefHeight(newHeight);
                textArea.requestLayout();
            });
        });
    }

    private double calculateTextHeight(String text, double prefWidth) {
        if (text == null || text.isEmpty()) return 50;

        Text helper = new Text(text);
        helper.setFont(Font.font(cachedFont.getFamily(), FONT_SIZE));
        double width = prefWidth > 0 ? prefWidth - 40 : 360;
        helper.setWrappingWidth(width);
        helper.applyCss();

        double h = helper.getLayoutBounds().getHeight();
        double padding = 50;
        if (text.length() > 200) padding = 60;
        if (text.length() > 500) padding = 70;

        return h + padding;
    }

    private void addFadeInAnimation(Node node) {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), node);
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
        saveCurrentSession();
        currentSession = createNewSession();
        chatArea.getChildren().clear();
        updateChatList();
        saveSessions();
    }

    @FXML
    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/SettingsView.fxml"));
            Parent root = loader.load();
            SettingsController controller = loader.getController();
            controller.setAvalAIService(avalAIService);

            Stage stage = new Stage();
            stage.setTitle("تنظیمات");
            stage.setScene(new Scene(root, 400, 300));
            stage.initModality(Modality.APPLICATION_MODAL); //بلاک کردن پنحره اصلی
            stage.showAndWait();
        } catch (IOException e) {
            System.out.println("خطا در باز کردن تنظیمات:" + e.getMessage());
        }
    }

    public void shutdown() {
        avalAIService.shutdown();
    }

    public void testConnection() {
        String testPrompt = "سلام، یک تست ساده برای چک اتصال.";

        PauseTransition delay = new PauseTransition(Duration.millis(800));
        delay.setOnFinished(event -> {
            Platform.runLater(() -> {
                Node testUserMsg = createDynamicTextFlow("شما: " + testPrompt, true);
                chatArea.getChildren().add(testUserMsg);
                scrollToBottom();
            });

            new Thread(() -> {
                try {
                    ChatMessageModel response = avalAIService.sendToAvalAI(testPrompt, selectedModel);
                    System.out.println("تست موفق: " + response.getContent());

                    Platform.runLater(() -> {
                        Node testAiMsg = createDynamicTextFlow("AI: " + response.getContent(), false);
                        chatArea.getChildren().add(testAiMsg);
                        addFadeInAnimation(testAiMsg);
                        scrollToBottom();
                        saveCurrentSession();
                    });
                } catch (Exception e) {
                    System.out.println("تست ناموفق: " + e.getMessage());
                    Platform.runLater(() -> {
                        Node errorMsg = createDynamicTextFlow("خطا در تست: " + e.getMessage(), false);
                        chatArea.getChildren().add(errorMsg);
                        scrollToBottom();
                        saveCurrentSession();
                        updateChatList();
                    });
                }
            }).start();
        });
        delay.play();
    }
}