package cs.bgu.maorash.plps.plpFields;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class PLPParameter implements ObservationGoal {

    public static String PLPParameterRegex = "[_a-zA-Z]\\w*|[_a-zA-Z]\\w*\\([_a-zA-Z]\\w*[\\s[_a-zA-Z]\\w*]*\\)";

    private String name;
    private List<String> paramFieldValues;
    private double readFrequency;
    private String errorParam;

    public PLPParameter(String name) {
        this.name = name;
        paramFieldValues = new LinkedList<>();
    }

    public PLPParameter(String name, List<String> paramFieldValues) {
        this.name = name;
        this.paramFieldValues = paramFieldValues;
    }

    public String getName() {
        return name;
    }

    public List<String> getParamFieldValues() {
        return paramFieldValues;
    }

    public void addParamFieldValue(String val) { paramFieldValues.add(val); }

    public void setReadFrequency(double readFrequency) {
        this.readFrequency = readFrequency;
    }

    public void setErrorParam(String errorParam) {
        this.errorParam = errorParam;
    }

    @Override
    public boolean containsParam(String paramName) {
        return this.name.equals(paramName);
    }

    @Override
    public String toString() {
        if (paramFieldValues.isEmpty())
            return name;

        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (String s : paramFieldValues) {
            sb.append(s).append(", ");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        sb.append(")");
        return sb.toString();
    }

}
