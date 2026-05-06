import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class CPUScheduler {
    // Represents one process from the JSON input
    public static class ProcessData {
        public String name;
        public int arrival;
        public int burst;
        public int priority;

        public ProcessData(String name, int arrival, int burst, int priority) {
            this.name = name;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
        }
    }

    // Represents simulation input section
    public static class SchedulerInput {
        public int contextSwitch;
        public int rrQuantum;
        public int agingInterval;
        public List<ProcessData> processes = new ArrayList<>();
    }

    // Represents a process during simulation
    public static class Process {
        public String name;
        public int arrivalTime;
        public int burstTime;
        public int remainingTime;
        public int priority;
        public int quantum;

        public Integer finishTime = null;
        public int waitingTime = 0;
        public int turnAroundTime = 0;
        public int lastTimeAged = 0; // Track when this process was last aged
        public List<Integer> quantumHistory = new ArrayList<>();
        public boolean Completed = false;

        public Process(String name, int arrivalTime, int burstTime, int priority, int quantum) {
            this.name = name;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
            this.priority = priority;
            this.quantum = quantum;
            this.lastTimeAged = arrivalTime; // Initialize to arrival time
            this.quantumHistory.add(quantum);
        }
    }

    // Represents the final per-process result
    public static class ProcessResult {
        public String name;
        public int waitingTime;
        public int turnaroundTime;

        public ProcessResult(String name, int waitingTime, int turnaroundTime) {
            this.name = name;
            this.waitingTime = waitingTime;
            this.turnaroundTime = turnaroundTime;
        }
    }

    // Represents the full result of a scheduler run
    public static class SchedulerResult {
        public List<String> executionOrder = new ArrayList<>();
        public List<ProcessResult> processResults = new ArrayList<>();
        public double averageWaitingTime;
        public double averageTurnaroundTime;
        public Map<String, List<Integer>> quantumHistory = new HashMap<>();
    }

    // Represents one test case file
    public static class TestCase {
        public String name;
        public SchedulerInput input = new SchedulerInput();
    }

    public static class TestLoader {
        public static TestCase loadFromFile(String filename) throws IOException, ParseException {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(new FileReader(filename));

            TestCase test = new TestCase();
            test.name = (String) root.get("name");

            JSONObject input = (JSONObject) root.get("input");
            test.input.contextSwitch = ((Long) input.get("contextSwitch")).intValue();
            test.input.rrQuantum = ((Long) input.get("rrQuantum")).intValue();
            test.input.agingInterval = ((Long) input.get("agingInterval")).intValue();

            JSONArray procArr = (JSONArray) input.get("processes");
            for (Object o : procArr) {
                JSONObject p = (JSONObject) o;
                String name = (String) p.get("name");
                int arrival = ((Long) p.get("arrival")).intValue();
                int burst = ((Long) p.get("burst")).intValue();
                int priority = ((Long) p.get("priority")).intValue();
                test.input.processes.add(new ProcessData(name, arrival, burst, priority));
            }
            return test;
        }
        
        public static List<Process> loadAGTestFromFile(String filename) throws IOException, ParseException {
            JSONParser parser = new JSONParser();
            JSONObject root = (JSONObject) parser.parse(new FileReader(filename));

            JSONObject input = (JSONObject) root.get("input");
            JSONArray procArr = (JSONArray) input.get("processes");
            
            List<Process> processes = new ArrayList<>();
            for (Object o : procArr) {
                JSONObject p = (JSONObject) o;
                String name = (String) p.get("name");
                int arrival = ((Long) p.get("arrival")).intValue();
                int burst = ((Long) p.get("burst")).intValue();
                int priority = ((Long) p.get("priority")).intValue();
                int quantum = ((Long) p.get("quantum")).intValue();
                processes.add(new Process(name, arrival, burst, priority, quantum));
            }
            return processes;
        }
    }

    public static class SJFScheduler {
        public static SchedulerResult runSJF(List<Process> processes, int contextSwitchTime) {
            SchedulerResult result = new SchedulerResult();
            
            // Array of all process for easy indexing
            Process[] procArray = new Process[processes.size()];
            for (int i = 0; i < processes.size(); i++) {
                Process p = processes.get(i);
                procArray[i] = new Process(p.name, p.arrivalTime, p.burstTime, p.priority, p.quantum);
            }

            // Sort by arrival time
            Arrays.sort(procArray, (a, b) -> a.arrivalTime - b.arrivalTime);
            
            int currentTime = 0; // The time on the gantt chart
            int completed = 0; // No of completed process
            String lastExecuted = null; // Current executed
            
            while (completed < procArray.length) {
                // Find process with shortest remaining time among those that have arrived
                int idx = -1;
                int shortestRemaining = Integer.MAX_VALUE;
                
                for (int i = 0; i < procArray.length; i++) {
                    Process p = procArray[i];
                    if (p.arrivalTime <= currentTime && p.remainingTime > 0) {
                        if (p.remainingTime < shortestRemaining) {
                            shortestRemaining = p.remainingTime;
                            idx = i;
                        } else if (p.remainingTime == shortestRemaining && idx != -1) {
                            // Tie-breaker: earlier arrival time
                            if (p.arrivalTime < procArray[idx].arrivalTime) {
                                idx = i;
                            }
                        }
                    }
                }
                
                if (idx != -1) {
                    Process selectedProcess = procArray[idx];
                    
                    // Add context switch if switching to different process
                    if (lastExecuted != null && !lastExecuted.equals(selectedProcess.name)) {
                        currentTime += contextSwitchTime;
                    }
                    
                    // Execute process for 1 time unit
                    result.executionOrder.add(selectedProcess.name);
                    selectedProcess.remainingTime--;
                    currentTime++;
                    
                    if (selectedProcess.remainingTime == 0) {
                        selectedProcess.finishTime = currentTime;
                        selectedProcess.Completed = true;
                        completed++;
                    }
                    
                    lastExecuted = selectedProcess.name;
                } else {
                    // No process ready, idle
                    currentTime++;
                }
            }
            
            // Calculate waiting time and turn around time
            for (Process p : procArray) {
                p.turnAroundTime = p.finishTime - p.arrivalTime;
                p.waitingTime = p.turnAroundTime - p.burstTime;
            }
            
            // Calculate results
            calculateResults(result, procArray);
            
            return result;
        }
        
        private static void calculateResults(SchedulerResult result, Process[] processes) {
            double totalWaiting = 0;
            double totalTurnaround = 0;
            
            for (Process p : processes) {
                ProcessResult pr = new ProcessResult(p.name, p.waitingTime, p.turnAroundTime);
                result.processResults.add(pr);
                totalWaiting += p.waitingTime;
                totalTurnaround += p.turnAroundTime;
            }
            
            result.averageWaitingTime = totalWaiting / processes.length;
            result.averageTurnaroundTime = totalTurnaround / processes.length;
        }
    }

    public static class RRScheduler {
        public static SchedulerResult runRR(List<Process> processes, int contextSwitchTime, int quantum) {
            SchedulerResult result = new SchedulerResult();
            
            // Create process array
            Process[] procArray = new Process[processes.size()];
            for (int i = 0; i < processes.size(); i++) {
                Process p = processes.get(i);
                procArray[i] = new Process(p.name, p.arrivalTime, p.burstTime, p.priority, quantum);
            }
            
            Queue<Process> readyQueue = new LinkedList<>(); // The read queue of processes
            int currentTime = 0; // The time on the guantt chart
            int completed = 0; // No of completed processes
            String lastExecuted = null; // Current last executed 
            int nextArrivalIndex = 0; // pointer to the next place in the queue
            
            // Sort by arrival time to know when to add processes
            Arrays.sort(procArray, (a, b) -> a.arrivalTime - b.arrivalTime);
            
            // Add first process if it arrives at time 0
            if (procArray[0].arrivalTime == 0) {
                readyQueue.add(procArray[0]);
                nextArrivalIndex = 1;
            }
            
            while (completed < procArray.length) {
                if (readyQueue.isEmpty()) {
                    // Fast forward to next process arrival
                    if (nextArrivalIndex < procArray.length) {
                        currentTime = procArray[nextArrivalIndex].arrivalTime;
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    continue;
                }
                
                Process current = readyQueue.poll();
                
                // Add context switch if switching to different process
                if (lastExecuted != null && !lastExecuted.equals(current.name)) {
                    currentTime += contextSwitchTime;
                }
                
                // Execute for quantum or remaining time, whichever is less
                int executeTime = Math.min(quantum, current.remainingTime);
                
                // Add to execution order for each time unit
                for (int i = 0; i < executeTime; i++) {
                    result.executionOrder.add(current.name);
                }
                
                current.remainingTime -= executeTime;
                currentTime += executeTime;
                lastExecuted = current.name;
                
                // Check for new arrivals during execution and add them to ready queue
                while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                    readyQueue.add(procArray[nextArrivalIndex]);
                    nextArrivalIndex++;
                }
                
                // If process completed
                if (current.remainingTime == 0) {
                    current.finishTime = currentTime;
                    current.Completed = true;
                    completed++;
                } else {
                    // Process not completed, add back to ready queue
                    readyQueue.add(current);
                }
            }
            
            // Calculate waiting time and turnaround time
            for (Process p : procArray) {
                p.turnAroundTime = p.finishTime - p.arrivalTime;
                p.waitingTime = p.turnAroundTime - p.burstTime;
            }
            
            // Calculate results
            calculateResults(result, procArray);
            
            return result;
        }
        
        private static void calculateResults(SchedulerResult result, Process[] processes) {
            double totalWaiting = 0;
            double totalTurnaround = 0;
            
            for (Process p : processes) {
                ProcessResult pr = new ProcessResult(p.name, p.waitingTime, p.turnAroundTime);
                result.processResults.add(pr);
                totalWaiting += p.waitingTime;
                totalTurnaround += p.turnAroundTime;
            }
            
            result.averageWaitingTime = totalWaiting / processes.length;
            result.averageTurnaroundTime = totalTurnaround / processes.length;
        }
    }

    public static class PriorityScheduler {
        public static SchedulerResult runPriority(List<Process> processes, int contextSwitchTime, int agingInterval) {
            SchedulerResult result = new SchedulerResult();
            
            // Create process array
            Process[] procArray = new Process[processes.size()];
            for (int i = 0; i < processes.size(); i++) {
                Process p = processes.get(i);
                procArray[i] = new Process(p.name, p.arrivalTime, p.burstTime, p.priority, p.quantum);
            }
            
            List<Process> readyQueue = new ArrayList<>();
            int currentTime = 0;
            int completed = 0;
            String lastExecuted = null;
            int nextArrivalIndex = 0;
            
            // Sort by arrival time
            Arrays.sort(procArray, (a, b) -> a.arrivalTime - b.arrivalTime);
            
            // Add first process if it arrives at time 0
            if (procArray[0].arrivalTime == 0) {
                readyQueue.add(procArray[0]);
                nextArrivalIndex = 1;
            }
            
            while (completed < procArray.length) {
                // Add newly arrived processes to ready queue
                while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                    readyQueue.add(procArray[nextArrivalIndex]);
                    nextArrivalIndex++;
                }
                
                if (readyQueue.isEmpty()) {
                    // Jump to next arrival
                    if (nextArrivalIndex < procArray.length) {
                        currentTime = procArray[nextArrivalIndex].arrivalTime;
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    lastExecuted = null;
                    continue;
                }
                
                // Update priorities based on aging before selecting
                for (Process p : readyQueue) {
                    int timeSinceLastAged = currentTime - p.lastTimeAged;
                    if (timeSinceLastAged >= agingInterval) {
                        p.priority = Math.max(1, p.priority - 1);
                        p.lastTimeAged = currentTime;
                    }
                }
                
                // Find highest priority process in ready queue
                Process current = null;
                int highestPriority = Integer.MAX_VALUE;
                
                for (Process p : readyQueue) {
                    if (p.priority < highestPriority) {
                        highestPriority = p.priority;
                        current = p;
                    } else if (p.priority == highestPriority && current != null) {
                        // Same priority - check arrival time
                        if (p.arrivalTime < current.arrivalTime) {
                            current = p;
                        } else if (p.arrivalTime == current.arrivalTime) {
                            // Same arrival time - check process ID
                            if (p.name.compareTo(current.name) < 0) {
                                current = p;
                            }
                        }
                    }
                }
                
                readyQueue.remove(current);
                
                // Add context switch if switching to different process
                if (lastExecuted != null && !lastExecuted.equals(current.name)) {
                    currentTime += contextSwitchTime;
                    
                    // Check for new arrivals during context switch
                    while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    
                    // Re-evaluate priorities after context switch (aging may have occurred)
                    for (Process p : readyQueue) {
                        int timeSinceLastAged = currentTime - p.lastTimeAged;
                        if (timeSinceLastAged >= agingInterval) {
                            p.priority = Math.max(1, p.priority - 1);
                            p.lastTimeAged = currentTime;
                        }
                    }
                    
                    // Also check current process
                    int timeSinceLastAged = currentTime - current.lastTimeAged;
                    if (timeSinceLastAged >= agingInterval) {
                        current.priority = Math.max(1, current.priority - 1);
                        current.lastTimeAged = currentTime;
                    }
                    
                    // Re-select highest priority process after context switch
                    readyQueue.add(current);
                    
                    Process previouslySelected = current;
                    current = null;
                    int newHighestPriority = Integer.MAX_VALUE;
                    
                    for (Process p : readyQueue) {
                        if (p.priority < newHighestPriority) {
                            newHighestPriority = p.priority;
                            current = p;
                        } else if (p.priority == newHighestPriority && current != null) {
                            // Same priority - check arrival time
                            if (p.arrivalTime < current.arrivalTime) {
                                current = p;
                            } else if (p.arrivalTime == current.arrivalTime) {
                                // Same arrival time - check process ID
                                if (p.name.compareTo(current.name) < 0) {
                                    current = p;
                                }
                            }
                        }
                    }
                    
                    readyQueue.remove(current);
                    
                    // If process changed after re-evaluation, add to execution order and do another CS
                    if (!current.name.equals(previouslySelected.name)) {
                        result.executionOrder.add(previouslySelected.name);
                        currentTime += contextSwitchTime;
                        lastExecuted = previouslySelected.name;
                        
                        // Re-evaluate aging after second context switch
                        for (Process p : readyQueue) {
                            int timeSinceLastAged2 = currentTime - p.lastTimeAged;
                            if (timeSinceLastAged2 >= agingInterval) {
                                p.priority = Math.max(1, p.priority - 1);
                                p.lastTimeAged = currentTime;
                            }
                        }
                    }
                }
                
                // Track base priority when process starts running (no aging while running)
                int runningProcessBasePriority = current.priority;
                
                // Execute the current process one unit at a time
                while (current.remainingTime > 0) {
                    // Execute for 1 time unit
                    result.executionOrder.add(current.name);
                    current.remainingTime--;
                    currentTime++;
                    
                    // Check for new arrivals
                    while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    
                    // Check if process completed
                    if (current.remainingTime == 0) {
                        current.finishTime = currentTime;
                        current.Completed = true;
                        completed++;
                        break;
                    }
                    
                    // Update priorities for processes in ready queue
                    for (Process p : readyQueue) {
                        int timeSinceLastAged = currentTime - p.lastTimeAged;
                        if (timeSinceLastAged >= agingInterval) {
                            p.priority = Math.max(1, p.priority - 1);
                            p.lastTimeAged = currentTime;
                        }
                    }
                    
                    // Check for preemption: find highest priority in queue
                    if (!readyQueue.isEmpty()) {
                        Process highestInQueue = null;
                        int lowestPriorityValue = runningProcessBasePriority; // Use base priority (no aging while running)
                        
                        for (Process p : readyQueue) {
                            if (p.priority < lowestPriorityValue) {
                                // Higher priority (lower value)
                                lowestPriorityValue = p.priority;
                                highestInQueue = p;
                            } else if (p.priority == lowestPriorityValue) {
                                // Same priority - check arrival time
                                if (p.arrivalTime < current.arrivalTime) {
                                    highestInQueue = p;
                                } else if (p.arrivalTime == current.arrivalTime && highestInQueue == null) {
                                    // Same priority and arrival time - check process ID
                                    if (p.name.compareTo(current.name) < 0) {
                                        highestInQueue = p;
                                    }
                                }
                            }
                        }
                        
                        // Preempt if a higher priority process found (or better tie-breaker)
                        if (highestInQueue != null) {
                            readyQueue.add(current);
                            current.lastTimeAged = currentTime;
                            break;
                        }
                    }
                }
                
                lastExecuted = current.name;
            }
            
            // Calculate waiting time and turnaround time
            for (Process p : procArray) {
                p.turnAroundTime = p.finishTime - p.arrivalTime;
                p.waitingTime = p.turnAroundTime - p.burstTime;
            }
            
            // Calculate results
            calculateResults(result, procArray);
            
            return result;
        }
        
        private static void calculateResults(SchedulerResult result, Process[] processes) {
            double totalWaiting = 0;
            double totalTurnaround = 0;
            
            for (Process p : processes) {
                ProcessResult pr = new ProcessResult(p.name, p.waitingTime, p.turnAroundTime);
                result.processResults.add(pr);
                totalWaiting += p.waitingTime;
                totalTurnaround += p.turnAroundTime;
            }
            
            result.averageWaitingTime = totalWaiting / processes.length;
            result.averageTurnaroundTime = totalTurnaround / processes.length;
        }
    }

    public static class AGScheduler {
        public static SchedulerResult runAG(List<Process> processes) {
            SchedulerResult result = new SchedulerResult();
            
            // Create process array
            Process[] procArray = new Process[processes.size()];
            for (int i = 0; i < processes.size(); i++) {
                Process p = processes.get(i);
                procArray[i] = new Process(p.name, p.arrivalTime, p.burstTime, p.priority, p.quantum);
            }
            
            Queue<Process> readyQueue = new LinkedList<>();
            int currentTime = 0;
            int completed = 0;
            String lastExecuted = null;
            int nextArrivalIndex = 0;
            
            // Sort by arrival time
            Arrays.sort(procArray, (a, b) -> a.arrivalTime - b.arrivalTime);
            
            // Add first process if it arrives at time 0
            if (procArray[0].arrivalTime == 0) {
                readyQueue.add(procArray[0]);
                nextArrivalIndex = 1;
            }
            
            while (completed < procArray.length) {
                // Add newly arrived processes to ready queue
                while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                    readyQueue.add(procArray[nextArrivalIndex]);
                    nextArrivalIndex++;
                }
                
                if (readyQueue.isEmpty()) {
                    // Jump to next arrival
                    if (nextArrivalIndex < procArray.length) {
                        currentTime = procArray[nextArrivalIndex].arrivalTime;
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    continue;
                }
                
                Process current = readyQueue.poll();
                
                int initialQuantum = current.quantum;
                int timeExecuted = 0;
                
                // Phase 1: First 25% - Non-preemptive FCFS
                int phase1Time = (int) Math.ceil(initialQuantum * 0.25);
                int phase1Exec = Math.min(phase1Time, current.remainingTime);
                
                for (int i = 0; i < phase1Exec; i++) {
                    result.executionOrder.add(current.name);
                    currentTime++;
                    current.remainingTime--;
                    timeExecuted++;
                    
                    // Check for arrivals
                    while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    
                    if (current.remainingTime == 0) {
                        current.Completed = true;
                        current.finishTime = currentTime;
                        current.quantum = 0;
                        current.quantumHistory.add(0);
                        completed++;
                        lastExecuted = current.name;
                        break;
                    }
                }
                
                if (current.Completed) {
                    continue;
                }
                
                // Check if we need to switch after phase 1 (priority check)
                if (timeExecuted >= phase1Time && !readyQueue.isEmpty()) {
                    // Find process with lowest priority value (highest priority)
                    Process highestPriorityProcess = null;
                    int lowestPriorityValue = Integer.MAX_VALUE;
                    
                    for (Process p : readyQueue) {
                        if (p.priority < lowestPriorityValue) {
                            lowestPriorityValue = p.priority;
                            highestPriorityProcess = p;
                        }
                    }
                    
                    // Switch if a higher priority process is found
                    if (highestPriorityProcess != null && lowestPriorityValue < current.priority) {
                        int remainingQuantum = initialQuantum - timeExecuted;
                        current.quantum = current.quantum + (int) Math.ceil(remainingQuantum / 2.0);
                        current.quantumHistory.add(current.quantum);
                        // Remove the higher priority process and add current to end
                        readyQueue.remove(highestPriorityProcess);
                        readyQueue.add(current);
                        // Add the higher priority process at the front to run next
                        ((LinkedList<Process>)readyQueue).addFirst(highestPriorityProcess);
                        lastExecuted = current.name;
                        continue;
                    }
                }
                
                // Phase 2: Next 25% - Non-preemptive Priority
                int phase2Time = (int) Math.ceil(initialQuantum * 0.25);
                int phase2Exec = Math.min(phase2Time, current.remainingTime);
                
                for (int i = 0; i < phase2Exec; i++) {
                    result.executionOrder.add(current.name);
                    currentTime++;
                    current.remainingTime--;
                    timeExecuted++;
                    
                    // Check for arrivals
                    while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    
                    if (current.remainingTime == 0) {
                        current.Completed = true;
                        current.finishTime = currentTime;
                        current.quantum = 0;
                        current.quantumHistory.add(0);
                        completed++;
                        lastExecuted = current.name;
                        break;
                    }
                }
                
                if (current.Completed) {
                    continue;
                }
                
                // Check if we need to switch after phase 2 (SJF check)
                if (timeExecuted >= (phase1Time + phase2Time) && !readyQueue.isEmpty()) {
                    // Find process with shortest remaining time
                    Process shortestProcess = null;
                    int shortestTime = Integer.MAX_VALUE;
                    
                    for (Process p : readyQueue) {
                        if (p.remainingTime < shortestTime) {
                            shortestTime = p.remainingTime;
                            shortestProcess = p;
                        }
                    }
                    
                    // Switch if a shorter process is found
                    if (shortestProcess != null && shortestProcess.remainingTime < current.remainingTime) {
                        int remainingQuantum = initialQuantum - timeExecuted;
                        current.quantum = current.quantum + remainingQuantum;
                        current.quantumHistory.add(current.quantum);
                        // Remove the shorter process and add current to end
                        readyQueue.remove(shortestProcess);
                        readyQueue.add(current);
                        // Add the shorter process at the front to run next
                        ((LinkedList<Process>)readyQueue).addFirst(shortestProcess);
                        lastExecuted = current.name;
                        continue;
                    }
                }
                
                // Phase 3: Remaining 50% - Preemptive SJF (unit time)
                while (current.remainingTime > 0 && timeExecuted < initialQuantum) {
                    result.executionOrder.add(current.name);
                    currentTime++;
                    current.remainingTime--;
                    timeExecuted++;
                    
                    // Check for arrivals
                    while (nextArrivalIndex < procArray.length && procArray[nextArrivalIndex].arrivalTime <= currentTime) {
                        readyQueue.add(procArray[nextArrivalIndex]);
                        nextArrivalIndex++;
                    }
                    
                    if (current.remainingTime == 0) {
                        current.Completed = true;
                        current.finishTime = currentTime;
                        current.quantum = 0;
                        current.quantumHistory.add(0);
                        completed++;
                        lastExecuted = current.name;
                        break;
                    }
                    
                    // Check if a shorter job has arrived (preemptive SJF)
                    if (!readyQueue.isEmpty()) {
                        Process shortestProcess = null;
                        int shortestTime = current.remainingTime;
                        
                        for (Process p : readyQueue) {
                            if (p.remainingTime < shortestTime) {
                                shortestTime = p.remainingTime;
                                shortestProcess = p;
                            }
                        }
                        
                        if (shortestProcess != null) {
                            int remainingQuantum = initialQuantum - timeExecuted;
                            current.quantum = current.quantum + remainingQuantum;
                            current.quantumHistory.add(current.quantum);
                            // Remove the shorter process and add current to end
                            readyQueue.remove(shortestProcess);
                            readyQueue.add(current);
                            // Add the shorter process at the front to run next
                            ((LinkedList<Process>)readyQueue).addFirst(shortestProcess);
                            lastExecuted = current.name;
                            break;
                        }
                    }
                }
                
                // If process used all quantum but not completed
                if (!current.Completed && timeExecuted >= initialQuantum) {
                    current.quantum = current.quantum + 2;
                    current.quantumHistory.add(current.quantum);
                    readyQueue.add(current);
                    lastExecuted = current.name;
                } else if (!current.Completed) {
                    // Process didn't complete and didn't use all quantum - shouldn't happen but handle it
                    lastExecuted = current.name;
                } else {
                    // Process completed
                    lastExecuted = current.name;
                }
            }
            
            // Calculate waiting time and turnaround time
            for (Process p : procArray) {
                p.turnAroundTime = p.finishTime - p.arrivalTime;
                p.waitingTime = p.turnAroundTime - p.burstTime;
            }
            
            // Calculate results
            calculateResults(result, procArray);
            
            return result;
        }
        
        private static void calculateResults(SchedulerResult result, Process[] processes) {
            double totalWaiting = 0;
            double totalTurnaround = 0;
            
            for (Process p : processes) {
                ProcessResult pr = new ProcessResult(p.name, p.waitingTime, p.turnAroundTime);
                result.processResults.add(pr);
                result.quantumHistory.put(p.name, p.quantumHistory);
                totalWaiting += p.waitingTime;
                totalTurnaround += p.turnAroundTime;
            }
            
            result.averageWaitingTime = totalWaiting / processes.length;
            result.averageTurnaroundTime = totalTurnaround / processes.length;
        }
    }

    public static void main(String[] args) {
        try {
            // For running AG test files
            for (int i = 1; i <= 6; i++) {
                String filename = "test_cases_v5/AG/AG_test" + i + ".json";
                System.out.println("=".repeat(70));
                System.out.println("Testing file: " + filename);
                System.out.println("=".repeat(70));
                
                // Load AG test file with per-process quantum
                List<Process> agProcesses = TestLoader.loadAGTestFromFile(filename);
                
                // Parse expected output
                JSONParser parser = new JSONParser();
                JSONObject root = (JSONObject) parser.parse(new FileReader(filename));
                JSONObject expectedOutput = (JSONObject) root.get("expectedOutput");
                
                // Run AG Scheduler
                SchedulerResult agResult = AGScheduler.runAG(agProcesses);
                
                System.out.println("\n=== AG Scheduler ===");
                System.out.println("Execution Order:");
                printExecutionOrder(agResult.executionOrder);
                
                System.out.println("\nProcess Results:");
                System.out.println("Name\tWaiting\tTurnAround\tQuantum History");
                System.out.println("-".repeat(60));
                for (ProcessResult pr : agResult.processResults) {
                    System.out.println(pr.name + "\t" + pr.waitingTime + "\t" + 
                        pr.turnaroundTime + "\t\t" + agResult.quantumHistory.get(pr.name));
                }
                
                System.out.println("\nAverage Waiting Time: " + 
                    String.format("%.2f", agResult.averageWaitingTime));
                System.out.println("Average TurnAround Time: " + 
                    String.format("%.2f", agResult.averageTurnaroundTime));
                
                // Validate results
                validateScheduler(agResult, expectedOutput, null);
                System.out.println();
            }
            
            // For running normal test files
            for (int i = 1; i <= 6; i++) {
                String filename = "test_cases_v5/Other_Schedulers/test_" + i + ".json";
                System.out.println("\n" + "=".repeat(70));
                System.out.println("Testing file: " + filename);
                System.out.println("=".repeat(70));
                
                TestCase test = TestLoader.loadFromFile(filename);
                
                // Parse expected output
                JSONParser parser = new JSONParser();
                JSONObject root = (JSONObject) parser.parse(new FileReader(filename));
                JSONObject expectedOutput = (JSONObject) root.get("expectedOutput");
                
                // Convert to simulation processes
                List<Process> processes = new ArrayList<>();
                for (ProcessData p : test.input.processes) {
                    processes.add(new Process(
                        p.name,
                        p.arrival,
                        p.burst,
                        p.priority,
                        test.input.rrQuantum
                    ));
                }
                
                // Run SJF Scheduler
                SchedulerResult sjfResult = SJFScheduler.runSJF(processes, test.input.contextSwitch);
                
                System.out.println("\n=== SJF Scheduler ===");
                System.out.println("Execution Order:");
                printExecutionOrder(sjfResult.executionOrder);
                
                System.out.println("\nProcess Results:");
                System.out.println("Name\tWaiting\tTurnAround");
                System.out.println("-".repeat(30));
                for (ProcessResult pr : sjfResult.processResults) {
                    System.out.printf("%s\t%d\t%d%n", 
                        pr.name, pr.waitingTime, pr.turnaroundTime);
                }
                
                System.out.println("\nAverage Waiting Time: " + 
                    String.format("%.2f", sjfResult.averageWaitingTime));
                System.out.println("Average TurnAround Time: " + 
                    String.format("%.2f", sjfResult.averageTurnaroundTime));
                
                // Validate SJF
                validateScheduler(sjfResult, expectedOutput, "SJF");
                
                // Run RR Scheduler
                SchedulerResult rrResult = RRScheduler.runRR(processes, test.input.contextSwitch, test.input.rrQuantum);
                
                System.out.println("\n=== RR Scheduler ===");
                System.out.println("Execution Order:");
                printExecutionOrder(rrResult.executionOrder);
                
                System.out.println("\nProcess Results:");
                System.out.println("Name\tWaiting\tTurnAround");
                System.out.println("-".repeat(30));
                for (ProcessResult pr : rrResult.processResults) {
                    System.out.printf("%s\t%d\t%d%n", 
                        pr.name, pr.waitingTime, pr.turnaroundTime);
                }
                
                System.out.println("\nAverage Waiting Time: " + 
                    String.format("%.2f", rrResult.averageWaitingTime));
                System.out.println("Average TurnAround Time: " + 
                    String.format("%.2f", rrResult.averageTurnaroundTime));
                
                // Validate RR
                validateScheduler(rrResult, expectedOutput, "RR");
                
                // Run Priority Scheduler
                SchedulerResult priorityResult = PriorityScheduler.runPriority(processes, test.input.contextSwitch, test.input.agingInterval);
                
                System.out.println("\n=== Priority Scheduler ===");
                printExecutionOrder(priorityResult.executionOrder);
                
                System.out.println("\nProcess Results:");
                System.out.println("Name\tWaiting\tTurnAround");
                System.out.println("-".repeat(30));
                for (ProcessResult pr : priorityResult.processResults) {
                    System.out.printf("%s\t%d\t%d%n", 
                        pr.name, pr.waitingTime, pr.turnaroundTime);
                }
                
                System.out.println("\nAverage Waiting Time: " + 
                    String.format("%.2f", priorityResult.averageWaitingTime));
                System.out.println("Average TurnAround Time: " + 
                    String.format("%.2f", priorityResult.averageTurnaroundTime));
                
                // Validate Priority
                validateScheduler(priorityResult, expectedOutput, "Priority");
            }
        } catch (IOException | ParseException e) {
            System.out.println("Error loading JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method for execution order
    private static void printExecutionOrder(List<String> order) {
        StringBuilder sb = new StringBuilder();
        String last = null;
        
        for (String current : order) {
            if (last == null) {
                last = current;
            } else if (current.equals(last)) {
                // Skip duplicates
                continue;
            } else {
                sb.append(last).append(" ");
                last = current;
            }
        }
        
        // Add the last process
        if (last != null) {
            sb.append(last);
        }
        
        System.out.println(sb.toString());
    }

    // Helper method to remove consecutive duplicates for validation
    private static List<String> removeDuplicates(List<String> order) {
        List<String> result = new ArrayList<>();
        String last = null;
        
        for (String current : order) {
            if (last == null || !current.equals(last)) {
                result.add(current);
                last = current;
            }
        }
        
        return result;
    }

    // Validate scheduler output against expected results using assertions
    private static void validateScheduler(SchedulerResult result, JSONObject expectedOutput, String key) {
        try {
            // For AG tests, key is null and expectedOutput is used directly
            JSONObject expected = (key == null) ? expectedOutput : (JSONObject) expectedOutput.get(key);
            if (expected == null) return;
            
            // Check execution order
            JSONArray expectedOrder = (JSONArray) expected.get("executionOrder");
            List<String> expectedOrderList = new ArrayList<>();
            for (Object o : expectedOrder) {
                expectedOrderList.add((String) o);
            }
            
            List<String> actualOrderNoDuplicates = removeDuplicates(result.executionOrder);
            assert actualOrderNoDuplicates.equals(expectedOrderList);
            
            // Check process results
            JSONArray expectedResults = (JSONArray) expected.get("processResults");
            Map<String, ProcessResult> actualMap = new HashMap<>();
            for (ProcessResult pr : result.processResults) {
                actualMap.put(pr.name, pr);
            }
            
            for (Object o : expectedResults) {
                JSONObject exp = (JSONObject) o;
                String name = (String) exp.get("name");
                int expectedWT = ((Long) exp.get("waitingTime")).intValue();
                int expectedTAT = ((Long) exp.get("turnaroundTime")).intValue();
                
                ProcessResult actual = actualMap.get(name);
                assert actual != null;
                assert actual.waitingTime == expectedWT;
                assert actual.turnaroundTime == expectedTAT;
            }
            
            // Check averages
            double expectedAvgWT = ((Number) expected.get("averageWaitingTime")).doubleValue();
            double expectedAvgTAT = ((Number) expected.get("averageTurnaroundTime")).doubleValue();
            
            assert Math.abs(result.averageWaitingTime - expectedAvgWT) <= 0.01;
            assert Math.abs(result.averageTurnaroundTime - expectedAvgTAT) <= 0.01;
            
            // Check quantum history for AG
            if (expected.containsKey("quantumHistory")) {
                JSONObject expectedQH = (JSONObject) expected.get("quantumHistory");
                for (Object qKey : expectedQH.keySet()) {
                    String processName = (String) qKey;
                    JSONArray expectedQuantums = (JSONArray) expectedQH.get(processName);
                    List<Integer> expectedQList = new ArrayList<>();
                    for (Object q : expectedQuantums) {
                        expectedQList.add(((Long) q).intValue());
                    }
                    
                    List<Integer> actualQuantums = result.quantumHistory.get(processName);
                    assert actualQuantums != null;
                    assert actualQuantums.equals(expectedQList);
                }
            }
            
            System.out.println("PASSED");
        } catch (AssertionError e) {
            System.out.println("FAILED");
        } catch (Exception e) {
            System.out.println("ERROR");
            e.printStackTrace();
        }
    }
}