package com.example.redis_crud_sb3.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.redis_crud_sb3.entity.User;
import com.example.redis_crud_sb3.repo.UserRepository;

@RestController
public class ObjectController {
    @Autowired
    private UserRepository repository;

    @PostMapping("/objects")
    public User createUser(@RequestBody User user) {
        return repository.save(user);
    }

    @GetMapping("/objects/{id}")
    public User getUser(@PathVariable("id") String id) {
        Optional<User> optionalUser = repository.findById(id);
        return optionalUser.orElse(null);
    }

    @GetMapping("/objects")
    public Iterable<User> getUsers() {
        return repository.findAll();
    }




    @PutMapping("/objects/{id}")
    public User updateUser(@PathVariable("id") String id, @RequestBody User user) {
        user.setId(id);
        return repository.save(user);
    }

    @DeleteMapping("/objects/{id}")
    public void deleteUser(@PathVariable("id") String id) {
        repository.deleteById(id);
    }
}