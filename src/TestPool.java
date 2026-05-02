import il.ac.hit.xpool.*;

public class TestPool {
    public static void main(String[] args) {

        ThreadsPool pool = new ThreadsPool(2);

        Task a = new SimpleTask(2, "Hello");
        Task b = new SimpleTask(7, "Good Morning");
        Task c = new SimpleTask(2, "Good Afternoon");
        Task d = new SimpleTask(12, "Good Evening");
        pool.submit(a);
        pool.submit(b);
        pool.submit(c);
        pool.submit(d);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Task e = new SimpleTask(1, "Good Night");
        Task f = new SimpleTask(3, "Good Day");
        Task g = new SimpleTask(1, "Hello Everyone");
        Task h = new SimpleTask(4, "Good Luck");
        Task i = new SimpleTask(10, "Bonjourno");
        Task j = new SimpleTask(8, "Bonjour");
        pool.submit(e);
        pool.submit(f);
        pool.submit(g);
        pool.submit(h);
        pool.submit(i);
        pool.submit(j);
    }
}