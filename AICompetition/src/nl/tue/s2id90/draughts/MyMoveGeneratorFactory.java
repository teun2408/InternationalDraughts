package nl.tue.s2id90.draughts;

import org10x10.dam.game.BoardState;
import org10x10.dam.game.MoveFilter;
import org10x10.dam.game.MoveGenerator;

/**
 *
 * @author huub
 */
public class MyMoveGeneratorFactory {
    
    public static MoveGenerator create(BoardState bs, boolean useKillerRules) {
         if (useKillerRules) {
             boolean longMoves=true;
             boolean backwardsCapture=true;
             boolean promoteDuringCapture=false;
             MoveFilter filter = new MoveFilterKiller(bs);
             return new MoveGenerator(longMoves, backwardsCapture, promoteDuringCapture, filter);
         }
         else {
             return org10x10.dam.game.MoveGeneratorFactory.createMoveGeneratorInternational();
         }
    }
}
