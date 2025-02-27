package com.example.redis_crud_sb3.RedisTemplate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.redis_crud_sb3.RedisTemplate.entity.Person;

// @Service
// public class PersonService {

//     @Autowired
//     private RedisTemplate<String, Object> redisTemplate;

//     private static final String KEY = "Person";

//     public void savePerson(Person person) {
//         redisTemplate.opsForValue().set(KEY + ":" + person.getId(), person);
//     }

//     public Person getPerson(String id) {
//         return (Person) redisTemplate.opsForValue().get(KEY + ":" + id);
//     }

//     public void updatePerson(Person person) {
//         savePerson(person);
//     }

//     public void deletePerson(String id) {
//         redisTemplate.delete(KEY + ":" + id);
//     }
// }



@Service
public class PersonService {

    private final HashOperations<String, String, Person> hashOperations;
    private static final String KEY = "Person";

    @Autowired
    public PersonService(RedisTemplate<String, Object> redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
    }

    public void savePerson(Person person) {
        hashOperations.put(KEY, person.getId(), person);
    }

    public Person getPerson(String id) {
        return hashOperations.get(KEY, id);
    }

    public void updatePerson(Person person) {
        savePerson(person);
    }

    public void deletePerson(String id) {
        hashOperations.delete(KEY, id);
    }
}