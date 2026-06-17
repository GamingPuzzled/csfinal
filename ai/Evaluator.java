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

    // Mobility and pawn-structure tuning
    private static final int MOBILITY_WEIGHT = 8; // per legal move difference
    private static final int DOUBLED_PAWN_PENALTY = 25;
    private static final int ISOLATED_PAWN_PENALTY = 20;
    private static final int PASSED_PAWN_BONUS = 40;

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

    private static final int[] ROOK_PST = {
        0, 0, 5, 10, 10, 5, 0, 0,
        0, 0, 5, 10, 10, 5, 0, 0,
        0, 0, 5, 10, 10, 5, 0, 0,
        0, 0, 5, 10, 10, 5, 0, 0,
        0, 0, 5, 10, 10, 5, 0, 0,
        0, 0, 5, 10, 10, 5, 0, 0,
        0, 0, 5, 10, 10, 5, 0, 0,
        0, 0, 5, 10, 10, 5, 0, 0
    };

    private static final int[] QUEEN_PST = {
        -10,-5,0,5,5,0,-5,-10,
        -5,0,5,10,10,5,0,-5,
         0,5,10,15,15,10,5,0,
         5,10,15,20,20,15,10,5,
         5,10,15,20,20,15,10,5,
         0,5,10,15,15,10,5,0,
        -5,0,5,10,10,5,0,-5,
       -10,-5,0,5,5,0,-5,-10
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
        mgScore += evaluatePieceType(board.pieceBitboards[Board.WR], ROOK_VALUE, ROOK_PST, true);
        mgScore += evaluatePieceType(board.pieceBitboards[Board.WQ], QUEEN_VALUE, QUEEN_PST, true);

        egScore += evaluatePieceType(board.pieceBitboards[Board.WP], PAWN_VALUE, PAWN_PST, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WN], KNIGHT_VALUE, KNIGHT_PST, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WB], BISHOP_VALUE, BISHOP_PST, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WR], ROOK_VALUE, ROOK_PST, true);
        egScore += evaluatePieceType(board.pieceBitboards[Board.WQ], QUEEN_VALUE, QUEEN_PST, true);

        // Black Pieces (Subtract from score)
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BP], PAWN_VALUE, PAWN_PST, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BN], KNIGHT_VALUE, KNIGHT_PST, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BB], BISHOP_VALUE, BISHOP_PST, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BR], ROOK_VALUE, ROOK_PST, false);
        mgScore -= evaluatePieceType(board.pieceBitboards[Board.BQ], QUEEN_VALUE, QUEEN_PST, false);

        egScore -= evaluatePieceType(board.pieceBitboards[Board.BP], PAWN_VALUE, PAWN_PST, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BN], KNIGHT_VALUE, KNIGHT_PST, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BB], BISHOP_VALUE, BISHOP_PST, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BR], ROOK_VALUE, ROOK_PST, false);
        egScore -= evaluatePieceType(board.pieceBitboards[Board.BQ], QUEEN_VALUE, QUEEN_PST, false);

        // --- 2. COMPUTE GAME PHASE ---
        int totalPhasePieces = 
            Long.bitCount(board.pieceBitboards[Board.WN] | board.pieceBitboards[Board.BN]) * 1 +
            Long.bitCount(board.pieceBitboards[Board.WB] | board.pieceBitboards[Board.BB]) * 1 +
            Long.bitCount(board.pieceBitboards[Board.WR] | board.pieceBitboards[Board.BR]) * 2 +
            Long.bitCount(board.pieceBitboards[Board.WQ] | board.pieceBitboards[Board.BQ]) * 4;

        int maxPhase = 24; 
        int phase = Math.min(totalPhasePieces, maxPhase);

        // --- 3. EVALUATE KINGS ---
        long wkBB = board.pieceBitboards[Board.WK];
        long bkBB = board.pieceBitboards[Board.BK];

        int whiteKingSq = (wkBB == 0L) ? -1 : Long.numberOfTrailingZeros(wkBB);
        int blackKingSq = (bkBB == 0L) ? -1 : Long.numberOfTrailingZeros(bkBB);

        // Add core king value baseline to both phase tracks
        mgScore += KING_VALUE; egScore += KING_VALUE;
        mgScore -= KING_VALUE; egScore -= KING_VALUE;

        // Apply position table logic only when the king exists on the board.
        if (whiteKingSq != -1) {
            mgScore += KING_MIDDLEGAME_PST[whiteKingSq];
            egScore += KING_ENDGAME_PST[whiteKingSq];
        }
        if (blackKingSq != -1) {
            mgScore -= KING_MIDDLEGAME_PST[flipVertical(blackKingSq)];
            egScore -= KING_ENDGAME_PST[flipVertical(blackKingSq)];
        }

        // --- 4. ADDITIONAL TERMS: MOBILITY & PAWN-STRUCTURE ---
        // Mobility: count pseudo-legal moves for each side (cheap heuristic)
        boolean origSide = board.whiteToMove;
        board.whiteToMove = true;
        int whiteMobility = MoveGenerator.generatePseudoLegalMoves(board).size();
        board.whiteToMove = false;
        int blackMobility = MoveGenerator.generatePseudoLegalMoves(board).size();
        board.whiteToMove = origSide;

        mgScore += MOBILITY_WEIGHT * (whiteMobility - blackMobility);

        // Pawn structure: doubled, isolated, and passed pawns
        long wPawns = board.pieceBitboards[Board.WP];
        long bPawns = board.pieceBitboards[Board.BP];

        for (int f = 0; f < 8; f++) {
            long fileMask = 0x0101010101010101L << f;
            int wCount = Long.bitCount(wPawns & fileMask);
            int bCount = Long.bitCount(bPawns & fileMask);
            if (wCount > 1) mgScore -= DOUBLED_PAWN_PENALTY * (wCount - 1);
            if (bCount > 1) mgScore += DOUBLED_PAWN_PENALTY * (bCount - 1);

            boolean wIsolated = (wPawns & ( (f>0? (fileMask>>1):0L) | (f<7? (fileMask<<1):0L) )) == 0 && wCount > 0;
            boolean bIsolated = (bPawns & ( (f>0? (fileMask>>1):0L) | (f<7? (fileMask<<1):0L) )) == 0 && bCount > 0;
            if (wIsolated) mgScore -= ISOLATED_PAWN_PENALTY * wCount;
            if (bIsolated) mgScore += ISOLATED_PAWN_PENALTY * bCount;
        }

        // Passed pawns: iterate over white pawns and black pawns
        long wp = wPawns;
        while (wp != 0) {
            int sq = Long.numberOfTrailingZeros(wp);
            int f = sq % 8; int r = sq / 8;
            long mask = 0L;
            for (int rr = r+1; rr < 8; rr++) {
                for (int ff = Math.max(0, f-1); ff <= Math.min(7, f+1); ff++) {
                    mask |= 1L << (rr*8 + ff);
                }
            }
            if ((bPawns & mask) == 0) mgScore += PASSED_PAWN_BONUS;
            wp &= (wp - 1);
        }

        long bp = bPawns;
        while (bp != 0) {
            int sq = Long.numberOfTrailingZeros(bp);
            int f = sq % 8; int r = sq / 8;
            long mask = 0L;
            for (int rr = r-1; rr >= 0; rr--) {
                for (int ff = Math.max(0, f-1); ff <= Math.min(7, f+1); ff++) {
                    mask |= 1L << (rr*8 + ff);
                }
            }
            if ((wPawns & mask) == 0) mgScore -= PASSED_PAWN_BONUS;
            bp &= (bp - 1);
        }

        // --- 5. TAPERED INTERPOLATION ---
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

    /**
     * Gets the value of the piece at a given square.
     * Used for MVV-LVA move ordering.
     * Returns 0 if no piece is on the square.
     */
    public static int getPieceValue(int square, Board board) {
        long squareMask = 1L << square;
        
        // Check white pieces
        if ((board.pieceBitboards[Board.WP] & squareMask) != 0) return PAWN_VALUE;
        if ((board.pieceBitboards[Board.WN] & squareMask) != 0) return KNIGHT_VALUE;
        if ((board.pieceBitboards[Board.WB] & squareMask) != 0) return BISHOP_VALUE;
        if ((board.pieceBitboards[Board.WR] & squareMask) != 0) return ROOK_VALUE;
        if ((board.pieceBitboards[Board.WQ] & squareMask) != 0) return QUEEN_VALUE;
        if ((board.pieceBitboards[Board.WK] & squareMask) != 0) return KING_VALUE;
        
        // Check black pieces
        if ((board.pieceBitboards[Board.BP] & squareMask) != 0) return PAWN_VALUE;
        if ((board.pieceBitboards[Board.BN] & squareMask) != 0) return KNIGHT_VALUE;
        if ((board.pieceBitboards[Board.BB] & squareMask) != 0) return BISHOP_VALUE;
        if ((board.pieceBitboards[Board.BR] & squareMask) != 0) return ROOK_VALUE;
        if ((board.pieceBitboards[Board.BQ] & squareMask) != 0) return QUEEN_VALUE;
        if ((board.pieceBitboards[Board.BK] & squareMask) != 0) return KING_VALUE;
        
        return 0;  // No piece on this square
    }
}