package com.devdeep.safedoc.service;

import com.devdeep.safedoc.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User createUser(User user);

   List<User> getAllUsers();
}
