package cs.bgu.maorash.plps.etc;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ConditionalProb {

    private double prob;
    private List<Condition> conditions;

    public ConditionalProb (double prob) {
        this.prob = prob;
        conditions = new LinkedList<>();
    }

    public void addCondition (Condition c) {
        this.conditions.add(c);
    }

    public double getProb() {
        return prob;
    }

    @Override
    public String toString() {
        return "[" + prob +
                " | " + Arrays.toString(conditions.toArray()) + "]";
    }
}
