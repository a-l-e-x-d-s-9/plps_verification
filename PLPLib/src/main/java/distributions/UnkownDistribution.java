package distributions;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class UnkownDistribution implements Distribution {
    private String description;

    public UnkownDistribution(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "(unknown): "+description;
    }
}
