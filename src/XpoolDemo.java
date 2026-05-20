import il.ac.hit.xpool.*;

public class XpoolDemo {

    public static void main(String[] args) {
 
        ThreadsPool pool = new ThreadsPool(4);
 
        Task a = new SimpleTask(2,"Hello");
 
        Task b = new SimpleTask(7,"Good Morning");
 
        Task c = new SimpleTask(2,"Good Afternoon");
 
        Task d = new SimpleTask(12, "Good Evening");
 
        pool.submit(a);
 
        pool.submit(b);
 
        pool.submit(c);
 
        pool.submit(d);
 
 
        try {
 
            Thread.sleep(1000);
 
        } catch (InterruptedException e) {
 
            throw new RuntimeException(e);
 
        }
 
 
        Task e = new SimpleTask(12, "Good Night");
 
        Task f = new SimpleTask(4, "Good Day");
 
        Task g = new SimpleTask(1, "Hello Everyone");
 
        Task h = new SimpleTask(8, "Good Luck");
 
        Task i = new SimpleTask(2, "Bonjourno");
 
        Task j = new SimpleTask(8, "Bonjour");
 
 
        pool.submit(e);
 
        pool.submit(f);
 
        pool.submit(g);
 
        pool.submit(h);
 
        pool.submit(i);
 
        pool.submit(j);
 
 
        /*
 
        We can expect the following:
 
        1. after the first four tasks and their output, we can expect the output
 
        of the next four tasks (e,j,h, and f) - not necessary in this order!
 
        2. after getting the output of tasks e,j,h, and f, we can expect to get the output of
 
        tasks g and i - not necessary in this order!
 
         */
 
 
    }
 
 } 
