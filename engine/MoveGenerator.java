package engine;
import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {

    // File and Rank Masks to prevent wrap-around edge effects
    private static final long FILE_A = 0x0101010101010101L;
    private static final long FILE_B = 0x0202020202020202L;
    private static final long FILE_G = 0x4040404040404040L;
    private static final long FILE_H = 0x8080808080808080L;
    
    private static final long RANK_4 = 0x00000000FF000000L;
    private static final long RANK_5 = 0x000000FF00000000L;

    // Precomputed non-sliding attack tables
    private static final long[] KNIGHT_ATTACKS = new long[64];
    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] WHITE_PAWN_ATTACKS = new long[64];
    private static final long[] BLACK_PAWN_ATTACKS = new long[64];

    static {
        initializeLeaperAttacks();
    }

    /**
     * Generates all pseudo-legal moves for the side whose turn it is.
     */
    public static List<String> generatePseudoLegalMoves(Board board) {
        List<String> moves = new ArrayList<>();
        boolean white = board.whiteToMove;
        
        long friendlyPieces = white ? board.whitePieces : board.blackPieces;
        long enemyPieces = white ? board.blackPieces : board.whitePieces;
        long allPieces = board.allPieces;

        int offset = white ? 0 : 6;

        // 1. Pawns (Includes En Passant)
        generatePawnMoves(board, white, allPieces, enemyPieces, moves);

        // 2. Knights
        long knights = board.pieceBitboards[1 + offset];
        while (knights != 0) {
            int fromSq = Long.numberOfTrailingZeros(knights);
            long attacks = KNIGHT_ATTACKS[fromSq] & ~friendlyPieces;
            addMovesToList(fromSq, attacks, moves);
            knights &= (knights - 1);
        }

        // 3. Bishops & Queens (Diagonal Sliders)
        long bishops = board.pieceBitboards[2 + offset] | board.pieceBitboards[4 + offset];
        while (bishops != 0) {
            int fromSq = Long.numberOfTrailingZeros(bishops);
            long attacks = generateDiagonalAttacks(fromSq, allPieces) & ~friendlyPieces;
            addMovesToList(fromSq, attacks, moves);
            bishops &= (bishops - 1);
        }

        // 4. Rooks & Queens (Orthogonal Sliders)
        long rooks = board.pieceBitboards[3 + offset] | board.pieceBitboards[4 + offset];
        while (rooks != 0) {
            int fromSq = Long.numberOfTrailingZeros(rooks);
            long attacks = generateOrthogonalAttacks(fromSq, allPieces) & ~friendlyPieces;
            addMovesToList(fromSq, attacks, moves);
            rooks &= (rooks - 1);
        }

        // 5. Kings & Castling
        long king = board.pieceBitboards[5 + offset];
        if (king != 0) {
            int fromSq = Long.numberOfTrailingZeros(king);
            long attacks = KING_ATTACKS[fromSq] & ~friendlyPieces;
            addMovesToList(fromSq, attacks, moves);
            
            // Castling Generation
            if (white) {
                if ((board.castlingRights & 1) != 0 && (allPieces & 0x60L) == 0) { // e1g1
                    if (!isSquareAttacked(board, 4, false) && !isSquareAttacked(board, 5, false)) moves.add("e1g1");
                }
                if ((board.castlingRights & 2) != 0 && (allPieces & 0x0EL) == 0) { // e1c1
                    if (!isSquareAttacked(board, 4, false) && !isSquareAttacked(board, 3, false)) moves.add("e1c1");
                }
            } else {
                if ((board.castlingRights & 4) != 0 && (allPieces & 0x6000000000000000L) == 0) { // e8g8
                    if (!isSquareAttacked(board, 60, true) && !isSquareAttacked(board, 61, true)) moves.add("e8g8");
                }
                if ((board.castlingRights & 8) != 0 && (allPieces & 0x0E00000000000000L) == 0) { // e8c8
                    if (!isSquareAttacked(board, 60, true) && !isSquareAttacked(board, 59, true)) moves.add("e8c8");
                }
            }
        }

        return moves;
    }

    private static void generatePawnMoves(Board board, boolean white, long allPieces, long enemyPieces, List<String> moves) {
        int offset = white ? 0 : 6;
        long pawns = board.pieceBitboards[0 + offset];
        
        while (pawns != 0) {
            int fromSq = Long.numberOfTrailingZeros(pawns);
            long pawnBit = 1L << fromSq;

            if (white) {
                long singlePush = (pawnBit << 8) & ~allPieces;
                if (singlePush != 0) {
                    addPawnMove(fromSq, fromSq + 8, moves);
                    long doublePush = (singlePush << 8) & ~allPieces & RANK_4;
                    if (doublePush != 0) moves.add(squareToCoord(fromSq) + squareToCoord(fromSq + 16));
                }
                long captureLeft = (pawnBit << 7) & enemyPieces & ~FILE_H;
                long captureRight = (pawnBit << 9) & enemyPieces & ~FILE_A;
                if (captureLeft != 0) addPawnMove(fromSq, fromSq + 7, moves);
                if (captureRight != 0) addPawnMove(fromSq, fromSq + 9, moves);
            } else {
                long singlePush = (pawnBit >>> 8) & ~allPieces;
                if (singlePush != 0) {
                    addPawnMove(fromSq, fromSq - 8, moves);
                    long doublePush = (singlePush >>> 8) & ~allPieces & RANK_5;
                    if (doublePush != 0) moves.add(squareToCoord(fromSq) + squareToCoord(fromSq - 16));
                }
                long captureLeft = (pawnBit >>> 9) & enemyPieces & ~FILE_H;
                long captureRight = (pawnBit >>> 7) & enemyPieces & ~FILE_A;
                if (captureLeft != 0) addPawnMove(fromSq, fromSq - 9, moves);
                if (captureRight != 0) addPawnMove(fromSq, fromSq - 7, moves);
            }

            // En Passant Verification
            if (board.enPassantSquare != -1) {
                long epMask = 1L << board.enPassantSquare;
                if (white) {
                    if (((pawnBit << 7) & ~FILE_H & epMask) != 0) moves.add(squareToCoord(fromSq) + squareToCoord(board.enPassantSquare));
                    if (((pawnBit << 9) & ~FILE_A & epMask) != 0) moves.add(squareToCoord(fromSq) + squareToCoord(board.enPassantSquare));
                } else {
                    if (((pawnBit >>> 9) & ~FILE_H & epMask) != 0) moves.add(squareToCoord(fromSq) + squareToCoord(board.enPassantSquare));
                    if (((pawnBit >>> 7) & ~FILE_A & epMask) != 0) moves.add(squareToCoord(fromSq) + squareToCoord(board.enPassantSquare));
                }
            }
            pawns &= (pawns - 1);
        }
    }

    private static void addPawnMove(int from, int to, List<String> moves) {
        String baseMove = squareToCoord(from) + squareToCoord(to);
        if (to >= 56 || to <= 7) {
            moves.add(baseMove + "q"); moves.add(baseMove + "r");
            moves.add(baseMove + "b"); moves.add(baseMove + "n");
        } else {
            moves.add(baseMove);
        }
    }

    /**
     * Checks if a target square is currently under attack by ANY piece of the specified attacking color.
     * Essential for king safety check rollbacks and castling legality.
     */
    public static boolean isSquareAttacked(Board board, int targetSq, boolean attackedByWhite) {
        int offset = attackedByWhite ? 0 : 6;
        long allPieces = board.allPieces;

        // 1. Check Pawn attacks targeting this square
        long attackingPawns = board.pieceBitboards[0 + offset];
        long pawnAttackMask = attackedByWhite ? BLACK_PAWN_ATTACKS[targetSq] : WHITE_PAWN_ATTACKS[targetSq];
        if ((pawnAttackMask & attackingPawns) != 0) return true;

        // 2. Check Knight attacks targeting this square
        long attackingKnights = board.pieceBitboards[1 + offset];
        if ((KNIGHT_ATTACKS[targetSq] & attackingKnights) != 0) return true;

        // 3. Check King attacks targeting this square
        long attackingKing = board.pieceBitboards[5 + offset];
        if ((KING_ATTACKS[targetSq] & attackingKing) != 0) return true;

        // 4. Check Diagonal (Bishop/Queen) sliding attacks
        long attackingSlidersDiag = board.pieceBitboards[2 + offset] | board.pieceBitboards[4 + offset];
        if ((generateDiagonalAttacks(targetSq, allPieces) & attackingSlidersDiag) != 0) return true;

        // 5. Check Orthogonal (Rook/Queen) sliding attacks
        long attackingSlidersOrtho = board.pieceBitboards[3 + offset] | board.pieceBitboards[4 + offset];
        if ((generateOrthogonalAttacks(targetSq, allPieces) & attackingSlidersOrtho) != 0) return true;

        return false;
    }

    // --- Sliding Piece Ray Casting ---
    private static long generateDiagonalAttacks(int sq, long allPieces) {
        long attacks = 0L;
        int r = sq / 8, f = sq % 8;
        for (int d = 1; r+d < 8 && f+d < 8; d++) { attacks |= (1L << (sq + d*9)); if (((allPieces >> (sq + d*9)) & 1) == 1) break; }
        for (int d = 1; r+d < 8 && f-d >= 0; d++) { attacks |= (1L << (sq + d*7)); if (((allPieces >> (sq + d*7)) & 1) == 1) break; }
        for (int d = 1; r-d >= 0 && f+d < 8; d++) { attacks |= (1L << (sq - d*7)); if (((allPieces >> (sq - d*7)) & 1) == 1) break; }
        for (int d = 1; r-d >= 0 && f-d >= 0; d++) { attacks |= (1L << (sq - d*9)); if (((allPieces >> (sq - d*9)) & 1) == 1) break; }
        return attacks;
    }

    private static long generateOrthogonalAttacks(int sq, long allPieces) {
        long attacks = 0L;
        int r = sq / 8, f = sq % 8;
        for (int d = 1; r+d < 8; d++) { attacks |= (1L << (sq + d*8)); if (((allPieces >> (sq + d*8)) & 1) == 1) break; }
        for (int d = 1; r-d >= 0; d++) { attacks |= (1L << (sq - d*8)); if (((allPieces >> (sq - d*8)) & 1) == 1) break; }
        for (int d = 1; f+d < 8; d++) { attacks |= (1L << (sq + d));   if (((allPieces >> (sq + d)) & 1) == 1) break; }
        for (int d = 1; f-d >= 0; d++) { attacks |= (1L << (sq - d));   if (((allPieces >> (sq - d)) & 1) == 1) break; }
        return attacks;
    }

    private static void addMovesToList(int fromSq, long attacks, List<String> moves) {
        String fromCoord = squareToCoord(fromSq);
        while (attacks != 0) {
            int toSq = Long.numberOfTrailingZeros(attacks);
            moves.add(fromCoord + squareToCoord(toSq));
            attacks &= (attacks - 1);
        }
    }

    public static String squareToCoord(int sq) {
        return "" + (char)('a' + (sq % 8)) + (char)('1' + (sq / 8));
    }

    /**
     * Checks if the specified side is currently in check.
     */
    public static boolean isInCheck(Board board, boolean whiteSide) {
        long kingBitboard = whiteSide ? board.pieceBitboards[Board.WK] : board.pieceBitboards[Board.BK];
        if (kingBitboard == 0L) return false;
        int kingSquare = Long.numberOfTrailingZeros(kingBitboard);
        return isSquareAttacked(board, kingSquare, !whiteSide);
    }

    /**
     * Checks if the current side (whiteToMove) is in checkmate.
     * Checkmate = in check AND no legal moves available
     */
    public static boolean isInCheckmate(Board board) {
        List<String> legalMoves = generatePseudoLegalMoves(board);
        if (!legalMoves.isEmpty()) return false;  // Has legal moves
        return isInCheck(board, board.whiteToMove);  // In check with no moves = checkmate
    }

    /**
     * Checks if the current side (whiteToMove) is in stalemate.
     * Stalemate = NOT in check AND no legal moves available
     */
    public static boolean isInStalemate(Board board) {
        List<String> legalMoves = generatePseudoLegalMoves(board);
        if (!legalMoves.isEmpty()) return false;  // Has legal moves
        return !isInCheck(board, board.whiteToMove);  // Not in check with no moves = stalemate
    }

    private static void initializeLeaperAttacks() {
        for (int i = 0; i < 64; i++) {
            long bit = 1L << i;
            
            // Knights
            long knight = 0L;
            if ((bit & ~FILE_A & ~FILE_B) != 0) { knight |= (bit << 6) | (bit >>> 10); }
            if ((bit & ~FILE_A) != 0)           { knight |= (bit << 15) | (bit >>> 17); }
            if ((bit & ~FILE_H) != 0)           { knight |= (bit << 17) | (bit >>> 15); }
            if ((bit & ~FILE_G & ~FILE_H) != 0) { knight |= (bit << 10) | (bit >>> 6); }
            KNIGHT_ATTACKS[i] = knight;

            // Kings
            long king = 0L;
            king |= (bit << 8) | (bit >>> 8);
            if ((bit & ~FILE_A) != 0) king |= (bit >>> 1) | (bit << 7) | (bit >>> 9);
            if ((bit & ~FILE_H) != 0) king |= (bit << 1) | (bit << 9) | (bit >>> 7);
            KING_ATTACKS[i] = king;

            // Pawns (For reverse lookup checking inside isSquareAttacked)
            long wPawn = 0L;
            if ((bit & ~FILE_A) != 0) wPawn |= (bit << 7);
            if ((bit & ~FILE_H) != 0) wPawn |= (bit << 9);
            WHITE_PAWN_ATTACKS[i] = wPawn;

            long bPawn = 0L;
            if ((bit & ~FILE_A) != 0) bPawn |= (bit >>> 9);
            if ((bit & ~FILE_H) != 0) bPawn |= (bit >>> 7);
            BLACK_PAWN_ATTACKS[i] = bPawn;
        }
    }
}