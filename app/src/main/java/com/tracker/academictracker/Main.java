package com.tracker.academictracker;

import com.tracker.academictracker.controller.DashboardController;
import com.tracker.academictracker.model.Student;
import com.tracker.academictracker.service.DataManager;
import com.tracker.academictracker.ui.AppTheme;
import com.tracker.academictracker.ui.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    // Giữ lại tham chiếu của cửa sổ chính để có thể đổi Scene
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Academic Tracker");

        // Sử dụng DataManager để kiểm tra và tải dữ liệu!
        if (DataManager.saveFileExists()) {
            System.out.println("Found save file. Loading data...");
            try {
                Student loadedStudent = DataManager.loadStudent();
                if (loadedStudent != null) {
                    showDashboard(loadedStudent);
                } else {
                    showWelcomeScreen();
                }
            } catch (IOException e) {
                System.err.println("Cannot load save file: " + e.getMessage());
                showWelcomeScreen();
            }
        } else {
            System.out.println("No save file found. Opening profile setup...");
            showWelcomeScreen();
        }
    }

    /**
     * Mở màn hình Khởi tạo Profile (Welcome.fxml)
     */
    public static void showWelcomeScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("Welcome.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 700);
        ThemeManager.applyToScene(scene);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(560);
        primaryStage.show();
    }

    /**
     * Mở màn hình Dashboard chính (Dashboard.fxml) và truyền dữ liệu Student vào
     */
    public static void showDashboard(Student student) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("Dashboard.fxml"));
        Parent root = loader.load();

        // Lấy Controller và truyền dữ liệu
        DashboardController controller = loader.getController();
        controller.initData(student);

        Scene scene = new Scene(root, 1100, 700);
        ThemeManager.setCurrentTheme(AppTheme.fromStorage(student == null ? null : student.getAppTheme()));
        ThemeManager.applyToScene(scene);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
