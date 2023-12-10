package com.wong.grpc.service;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRatingStoreTest {

    @Test
    void add() throws InterruptedException {

        InMemoryRatingStore inMemoryRatingStore = new InMemoryRatingStore();
        List<Callable<Rating>> tasks = new LinkedList<>();
        String laptopID = UUID.randomUUID().toString();
        double score = 5;
        int n = 10;
        for (int i =0; i < n; i++){
            tasks.add(()-> inMemoryRatingStore.Add(laptopID, score));
        }

        Set<Integer> ratedCount = new HashSet<>();
        Executors.newWorkStealingPool().invokeAll(tasks)
                .forEach(future ->{
                    try {
                        Rating rating = future.get();
                        assertEquals(rating.getSum(), rating.getCount() * score, 1e-9);
                        ratedCount.add(rating.getCount());
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
        assertEquals(n, ratedCount.size());
        for(int cnt = 1; cnt <= n; cnt++){
            assertTrue(ratedCount.contains(cnt));
        }
    }
}