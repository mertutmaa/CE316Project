module org.example.ce316project {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.xerial.sqlitejdbc;

    opens org.example.ce316project to javafx.fxml;
    exports org.example.ce316project;
}