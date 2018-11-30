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
    private CheckBox methaneBox = new CheckBox("CH4");
    private CheckBox rgbAlignBox = new CheckBox("RGB Align");
    private int saved_images = 0;

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
                    saved_images = 0;
                    if (!directory) statusLabel.setText("Processing " + name);
                    processButton.setDisable(true);
                    openButton.setDisable(true);
                    openDirectory.setDisable(true);
                    rgbAlignBox.setDisable(true);
                    methaneBox.setDisable(true);

                    if (directory) {
                        Runnable runnable = () -> {
                            for (int i = 0; i < paths.size(); i++) {
                                assembleFrames(sliceRAW(Objects.requireNonNull(loadImage(paths.get(i), i))), i);
                            }
                        };
                        Thread thread = new Thread(runnable);
                        thread.start();
                    } else {
                        assembleFrames(sliceRAW(Objects.requireNonNull(loadImage(path, 0))), 0);
                    }

                    progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                });

        methaneBox.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (rgbAlignBox.isSelected()) {
                rgbAlignBox.setSelected(!t1);
            }
        });

        rgbAlignBox.selectedProperty().addListener((observableValue, aBoolean, t1) -> {
            if (methaneBox.isSelected()) {
                methaneBox.setSelected(!t1);
            }
        });

        final GridPane inputGridPane = new GridPane();

        GridPane.setConstraints(loadLabel, 0, 0);
        GridPane.setConstraints(methaneBox, 1, 0);
        GridPane.setConstraints(rgbAlignBox, 1, 1);
        GridPane.setConstraints(openButton, 0, 1);
        GridPane.setConstraints(openDirectory, 0, 2);
        GridPane.setConstraints(selectedLabel, 0, 4);
        GridPane.setConstraints(statusLabel, 0, 5);
        GridPane.setConstraints(progressBar, 0, 6);
        GridPane.setConstraints(processLabel, 0, 8);
        GridPane.setConstraints(processButton, 0, 9);
        inputGridPane.setHgap(6);
        inputGridPane.setVgap(6);
        inputGridPane.getChildren().addAll(loadLabel, methaneBox, rgbAlignBox, openButton, openDirectory, selectedLabel, statusLabel, progressBar, processLabel, processButton);

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

    private BufferedImage[] sliceRAW(BufferedImage image) {
        BufferedImage[] result = new BufferedImage[image.getHeight() / 128];
        for (int i = 0; i < result.length; i++) {
            result[i] = cropImg(image, i);
        }
        return result;
    }

    private void assembleFrames(BufferedImage[] image, int iteration) {
        final BufferedImage[] finalBlueImage = new BufferedImage[1];
        final BufferedImage[] finalGreenImage = new BufferedImage[1];
        final BufferedImage[] finalRedImage = new BufferedImage[1];

        if (methaneBox.isSelected()) {
            Runnable runnable = () -> {
                BufferedImage finalMethaneImage = new BufferedImage(image[0].getWidth(), (image[0].getHeight() * (image.length)), BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g2d = finalMethaneImage.createGraphics();
                int heightCurrent = 0;
                for (BufferedImage bufferedImage : image) {
                    g2d.drawImage(bufferedImage, 0, heightCurrent, null);
                    heightCurrent += 116;
                }

                g2d.dispose();

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
                        rgbAlignBox.setDisable(false);
                        methaneBox.setDisable(false);
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
                finalBlueImage[0] = new BufferedImage(image[0].getWidth(), (image[0].getHeight() * (image.length / 3)),1);
                Graphics2D g2dB = finalBlueImage[0].createGraphics();
                int heightCurrentB = 0;
                for (int i = 0; i < image.length; i += 3) { //go through all blue framelets
                    g2dB.drawImage(image[i], 0, heightCurrentB, null);
                    heightCurrentB += 114;
                }
                g2dB.dispose();
            };
            Thread threadB = new Thread(runnableB);
            threadB.start();

            Runnable runnableG = () -> {
                finalGreenImage[0] = new BufferedImage(image[0].getWidth(), (image[0].getHeight() * (image.length / 3)),1);
                Graphics2D g2dG = finalGreenImage[0].createGraphics();
                int heightCurrentG = 0;
                for (int i = 1; i < image.length; i += 3) { //go through all green framelets
                    g2dG.drawImage(image[i], 0, heightCurrentG, null);
                    heightCurrentG += 114;
                }
                g2dG.dispose();
            };
            Thread threadG = new Thread(runnableG);
            threadG.start();

            Runnable runnableR = () -> {
                finalRedImage[0] = new BufferedImage(image[0].getWidth() ,(image[0].getHeight()*(image.length/3)),1);
                Graphics2D g2dR = finalRedImage[0].createGraphics();
                int heightCurrentR =0;
                for (int i=2;i<image.length; i += 3) { //go through all red framelets
                    g2dR.drawImage(image[i], 0, heightCurrentR, null);
                    heightCurrentR +=114;
                }
                g2dR.dispose();
            };
            Thread threadR = new Thread(runnableR);
            threadR.start();

            Runnable runnable = () -> {
                try {
                    threadB.join();
                    threadG.join();
                    threadR.join();

                    Runnable runnableSaveR = () -> {
                        try {
                            if (directory) {
                                new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                                ImageIO.write(finalRedImage[0], "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_red_channel.png"));
                            } else {
                                new File(original_directory + "/" + name + "/").mkdirs();
                                ImageIO.write(finalRedImage[0], "png", new File(original_directory + "/" + name + "/" + name + "_red_channel.png"));
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
                    Thread threadSaveR = new Thread(runnableSaveR);
                    threadSaveR.start();

                    Runnable runnableSaveG = () -> {
                        try {
                            if (directory) {
                                new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                                ImageIO.write(finalGreenImage[0], "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_green_channel.png"));
                            } else {
                                new File(original_directory + "/" + name + "/").mkdirs();
                                ImageIO.write(finalGreenImage[0], "png", new File(original_directory + "/" + name + "/" + name + "_green_channel.png"));
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
                    Thread threadSaveG = new Thread(runnableSaveG);
                    threadSaveG.start();

                    Runnable runnableSaveB = () -> {
                        try {
                            if (directory) {
                                new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                                ImageIO.write(finalBlueImage[0], "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_blue_channel.png"));
                            } else {
                                new File(original_directory + "/" + name + "/").mkdirs();
                                ImageIO.write(finalBlueImage[0], "png", new File(original_directory + "/" + name + "/" + name + "_blue_channel.png"));
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
                    Thread threadSaveB = new Thread(runnableSaveB);
                    threadSaveB.start();

                    if (rgbAlignBox.isSelected()) {
                        Runnable runnableAlign = () -> {
                            BufferedImage rgbImage = rgbAlign(finalRedImage[0], finalGreenImage[0], finalBlueImage[0]);
                            try {
                                if (directory) {
                                    new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/").mkdirs();
                                    ImageIO.write(rgbImage, "png", new File(original_directory_array.get(iteration) + "/" + name_array.get(iteration) + "/" + name_array.get(iteration) + "_RGB.png"));
                                } else {
                                    new File(original_directory + "/" + name + "/").mkdirs();
                                    ImageIO.write(rgbImage, "png", new File(original_directory + "/" + name + "/" + name + "_RGB.png"));
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
                        Thread threadAlign = new Thread(runnableAlign);
                        threadAlign.start();
                        threadAlign.join();
                    }

                    threadSaveR.join();
                    threadSaveG.join();
                    threadSaveB.join();

                    saved_images++;

                    if (directory) {
                        if (saved_images == paths.size())
                            Platform.runLater(() -> {
                                progressBar.setProgress(0);
                                statusLabel.setText("Idle");
                                processButton.setDisable(false);
                                openButton.setDisable(false);
                                openDirectory.setDisable(false);
                                rgbAlignBox.setDisable(false);
                                methaneBox.setDisable(false);
                                if (!directory) showDialog("Success", "Done", "Finished processing file " + name, false);
                            });
                    }
                } catch (InterruptedException e) {
                    showDialog("Error", "An error happened", "", true);
                    e.printStackTrace();
                }
            };
            Thread thread = new Thread(runnable);
            thread.start();
        }

    }

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

    private void showDialog(String title, String header, String message, boolean error) {
        if (error) {
            statusLabel.setText("Idle");
            processButton.setDisable(false);
            openButton.setDisable(false);
            openDirectory.setDisable(false);
            rgbAlignBox.setDisable(false);
            methaneBox.setDisable(false);
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