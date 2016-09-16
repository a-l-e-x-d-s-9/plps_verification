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
public class MaintainPLP extends PLP {

    private Condition maintainedCondition;
    private boolean initiallyTrue;

    private List<ConditionalDist> timeUntilTrue;

    private Condition successTerminationCondition;
    private List<Condition> failureTerminationConditions;

    private List<ConditionalProb> successProb;

    private List<FailureMode> failureModes;
    private List<ConditionalProb> generalFailureProb;

    private List<ConditionalDist> successRuntime;
    private List<ConditionalDist> failRuntime;

    public MaintainPLP(String baseName) {
        super(baseName);
        this.failureTerminationConditions = new LinkedList<>();
        this.successProb = new LinkedList<>();
        this.failureModes = new LinkedList<>();
        this.generalFailureProb = new LinkedList<>();
        this.successRuntime = new LinkedList<>();
        this.failRuntime = new LinkedList<>();
        this.maintainedCondition = new Predicate("empty-condition");
        this.successTerminationCondition = new Predicate("empty-condition");
    }

    public void setMaintainedCondition(Condition maintainedCondition) {
        this.maintainedCondition = maintainedCondition;
    }

    public void setInitiallyTrue(boolean initiallyTrue) {
        this.initiallyTrue = initiallyTrue;
    }

    public void setSuccessTerminationCondition(Condition successTerminationCondition) {
        this.successTerminationCondition = successTerminationCondition;
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

    public void addFailureTerminationConditions(Condition c) {
        failureTerminationConditions.add(c);
    }

    public String getName() {
        return "Achieve '"+name+"'";
    }

    public Condition getMaintainedCondition() {
        return maintainedCondition;
    }

    public boolean isInitiallyTrue() {
        return initiallyTrue;
    }

    public Condition getSuccessTerminationCondition() {
        return successTerminationCondition;
    }

    public List<Condition> getFailureTerminationConditions() {
        return failureTerminationConditions;
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

    public List<ConditionalDist> getTimeUntilTrue() {
        return timeUntilTrue;
    }

    public void setTimeUntilTrue(List<ConditionalDist> timeUntilTrue) {
        this.timeUntilTrue = timeUntilTrue;
    }

    public boolean hasTimeUntilTrue() {
        return this.timeUntilTrue != null;
    }

    @Override
    public String toString() {
        return super.toString()  + "\n" +
                " - Maintained Condition (initially "+initiallyTrue+"): " + maintainedCondition.toString() + "\n" +
                (hasTimeUntilTrue() ? " - Runtime Until True: " + Arrays.toString(timeUntilTrue.toArray()) + "\n" : "") +
                " - Success Termination Condition: " + successTerminationCondition.toString() + "\n" +
                " - Failure Termination Conditions: " + Arrays.toString(failureTerminationConditions.toArray()) + "\n" +
                " - Success Prob: " + Arrays.toString(successProb.toArray()) + "\n" +
                " - Failure Modes: " + Arrays.toString(failureModes.toArray()) + "\n" +
                (failureModes.isEmpty() ? " - Failure Prob: " + Arrays.toString(generalFailureProb.toArray()) : "") +
                " - Runtime Given Success: " + Arrays.toString(successRuntime.toArray()) + "\n" +
                " - Runtime Given Failure: " + Arrays.toString(failRuntime.toArray()) ;
    }
}
