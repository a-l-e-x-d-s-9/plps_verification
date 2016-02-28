package distributions;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class NormalDistribution implements Distribution {
    private String mean;
    private String variance;

    public NormalDistribution(String mean, String variance) {
        this.mean = mean;
        this.variance = variance;
    }

    public String getMean() {
        return mean;
    }

    public String getVariance() {
        return variance;
    }

    @Override
    public String toString() {
        return "Normal("+mean+","+variance+")";
    }
}
