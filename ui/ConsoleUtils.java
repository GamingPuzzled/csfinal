package ui;

import engine.Board;
import engine.MoveGenerator;
import ai.Searcher;
import ai.Evaluator;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    /**
     * Clears the console screen using standard terminal ANSI escape codes.
     * Makes the gameplay loop look clean and stationary in your terminal window.
     */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Prints a stylized header welcoming players to your custom engine game interface.
     */
    public static void printWelcomeHeader() {
        System.out.println("==================================================");
        System.out.println("       WELCOME TO YOUR JAVA BITBOARD ENGINE       ");
        System.out.println("==================================================");
        System.out.println(" Commands:");
        System.out.println("  [your move] - Type moves in UCI format (e.g., e2e4)");
        System.out.println("  'ai'        - Force the engine to calculate and play");
        System.out.println("  'print'     - Redraw the current board configuration");
        System.out.println("  'quit'      - Exit the chess application");
        System.out.println("==================================================");
    }

    /**
     * Prompts the user for input and wraps terminal reading securely.
     */
    public static String getPlayerInput(Scanner scanner) {
        System.out.print("Your move/command > ");
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return "";
    }

    /**
     * Prints a nicely formatted evaluation summary showing what the AI thinks of the board.
     * Score values are taken directly from Evaluator.java.
     */
    public static void printEvaluationSummary(Board board) {
        int evalScore = Evaluator.evaluate(board);
        double evaluationInPawns = evalScore / 100.0;
        
        System.out.println("--------------------------------------------------");
        System.out.printf("Current Raw Engine Evaluation: %+d centipawns\n", evalScore);
        System.out.printf("Advantage Estimate: %.2f Pawns (%s)\n", 
                Math.abs(evaluationInPawns), 
                evalScore == 0 ? "Equal" : (evalScore > 0 ? "White is better" : "Black is better"));
        System.out.println("--------------------------------------------------");
    }

    /**
     * Displays a list of legal move choices to help you check your generator logic or see options.
     */
    public static void printAvailableMoves(Board board) {
        List<String> moves = MoveGenerator.generatePseudoLegalMoves(board);
        System.out.println("Available Moves: " + moves.toString());
    }
}