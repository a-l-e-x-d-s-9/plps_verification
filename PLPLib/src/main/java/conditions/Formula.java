package conditions;

import effects.AssignmentEffect;
import effects.Effect;
import plpEtc.ParamHolder;
import plpFields.PLPParameter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maora_000 on 23-Dec-15.
 */
public class Formula implements Condition {

    private String operator;
    private String leftExpr;
    private String rightExpr;

    public Formula(String leftExpr, String rightExpr, String operator) {
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public String getRightExpr() {
        return rightExpr;
    }

    public String getLeftExpr() {
        return leftExpr;
    }

    public String toString() {
        return "[" + leftExpr + " " + operator + " " + rightExpr + "]";
    }

    public boolean containsParam(String paramName) {
        Pattern p = Pattern.compile("[a-zA-Z]\\w*|[_]\\w+");
        Matcher matcher = p.matcher(this.leftExpr.concat("|").concat(this.rightExpr));
        while (matcher.find()) {
            if (paramName.equals(matcher.group()))
                return true;
        }
        return false;

        //return leftExpr.getName().equals(param.getName()) || rightExpr.equals(param.getName());
        //??TODO: change rightExpr to be constant/variable/parameter
    }

    public boolean sharesParams(ParamHolder c) {
        Pattern p = Pattern.compile("[_a-zA-Z]\\w*");
        Matcher matcher = p.matcher(this.leftExpr.concat("|").concat(this.rightExpr));
        while (matcher.find()) {
            if (c.containsParam(matcher.group()))
                return true;
        }
        return false;
        //TODO: Fix this.
    }

    public Effect createProperEffect() {
        if (!this.leftExpr.matches(PLPParameter.PLPParameterRegex))
            throw new UnsupportedOperationException("Can't treat condition "+toString()+" as an action effect, " +
                    "the left expression needs to be a parameter");
        return new AssignmentEffect(new PLPParameter(this.leftExpr),this.rightExpr);
    }
}
