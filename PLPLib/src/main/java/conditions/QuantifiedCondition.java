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

}
