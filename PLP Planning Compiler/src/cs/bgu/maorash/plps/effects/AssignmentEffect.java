package cs.bgu.maorash.plps.effects;

import cs.bgu.maorash.plps.etc.ParamHolder;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class AssignmentEffect implements Effect {

    private PLPParameter param;
    private String expression;

    public AssignmentEffect(PLPParameter param, String expression) {
        this.param = param;
        this.expression = expression;
    }

    public PLPParameter getParam() {
        return param;
    }

    public String getExpression() {
        return expression;
    }


    @Override
    public boolean containsParam(String paramName) {
        if (paramName.equals(this.param)){
            return true;
        }
        Pattern p = Pattern.compile("[a-zA-Z]\\w*|[_]\\w+");
        Matcher matcher = p.matcher(this.expression);
        while (matcher.find()) {
            if (paramName.equals(matcher.group()))
                return true;
        }
        return false;
    }

    @Override
    public boolean sharesParams(ParamHolder c) {
        Pattern p = Pattern.compile("[_a-zA-Z]\\w*");
        Matcher matcher = p.matcher(this.expression.concat("|").concat(this.param.toString()));
        while (matcher.find()) {
            if (c.containsParam(matcher.group()))
                return true;
        }
        return false;
    }

    public String toString() {
        return "[" + param.toString() + " = " + expression + "]";
    }

}
