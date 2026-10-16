
import java.util.*;

enum Direction {
    UP, DOWN, IDLE
}

enum Suitability {
    IDLE, SAME_DIRECTION, OTHER
}

interface ElevatorState {
    void move(Elevator e);

    Direction direction();
}

class IdleState implements ElevatorState {
    public void move(Elevator e) {
        if (!e.upQueue.isEmpty()) {
            e.setState(new UpState());
        } else if (!e.downQueue.isEmpty()) {
            e.setState(new DownState());
        }
    }

    public Direction direction() {
        return Direction.IDLE;
    }
}

class UpState implements ElevatorState {
    public void move(Elevator e) {
        if (e.target == null) {
            if (!e.upQueue.isEmpty()) {
                e.target = e.upQueue.poll();
            } else {
                e.setState(e.downQueue.isEmpty()
                        ? new IdleState()
                        : new DownState());
                return;
            }
        }

        if (e.floor < e.target) {
            e.floor++;
        } else {
            e.target = null;
        }
    }

    public Direction direction() {
        return Direction.UP;
    }
}

class DownState implements ElevatorState {
    public void move(Elevator e) {
        if (e.target == null) {
            if (!e.downQueue.isEmpty()) {
                e.target = e.downQueue.poll();
            } else {
                e.setState(e.upQueue.isEmpty()
                        ? new IdleState()
                        : new UpState());
                return;
            }
        }

        if (e.floor > e.target) {
            e.floor--;
        } else {
            e.target = null;
        }
    }

    public Direction direction() {
        return Direction.DOWN;
    }
}

interface ElevatorObserver {
    void update(Elevator e);
}

class StatusDisplay implements ElevatorObserver {
    public void update(Elevator e) {
        System.out.println(
                "Lift " + e.id +
                        " | Floor: " + e.floor +
                        " | Direction: " + e.state.direction() +
                        " | Target: " + e.target +
                        " | Up: " + e.upQueue +
                        " | Down: " + e.downQueue);
    }
}

class Elevator {
    int id;
    int floor;
    Integer target;

    PriorityQueue<Integer> upQueue = new PriorityQueue<>();
    PriorityQueue<Integer> downQueue = new PriorityQueue<>(Collections.reverseOrder());

    ElevatorState state = new IdleState();
    List<ElevatorObserver> observers = new ArrayList<>();

    Elevator(int id, int floor) {
        this.id = id;
        this.floor = floor;
    }

    void addObserver(ElevatorObserver observer) {
        observers.add(observer);
    }

    void notifyObservers() {
        for (ElevatorObserver observer : observers) {
            observer.update(this);
        }
    }

    void setState(ElevatorState state) {
        this.state = state;
        notifyObservers();
    }

    Suitability suitability(Direction requestedDirection, int requestedFloor) {
        if (state.direction() == Direction.IDLE) {
            return Suitability.IDLE;
        }

        boolean sameDirection = state.direction() == requestedDirection;

        boolean ahead = requestedDirection == Direction.UP
                ? floor <= requestedFloor
                : floor >= requestedFloor;

        if (sameDirection && ahead) {
            return Suitability.SAME_DIRECTION;
        }

        return Suitability.OTHER;
    }

    void addRequest(int requestedFloor) {
        if (requestedFloor > floor) {
            upQueue.offer(requestedFloor);
        } else if (requestedFloor < floor) {
            downQueue.offer(requestedFloor);
        } else {
            notifyObservers();
            return;
        }

        state.move(this);
        notifyObservers();
    }

    void step() {
        state.move(this);
        notifyObservers();
    }

    int distanceTo(int requestedFloor) {
        return Math.abs(floor - requestedFloor);
    }
}

class ElevatorController {
    List<Elevator> elevators = new ArrayList<>();

    ElevatorController(int count, ElevatorObserver observer) {
        for (int i = 1; i <= count; i++) {
            Elevator e = new Elevator(i, 0);
            e.addObserver(observer);
            elevators.add(e);
        }
    }

    void request(int floor, Direction direction) {
        Elevator best = Collections.min(
                elevators,
                Comparator
                        .comparing((Elevator e) -> e.suitability(direction, floor))
                        .thenComparingInt(e -> e.distanceTo(floor)));

        System.out.println(
                "\nController assigns floor " + floor +
                        " (" + direction + ") to lift " + best.id);

        best.addRequest(floor);
    }

    void stepAll() {
        for (Elevator e : elevators) {
            e.step();
        }
    }
}

public class ElevatorSystem {
    public static void main(String[] args) {
        StatusDisplay display = new StatusDisplay();
        ElevatorController controller = new ElevatorController(3, display);

        controller.request(5, Direction.UP);
        controller.request(8, Direction.UP);
        controller.request(2, Direction.DOWN);

        for (int i = 1; i <= 12; i++) {
            System.out.println("\n--- Step " + i + " ---");
            controller.stepAll();
        }
    }
}
