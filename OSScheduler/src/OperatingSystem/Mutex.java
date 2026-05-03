package OperatingSystem;
import java.util.Queue;
import java.util.LinkedList;

public class Mutex {
    public boolean available;
    Queue<Process> blockedQueue;
    public Mutex() {
        this.available = true;
        this.blockedQueue = new LinkedList<>();
    }
}
