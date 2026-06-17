package engine;
public class Move {
    public final int fromSq;
    public final int toSq;
    public final char promotion; // 'q', 'r', 'b', 'n' or ' ' for no promotion

    public Move(int fromSq, int toSq) {
        this.fromSq = fromSq;
        this.toSq = toSq;
        this.promotion = ' ';
    }

    public Move(int fromSq, int toSq, char promotion) {
        this.fromSq = fromSq;
        this.toSq = toSq;
        this.promotion = promotion;
    }

    /**
     * Creates a Move object from a standard UCI string (e.g., "e2e4", "a7a8q").
     */
    public static Move fromString(String uciMove) {
        if (uciMove == null || uciMove.length() < 4) {
            return null;
        }
        int from = (uciMove.charAt(0) - 'a') + (uciMove.charAt(1) - '1') * 8;
        int to = (uciMove.charAt(2) - 'a') + (uciMove.charAt(3) - '1') * 8;
        
        char promo = ' ';
        if (uciMove.length() == 5) {
            promo = Character.toLowerCase(uciMove.charAt(4));
        }
        
        return new Move(from, to, promo);
    }

    /**
     * Converts the move back into a standard UCI-compliant string.
     */
    @Override
    public String toString() {
        String moveStr = squareToCoord(fromSq) + squareToCoord(toSq);
        if (promotion != ' ') {
            moveStr += promotion;
        }
        return moveStr;
    }

    private static String squareToCoord(int sq) {
        char file = (char) ('a' + (sq % 8));
        char rank = (char) ('1' + (sq / 8));
        return "" + file + rank;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Move move = (Move) obj;
        return fromSq == move.fromSq && toSq == move.toSq && promotion == move.promotion;
    }

    @Override
    public int hashCode() {
        int result = fromSq;
        result = 31 * result + toSq;
        result = 31 * result + promotion;
        return result;
    }
}