package junogui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public final class Main extends Application {

    private Label selectedLabel = new Label("No image selected");
    private Label statusLabel = new Label("Idle");
    private ArrayList<String> paths = new ArrayList<>();
    private String path;
    private String original_directory;
    private String name;
    private ArrayList<String> original_directory_array = new ArrayList<>();
    private ArrayList<String> name_array = new ArrayList<>();
    private Button processButton = new Button("Process the image");
    private Button openButton = new Button("Open a picture");
    private ProgressBar progressBar = new ProgressBar();
    private Button openDirectory = new Button("Batch processing");
    private boolean directory;
    private CheckBox checkBox = new CheckBox("CH4");

    @Override
    public void start(final Stage stage) {
        stage.setTitle("Juno Processor");

        FileChooser fileChooser = new FileChooser();

        Label loadLabel = new Label("First, load desired Juno image");
        Label processLabel = new Label("Then process the image using button below");
        progressBar.setProgress(0);

        processButton.setDisable(true);

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
                path = selectedDirectory.getAbsolutePath();
                findFiles(new File(path));
                directory = true;
                processButton.setDisable(false);
            }
        });

        processButton.setOnAction(
                e -> {
                    if (directory) {
                        for (int i = 0; i < paths.size(); i++) {
                            int finalI = i;
                            Runnable runnable = () -> assembleFrames(sliceRAW(Objects.requireNonNull(loadImage(paths.get(finalI), finalI))), finalI);
                            Thread thread = new Thread(runnable);
                            thread.start();
                        }
                    } else {
                        Runnable runnable = () -> assembleFrames(sliceRAW(Objects.requireNonNull(loadImage(path, 0))), 0);
                        Thread thread = new Thread(runnable);
                        thread.start();
                    }

                    progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                });

        final GridPane inputGridPane = new GridPane();

        GridPane.setConstraints(loadLabel, 0, 0);
        GridPane.setConstraints(checkBox, 1, 0);
        GridPane.setConstraints(openButton, 0, 1);
        GridPane.setConstraints(openDirectory, 0, 2);
        GridPane.setConstraints(selectedLabel, 0, 4);
        GridPane.setConstraints(statusLabel, 0, 5);
        GridPane.setConstraints(progressBar, 0, 6);
        GridPane.setConstraints(processLabel, 0, 8);
        GridPane.setConstraints(processButton, 0, 9);
        inputGridPane.setHgap(6);
        inputGridPane.setVgap(6);
        inputGridPane.getChildren().addAll(loadLabel, checkBox, openButton, openDirectory, selectedLabel, statusLabel, progressBar, processLabel, processButton);

        final Pane rootGroup = new VBox(12);
        rootGroup.getChildren().addAll(inputGridPane);
        rootGroup.setPadding(new Insets(12, 12, 12, 12));
        stage.setScene(new Scene(rootGroup));
        stage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private void findFiles(final File folder) {
        paths.clear();
        for (final File fileEntry : Objects.requireNonNull(folder.listFiles())) {
            if (!fileEntry.isDirectory()) {
                paths.add(fileEntry.getPath());
                selectedLabel.setText("Found " + paths.size() + " files.");
            }
        }
    }

    private void openFile(File file) {
        path = file.getPath();
        original_directory = file.getParent();
        name = file.getName();
        int pos = name.lastIndexOf(".");
        if (pos > 0) {
            name = name.substring(0, pos);
        }
        selectedLabel.setText("Current file: " + name);
    }

    private BufferedImage loadImage(String filePath, int i) {
        if (directory) {
            File file = new File(filePath);
            original_directory_array.add(i, file.getParent());
            name_array.add(i, file.getName());
            int pos = name_array.get(i).lastIndexOf(".");
            if (pos > 0) {
                name_array.add(i, name_array.get(i).substring(0, pos));
            }
            Platform.runLater(() -> selectedLabel.setText("Selected " + paths.size() + " files."));
            try {
                return ImageIO.read(file);
            } catch (IOException e) {
                showDialog("Error", "An error happened", "Cannot read file " + path, true);
                e.printStackTrace();
                return null;
            }
        } else {
            try {
                return ImageIO.read(new File(path));
            }
            catch (IOException e) {
                showDialog("Error", "An error happened", "Cannot read file " + path, true);
                e.printStackTrace();
                return null;
            }
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
            showDialog("Error", "An error happened", "", true);
            e.printStackTrace();
        }
        return img_temp[0];
    }

    private BufferedImage[] sliceRAW(BufferedImage rawImage) {
        BufferedImage[] result = new BufferedImage[rawImage.getHeight() / 128];
        for (int i = 0; i < result.length; i++) {
            result[i] = cropImg(rawImage, i);
        }
        return result;
    }

    private void assembleFrames(BufferedImage[] array, int iteration) {
        Platform.runLater(() -> {
            if (!directory) statusLabel.setText("Processing " + name);
            processButton.setDisable(true);
            openButton.setDisable(true);
            openDirectory.setDisable(true);
        });

        if (checkBox.isSelected()) {
            Runnable runnable = () -> {
                BufferedImage finalMethaneImage = new BufferedImage(array[0].getWidth(), (array[0].getHeight() * (array.length)), BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g2d = finalMethaneImage.createGraphics();
                int heightCurrent = 0;
                for (BufferedImage bufferedImage : array) {
                    g2d.drawImage(bufferedImage, 0, heightCurrent, null);
                    heightCurrent += 116;
                }

                try {
                    if (directory) {
                        new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                        ImageIO.write(finalMethaneImage, "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_methane.png"));
                    } else {
                        new File(original_directory + "/" + name + "/").mkdirs();
                        ImageIO.write(finalMethaneImage, "png", new File(original_directory + "/" + name + "/" + name + "_methane.png"));
                    }
                } catch (IOException e) {
                    if (directory) {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_methane.png", true);
                    } else {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory + "/" + name + "/" + name + "_methane.png", true);
                    }
                    e.printStackTrace();
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();

            Runnable runnableWait = () -> {
                try {
                    thread.join();

                    Platform.runLater(() -> {
                        progressBar.setProgress(0);
                        statusLabel.setText("Idle");
                        processButton.setDisable(false);
                        openButton.setDisable(false);
                        openDirectory.setDisable(false);
                        if (!directory) showDialog("Success", "Done", "Finished processing file " + name, false);
                    });
                } catch (InterruptedException e) {
                    showDialog("Error", "An error happened", "", true);
                    e.printStackTrace();
                }
            };
            Thread threadWait = new Thread(runnableWait);
            threadWait.start();
        } else {
            Runnable runnableB = () -> {
                BufferedImage finalBlueImage = new BufferedImage(array[0].getWidth(), (array[0].getHeight() * (array.length / 3)),1);
                Graphics2D g2dB = finalBlueImage.createGraphics();
                int heightCurrentB = 0;
                for (int i = 0; i < array.length; i += 3) { //go through all blue framelets
                    g2dB.drawImage(array[i], 0, heightCurrentB, null);
                    heightCurrentB += 114;
                }

                try {
                    if (directory) {
                        new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                        ImageIO.write(finalBlueImage, "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_blue_channel.png"));
                    } else {
                        new File(original_directory + "/" + name + "/").mkdirs();
                        ImageIO.write(finalBlueImage, "png", new File(original_directory + "/" + name + "/" + name + "_blue_channel.png"));
                    }
                } catch (IOException e) {
                    if (directory) {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_blue_channel.png", true);
                    } else {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory + "/" + name + "/" + name + "_blue_channel.png", true);
                    }
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
                    heightCurrentG += 114;
                }

                try {
                    if (directory) {
                        new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                        ImageIO.write(finalGreenImage, "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_green_channel.png"));
                    } else {
                        new File(original_directory + "/" + name + "/").mkdirs();
                        ImageIO.write(finalGreenImage, "png", new File(original_directory + "/" + name + "/" + name + "_green_channel.png"));
                    }
                } catch (IOException e) {
                    if (directory) {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_green_channel.png", true);
                    } else {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory + "/" + name + "/" + name + "_green_channel.png", true);
                    }
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
                    heightCurrentR +=114;
                }

                try {
                    if (directory) {
                        new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                        ImageIO.write(finalRedImage, "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_red_channel.png"));
                    } else {
                        new File(original_directory + "/" + name + "/").mkdirs();
                        ImageIO.write(finalRedImage, "png", new File(original_directory + "/" + name + "/" + name + "_red_channel.png"));
                    }
                } catch (IOException e) {
                    if (directory) {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_red_channel.png", true);
                    } else {
                        showDialog("Error", "An error happened", "Cannot write to " + original_directory + "/" + name + "/" + name + "_red_channel.png", true);
                    }
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

                    Platform.runLater(() -> {
                        progressBar.setProgress(0);
                        statusLabel.setText("Idle");
                        processButton.setDisable(false);
                        openButton.setDisable(false);
                        openDirectory.setDisable(false);
                        if (!directory) showDialog("Success", "Done", "Finished processing file " + name, false);
                    });
                } catch (InterruptedException e) {
                    showDialog("Error", "An error happened", "", true);
                    e.printStackTrace();
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }

    }

    private void showDialog(String title, String header, String message, boolean error) {
        if (error) {
            statusLabel.setText("Idle");
            processButton.setDisable(false);
            openButton.setDisable(false);
            openDirectory.setDisable(false);
            progressBar.setProgress(0);
        }

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