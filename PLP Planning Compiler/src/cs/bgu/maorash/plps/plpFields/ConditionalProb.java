package cs.bgu.maorash.plps.plpFields;

import cs.bgu.maorash.plps.conditions.Condition;
import cs.bgu.maorash.plps.etc.Probability;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ConditionalProb implements Probability {

    private String prob;
    private Condition condition;

    public ConditionalProb(String prob, Condition condition) {
        this.prob = prob;
        this.condition = condition;
    }

    public String getProb() {
        return prob;
    }

    public Condition getCondition() {
        return condition;
    }

    public boolean isConditional() { return condition == null; }

    @Override
    public String toString() {
        if (condition == null) {
            return prob;
        }
        else {
            return "["+prob+"|"+condition.toString()+"]";
        }
    }
}
