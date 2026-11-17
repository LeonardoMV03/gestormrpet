module net.martinezoft {
    requires javafx.controls;
    requires javafx.fxml;

    opens net.martinezoft to javafx.fxml;
    exports net.martinezoft;
}
