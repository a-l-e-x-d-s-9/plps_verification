package cs.bgu.maorash.plps.etc;

import cs.bgu.maorash.compiler.PDDLCompiler;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Predicate implements Condition {

    private String name;
    private List<PLPParameter> values;

    public Predicate(String name) {
        this.name = name;
        this.values = new LinkedList<>();
    }

    public void addValue(PLPParameter value) {
        this.values.add(value);
    }
    public void addValue(String value) {
        this.values.add(new PLPParameter(value));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PLPParameter> getValues() {
        return values;
    }

    @Override
    public boolean containsParam(PLPParameter param) {
        for (PLPParameter p : values) {
            if (p.getName().equals(param.getName()))
                return true;
        }
        return false;

    }

    @Override
    public boolean sharesParams(Condition c) {
        if (c.getClass().isAssignableFrom(Predicate.class)) {
            return name.equals(((Predicate) c).getName());
        }
        if (c.getClass().isAssignableFrom(Equality.class)) {
            for (PLPParameter p : values) {
                if (p.getName().equals(((Equality) c).getLeftExpr().getName())
                        || p.getName().equals(((Equality) c).getRightExpr()))
                    return true;
            }
            return false;
        }
        return c.sharesParams(this);
    }

    public String toString() {
        int stringLength = Arrays.toString(values.toArray()).length();
        if (stringLength <= 2) return "(" + name + ")";
        return "(" + name + " " +Arrays.toString(values.toArray()).substring(1,stringLength-1).replaceAll(",","")+ ")";
    }

    @Override
    public String toPDDL() {
        return PDDLCompiler.compile(this);
    }
}
