package nl.tue.s2id90.group11;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import java.util.Collections;
import java.util.List;
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
        bestValue = MIN_VALUE;
        try {
            for(int i = 1; i < maxSearchDepth +20; i++){
                
            DraughtsNode node = new DraughtsNode(s.clone());    // the root of the search tree

            DraughtsState endstate = null;
            
            // compute bestMove and bestValue in a call to alphabeta
            int newbest = alphaBeta(node, MIN_VALUE, MAX_VALUE, 0, i, s.isWhiteToMove(), endstate);

            // store the bestMove found uptill now
            // NB this is not done in case of an AIStoppedException in alphaBeat()
            Move result = node.getBestMove();

            if(result != null){
                bestMove = result;
                bestValue = newbest;
            }
            
            // print the results for debugging reasons
            System.err.format(
                    "%s: depth= %2d, best move = %5s, value=%d\n",
                    this.getClass().getSimpleName(), i, bestMove, bestValue
            );            
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
    int alphaBeta(DraughtsNode node, int alpha, int beta, int depth, int maxDepth, boolean isWhite, DraughtsState endstate)
            throws AIStoppedException {
        if(depth >= maxDepth && IsQuiet(node.getState())){
            return evaluate(node.getState(), isWhite);
        }      
        if(node.getState().isEndState()){
            return MIN_VALUE;
        }
               
        if (depth%2 == 0) {
            return alphaBetaMax(node, alpha, beta, depth, maxDepth, isWhite, endstate);
        } else {
            return alphaBetaMin(node, alpha, beta, depth, maxDepth, isWhite, endstate);
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
    int alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth, int maxDepth, boolean isWhite, DraughtsState endstate)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        
        DraughtsState state = node.getState();
        
        // ToDo: write an alphabeta search to compute bestMove and value
        int bestVal = MAX_VALUE;
        List<Move> moves = state.getMoves();
        for (Move move : moves) {
            state.doMove(move);
            int score = alphaBeta(node, alpha, beta, depth + 1, maxDepth, isWhite, endstate);
            if(score <= bestVal && depth == 0){
                node.setBestMove(move);
                endstate = state.clone();
            }
            bestVal = Math.min(bestVal, score);
            
            state.undoMove(move);
            
            beta = Math.min( beta, bestVal);
            if(beta <= alpha){
                break;
            }
        }
        return bestVal;
    }

    int alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth, int maxDepth, boolean isWhite, DraughtsState endstate)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        
        // ToDo: write an alphabeta search to compute bestMove and value
        int bestVal = MIN_VALUE;
        List<Move> moves = state.getMoves();
        for (Move move : moves) {
            state.doMove(move);
            
            int score = alphaBeta(node, alpha, beta, depth + 1, maxDepth, isWhite, endstate);
            if(score > bestVal && depth == 0){
                node.setBestMove(move);
                endstate = state.clone();
            }
            bestVal = Math.max(bestVal, score);
            
            state.undoMove(move);
            
            alpha = Math.max( alpha, bestVal);
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
    int evaluate(DraughtsState state, boolean isWhite) {
        DraughtsNode node = new DraughtsNode(state);
        //Devide certain aspects in weights with a total of 100
        int piecedifferenceWeight = 90;
        int positionWeight = 9;
        int tempiWeight = 1;
        int piecePostionWeight = 10;
        
        int pieceDiffscore = PieceDifference(state, isWhite) * piecedifferenceWeight;
        int positionscore = Position2Score(state) * positionWeight;
        int tempiscore = Tempi(state) * tempiWeight;
        //int piecePosition = PiecePostion(state) * piecePostionWeight;

        int multiplier = 1;
        if(!isWhite){
            multiplier = -1;
        }
        return multiplier * (pieceDiffscore + positionscore + tempiscore);
    }

    //Get the difference in piecies between black and white
    int PieceDifference(DraughtsState state, boolean isWhite){
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
                    whitepieces += 4;
                    break;
                case 2:
                    blackpieces++;
                    break;
                case 4:
                    blackpieces += 4;
                    break;
                default:
                    break;
            }
        }
        return whitepieces - blackpieces;
    }

    //Get a score of the curren positioning
    int Position2Score(DraughtsState state){
        int[] pieces = state.getPieces();
        int whitepieceScore = 0;
        for(int i=0; i<pieces.length; i++){
            if(i > 5 && i < 46)
            {
                if(i%10 != 6 && i%10 != 5){
                    if(pieces[i] == 1){
                        if((pieces[i-5] == 1 && pieces[i+4] == 1) || (pieces[i-6] == 1 && pieces[i+5] == 1)){
                            whitepieceScore++;
                        }
                    }
            
                    if(pieces[i] == 2){
                        if((pieces[i-5] == 2 && pieces[i+4] == 2) || (pieces[i-6] == 2 && pieces[i+5] == 2)){
                            whitepieceScore--;
                        }
                      }
                }
            }
        }
        return whitepieceScore;
    }

    int PositionScore(DraughtsState state) {
        int[] pieces = state.getPieces();
        int wValue = 0;
        int bValue = 0;
        int[] corners = new int[]{5, 6, 15, 16, 25, 26, 35, 36, 45, 46};

        for (int i = 0; i < pieces.length; i = i + 1) {
            for (int p = 0; p < corners.length; p++) {
                if (corners[p] == i) {
                    switch (pieces[i]) {
                    case 1:
                        wValue++;
                        break;
                    case 3:
                        wValue++;
                        break;
                    default:
                        break;
                    }
                }
            }
        }
        return -wValue;
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
    
    //Try to keep the peices devided wel acros the board
    int PiecePostion(DraughtsState state){
        
        int[] whitepieces = WhitePieces(state);
        int[] blackpieces = BlackPieces(state);

        int[] diffpercolumn = new int[10];
        for(int i=0; i<whitepieces.length; i++){
            diffpercolumn[whitepieces[i]%10] += 1;
        }
        
        for(int i=0; i<blackpieces.length; i++){
            diffpercolumn[blackpieces[i]%10] -= 1;
        }
        
        int result = 0;
        for(int i=0; i<diffpercolumn.length; i++){
            //Add the square of the difference. So it For example if black got 3 more pieces in column 1 
            //it counts as a bigger advantage as white having 3x1 more piece in the other columns
            result += diffpercolumn[i] * diffpercolumn[i];
        }
        
        return result *-1;
    }
    
    int[] WhitePieces(DraughtsState state){
        int[] whitepieces = new int[20];
        int[] pieces = state.getPieces();
        int count = 0;
        for(int i=1; i<pieces.length; i++){
            int piece = pieces[i];
            if(piece == 1 || piece == 3){
                whitepieces[count] = i;
                count++;
            }
        }
        return whitepieces;
    }
    
    int[] BlackPieces(DraughtsState state){
        int[] blackpieces = new int[20];
        int[] pieces = state.getPieces();
        int count = 0;
        for(int i=1; i<pieces.length; i++){
            int piece = pieces[i];
            if(piece == 2 || piece == 4){
                blackpieces[count] = i;
                count++;
            }
        }
        return blackpieces;
    }
    
    //Count the amount of pieces that are either completly blocked or would be captured directly
    int blockedPiecesScore(DraughtsState state){
        List<Move> posmoves = state.getMoves();
        for(int i=0; i<posmoves.size(); i++){
            
        }
        return 0;
    }
    
    boolean IsQuiet(DraughtsState state){
        if(!state.isEndState()){
            return !state.getMoves().get(0).isCapture();    
        }
        else{
            return true;
        }
    }
}
