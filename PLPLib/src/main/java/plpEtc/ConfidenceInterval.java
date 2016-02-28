package plpEtc;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ConfidenceInterval {

    private Range interval;
    private double confidence_level;

    public ConfidenceInterval(Range interval, double confidence_level) {
        this.interval = interval;
        this.confidence_level = confidence_level;
    }

    public Range getInterval() {
        return interval;
    }

    public double getConfidence_level() {
        return confidence_level;
    }

    @Override
    public String toString() {
        return "[" + interval.toString() + ", " + confidence_level + "]";
    }
}
