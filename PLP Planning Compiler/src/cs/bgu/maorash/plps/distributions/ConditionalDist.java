package cs.bgu.maorash.plps.distributions;

import cs.bgu.maorash.plps.etc.Condition;

import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ConditionalDist {

    private Distribution dist;
    private Condition condition;

    public ConditionalDist() {
    }

    public ConditionalDist(Distribution dist, Condition condition) {
        this.dist = dist;
        this.condition = condition;
    }

    public Distribution getDist() {
        return dist;
    }

    public void setDist(Distribution dist) {
        this.dist = dist;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}
