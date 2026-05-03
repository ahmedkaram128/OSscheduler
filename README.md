🖥️ Operating System Simulator

A Java-based Operating System simulator that models how an OS manages processes, memory, and CPU execution. The project provides a simplified but realistic environment to demonstrate core OS concepts such as scheduling, context switching, memory allocation, and interrupt handling.

The system loads programs into a constrained memory space, tracks them using Process Control Blocks (PCB), and executes instructions through a simulated CPU. Multiple processes are handled concurrently using different scheduling algorithms, allowing comparison of their behavior and performance.

⚙️ Features
Process lifecycle management (Ready, Running, Blocked, Terminated)
Fixed-size memory model (40 words)
Process Control Blocks (PCB)
Instruction-level execution
Context switching
Interrupt handling and system calls
CPU Scheduling Algorithms:
  -Highest Response Ratio Next (HRRN)
  -Round Robin (RR)
  -Multilevel Feedback Queue (MLFQ)

+----------------------+
|        CPU           |
|  (Executes Instr.)   |
+----------+-----------+
           |
           v
+----------------------+
|     Scheduler        |
| (HRRN / RR / MLFQ)   |
+----------+-----------+
           |
           v
+----------------------+
|     Ready Queue      |
+----------+-----------+
           |
           v
+----------------------+
|       Memory         |
|  (40 Words Total)    |
+----------+-----------+
           |
           v
+----------------------+
|        PCB           |
| Process Metadata     |
+----------------------+

🧠 Memory Layout
+----------------------------------+
| Process 1 Instructions & Data    |
+----------------------------------+
| Process 2 Instructions & Data    |
+----------------------------------+
| Process 3 Instructions & Data    |
+----------------------------------+
|     Free / Unused Space          |
+----------------------------------+
Total Size = 40 Words


🔄 Process Lifecycle
        +--------+
        |  New   |
        +---+----+
            |
            v
        +--------+
        | Ready  |
        +---+----+
            |
            v
        +--------+
        |Running |
        +---+----+
         /     \
        v       v
  +--------+  +-----------+
  |Blocked |  |Terminated |
  +--------+  +-----------+
        |
        v
      Ready

🧠 Scheduling Algorithms Demonstration
🔹 Round Robin (RR)
Each process gets a fixed time quantum and cycles through the ready queue.

Example (Quantum = 2):
Processes:
P1 (5), P2 (3), P3 (2)

Execution Timeline:
| P1 | P2 | P3 | P1 | P2 | P1 |

Explanation:
 -Each process runs for max 2 units
 -If unfinished → goes back to queue
 -Fair but causes frequent context switching


🔹 Highest Response Ratio Next (HRRN)
Formula:
ResponseRatio=(WaitingTime+BurstTime)/BurstTime

Example:
P1: Burst=4, Waiting=4 → RR = 2.0
P2: Burst=2, Waiting=1 → RR = 1.5
P3: Burst=6, Waiting=3 → RR = 1.5

Execution Order:
P1 → P2 → P3

Explanation:
 -Prioritizes long-waiting processes
 -Prevents starvation
 -Balances fairness and efficiency


 🔹 Multilevel Feedback Queue (MLFQ)
 +-----------------------------+
| Queue 1 (High, q=1)         |
+-----------------------------+
| Queue 2 (Medium, q=2)       |
+-----------------------------+
| Queue 3 (Low, FCFS)         |
+-----------------------------+


New Process → Q1
   |
   | uses full quantum
   v
Move to Q2
   |
   | still not finished
   v
Move to Q3


🎯 Purpose
This project bridges the gap between theoretical OS concepts and practical implementation by simulating how real operating systems manage resources, schedule processes, and execute programs.


Screenshot executing the RR algorithm: 
<img width="1060" height="771" alt="image" src="https://github.com/user-attachments/assets/dde95b52-9863-46c8-8b40-fd4d6f847e56" />

<img width="1440" height="945" alt="image" src="https://github.com/user-attachments/assets/dcac7691-eff9-4330-8685-cca2cc28c693" />



