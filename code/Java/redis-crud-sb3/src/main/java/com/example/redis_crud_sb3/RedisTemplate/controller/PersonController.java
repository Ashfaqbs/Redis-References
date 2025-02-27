package com.example.redis_crud_sb3.RedisTemplate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.redis_crud_sb3.RedisTemplate.entity.Person;
import com.example.redis_crud_sb3.RedisTemplate.service.PersonService;

// @RestController
// @RequestMapping("/person")
// public class PersonController {

//     @Autowired
//     private PersonService personService;

//     @PostMapping
//     public void savePerson(@RequestBody Person person) {
//         personService.savePerson(person);
//     }

//     @GetMapping("/{id}")
//     public Person getPerson(@PathVariable String id) {
//         return personService.getPerson(id);
//     }

//     @PutMapping
//     public void updatePerson(@RequestBody Person person) {
//         personService.updatePerson(person);
//     }

//     @DeleteMapping("/{id}")
//     public void deletePerson(@PathVariable String id) {
//         personService.deletePerson(id);
//     }
// }



@RestController
@RequestMapping("/person")
public class PersonController {

    @Autowired
    private PersonService personService;

    @PostMapping
    public void savePerson(@RequestBody Person person) {
        personService.savePerson(person);
    }

    @GetMapping("/{id}")
    public Person getPerson(@PathVariable String id) {
        return personService.getPerson(id);
    }

    @PutMapping
    public void updatePerson(@RequestBody Person person) {
        personService.updatePerson(person);
    }

    @DeleteMapping("/{id}")
    public void deletePerson(@PathVariable String id) {
        personService.deletePerson(id);
    }
}