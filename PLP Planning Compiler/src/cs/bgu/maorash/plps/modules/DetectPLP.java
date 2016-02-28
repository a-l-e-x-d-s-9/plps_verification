package cs.bgu.maorash.plps.modules;

import cs.bgu.maorash.plps.conditions.Condition;
import cs.bgu.maorash.plps.etc.Predicate;
import cs.bgu.maorash.plps.plpFields.ConditionalProb;

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

    public String toString() {
        return super.toString() + "\n" +
                " - Detection Goal: " + goal.toString() + "\n" +
                " - Success Prob Given Condition: " + Arrays.toString(successProbGivenCondition.toArray());
    }
}
