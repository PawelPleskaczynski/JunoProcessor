package junogui;

import com.objectplanet.image.PngEncoder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Paweł Pleskaczyński
 * @author Karol Masztalerz
 */
public final class Main extends Application {

    private final Label selectedLabel = new Label("No image selected");
    private final Label statusLabel = new Label("Idle");
    private final Label creditsLabel = new Label("Made by Pawel Pleskaczynski and Karol Masztalerz");
    private final Label overlapLabel = new Label("Adjust image overlap by:");
    private final Button processButton = new Button("Process the image");
    private final Button openButton = new Button("Open a picture");
    private final Button openDirectory = new Button("Batch processing");
    private final CheckBox methaneBox = new CheckBox("Image is single-band CH4");
    private final CheckBox rgbAlignBox = new CheckBox("RGB Align");
    private final CheckBox rgbOnlyBox = new CheckBox("Output RGB only");
    private final CheckBox openLaterBox = new CheckBox("Open files after processing");
    private final ProgressBar progressBar = new ProgressBar();
    private final NumberField overlapField = new NumberField();

    private final ArrayList<String> paths = new ArrayList<>();
    private final ArrayList<String> original_directory_array = new ArrayList<>();
    private final ArrayList<String> name_array = new ArrayList<>();

    private boolean directory;
    private int saved_images = 0;

    @Override
    public void start(final Stage stage) {
        stage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        stage.setTitle("Juno Processor");
        Label loadLabel = new Label("First, load desired Juno image");
        Label processLabel = new Label("Then process the image using button below");
        progressBar.setProgress(0);
        processButton.setDisable(true);

        HBox box = new HBox();
        box.getChildren().addAll(overlapLabel, overlapField);
        overlapField.setMaxSize(50,30);
        box.setSpacing(10);

        Tooltip overlapToolip = new Tooltip();
        overlapToolip.setText("Input by how many pixels you'd like\n" +
                              "to change default overlap (for RGB images\n" +
                              "it's 114px, for CH4 images it's 116px)");
        overlapField.setTooltip(overlapToolip);

        FileChooser fileChooser = new FileChooser();

        openButton.setOnAction(
                e -> {
                    File file = fileChooser.showOpenDialog(stage);
                    if (file != null) {
                        openFile(file);
                        directory = false;
                        processButton.setDisable(false);
                    }
                });

        openDirectory.setOnAction(e -> {
            final DirectoryChooser directoryChooser =
                    new DirectoryChooser();
            final File selectedDirectory =
                    directoryChooser.showDialog(stage);
            if (selectedDirectory != null) {
                String path = selectedDirectory.getAbsolutePath();
                findFiles(new File(path));
                directory = true;
                processButton.setDisable(false);
            }
        });

        processButton.setOnAction(
                e -> {
                    Runnable runnable = () -> {
                        saved_images = 0;
                        Platform.runLater(() -> {
                            if (!directory) statusLabel.setText("Processing " + name_array.get(0));
                            disableCheckBoxes(true);
                            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                        });

                        ExecutorService executor = Executors.newFixedThreadPool(
                                Runtime.getRuntime().availableProcessors());

                        if (directory) {
                            Platform.runLater(() -> statusLabel.setText("Processed " + saved_images + "/" + paths.size()
                                    + " files..."));
                            for (int i = 0; i < paths.size(); i++) {
                                int finalI = i;
                                executor.submit(() -> assembleFrames(
                                        sliceRAW(loadImage(paths.get(finalI), finalI)), finalI));
                            }
                        } else {
                            executor.submit(() -> assembleFrames(sliceRAW(loadImage(paths.get(0), 0)), 0));
                        }

                        try {
                            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        } catch (InterruptedException err) {
                            err.printStackTrace();
                        }
                    };
                    Thread thread = new Thread(runnable);
                    thread.start();
                });

        rgbOnlyBox.setDisable(true);

        methaneBox.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (rgbAlignBox.isSelected()) {
                rgbAlignBox.setSelected(!t1);
                rgbOnlyBox.setDisable(t1);
            }
            rgbOnlyBox.setSelected(false);
        });

        rgbAlignBox.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (methaneBox.isSelected()) {
                methaneBox.setSelected(!t1);
            }
            rgbOnlyBox.setSelected(false);
            rgbOnlyBox.setDisable(!t1);
        });

        final GridPane inputGridPane = new GridPane();

        GridPane.setConstraints(loadLabel, 0, 0);
        GridPane.setConstraints(methaneBox, 1, 0);
        GridPane.setConstraints(rgbAlignBox, 1, 1);
        GridPane.setConstraints(rgbOnlyBox, 1, 2);
        GridPane.setConstraints(openLaterBox, 1, 3);
        GridPane.setConstraints(box, 1, 4);
        GridPane.setConstraints(openButton, 0, 1);
        GridPane.setConstraints(openDirectory, 0, 2);
        GridPane.setConstraints(selectedLabel, 0, 4);
        GridPane.setConstraints(statusLabel, 0, 5);
        GridPane.setConstraints(progressBar, 0, 6);
        GridPane.setConstraints(processLabel, 0, 8);
        GridPane.setConstraints(processButton, 0, 9);
        GridPane.setConstraints(creditsLabel, 0, 10);
        inputGridPane.setHgap(6);
        inputGridPane.setVgap(6);
        inputGridPane.getChildren().addAll(loadLabel, methaneBox, rgbAlignBox, rgbOnlyBox, openLaterBox, box, openButton,
                openDirectory, selectedLabel, statusLabel, progressBar, processLabel, processButton, creditsLabel);

        final Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(inputGridPane);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));
        Scene scene = new Scene(rootGroup);
        scene.getStylesheets().add("theme.css");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    /**
     * Finds files in a directory, gets each file's name, path and parent
     * @param directory - Path to the directory
     */
    private void findFiles(final File directory) {
        paths.clear();
        for (final File fileEntry : Objects.requireNonNull(directory.listFiles())) {
            if (!fileEntry.isDirectory()) {
                if (fileEntry.getName().endsWith("png") || fileEntry.getName().endsWith("jpg") ||
                        fileEntry.getName().endsWith("jpeg")) {
                    paths.add(fileEntry.getPath());
                    original_directory_array.add(fileEntry.getParent());
                    name_array.add(fileEntry.getName());
                    selectedLabel.setText("Found " + paths.size() + " files.");
                }
            }
        }
    }

    /**
     * Gets file's name, path and parent
     * @param file - Path to the file
     */
    private void openFile(File file) {
        paths.add(0, file.getPath());
        original_directory_array.add(0, file.getParent());
        name_array.add(0, file.getName());
        selectedLabel.setText("Current file: " + name_array.get(0));
    }

    /**
     * Loads image from file to BufferedImage
     * @param filePath - File's path
     * @param i - nth image to process
     * @return BufferedImage
     */
    private BufferedImage loadImage(String filePath, int i) {
        File file = new File(filePath);
        original_directory_array.set(i, file.getParent());
        name_array.set(i, file.getName());
        if (directory) {
            Platform.runLater(() -> selectedLabel.setText("Selected " + paths.size() + " files."));
        }
        int pos = name_array.get(i).lastIndexOf(".");
        if (pos > 0) {
            name_array.set(i, name_array.get(i).substring(0, pos));
        }
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            showDialog("Cannot read file " + paths.get(i));
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Cuts image into slices that are returned in an array
     * @param image - The BufferedImage to process
     * @return Array of stripes
     */
    private BufferedImage[] sliceRAW(BufferedImage image) {
        if (image != null) {
            BufferedImage[] result = new BufferedImage[image.getHeight() / 128];
            for (int i = 0; i < result.length; i++) {
                result[i] = cropImg(image, i);
            }
            return result;
        }
        return null;
    }

    /**
     * Slice the image to 128px high stripe (128px because of JunoCam construction)
     * @param image - The BufferedImage to process
     * @param line - nth slice of the image
     * @return Sliced stripe
     */
    private BufferedImage cropImg(BufferedImage image, int line) {
        return image.getSubimage(0, line * 128, image.getWidth(),128);
    }

    /**
     * Assembles image from stripes
     * @param image - BufferedImage array to process, it's made of individual stripes
     * @param iteration - nth image to process
     */
    private void assembleFrames(BufferedImage[] image, int iteration) {
        final BufferedImage[] finalImage = new BufferedImage[3];
        final BufferedImage[] finalMethaneImage = new BufferedImage[1];
        final BufferedImage[] rgbImage = new BufferedImage[1];

        if (methaneBox.isSelected()) {
            ExecutorService executor = Executors.newCachedThreadPool();
            executor.submit(() -> {
                finalMethaneImage[0] = new BufferedImage(image[0].getWidth(), (image[0].getHeight() * (image.length)),
                        BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g2d = finalMethaneImage[0].createGraphics();
                int heightCurrent = 0;
                for (BufferedImage bufferedImage : image) {
                    g2d.drawImage(bufferedImage, 0, heightCurrent, null);
                    heightCurrent += 116 + (overlapField.getText().trim().isEmpty() ? 0 : Integer.parseInt(overlapField.getText()));
                }

                g2d.dispose();

                saveImage(finalMethaneImage[0], iteration, "methane");
            });

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            ExecutorService executor = Executors.newCachedThreadPool();
            for (int i = 0; i < 3; i++) {
                int finalI = i;
                executor.submit(() -> {
                    finalImage[finalI] = new BufferedImage(image[0].getWidth(),
                            (image[0].getHeight() * (image.length / 3)),1);
                    Graphics2D graphics2D = finalImage[finalI].createGraphics();
                    int heightCurrent = 0;
                    for (int j = finalI; j < image.length; j += 3) {
                        graphics2D.drawImage(image[j], 0, heightCurrent, null);
                        heightCurrent += 114 + (overlapField.getText().trim().isEmpty() ? 0 : Integer.parseInt(overlapField.getText()));
                    }
                    graphics2D.dispose();
                });
            }

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ExecutorService executorSave = Executors.newCachedThreadPool();

            if (rgbAlignBox.isSelected()) {
                executorSave.submit(() -> {
                    rgbImage[0] = rgbAlign(finalImage[2], finalImage[1], finalImage[0]);
                    saveImage(rgbImage[0], iteration, "RGB");
                    rgbImage[0].flush();
                });
            }

            if (!rgbOnlyBox.isSelected()) {
                String[] array = {"blue_channel", "green_channel", "red_channel"};
                for (int i = 0; i < 3; i++) {
                    int finalI = i;
                    executorSave.submit(() -> saveImage(finalImage[finalI], iteration, array[finalI]));
                }
            }

            executorSave.shutdown();
            try {
                executorSave.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < 3; i++) { finalImage[i].flush(); }

            saved_images++;
            Platform.runLater(() -> {
                progressBar.setProgress((double) saved_images / paths.size());
                statusLabel.setText("Processed " + saved_images + "/" + paths.size() + " files...");
            });

        }

        try {
            if (directory) {
                if (saved_images == paths.size()) {
                    if (openLaterBox.isSelected()) {
                        Desktop.getDesktop().open(new File(original_directory_array.get(0)));
                    }
                    Platform.runLater(() -> {
                        progressBar.setProgress(0);
                        statusLabel.setText("Idle");
                        disableCheckBoxes(false);
                    });
                }
            } else {
                if (openLaterBox.isSelected()) {
                    Desktop.getDesktop().open(new File(original_directory_array.get(iteration) + "/" +
                            name_array.get(iteration)));
                }
                Platform.runLater(() -> {
                    progressBar.setProgress(0);
                    statusLabel.setText("Idle");
                    disableCheckBoxes(false);
                });
            }
        } catch (IOException e) {
            showDialog("");
            e.printStackTrace();
        }
    }

    /**
     * Saves the image to PNG file
     * @param image - BufferedImage to save
     * @param iteration - nth image to save
     * @param name - name of the file (it's appended at the end of filename)
     */
    private void saveImage(BufferedImage image, int iteration, String name) {
        try {
            new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
            BufferedOutputStream imageOutputStream = new BufferedOutputStream(new FileOutputStream(
                    new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration)
                            + "/" + name_array.get(iteration) + "_" + name + ".png")));
            new PngEncoder().encode(image, imageOutputStream);
            imageOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            showDialog("Cannot write to " +
                    original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" +
                    name_array.get(iteration) + "_" + name + ".png");
        }
    }

    /**
     * Assembles three grayscale images to one RGB image
     * @param imageR - BufferedImage - red channel of the image
     * @param imageG - BufferedImage - green channel of the image
     * @param imageB - BufferedImage - blue channel of the image
     * @return Assembled RGB image
     */
    private BufferedImage rgbAlign(BufferedImage imageR, BufferedImage imageG, BufferedImage imageB) {
        BufferedImage temp = new BufferedImage(imageB.getWidth(), imageB.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = temp.createGraphics();
        g2d.drawImage(imageB, 0, -154,null);
        imageB = temp;

        temp = new BufferedImage(imageG.getWidth(), imageG. getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        g2d = temp.createGraphics();
        g2d.drawImage(imageG, 0, 0, null);
        imageG = temp;

        temp = new BufferedImage(imageR.getWidth(), imageR.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        g2d = temp.createGraphics();
        g2d.drawImage(imageR, 0, 152,null);
        imageR = temp;

        g2d.dispose();

        BufferedImage image = new BufferedImage(imageG.getWidth(), imageG.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

        for (int i = 0; i < image.getHeight(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                Color pixelR = new Color(imageR.getRGB(j, i));
                Color pixelG = new Color(imageG.getRGB(j, i));
                Color pixelB = new Color(imageB.getRGB(j, i));

                int red = pixelR.getRed();
                int green = pixelG.getGreen();
                int blue = pixelB.getBlue();

                Color pixelValue = new Color(red, green, blue);
                int rgb = pixelValue.getRGB();

                image.setRGB(j, i, rgb);
            }
        }

        return image;
    }

    /**
     * Show a error dialog
     * @param message - message text
     */
    private void showDialog(String message) {
        Platform.runLater(() -> {
            statusLabel.setText("Idle");
            progressBar.setProgress(0);
            disableCheckBoxes(false);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initStyle(StageStyle.UTILITY);
            alert.setTitle("Error");
            alert.setHeaderText("An error happened");
            Text text = new Text(message);
            text.setWrappingWidth(400);
            alert.getDialogPane().setContent(text);

            alert.showAndWait();
        });
    }

    /**
     * Function to enable/disable every CheckBox
     * @param enabled - boolean, if true, CheckBoxes will be disabled, else, CheckBoxes will be enabled
     */
    private void disableCheckBoxes(boolean enabled) {
        processButton.setDisable(enabled);
        openButton.setDisable(enabled);
        openDirectory.setDisable(enabled);
        rgbAlignBox.setDisable(enabled);
        methaneBox.setDisable(enabled);
        if (rgbAlignBox.isSelected()) {
            rgbOnlyBox.setDisable(enabled);
        }
        openLaterBox.setDisable(enabled);
        overlapField.setDisable(enabled);
        overlapLabel.setDisable(enabled);
    }
}
