package plpEtc;

import conditions.Condition;
import effects.Effect;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Predicate implements Condition, Effect {

    private String name;
    private List<String> values;

    public Predicate(String name) {
        this.name = name;
        this.values = new LinkedList<String>();
    }

    public void addValue(String value) {
        this.values.add(value);
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean containsParam(String paramName) {
        for (String val : values) {
            if (val.equals(paramName))
                return true;
        }
        return false;
    }

    public boolean sharesParams(ParamHolder ph) {
        if (ph.getClass().isAssignableFrom(Predicate.class)) {
            return name.equals(((Predicate) ph).getName());
        }
        for (String val : values) {
            if (ph.containsParam(val))
                return true;
        }
        return false;
        /*if (c.getClass().isAssignableFrom(Formula.class)) {
            for (PLPParameter p : values) {
                if (p.getName().equals(((Formula) c).getLeftExpr().getName())
                        || p.getName().equals(((Formula) c).getRightExpr()))
                    return true;
            }
            return false;
        }
        return c.sharesParams(this);*/
    }

    public String toString() {
        int stringLength = Arrays.toString(values.toArray()).length();
        if (stringLength <= 2) return "(" + name + ")";
        return "(" + name + " " +Arrays.toString(values.toArray()).substring(1,stringLength-1).replaceAll(",","")+ ")";
    }

    public String simpleString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        for (String s : values) {
            sb.append("_").append(s);
        }
        return sb.toString();
    }

    public Effect createProperEffect() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass().isInstance(obj)) {
            Predicate pobj = (Predicate) obj;
            if (name.equals(pobj.name) && values.size() == pobj.values.size()) {
                for (int i=0;i<values.size();i++)
                    if (!values.get(i).equals(pobj.values.get(i)))
                        return false;
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return "predicate".concat(name).hashCode();
    }
}
