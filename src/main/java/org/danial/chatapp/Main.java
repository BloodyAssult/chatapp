package org.danial.chatapp;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
public class Main extends Application {
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    public void start(Stage stage) throws Exception {
        //بارگذاری فایل FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
        BorderPane root = loader.load();
        // راست‌چین کردن کل اپلیکیشن
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        // صحنه با اندازه اولیه
        Scene scene = new Scene(root, 900,600);
        // بارگذاری CSS
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        // تنظیم مشخصات پنجره
        stage.setTitle("🤖 AvalAI Chat Client");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args){
        launch(args);
    }
}
