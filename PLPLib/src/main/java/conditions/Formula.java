package conditions;

import effects.AssignmentEffect;
import effects.Effect;
import plpEtc.ParamHolder;
import plpEtc.Range;
import plpFields.PLPParameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maora_000 on 23-Dec-15.
 */
public class Formula implements Condition {

    private String operator;
    private String leftExpr;
    private String rightExpr;
    private String key_desc;
    private Range inRange;

    public Formula(String leftExpr, String rightExpr, String operator) {
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
        this.operator = operator;
    }

    public Formula(String leftExpr, Range inRange) {
        this.leftExpr = leftExpr;
        this.inRange = inRange;
    }

    public void setDescription(String desc) {
        this.key_desc = desc;
    }

    public String getKeyDesc() {
        return key_desc;
    }

    public Range getRange() {
        return inRange;
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
        if (this.inRange == null) {
            return "[" + leftExpr + " " + operator + " " + rightExpr + "]";
        }
        else
            return "[" + leftExpr + " in " + inRange.toString() + "]";
    }

    public boolean containsParam(String paramName) {
        Pattern p = Pattern.compile("[a-zA-Z]\\w*|[_]\\w+");
        Matcher matcher;
        if (this.rightExpr != null)
            matcher = p.matcher(this.leftExpr.concat("|").concat(this.rightExpr));
        else
            matcher = p.matcher(this.leftExpr.concat("|").concat(this.inRange.getMinValue())
                                        .concat("|").concat(this.inRange.getMaxValue()));
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
        Matcher matcher;
        if (this.rightExpr != null && !Arrays.asList(new String[]{"TRUE","FALSE","NULL"}).contains(this.rightExpr))
            matcher = p.matcher(this.leftExpr.concat("|").concat(this.rightExpr));
        else if (this.inRange != null)
            matcher = p.matcher(this.leftExpr.concat("|").concat(this.inRange.getMinValue())
                    .concat("|").concat(this.inRange.getMaxValue()));
        else
            matcher = p.matcher(this.leftExpr);
        while (matcher.find()) {
            if (c.containsParam(matcher.group()))
                return true;
        }
        return false;
        //TODO: Fix this.
    }

    public Effect createProperEffect() {
        if (!this.leftExpr.matches(PLPParameter.PLPParameterRegex)
                || this.rightExpr == null)
            throw new UnsupportedOperationException("Can't treat condition "+toString()+" as an action effect, " +
                    "the left expression needs to be a parameter");

        return new AssignmentEffect(PLPParameter.createParamFromString(this.leftExpr),this.rightExpr);
    }

    @Override
    public String simpleString() {
        return key_desc;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass().isInstance(obj)) {
            Formula fobj = (Formula) obj;
            if (inRange == null && fobj.inRange == null) {
                if (operator.equals(fobj.operator)
                        && leftExpr.replaceAll("\\s+","").equalsIgnoreCase(fobj.leftExpr.replaceAll("\\s+",""))
                        && rightExpr.replaceAll("\\s+","").equalsIgnoreCase(fobj.rightExpr.replaceAll("\\s+","")))
                {
                    return true;
                }
            }
            if (inRange != null && fobj.inRange != null) {
                if (leftExpr.replaceAll("\\s+","").equalsIgnoreCase(fobj.leftExpr.replaceAll("\\s+",""))
                    && inRange.equals(fobj.inRange))
                    return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return "formula".concat(leftExpr.replaceAll("\\s+","")).hashCode();
    }
}
/*    private String operator;
    private String leftExpr;
    private String rightExpr;
    private String key_desc;
    private Range inRange;*/
