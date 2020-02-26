package nl.tue.s2id90.group11;

import nl.tue.s2id90.group11.samples.UninformedPlayer;
import nl.tue.s2id90.group11.samples.OptimisticPlayer;
import nl.tue.s2id90.group11.samples.BuggyPlayer;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import nl.tue.s2id90.draughts.DraughtsPlayerProvider;
import nl.tue.s2id90.draughts.DraughtsPlugin;



/**
 *
 * @author huub
 */
@PluginImplementation
public class MyDraughtsPlugin extends DraughtsPlayerProvider implements DraughtsPlugin {
    public MyDraughtsPlugin() {
        // make one or more players available to the AICompetition tool
        // During the final competition you should make only your 
        // best player available. For testing it might be handy
        // to make more than one player available.
        super(
                new Group11DraughtsPlayer(90, 05, 05, 15),
                //new Group11DraughtsPlayer(80, 10, 05, 05),
                new Group11DraughtsPlayer(90, 00, 00, 00)
        );
    }
}
