package junogui;

import javafx.scene.control.TextField;

/**
 * TextField that only allows numbers
 */
class NumberField extends TextField {
    NumberField() {
        this.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("(^-|)[^\\D+]")) {
                if (newValue.startsWith("-")) {
                    this.setText("-" + newValue.substring(1).replaceAll("[^\\d]+", ""));
                } else {
                    this.setText(newValue.replaceAll("[^\\d]+", ""));
                }
            }
        });
    }
}
