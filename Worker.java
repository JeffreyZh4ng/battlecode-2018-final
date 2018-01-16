import bc.Direction;
import bc.MapLocation;
import bc.UnitType;

public class Worker extends Robot {

    private static final int MOVEMENT_HEAT_LIMIT = 10;
    private static final int MIN_WORKER_LIST_SIZE = 4;

    public Worker(int id) {
        super(id);
    }

    @Override
    public void run() {
        if (this.getRobotTaskQueue().size() != 0) {
            manageCurrentRobotTask();
        }

        if (this.getEmergencyTask() != null) {
            if (executeTask(this.getEmergencyTask())) {
                this.setEmergencyTask(null);
            }

        } else if (this.getRobotTaskQueue().size() != 0) {
            RobotTask currentTask = this.getRobotTaskQueue().peek();

            if (executeTask(currentTask)) {
                this.getRobotTaskQueue().poll();
                GlobalTask globalTask = Earth.earthTaskMap.get(currentTask.getTaskId());
                globalTask.incrementCompletionStage();
            }
        }

        mineKarbonite();

    }

    /**
     * Method that manages what task the worker should be on and other nuances such as if it should set a
     * priority task based on different parameters
     */
    private void manageCurrentRobotTask() {
        GlobalTask globalTask = Earth.earthTaskMap.get(this.getTopTask().getTaskId());
        int completionStage = this.getTopTask().getCompletionStage();
        if (completionStage > 1 && completionStage < globalTask.getCompletionStage()) {
            for (int i = completionStage-1; i < globalTask.getCompletionStage(); i++) {
                this.removeTask();
            }

            // Need to reset the completion stage if tasks have been popped.
            completionStage = this.getTopTask().getCompletionStage();
        }

        // If the task is on the build stage and there are not more than 4 workers in the list, clone
        if (completionStage == 3 && globalTask.getWorkersOnTask().size() < MIN_WORKER_LIST_SIZE && this.getEmergencyTask() == null) {
            RobotTask emergencyTask = new RobotTask(-1, -1, Command.CLONE, globalTask.getTaskLocation());
            this.setEmergencyTask(emergencyTask);
        }
    }

    /**
     * Executes the task from the task queue
     * @param robotTask The task the robot has to complete
     * @return If the task was completed or not
     */
    private boolean executeTask(RobotTask robotTask) {
        Command robotCommand = robotTask.getCommand();
        MapLocation commandLocation = robotTask.getCommandLocation();

        switch (robotCommand) {
            case MOVE:
                if (Player.gc.unit(this.getId()).movementHeat() < MOVEMENT_HEAT_LIMIT) {
                    return move(this.getId(), commandLocation);
                } else {
                    return false;
                }

            case CLONE:
                return cloneWorker(commandLocation);
            case BUILD:
                return buildStructure(commandLocation, UnitType.Factory);
            case BLUEPRINT_FACTORY:
                return blueprintStructure(commandLocation, UnitType.Factory);
            case BLUEPRINT_ROCKET:
                return blueprintStructure(commandLocation, UnitType.Rocket);
            default:
                return true;
        }
    }

    /**
     * Given a MapLocation, see if you can clone a worker and put it at that spot
     * @param commandLocation The MapLocation of the new worker
     * @return If the worker was cloned or not
     */
    private boolean cloneWorker(MapLocation commandLocation) {
        MapLocation robotCurrentLocation = Player.gc.unit(this.getId()).location().mapLocation();

        for (int i = 1; i < 9; i++) {
            Direction direction = Direction.swigToEnum(i);
            MapLocation newLocation = commandLocation.add(direction);

            if (robotCurrentLocation.isAdjacentTo(newLocation)) {
                Direction directionToClone = robotCurrentLocation.directionTo(newLocation);
                if (Player.gc.canReplicate(this.getId(), directionToClone)) {
                    Player.gc.replicate(this.getId(), directionToClone);

                    int clonedWorkerId = Player.gc.senseUnitAtLocation(commandLocation).id();
                    UnitInstance newWorker = new Worker(clonedWorkerId);

                    Earth.earthStagingWorkerMap.put(clonedWorkerId, newWorker);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Given a MapLocation of a blueprint, build it until it reaches full health and becomes a rocket/factory
     * @param commandLocation The location of the unfinished structure
     * @param unitType The type of structure you are building
     * @return If the structure finished building
     */
    private boolean buildStructure(MapLocation commandLocation, UnitType unitType) {
        int structureId = Player.gc.senseUnitAtLocation(commandLocation).id();

        if (Player.gc.canBuild(this.getId(), structureId)) {
            Player.gc.build(this.getId(), structureId);

            if (Player.gc.unit(structureId).structureIsBuilt() > 0) {
                if (unitType == UnitType.Factory) {
                    UnitInstance factory = Earth.earthFactoryMap.get(structureId);
                    UnitInstance builtFactory = new Factory(factory.getId(), true, commandLocation);
                    Earth.earthFactoryMap.put(factory.getId(), builtFactory);
                } else {
                    UnitInstance rocket = Earth.earthFactoryMap.get(structureId);
                    UnitInstance builtRocket = new Factory(rocket.getId(), true, commandLocation);
                    Earth.earthFactoryMap.put(rocket.getId(), builtRocket);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Given the map location of a blueprint you want to build, check if you can build it and add
     * a new blueprint instance to the blueprint map
     * @param commandLocation The MapLocation of the blueprint you want to build
     * @param unitType Either a factory or rocket blueprint
     * @return If the blueprint was built or not
     */
    private boolean blueprintStructure(MapLocation commandLocation, UnitType unitType) {
        MapLocation robotCurrentLocation = Player.gc.unit(this.getId()).location().mapLocation();
        Direction directionToBlueprint = robotCurrentLocation.directionTo(commandLocation);

        if (Player.gc.canBlueprint(this.getId(), unitType, directionToBlueprint)) {
            Player.gc.blueprint(this.getId(), unitType, directionToBlueprint);

            int structureId = Player.gc.senseUnitAtLocation(commandLocation).id();
            UnitInstance newStructure;
            if (unitType == UnitType.Factory) {
                newStructure = new Factory(structureId, false, commandLocation);
                Earth.earthFactoryMap.put(structureId, newStructure);
            } else {
                newStructure = new Rocket(structureId, false, commandLocation);
                Earth.earthRocketMap.put(structureId, newStructure);
            }

            return true;
        }

        return false;
    }

    /**
     * Method that will check if a worker can mine karbonite. If it has not performed an action this turn and
     * there is a karbonite pocket in adjacent squares, it will mine it
     */
    private void mineKarbonite() {
        for (int i = 0; i < 9; i++) {
            Direction direction = Direction.swigToEnum(i);
            if (Player.gc.canHarvest(this.getId(), direction)) {
                Player.gc.harvest(this.getId(), direction);
                break;
            }
        }
    }
}
