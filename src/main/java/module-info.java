module tictactoe_test {
    requires javafx.controls;
    requires javafx.fxml;

    opens tictactoe_test to javafx.fxml;
    exports tictactoe_test;
}
