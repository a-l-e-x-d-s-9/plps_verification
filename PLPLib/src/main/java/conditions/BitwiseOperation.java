package conditions;

import effects.AndEffect;
import effects.Effect;
import plpEtc.ParamHolder;

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

    public BitwiseOperation(Operation op, List<Condition> conditions) {
        this.conditions = conditions;
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

    public boolean containsParam(String paramName) {
        for (Condition c : conditions) {
            if (c.containsParam(paramName))
                return true;
        }
        return false;
    }

    public boolean sharesParams(ParamHolder c) {
        for (Condition condition : conditions) {
            if (condition.sharesParams(c))
                return true;
        }
        return false;
    }

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

    @Override
    public String simpleString() {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<conditions.size(); i++) {
            sb.append(conditions.get(i).simpleString());
            if (i < conditions.size()-1)
                sb.append(operation);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass().isInstance(obj)) {
            BitwiseOperation bObj = (BitwiseOperation) obj;
            if (operation == bObj.operation && conditions.size() == bObj.conditions.size()) {
                for (int i=0; i<conditions.size(); i++) {
                    if (!conditions.get(i).equals(bObj.conditions.get(i)))
                        return false;
                }
                return true;
            }
        }
        return false;
    }
}
