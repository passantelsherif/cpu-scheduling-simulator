# CPU Scheduling Algorithms

A comprehensive educational project implementing fundamental CPU scheduling algorithms with detailed analysis, visualization tools, and performance metrics.

## Overview

This repository contains production-ready implementations of four essential CPU scheduling algorithms studied in Operating Systems courses. Each algorithm includes detailed documentation, performance analysis, and visual representations to help understand scheduling behavior and trade-offs.

## Algorithms Implemented

### 1. **SJF (Shortest Job First)**
- Non-preemptive scheduling based on job burst time
- Minimizes average waiting time
- Optimal for known job durations

### 2. **Round Robin (RR)**
- Preemptive scheduling with fixed time quantum
- Fair CPU allocation
- Commonly used in modern systems

### 3. **Priority Scheduling with Aging**
- Process priority-based scheduling
- Aging mechanism to prevent starvation
- Dynamic priority adjustment

### 4. **Adaptive Gradient (AG)**
- Advanced adaptive scheduling algorithm
- Adjusts scheduling decisions based on system metrics
- Optimizes for mixed workloads

## Features

- 📊 **Performance Metrics**: Turnaround time, waiting time, response time analysis
- 🎨 **Visualization**: Gantt charts and scheduling timeline representations
- 📈 **Comparative Analysis**: Side-by-side algorithm performance comparison
- 🧪 **Test Suites**: Comprehensive test cases with various workload scenarios
- 📚 **Detailed Documentation**: Algorithm explanation and complexity analysis

### Installation

```bash
git clone https://github.com/passantelsherif/cpu-scheduling-algorithms.git
cd cpu-scheduling-algorithms
```

### Usage

```python
from schedulers import SJF, RoundRobin, PriorityScheduling, AdaptiveGradient

# Example: Running SJF scheduler
processes = [
    {"id": 1, "burst_time": 8, "arrival_time": 0},
    {"id": 2, "burst_time": 4, "arrival_time": 1},
    {"id": 3, "burst_time": 2, "arrival_time": 2},
]

sjf = SJF(processes)
sjf.schedule()
sjf.display_results()
```

## Algorithm Complexity Analysis

| Algorithm | Time Complexity | Space Complexity | Use Case |
|-----------|-----------------|------------------|----------|
| SJF | O(n²) | O(n) | Short jobs, offline scheduling |
| Round Robin | O(n) | O(n) | Interactive systems, fairness |
| Priority Scheduling | O(n²) | O(n) | Real-time systems |
| Adaptive Gradient | O(n log n) | O(n) | Mixed workloads |


## Key Metrics

- **Turnaround Time**: Total time from arrival to completion
- **Waiting Time**: Time spent in ready queue
- **Response Time**: Time from arrival to first execution
- **CPU Utilization**: Percentage of time CPU is actively executing
