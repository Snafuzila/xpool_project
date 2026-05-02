import il.ac.hit.xpool.Task;

public class SimpleTask implements Task {
    private int priority;
    private String message;

    public SimpleTask(int priority, String message) {
        this.setPriority(priority);
        this.setMessage(message);
    }

    @Override
    public void perform() {
        try {
            Thread.sleep(500);
            System.out.println(this.getPriority() + " " + this.getMessage());
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setPriority(int level) {
        this.priority = level;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}