package nl.tue.s2id90.group11;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

/**
 * Implementation of the DraughtsPlayer interface.
 *
 * @author huub
 */
// ToDo: rename this class (and hence this file) to have a distinct name
//       for your player during the tournament
public class Group11DraughtsPlayer extends DraughtsPlayer {

    private int bestValue = 0;
    int maxSearchDepth;
    HashMap<Integer, ReturnObject> knownResults = new HashMap<Integer, ReturnObject>();
    HashMap<Integer, Integer> KillerHeuristics = new HashMap<Integer, Integer>();   
    int leafCount = 0;

    /**
     * boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    public Group11DraughtsPlayer(int maxSearchDepth) {
        super("best.png"); // ToDo: replace with your own icon
        this.maxSearchDepth = maxSearchDepth;
    }

    @Override
    public Move getMove(DraughtsState s) {
        Move bestMove = null;
        bestValue = 0;
        System.out.println(s.isWhiteToMove() ? "White" : "Black");
        try {
            boolean searching = true;
            int depth = 1;
            List<Move> PreviousItterationBestMoves = new ArrayList<Move>();
            
            while(searching){
                leafCount = 0;

                DraughtsNode node = new DraughtsNode(s.clone());    // the root of the search tree
                knownResults = new HashMap<Integer, ReturnObject>();
                
                // compute bestMove and bestValue in a call to alphabeta
                ReturnObject newbest = alphaBeta(node, MIN_VALUE, MAX_VALUE, 0, depth, PreviousItterationBestMoves);
                PreviousItterationBestMoves = newbest.moves;

                // store the bestMove found uptill now
                // NB this is not done in case of an AIStoppedException in alphaBeat()
                Move result = node.getBestMove();

                if(result != null){
                    bestMove = result;
                    bestValue = newbest.score;
                }
                
                System.out.println(leafCount);
                
                // print the results for debugging reasons
                System.err.format(
                        "%s: depth= %2d, best move = %5s, value=%d\n",
                        this.getClass().getSimpleName(), depth, bestMove, bestValue
                );     
                depth++;
        }

        } catch (AIStoppedException ex) {
            /* nothing to do */        }

        if (bestMove == null) {
            System.err.println("no valid move found!");
            return getRandomValidMove(s);
        } else {
            return bestMove;
        }
    }

    /**
     * This method's return value is displayed in the AICompetition GUI.
     *
     * @return the value for the draughts state s as it is computed in a call to
     * getMove(s).
     */
    @Override
    public Integer getValue() {
        return bestValue;
    }

    /**
     * Tries to make alphabeta search stop. Search should be implemented such
     * that it throws an AIStoppedException when boolean stopped is set to true;
    *
     */
    @Override
    public void stop() {
        stopped = true;
    }

    /**
     * returns random valid move in state s, or null if no moves exist.
     */
    Move getRandomValidMove(DraughtsState s) {
        List<Move> moves = s.getMoves();
        Collections.shuffle(moves);
        return moves.isEmpty() ? null : moves.get(0);
    }

    /**
     * Implementation of alphabeta that automatically chooses the white player
     * as maximizing player and the black player as minimizing player.
     *
     * @param node contains DraughtsState and has field to which the best move
     * can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the computed value of this node
     * @throws AIStoppedException
     *
     */
    ReturnObject alphaBeta(DraughtsNode node, int alpha, int beta, int depth, int maxDepth, List<Move> prevMoves)
            throws AIStoppedException {
        leafCount++;
        if(depth >= maxDepth && IsQuiet(node.getState())){
            return new ReturnObject(evaluate(node.getState()));
        }      
        if(node.getState().getMoves().isEmpty()){
            return new ReturnObject(node.getState().isWhiteToMove() ? MIN_VALUE : MAX_VALUE);
        }
               
        if (node.getState().isWhiteToMove()) {
            return alphaBetaMax(node, alpha, beta, depth, maxDepth, prevMoves);
        } else {
            return alphaBetaMin(node, alpha, beta, depth, maxDepth, prevMoves);
        }
    }

    /**
     * Does an alphabeta computation with the given alpha and beta where the
     * player that is to move in node is the minimizing player.
     *
     * <p>
     * Typical pieces of code used in this method are:
     * <ul> <li><code>DraughtsState state = node.getState()</code>.</li>
     * <li><code> state.doMove(move); .... ; state.undoMove(move);</code></li>
     * <li><code>node.setBestMove(bestMove);</code></li>
     * <li><code>if(stopped) { stopped=false; throw new AIStoppedException(); }</code></li>
     * </ul>
     * </p>
     *
     * @param node contains DraughtsState and has field to which the best move
     * can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the compute value of this node
     * @throws AIStoppedException thrown whenever the boolean stopped has been
     * set to true.
     */
    ReturnObject alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth, int maxDepth, List<Move> prevMoves)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        
        DraughtsState state = node.getState();
        
        // ToDo: write an alphabeta search to compute bestMove and value
        ReturnObject bestVal = new ReturnObject(MAX_VALUE);
        List<Move> moves = orderMoves(state, depth, prevMoves);
        for (Move move : moves) {
            state.doMove(move);
            
            ReturnObject result = null;
            int hashcode = HashCode(state);
            
            if(knownResults.containsKey(hashcode)){
                result = knownResults.get(hashcode);
            } else{
                result = alphaBeta(node, alpha, beta, depth + 1, maxDepth, prevMoves); 
                knownResults.put(hashcode, result);
            }
            
            if(result.score < bestVal.score && depth == 0){
                node.setBestMove(move);
            }
            if(result.score < bestVal.score){
                bestVal = result;
                bestVal.moves.add(0, move);
            }
            
            state.undoMove(move);
            
            beta = Math.min(beta, bestVal.score);
            if(beta <= alpha){
                break;
            }
        }
        return bestVal;
    }

    ReturnObject alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth, int maxDepth, List<Move> prevMoves)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        
        // ToDo: write an alphabeta search to compute bestMove and value
        ReturnObject bestVal = new ReturnObject(MIN_VALUE);
        List<Move> moves = orderMoves(state, depth, prevMoves);
        for (Move move : moves) {
            state.doMove(move);
            
            ReturnObject result = null;
            int hashcode = HashCode(state);
            
            if(knownResults.containsKey(hashcode)){
                result = knownResults.get(hashcode);
            } else{
                result = alphaBeta(node, alpha, beta, depth + 1, maxDepth, prevMoves); 
                knownResults.put(hashcode, result);
            }
            
            if(result.score > bestVal.score && depth == 0){
                node.setBestMove(move);
            }
            if(result.score > bestVal.score){
                bestVal = result;
                bestVal.moves.add(0, move);
            }
            state.undoMove(move);
            
            alpha = Math.max(alpha, bestVal.score);
            if(beta <= alpha){
               break;
            }
        }
        return bestVal;
    }

    /**
     * A method that evaluates the given state.
     */
    // ToDo: write an appropriate evaluation function
    int evaluate(DraughtsState state) {
        int hashcode = HashCode(state);
        if(KillerHeuristics.containsKey(hashcode)){
            return KillerHeuristics.get(hashcode);
        }
        
        //Devide certain aspects in weights with a total of 100
        int piecedifferenceWeight = 90;
        int positionWeight = 5;
        int tempiWeight = 5;
        int piecesSpreadWeight = 5;
        
        int pieceDiffscore = (int)(PieceDifference(state) * piecedifferenceWeight);
        int positionscore = PositionScore(state) * positionWeight;
        int tempiscore = Tempi(state) * tempiWeight;
        int centerPieceScore = KeepCenterPieces(state);
        int piceSpradScore = PieceSpread(state) * piecesSpreadWeight;
        int isQuietScore = QuietScore(state);
        
        int result = pieceDiffscore + positionscore + tempiscore + centerPieceScore + piceSpradScore + isQuietScore;  
        KillerHeuristics.put(hashcode, result);
        return result;
    }

    //Get the difference in piecies between black and white
    double PieceDifference(DraughtsState state){
        int[] pieces = state.getPieces();
        int blackpieces = 0;
        int whitepieces = 0;
        //Give 1 point per piece, and 3 for kings
        //Compare the result of white - black pieces or the opposide 
        for (int i = 0; i < pieces.length; i++) {
            switch (pieces[i]) {
                case 1:
                    whitepieces++;
                    break;
                case 3:
                    whitepieces += 3;
                    break;
                case 2:
                    blackpieces++;
                    break;
                case 4:
                    blackpieces += 3;
                    break;
                default:
                    break;
            }
        }
        double multiplier = (Math.max(blackpieces, whitepieces) + 0.0) / Math.max(Math.min(blackpieces, whitepieces), 0.01);
        return ((whitepieces - blackpieces) * multiplier);
    }
    
    //Try to keep the center piece if possible, prefer to use pieces at 2, 4 or 47 and 49 instead.
    int KeepCenterPieces(DraughtsState state){
        int score = 0;
        if(state.getPiece(3) == 2){
            score -= 10;
        }
        if(state.getPiece(48) == 1){
            score += 10;
        }
        return score;
    }

    //Get a score of the curren positioning
    int PositionScore(DraughtsState state){
        int[] pieces = state.getPieces();
        int whitepieceScore = 0;
        for(int i=0; i<pieces.length; i++){
            if(i > 5 && i < 46)
            {
                if(i%10 != 6 && i%10 != 5){
                    if(pieces[i] == 1){
                        //Check if i is an odd or even row
                        if(((i-1)/5)%2 == 0){
                            if((pieces[i-6] == 1 && pieces[i+5] == 1) || (pieces[i-5] == 1 && pieces[i+4] == 1)){
                                whitepieceScore++;
                            }
                        } else{
                            if((pieces[i-5] == 1 && pieces[i+6] == 1) || (pieces[i-4] == 1 && pieces[i+5] == 1)){
                                whitepieceScore++;
                            }
                        }
                    }
            
                    if(pieces[i] == 2){
                        if(((i-1)/5)%2 == 0){
                            if((pieces[i-6] == 2 && pieces[i+5] == 2) || (pieces[i-5] == 2 && pieces[i+4] == 2)){
                                whitepieceScore--;
                            }
                        } else{
                            if((pieces[i-5] == 2 && pieces[i+6] == 2) || (pieces[i-4] == 2 && pieces[i+5] == 2)){
                                whitepieceScore--;
                            }
                        }
                    }
                }
            }
        }
        return whitepieceScore;
    }
    
    int Tempi(DraughtsState state){
        int[] pieces = state.getPieces();
        int whiteTempi = 0;
        int blackTempi = 0;
        for(int i=0; i < pieces.length; i++){
            if(pieces[i] == 1){
                whiteTempi += (51-i)/5;
            }
            if(pieces[i] == 2)
            {
                blackTempi += i/5;
            }
        }
        return whiteTempi - blackTempi;
    }
    
    int HashCode(DraughtsState state){
        int[] pieces = state.getPieces();
        if(state.isWhiteToMove()){
            pieces[0] = 10;
        }
        int res = Arrays.hashCode(pieces);
        return res;
    }
  
    boolean IsQuiet(DraughtsState state){
        if(!state.isEndState()){
            return !state.getMoves().get(0).isCapture();    
        }
        else{
            return true;
        }
    }
    
    boolean WhiteWon(DraughtsState state){
        return !state.isWhiteToMove() && state.getMoves().isEmpty();
    }
    
    boolean BlackWon(DraughtsState state){
        return state.isWhiteToMove() && state.getMoves().isEmpty();
    }
    
    int PieceSpreadPerSide(DraughtsState state, boolean white){
        int[] pieces = state.getPieces();
        
        int comparer = white ? 1 : 2;
        
        int left = 0;
        int middle = 0;
        int right = 0;
        
        for(int i=1; i<pieces.length; i++){
            if(pieces[i] == comparer){              
                switch (i%10){
                    case 1:
                    case 6:
                    case 7:
                        left++;
                        break;
                    case 2:
                    case 3:
                    case 8:
                    case 9:
                        middle++;
                        break;
                    case 4:
                    case 5:
                    case 0:
                        right++;
                        break;
                }                
            }
        }
        int total = left + middle + right;
        
        int leftDiff =  Math.min(0, left -(int)(0.25 * total));
        int middleDiff = Math.min(0, middle -(int)(0.5 * total));
        int rightDiff = Math.min(0, right - (int)(0.25 * total));
        
        return leftDiff + middleDiff + rightDiff;
    }
    
    int PieceSpread(DraughtsState state){
        return PieceSpreadPerSide(state, true) - PieceSpreadPerSide(state, false);
    }
    
    int QuietScore(DraughtsState state){
        if(IsQuiet(state)){
            return 0;
        }
        
        if(state.isWhiteToMove()){
            return 50;
        }
        return -50;
    }
    
    List<Move> orderMoves(DraughtsState state, int depth, List<Move> previousItterationMoves){
        List<Move> moves = state.getMoves();
        Collections.sort(moves, new Comparator<Move>() {
            @Override
            public int compare(Move lhs, Move rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                state.doMove(lhs);
                int lhsScore = evaluate(state);
                state.undoMove(lhs);
                state.doMove(rhs);
                int rhsScore = evaluate(state);
                state.undoMove(rhs);    
                
                return lhsScore > rhsScore ? -1 : (lhsScore < rhsScore) ? 1 : 0;
            }
        });
        if(previousItterationMoves.size() > depth){
            Move bestMovePrevItt = previousItterationMoves.get(depth);
            if(moves.contains(bestMovePrevItt)){
                int i = moves.indexOf(bestMovePrevItt);
                Collections.swap(moves, i, 0);
            }
        }

        return moves;
    }
}

class ReturnObject{
    public int score;
    public List<Move> moves;
    
    public ReturnObject(int score){
        this.score = score;
        this.moves = new ArrayList<Move>();
    }
    
    public ReturnObject(int score, List<Move> moves){
        this.score = score;
        this.moves = moves;
    }
}
