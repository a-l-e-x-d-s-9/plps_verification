package cs.bgu.maorash.plps.modules;

import cs.bgu.maorash.plps.distributions.ConditionalDist;
import cs.bgu.maorash.plps.etc.Condition;
import cs.bgu.maorash.plps.etc.ConditionalProb;
import cs.bgu.maorash.plps.plpFields.ObservationGoal;

import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ObservePLP extends PLP {

    private ObservationGoal goal;

    private List<ConditionalProb> failureToObserveProb;
    private List<ConditionalProb> correctObservationProb;

    private List<ConditionalDist> successRuntime;
    private List<ConditionalDist> failureRuntime;

    public ObservePLP(String baseName) {
        super(baseName);
    }

    public ObservationGoal getGoal() {
        return goal;
    }

    public List<ConditionalProb> getFailureToObserveProb() {
        return failureToObserveProb;
    }

    public List<ConditionalProb> getCorrectObservationProb() {
        return correctObservationProb;
    }

    public List<ConditionalDist> getSuccessRuntime() {
        return successRuntime;
    }

    public List<ConditionalDist> getFailureRuntime() {
        return failureRuntime;
    }

    public void setGoal(ObservationGoal og) {
        this.goal = og;
    }

    public void addFailureToObserveProb(ConditionalProb cp) {
        failureToObserveProb.add(cp);
    }

    public void addCorrectObservationProb(ConditionalProb cp) {
        correctObservationProb.add(cp);
    }

    public void addSuccessRuntime(ConditionalDist cd) {
        successRuntime.add(cd);
    }

    public void addFailureRuntime(ConditionalDist cd) {
        failureRuntime.add(cd);
    }

    public String getName() {
        return "Observe '"+name+"'";
    }

    @Override
    public String toString() {
        return super.toString() + "\n" +
                " - Observation Goal: " + goal + "\n" +
                " - Failure to Observe Probability: " + "\n" +
                " - Correct Observation Probability: " + "\n" +
                " - Runtime Given Success: " + "\n" +
                " - Runtime Given Failure:";
    }
}
