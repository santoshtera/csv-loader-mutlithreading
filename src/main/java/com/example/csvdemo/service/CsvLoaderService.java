package com.example.csvdemo.service;

import com.example.csvdemo.model.Student;
import com.example.csvdemo.store.StudentStore;
import com.example.csvdemo.util.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class CsvLoaderService {

    private static final Logger log = LoggerFactory.getLogger(CsvLoaderService.class);
    private static final String CSV_FILE = "student_data.csv";

    private final StudentStore studentStore;
    private final ExecutorService executorService;
    private final ExecutorService virtualThreadExecutor;

    public CsvLoaderService(StudentStore studentStore
            , @Qualifier("fixedPoolExecutor") ExecutorService executorService
            , @Qualifier("virtualThreadExecutor") ExecutorService virtualExecutorService) {
        this.studentStore = studentStore;
        this.executorService = executorService;
        this.virtualThreadExecutor =  virtualExecutorService;
    }

    public LoadResult loadFromCsv() {
        studentStore.clear();
        long start = System.nanoTime();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(CSV_FILE).getInputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine();
            int rowsLoaded = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Student student = parseLine(line);
                studentStore.save(student);
                rowsLoaded++;
            }

            long tookMillis = (System.nanoTime() - start) / 1_000_000;
            log.info("Loaded {} students on thread [{}] in {} ms",
                    rowsLoaded, Thread.currentThread().getName(), tookMillis);
            return new LoadResult(rowsLoaded, tookMillis, Thread.currentThread().getName());

        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + CSV_FILE, e);
        }
    }

    private List<String> readAllDataLines() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(CSV_FILE).getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().collect(Collectors.toList());
            return lines.subList(1, lines.size()); // drop header
        }
    }

    private List<List<String>> partition(List<String> lines, int numChunks) {
        int chunkSize = (int) Math.ceil((double) lines.size() / numChunks);
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += chunkSize) {
            chunks.add(lines.subList(i, Math.min(i + chunkSize, lines.size())));
        }
        return chunks;
    }


    public LoadResult loadFromCsvParallel() throws IOException, ExecutionException, InterruptedException {
        studentStore.clear();
        long start = System.nanoTime();

        List<String> dataLines = readAllDataLines();
        long readDoneAt = System.nanoTime();
        long readMillis = (readDoneAt - start) / 1_000_000;

        List<List<String>> chunks = partition(dataLines, 4);

        List<Future<Integer>> futures = new ArrayList<>();
        for (List<String> chunk : chunks) {
            futures.add(executorService.submit(() -> {
                int count = 0;
                for (String line : chunk) {
                    studentStore.save(parseLine(line));
                    count++;
                }
                log.info("Chunk of {} rows loaded on thread [{}]", chunk.size(), Thread.currentThread().getName());
                return count;
            }));
        }

        int total = 0;
        for (Future<Integer> f : futures) {
            total += f.get(); // blocks until this chunk's task is done — same role as join() in 1d
        }

        long tookMillis = (System.nanoTime() - start) / 1_000_000;
        long submitAndProcessMillis = tookMillis - readMillis;
        log.info("Parallel load: {} students in {} ms total (read: {} ms, submit+process+wait: {} ms)",
                total, tookMillis, readMillis, submitAndProcessMillis);
        return new LoadResult(total, tookMillis, "multiple-threads");
    }



    private Student parseLine(String line) {
        String[] fields = line.split(",");
        return new Student(
                Long.parseLong(fields[0].trim()),
                fields[1].trim(),
                Integer.parseInt(fields[2].trim()),
                fields[3].trim(),
                Integer.parseInt(fields[4].trim())
        );
    }

    public record LoadResult(int rowsLoaded, long tookMillis, String threadName) {
    }


    public CompletableFuture<LoadResult> loadFromCsvParallelAsync() throws IOException {
        studentStore.clear();
        long start = System.nanoTime();

        List<String> dataLines = readAllDataLines();
        List<List<String>> chunks = partition(dataLines, 100);

        List<CompletableFuture<Integer>> chunkFutures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> {
                    int count = 0;
                    for (String line : chunk) {
                        studentStore.save(parseLine(line));
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        count++;
                    }
                    log.info("Chunk of {} rows loaded on thread [{}]", chunk.size(), Thread.currentThread().getName());
                    return count;
                }, executorService))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int total = chunkFutures.stream().mapToInt(CompletableFuture::join).sum();
                    long tookMillis = (System.nanoTime() - start) / 1_000_000;
                    log.info("Async parallel load: {} students in {} ms", total, tookMillis);
                    return new LoadResult(total, tookMillis, "multiple-threads-async");
                });

    }



    public CompletableFuture<LoadResult> loadFromCsvVirtual() throws IOException {

        studentStore.clear();
        long start = System.nanoTime();

        List<String> dataLines = readAllDataLines();
        List<List<String>> chunks = partition(dataLines, 4);

        List<CompletableFuture<Integer>> chunkFutures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> {
                    int count = 0;
                    for (String line : chunk) {
                        studentStore.save(parseLine(line));
                        try {
                            Thread.sleep(0);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        count++;
                    }
                    log.info("Chunk of {} rows loaded on thread [{}]", chunk.size(), Thread.currentThread().getName());
                    return count;
                }, virtualThreadExecutor))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    int total = chunkFutures.stream().mapToInt(CompletableFuture::join).sum();
                    long tookMillis = (System.nanoTime() - start) / 1_000_000;
                    log.info("Async parallel load: {} students in {} ms", total, tookMillis);
                    return new LoadResult(total, tookMillis, "multiple-threads-async");
                });

    }}

