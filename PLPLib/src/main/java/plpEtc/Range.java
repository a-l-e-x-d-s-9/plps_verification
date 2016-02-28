package plpEtc;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class Range {
    private String minValue;
    private String maxValue;
    private boolean minInclusive;
    private boolean maxInclusive;

    public Range(String minValue, boolean minInclusive, String maxValue, boolean maxInclusive) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.maxInclusive = maxInclusive;
        this.minInclusive = minInclusive;
    }

    public String getMinValue() {
        return minValue;
    }

    public String getMaxValue() {
        return maxValue;
    }

    public boolean isMinInclusive() {
        return minInclusive;
    }

    public boolean isMaxInclusive() {
        return maxInclusive;
    }

    @Override
    public String toString() {
        return (minInclusive ? "[" : "(") + minValue + ", " +
                maxValue + (maxInclusive ? "]" : ")");
    }
}
