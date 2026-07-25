# Multithreading CSV Loader — Java Concurrency Demo

A Spring Boot REST API that loads student records from a CSV file into an in-memory store, built specifically to demonstrate core Java concurrency concepts end-to-end: raw threads, thread pools, race conditions and their fixes, `CompletableFuture`, and Java 21 virtual threads — all applied to the same real workload so the tradeoffs are directly comparable.

## Why this project exists

Rather than use a framework like Spring Batch (which abstracts multithreading into declarative configuration), this project intentionally hand-rolls concurrency using core `java.util.concurrent` APIs. The goal was to *own* every thread, pool, and synchronization decision — and to measure the consequences of each one, not just implement them.

## Concepts demonstrated

- **Thread fundamentals** — `Thread` vs `Runnable`, `start()` vs `run()`, non-deterministic scheduling/interleaving, `join()`
- **Race conditions** — reproduced a real concurrent-modification bug in a shared `HashMap` under parallel writes, and diagnosed its actual cause (unsafe internal resizing, not simple lost-update)
- **Thread-safe collections & primitives** — compared `ConcurrentHashMap`, `synchronized` blocking, and `AtomicInteger`, with measured throughput tradeoffs between them
- **`ExecutorService` & thread pools** — fixed thread pools, `Callable`/`Future`, chunked parallel processing of a large CSV
- **`CompletableFuture`** — async composition with `supplyAsync`, `thenApply`, `allOf`, and exception handling (`exceptionally`/`handle`/`whenComplete`)
- **Non-blocking REST endpoints** — controller methods returning `CompletableFuture<T>` to free up server request-handling threads under concurrent load
- **Java 21 virtual threads** — implemented and compared against platform thread pools, with an emphasis on *when* virtual threads actually help (high-concurrency blocking I/O) versus when they don't (CPU-bound work)
- **Performance reasoning** — applied Amdahl's Law to explain why naive parallelization can be *slower* than sequential code at small scale, and why it pays off once the parallel portion of the work dominates

## API

| Method | Endpoint | Strategy |
|---|---|---|
| `POST` | `/api/students/load` | Sequential, single-threaded baseline |
| `POST` | `/api/students/load-parallel` | Fixed thread pool, blocking (`ExecutorService` + `Future.get()`) |
| `POST` | `/api/students/load-parallel-async` | Fixed thread pool, non-blocking (`CompletableFuture`) |
| `POST` | `/api/students/load-parallel-async-virtual` | Virtual threads, non-blocking |
| `GET` | `/api/students` | List all loaded students |
| `GET` | `/api/students/count` | Count of students currently in the store |

## Tech stack

Java 21 · Spring Boot 3.5 · Spring Web · Maven

## Architecture

```
controller/  StudentController      REST endpoints for all four load strategies
service/     CsvLoaderService       CSV parsing + each concurrency strategy implementation
store/       StudentStore           In-memory ConcurrentHashMap-backed data store
model/       Student                Record representing one CSV row
util/        ThreadPoolConfig       Executor beans: fixed thread pool + virtual thread pool
```

## Running it

```bash
./mvnw spring-boot:run
```

```bash
curl -X POST http://localhost:8080/api/students/load-parallel
curl http://localhost:8080/api/students/count
```

## What I'd highlight from this project

- Deliberately reproduced a real concurrency bug (unsafe `HashMap` under parallel writes) before fixing it, rather than jumping straight to "the right answer" — verified the fix by comparing actual stored counts, not just the API's self-reported success count
- Measured and explained a counterintuitive result (parallel code running *slower* than sequential at small data volumes) using Amdahl's Law, instead of assuming more threads always means faster
- Implemented the same workload four different ways (sequential, blocking-parallel, async-parallel, virtual-thread-parallel) to make the tradeoffs concrete and comparable rather than theoretical
