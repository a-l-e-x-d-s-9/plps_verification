package plpFields;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class PLPParameter implements ObservationGoal {

    public static String PLPParameterRegex = "\\b[^()]+\\((.*)\\)$|\\b[^()]+"; //"[_a-zA-Z]\\w*|[_a-zA-Z]\\w*\\([_a-zA-Z]\\w*[\\s[_a-zA-Z]\\w*]*\\)";

    private String name;
    private List<String> paramFieldValues;
    private double readFrequency;
    private String errorParam;

    public PLPParameter(String name) {
        this.name = name;
        paramFieldValues = new LinkedList<>();
    }

    public PLPParameter(String name, List<String> paramFieldValues) {
        this.name = name;
        this.paramFieldValues = paramFieldValues;
    }

    public String getName() {
        return name;
    }

    public List<String> getParamFieldValues() {
        return paramFieldValues;
    }

    public void addParamFieldValue(String val) { paramFieldValues.add(val); }

    public void setReadFrequency(double readFrequency) {
        this.readFrequency = readFrequency;
    }

    public void setErrorParam(String errorParam) {
        this.errorParam = errorParam;
    }

    @Override
    public boolean containsParam(String paramName) {
        if (paramName.indexOf('(') >= 0) {
            String name = paramName.substring(0, paramName.indexOf('('));
            return this.name.equals(name);
        }
        return this.name.equals(paramName);
    }

    @Override
    public String toString() {
        if (paramFieldValues.isEmpty())
            return name;

        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (String s : paramFieldValues) {
            sb.append(s).append(", ");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        sb.append(")");
        return sb.toString();
    }

    public String simpleString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        for (String s : paramFieldValues) {
            sb.append("_").append(s);
        }
        return sb.toString();
    }

    public static PLPParameter createParamFromString(String param) {
        if (!param.matches(PLPParameterRegex))
            return null;
        Pattern p = Pattern.compile("[_a-zA-Z]\\w*");
        Matcher matcher = p.matcher(param);
        boolean isFirstMatch = true;
        PLPParameter resultParam = null;
        while (matcher.find()) {
            if (isFirstMatch)
                resultParam = new PLPParameter(matcher.group());
            else
                resultParam.addParamFieldValue(matcher.group());
            isFirstMatch = false;
        }
        return resultParam;
    }

}
