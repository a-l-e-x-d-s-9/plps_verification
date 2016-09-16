package modules;

import conditions.Condition;
import distributions.ConditionalDist;
import plpEtc.Predicate;
import plpFields.ConditionalProb;
import plpFields.FailureMode;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class AchievePLP extends PLP {

    private Condition goal;

    private List<ConditionalProb> successProb;

    private List<FailureMode> failureModes;
    private List<ConditionalProb> generalFailureProb;

    private List<ConditionalDist> successRuntime;
    private List<ConditionalDist> failRuntime;

    // If the goal includes an unobservable parameter, in order to detect success, the user can specify:
    private Condition successTerminationCond;

    // If the failure modes include an unobservable parameter, in order to detect failure, the user can specify:
    private Condition failTerminationCond;

    public AchievePLP(String baseName) {
        super(baseName);
        this.successProb = new LinkedList<>();
        this.failureModes = new LinkedList<>();
        this.generalFailureProb = new LinkedList<>();
        this.successRuntime = new LinkedList<>();
        this.failRuntime = new LinkedList<>();
        this.goal = new Predicate("empty-goal");
    }

    public void addSuccessProb(ConditionalProb prob) {
        successProb.add(prob);
    }

    public void addFailureMode(FailureMode fm) {
        failureModes.add(fm);
    }

    public void addGeneralFailureProb(ConditionalProb prob) {
        generalFailureProb.add(prob);
    }

    public void addSuccessRuntime(ConditionalDist dist) {
        successRuntime.add(dist);
    }

    public void addFailureRuntime(ConditionalDist dist) {
        failRuntime.add(dist);
    }

    public Condition getGoal() {
        return goal;
    }

    public void setGoal(Condition c) {
        this.goal = c;
    }

    public List<ConditionalProb> getSuccessProb() {
        return successProb;
    }

    public List<FailureMode> getFailureModes() {
        return failureModes;
    }

    public List<ConditionalProb> getGeneralFailureProb() {
        return generalFailureProb;
    }

    public List<ConditionalDist> getSuccessRuntime() {
        return successRuntime;
    }

    public List<ConditionalDist> getFailRuntime() {
        return failRuntime;
    }

    public String getName() {
        return "Achieve '"+name+"'";
    }

    public Condition getFailTerminationCond() {
        return failTerminationCond;
    }

    public void setFailTerminationCond(Condition failTerminationCond) {
        this.failTerminationCond = failTerminationCond;
    }

    public Condition getSuccessTerminationCond() {
        return successTerminationCond;
    }

    public void setSuccessTerminationCond(Condition successTerminationCond) {
        this.successTerminationCond = successTerminationCond;
    }

    public boolean hasSuccessTerminationCond() { return successTerminationCond != null; }

    public boolean hasFailTerminationCond() { return failTerminationCond != null; }

    @Override
    public String toString() {
        return super.toString()  + "\n" +
        " - Achievement Goal: " + goal.toString() + "\n" +
                " - Success Prob: " + Arrays.toString(successProb.toArray()) + "\n" +
                " - Failure Modes: " + Arrays.toString(failureModes.toArray()) + "\n" +
                (failureModes.isEmpty() ? " - Failure Prob: " + Arrays.toString(generalFailureProb.toArray()) + "\n" : "") +
                " - Success Runtime: " + Arrays.toString(successRuntime.toArray()) + "\n" +
                " - Failure Runtime: " + Arrays.toString(failRuntime.toArray()) +
                (successTerminationCond == null ? "" : "\n - Success Termination Condition: " + successTerminationCond.toString()) +
                (failTerminationCond == null ? "" : "\n - Fail Termination Condition: " + failTerminationCond.toString());
    }
}
