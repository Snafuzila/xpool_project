package il.ac.hit.xpool;

public interface Task {
    public abstract void perform();

    public abstract void setPriority(int level);

    public abstract int getPriority();
}
