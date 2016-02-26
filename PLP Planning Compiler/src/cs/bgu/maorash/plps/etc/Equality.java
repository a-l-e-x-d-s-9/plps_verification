package cs.bgu.maorash.plps.etc;

import cs.bgu.maorash.compiler.PDDLCompiler;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

/**
 * Created by maora_000 on 23-Dec-15.
 */
public class Equality implements Condition {

    private String leftExpr;
    private String rightExpr;

    public Equality(String leftExpr, String rightExpr) {
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    public String getRightExpr() {
        return rightExpr;
    }

    public PLPParameter getLeftExpr() {
        return leftExpr;
    }

    public String toString() {
        return "[" + leftExpr + " = " + rightExpr + "]";
    }

    @Override
    public boolean containsParam(PLPParameter param) {
        return leftExpr.getName().equals(param.getName()) || rightExpr.equals(param.getName());
        //TODO: change rightExpr to be constant/variable/parameter
    }

    @Override
    public boolean sharesParams(Condition c) {
        if (c.getClass().isAssignableFrom(Equality.class)) {
            return c.containsParam(this.leftExpr) || c.containsParam(new PLPParameter(this.rightExpr));
        }
        return c.sharesParams(this);

        //TODO: Fix this.
    }

    @Override
    public String toPDDL() {
        return PDDLCompiler.compile(this);
    }
}
