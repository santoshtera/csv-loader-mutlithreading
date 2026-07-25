package com.example.csvdemo.basics;

import java.util.List;
import java.util.concurrent.*;

public class CompletableFutureDemo {

    public static void main(String args[]) throws InterruptedException, ExecutionException {

        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 9;
        });
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 8);

        CompletableFuture<Integer> sum  = a.thenCombine(b,(x,y) -> x+y);
        System.out.println("Sum  " + sum.get());


        List<CompletableFuture<Integer>> futures = List.of(
                 CompletableFuture.supplyAsync(()-> {
                     try {
                         Thread.sleep(5000);
                     } catch (InterruptedException e) {
                         throw new RuntimeException(e);
                     }
                     return 5;
                 })
                ,CompletableFuture.supplyAsync(()->5)
                ,CompletableFuture.supplyAsync(()->8));




        int sumOfAll = futures.stream().mapToInt(CompletableFuture::join).sum();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        System.out.println("Sum of All Futures  " + sum.get());


       // CompletableFuture<Void> future2 = CompletableFuture.runAsync(()-> System.out.println("Running future2"));

       // future2.get();


    }

}

