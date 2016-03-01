package effects;


import plpEtc.ParamHolder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ForAllEffect implements Effect {

    private Effect effect;
    private List<String> params;

    public ForAllEffect(Effect effect) {
        this.effect = effect;
        params = new LinkedList<>();
    }

    public void addParam(String paramName) {
        this.params.add(paramName);
    }

    public Effect getEffect() {
        return effect;
    }

    public List<String> getParams() {
        return params;
    }

    public boolean sharesParams(ParamHolder ph) {
        return effect.sharesParams(ph);
    }

    public boolean containsParam(String paramName) {
        return effect.containsParam(paramName);
    }

    public String toString() {
        return "[forall " + Arrays.toString(params.toArray()) +
                "->" + effect.toString() + "]";
    }

    @Override
    public String simpleString() {
        StringBuilder sb = new StringBuilder();
        sb.append("forall");
        for (String s : params) {
            sb.append("_").append(s);
        }
        sb.append("_").append(effect.simpleString());
        return sb.toString();
    }
}
