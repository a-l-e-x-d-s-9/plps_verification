package cs.bgu.maorash.plps.plpFields;

import cs.bgu.maorash.compiler.PDDLCompiler;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class PLPParameter implements ObservationGoal {

    private String name;
    private List<String> paramFieldValues;


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

    @Override
    public boolean containsParam(PLPParameter param) {
        return this.name.equals(param.getName());
    }

    @Override
    public String toString() {
        if (paramFieldValues.isEmpty())
            return name;

        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (String s : paramFieldValues) {
            sb.append(s).append(" ");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append(")");
        return sb.toString();
    }

}
