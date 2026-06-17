package engine;
import java.util.Random;

public class Zobrist {

    // [Piece Type][Square 0-63]
    // Piece types mapped exactly to Board.java indexes:
    // 0-5: White (P, N, B, R, Q, K), 6-11: Black (P, N, B, R, Q, K)
    private static final long[][] PIECE_KEYS = new long[12][64];
    // 16 combinations of castling rights (WK, WQ, BK, BQ)
    private static final long[] CASTLING_KEYS = new long[16];
    // 8 files where an en passant capture could happen (or 1 key for no en passant)
    private static final long[] EN_PASSANT_KEYS = new long[9];
    
    // Side to move key (XORed if it's Black's turn)
    private static final long SIDE_KEY;

    static {
        // Use a fixed seed so the random numbers are identical every time the engine runs.
        // This is critical for debugging and consistency.
        Random random = new Random(23081997L);

        // Initialize piece keys
        for (int piece = 0; piece < 12; piece++) {
            for (int square = 0; square < 64; square++) {
                PIECE_KEYS[piece][square] = random.nextLong();
            }
        }

        // Initialize turn key
        SIDE_KEY = random.nextLong();

        for (int i = 0; i < 16; i++) {
            CASTLING_KEYS[i] = random.nextLong();
        }
        for (int i = 0; i < 9; i++) {
            EN_PASSANT_KEYS[i] = random.nextLong();
        }
    }

    /**
     * Computes the Zobrist hash key from scratch for a given board layout.
     * Use this only when initializing a new position (e.g., parsing a FEN).
     */
    public static long calculateHash(Board board) {
        long hash = 0L;

        // Loop through all 12 piece types
        for (int piece = 0; piece < 12; piece++) {
            long bitboard = board.pieceBitboards[piece];
            
            // Extract each set bit (piece location) from the bitboard
            while (bitboard != 0) {
                int square = Long.numberOfTrailingZeros(bitboard);
                hash ^= PIECE_KEYS[piece][square];
                bitboard &= (bitboard - 1); // Clear the processed bit
            }
        }

        // If it is Black's turn, factor in the side key
        if (!board.whiteToMove) {
            hash ^= SIDE_KEY;
        }

        return hash;
    }

    /**
     * Safely updates an existing hash when a piece moves or toggles squares.
     * XORing the same key a second time completely removes it from the hash.
     */
    public static long togglePiece(long currentHash, int piece, int square) {
        return currentHash ^ PIECE_KEYS[piece][square];
    }

    /**
     * Safely updates an existing hash when the turn flips.
     */
    public static long toggleTurn(long currentHash) {
        return currentHash ^ SIDE_KEY;
    }

    public static long toggleCastling(long currentHash, int oldRights, int newRights) {
        return currentHash ^ CASTLING_KEYS[oldRights] ^ CASTLING_KEYS[newRights];
    }

    public static long toggleEnPassant(long currentHash, int oldFile, int newFile) {
        // File 8 represents "no en passant square available"
        return currentHash ^ EN_PASSANT_KEYS[oldFile] ^ EN_PASSANT_KEYS[newFile];
    }
}