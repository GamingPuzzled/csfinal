package ui;

import engine.Board;
import engine.MoveGenerator;
import ai.Evaluator;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    private static final String[] PIECE_ICONS = {"P", "N", "B", "R", "Q", "K", "p", "n", "b", "r", "q", "k"};
    private static final String EMPTY_SQUARE = " ";

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static void printWelcomeHeader() {
        System.out.println("====================================================");
        System.out.println("           JAVA BITBOARD CHESS ENGINE UI            ");
        System.out.println("====================================================");
        System.out.println("Welcome! Play moves in UCI format like e2e4, or use commands below.");
        System.out.println("Type 'help' any time for a command summary.");
        System.out.println("====================================================");
    }

    public static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println("  help      - Show this command summary");
        System.out.println("  print     - Refresh the board display");
        System.out.println("  moves     - List all legal moves for current side");
        System.out.println("  history   - Show move history so far");
        System.out.println("  eval      - Show the current engine evaluation");
        System.out.println("  ai        - Let the engine play the best move for the current side");
        System.out.println("  quit      - Exit the program");
        System.out.println("\nMove formats supported:");
        System.out.println("  Chess notation: e4, Nf3, Bc4, exd5, O-O (kingside castle), O-O-O (queenside castle)");
        System.out.println("  UCI format:     e2e4, e7e5, e1g1, e8c8, etc.\n");
    }

    public static String getPlayerInput(Scanner scanner) {
        System.out.print("Command or move > ");
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return "";
    }

    public static void printGameBoard(Board board) {
        System.out.println();
        System.out.println("    a   b   c   d   e   f   g   h");
        System.out.println("  +---+---+---+---+---+---+---+---+");

        for (int rank = 7; rank >= 0; rank--) {
            System.out.print((rank + 1) + " |");
            for (int file = 0; file < 8; file++) {
                int sq = rank * 8 + file;
                String symbol = EMPTY_SQUARE;
                for (int piece = 0; piece < 12; piece++) {
                    if ((board.pieceBitboards[piece] & (1L << sq)) != 0) {
                        symbol = PIECE_ICONS[piece];
                        break;
                    }
                }
                System.out.print(" " + symbol + " |");
            }
            System.out.println(" " + (rank + 1));
            System.out.println("  +---+---+---+---+---+---+---+---+");
        }
        System.out.println("    a   b   c   d   e   f   g   h\n");
    }

    public static void printGameStatus(Board board, String lastMove, int moveNumber) {
        String turn = board.whiteToMove ? "White" : "Black";
        int moveCount = MoveGenerator.generatePseudoLegalMoves(board).size();
        System.out.println("Status: " + turn + " to move | " + moveCount + " legal moves available");
        System.out.println("Last move: " + (lastMove == null || lastMove.isEmpty() ? "None" : lastMove));
        System.out.println("Move number: " + moveNumber);
        System.out.println("Castling: " + getCastlingRights(board) + " | En passant: " + formatEnPassant(board));
        printEvaluationSummary(board);
    }

    public static void printEvaluationSummary(Board board) {
        int evalScore = Evaluator.evaluate(board);
        double evaluationInPawns = evalScore / 100.0;
        String advantage;
        if (evalScore == 0) {
            advantage = "Equal position";
        } else if (evalScore > 0) {
            advantage = "White is better";
        } else {
            advantage = "Black is better";
        }

        System.out.println("--------------------------------------------------");
        System.out.printf("Engine evaluation: %+d centipawns (%+.2f pawns)\n", evalScore, evaluationInPawns);
        System.out.println("Assessment: " + advantage);
        System.out.println("--------------------------------------------------");
    }

    public static void printAvailableMoves(Board board) {
        List<String> moves = MoveGenerator.generatePseudoLegalMoves(board);
        if (moves.isEmpty()) {
            System.out.println("No legal moves available.");
            return;
        }

        System.out.println("Legal moves (" + moves.size() + "):");
        for (int i = 0; i < moves.size(); i++) {
            System.out.printf("%-7s", moves.get(i));
            if ((i + 1) % 6 == 0) System.out.println();
        }
        if (moves.size() % 6 != 0) System.out.println();
        System.out.println();
    }

    private static String getCastlingRights(Board board) {
        StringBuilder builder = new StringBuilder();
        if ((board.castlingRights & 1) != 0) builder.append("K");
        if ((board.castlingRights & 2) != 0) builder.append("Q");
        if ((board.castlingRights & 4) != 0) builder.append("k");
        if ((board.castlingRights & 8) != 0) builder.append("q");
        return builder.length() == 0 ? "-" : builder.toString();
    }

    private static String formatEnPassant(Board board) {
        if (board.enPassantSquare < 0 || board.enPassantSquare > 63) {
            return "-";
        }
        int file = board.enPassantSquare % 8;
        int rank = board.enPassantSquare / 8;
        return "" + (char)('a' + file) + (rank + 1);
    }
}