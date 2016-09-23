package effects;


import plpEtc.ParamHolder;
import plpFields.PLPParameter;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class AssignmentEffect implements Effect {

    private PLPParameter param;
    private String expression;
    private String key_desc;

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

    public void setDescription(String desc) {
        this.key_desc = desc;
    }

    public String getKeyDesc() {
        return key_desc;
    }

    public boolean containsParam(String paramName) {
        if (paramName.equals(this.param.toString())){
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

    public boolean sharesParams(ParamHolder c) {
        Pattern p = Pattern.compile("[_a-zA-Z]\\w*");
        Matcher matcher;
        if (!Arrays.asList(new String[]{"TRUE","FALSE","NULL"}).contains(this.expression))
            matcher = p.matcher(this.param.toString());
        else
            matcher = p.matcher(this.expression.concat("|").concat(this.param.toString()));
        while (matcher.find()) {
            if (c.containsParam(matcher.group()))
                return true;
        }
        return false;
    }

    public String toString() {
        return "[" + param.toString() + " = " + expression + "]";
    }

    @Override
    public String simpleString() {
        return key_desc;
    }
}
