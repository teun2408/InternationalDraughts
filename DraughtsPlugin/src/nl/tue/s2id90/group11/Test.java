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
public class Test extends DraughtsPlayer {

    private int bestValue = 0;
    List<HashMap<Integer, returnObject>> TransPositionTables = new ArrayList<HashMap<Integer, returnObject>>();
    HashMap<Integer, returnObject> TransPositionTable = new HashMap<Integer, returnObject>();
    HashMap<Integer, returnObject> PreviousTransPositionTable = new HashMap<Integer, returnObject>();
    int[][][] HistoryHeuristic = new int[2][51][51];
    int nodeCount = 0;
    List<Move> previousBestMoves = null;
    
    int piecedifferenceWeight = 90;
    int positionWeight = 5;
    int tempiWeight = 5;
    int piecesSpreadWeight = 15;
    
    boolean asperationSearch = false;
    int aspirationWindow = 50;
    
    /**
     * boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    public Test(int maxSearchDepth) {
        super("best.png"); // ToDo: replace with your own icon
    }
    
    public Test(boolean asperationSearch, int aspirationWindow) {
        super("best.png"); // ToDo: replace with your own icon
        this.asperationSearch = asperationSearch;
        this.aspirationWindow = aspirationWindow;
    }
    
    public Test(int piecedifferenceWeight, int positionWeight, int tempiWeight, int piecesSpreadWeight) {
        super("best.png"); // ToDo: replace with your own icon
        this.piecedifferenceWeight = piecedifferenceWeight;
        this.positionWeight = positionWeight;
        this.tempiWeight = tempiWeight;
        this.piecesSpreadWeight = piecesSpreadWeight;
    }

    @Override
    public Move getMove(DraughtsState s) {
        Move bestMove = null;
        bestValue = 0;
        HistoryHeuristic = new int[2][51][51];
        
        try {
            boolean searching = true;
            int depth = 1;
            
            int alpha = MIN_VALUE;
            int beta = MAX_VALUE;
            
            long start = System.currentTimeMillis();
            
            while(searching && depth < 30){
                DraughtsNode node = new DraughtsNode(s.clone());
                TransPositionTable = new HashMap<Integer, returnObject>();
                if(depth > 1){
                    PreviousTransPositionTable = TransPositionTables.get(depth - 2);                    
                }
                nodeCount = 0;
                // compute bestMove and bestValue in a call to alphabeta
                returnObject result = alphaBeta(node, alpha, beta, 0, depth);

                if(asperationSearch){
                    if(result.score <= alpha || result.score >= beta){
                        result = alphaBeta(node, MIN_VALUE, MAX_VALUE, 0, depth);
                    }

                    alpha = result.score - aspirationWindow;
                    beta = result.score + aspirationWindow;
                }

                // store the bestMove found uptill now
                // NB this is not done in case of an AIStoppedException in alphaBeat()
                Move resultmove = node.getBestMove();
                
                TransPositionTables.add(TransPositionTable);
                               
                if(result != null && Validate(result, node.getState())){
                    bestMove = resultmove;
                    bestValue = result.score;
                    previousBestMoves = result.moves;
                }
                
                if((bestValue > 10000 && node.getState().isWhiteToMove()) || (bestValue < -10000 && !node.getState().isWhiteToMove())){
                    searching = false;
                }
                
                System.out.println(nodeCount);                
                System.out.println(System.currentTimeMillis() - start);
                System.out.println(result.ToString());

                System.err.format(
                        "%s: depth= %2d, best move = %5s, value=%d\n",
                        this.getClass().getSimpleName(), depth, bestMove, result.score
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
    returnObject alphaBeta(DraughtsNode node, int alpha, int beta, int depth, int maxDepth)
            throws AIStoppedException {
        int hashcode = HashCode(node.getState());
        if(TransPositionTable.containsKey(hashcode)){
            return TransPositionTable.get(hashcode);
        }  
        nodeCount++;

        DraughtsState state = node.getState();
        returnObject score = new returnObject(0, new ArrayList<Move>());
        
        if((depth >= maxDepth && IsQuiet(state))){
            score.score = evaluate(state);
        } else if(state.isWhiteToMove()){
            score = alphaBetaMax(node, alpha, beta, depth, maxDepth);
        } else {
            score = alphaBetaMin(node, alpha, beta, depth, maxDepth);
        }
        TransPositionTable.put(hashcode, score);
        return score;
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
    returnObject alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth, int maxDepth)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        
        DraughtsState state = node.getState();
        Move prev = null;
        if(previousBestMoves != null && previousBestMoves.size() >= depth){
            prev = previousBestMoves.get(Math.max(depth - 1, 0));
        }
        List<Move> moves = orderMoves(state, prev);
        
        returnObject bestScore = new returnObject(MAX_VALUE, new ArrayList<Move>());
        Move bestMove = null;
        
        for(Move move : moves){
            state.doMove(move);
            
            returnObject result = alphaBeta(node, alpha, beta, depth + 1, maxDepth);
        
            if(result.score < bestScore.score){
                bestScore = result;
                bestMove = move;
                if(depth == 0){
                    node.setBestMove(move);                    
                }
            }
            state.undoMove(move);
            
            beta = Math.min(beta, result.score);
            if(alpha >= beta){
                break;
            }
        }
        bestScore.moves.add(0, bestMove);
        SetHistoryHeuristic(bestMove, depth);
        return bestScore;
    }

    returnObject alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth, int maxDepth)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }

        DraughtsState state = node.getState();
        Move prev = null;
        if(previousBestMoves != null && previousBestMoves.size() >= depth){
            prev = previousBestMoves.get(Math.max(depth - 1, 0));
        }
        List<Move> moves = orderMoves(state, prev);
              
        returnObject bestScore = new returnObject(MIN_VALUE, new ArrayList<Move>());
        Move bestMove = null;
        
        for(Move move : moves){
            state.doMove(move);
            returnObject result = alphaBeta(node, alpha, beta, depth + 1, maxDepth);
            state.undoMove(move);

            if(result.score > bestScore.score){
                bestScore = result;
                bestMove = move;
                if(depth == 0){
                    node.setBestMove(move);                    
                }
            }
            
            alpha = Math.max(alpha, result.score);
            if(alpha >= beta){
                break;
            }
        }
        bestScore.moves.add(0, bestMove);
        SetHistoryHeuristic(bestMove, depth);
        return bestScore;
    }

    /**
     * A method that evaluates the given state.
     */
    int evaluate(DraughtsState state) {        
        //Devide certain aspects in weights with a total of 100
        int pieceDiffscore = piecedifferenceWeight == 0 ? 0 : (int)(PieceDifference(state) * piecedifferenceWeight);
        int positionscore = positionWeight == 0 ? 0 : PositionScore(state) * positionWeight;
        int tempiscore = tempiWeight == 0 ? 0 : Tempi(state) * tempiWeight;
        int centerPieceScore = KeepCenterPieces(state);
        int piceSpradScore = piecesSpreadWeight == 0 ? 0 : PieceSpread(state) * piecesSpreadWeight;
        
        return pieceDiffscore + positionscore + tempiscore + centerPieceScore + piceSpradScore;
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
        double multiplier = 10;
        if(Math.min(whitepieces, blackpieces) >= 1){
            multiplier = (Math.max(blackpieces, whitepieces) + 0.0) / Math.min(blackpieces, whitepieces);
        }
        multiplier *= multiplier;
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
        int res = Arrays.hashCode(state.getPieces());
        if(state.isWhiteToMove()){
            res += 1;
        }
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
    
    List<Move> orderMoves(DraughtsState state, Move previousMove){
        List<Move> moves = state.getMoves();
        
        Collections.sort(moves, new Comparator<Move>() {
            @Override
            public int compare(Move lhs, Move rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending

                if(previousMove != null){
                   if(lhs.equals(previousMove)){
                       return -1;
                   } else if(rhs.equals(previousMove)){
                       return 1;
                   }                   
                }
                
                int lhsScore = HistoryHeuristicScore(lhs);
                int rhsScore = HistoryHeuristicScore(rhs);                    
                
                if(lhsScore == rhsScore){
                    lhsScore = PreviousTranspositionScore(lhs, state).score;
                    rhsScore = PreviousTranspositionScore(rhs, state).score;
                }
               
                return lhsScore > rhsScore ? -1 : (lhsScore < rhsScore) ? 1 : 0;
            }
        });
        return moves;
    }
    
    returnObject PreviousTranspositionScore(Move move, DraughtsState state){
        state.doMove(move);
        int hascode = HashCode(state);
        returnObject res = PreviousTransPositionTable.getOrDefault(hascode, new returnObject(MIN_VALUE, new ArrayList<Move>()));
        state.undoMove(move);
        return res;
    }
    
    int HistoryHeuristicScore(Move move){
        return HistoryHeuristic[move.isWhiteMove() ? 0 : 1][move.getBeginField()][move.getEndField()];
    }
    void SetHistoryHeuristic(Move move, int depth){
        if(move != null){
            HistoryHeuristic[move.isWhiteMove()? 0 : 1][move.getBeginField()][move.getEndField()] += Math.pow(2, depth);
        }
    }
    
    class TranspositionItem{
        public int score;
        public List<Move> moves;
        
        public TranspositionItem(int score, List<Move> moves){
            this.score = score;
            this.moves = moves;
        }
    }
    
    @Override 
    public String getName() {
        return "test " + asperationSearch + " " + aspirationWindow;
    }
    
    public boolean Validate(returnObject item, DraughtsState state){
        DraughtsState clonedState = state.clone();
        for(Move move: item.moves){
            clonedState.doMove(move);
        }
        int actual = evaluate(clonedState);
        boolean result = actual == item.score;
        if(!result){
            System.out.println("Expected: " + item.score + " Found: " + actual);
        }
        
        return result;
    }
    
    class returnObject{
        int score;
        List<Move> moves;

        public returnObject(int score, List<Move> moves){
            this.score = score;
            this.moves = moves;
        }

        public String ToString(){
            String res = "Score: " + score + " ";
            for(Move move : moves){
                res += move.getBeginField();
                res += move.isCapture() ? "x" : "-";
                res += move.getEndField() + " ";
            }
            return res;
        }
    }
}

