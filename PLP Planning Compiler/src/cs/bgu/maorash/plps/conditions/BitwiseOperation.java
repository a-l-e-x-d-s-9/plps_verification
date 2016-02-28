package cs.bgu.maorash.plps.conditions;

import cs.bgu.maorash.plps.effects.AndEffect;
import cs.bgu.maorash.plps.effects.Effect;
import cs.bgu.maorash.plps.etc.ParamHolder;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class BitwiseOperation implements Condition {

    public enum Operation {
        AND, OR
    }
    private List<Condition> conditions;
    private Operation operation;

    public BitwiseOperation(Operation op) {
        this.conditions = new LinkedList<>();
        this.operation = op;
    }

    public void addCondition(Condition c) {
        conditions.add(c);
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public Operation getOperation() {
        return operation;
    }

    @Override
    public boolean containsParam(String paramName) {
        for (Condition c : conditions) {
            if (c.containsParam(paramName))
                return true;
        }
        return false;
    }

    @Override
    public boolean sharesParams(ParamHolder c) {
        for (Condition condition : conditions) {
            if (condition.sharesParams(c))
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(operation).append(" ");
        for (Condition c : conditions) {
            sb.append(c.toString()).append(" ");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Effect createProperEffect() {
        if (operation.equals(Operation.OR)) {
            throw new UnsupportedOperationException("Can't treat condition "+toString()+" as an action effect");
        }
        AndEffect andEffect = new AndEffect();
        for (Condition c : conditions) {
            andEffect.addEffect(c.createProperEffect());
        }
        return andEffect;
    }
}
