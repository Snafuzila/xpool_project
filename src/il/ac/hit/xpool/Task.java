package il.ac.hit.xpool;

public interface Task {
    public abstract void perform();
    public abstract void setPriority(int level);
    public abstract int getPriority();
}

/*
Technical Note on Dynamic Priority:
"The library uses a PriorityQueue for efficient task management (O(log n)).
As per Java's standard behavior, changing a task's priority while it is inside the queue does not trigger a re-sort.
 To handle dynamic changes, I have provided the changePriority(Task, int) method in the ThreadsPool class -
 which ensures the internal heap is correctly updated."
 */
