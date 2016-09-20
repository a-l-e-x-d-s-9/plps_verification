package codegen.common;

public class ParameterGlue {

    private String parameterName;
    private String rosTopic;
    private String messageType;
    private String field;
    private String fieldType;

    public ParameterGlue(String parameterName, String rosTopic, String messageType, String field, String fieldType) {
        if ((field.equals("") || fieldType.equals("")) && !field.equals(fieldType)) {
            throw new IllegalArgumentException("Trying to create parameter glue with a field in message but no field type");
        }
        this.parameterName = parameterName;
        this.rosTopic = rosTopic;
        this.messageType = messageType;
        this.field = field;
        this.fieldType = fieldType;
    }

    public String getRosTopic() {
        return rosTopic;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getField() {
        return field;
    }

    public String getFieldType() { return fieldType; }

    public String getParameterName() {
        return parameterName;
    }

    public boolean hasFieldInMessage() { return !field.equals(""); }

    @Override
    public String toString() {
        return "ParameterGlue{" +
                "parameterName='" + parameterName + '\'' +
                ", rosTopic='" + rosTopic + '\'' +
                ", messageType='" + messageType + '\'' +
                ", field='" + field + '\'' +
                ", fieldType='" + fieldType + '\'' +
                '}';
    }
}
