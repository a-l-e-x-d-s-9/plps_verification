package distributions;

import conditions.Condition;

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

    public boolean isConditional() { return condition != null; }

    public Distribution getDist() {
        return dist;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        if (condition == null) {
            return dist.toString();
        }
        else {
            return "["+dist.toString()+"|"+condition.toString()+"]";
        }
    }
}
