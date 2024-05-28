package tictactoe_test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class App extends Application {

    private char currentPlayer = 'X';
    private Button[][] buttons;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int boardSize = 3;
    private boolean playerVsComputer = false;
    private char[][] board;
    private static boolean isGameFinished = false;
    private Stage gameStage;
    private int port = 2715;
    private boolean isFirstGame = true;

    @Override
    public void start(Stage primaryStage) {
        showSettingsDialog();
    }

    private void showSettingsDialog() {
        Stage settingsStage = new Stage();
        settingsStage.setTitle("Tic-tac-toe: New game");

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);

        Label sizeLabel = new Label("Choose board size:");
        ComboBox<String> sizeComboBox = new ComboBox<>();
        sizeComboBox.getItems().addAll("3x3", "4x4", "5x5");
        sizeComboBox.setValue("3x3");

        Label modeLabel = new Label("Choose game mode:");
        ComboBox<String> modeComboBox = new ComboBox<>();
        modeComboBox.getItems().addAll("Player vs Player", "Player vs Computer");
        modeComboBox.setValue("Player vs Player");

        Button startButton = new Button("Start Game");
        startButton.setOnAction(e -> {
            String size = sizeComboBox.getValue();
            switch (size) {
                case "4x4":
                    boardSize = 4;
                    break;
                case "5x5":
                    boardSize = 5;
                    break;
                default:
                    boardSize = 3;
            }

            playerVsComputer = modeComboBox.getValue().equals("Player vs Computer");
            settingsStage.close();
            initializeGame();
        });

        Button closeButton = new Button("Exit");
        closeButton.setOnAction(e -> {
            settingsStage.close();
        });

        vbox.getChildren().addAll(sizeLabel, sizeComboBox, modeLabel, modeComboBox, startButton, closeButton);

        Scene scene = new Scene(vbox, 300, 200);
        settingsStage.setScene(scene);
        settingsStage.show();
    }

    private void initializeGame() {
        if (!isFirstGame) {
            out.println("SIZE " + boardSize);
        }
        gameStage = new Stage();
        GridPane pane = new GridPane();
        buttons = new Button[boardSize][boardSize];
        board = new char[boardSize][boardSize];

        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                Button button = new Button();
                button.setPrefSize(100, 100);
                final int row = i;
                final int col = j;
                button.setOnAction(e -> handleButtonClick(button, row, col));
                buttons[i][j] = button;
                pane.add(button, j, i);
            }
        }

        gameStage.setOnCloseRequest(e -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(pane, 100 * boardSize, 100 * boardSize);
        gameStage.setScene(scene);
        gameStage.setTitle("Tic-tac-toe");
        gameStage.show();

        if (isFirstGame) {
            System.out.println("Starting server");
            startServer();
        }
    }

    private void handleButtonClick(Button button, int row, int col) {
        if (button.getText().isEmpty() && (currentPlayer == 'X' || !playerVsComputer)) {
            button.setText(String.valueOf(currentPlayer));
            board[row][col] = currentPlayer;
            out.println("MOVE " + currentPlayer + " " + row + " " + col);
            if (isPlayerWon(currentPlayer, row, col) || isGameFinished) {
                return;
            }
            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        }
        if (playerVsComputer && currentPlayer == 'O' && !isBoardFull()) {
            computerMove();
        }
    }

    private void computerMove() {
        Random rand = new Random();
        int row, col;
        do {
            row = rand.nextInt(boardSize);
            col = rand.nextInt(boardSize);
        } while (board[row][col] != '\0');

        out.println("MOVE O " + row + " " + col);
        buttons[row][col].setText("O");
        board[row][col] = 'O';

        currentPlayer = 'X';
    }

    private void startServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", port);
                System.out.println("Connected to server.");

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("SIZE " + boardSize);

                while (true) {
                    String response = in.readLine();
                    handleServerResponse(response);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleServerResponse(String response) {
        System.out.println(response);
        if (response.startsWith("MOVE")) {
            Platform.runLater(() -> {
                String[] parts = response.split(" ");
                char player = parts[1].charAt(0);
                int row = Integer.parseInt(parts[2]);
                int col = Integer.parseInt(parts[3]);
                buttons[row][col].setText(String.valueOf(player));
                board[row][col] = player;
                // currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
            });
        } else if (response.startsWith("WIN") || response.startsWith("DRAW")) {
            isGameFinished = true;
            Platform.runLater(() -> showEndGameDialog(response));
        }
    }

    private void showEndGameDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game over!");
        if (message.contains("DRAW")) {
            alert.setHeaderText("It's a draw!");
        } else {
            String[] winMessage = message.split(" ");
            alert.setHeaderText("Player \"" + winMessage[1] + "\" wins!");
        }
        ButtonType replayButton = new ButtonType("Replay");
        ButtonType newGameButton = new ButtonType("New Game");
        ButtonType closeButton = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(replayButton, newGameButton, closeButton);

        alert.showAndWait().ifPresent(type -> {
            if (type == replayButton) {
                resetGame();
                System.out.println("Game restarted");
            } else if (type == newGameButton) {
                resetGame();
                showSettingsDialog();
                if (gameStage != null) {
                    gameStage.close();
                }
                System.out.println("New game");
            } else if (type == closeButton) {
                if (gameStage != null) {
                    gameStage.close();
                }
            }
        });
    }

    private void resetGame() {
        isFirstGame = false;
        isGameFinished = false;
        currentPlayer = 'X';
        board = new char[boardSize][boardSize];
        Platform.runLater(() -> {
            for (int i = 0; i < boardSize; i++) {
                for (int j = 0; j < boardSize; j++) {
                    buttons[i][j].setText("");
                }
            }
        });
        System.out.println("Player vs Computer: " + playerVsComputer);
        System.out.println("Is first game: " + isFirstGame);
        out.println("SIZE " + boardSize);
    }

    private boolean isBoardFull() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == '\0') {
                    return false;
                }
            }
        }
        isGameFinished = true;
        return true;
    }

    private boolean isPlayerWon(char player, int row, int col) {
        // Проверка строки
        for (int i = 0; i < boardSize; i++) {
            if (board[row][i] != player) {
                break;
            }
            if (i == boardSize - 1) {
                return true;
            }
        }

        // Проверка столбца
        for (int i = 0; i < boardSize; i++) {
            if (board[i][col] != player) {
                break;
            }
            if (i == boardSize - 1) {
                return true;
            }
        }

        // Проверка главной диагонали
        if (row == col) {
            for (int i = 0; i < boardSize; i++) {
                if (board[i][i] != player) {
                    break;
                }
                if (i == boardSize - 1) {
                    return true;
                }
            }
        }

        // Проверка побочной диагонали
        if (row + col == boardSize - 1) {
            for (int i = 0; i < boardSize; i++) {
                if (board[i][(boardSize - 1) - i] != player) {
                    break;
                }
                if (i == boardSize - 1) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void main(String[] args) {
        launch();
    }
}
