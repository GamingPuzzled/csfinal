package engine;
import java.util.Arrays;

public class Board {
    // 12 bitboards: White (P, N, B, R, Q, K), Black (P, N, B, R, Q, K)
    public long[] pieceBitboards = new long[12];
    
    // Combined color occupancy bitboards
    public long whitePieces = 0L;
    public long blackPieces = 0L;
    public long allPieces = 0L;

    // Castling rights represented as a 4-bit integer bitmask:
    // Bit 0 (1): White King Side, Bit 1 (2): White Queen Side
    // Bit 2 (4): Black King Side, Bit 3 (8): Black Queen Side
    public int castlingRights = 15; // 1111 binary (all rights enabled at start)

    // En Passant square index (0-63). Use -1 if no square is available.
    public int enPassantSquare = -1;

    public boolean whiteToMove = true;
    
    // The fast 64-bit fingerprint of the current position layout
    public long currentHash = 0L;

    // Piece Array Indexes mapped to match Zobrist's structures
    public static final int WP=0, WN=1, WB=2, WR=3, WQ=4, WK=5;
    public static final int BP=6, BN=7, BB=8, BR=9, BQ=10, BK=11;

    public Board() {
        // Setup standard starting layout and initialize hash
        importFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    /**
     * Updates composite occupancy bitboards based on the 12 individual piece layers.
     */
    public void updateOccupancy() {
        whitePieces = 0L;
        blackPieces = 0L;
        for (int i = WP; i <= WK; i++) whitePieces |= pieceBitboards[i];
        for (int i = BP; i <= BK; i++) blackPieces |= pieceBitboards[i];
        allPieces = whitePieces | blackPieces;
    }

    /**
     * Executes a move on the board from a UCI-formatted string (e.g. "e2e4").
     * Updates both the underlying bitboards and updates the Zobrist hash incrementally.
     */
    public boolean makeMove(String uciMove) {
        if (uciMove == null) return false;

        String raw = uciMove.trim();
        // Try to locate a UCI-style substring like "e2e4" inside the input.
        // This handles inputs such as "Bf1d3" or long algebraic forms by extracting
        // the final 4- or 5-character UCI move if present.
        String found = null;
        for (int i = 0; i + 4 <= raw.length(); i++) {
            String sub4 = raw.substring(i, i + 4).toLowerCase();
            boolean ok4 = sub4.charAt(0) >= 'a' && sub4.charAt(0) <= 'h'
                && sub4.charAt(1) >= '1' && sub4.charAt(1) <= '8'
                && sub4.charAt(2) >= 'a' && sub4.charAt(2) <= 'h'
                && sub4.charAt(3) >= '1' && sub4.charAt(3) <= '8';
            if (ok4) {
                // Check for 5-char promotion (e.g., e7e8q)
                if (i + 5 <= raw.length()) {
                    char p = Character.toLowerCase(raw.charAt(i + 4));
                    if (p == 'q' || p == 'r' || p == 'b' || p == 'n') {
                        found = raw.substring(i, i + 5).toLowerCase();
                        break;
                    }
                }
                found = sub4;
                break;
            }
        }
        if (found == null) return false;

        uciMove = found;

        int fromSq = (uciMove.charAt(0) - 'a') + (uciMove.charAt(1) - '1') * 8;
        int toSq = (uciMove.charAt(2) - 'a') + (uciMove.charAt(3) - '1') * 8;

        long fromMask = 1L << fromSq;
        long toMask = 1L << toSq;

        int movingPiece = -1;
        int targetPiece = -1;

        // Identify moving piece
        int startIdx = whiteToMove ? WP : BP;
        int endIdx = whiteToMove ? WK : BK;
        for (int i = startIdx; i <= endIdx; i++) {
            if ((pieceBitboards[i] & fromMask) != 0) {
                movingPiece = i;
                break;
            }
        }

        if (movingPiece == -1) return false; // Invalid move selection

        // Check for a standard opponent piece capture
        int oppStartIdx = whiteToMove ? BP : WP;
        int oppEndIdx = whiteToMove ? BK : WK;
        for (int i = oppStartIdx; i <= oppEndIdx; i++) {
            if ((pieceBitboards[i] & toMask) != 0) {
                targetPiece = i;
                break;
            }
        }

        // Save previous state to allow a complete rollback if the move is illegal
        long oldHash = this.currentHash;
        int oldRights = this.castlingRights;
        int oldEpSquare = this.enPassantSquare;
        int oldEpFile = (this.enPassantSquare == -1) ? 8 : (this.enPassantSquare % 8);

        // --- EN PASSANT CAPTURE LOGIC ---
        int epVictimSq = -1;
        int epVictimPiece = -1;
        if ((movingPiece == WP || movingPiece == BP) && toSq == enPassantSquare) {
            epVictimSq = whiteToMove ? toSq - 8 : toSq + 8;
            epVictimPiece = whiteToMove ? BP : WP;
            pieceBitboards[epVictimPiece] &= ~(1L << epVictimSq);
            currentHash = Zobrist.togglePiece(currentHash, epVictimPiece, epVictimSq);
        }

        // --- CASTLING EXECUTION LOGIC ---
        boolean executedCastling = false;
        int castlingRookFrom = -1, castlingRookTo = -1;
        if (movingPiece == WK && fromSq == 4 && toSq == 6) { // White King Side
            castlingRookFrom = 7; castlingRookTo = 5; executedCastling = true;
        } else if (movingPiece == WK && fromSq == 4 && toSq == 2) { // White Queen Side
            castlingRookFrom = 0; castlingRookTo = 3; executedCastling = true;
        } else if (movingPiece == BK && fromSq == 60 && toSq == 62) { // Black King Side
            castlingRookFrom = 63; castlingRookTo = 61; executedCastling = true;
        } else if (movingPiece == BK && fromSq == 60 && toSq == 58) { // Black Queen Side
            castlingRookFrom = 56; castlingRookTo = 59; executedCastling = true;
        }

        if (executedCastling) {
            int rookPiece = whiteToMove ? WR : BR;
            pieceBitboards[rookPiece] &= ~(1L << castlingRookFrom);
            pieceBitboards[rookPiece] |= (1L << castlingRookTo);
            currentHash = Zobrist.togglePiece(currentHash, rookPiece, castlingRookFrom);
            currentHash = Zobrist.togglePiece(currentHash, rookPiece, castlingRookTo);
        }

        // --- STANDARD BITBOARD MUTATIONS ---
        currentHash = Zobrist.togglePiece(currentHash, movingPiece, fromSq);
        if (targetPiece != -1) {
            currentHash = Zobrist.togglePiece(currentHash, targetPiece, toSq);
        }

        pieceBitboards[movingPiece] &= ~fromMask;
        if (targetPiece != -1) {
            pieceBitboards[targetPiece] &= ~toMask;
        }

        // Check for Pawn Promotion Logic
        boolean isPromotion = uciMove.length() == 5 && (movingPiece == WP || movingPiece == BP);
        int promoPiece = -1;
        if (isPromotion) {
            char promoChar = uciMove.charAt(4);
            if (whiteToMove) {
                if (promoChar == 'q') promoPiece = WQ;
                else if (promoChar == 'r') promoPiece = WR;
                else if (promoChar == 'b') promoPiece = WB;
                else if (promoChar == 'n') promoPiece = WN;
            } else {
                if (promoChar == 'q') promoPiece = BQ;
                else if (promoChar == 'r') promoPiece = BR;
                else if (promoChar == 'b') promoPiece = BB;
                else if (promoChar == 'n') promoPiece = BN;
            }

            pieceBitboards[promoPiece] |= toMask;
            currentHash = Zobrist.togglePiece(currentHash, promoPiece, toSq);
        } else {
            pieceBitboards[movingPiece] |= toMask;
            currentHash = Zobrist.togglePiece(currentHash, movingPiece, toSq);
        }

        // --- UPDATE STATE AND CASTLING RIGHTS TRACKING ---
        if (fromSq == 4 || toSq == 4) castlingRights &= ~3;
        if (fromSq == 60 || toSq == 60) castlingRights &= ~12;
        if (fromSq == 7 || toSq == 7) castlingRights &= ~1;
        if (fromSq == 0 || toSq == 0) castlingRights &= ~2;
        if (fromSq == 63 || toSq == 63) castlingRights &= ~4;
        if (fromSq == 56 || toSq == 56) castlingRights &= ~8;

        // Determine if a new En Passant target square opens up
        int newEpSquare = -1;
        if (movingPiece == WP && (toSq - fromSq == 16)) newEpSquare = fromSq + 8;
        if (movingPiece == BP && (fromSq - toSq == 16)) newEpSquare = fromSq - 8;
        this.enPassantSquare = newEpSquare;

        int newEpFile = (this.enPassantSquare == -1) ? 8 : (this.enPassantSquare % 8);

        // Apply metadata Zobrist updates
        currentHash = Zobrist.toggleCastling(currentHash, oldRights, this.castlingRights);
        currentHash = Zobrist.toggleEnPassant(currentHash, oldEpFile, newEpFile);
        currentHash = Zobrist.toggleTurn(currentHash);

        // Temporarily flip occupancy matrices to verify king safety state
        updateOccupancy();

        // --- KING SAFETY / LEGALITY CHECK ---
        // Identify the square of the king belonging to the color that JUST moved
        long kingBitboard = whiteToMove ? pieceBitboards[WK] : pieceBitboards[BK];
        if (kingBitboard == 0L) {
            // Sanity: king missing after the move — treat as illegal and rollback
            pieceBitboards[movingPiece] |= fromMask;
            if (isPromotion) {
                pieceBitboards[promoPiece] &= ~toMask;
            } else {
                pieceBitboards[movingPiece] &= ~toMask;
            }

            if (targetPiece != -1) {
                pieceBitboards[targetPiece] |= toMask;
            }

            if (epVictimSq != -1) {
                pieceBitboards[epVictimPiece] |= (1L << epVictimSq);
            }

            if (executedCastling) {
                int rookPiece = whiteToMove ? WR : BR;
                pieceBitboards[rookPiece] |= (1L << castlingRookFrom);
                pieceBitboards[rookPiece] &= ~(1L << castlingRookTo);
            }

            this.currentHash = oldHash;
            this.castlingRights = oldRights;
            this.enPassantSquare = oldEpSquare;
            updateOccupancy();
            return false;
        }

        int kingSquare = Long.numberOfTrailingZeros(kingBitboard);

        // Pass the opposite side to check if they can strike our king square
        if (MoveGenerator.isSquareAttacked(this, kingSquare, !whiteToMove)) {
            // ROLLBACK: The move was illegal because it exposed or left the king in check!
            pieceBitboards[movingPiece] |= fromMask;
            if (isPromotion) {
                pieceBitboards[promoPiece] &= ~toMask;
            } else {
                pieceBitboards[movingPiece] &= ~toMask;
            }

            if (targetPiece != -1) {
                pieceBitboards[targetPiece] |= toMask;
            }

            // Revert En Passant pieces if modified
            if (epVictimSq != -1) {
                pieceBitboards[epVictimPiece] |= (1L << epVictimSq);
            }

            // Revert Castling rooks if modified
            if (executedCastling) {
                int rookPiece = whiteToMove ? WR : BR;
                pieceBitboards[rookPiece] |= (1L << castlingRookFrom);
                pieceBitboards[rookPiece] &= ~(1L << castlingRookTo);
            }

            // Restore baseline metadata fields
            this.currentHash = oldHash;
            this.castlingRights = oldRights;
            this.enPassantSquare = oldEpSquare;
            
            updateOccupancy();
            return false; 
        }

        // Move was fully legal; finalize side change
        whiteToMove = !whiteToMove;

        // Force a full recalculation from scratch to bypass incremental bugs
        this.currentHash = Zobrist.calculateHash(this);
        return true;
    }

    /**
     * Parses a standard FEN string to establish any custom chess position state.
     * Computes a fresh Zobrist hash from scratch at the end.
     */
    public void importFEN(String fen) {
        Arrays.fill(pieceBitboards, 0L);
        String[] parts = fen.split(" ");
        String rows[] = parts[0].split("/");
        
        for (int r = 0; r < 8; r++) {
            int rank = 7 - r;
            int file = 0;
            for (char c : rows[r].toCharArray()) {
                if (Character.isDigit(c)) {
                    file += Character.getNumericValue(c);
                } else {
                    int sq = rank * 8 + file;
                    int pType = getPieceTypeFromChar(c);
                    if (pType != -1) pieceBitboards[pType] |= (1L << sq);
                    file++;
                }
            }
        }
        whiteToMove = parts[1].equals("w");
        updateOccupancy();
        
        this.castlingRights = 15;
        this.enPassantSquare = -1;

        // 4. Compute the whole initial hash value from scratch for the new position layout
        this.currentHash = Zobrist.calculateHash(this);
    }

    private int getPieceTypeFromChar(char c) {
        switch (c) {
            case 'P': return WP; case 'N': return WN; case 'B': return WB;
            case 'R': return WR; case 'Q': return WQ; case 'K': return WK;
            case 'p': return BP; case 'n': return BN; case 'b': return BB;
            case 'r': return BR; case 'q': return BQ; case 'k': return BK;
            default: return -1;
        }
    }

    /**
     * Renders a robust ASCII graphic representing the board into your system console.
     */
    public void printBoard() {
        System.out.println("\n  +---+---+---+---+---+---+---+---+");
        for (int r = 7; r >= 0; r--) {
            System.out.print((r + 1) + " | ");
            for (int f = 0; f < 8; f++) {
                int sq = r * 8 + f;
                long mask = 1L << sq;
                char symbol = '.';
                
                for (int i = 0; i < 12; i++) {
                    if ((pieceBitboards[i] & mask) != 0) {
                        symbol = getCharFromPieceType(i);
                        break;
                    }
                }
                System.out.print(symbol + " | ");
            }
            System.out.println();
            System.out.println("  +---+---+---+---+---+---+---+---+");
        }
        System.out.println("    a   b   c   d   e   f   g   h\n");
        System.out.println("Turn: " + (whiteToMove ? "White" : "Black"));
        System.out.println("Zobrist Signature (Hex): 0x" + Long.toHexString(currentHash).toUpperCase());
    }

    /**
     * Checks whether the provided input contains a UCI-style move
     * that appears in the current pseudo-legal move list.
     */
    public boolean isPseudoLegal(String input) {
        if (input == null) return false;
        String raw = input.trim();
        String found = null;
        for (int i = 0; i + 4 <= raw.length(); i++) {
            String sub4 = raw.substring(i, i + 4).toLowerCase();
            boolean ok4 = sub4.charAt(0) >= 'a' && sub4.charAt(0) <= 'h'
                && sub4.charAt(1) >= '1' && sub4.charAt(1) <= '8'
                && sub4.charAt(2) >= 'a' && sub4.charAt(2) <= 'h'
                && sub4.charAt(3) >= '1' && sub4.charAt(3) <= '8';
            if (ok4) {
                if (i + 5 <= raw.length()) {
                    char p = Character.toLowerCase(raw.charAt(i + 4));
                    if (p == 'q' || p == 'r' || p == 'b' || p == 'n') {
                        found = raw.substring(i, i + 5).toLowerCase();
                        break;
                    }
                }
                found = sub4;
                break;
            }
        }
        if (found == null) return false;
        java.util.List<String> moves = engine.MoveGenerator.generatePseudoLegalMoves(this);
        return moves.contains(found);
    }

    private char getCharFromPieceType(int type) {
        char[] symbols = {'P', 'N', 'B', 'R', 'Q', 'K', 'p', 'n', 'b', 'r', 'q', 'k'};
        return symbols[type];
    }
}