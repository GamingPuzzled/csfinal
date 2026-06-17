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
            // Check if the current side has any pseudo-legal moves left
            List<String> legalMoves = MoveGenerator.generatePseudoLegalMoves(board);
            if (legalMoves.isEmpty()) {
                System.out.println("Game Over! No legal moves available.");
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
                String aiMove = Searcher.findBestMove(board, 4);
                
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