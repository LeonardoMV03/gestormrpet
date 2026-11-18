package net.martinezoft.ui;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import javafx.application.Platform;
import javafx.stage.Stage;
import net.martinezoft.di.AppContext;
import net.martinezoft.ui.components.SidebarNav;
import net.martinezoft.ui.pages.AgendaPage;
import net.martinezoft.ui.pages.ClientesPage;
import net.martinezoft.ui.pages.CobrosPage;
import net.martinezoft.ui.pages.LoginPage;
import net.martinezoft.ui.pages.ServiciosPage;
import net.martinezoft.ui.templates.MainTemplate;
import net.martinezoft.ui.viewmodels.AgendaViewModel;
import net.martinezoft.ui.viewmodels.ClientesViewModel;
import net.martinezoft.ui.viewmodels.CobrosViewModel;
import net.martinezoft.ui.viewmodels.LoginViewModel;
import net.martinezoft.ui.viewmodels.MascotasViewModel;
import net.martinezoft.ui.viewmodels.ServiciosViewModel;
import net.martinezoft.ui.viewmodels.ShellViewModel;

/**
 * Smoke test: las vistas principales cargan sus FXML sin excepciones.
 */
@ExtendWith(ApplicationExtension.class)
class CargaVistasSmokeTest {

    @TempDir
    Path tempDir;

    private AppContext contexto;

    @Start
    void iniciar(Stage stage) throws Exception {
        contexto = AppContext.crear(tempDir.resolve("smoke.db"));
        contexto.inicializarBaseDatos();
    }

    @Test
    void debeCargarLasVistasPrincipales() throws Exception {
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        CountDownLatch listo = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                new LoginPage(new LoginViewModel(contexto.auth()), () -> { });
                new ClientesPage(new ClientesViewModel(contexto.clientes()),
                        new MascotasViewModel(contexto.mascotas()));
                new ServiciosPage(new ServiciosViewModel(contexto.servicios()));
                new AgendaPage(new AgendaViewModel(contexto.citas()));
                new CobrosPage(new CobrosViewModel(contexto.citas()));

                SidebarNav.Destino inicio = new SidebarNav.Destino("inicio", "Inicio");
                MainTemplate plantilla = new MainTemplate(new ShellViewModel(contexto.sesion()), () -> { });
                plantilla.configurarDestinos(List.of(inicio), inicio, destino -> { });
            } catch (Throwable t) {
                fallo.set(t);
            } finally {
                listo.countDown();
            }
        });
        if (!listo.await(15, TimeUnit.SECONDS)) {
            throw new AssertionError("La carga de vistas no terminó a tiempo");
        }
        assertNull(fallo.get(), () -> "Falló la carga de vistas: " + fallo.get());
    }
}
