package old_codeGen;

public class ParameterGlue {

    private String rosTopic;
    private String messageType;
    private String field;

    public ParameterGlue(String rosTopic, String messageType, String field) {
        this.rosTopic = rosTopic;
        this.messageType = messageType;
        this.field = field;
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
}
