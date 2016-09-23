package plpFields;

import plpEtc.FieldType;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class Constant {
    private String name;
    private FieldType type;
    private String value;

    public Constant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public FieldType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(name).append(" - ").append(type);
        if (value != null)
            sb.append(", value: ").append(value);
        sb.append("]");
        return sb.toString();
    }

}
