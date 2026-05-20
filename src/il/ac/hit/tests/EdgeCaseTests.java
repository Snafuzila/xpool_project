package il.ac.hit.tests;

import il.ac.hit.xpool.Task;
import il.ac.hit.xpool.ThreadsPool;

/**
 * Assert-style checks for surprising failures: invalid usage, pool degradation,
 * heap misuse, and burst load. Run: {@code java -cp out il.ac.hit.tests.EdgeCaseTests}
 */
public final class EdgeCaseTests {

    private static final Object STAT_LOCK = new Object();
    private static int completed;

    public static void main(String[] args) throws InterruptedException {
        int failed = 0;
        failed += run("constructor rejects 0 threads", EdgeCaseTests::testConstructorZero);
        failed += run("constructor rejects negative threads", EdgeCaseTests::testConstructorNegative);
        failed += run("submit(null) fails fast (NPE)", EdgeCaseTests::testSubmitNull);
        failed += run("RuntimeException in perform does not kill worker", EdgeCaseTests::testRuntimeExceptionHandled);
        failed += run("Error in perform kills worker (pool may starve with size 1)", EdgeCaseTests::testErrorKillsSingleWorker);
        failed += run("Error in perform still drains with extra workers", EdgeCaseTests::testErrorWithSpareWorkers);
        failed += run("burst submit on single worker completes all", EdgeCaseTests::testBurstSingleWorker);
        failed += run("many equal priorities all finish", EdgeCaseTests::testManyTies);
        failed += run("updateTaskPriority never submitted still mutates task", EdgeCaseTests::updatePriorityWhenAbsentFromQueue);
        failed += run("same Task reference queued twice runs twice", EdgeCaseTests::testSameInstanceSubmittedTwice);
        failed += run("non-stable getPriority stresses queue (should stay consistent)", EdgeCaseTests::testVolatilePriorityCalls);

        System.out.println("====================================");
        if (failed == 0) {
            System.out.println("EdgeCaseTests: ALL CHECKS PASSED");
        } else {
            System.out.println("EdgeCaseTests: " + failed + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void run() throws Exception;
    }

    private static int run(String label, TestCase t) {
        System.out.println("--- " + label + " ---");
        try {
            t.run();
            System.out.println("PASS: " + label);
            return 0;
        } catch (Throwable e) {
            System.err.println("FAIL: " + label + " -> " + e);
            e.printStackTrace();
            return 1;
        }
    }

    private static void resetCounter() {
        synchronized (STAT_LOCK) {
            completed = 0;
        }
    }

    private static void incCompleted() {
        synchronized (STAT_LOCK) {
            completed++;
        }
    }

    private static int getCompleted() {
        synchronized (STAT_LOCK) {
            return completed;
        }
    }

    private static Task countingTask(int priority) {
        return new Task() {
            private int p = priority;

            @Override
            public void perform() {
                incCompleted();
            }

            @Override
            public void setPriority(int level) {
                this.p = level;
            }

            @Override
            public int getPriority() {
                return p;
            }
        };
    }

    private static void testConstructorZero() {
        try {
            new ThreadsPool(0);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private static void testConstructorNegative() {
        try {
            new ThreadsPool(-3);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private static void testSubmitNull() {
        ThreadsPool pool = new ThreadsPool(1);
        try {
            pool.submit(null);
            throw new AssertionError("expected NullPointerException");
        } catch (NullPointerException expected) {
            // PriorityQueue does not accept null; comparator may NPE earlier
        }
    }

    private static void testRuntimeExceptionHandled() throws InterruptedException {
        ThreadsPool pool = new ThreadsPool(2);
        resetCounter();
        pool.submit(new Task() {
            private int p = 100;

            @Override
            public void perform() {
                throw new RuntimeException("boom");
            }

            @Override
            public void setPriority(int level) {
                this.p = level;
            }

            @Override
            public int getPriority() {
                return p;
            }
        });
        pool.submit(countingTask(1));
        Thread.sleep(500);
        if (getCompleted() != 1) {
            throw new AssertionError("worker should survive; expected 1 completion, got " + getCompleted());
        }
    }

    /**
     * Workers only catch {@link Exception}, not {@link Error}: a single dying worker can stall the pool.
     */
    private static void testErrorKillsSingleWorker() throws InterruptedException {
        ThreadsPool pool = new ThreadsPool(1);
        resetCounter();
        pool.submit(new Task() {
            private int p = 10;

            @Override
            public void perform() {
                throw new AssertionError("fatal from task");
            }

            @Override
            public void setPriority(int level) {
                this.p = level;
            }

            @Override
            public int getPriority() {
                return p;
            }
        });
        pool.submit(countingTask(5));
        Thread.sleep(800);
        if (getCompleted() != 0) {
            throw new AssertionError("expected starvation after Error on single worker; got " + getCompleted());
        }
    }

    private static void testErrorWithSpareWorkers() throws InterruptedException {
        ThreadsPool pool = new ThreadsPool(3);
        resetCounter();
        pool.submit(new Task() {
            private int p = 10;

            @Override
            public void perform() {
                throw new AssertionError("fatal from one task");
            }

            @Override
            public void setPriority(int level) {
                this.p = level;
            }

            @Override
            public int getPriority() {
                return p;
            }
        });
        int tail = 16;
        for (int i = 0; i < tail; i++) {
            pool.submit(countingTask(1));
        }
        Thread.sleep(1500);
        if (getCompleted() != tail) {
            throw new AssertionError("expected all tail tasks to finish; got " + getCompleted() + " / " + tail);
        }
    }

    private static void testBurstSingleWorker() throws InterruptedException {
        ThreadsPool pool = new ThreadsPool(1);
        resetCounter();
        int n = 400;
        for (int i = 0; i < n; i++) {
            pool.submit(countingTask(5));
        }
        Thread.sleep(2500);
        if (getCompleted() != n) {
            throw new AssertionError("expected " + n + " completions, got " + getCompleted());
        }
    }

    private static void testManyTies() throws InterruptedException {
        ThreadsPool pool = new ThreadsPool(4);
        resetCounter();
        int n = 120;
        for (int i = 0; i < n; i++) {
            pool.submit(countingTask(7));
        }
        Thread.sleep(2000);
        if (getCompleted() != n) {
            throw new AssertionError("expected " + n + " completions with ties, got " + getCompleted());
        }
    }

    private static void updatePriorityWhenAbsentFromQueue() {
        ThreadsPool pool = new ThreadsPool(1);
        Task t = countingTask(3);
        pool.updateTaskPriority(t, 99);
        if (t.getPriority() != 99) {
            throw new AssertionError("priority should update even when task was never queued");
        }
    }

    /**
     * Duplicate references in the queue are allowed by {@link java.util.PriorityQueue};
     * both entries should run (may be surprising if the task closes over shared state).
     */
    private static void testSameInstanceSubmittedTwice() throws InterruptedException {
        ThreadsPool pool = new ThreadsPool(1);
        resetCounter();
        Task once = countingTask(2);
        pool.submit(once);
        pool.submit(once);
        Thread.sleep(600);
        if (getCompleted() != 2) {
            throw new AssertionError("same instance submitted twice should execute twice; got " + getCompleted());
        }
    }

    /**
     * Comparator contract expects stable ordering keys; if {@code getPriority} mutates between
     * comparisons, the internal heap may behave oddly. Current impl only compares ints at call time.
     */
    private static void testVolatilePriorityCalls() throws InterruptedException {
        ThreadsPool pool = new ThreadsPool(1);
        resetCounter();
        pool.submit(new Task() {
            private int phase;

            @Override
            public void perform() {
                incCompleted();
            }

            @Override
            public void setPriority(int level) {
                // not used
            }

            @Override
            public int getPriority() {
                // Alternate between low and high while the queue heap operates — pathological client.
                phase++;
                return (phase % 2 == 0) ? 1 : 1000;
            }
        });
        Thread.sleep(300);
        if (getCompleted() < 1) {
            throw new AssertionError("expected at least one execution");
        }
    }
}
