package plpFields;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ModuleRestriction {
    public enum ConcurrencyType {
        Mutex, Parallel
    }

    private String moduleName;
    private ConcurrencyType type;

    public ModuleRestriction(String moduleName, ConcurrencyType type) {
        this.moduleName = moduleName;
        this.type = type;
    }

    public String getModuleName() {
        return moduleName;
    }

    public ConcurrencyType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "[" + moduleName + " - " + type.toString() + "]";
    }
}
