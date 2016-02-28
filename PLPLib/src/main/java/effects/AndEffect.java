package effects;


import plpEtc.ParamHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class AndEffect implements Effect{

    private List<Effect> effects;

    public AndEffect() {
        effects = new LinkedList<>();
    }

    public void addEffect(Effect effect){
        effects.add(effect);
    }

    public List<Effect> getEffects() {
        return effects;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[AND [");
        for (Effect e : effects) {
            sb.append(e.toString()).append(" ");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("]");
        return sb.toString();
    }

    public boolean sharesParams(ParamHolder ph) {
        for (Effect effect : effects) {
            if (effect.sharesParams(ph))
                return true;
        }
        return false;
    }

    public boolean containsParam(String paramName) {
        for (Effect effect : effects) {
            if (effect.containsParam(paramName))
                return true;
        }
        return false;
    }
}
