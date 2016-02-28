package plpFields;


import conditions.Condition;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class FailureMode {

    private Condition condition;
    private List<ConditionalProb> probList;

    public FailureMode(Condition condition) {
        this.condition = condition;
        probList = new LinkedList<>();
    }

    public void addProb(ConditionalProb prob) {
        probList.add(prob);
    }

    public Condition getCondition() {
        return condition;
    }

    public List<ConditionalProb> getProbList() {
        return probList;
    }

    @Override
    public String toString() {
        StringBuilder probSB = new StringBuilder();
        for (ConditionalProb prob : probList) {
            probSB.append(prob.toString()).append(" ");
        }
        probSB.deleteCharAt(probSB.length()-1);
        return "[Fail condition - " + condition.toString() + " probability - " + probSB.toString() + "]";
    }
}
