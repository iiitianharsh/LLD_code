import java.util.*;

enum Direction {
    UP, DOWN, IDLE
}

interface ElevatorState {
    void move(Elevator elevator);

    Direction getDirection();
}

class IdleState implements ElevatorState {
    public void move(Elevator e) {
        if (!e.upQueue.isEmpty()) {
            e.setState(new UpState());
        } else if (!e.downQueue.isEmpty()) {
            e.setState(new DownState());
        }
    }

    public Direction getDirection() {
        return Direction.IDLE;
    }
}

class UpState implements ElevatorState {
    public void move(Elevator e) {
        if (e.target == null) {
            if (!e.upQueue.isEmpty()) {
                e.target = e.upQueue.poll();
            } else {
                e.setState(new DownState());
                return;
            }
        }

        if (e.floor < e.target) {
            e.floor++;
        } else {
            e.target = null;
            if (e.upQueue.isEmpty()) {
                e.setState(new IdleState());
            }
        }
    }

    public Direction getDirection() {
        return Direction.UP;
    }
}

class DownState implements ElevatorState {
    public void move(Elevator e) {
        if (e.target == null) {
            if (!e.downQueue.isEmpty()) {
                e.target = e.downQueue.poll();
            } else {
                e.setState(new IdleState());
                return;
            }
        }

        if (e.floor > e.target) {
            e.floor--;
        } else {
            e.target = null;
            if (e.downQueue.isEmpty()) {
                e.setState(new IdleState());
            }
        }
    }

    public Direction getDirection() {
        return Direction.DOWN;
    }
}

interface ElevatorObserver {
    void update(Elevator elevator);
}

class StatusDisplay implements ElevatorObserver {
    public void update(Elevator e) {
        System.out.println(
                "Lift " + e.id +
                        " | Floor: " + e.floor +
                        " | Direction: " + e.state.getDirection() +
                        " | Up queue: " + e.upQueue +
                        " | Down queue: " + e.downQueue);
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

    void addRequest(int requestedFloor, Direction direction) {
        if (direction == Direction.UP) {
            if (requestedFloor > floor) {
                upQueue.offer(requestedFloor);
            } else {
                // A request at/below this floor cannot be
                // serviced as an upward stop from here.
                downQueue.offer(requestedFloor);
            }
        } else if (direction == Direction.DOWN) {
            if (requestedFloor < floor) {
                downQueue.offer(requestedFloor);
            } else {
                upQueue.offer(requestedFloor);
            }
        }

        if (state.getDirection() == Direction.IDLE) {
            state.move(this);
        }

        notifyObservers();
    }

    void step() {
        state.move(this);
        notifyObservers();
    }

    boolean isIdle() {
        return state.getDirection() == Direction.IDLE
                && target == null
                && upQueue.isEmpty()
                && downQueue.isEmpty();
    }

    int distanceTo(int requestedFloor) {
        return Math.abs(floor - requestedFloor);
    }
}

class ElevatorController {
    List<Elevator> elevators = new ArrayList<>();

    ElevatorController(int count, ElevatorObserver observer) {
        for (int i = 0; i < count; i++) {
            Elevator e = new Elevator(i + 1, 0);
            e.addObserver(observer);
            elevators.add(e);
        }
    }

    void request(int floor, Direction direction) {
        Elevator best = null;
        int bestScore = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            int score = e.distanceTo(floor);
            Direction current = e.state.getDirection();

            boolean movingTowardRequest = current == direction &&
                    ((direction == Direction.UP && e.floor <= floor) ||
                            (direction == Direction.DOWN && e.floor >= floor));

            if (e.isIdle()) {
                score -= 1000;
            } else if (movingTowardRequest) {
                score -= 100;
            } else {
                score += 100;
            }

            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }

        System.out.println(
                "\nController assigns floor " + floor +
                        " (" + direction + ") to lift " + best.id);

        best.addRequest(floor, direction);
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
        ElevatorController controller =
            new ElevatorController(3, display);

        controller.request(5, Direction.UP);
        controller.request(8, Direction.UP);
        controller.request(2, Direction.DOWN);
s
        for (int i = 0; i < 12; i++) {
            System.out.println("\n--- Time step " + (i + 1) + " ---");
            controller.stepAll();
        }
    }
}
