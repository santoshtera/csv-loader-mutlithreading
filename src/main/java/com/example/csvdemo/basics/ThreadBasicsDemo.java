package com.example.csvdemo.basics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ThreadBasicsDemo {

    public static void main(String args[]) throws InterruptedException, ExecutionException {

        System.out.println(Runtime.getRuntime().availableProcessors());

        // Creating a thread by extending Thread
        HelloThread helloThread = new HelloThread();
        helloThread.run();
        helloThread.start();

        /**
         * One gotcha to watch for: if you call t.start() twice on the same Thread object,
         * you'll get an IllegalThreadStateException — a Thread instance can only be started once.
         * Calling run() directly has no such restriction since it's not touching thread state at all,
         * just executing code synchronously.
         */


    Runnable task = () -> System.out.println("Creating the thread by implementing Runnable interface " +
            Thread.currentThread().getName());

        Thread t2 = new Thread(task,"worker -1");
        t2.start();
        System.out.println(t2.getState());




        for (int i = 0; i < 5; i++) {

            int finalI = i;
            Runnable task2 = () -> {
                System.out.println("Task " + finalI + " start on " + Thread.currentThread().getName());

                try {
                    Thread.sleep((long) (Math.random() * 500));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };

            new Thread(task2, "worker-" + finalI).start();
            }

    int[] counter =  {0};
        List<Thread> threadList =  new ArrayList<>();
        ExecutorService executors1 = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 1000; i++) {
            int id = i;
            Thread t = new Thread(() -> {System.out.println("Task ----" + id + " stat on "
                    + Thread.currentThread().getName() + "with Counter " + counter[0]++ );});
            executors1.submit(t);
            threadList.add(t);
        }

        for (Thread t : threadList) {
            t.join();
        }
        executors1.shutdown();
        System.out.println("Expected 1000, got: " + counter[0]);



        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<Integer>> futures = new ArrayList<>();
        for(int i=0;i<10; i++){

            int id =i;
            Callable<Integer> taskCall = () -> {
                Thread.sleep(200);
                System.out.println("Result" + Thread.currentThread().getName());
                return id*2;
            }  ;
            futures.add(executor.submit(taskCall));
        }

        for (Future<Integer> f: futures) {
            System.out.println("Result" + f.get());
        }

        executor.shutdown();




    }

}

class HelloThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread Class Running on: " + Thread.currentThread().getName());
    }
}