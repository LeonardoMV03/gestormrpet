package net.martinezoft.ui.pages;

import java.util.Objects;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.martinezoft.domain.Cliente;
import net.martinezoft.domain.Mascota;
import net.martinezoft.ui.CargadorFxml;
import net.martinezoft.ui.components.ClienteForm;
import net.martinezoft.ui.components.ClientesTable;
import net.martinezoft.ui.components.MascotaForm;
import net.martinezoft.ui.components.MascotasTable;
import net.martinezoft.ui.viewmodels.ClientesViewModel;
import net.martinezoft.ui.viewmodels.MascotasViewModel;

/**
 * Pantalla de clientes y sus mascotas. La confirmación de baja es
 * responsabilidad de la vista; las reglas viven en los ViewModels.
 */
public class ClientesPage extends VBox {

    @FXML
    private TextField buscador;
    @FXML
    private StackPane slotClientes;
    @FXML
    private StackPane slotClienteForm;
    @FXML
    private Tab tabMascotas;
    @FXML
    private Label tituloMascotas;
    @FXML
    private StackPane slotMascotas;
    @FXML
    private StackPane slotMascotaForm;

    private final ClientesViewModel vmClientes;
    private final MascotasViewModel vmMascotas;

    public ClientesPage(ClientesViewModel vmClientes, MascotasViewModel vmMascotas) {
        this.vmClientes = Objects.requireNonNull(vmClientes, "El ViewModel de clientes es obligatorio");
        this.vmMascotas = Objects.requireNonNull(vmMascotas, "El ViewModel de mascotas es obligatorio");
        CargadorFxml.cargar(this, ClientesPage.class);

        buscador.textProperty().bindBidirectional(vmClientes.busquedaProperty());
        slotClientes.getChildren().setAll(new ClientesTable(vmClientes));
        slotClienteForm.getChildren().setAll(new ClienteForm(vmClientes, this::confirmarDesactivarCliente));
        slotMascotas.getChildren().setAll(new MascotasTable(vmMascotas));
        slotMascotaForm.getChildren().setAll(new MascotaForm(vmMascotas, this::confirmarDesactivarMascota));

        tabMascotas.disableProperty().bind(vmClientes.seleccionadoProperty().isNull());
        tituloMascotas.textProperty().bind(Bindings.createStringBinding(() -> {
            Cliente cliente = vmClientes.seleccionadoProperty().get();
            return cliente == null ? "Selecciona un cliente" : "Mascotas de " + cliente.nombre();
        }, vmClientes.seleccionadoProperty()));

        vmClientes.seleccionadoProperty().addListener((observable, anterior, actual) ->
                vmMascotas.cargarPara(actual == null ? null : actual.id()));

        vmClientes.cargar();
    }

    private void confirmarDesactivarCliente() {
        Cliente seleccionado = vmClientes.seleccionadoProperty().get();
        if (seleccionado == null) {
            return;
        }
        if (confirmar("Desactivar cliente",
                "¿Desactivar a \"" + seleccionado.nombre() + "\"?",
                "El cliente dejará de aparecer en la lista, pero sus datos se conservan.")) {
            vmClientes.desactivarSeleccionado();
        }
    }

    private void confirmarDesactivarMascota() {
        Mascota seleccionada = vmMascotas.seleccionadaProperty().get();
        if (seleccionada == null) {
            return;
        }
        if (confirmar("Desactivar mascota",
                "¿Desactivar a \"" + seleccionada.nombre() + "\"?",
                "La mascota dejará de aparecer en la lista, pero sus datos se conservan.")) {
            vmMascotas.desactivarSeleccionada();
        }
    }

    private boolean confirmar(String titulo, String encabezado, String contenido) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.initOwner(getScene() != null ? getScene().getWindow() : null);
        confirmacion.setTitle(titulo);
        confirmacion.setHeaderText(encabezado);
        confirmacion.setContentText(contenido);
        ButtonType desactivar = new ButtonType("Desactivar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacion.getButtonTypes().setAll(desactivar, cancelar);
        Optional<ButtonType> respuesta = confirmacion.showAndWait();
        return respuesta.isPresent() && respuesta.get() == desactivar;
    }
}
