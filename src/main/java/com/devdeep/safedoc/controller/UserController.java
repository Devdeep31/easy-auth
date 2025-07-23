package com.devdeep.safedoc.controller;

import com.devdeep.safedoc.entity.User;
import com.devdeep.safedoc.service.UserServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private UserServiceImpl userService;

    UserController(UserServiceImpl userService){
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody User user){
        return ResponseEntity.ok(this.userService.createUser(user));
    }
}
