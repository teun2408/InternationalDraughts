package nl.tue.s2id90.tournament;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.groupingBy;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.xeoh.plugins.base.Plugin;
import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.options.addpluginsfrom.OptionReportAfter;
import net.xeoh.plugins.base.util.PluginManagerUtil;
import net.xeoh.plugins.base.util.uri.ClassURI;
import nl.tue.s2id90.contest.PlayerProvider;
import nl.tue.s2id90.contest.util.SearchTask;
import nl.tue.s2id90.contest.util.TimedSearchTask;
import nl.tue.s2id90.draughts.DraughtsPlayerProvider;
import nl.tue.s2id90.draughts.DraughtsPlugin;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import nl.tue.s2id90.draughts.player.HumanPlayer;
import nl.tue.s2id90.game.GameState;
import nl.tue.s2id90.game.Player;
import org10x10.dam.game.Move;

/**
 * checks whether or not the give plugins fullfill the requirements for the
 * tournament.
 * <ol>
 * <li> have precisely one player per plugin
 * <li> have q unique package name: nl.tue.s2id90.groupNN
 * </ol>
 *
 * @author huub
 * @param <P> Player
 * @param <PP> PlayerProvider
 * @param <M> Move
 * @param <S> State
 */
public class OfflineTournament<P extends Player<M,S>, PP extends PlayerProvider<P>, M extends Move, S extends GameState<M>> {

    static AtomicInteger atomicIndex=new AtomicInteger(0); // ugly hack to have indices in a stream.
    
    Supplier<S> constructState;    // method to create initial state
    
    public static void main(String[] args) {
        // create offline tournament
        OfflineTournament<DraughtsPlayer,DraughtsPlayerProvider,Move,DraughtsState> ot = new OfflineTournament<>();
        
        // start tournament
        ot.go(args, DraughtsState::new);
        
        // end program in a normal way
        System.exit(0);
    }
    
    /** 
     * start tournament.
     */
    private void go(String[] pluginFolders,  Supplier<S> constructState) {
        this.constructState = constructState;
        
        System.err.println("plugin folders: "+Arrays.asList(pluginFolders));
        
        /** get all plugins that are botha a DraughtsPlugin and a DraughtsPlayerProvider. */
        List<PP> plugins = getPlugins(
                pluginFolders,
                p ->   (p instanceof DraughtsPlugin)
                   && (p instanceof DraughtsPlayerProvider)
        );
        
        // sort plugins on class name
        plugins.sort(Comparator.comparing(p->className(p.getName())));
        
        // collect 4 players in the plugins
        List<P> players = plugins.stream()
                .flatMap(p->p.getPlayers().stream())      // all players in the plugin Folder,
                .filter(p->!(p instanceof HumanPlayer))   // but human players
                .limit(10)
                .collect(Collectors.toList());
        
        // play tournament
        playDoubleRoundRobinTournament(
                players
                , 100                                          // max number of moves in a game
                , 200                                         // max milliseconds/move
        );
    }
    
    /**
     * Plays a double round robin tournament and prints statistics.
     * @param players          a list of players
     * @param maxMove          games have maxMove number of moves
     * @param maxTimeInMs      maximum time per move in milliseconds
     */
    public void playDoubleRoundRobinTournament(List<P> players, int maxMove, int maxTimeInMs) {
        List<Result<P,M>> results = players.stream()
            .flatMap(                                      // play all matches and pick up the results
                p0->players.stream()
                           .filter(p1->p1!=p0)             // avoid players playing against themselves
                           .map( p1 -> playMatch(p0,p1,maxMove,maxTimeInMs) )
            ).peek(result->                               // print results
                System.err.format("#%4d %10s %25s - %25s\n",
                        result.index++,
                        ""+result.getR0()+" - " + result.getR1(), 
                        result.getP0().getName(), result.getP1().getName()
                )                    
            ).collect(Collectors.toList());               // collect all results in a list
        
        // print statistics
        System.err.println("\n# stats 1 ----------------------------------------");
        statistics1(results);
        
        // print statistics
        System.err.println("\n# stats 2 ----------------------------------------");
        statistics2(results);
        
        // print statistics
        System.err.println("\n# stats 3 ----------------------------------------");
        String stats = statistics3(results);
        System.err.println(stats);
        
        // zip tournament results and stats
        try {
            toZip(results,stats);
        } catch (IOException ex) {
            Logger.getLogger(OfflineTournament.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Plays a game between the players p0 and p1.
     * @param p0   white player
     * @param p1   black player
     * @param maxMove   maximum number of allowed moves
     * @param maxTimeinMS  maximum time in milliseconds allowed per move
     * @return Result of the game, contains a.o. list of moves and result of the match.
     */
    private Result<P,M> playMatch(P p0, P p1, int maxMove, int maxTimeinMS) {
        S state = constructState.get();    // create an initial state. 
        List<M> moves = new ArrayList<>(); // start with empty move list
        int index = atomicIndex.addAndGet(1);               // increase match index
        int moveCount=0;                   // number of moves made
        while (moveCount<maxMove && !state.isEndState()) {
            P player = (state.isWhiteToMove()?p0:p1);                // current player
            M move = getComputerMove(player, state, maxTimeinMS);    // get move 
            
            // check for illegal moves
            if (move==null||!state.getMoves().contains(move)) { // illegal move
                // player who tried to make the illegal move, looses the game
                return state.isWhiteToMove()
                        ? Result.of(index, p0, p1, moves, 0, 2, false)
                        : Result.of(index, p0, p1, moves, 2, 0, false);
            }
            
            // store move and play it
            moves.add(move);
            state.doMove(move);
            
            // increase move count
            moveCount++;
        }
        
        if (state.isEndState()) { // player who is to move, looses the game
                return state.isWhiteToMove()
                        ? Result.of(index, p0, p1, moves, 0, 2, false)
                        : Result.of(index, p0, p1, moves, 2, 0, false);
        } else {                  // maxMove count reached and then game is a draw
                return  Result.of(index, p0, p1, moves, 1, 1, true);
        }
    }
    
    /** get computer move of player p in game state gs and with 
     *  maxTime milliseconds computing time.
     * 
     * @param player     player who is to Move
     * @param gs         game state
     * @param maxTime    maximum time for computing the move in milliseconds
     * @return           computed Move
     */
    private M getComputerMove(final Player player, final S gs, final int maxTime) {
        Semaphore flag = new Semaphore(0);  // semaphore that is released when move is found
        Object[] moves= new Object[1];      // create array to store search result
        SearchTask<M, Long, S> searchTask;  // Search task that computes next move in state gs
                                            // for player taking atmost maxTime milliseconds.
        searchTask = new TimedSearchTask<M, Long, S>(player, gs, maxTime) {
            // when searching is finished, store result in array moves and release semaphore.
            @Override public void done(M m) { moves[0]=m; flag.release(); }
        };
        
        // execute search task
        searchTask.execute();
        
        // wait until search task finishes
        try {
            flag.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(OfflineTournament.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // return the found move
        return (M)moves[0];
    }
    
    private String className(String name) {
        int i = name.lastIndexOf(".");
        return name.substring(i+1);
    }
    
    /** 
     * get plugins from the given folder for which the given selector returns true. 
     */
    private List<PP> getPlugins(String[] pluginFolders, Predicate<Plugin> selector) {
        PluginManager pm = PluginManagerFactory.createPluginManager();
        pm.addPluginsFrom(ClassURI.CLASSPATH, new OptionReportAfter());
        
        // add all plugin folders to the plugin manager pm
        Arrays.asList(pluginFolders).stream()
            .forEach(folder ->
                    pm.addPluginsFrom(new File(folder).toURI(), new OptionReportAfter())
            );
        
        // create a list of all found plugins for which the predicate selector holds.
        PluginManagerUtil pmu = new PluginManagerUtil(pm);
        return pmu.getPlugins(Plugin.class).stream()
                  .filter(selector)
                  .map(p -> (PP) p)
                  .collect(Collectors.toList());
    }

    
        /** prints unsorted statistics like:
     * <pre>
         YoptaPlayer	    6
  DraughtsPlayerGr02	    3
            Sate9000	    8
  BogdanRazvanPlayer	    7
     * </pre>
     * Per player prints the number of points.
     * @param results  list of tournament results.
     */
    private void statistics1(List<Result<P,M>> results) {
        Map<P, List<Result<P,M>>> whiteResults = results.stream().collect(groupingBy(Result::getP0));
        Map<P, List<Result<P,M>>> blackResults = results.stream().collect(groupingBy(Result::getP1));
        Map<P, Integer> whiteResult = results.stream().collect(
                groupingBy(
                        Result::getP0,
                        Collectors.summingInt(Result::getR0)
                )
        );
        Map<P, Integer> blackResult = results.stream().collect(
                groupingBy(
                        Result::getP1,
                        Collectors.summingInt(Result::getR1)
                )
        );
        
        Map<P, Integer> scores = Stream.concat(whiteResult.entrySet().stream(),
                blackResult.entrySet().stream()
        ).collect(
                groupingBy(
                        Entry::getKey,
                        Collectors.summingInt(Entry::getValue)
                )
        );
        
        scores.forEach((k,v)-> System.err.format("#%20s\t%5d\n",k.getName(),v.intValue()));
    }
   
    
    /** prints unsorted statistics like:
     * <pre>
         YoptaPlayer	  6   1   4   1   6
  DraughtsPlayerGr02	  6   0   3   3   3
            Sate9000	  6   2   4   0   8
  BogdanRazvanPlayer	  6   1   5   0   7
     * <pre>
     * Per player prints the number of matches,wins, draws,
     * losses, and points.
     * @param results  list of tournament results.
     */
    private void statistics2(List<Result<P,M>> results) {
        Map<P, Row> whiteResult = results.stream().collect(
                groupingBy(
                        Result::getP0,
                        collectorResultRow((row,result)->row.addWhite(result))
                )
        );
        
        Map<P, Row> blackResult = results.stream().collect(
                groupingBy(
                        Result::getP1,
                        collectorResultRow((row,result)->row.addBlack(result))
                )
        );
        
        Stream.concat(
                whiteResult.entrySet().stream(),
                blackResult.entrySet().stream()
        ).collect(groupingBy(Entry<P,Row>::getKey, collectorEntryRow())
        ).entrySet().forEach(e-> 
                System.err.format(
                    "#%20s\t%s\n",e.getKey().getName(),e.getValue().toString()
                )
        );
    }
    
    /** Combines sorted statistics like the following in a String.
     * <pre>
  DraughtsPlayerGr02	   0   1   1   1   0   0	  6   0   3   3   3
         YoptaPlayer	   0   1   1   2   1   1	  6   1   4   1   6
  BogdanRazvanPlayer	   1   1   1   1   2   1	  6   1   5   0   7
            Sate9000	   2   2   1   1   1   1	  6   2   4   0   8
     * </pre>
     * Per player combines the results of the matches he played, followed by
     * the number of matches,wins, draws, losses, and points.
     * @param results  list of tournament results.
     * @return string containing the statistics.
     */
    private String statistics3(List<Result<P,M>> results) {
        Map<P, Row> whiteResult = results.stream().collect(
                groupingBy(
                        Result::getP0,
                        collectorResultRow((row,result)->row.addWhite(result))
                )
        );
        
        Map<P, Row> blackResult = results.stream().collect(
                groupingBy(
                        Result::getP1,
                        collectorResultRow((row,result)->row.addBlack(result))
                )
        );
        
        return Stream.concat(
                whiteResult.entrySet().stream(),
                blackResult.entrySet().stream()
        ).collect(groupingBy(Entry<P,Row>::getKey, collectorEntryRow())
        ).entrySet().stream().sorted((Entry<P, Row> e0, Entry<P, Row> e1) -> 
                Integer.compare(e0.getValue().noPoints, e1.getValue().noPoints)
        ).map(e-> {
                String scores=results.stream().filter(r->r.getP0()==e.getKey()||r.getP1()==e.getKey())
                       .map(r-> String.format("%4s", (r.getP0()==e.getKey()?r.getR0():r.getR1())))
                        .collect(Collectors.joining());
                return String.format(
                    "%20s\t%s\t%s",e.getKey().getName(),scores,e.getValue().toString()
                );
        }).collect(Collectors.joining("\n#","#",""));
    }
    
    private static Collector<Result, Row, Row> collectorResultRow(BiConsumer<Row,Result> accumulator) {
        return new Collector<Result, Row, Row> (){
            @Override
            public Supplier<Row> supplier() {
                return () -> new Row();
            }

            @Override
            public BiConsumer<Row, Result> accumulator() {
                return accumulator;
            }

            @Override
            public BinaryOperator<Row> combiner() {
                return (r0,r1) -> { r0.add(r1); return r0; };
            }

            @Override
            public Function<Row, Row> finisher() {
                return r->r;
            }

            @Override
            public Set<Collector.Characteristics> characteristics() {
                return new HashSet(Arrays.asList(Collector.Characteristics.IDENTITY_FINISH));
            }
        };
    }
    
    private  Collector<Entry<P, Row>, Row, Row> collectorEntryRow() {
        return new Collector<Entry<P,Row>,Row,Row>() {
            @Override
            public Supplier<Row> supplier() {
                return Row::new;
            }
            
            @Override
            public BiConsumer<Row, Entry<P,Row>> accumulator() {
                return (row,entry) -> row.add(entry.getValue());
            }
            
            @Override
            public BinaryOperator<Row> combiner() {
                return (r0,r1)-> { r0.add(r1); return r0; };
            }
            
            @Override
            public Function<Row, Row> finisher() {
                return r->r;
            }
            
            @Override
            public Set<Collector.Characteristics> characteristics() {
                return new HashSet(Arrays.asList(Collector.Characteristics.IDENTITY_FINISH));
            }
        };
    }

    
    //<editor-fold defaultstate="collapsed" desc="zip">
    /** zips results and stats in file "roundrobin.zip". */
    private void toZip(List<Result<P, M>> results, String stats) throws FileNotFoundException, IOException {
        File file = new File("roundrobin.zip");
        OutputStream os = new FileOutputStream(file);
        try (ZipOutputStream out = new ZipOutputStream(os)) {
            for(Result<P,M> result : results) {
                String fileName = format("%s - %s.pdn",result.getP0().getName(),result.getP1().getName());
                entry(out, "pdns/"+fileName, resultToPDN(result));
            }
            entry(out,"statistics.txt",stats);
        }
        //results.stream().map(result -> resultToPDN(result)).forEach(s->System.err.println(s));
    }
    
    private ZipEntry entry(ZipOutputStream out, String file, String content) throws IOException {
        ZipEntry e = new ZipEntry(file);
        out.putNextEntry(e);
        out.write(content.getBytes());
        out.closeEntry();
        return e;
    }
    
    private String resultToPDN(Result<P, M> result) {
        StringBuilder b = new StringBuilder();
        tag(b, "Site", "Eindhoven, The Netherlands");
        tag(b,"Event","2ID90 Round Robin Tournament");
        tag(b, "date", Calendar.getInstance().getTime().toString());
        tag(b,"White", result.getP0().getName());
        tag(b,"Black", result.getP1().getName());
        tag(b,"Result", String.format("%d-%d",result.getR0(),result.getR1()));
        tag(b,"GameType", "20"); // International draughts
        
        List<M> moves = result.getMoves();
        for(int i=0; i < moves.size();i=i+1) {
            M move = moves.get(i);
            if (move==null) { add(b, "null"); break; }
            if (i%2==0) { // white move
                add(b,String.format("%2d.%s ",1+i/2,move.getNotation()));
            } else {
                add(b,String.format("%s ", move.getNotation()));
            }
            if (i%10==9) add(b,"\n");
        }
        
        add(b," *");
        return b.toString();
    }
    
    private void add(StringBuilder b, String s) {
        b.append(s);
    }
    private void tag(StringBuilder b, String tag, String value) {
        b.append(String.format("[%s \"%s\"]\n",tag,value));
    }
    
    Date today() {
        Calendar today = Calendar.getInstance();
        return today.getTime();
    }
    //</editor-fold>
}

//<editor-fold defaultstate="collapsed" desc="Row class">
class Row {
    int noGames=0;
    int noWins=0;
    int noDraws=0;
    int noLosses=0;
    int noPoints=0;
    
    public void addWhite(Result r) {
        int r0 = r.getR0();
        add(1,
                r0==2?1:0,
                r0==1?1:0,
                r0==0?1:0,
                r0);
    }
    
    public void addBlack(Result r) {
        int r1 = r.getR1();
        add(1,                    // noGames
                r1==2?1:0,        // noWins
                r1==1?1:0,        // noDraws
                r1==0?1:0,        // noLosses
                r1);              // noPoints
    }
    
    public void add(Row row) {
        add(row.noGames,row.noWins,row.noDraws,row.noLosses,row.noPoints);
    }
    
    private void add(int g, int w, int d, int l, int p) {
        noGames += g; noWins += w; noDraws += d; noLosses += l; noPoints += p;
    }
    
    public String toString() {
        return String.format("%3d %3d %3d %3d %3d", noGames, noWins, noDraws, noLosses, noPoints);
    }
}
//</editor-fold>

