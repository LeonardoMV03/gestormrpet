package net.martinezoft;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import net.martinezoft.config.AppConfig;
import net.martinezoft.di.AppContext;
import net.martinezoft.ui.navigation.NavigationService;

/**
 * Punto de entrada de la aplicación. Inicializa la base de datos antes de
 * mostrar cualquier pantalla y elige entre login y shell según la sesión.
 */
public class App extends Application {

    private static final Logger REGISTRO = Logger.getLogger(App.class.getName());
    private static final String MENSAJE_ERROR_BD =
            "No se pudo iniciar la aplicación porque la base de datos no está disponible. "
                    + "Revisa los permisos de la carpeta de datos e inténtalo de nuevo.";

    @Override
    public void start(Stage stage) {
        AppContext contexto;
        try {
            contexto = AppContext.crear(AppConfig.rutaBdPorDefecto());
            contexto.inicializarBaseDatos();
        } catch (SQLException | RuntimeException e) {
            REGISTRO.log(Level.SEVERE, "Fallo al inicializar la base de datos", e);
            mostrarErrorInicio();
            return;
        }

        NavigationService navegacion = new NavigationService(stage, contexto);
        if (contexto.sesion().activa()) {
            navegacion.mostrarShell();
        } else {
            navegacion.mostrarLogin();
        }
        stage.show();
    }

    private static void mostrarErrorInicio() {
        Alert alerta = new Alert(Alert.AlertType.ERROR, MENSAJE_ERROR_BD, ButtonType.OK);
        alerta.setHeaderText("Error al iniciar gestormrpet");
        alerta.showAndWait();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch();
    }
}
