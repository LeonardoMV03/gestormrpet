package net.martinezoft;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

import net.martinezoft.config.AppConfig;
import net.martinezoft.di.AppContext;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private AppContext contexto;

    @Override
    public void start(Stage stage) throws IOException {
        contexto = AppContext.crear(AppConfig.rutaBdPorDefecto());
        try {
            contexto.inicializarBaseDatos();
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo inicializar la base de datos", e);
        }
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}