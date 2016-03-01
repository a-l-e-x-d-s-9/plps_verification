package conditions;

import effects.Effect;
import effects.ForAllEffect;
import plpEtc.ParamHolder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class QuantifiedCondition implements Condition {

    public enum Quantifier {
        EXISTS, FORALL
    }
    private List<String> params;
    private Condition condition;
    private Quantifier quantifier;

    public QuantifiedCondition(Condition c, Quantifier quantifier) {
        params = new LinkedList<>();
        this.condition = c;
        this.quantifier = quantifier;
    }

    public Quantifier getQuantifier() {
        return quantifier;
    }

    public Condition getCondition() {
        return condition;
    }

    public void addParam(String param){
        params.add(param);
    }

    public String toString() {
        return (quantifier.equals(Quantifier.FORALL) ? "[forall " : "[exists ") +
                Arrays.toString(params.toArray()) +
                "->" + condition.toString() + "]";
    }

    public String simpleString() {
        StringBuilder sb = new StringBuilder();
        sb.append(quantifier.toString().toLowerCase());
        for (String s : params) {
            sb.append("_").append(s);
        }
        sb.append("_").append(condition.simpleString());
        return sb.toString();
    }

    public List<String> getParams() {
        return params;
    }

    public boolean containsParam(String paramName) {
        return condition.containsParam(paramName);
    }

    public boolean sharesParams(ParamHolder c) {
        return condition.sharesParams(c);
    }

    public Effect createProperEffect() {
        if (quantifier.equals(Quantifier.EXISTS)) {
            throw new UnsupportedOperationException("Can't treat condition "+toString()+" as an action effect");
        }
        ForAllEffect feEffect = new ForAllEffect(condition.createProperEffect());
        for (String param : params) {
            feEffect.addParam(param);
        }
        return feEffect;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass().isInstance(obj)) {
            QuantifiedCondition qObj = (QuantifiedCondition) obj;
            if (quantifier == qObj.quantifier && condition.equals(qObj.condition)
                    && params.size() == qObj.params.size()) {
                for (int i=0; i<params.size(); i++) {
                    if (!params.get(i).equals(qObj.params.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (quantifier.toString() + "_" + condition.hashCode()).hashCode();
    }
}
