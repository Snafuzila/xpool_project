package il.ac.hit.xpool;

import java.util.PriorityQueue;

/**
 * Manages a pool of worker threads that execute tasks based on priority.
 */
public class ThreadsPool {

    /**
     * The priority queue storing the tasks pending execution.
     */
    private final PriorityQueue<Task> taskQueue;

    /**
     * The array of active worker threads.
     */
    private final Thread[] workers;

    /**
     * Initializes the threads pool with the specified number of worker threads.
     * * @param numberOfThreads the number of threads to spawn
     * @throws IllegalArgumentException if numberOfThreads is 0 or negative
     */
    public ThreadsPool(int numberOfThreads) {
        if (numberOfThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0. Received: " + numberOfThreads);
        }

        /*
         * Compare t2 to t1 to ensure the task with the larger priority number
         * is taken first, as PriorityQueue defaults to smallest first.
         */
        this.taskQueue = new PriorityQueue<>((t1, t2) ->
                Integer.compare(t2.getPriority(), t1.getPriority())
        );

        this.workers = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            workers[i] = new Worker();
            workers[i].start();
        }
    }

    /**
     * Submits a new task to the pool for execution.
     * * @param task the task to be executed
     * @throws IllegalArgumentException if the task is null
     */
    public void submit(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        synchronized (taskQueue) {
            taskQueue.add(task);
            taskQueue.notify();
        }
    }

    /**
     * Updates the priority of a task currently in the pool.
     * * @param task the task to update
     * @param newLevel the new priority level
     * @throws IllegalArgumentException if the task is null
     */
    public void updateTaskPriority(Task task, int newLevel) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        synchronized (taskQueue) {
            if (taskQueue.remove(task)) {
                task.setPriority(newLevel);
                taskQueue.add(task);
            } else {
                /*
                 * If the task is not found in the queue, it is likely already in execution.
                 * We update the value on the object in case the user queries it.
                 */
                task.setPriority(newLevel);
            }
        }
    }

    @Override
    public String toString() {
        return "ThreadsPool{" +
                "taskQueueSize=" + taskQueue.size() +
                ", numberOfWorkers=" + workers.length +
                '}';
    }

    /**
     * A worker thread that continuously polls and executes tasks from the queue.
     */
    private class Worker extends Thread {
        @Override
        public void run() {
            while (true) {
                Task task;

                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                            taskQueue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    task = taskQueue.poll();
                }

                if (task != null) {
                    try {
                        task.perform();
                    } catch (RuntimeException e) {
                        System.err.println("Task execution failed with a runtime exception: " + e.getMessage());
                    }
                }
            }
        }
    }
}