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

        // Base case
        if (depth == 0) return Evaluator.evaluate(board);

        List<String> legalMoves = MoveGenerator.generatePseudoLegalMoves(board);
        
        if (legalMoves.isEmpty()) {
            return Evaluator.evaluate(board); 
        }

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