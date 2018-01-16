import bc.MapLocation;

/**
 * Class that extends the Unit class and holds information specific to the two structure classes: Factories and Rockets
 */
public abstract class Structure extends UnitInstance {

    private boolean isBuilt;
    private MapLocation structureLocation;

    public Structure(int id, boolean isBuilt, MapLocation structureLocation) {
        super(id);
        this.isBuilt = isBuilt;
        this.structureLocation = structureLocation;
    }

    public boolean isBuilt() {
        return isBuilt;
    }

    public void setBuilt() {
        isBuilt = true;
    }
}