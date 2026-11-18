package net.martinezoft;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

import net.martinezoft.config.AppConfig;
import net.martinezoft.di.AppContext;
import net.martinezoft.ui.pages.LoginPage;

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
        scene = new Scene(new LoginPage(), 640, 480);
        scene.getStylesheets().addAll(
                App.class.getResource("ui/design/tokens.css").toExternalForm(),
                App.class.getResource("ui/design/theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}