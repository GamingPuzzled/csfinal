package ui;

import engine.Board;
import engine.MoveGenerator;
import ai.Searcher;
import ai.Evaluator;

import java.util.Scanner;

public class UCIHandler {

    private static Board board = new Board();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Java Bitboard Chess Engine Ready. Type 'uci' to begin or play directly.");

        while (true) {
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if (input.equals("quit")) {
                break;
            } else if (input.equals("uci")) {
                handleUCI();
            } else if (input.equals("isready")) {
                System.out.println("readyok");
            } else if (input.startsWith("position")) {
                handlePosition(input);
            } else if (input.startsWith("go")) {
                handleGo(input);
            } else if (input.equals("d") || input.equals("print")) {
                // Custom convenience command to display the board in console
                board.printBoard();
            }
        }
        scanner.close();
    }

    private static void handleUCI() {
        System.out.println("id name MyJavaEngine");
        System.out.println("id author YourName");
        System.out.println("uciok");
    }

    private static void handlePosition(String input) {
        // Example: position startpos moves e2e4 e7e5
        if (input.contains("startpos")) {
            board.importFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        } else if (input.contains("fen")) {
            // Extract the FEN string out of the command
            String fen = input.substring(input.indexOf("fen") + 4);
            if (fen.contains("moves")) {
                fen = fen.substring(0, fen.indexOf("moves")).trim();
            }
            board.importFEN(fen);
        }

        // Apply any sequential moves passed by the GUI/User
        if (input.contains("moves")) {
            String[] moves = input.substring(input.indexOf("moves") + 6).split(" ");
            for (String move : moves) {
                if (!move.trim().isEmpty()) {
                    board.makeMove(move.trim());
                }
            }
        }
    }

    private static void handleGo(String input) {
        int depth = 4; // Default baseline search depth

        // Parse user-specified depth if present (e.g., "go depth 5")
        if (input.contains("depth")) {
            String[] parts = input.split(" ");
            for (int i = 0; i < parts.length - 1; i++) {
                if (parts[i].equals("depth")) {
                    try {
                        depth = Integer.parseInt(parts[i + 1]);
                    } catch (NumberFormatException e) {
                        // Keep default depth if parsing fails
                    }
                }
            }
        }

        // Trigger the Alpha-Beta Searcher
        String bestMove = Searcher.findBestMove(board, depth);
        
        // Print the official UCI output so GUI or user knows the final selection
        System.out.println("bestmove " + bestMove);
    }
}