package modules;

import conditions.Condition;
import distributions.ConditionalDist;
import plpEtc.ConfidenceInterval;
import plpEtc.Predicate;
import plpFields.ConditionalProb;
import plpFields.ObservationGoal;
import plpFields.PLPParameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ObservePLP extends PLP {

    private ObservationGoal goal;

    // If the observation goal is a condition we need a result parameter
    private String resultParameter;

    private List<ConditionalProb> failureToObserveProb;
    private List<ConditionalProb> correctObservationProb;
    private ConfidenceInterval correctObservationConfidence;

    private List<ConditionalDist> successRuntime;
    private List<ConditionalDist> failureRuntime;

    private Condition failTerminationCond;

    public ObservePLP(String baseName) {
        super(baseName);
        failureToObserveProb = new LinkedList<>();
        correctObservationProb = new LinkedList<>();
        successRuntime = new LinkedList<>();
        failureRuntime = new LinkedList<>();
        this.goal = new Predicate("empty-goal");
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

    public ConfidenceInterval getCorrectObservationConfidence() {
        return correctObservationConfidence;
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

    public void setCorrectObservationConfidence(ConfidenceInterval correctObservationConfidence) {
        this.correctObservationConfidence = correctObservationConfidence;
    }

    public void addSuccessRuntime(ConditionalDist cd) {
        successRuntime.add(cd);
    }

    public void addFailureRuntime(ConditionalDist cd) {
        failureRuntime.add(cd);
    }

    public boolean isGoalParameter() { return goal.getClass().isAssignableFrom(PLPParameter.class); }

    public String getName() {
        return "Observe '"+name+"'";
    }

    public void setResultParameterName(String name) {
        this.resultParameter = name;
    }

    public PLPParameter getResultParameter() {
        for (PLPParameter outputParam : getOutputParams()) {
            if (outputParam.simpleString().equals(this.resultParameter))
                return outputParam;
        }
        return null;
    }

    public Condition getFailTerminationCond() {
        return failTerminationCond;
    }

    public void setFailTerminationCond(Condition failTerminationCond) {
        this.failTerminationCond = failTerminationCond;
    }

    public boolean hasFailTerminationCond() { return failTerminationCond != null; }

    @Override
    public String toString() {
        return super.toString() + "\n" +
                " - Observation Goal: " + goal.toString() + "\n" +
                " - Failure to Observe Probability: " + Arrays.toString(failureToObserveProb.toArray()) + "\n" +
                (correctObservationConfidence == null ?
                        " - Correct Observation Probability: "
                                + Arrays.toString(correctObservationProb.toArray()) + "\n" :
                        " - Correct Observation Confidence Interval: "
                                + correctObservationConfidence.toString() + "\n") +
                " - Runtime Given Success: " + Arrays.toString(successRuntime.toArray()) + "\n" +
                " - Runtime Given Failure: " + Arrays.toString(failureRuntime.toArray());
    }
}
