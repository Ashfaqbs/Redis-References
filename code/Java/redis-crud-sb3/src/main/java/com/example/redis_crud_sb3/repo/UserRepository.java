package com.example.redis_crud_sb3.repo;


import org.springframework.data.repository.CrudRepository;

import com.example.redis_crud_sb3.entity.User;

public interface UserRepository extends CrudRepository<User, String> {
}