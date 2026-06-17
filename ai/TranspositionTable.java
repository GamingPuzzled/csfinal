package ai;

import engine.Board;
import engine.MoveGenerator;
import engine.Move;

public class TranspositionTable {

    // Flag types to denote what kind of score is stored
    public static final byte EXACT = 0;   // The exact evaluation of the position
    public static final byte ALPHA = 1;   // A fail-low score (upper bound)
    public static final byte BETA  = 2;   // A fail-high score (lower bound)

    public static class TTEntry {
        public long zobristKey;
        public int score;
        public int depth;
        public byte flags;
        public String bestMove; // Helps move ordering on subsequent visits

        public TTEntry(long zobristKey, int score, int depth, byte flags, String bestMove) {
            this.zobristKey = zobristKey;
            this.score = score;
            this.depth = depth;
            this.flags = flags;
            this.bestMove = bestMove;
        }
    }

    private final TTEntry[] table;
    private final int sizeMask;

    /**
     * Initializes the table. 
     * @param sizePower The power of 2 for the table size (e.g., 20 = 2^20 entries, roughly 1 million entries).
     */
    public TranspositionTable(int sizePower) {
        int size = 1 << sizePower;
        this.table = new TTEntry[size];
        this.sizeMask = size - 1; // Fast bitwise modulo operator
    }

    /**
     * Stores a search result inside the transposition table.
     */
    public void store(long key, int score, int depth, byte flags, String bestMove) {
        // Find the index using the hash key and our bitmask
        int index = (int) (key & sizeMask);

        // Replacement Strategy: Overwrite if empty, or if the new search explored deeper
        if (table[index] == null || depth >= table[index].depth) {
            table[index] = new TTEntry(key, score, depth, flags, bestMove);
        }
    }

    /**
     * Looks up a position in the table. Returns null if not found or if a collision occurs.
     */
    public TTEntry lookup(long key) {
        int index = (int) (key & sizeMask);
        TTEntry entry = table[index];

        // Ensure an entry exists and the keys match perfectly (prevents hash collisions)
        if (entry != null && entry.zobristKey == key) {
            return entry;
        }
        return null;
    }
}