package cs.bgu.maorash.plps.modules;

import cs.bgu.maorash.plps.distributions.ConditionalDist;
import cs.bgu.maorash.plps.etc.Condition;
import cs.bgu.maorash.plps.etc.ConditionalProb;
import cs.bgu.maorash.plps.plpFields.ObservationGoal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class AchievePLP extends PLP {

    private List<Condition> goals;
    // probabilities

    private List<ConditionalProb> successProb;
    private HashMap<String,List<ConditionalProb>> failuresProb;

    private List<ConditionalDist> successRuntime;
    private HashMap<String,List<ConditionalDist>> failuresRuntime;

    public AchievePLP(String baseName) {
        super(baseName);

        goals = new LinkedList<>();

        this.successProb = new LinkedList<>();
        this.failuresProb = new HashMap<>();
        this.successRuntime = new LinkedList<>();
        this.failuresRuntime = new HashMap<>();

    }

    public List<Condition> getGoals() {
        return goals;
    }

    public List<ConditionalProb> getSuccessProb() {
        return successProb;
    }

    public HashMap<String, List<ConditionalProb>> getFailuresProb() {
        return failuresProb;
    }

    public List<ConditionalDist> getSuccessRuntime() {
        return successRuntime;
    }

    public HashMap<String, List<ConditionalDist>> getFailuresRuntime() {
        return failuresRuntime;
    }

    public void addGoal(Condition c) {
        goals.add(c);
    }

    public void addSuccessProb(ConditionalProb cp) {
        successProb.add(cp);
    }

    public void addFailuresProb(String c, ConditionalProb cp) {
        if (!failuresProb.containsKey(c)){
            failuresProb.put(c,new LinkedList<>());
        }
        failuresProb.get(c).add(cp);
    }

    public void addSuccessRuntime(ConditionalDist cd){
        successRuntime.add(cd);
    }

    public void addFailuresRuntime(String c,ConditionalDist cd) {
        if (!failuresRuntime.containsKey(c)){
            failuresRuntime.put(c,new LinkedList<>());
        }
        failuresRuntime.get(c).add(cd);
    }

    public String getName() {
        return "Achieve '"+name+"'";
    }

    @Override
    public String toString() {
        return super.toString()  + "\n" +
        " - Achievement Goals: " + Arrays.toString(goals.toArray()) + "\n" +
                " - successProb: " + "\n" +
                " - failuresProb: " + "\n" +
                " - successRuntime: " + "\n" +
                " - failuresRuntime: " ;
    }

}
