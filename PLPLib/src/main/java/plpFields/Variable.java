package plpFields;

import plpEtc.FieldType;
import plpEtc.Range;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class Variable {

    private String name;
    List<Range> possibleRanges;
    List<String> possibleValues;
    private FieldType type;

    public Variable(String name, FieldType type) {
        this.name = name;
        this.type = type;
        possibleRanges = new LinkedList<>();
        possibleValues = new LinkedList<>();
    }

    public void addRange(String minValue, boolean minInclusive, String maxValue, boolean maxInclusive) {
        possibleRanges.add(new Range(minValue,minInclusive,maxValue,maxInclusive));
    }

    public void addRange(Range range) {
        possibleRanges.add(range);
    }

    public void addPossibleValue(String value) {
        possibleValues.add(value);
    }

    public String getName() {
        return name;
    }

    public List<Range> getPossibleRanges() {
        return possibleRanges;
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }

    public FieldType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(name).append(" - ").append(type.toString());
        if (possibleRanges.size() > 0 || possibleValues.size() > 0)
            sb.append(", in ");
        if (possibleRanges.size() > 0) {
            for (Range range : possibleRanges) {
                sb.append(range.toString()).append(" ");
            }
            sb.deleteCharAt(sb.length()-1);
        }
        if (possibleValues.size() > 0) {
            for (String value : possibleValues) {
                sb.append("[").append(value).append(", ");
            }
            sb.delete(sb.length()-2,sb.length());
        }
        sb.append("]");
        return sb.toString();
    }
}
