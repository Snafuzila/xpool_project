package il.ac.hit.tests;

import il.ac.hit.xpool.Task;
import il.ac.hit.xpool.ThreadsPool;

public class TestMain {

    // Variables for the Synchronization Test
    private static int sharedCounter = 0;
    private static final Object counterLock = new Object();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- STARTING LOGIC TESTS ---\n");
        testPriorityLogic();
        // Wait a moment before starting the second test to keep console output clean

        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        System.out.println("------------------------------------");
        testSynchronizationLogic();

        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        System.out.println("------------------------------------");
        testDirectSetPriority(); // בדיקה 1: שינוי ישיר (ייכשל בסידור מחדש)

        try { Thread.sleep(3000); } catch (InterruptedException e) {}
        System.out.println("------------------------------------");
        testUpdateTaskPriority(); // בדיקה 2: שינוי דרך ה-Pool (יצליח בסידור מחדש)
    }

    /**
     * TEST 1: PRIORITY LOGIC
     * We use a pool of 1 thread to force a traffic jam in the queue.
     */
    private static void testPriorityLogic() {
        System.out.println(">>> TEST 1: Priority Order Verification");
        ThreadsPool singlePool = new ThreadsPool(1);

        // 1. Send a blocker task to jam the queue
        singlePool.submit(new Task() {
            private int p = 999;
            @Override
            public void perform() {
                System.out.println("[Blocker] Jamming the queue for 2 seconds...");
                try { Thread.sleep(2000); } catch (InterruptedException e) {}
                System.out.println("[Blocker] Queue released! Workers should now process by priority:");
            }
            @Override public void setPriority(int level) { this.p = level; }
            @Override public int getPriority() { return p; }
        });

        // 2. While the queue is jammed, submit tasks 1 through 10 in random order
        int[] prioritiesToSubmit = {3, 1, 8, 2, 10, 5, 7, 4, 9, 6};
        for (int p : prioritiesToSubmit) {
            final int taskPriority = p; // need final for anonymous class
            singlePool.submit(new Task() {
                @Override
                public void perform() {
                    System.out.println("Executed Task Priority: " + taskPriority);
                }
                @Override public void setPriority(int level) {}
                @Override public int getPriority() { return taskPriority; }
            });
        }

        /*
         * EXPECTED RESULT:
         * The console MUST print 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 in PERFECT order.
         */
    }

    /**
     * TEST 2: SYNCHRONIZATION AND RACE CONDITIONS
     * We use multiple threads to hammer a single shared variable.
     */
    private static void testSynchronizationLogic() {
        System.out.println("\n>>> TEST 2: Synchronization & Thread Safety");
        int numberOfThreads = 10;
        int totalTasks = 1000;
        ThreadsPool multiPool = new ThreadsPool(numberOfThreads);

        System.out.println("Submitting " + totalTasks + " tasks to increment a shared counter...");

        for (int i = 0; i < totalTasks; i++) {
            multiPool.submit(new Task() {
                private int p = 1;
                @Override
                public void perform() {
                    // All threads fight to modify this single variable
                    synchronized (counterLock) {
                        sharedCounter++;
                    }
                }
                @Override public void setPriority(int level) { this.p = level; }
                @Override public int getPriority() { return p; }
            });
        }

        // Wait a bit to let all 1000 tasks finish
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        System.out.println("Expected Final Counter: " + totalTasks);
        System.out.println("Actual Final Counter  : " + sharedCounter);

        if (sharedCounter == totalTasks) {
            System.out.println("RESULT: PERFECT SYNCHRONIZATION! No race conditions detected.");
        } else {
            System.out.println("RESULT: FAILED! Threads overwrote each other. Check your synchronized blocks.");
        }
    }

    private static void testDirectSetPriority() throws InterruptedException {
        System.out.println("TEST 1: Direct task.setPriority() (Should NOT re-sort)");
        ThreadsPool pool = new ThreadsPool(1); // חוט אחד בלבד

        // 1. חוסמים את התור
        Task blocker = createStaticTask(999, "BLOCKER",2000);
        pool.submit(blocker);

        // 2. מכניסים שתי משימות: A (עדיפות נמוכה), B (עדיפות בינונית)
        Task taskA = createStaticTask(1, "Task A",0);
        Task taskB = createStaticTask(10, "Task B", 0);

        pool.submit(taskA);
        pool.submit(taskB);

        // 3. משנים את העדיפות של A ישירות ל-100 (גבוה מ-B)
        System.out.println("[Test] Manually changing Task A priority to 100 via setPriority...");
        taskA.setPriority(100);

        // 4. משחררים את החסימה (הדמיה ע"י המתנה קצרה)
        Thread.sleep(1000);
        System.out.println("[Test] Expecting Task B to run BEFORE Task A (because queue didn't re-sort):");
    }

    /**
     * בדיקה 2: מראה ששינוי דרך ה-Pool מעדכן את התור בהצלחה.
     */
    private static void testUpdateTaskPriority() throws InterruptedException {
        System.out.println("TEST 2: pool.updateTaskPriority() (Should SUCCESSFULY re-sort)");
        ThreadsPool pool = new ThreadsPool(1);

        // 1. חוסמים את התור
        pool.submit(createStaticTask(999, "BLOCKER", 2000));

        // 2. מכניסים שתי משימות: C (נמוכה), D (בינונית)
        Task taskC = createStaticTask(1, "Task C", 0);
        Task taskD = createStaticTask(10, "Task D", 0);

        pool.submit(taskC);
        pool.submit(taskD);

        // 3. משנים את העדיפות של C ל-100 דרך ה-Pool
        System.out.println("[Test] Updating Task C priority to 100 via pool.updateTaskPriority...");
        pool.updateTaskPriority(taskC, 100);

        // 4. משחררים חסימה
        Thread.sleep(1000);
        System.out.println("[Test] Expecting Task C to run BEFORE Task D (because queue RE-SORTED):");
    }

    // פונקציית עזר ליצירת משימות פשוטות להדפסה
    private static Task createStaticTask(int initialPriority, String name, int sleepTime) {
        return new Task() {
            private int p = initialPriority;
            @Override
            public void perform() {
                System.out.println(">>> Start Executing: " + name);
                if (sleepTime > 0) {
                    try { Thread.sleep(sleepTime); } catch (InterruptedException e) {}
                }
                System.out.println(">>> Finished: " + name + " (Priority: " + p + ")");
            }
            @Override public void setPriority(int level) { this.p = level; }
            @Override public int getPriority() { return p; }
        };
    }
}