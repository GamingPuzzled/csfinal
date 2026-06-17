package ui;

import engine.Board;
import engine.MoveGenerator;

import java.util.List;
import java.util.ArrayList;

public class ChessNotationParser {

    /**
     * Converts standard chess notation to UCI format.
     * Supports: e4, Nf3, exd5, O-O, O-O-O, e8=Q, etc.
     * Returns the UCI move string, or null if notation is invalid/ambiguous.
     */
    public static String parseNotationToUCI(String notation, Board board) {
        if (notation == null || notation.isEmpty()) {
            return null;
        }

        String move = notation.trim();

        // Handle castling
        if (move.equals("O-O") || move.equals("0-0")) {
            return board.whiteToMove ? "e1g1" : "e8g8";
        }
        if (move.equals("O-O-O") || move.equals("0-0-0")) {
            return board.whiteToMove ? "e1c1" : "e8c8";
        }

        // Get all legal moves for the current position
        List<String> legalMoves = MoveGenerator.generatePseudoLegalMoves(board);
        
        // Try to match the notation to legal moves
        List<String> matches = findMatchingMoves(move, legalMoves, board);
        
        if (matches.size() == 1) {
            return matches.get(0);
        } else if (matches.size() > 1) {
            return null; // Ambiguous
        }

        return null; // No matches
    }

    private static List<String> findMatchingMoves(String notation, List<String> legalMoves, Board board) {
        List<String> matches = new ArrayList<>();

        // Extract components from notation
        boolean isCapture = notation.contains("x");
        int eqIdx = notation.indexOf("=");
        String promotionChar = eqIdx > 0 ? notation.substring(eqIdx + 1).toLowerCase() : null;
        String cleanNotation = notation.replaceAll("[x=+#]", "").trim();

        for (String uciMove : legalMoves) {
            if (matchesNotation(cleanNotation, uciMove, board, isCapture, promotionChar)) {
                matches.add(uciMove);
            }
        }

        return matches;
    }

    private static boolean matchesNotation(String notation, String uciMove, Board board, 
                                          boolean captureRequired, String promotionRequired) {
        // Parse UCI move: fromSq is chars 0-1, toSq is chars 2-3, promotion is char 4 (optional)
        int fromSq = parseSquare(uciMove.substring(0, 2));
        int toSq = parseSquare(uciMove.substring(2, 4));
        String promotion = uciMove.length() > 4 ? uciMove.substring(4) : "";

        // Check promotion requirement
        if (promotionRequired != null) {
            if (!promotion.equals(promotionRequired)) {
                return false;
            }
        }

        int toFile = toSq % 8;
        int toRank = toSq / 8;
        String toSquareStr = "" + (char)('a' + toFile) + (char)('1' + toRank);

        // Determine what piece is moving
        int piece = getPieceAtSquare(board, fromSq);
        if (piece < 0) return false;

        String pieceLetter = getPieceLetter(piece);

        // Handle pawn moves
        if (pieceLetter.isEmpty()) {
            // Pawn move
            if (notation.length() == 2 && notation.equals(toSquareStr)) {
                // Simple pawn move: e4
                return !captureRequired;
            }
            if (notation.length() >= 3 && notation.endsWith(toSquareStr)) {
                // Pawn capture: exd5 or e4xd5
                return captureRequired;
            }
            return false;
        }

        // Handle piece moves
        if (notation.contains(pieceLetter)) {
            // Extract disambiguation if present (e.g., "Nbd2" or "R1a1")
            String afterPiece = notation.substring(notation.indexOf(pieceLetter) + 1);
            
            // Remove any remaining notation to get target square
            afterPiece = afterPiece.replaceAll("[+#]", "");
            
            if (afterPiece.endsWith(toSquareStr)) {
                // This move ends at the target square
                return true;
            }
        }

        return false;
    }

    private static int getPieceAtSquare(Board board, int square) {
        long mask = 1L << square;
        for (int i = 0; i < 12; i++) {
            if ((board.pieceBitboards[i] & mask) != 0) {
                return i;
            }
        }
        return -1;
    }

    private static String getPieceLetter(int pieceType) {
        switch (pieceType) {
            case Board.WN:
            case Board.BN:
                return "N";
            case Board.WB:
            case Board.BB:
                return "B";
            case Board.WR:
            case Board.BR:
                return "R";
            case Board.WQ:
            case Board.BQ:
                return "Q";
            case Board.WK:
            case Board.BK:
                return "K";
            default:
                return ""; // Pawn
        }
    }

    private static int parseSquare(String sq) {
        if (sq.length() != 2) return -1;
        int file = sq.charAt(0) - 'a';
        int rank = sq.charAt(1) - '1';
        if (file < 0 || file > 7 || rank < 0 || rank > 7) return -1;
        return rank * 8 + file;
    }
}
