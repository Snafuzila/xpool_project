package il.ac.hit.xpool;

import java.util.PriorityQueue;
import java.util.Comparator;

public class ThreadsPool {
    // 1. הגדרת המשתנים כ-Fields של המחלקה
    private final PriorityQueue<Task> taskQueue;
    private final Thread[] workers;

    public ThreadsPool(int numberOfThreads) {
        // 2. אתחול התור בתוך הקונסטרקטור
        // validation check, number of threads greater than 0
        if (numberOfThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be greater than 0. Received: " + numberOfThreads);
        }
        //compare(t2, t1) to make sure bigger priority number is taken first - in PriorityQueue its smallest first.
        this.taskQueue = new PriorityQueue<>((t1, t2) ->
                Integer.compare(t2.getPriority(), t1.getPriority())
        );

        // 3. יצירת מערך החוטים
        this.workers = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            workers[i] = new Worker(); // יצירת החוט (המחלקה הפנימית)
            workers[i].start();        // הפעלה שלו
        }
    }

    public void submit(Task task) {
        synchronized (taskQueue) {
            taskQueue.add(task);
            // מעירים חוט אחד שממתין ב-wait()
            taskQueue.notify();
        }
    }
    public void updateTaskPriority(Task task, int newLevel) {
        synchronized (taskQueue) {
            // 1. ננסה להוציא את המשימה מהתור (O(n))
            if (taskQueue.remove(task)) {
                // 2. אם היא הייתה בתור, נעדכן את הערך ונוסיף אותה מחדש (O(log n))
                task.setPriority(newLevel);
                taskQueue.add(task);
                // 3. אין צורך ב-notify() כי מספר המשימות לא השתנה, רק הסדר
            } else {
                /*
                 * אם המשימה לא נמצאה בתור, זה אומר שהיא כנראה כבר בביצוע.
                 * לפי דרישות המרצה, משימות בביצוע לא יופסקו.
                 * לכן רק נעדכן את הערך באובייקט למקרה שהמשתמש בודק אותו.
                 */
                task.setPriority(newLevel);
            }
        }
    }

    // 4. המחלקה הפנימית בתוך ThreadsPool
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
                            // אם החוט הופרע, הוא מסיים את הריצה שלו
                            return;
                        }
                    }
                    task = taskQueue.poll();
                }

                if (task != null) {
                    try {
                        task.perform();
                    } catch (Exception e) {
                        // מונע מהחוט ב-Pool למות אם המשימה נכשלה
                        System.err.println("Task execution failed: " + e.getMessage());
                    }
                }
            }
        }
    }
}