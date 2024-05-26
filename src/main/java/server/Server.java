package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int PORT = 2715;
    private static int boardSize = 3;
    private static char[][] board;
    private static char currentPlayer = 'X';

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT + ". Waiting for clients...");

            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected.");

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            while (true) {
                String[] message = in.readLine().split(" ");
                if (message[0].equals("SIZE")) {
                    boardSize = Integer.parseInt(message[1]);
                    board = new char[boardSize][boardSize];
                    initializeBoard();
                    out.println("SIZE " + boardSize);
                } else if (message[0].equals("MOVE")) {
                    if (board == null) {
                        board = new char[boardSize][boardSize];
                        initializeBoard();
                    }

                    int row = Integer.parseInt(message[2]);
                    int col = Integer.parseInt(message[3]);
                    if (isValidMove(row, col)) {
                        board[row][col] = currentPlayer;
                        out.println("MOVE " + currentPlayer + " " + row + " " + col);
                        if (isWinner(currentPlayer)) {
                            out.println("WIN " + currentPlayer);
                            resetBoard();
                        } else if (isBoardFull()) {
                            out.println("DRAW");
                            resetBoard();
                        } else {
                            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                        }
                    } else {
                        out.println("INVALID_MOVE");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initializeBoard() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                board[i][j] = '\0';
            }
        }
    }

    private static boolean isValidMove(int row, int col) {
        return row >= 0 && row < boardSize && col >= 0 && col < boardSize && board[row][col] == '\0';
    }

    private static boolean isBoardFull() {
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                if (board[i][j] == '\0') {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isWinner(char player) {
        for (int i = 0; i < boardSize; i++) {
            if (checkLine(player, board[i])) return true;
            char[] col = new char[boardSize];
            for (int j = 0; j < boardSize; j++) {
                col[j] = board[j][i];
            }
            if (checkLine(player, col)) return true;
        }

        char[] mainDiagonal = new char[boardSize];
        char[] antiDiagonal = new char[boardSize];
        for (int i = 0; i < boardSize; i++) {
            mainDiagonal[i] = board[i][i];
            antiDiagonal[i] = board[i][boardSize - 1 - i];
        }

        if (checkLine(player, mainDiagonal)) return true;
        return checkLine(player, antiDiagonal);
    }

    private static boolean checkLine(char player, char[] line) {
        if (player == '\0') return false;
        for (char c : line) {
            if (c != player) return false;
        }
        return true;
    }

    private static void resetBoard() {
        board = new char[boardSize][boardSize];
        currentPlayer = 'X';
        initializeBoard();
    }
}
