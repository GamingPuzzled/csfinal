import engine.Board;
import engine.MoveGenerator;
import ai.Searcher;
import ui.ConsoleUtils;

import java.util.Scanner;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Board board = new Board(); // Initializes to the standard start position

        ConsoleUtils.clearScreen();
        ConsoleUtils.printWelcomeHeader();
        board.printBoard();

        while (true) {
            // Check for checkmate or stalemate
            if (MoveGenerator.isInCheckmate(board)) {
                String winner = board.whiteToMove ? "Black" : "White";
                System.out.println("⚔️  CHECKMATE! " + winner + " wins!");
                break;
            }
            if (MoveGenerator.isInStalemate(board)) {
                System.out.println("🤝 STALEMATE! The game is a draw.");
                break;
            }

            String input = ConsoleUtils.getPlayerInput(scanner);

            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Thanks for playing!");
                break;
            } 
            else if (input.equalsIgnoreCase("print")) {
                ConsoleUtils.clearScreen();
                board.printBoard();
                ConsoleUtils.printEvaluationSummary(board);
            } 
            else if (input.equalsIgnoreCase("ai")) {
                System.out.println("AI is thinking...");
                // Search depth 4 (calculates 4 plies ahead)
                String aiMove = Searcher.findBestMove(board, 6);
                
                if (aiMove != null) {
                    System.out.println("AI plays: " + aiMove);
                    board.makeMove(aiMove);
                    ConsoleUtils.clearScreen();
                    board.printBoard();
                    ConsoleUtils.printEvaluationSummary(board);
                } else {
                    System.out.println("AI couldn't find a move (Checkmate/Stalemate).");
                }
            } 
            else {
                // Assume the user typed a move string (e.g., "e2e4")
                // First verify the move appears in the pseudo-legal move list.
                if (!board.isPseudoLegal(input)) {
                    System.out.println("❌ Invalid or Illegal move! Try again (e.g., e2e4).");
                    ConsoleUtils.printAvailableMoves(board);
                    continue;
                }

                boolean success = board.makeMove(input);
                if (success) {
                    ConsoleUtils.clearScreen();
                    board.printBoard();
                    ConsoleUtils.printEvaluationSummary(board);
                    
                    // Optional: Automatically force the AI to respond as Black
                    System.out.println("AI is thinking...");
                    String aiMove = Searcher.findBestMove(board, 4);
                    if (aiMove != null) {
                        board.makeMove(aiMove);
                        ConsoleUtils.clearScreen();
                        board.printBoard();
                        ConsoleUtils.printEvaluationSummary(board);
                    }
                } else {
                    System.out.println("❌ Invalid or Illegal move! Try again (e.g., e2e4).");
                    ConsoleUtils.printAvailableMoves(board);
                }
            }
        }
        scanner.close();
    }
}