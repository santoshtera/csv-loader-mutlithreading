package com.example.csvdemo.controller;

import com.example.csvdemo.model.Student;
import com.example.csvdemo.service.CsvLoaderService;
import com.example.csvdemo.store.StudentStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final CsvLoaderService csvLoaderService;
    private final StudentStore studentStore;

    public StudentController(CsvLoaderService csvLoaderService, StudentStore studentStore) {
        this.csvLoaderService = csvLoaderService;
        this.studentStore = studentStore;
    }

    @PostMapping("/load")
    public CsvLoaderService.LoadResult load() {
        return csvLoaderService.loadFromCsv();
    }

    @PostMapping("/load-parallel")
    public CsvLoaderService.LoadResult loadParallel() throws Exception {
        return csvLoaderService.loadFromCsvParallel();
    }

    @GetMapping
    public Collection<Student> listAll() {
        return studentStore.findAll();
    }

    @GetMapping("/count")
    public int count() {
        return studentStore.count();
    }


    @PostMapping("/load-parallel-async")
    public CompletableFuture<CsvLoaderService.LoadResult> loadParallelAsync() throws IOException {
        System.out.println("Printing Main Thread " + Thread.currentThread().getName());
        return csvLoaderService.loadFromCsvParallelAsync();
    }


    @PostMapping("/load-parallel-async-virtual")
    public CompletableFuture<CsvLoaderService.LoadResult> loadParallelAsyncVirtual() throws IOException {
        System.out.println("Printing Main Thread " + Thread.currentThread().getName());
        return csvLoaderService.loadFromCsvVirtual();
    }
}
