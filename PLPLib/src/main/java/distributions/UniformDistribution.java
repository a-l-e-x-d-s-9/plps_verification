package distributions;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class UniformDistribution implements Distribution {
    private String lowerBound;
    private String upperBound;

    public UniformDistribution(String lowerBound, String upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public String getLowerBound() {
        return lowerBound;
    }

    public String getUpperBound() {
        return upperBound;
    }

    @Override
    public String toString() {
        return "Uniform("+lowerBound+","+upperBound+")";
    }
}
