package plpFields;

public class RequiredResource {
    public enum RequirementStatus {
        Exclusive, Frequency
    }

    private String name;
    private double quantity;
    private RequirementStatus reqStatus;
    private double frequency;
    private double duration;

    public RequiredResource(String name, RequirementStatus reqStatus) {
        this.name = name;
        this.reqStatus = reqStatus;
        this.quantity = -1;
    }

    public String getName() {
        return name;
    }

    public double getQuantity() {
        return quantity;
    }

    public RequirementStatus getReqStatus() {
        return reqStatus;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getDuration() {
        return duration;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        if (reqStatus.equals(RequirementStatus.Exclusive))
            return "[" + name + "(exclusive)" + (quantity == -1 ? "" : "- quantity: "+quantity);
        else
            return "[" + name + " - frequency: " + frequency + ", duration: "
                    + duration +  (quantity == -1 ? "" : ", quantity: "+quantity);
    }
}
