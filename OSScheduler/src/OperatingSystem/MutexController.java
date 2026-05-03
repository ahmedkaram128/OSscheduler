package OperatingSystem;

public class MutexController {
    public Mutex userInput;
    public Mutex userOutput;
    public Mutex file;
    public Scheduler sched;
    public MutexController(Scheduler sched) {
        userInput = new Mutex();
        userOutput = new Mutex();
        file = new Mutex();
        this.sched = sched;
    }
    
    public boolean semWait(Mutex m, Process p) {

        if (m.available) {
            m.available = false;
            return true;
        } 
        else {
            p.pcb.state = State.Blocked;
            m.blockedQueue.add(p);
            BlockedQueue.BlockedQueue.add(p);
            return false;
        }
    }
    
    public void semSignal(Mutex m) { 
        if (!m.blockedQueue.isEmpty()) { 
            Process p = m.blockedQueue.remove();
            BlockedQueue.BlockedQueue.remove(p);
            p.pcb.state = State.Ready;
            if(sched.mode == SMode.MLFQ){
            	sched.MLFQ.get(0).add(p);
            }
            ReadyQueue.ReadyQueue.add(p);
            m.available = true;
        } 
        else {
            m.available = true;
        }
    }
    
}
