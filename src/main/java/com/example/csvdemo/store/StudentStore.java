package com.example.csvdemo.store;

import com.example.csvdemo.model.Student;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StudentStore {

    // Plain HashMap on purpose: this is our single-threaded baseline.
    // Once we introduce concurrent loading, this becomes the race-condition
    // demo before we swap it for ConcurrentHashMap.
    //private final Map<Long, Student> students = new HashMap<>();
    private final Map<Long, Student> students = new ConcurrentHashMap<>();
    public void save(Student student) {
        students.put(student.id(), student);
    }

    public Collection<Student> findAll() {
        return students.values();
    }

    public int count() {
        return students.size();
    }

    public void clear() {
        students.clear();
    }
}
