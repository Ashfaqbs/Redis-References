package com.example.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Note;

public interface NoteRepo extends JpaRepository<Note, Long> {
    
}
