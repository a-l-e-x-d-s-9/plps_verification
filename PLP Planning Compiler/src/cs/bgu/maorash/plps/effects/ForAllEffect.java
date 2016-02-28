package cs.bgu.maorash.plps.effects;

import cs.bgu.maorash.plps.etc.ParamHolder;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Override
    public boolean sharesParams(ParamHolder ph) {
        return effect.sharesParams(ph);
    }

    @Override
    public boolean containsParam(String paramName) {
        return effect.containsParam(paramName);
    }

    public String toString() {
        return "[forall " + Arrays.toString(params.toArray()) +
                "->" + effect.toString() + "]";
    }
}
