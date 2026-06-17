import engine.Board;
import engine.MoveGenerator;
import ai.Searcher;
import ui.ConsoleUtils;
import ui.ChessNotationParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Board board = new Board();
        List<String> history = new ArrayList<>();
        String lastMove = "";
        int moveNumber = 1;

        ConsoleUtils.clearScreen();
        ConsoleUtils.printWelcomeHeader();
        ConsoleUtils.printGameBoard(board);
        ConsoleUtils.printGameStatus(board, lastMove, moveNumber);

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
            String normalized = input.toLowerCase();
            if (normalized.equals("help")) {
                ConsoleUtils.printHelp();
                continue;
            }
            if (normalized.equals("print")) {
                ConsoleUtils.clearScreen();
                ConsoleUtils.printGameBoard(board);
                ConsoleUtils.printGameStatus(board, lastMove, moveNumber);
                continue;
            }
            if (normalized.equals("moves")) {
                ConsoleUtils.printAvailableMoves(board);
                continue;
            }
            if (normalized.equals("history")) {
                if (history.isEmpty()) {
                    System.out.println("No moves have been played yet.");
                } else {
                    System.out.println("Move history: " + String.join(" ", history));
                }
                continue;
            }
            if (normalized.equals("eval")) {
                ConsoleUtils.printEvaluationSummary(board);
                continue;
            }
            if (normalized.equals("ai")) {
                System.out.println("AI is thinking...");
                String aiMove = Searcher.findBestMove(board, 4);
                if (aiMove != null && board.makeMove(aiMove)) {
                    history.add(aiMove);
                    lastMove = aiMove;
                    moveNumber++;
                    ConsoleUtils.clearScreen();
                    ConsoleUtils.printGameBoard(board);
                    ConsoleUtils.printGameStatus(board, lastMove, moveNumber);
                } else {
                    System.out.println("AI could not find a legal move.");
                }
                continue;
            }
            
            // Try to parse as chess notation first, then fall back to UCI
            String moveToPlay = null;
            
            // Check if it's already valid UCI format
            if (board.isPseudoLegal(input)) {
                moveToPlay = input;
            } else {
                // Try to parse as standard chess notation
                String parsedMove = ChessNotationParser.parseNotationToUCI(input, board);
                if (parsedMove != null && board.isPseudoLegal(parsedMove)) {
                    moveToPlay = parsedMove;
                }
            }

            if (moveToPlay == null) {
                System.out.println("❌ Invalid move. Use chess notation (e4, Nf3, O-O) or UCI format (e2e4).\n");
                ConsoleUtils.printAvailableMoves(board);
                continue;
            }

            if (!board.makeMove(moveToPlay)) {
                System.out.println("❌ That move is illegal in the current position.\n");
                ConsoleUtils.printAvailableMoves(board);
                continue;
            }

            history.add(moveToPlay);
            lastMove = moveToPlay;
            moveNumber++;
            ConsoleUtils.clearScreen();
            ConsoleUtils.printGameBoard(board);
            ConsoleUtils.printGameStatus(board, lastMove, moveNumber);

            // AUTO-PLAY: AI responds for Black immediately
            if (!board.whiteToMove) { // It's now Black's turn (AI's turn)
                // Check for checkmate or stalemate before AI move
                if (MoveGenerator.isInCheckmate(board)) {
                    String winner = board.whiteToMove ? "Black" : "White";
                    System.out.println("\n⚔️  CHECKMATE! " + winner + " wins!");
                    break;
                }
                if (MoveGenerator.isInStalemate(board)) {
                    System.out.println("\n🤝 STALEMATE! The game is a draw.");
                    break;
                }

                System.out.println("\nAI is thinking...");
                String aiMove = Searcher.findBestMove(board, 4);
                if (aiMove != null && board.makeMove(aiMove)) {
                    history.add(aiMove);
                    lastMove = aiMove;
                    moveNumber++;
                    ConsoleUtils.clearScreen();
                    ConsoleUtils.printGameBoard(board);
                    ConsoleUtils.printGameStatus(board, lastMove, moveNumber);
                    System.out.println("AI played: " + aiMove);
                } else {
                    System.out.println("❌ AI could not find a legal move.");
                }
            }
        }
        scanner.close();
    }
}