package nl.tue.s2id90.draughts;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org10x10.dam.game.BoardState;
import org10x10.dam.game.DamConstants;
import org10x10.dam.game.Move;
import org10x10.dam.game.MoveFilter;
import org10x10.dam.game.MoveFilterMaximumCapture;

/**
 *
 * @author huub
 */
public class MoveFilterKiller implements MoveFilter {
    BoardState bs;
    MoveFilterMaximumCapture mfMax = new MoveFilterMaximumCapture();
    public MoveFilterKiller(BoardState bs) {
        this.bs = bs;
    }

    @Override
    public List<List<Move>> filter(Collection<? extends Move> clctn) {
        // get international draughts moves: killer moves are a subset of them
        List<List<Move>> result = mfMax.filter(clctn);
        
        // filter out the killer moves
        List<Move> killerMoves=result.get(0).stream()
                .filter(m->killer(m))
                .collect(Collectors.toList());
        
        // update result; we don't bother to update the illegal moves
        // which are in result.get(1).
        result.set(0, killerMoves);
        return result;
    }
    
        
    private boolean killer(Move m) {
            if (m.isPieceMove() || !m.isCapture()) {
                // all piece moves and non-captures are killer moves
                return true;     
            } else { 
                // m is a capture move and a king move
                int c = m.getCaptureCount()-1;  // index of last captured piece
                int p = m.getCapturedPiece(c);  // last captured piece
                if (p==DamConstants.WHITEKING || p==DamConstants.BLACKKING) {
                    // last captured piece is a king
                    int e = m.getEndField();        // end field of move
                    int f = m.getCapturedField(c);  // field of last captured piece
                    // if e and f are neighbouring fields, m is a killer move.
                    return areNeighbours(e,f);
                } else {
                    // all piece captures are killer moves
                    return true;
                }
            }
    }
    
    private boolean areNeighbours(int f0, int f1) {
        int r0 =bs.f2r(f0) , c0=bs.f2c(f0);
        int r1 =bs.f2r(f1) , c1=bs.f2c(f1);
        return (r0-r1==1||r1-r0==1) && (c0-c1==1||c1-c0==1);
    }
}
