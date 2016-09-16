package modules;

import conditions.Condition;
import plpEtc.Predicate;
import plpFields.ConditionalProb;
import plpFields.PLPParameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class DetectPLP extends PLP {

    private Condition goal;
    private List<ConditionalProb> successProbGivenCondition;

    private String resultParameter;

    private Condition failTerminationCond;

    public DetectPLP(String baseName) {
        super(baseName);
        this.successProbGivenCondition = new LinkedList<>();
        this.goal = new Predicate("empty-goal");
    }

    public Condition getGoal() {
        return goal;
    }

    public List<ConditionalProb> getSuccessProbGivenCondition() {
        return successProbGivenCondition;
    }

    public void setGoal(Condition goal) {
        this.goal = goal;
    }

    public void addSuccessProbGivenCond(ConditionalProb prob) {
        successProbGivenCondition.add(prob);
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

    public String toString() {
        return super.toString() + "\n" +
                " - Detection Goal: " + goal.toString() + "\n" +
                " - Success Prob Given Condition: " + Arrays.toString(successProbGivenCondition.toArray());
    }
}
