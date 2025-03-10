package com.mayo;

import java.io.File;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Screen extends Application {
  private Canvas canvas;
  private static Chip8 chip8;
  private volatile boolean isRunning;

  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("program {filename}");
      return;
    }
    File file = new File(args[0]);
    if (file.exists()) {
      chip8 = new Chip8(args[0]);
      launch(args);
    } else {
      System.out.println("Program does not exist");
    }
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    isRunning = true;
    int width = 1920;
    int height = width / 2;
    canvas = new Canvas(width, height);
    StackPane root = new StackPane(canvas);
    Scene scene = new Scene(root, width, height);
    Thread cpuThread = launchCpuThread();
    draw(chip8.getDisplay());
    AnimationTimer timer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        if (chip8.getDraw()) {
          draw(chip8.getDisplay());
          chip8.setDraw(false);
        }
      }
    };
    timer.start();
    primaryStage.setScene(scene);
    primaryStage.setTitle("Chip8 Emulator");
    primaryStage.show();
    primaryStage.setOnCloseRequest(event -> {
      isRunning = false;
      cpuThread.interrupt();
    });
  }

  public void draw(byte[][] display) {
    int screenWidth = (int) canvas.getWidth() / 64;
    GraphicsContext gc = canvas.getGraphicsContext2D();
    PixelWriter writer = gc.getPixelWriter();
    for (int i = 0; i < 64; i++) {
      for (int j = 0; j < 32; j++) {
        for (int k = 0; k < screenWidth; k++) {
          for (int l = 0; l < screenWidth; l++) {
            writer.setColor(i * screenWidth + k, j * screenWidth + l,
                (display[i][j] != 0 ? Color.RED : Color.BLACK));
          }
        }
      }
    }
  }

  public Thread launchCpuThread() {
    Thread cpuThread = new Thread(() -> {
      while (isRunning) {
        try {
          Thread.sleep(1, 430000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
        chip8.run();
      }
    });
    cpuThread.start();
    return cpuThread;
  }
}
