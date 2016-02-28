package plpFields;

import conditions.Condition;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ProgressMeasure {

    private double frequency;
    private Condition condition;

    public ProgressMeasure(double frequency, Condition condition) {
        this.frequency = frequency;
        this.condition = condition;
    }

    public double getFrequency() {
        return frequency;
    }

    public Condition getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "[" + condition.toString() + " @ " + frequency + "Hz]";
    }
}
