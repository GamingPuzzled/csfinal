package ai;

import engine.Board;
import engine.MoveGenerator;
import engine.Move;

public class Evaluator {

    // Material values in Centipawns (1 pawn = 100 points)
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 320;
    private static final int BISHOP_VALUE = 330;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;

    // Piece-Square Tables (Visualized from White's perspective: Index 0 is A1, Index 63 is H8)
    private static final int[] PAWN_PST = {
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_PST = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_PST = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] KING_MIDDLEGAME_PST = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    private static final int[] KING_ENDGAME_PST = {
        -50,-30,-30,-30,-30,-30,-30,-50,
        -30,-10,  0,  0,  0,  0,-10,-30,
        -30,  0, 20, 30, 30, 20,  0,-30,
        -30,  0, 30, 40, 40, 30,  0,-30,
        -30,  0, 30, 40, 40, 30,  0,-30,
        -30,  0, 20, 30, 30, 20,  0,-30,
        -30,-10,  0,  0,  0,  0,-10,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };

    /**
     * Statically scores the current position.
     * Positive scores mean White is winning. Negative scores mean Black is winning.
     */
    public static int evaluate(Board board) {
        int mgScore = 0;
        int egScore = 0;

        // --- 1. MATERIAL & POSITIONAL VALUE CALCULATION ---
        // White Pieces (Add to score)
        mgScore += evaluatePieceType(board.pieceBitboards[Board.WP], PAWN_VALUE, PAWN_PST, true);
        mgScore += evaluatePieceType(board.pieceBitboards[Board.WN], KNIGHT_VALUE, KNIGHT_PST, true);
        mgScore += evaluatePieceType(board.pieceBitboards[Board.WB], BISHOP_VALUE, BISHOP_PST, true);
        mgScore += evaluatePieceType(board.pieceBitboards[Board.WR], ROOK_VALUE, null, true);
        mgScore += evaluatePieceType(board.pieceBitboards[Board.WQ], QUEEN_VALUE, null, true);

        egScore += evaluatePieceType(board.pieceBitboards[Board.WP], PAWN_VALUE, PAWN_PST, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WN], KNIGHT_VALUE, KNIGHT_PST, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WB], BISHOP_VALUE, BISHOP_PST, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WR], ROOK_VALUE, null, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WQ], QUEEN_VALUE, null, true);

        // Black Pieces (Subtract from score)
        String s = "";
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BP], PAWN_VALUE, PAWN_PST, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BN], KNIGHT_VALUE, KNIGHT_PST, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BB], BISHOP_VALUE, BISHOP_PST, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BR], ROOK_VALUE, null, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BQ], QUEEN_VALUE, null, false);

        egScore -= evaluatePieceType(board.pieceBitboards[Board.BP], PAWN_VALUE, PAWN_PST, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BN], KNIGHT_VALUE, KNIGHT_PST, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BB], BISHOP_VALUE, BISHOP_PST, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BR], ROOK_VALUE, null, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BQ], QUEEN_VALUE, null, false);

        // --- 2. COMPUTE GAME PHASE ---
        int totalPhasePieces = 
            Long.bitCount(board.pieceBitboards[Board.WN] | board.pieceBitboards[Board.BN]) * 1 +
            Long.bitCount(board.pieceBitboards[Board.WB] | board.pieceBitboards[Board.BB]) * 1 +
            Long.bitCount(board.pieceBitboards[Board.WR] | board.pieceBitboards[Board.BR]) * 2 +
            Long.bitCount(board.pieceBitboards[Board.WQ] | board.pieceBitboards[Board.BQ]) * 4;

        int maxPhase = 24; 
        int phase = Math.min(totalPhasePieces, maxPhase);

        // --- 3. EVALUATE KINGS ---
        int whiteKingSq = Long.numberOfTrailingZeros(board.pieceBitboards[Board.WK]);
        int blackKingSq = Long.numberOfTrailingZeros(board.pieceBitboards[Board.BK]);

        // Add core king value baseline to both phase tracks
        mgScore += KING_VALUE; egScore += KING_VALUE;
        mgScore -= KING_VALUE; egScore -= KING_VALUE;
        
        // Apply position table logic
        mgScore += KING_MIDDLEGAME_PST[whiteKingSq];
        egScore += KING_ENDGAME_PST[whiteKingSq];

        mgScore -= KING_MIDDLEGAME_PST[flipVertical(blackKingSq)];
        egScore -= KING_ENDGAME_PST[flipVertical(blackKingSq)];

        // --- 4. TAPERED INTERPOLATION ---
        int finalScore = ((mgScore * phase) + (egScore * (maxPhase - phase))) / maxPhase;

        return finalScore;
    }

    /**
     * Loops through a bitboard layer using a local reference copy. 
     * Keeps original array layers completely safe from clearing.
     */
    private static int evaluatePieceType(long bitboardLayer, int baseValue, int[] pst, boolean isWhite) {
        int score = 0;
        long workingBitboard = bitboardLayer; 
        
        while (workingBitboard != 0) {
            int sq = Long.numberOfTrailingZeros(workingBitboard);
            score += baseValue;
            
            if (pst != null) {
                int tableIndex = isWhite ? sq : flipVertical(sq);
                score += pst[tableIndex];
            }
            
            workingBitboard &= (workingBitboard - 1); 
        }
        return score;
    }

    private static int flipVertical(int sq) {
        return sq ^ 56;
    }
}