package com.mediatracker;

import com.mediatracker.controllers.MediaController;
import com.mediatracker.database.DatabaseManager;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class App extends Application {

    public static void main(String[] args) {
        // 1. Initialize SQLite Database & Table Schema
        DatabaseManager.initializeDatabase();

        // 2. Start Javalin Backend Server
        Javalin server = Javalin.create(config -> {
            // Serves static HTML/CSS/JS files from src/main/resources/public
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(7070);

        // 3. Define REST API Routes
        server.get("/api/media", MediaController::getAllMedia);
        server.post("/api/media/add", MediaController::addMedia);
        server.put("/api/media/update", MediaController::updateMedia);
        server.delete("/api/media/{id}", MediaController::deleteMedia);
        server.post("/api/media/redirect/{id}", MediaController::openSourceLink);

        System.out.println("Javalin server running on http://localhost:7070");

        // 4. Launch JavaFX Desktop Window
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        // Loads the local web app into the native desktop wrapper window
        webView.getEngine().load("http://localhost:7070/");
        
        stage.setTitle("Media Tracker App");
        stage.setScene(new Scene(webView, 1050, 650));
        stage.show();
    }
}