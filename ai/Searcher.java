package ai;

import engine.Board;
import engine.MoveGenerator;
import engine.Move;

import java.util.List;

public class Searcher {
    private static TranspositionTable tt = new TranspositionTable(20);

    /**
     * Entry point for the AI. Finds the best move for the current player
     * using iterative deepening up to a specific max depth.
     */
    public static String findBestMove(Board board, int maxDepth) {
        String bestMove = null;
        
        for (int depth = 1; depth <= maxDepth; depth++) {
            int alpha = -20000000;
            int beta = 20000000;
            
            List<String> legalMoves = MoveGenerator.generatePseudoLegalMoves(board);
            if (legalMoves.isEmpty()) break;

            String bestMoveAtThisDepth = legalMoves.get(0);
            int bestScore = board.whiteToMove ? -20000000 : 20000000;

            for (String move : legalMoves) {
                Board hypothesisBoard = cloneBoard(board);
                if (!hypothesisBoard.makeMove(move)) continue;

                int score = alphaBeta(hypothesisBoard, depth - 1, alpha, beta);

                if (board.whiteToMove) {
                    if (score > bestScore) {
                        bestScore = score;
                        bestMoveAtThisDepth = move;
                    }
                    alpha = Math.max(alpha, score);
                } else {
                    if (score < bestScore) {
                        bestScore = score;
                        bestMoveAtThisDepth = move;
                    }
                    beta = Math.min(beta, score);
                }
                
                if (beta <= alpha) {
                    break; 
                }
            }
            bestMove = bestMoveAtThisDepth;
        }
        
        return bestMove;
    }

    private static int alphaBeta(Board board, int depth, int alpha, int beta) {
        
        // 1. TT Lookup
        TranspositionTable.TTEntry entry = tt.lookup(board.currentHash);
        if (entry != null && entry.depth >= depth) {
            if (entry.flags == TranspositionTable.EXACT) return entry.score;
            if (entry.flags == TranspositionTable.ALPHA && entry.score <= alpha) return entry.score;
            if (entry.flags == TranspositionTable.BETA && entry.score >= beta) return entry.score;
        }

        // Base case: use quiescence search at leaf nodes
        if (depth == 0) return quiescenceSearch(board, alpha, beta);

        List<String> legalMoves = MoveGenerator.generatePseudoLegalMoves(board);
        
        if (legalMoves.isEmpty()) {
            return Evaluator.evaluate(board); 
        }

        // Sort moves using MVV-LVA for better move ordering
        sortMovesByMVVLVA(legalMoves, board);

        int originalAlpha = alpha;
        String bestMoveFound = null;

        if (board.whiteToMove) {
            int maxEval = -20000000;
            for (String move : legalMoves) {
                Board nextBoard = cloneBoard(board);
                if (!nextBoard.makeMove(move)) continue;
                
                int eval = alphaBeta(nextBoard, depth - 1, alpha, beta);
                if (eval > maxEval) {
                    maxEval = eval;
                    bestMoveFound = move;
                }
                alpha = Math.max(alpha, eval);
                
                if (beta <= alpha) {
                    tt.store(board.currentHash, maxEval, depth, TranspositionTable.BETA, bestMoveFound);
                    return maxEval; 
                }
            }
            
            byte flag = (maxEval <= originalAlpha) ? TranspositionTable.ALPHA : TranspositionTable.EXACT;
            tt.store(board.currentHash, maxEval, depth, flag, bestMoveFound);
            return maxEval;

        } else {
            int minEval = 20000000;
            for (String move : legalMoves) {
                Board nextBoard = cloneBoard(board);
                if (!nextBoard.makeMove(move)) continue;
                
                int eval = alphaBeta(nextBoard, depth - 1, alpha, beta);
                if (eval < minEval) {
                    minEval = eval;
                    bestMoveFound = move;
                }
                beta = Math.min(beta, eval);
                
                if (beta <= alpha) {
                    tt.store(board.currentHash, minEval, depth, TranspositionTable.ALPHA, bestMoveFound);
                    return minEval; 
                }
            }
            
            byte flag = (minEval >= beta) ? TranspositionTable.BETA : TranspositionTable.EXACT;
            tt.store(board.currentHash, minEval, depth, flag, bestMoveFound);
            return minEval;
        }
    }

    /**
     * Quiescence Search - extends search at leaf nodes with only forcing moves (captures).
     * Solves the horizon effect by ensuring positions aren't evaluated mid-tactic.
     */
    private static int quiescenceSearch(Board board, int alpha, int beta) {
        // Standing pat: eval current position without making any moves
        int standingPat = Evaluator.evaluate(board);
        
        if (board.whiteToMove) {
            // For white (maximizing), if standing pat is already >= beta, we can cutoff
            if (standingPat >= beta) return standingPat;
            // Update alpha with the best we can do so far (standing pat)
            if (standingPat > alpha) alpha = standingPat;
        } else {
            // For black (minimizing), if standing pat is already <= alpha, we can cutoff
            if (standingPat <= alpha) return standingPat;
            // Update beta with the best we can do so far (standing pat)
            if (standingPat < beta) beta = standingPat;
        }
        
        // Generate only capturing moves for deeper search
        List<String> captures = generateCaptures(board);
        
        if (board.whiteToMove) {
            int maxEval = standingPat;
            for (String move : captures) {
                Board nextBoard = cloneBoard(board);
                if (!nextBoard.makeMove(move)) continue;
                
                int eval = quiescenceSearch(nextBoard, alpha, beta);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                
                if (beta <= alpha) return maxEval;  // Beta cutoff
            }
            return maxEval;
        } else {
            int minEval = standingPat;
            for (String move : captures) {
                Board nextBoard = cloneBoard(board);
                if (!nextBoard.makeMove(move)) continue;
                
                int eval = quiescenceSearch(nextBoard, alpha, beta);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                
                if (beta <= alpha) return minEval;  // Alpha cutoff
            }
            return minEval;
        }
    }

    /**
     * Generates only capturing moves from the current position.
     */
    private static List<String> generateCaptures(Board board) {
        List<String> allMoves = MoveGenerator.generatePseudoLegalMoves(board);
        List<String> captures = new java.util.ArrayList<>();
        
        long opponentPieces = board.whiteToMove ? board.blackPieces : board.whitePieces;
        
        for (String move : allMoves) {
            // Extract destination square
            int toSq = (move.charAt(2) - 'a') + (move.charAt(3) - '1') * 8;
            
            // Check if destination square has an opponent piece
            if (((1L << toSq) & opponentPieces) != 0) {
                captures.add(move);
            }
        }
        
        return captures;
    }

    /**
     * Sorts moves using MVV-LVA (Most Valuable Victim, Least Valuable Attacker) heuristic.
     * Prioritizes captures of valuable pieces by less valuable attacking pieces.
     */
    private static void sortMovesByMVVLVA(List<String> moves, Board board) {
        moves.sort((move1, move2) -> {
            int score1 = calculateMVVLVAScore(move1, board);
            int score2 = calculateMVVLVAScore(move2, board);
            return Integer.compare(score2, score1);  // Descending order (higher scores first)
        });
    }

    /**
     * Calculates MVV-LVA score for a move.
     * Higher values indicate better move ordering priority.
     * Formula: (victim_value * 10) - attacker_value
     */
    private static int calculateMVVLVAScore(String move, Board board) {
        // Extract from and to squares
        int fromSq = (move.charAt(0) - 'a') + (move.charAt(1) - '1') * 8;
        int toSq = (move.charAt(2) - 'a') + (move.charAt(3) - '1') * 8;
        
        // Get victim (piece on destination square)
        int victimValue = Evaluator.getPieceValue(toSq, board);
        
        // Get attacker (piece on source square)
        int attackerValue = Evaluator.getPieceValue(fromSq, board);
        
        // MVV-LVA formula: prioritize capturing valuable pieces with cheap pieces
        return (victimValue * 10) - attackerValue;
    }

    /**
     * Helper to clone the board cleanly with full state sync.
     */
    private static Board cloneBoard(Board original) {
        Board copy = new Board();
        System.arraycopy(original.pieceBitboards, 0, copy.pieceBitboards, 0, 12);
        copy.whitePieces = original.whitePieces;
        copy.blackPieces = original.blackPieces;
        copy.allPieces = original.allPieces;
        copy.whiteToMove = original.whiteToMove;
        copy.currentHash = original.currentHash;
        
        // CRITICAL ADDICTIONS FOR STATE SYNC:
        copy.castlingRights = original.castlingRights;
        copy.enPassantSquare = original.enPassantSquare;
        
        return copy;
    }
}