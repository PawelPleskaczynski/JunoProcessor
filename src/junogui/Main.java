package junogui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class Main extends Application {

    private Label statusLabel = new Label("No image selected");
    private String path;
    private String original_directory;
    private String name;

    @Override
    public void start(final Stage stage) {
        stage.setTitle("Juno Processor");

        FileChooser fileChooser = new FileChooser();

        Label loadLabel = new Label("First, load desired Juno image");
        Button openButton = new Button("Open a picture");
        //Button openDirectory = new Button("Open a directory");
        Label processLabel = new Label("Then process the image using button below");
        Button processButton = new Button("Process the image");

        processButton.setDisable(true);

        openButton.setOnAction(
                e -> {
                    File file = fileChooser.showOpenDialog(stage);
                    if (file != null) {
                        openFile(file);
                        processButton.setDisable(false);
                    }
                });

        processButton.setOnAction(
                e -> assembleFrames(sliceRAW(Objects.requireNonNull(loadImage(path)))));

        final GridPane inputGridPane = new GridPane();

        GridPane.setConstraints(loadLabel, 0, 0);
        GridPane.setConstraints(openButton, 0, 1);
        GridPane.setConstraints(statusLabel, 0, 4);
        GridPane.setConstraints(processLabel, 0, 6);
        GridPane.setConstraints(processButton, 0, 7);
        inputGridPane.setHgap(6);
        inputGridPane.setVgap(6);
        inputGridPane.getChildren().addAll(loadLabel, openButton, statusLabel, processLabel, processButton);

        final Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(inputGridPane);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));
        stage.setScene(new Scene(rootGroup));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private void openFile(File file) {
        path = file.getPath();
        original_directory = file.getParent();
        name = file.getName();
        int pos = name.lastIndexOf(".");
        if (pos > 0) {
            name = name.substring(0, pos);
        }
        statusLabel.setText("Current file: " + name);
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        }
        catch (IOException e) {
            showDialog("Error", "Cannot read file", path, true);
            return null;
        }
    }

    private BufferedImage cropImg(BufferedImage image, int line) {
        final BufferedImage[] img_temp = new BufferedImage[1];
        Runnable runnable = () -> {
            img_temp[0] = image.getSubimage(0, line * 128, image.getWidth(),128);
        };
        Thread thread = new Thread(runnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return img_temp[0];
    }

    private BufferedImage[] sliceRAW(BufferedImage rawImage) {
        BufferedImage[] result = new BufferedImage[rawImage.getHeight() / 128];
        Runnable runnable = () -> {
            for (int i = 0; i < result.length; i++) {
                result[i] = cropImg(rawImage, i);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void assembleFrames(BufferedImage[] array) {

        Runnable runnableB = () -> {
            BufferedImage finalBlueImage = new BufferedImage(array[0].getWidth(), (array[0].getHeight() * (array.length / 3)),1);
            Graphics2D g2dB = finalBlueImage.createGraphics();
            int heightCurrentB = 0;
            for (int i = 0; i < array.length; i += 3) { //go through all blue framelets
                g2dB.drawImage(array[i], 0, heightCurrentB, null);
                heightCurrentB += 113;
            }

            try {
                new File(original_directory + "/" + name + "/").mkdirs();
                ImageIO.write(finalBlueImage, "png", new File(original_directory + "/" + name + "/" + name + "_blue_channel.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread threadB = new Thread(runnableB);
        threadB.start();

        Runnable runnableG = () -> {
            BufferedImage finalGreenImage = new BufferedImage(array[0].getWidth(), (array[0].getHeight() * (array.length / 3)),1);
            Graphics2D g2dG = finalGreenImage.createGraphics();
            int heightCurrentG = 0;
            for (int i = 1; i < array.length; i += 3) { //go through all green framelets
                g2dG.drawImage(array[i], 0, heightCurrentG, null);
                heightCurrentG += 113;
            }

            try {
                new File(original_directory + "/" + name + "/").mkdirs();
                ImageIO.write(finalGreenImage, "png", new File(original_directory + "/" + name + "/" + name + "_green_channel.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread threadG = new Thread(runnableG);
        threadG.start();

        Runnable runnableR = () -> {
            BufferedImage finalRedImage=new BufferedImage(array[0].getWidth() ,(array[0].getHeight()*(array.length/3)),1);
            Graphics2D g2dR = finalRedImage.createGraphics();
            int heightCurrentR =0;
            for (int i=2;i<array.length; i += 3) { //go through all red framelets
                g2dR.drawImage(array[i], 0, heightCurrentR, null);
                heightCurrentR +=113;
            }

            try {
                new File(original_directory + "/" + name + "/").mkdirs();
                ImageIO.write(finalRedImage, "png", new File(original_directory + "/" + name + "/" + name + "_red_channel.png"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread threadR = new Thread(runnableR);
        threadR.start();

        Runnable runnable = () -> {
            try {
                threadB.join();
                threadG.join();
                threadR.join();

                Platform.runLater(() -> showDialog("Success", "Done", "Finished processing file " + name, false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void showDialog(String title, String header, String message, boolean error) {
        Alert alert = new Alert(error ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Text text = new Text(message);
        text.setWrappingWidth(400);
        alert.getDialogPane().setContent(text);

        alert.showAndWait();
    }
}