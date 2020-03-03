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

    //Current best vale
    private int bestValue = 0;
    
    //A list of transpostiontables so the one of the previous depth can still be used
    List<HashMap<Integer, returnObject>> TransPositionTables = new ArrayList<HashMap<Integer, returnObject>>();
    //The transpostiontable of the current depth
    HashMap<Integer, returnObject> TransPositionTable = new HashMap<Integer, returnObject>();
    //The transpostitiontable of the last depth
    HashMap<Integer, returnObject> PreviousTransPositionTable = new HashMap<Integer, returnObject>();
    //An history heuristic list, this gives each possible move a score based upon the amount of times it is chosen as best move in order to order the moves better and optimize alphabeta
    //It is an int[2][51][51] because there are 50 (+1) fields to start and end, and 2 colors the move could be made from (Although it is rare that white and black do the same move, it can happen espeically with kings)
    int[][][] HistoryHeuristic = new int[2][51][51];
    //A list of the best moves of the previous depth in order to optimize alphabeta
    List<Move> previousBestMoves = null;
    
    //The weights of the different evaluation functions
    int piecedifferenceWeight = 90;
    int positionWeight = 5;
    int tempiWeight = 5;
    int piecesSpreadWeight = 15;
    int outPostWeight = 5;
    
    //boolean to enable or diable aspiration search and it's window
    boolean asperationSearch = true;
    int aspirationWindow = 50;
    
    boolean transpositionHistory = true;
    
    /**
     * boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    public Test(int maxSearchDepth, boolean trans) {
        super("robin.png"); // ToDo: replace with your own icon
        this.transpositionHistory = trans;
    }
    
    /**
     * Another constructor which can be called with different weights which is usefull to find the ideal weights
     * @param piecedifferenceWeight the weight for the pieceDifference
     * @param positionWeight the weight for the positionscore
     * @param tempiWeight the weight for the tempiscore
     * @param piecesSpreadWeight the weight for the peice spread score
     * @param outpostWeight the weight for the outpost score
     */
    public Test(int piecedifferenceWeight, int positionWeight, int tempiWeight, int piecesSpreadWeight, int outpostWeight) {
        super("robin.png"); // ToDo: replace with your own icon
        this.piecedifferenceWeight = piecedifferenceWeight;
        this.positionWeight = positionWeight;
        this.tempiWeight = tempiWeight;
        this.piecesSpreadWeight = piecesSpreadWeight;
        this.outPostWeight = outpostWeight;
    }

    @Override
    public Move getMove(DraughtsState s) {
        //Reset bestmove, bestvalue and historyHeuristics
        Move bestMove = null;
        bestValue = 0;
        HistoryHeuristic = new int[2][51][51];
        PreviousTransPositionTable = new HashMap<Integer, returnObject>();
        TransPositionTables = new ArrayList<HashMap<Integer, returnObject>>();
        
        try {
            boolean searching = true;
            int depth = 1;
            
            //Set alpha beta to min anx max value
            int alpha = MIN_VALUE;
            int beta = MAX_VALUE;
                       
            //Limit search till 30 or until it found a solution which wins the game
            while(searching && depth < 30){
                DraughtsNode node = new DraughtsNode(s.clone());
                
                //Initialize a transposition table to keep track of states it knows with it's score
                TransPositionTable = new HashMap<Integer, returnObject>();
                
                //If the depth is 2 or higher get the transposition table of the previous itteration
                if(depth > 1){
                    PreviousTransPositionTable = TransPositionTables.get(depth - 2);                    
                }
                // compute bestMove and bestValue in a call to alphabeta
                returnObject result = alphaBeta(node, alpha, beta, 0, depth);

                //If using asperation search we check if the result is lower than alpha or higher then beta
                //If that is the case we have to redo this depth with the min max values for alpha and beta
                //If this is not the case we know our limited windows was sufficient and thus optimized the search
                if(asperationSearch){
                    if(result.score <= alpha || result.score >= beta){
                        result = alphaBeta(node, MIN_VALUE, MAX_VALUE, 0, depth);
                    }

                    //Alpha and beta for the next depth are the current result + / - the asperation window
                    alpha = result.score - aspirationWindow;
                    beta = result.score + aspirationWindow;
                }

                // store the bestMove found uptill now
                // NB this is not done in case of an AIStoppedException in alphaBeat()
                Move resultmove = node.getBestMove();
                
                //Add the current aspiration table to the list of tables
                TransPositionTables.add(TransPositionTable);
                               
                //In some rare cases, which I was unable to find the cause of the result was null or the result would return a list of moves
                //of which it for some reason thought it were a great set of moves but actually was quite bad. So the validate method
                //checks if the done moves actually result in the given score if this is not the case we know something went wrong 
                //and thus dont' use the result. This is really rare of happening but solved some obvious wrong moves
                if(result != null && Validate(result, node.getState())){
                    bestMove = resultmove;
                    bestValue = result.score;
                    previousBestMoves = result.moves;
                }
                
                //If you are going to win anyway (+-10000 score) there is no need to keep searching till depth 30 so just stop here
                if((bestValue > 10000 && node.getState().isWhiteToMove()) || (bestValue < -10000 && !node.getState().isWhiteToMove())){
                    searching = false;
                }
                
//                System.err.format(
//                        "%s: depth= %2d, best move = %5s, value=%d\n",
//                        this.getClass().getSimpleName(), depth, bestMove, result.score
//                );     
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
        //Get the hashcode of the current state to look and put the result in the transpositiontable
        int hashcode = HashCode(node.getState());
        //Check if the transposition already knows the result for our current state, if so just return it instead of continueing alpha beta for the current state
        if(TransPositionTable.containsKey(hashcode)){
            return TransPositionTable.get(hashcode);
        }  

        DraughtsState state = node.getState();
        returnObject score = new returnObject(0, new ArrayList<Move>());
        
        //If the current state => the maxdepth it may go and is quiet (meaning the next player can not capture a piece) we evaluate the current state and return it
        if((depth >= maxDepth && IsQuiet(state))){
            score.score = evaluate(state);
        } else if(state.isWhiteToMove()){
            score = alphaBetaMax(node, alpha, beta, depth, maxDepth);
        } else {
            score = alphaBetaMin(node, alpha, beta, depth, maxDepth);
        }
        //Put the score of the current state in the transpostion table so if we ever run into it again we dont' have to do alpha beta again for it
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
        
        //Try to get the previous best move of the last itteration for the current depth, this move has a decent chance of still beeing the best move this itteration
        //Meaning we can use it to order the moves better and thus optimize alpha beta
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
        
            //This is alphabeta min, so if the move returns a lower score than the current loweest we prefer that one
            if(result.score < bestScore.score){
                bestScore = result;
                bestMove = move;
                if(depth == 0){
                    node.setBestMove(move);                    
                }
            }
            state.undoMove(move);
            
            //Set beta to be the current lowest score
            beta = Math.min(beta, result.score);
            
            //If alpha >= beta we know we don't have to search any further and just stop the loop to save time
            if(alpha >= beta){
                break;
            }
        }
        //Add the current best move to the return object
        bestScore.moves.add(0, bestMove);
        //Add teh best move the history heuristic so in the future we can use this information because it is likely it is a good move for another state aswell
        SetHistoryHeuristic(bestMove, depth);
        return bestScore;
    }

    /**
     * Alpha beta max
     * @param node the draughtsNode
     * @param alpha The current alpha for cutt ofs
     * @param beta The current beta for cut offs
     * @param depth The current depth
     * @param maxDepth The max depth
     * @return return a returnobject with the moves and best score
     * @throws AIStoppedException 
     */
    returnObject alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth, int maxDepth)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }

        DraughtsState state = node.getState();
        
        //Try to get the previous best move of the last itteration for the current depth, this move has a decent chance of still beeing the best move this itteration
        //Meaning we can use it to order the moves better and thus optimize alpha beta
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

            //This is alphabeta min, so if the move returns a lower score than the current loweest we prefer that one
            if(result.score > bestScore.score){
                bestScore = result;
                bestMove = move;
                if(depth == 0){
                    node.setBestMove(move);                    
                }
            }
            
            //This is alphabeta max, so if the move returns a higher score than the current best we prefer that one
            alpha = Math.max(alpha, result.score);
            if(alpha >= beta){
                //If alpha >= beta we don't have to search the other options because we know they won't be the best move anyway
                break;
            }
        }
        //Add the current best move to the return object
        bestScore.moves.add(0, bestMove);
        //Add teh best move the history heuristic so in the future we can use this information because it is likely it is a good move for another state aswell
        SetHistoryHeuristic(bestMove, depth);
        return bestScore;
    }

    /**
     * A method that evaluates the given state.
     */
    int evaluate(DraughtsState state) {        
        //Calculate the score for each sub evaluation and multiply them by their weights
        //pieceDifference is by far the most important one since losing a piece early on basically means you lost the game
        //So it always tries to keep the peiceScore as high as possible, and when they are equel it will try to get the best
        //score for the other sub evaluations
        int pieceDiffscore = (int)(PieceDifference(state) * piecedifferenceWeight);
        int positionscore = PositionScore(state) * positionWeight;
        int tempiscore = Tempi(state) * tempiWeight;
        int centerPieceScore = KeepCenterPieces(state);
        int piceSpradScore = PieceSpread(state) * piecesSpreadWeight;
        int outPostScore = outPostWeight * OutPostScore(state);
                
        return pieceDiffscore + positionscore + tempiscore + centerPieceScore + piceSpradScore + outPostScore;
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
        //Check if both players still ahve atleast 1 piece otherwise we get divide by 0 stuff
        if(Math.min(whitepieces, blackpieces) >= 1){
            multiplier = (Math.max(blackpieces, whitepieces) + 0.0) / Math.min(blackpieces, whitepieces);
        }
        multiplier *= multiplier;
        //Multiply the difference with a multiplyer which is based on the pieces left.
        //Because for example 3-4 is better for black than 19-20. So the player with already and advantage
        //Will try to trade pieces which would usually be good for him.
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
            //The first and last row can never be the middle of a formation of 3
            if(i > 5 && i < 46)
            {
                //The sides of the board can never be part of a formation
                if(i%10 != 6 && i%10 != 5){
                    if(pieces[i] == 1){
                        //Check if i is an odd or even row
                        if(((i-1)/5)%2 == 0){
                            //Check if it is the middle of a formation of 3
                            if((pieces[i-6] == 1 && pieces[i+5] == 1) || (pieces[i-5] == 1 && pieces[i+4] == 1)){
                                whitepieceScore++;
                            }
                        } else{
                            //Check if it is the middle of a formation of 3
                            if((pieces[i-5] == 1 && pieces[i+6] == 1) || (pieces[i-4] == 1 && pieces[i+5] == 1)){
                                whitepieceScore++;
                            }
                        }
                    }
            
                    if(pieces[i] == 2){
                        //Check if i is an odd or even row
                        if(((i-1)/5)%2 == 0){
                            //Check if it is the middle of a formation of 3
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
    
    /**
     * Return a tempi score
     * @param state the draughtsState
     * @return An integer of the tempi score
     */
    int Tempi(DraughtsState state){
        int[] pieces = state.getPieces();
        int whiteTempi = 0;
        int blackTempi = 0;
        //Count the steps white made forward and subtract the steps black made forward
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
    
    /**
     * Return a hashcode for the current state
     * @param state the drauhgtsState
     * @return an (hopefully) unique code to store the state and score in the transpositionTable
     */
    int HashCode(DraughtsState state){
        int res = Arrays.hashCode(state.getPieces());
        
        //Add 1 to the hashcode if white is to move this differentiates the black and white state if they somehow get in the same position
        if(state.isWhiteToMove()){
            res += 1;
        }
        return res;
    }
  
    /**
     * Return if the current state is quiet, meaning there is no possibility to capture
     * @param state the draughtsState
     * @return true or false if there is a capture possible
     */
    boolean IsQuiet(DraughtsState state){
        if(!state.isEndState()){
            return !state.getMoves().get(0).isCapture();    
        }
        else{
            return true;
        }
    }
    
    /**
     * Get the piece spread score
     * @param state The current draughtsState
     * @param white If white or black is to move
     * @return A score for the given side
     */
    int PieceSpreadPerSide(DraughtsState state, boolean white){
        int[] pieces = state.getPieces();
        
        //If we need to check for white or black pieces
        int comparer = white ? 1 : 2;
        
        int left = 0;
        int middle = 0;
        int right = 0;
        
        //Keep a count of the pieces on the left 3, middle 4, and right 3 columns.
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
        
        //We want a somewhat evenly devided board. We prefer the middle pieces however, so we want 50% of the pieces on the middle 4 rows and 25% on both sides.
        int leftDiff =  Math.min(0, left -(int)(0.25 * total));
        int middleDiff = Math.min(0, middle -(int)(0.5 * total));
        int rightDiff = Math.min(0, right - (int)(0.25 * total));
        
        return leftDiff + middleDiff + rightDiff;
    }
      
    /**
     * Get total piece spread, white - black
     * @param state The current draughtsState, which is the white - black score
     * @return the piece spread score
     */
    int PieceSpread(DraughtsState state){
        return PieceSpreadPerSide(state, true) - PieceSpreadPerSide(state, false);
    }
    
    /**
     * Get an outpost score
     * @param state The current draughtsState
     * @return an outpost score
     */
    int OutPostScore(DraughtsState state){
        int[] pieces = state.getPieces();
        int whiteScore = 0;
        int blackScore = 0;
        
        //We define an outpost that is not backed up by another piece within 2 moves away
        for(int i = 0; i< pieces.length; i++){
            //white outposts can only be in field 1 - 25
            //Black outposts can only be in field 31-50
                        
            if(11 <= i && i <= 25 && pieces[i] == 1){
                //should have another whitepiece in max 1 move away
                // +9, +10, +11, +5, (+4, +6) depending on odd or even row
                int[] piecesToCheck = new int[]{
                    i+9,
                    i+10,
                    i+11,
                    i+5,
                    i+4
                };
                if(((i-1)/5)%2 == 0){
                    piecesToCheck[4] = i+6;
                }
                if(!isPiece(pieces, true, piecesToCheck)){
                    whiteScore--;
                }
            } else if(31 <= i && i <= 40 && pieces[i] == 2){
                //should have another blackpiece in max 1 move away
                // -9, -10, *11, -5, (-4, -6) depending on odd or even row
                int[] piecesToCheck = new int[]{
                    i-9,
                    i-10,
                    i-11,
                    i-5,
                    i-4
                };
                if(((i-1)/5)%2 == 0){
                    piecesToCheck[4] = i-6;
                }
                if(!isPiece(pieces, false, piecesToCheck)){
                    blackScore--;
                }
            }
        }
        
        return whiteScore - blackScore;
    }
    
    /**
     * Check if there is a piece on one of the given locations
     * @param pieces list of all pieces
     * @param white boolean if you look for a white or black piece
     * @param fields the fields to search on
     * @return boolean if there is a piece
     */
    boolean isPiece(int[] pieces, boolean white, int[] fields){
        int piece = white ? 1 : 0;
        int king = white ? 3 : 4;
        for(int i =0; i < fields.length; i++){
            if(pieces[fields[i]] == piece || pieces[fields[i]] == king){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Order the moves to optimize the alpha beta search by finding the optimal move early on
     * @param state The draughtstState
     * @param previousMove The move done in the last itteration on the same depth if it exists
     * @return And ordered list on with the most likely to be best move first
     */
    List<Move> orderMoves(DraughtsState state, Move previousMove){
        List<Move> moves = state.getMoves();
        
        Collections.sort(moves, new Comparator<Move>() {
            @Override
            public int compare(Move lhs, Move rhs) {
                //If the left or right move was the best move for this depth in the previous itteration we always prefer that one
                if(previousMove != null){
                   if(lhs.equals(previousMove)){
                       return -1;
                   } else if(rhs.equals(previousMove)){
                       return 1;
                   }                   
                }
                
                //Get the score from the history heuristic table for both moves
                int lhsScore = HistoryHeuristicScore(lhs);
                int rhsScore = HistoryHeuristicScore(rhs);                    
                
                //If they are equal then we try to sort them based on the last transposition table
                if(lhsScore == rhsScore){
                    lhsScore = PreviousTranspositionScore(lhs, state).score;
                    rhsScore = PreviousTranspositionScore(rhs, state).score;
                    
                    if(!state.isWhiteToMove() && transpositionHistory){
                        lhsScore *= -1;
                        rhsScore *= -1;
                    }
                }
               
                return lhsScore > rhsScore ? -1 : (lhsScore < rhsScore) ? 1 : 0;
            }
        });
        return moves;
    }
    
    /**
     * Get the score of a move based on the transposition table of the previous itteration
     * @param move the move to check
     * @param state the DraughstState
     * @return the score of the move from the previous transTable
     */
    returnObject PreviousTranspositionScore(Move move, DraughtsState state){
        state.doMove(move);
        int hascode = HashCode(state);
        returnObject res = PreviousTransPositionTable.getOrDefault(hascode, new returnObject(MIN_VALUE, new ArrayList<Move>()));
        state.undoMove(move);
        return res;
    }
    
    //Get the score of a move based on it's HistoryHeuristic
    int HistoryHeuristicScore(Move move){
        return HistoryHeuristic[move.isWhiteMove() ? 0 : 1][move.getBeginField()][move.getEndField()];
    }
    
    //set the history heuristic of a move
    void SetHistoryHeuristic(Move move, int depth){
        if(move != null){
            //The history heuristic keeps track how many times and for which deepth a move was deceided that it was the
            //best move possible. Because if a move is chosen as best move 20 times before it is move likely that for the next
            //check it also is the best move rather than anothe move that only has been the best move once. Also how higher
            //The depth the more weight this counter has
            HistoryHeuristic[move.isWhiteMove()? 0 : 1][move.getBeginField()][move.getEndField()] += Math.pow(2, depth);
        }
    }
      
    //For debugging purposes give a custom name to the instance rather than just the class name
    @Override 
    public String getName() {
        return "test " + transpositionHistory;
    }
   
    /**
     * Validate if the given list of moves actually results in the expected score. For some reason
     * This does not always happen (In really rare cases) which sometimes resulted the ai to make
     * stupid moves thinking it was great for some reason. I have not been able to find the bug 
     * that caused this so I just ignore the really rare results where this happens by using this
     * validate method to check if the expected result is actually the result.
     * @param item A returnobject with the moves and expected score
     * @param state A draughtsState to try it on
     * @return true or false if they are equal
     */
    public boolean Validate(returnObject item, DraughtsState state){
        DraughtsState clonedState = state.clone();
        for(Move move: item.moves){
            if(move != null){
                clonedState.doMove(move);
            }
        }
        return evaluate(clonedState) == item.score;
    }
    
    /**
     * An object used to return moves in alphabeta rather than just it's score
     * This way we can return the list of bestmoves that were used when finding
     * the best move. These can then later be used to order the moves in the next
     * itterations
     */
    class returnObject{
        int score;
        List<Move> moves;

        public returnObject(int score, List<Move> moves){
            this.score = score;
            this.moves = moves;
        }

        //A print method to print the list of moves it has
        public String ToString(){
            String res = "Score: " + score + " ";
            for(Move move : moves){
                if(move != null){
                    res += move.getBeginField();
                    res += move.isCapture() ? "x" : "-";
                    res += move.getEndField() + " ";
                }
            }
            return res;
        }
    }
}

