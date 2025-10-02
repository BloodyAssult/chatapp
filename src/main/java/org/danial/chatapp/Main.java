package org.danial.chatapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.danial.chatapp.controller.MainController;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // بارگذاری فایل FXML
            FXMLLoader fxmlloader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            Parent root = fxmlloader.load();

            // صحنه با اندازه اولیه
            Scene scene = new Scene(root);

            // بارگذاری CSS
            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("Warning: CSS file not found.");
            }

            // تنظیم مشخصات پنجره (فقط یک بار)
            primaryStage.setTitle("🤖 AvalAI Chat Client");
            primaryStage.setScene(scene);
            primaryStage.show();

            // گرفتن کنترلر برای shutdown
            MainController controller = fxmlloader.getController();

            // تست اتصال اولیه (اختیاری — برای چک)
            controller.testConnection();

            // shutdown وقتی پنجره بسته شد
            primaryStage.setOnCloseRequest(event -> controller.shutdown());
        } catch (IOException e) {
            System.err.println("Error loading FXML/CSS: " + e.getMessage());
            e.printStackTrace();
            // می‌تونی یک پنجره خطا نمایش بدی یا برنامه رو ببندی
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}