package com.example.proiectpao.service.UserService;

import com.example.proiectpao.collection.User;
import com.example.proiectpao.dtos.AssignRoleDTO;
import com.example.proiectpao.dtos.UserDTO;
import com.example.proiectpao.dtos.UserLoginDTO;
import com.example.proiectpao.dtos.UserRegisterDTO;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService {
    CompletableFuture<User> register(UserRegisterDTO userRegisterDTO);

    CompletableFuture<UserDTO> login(UserLoginDTO userLoginDTO) throws IOException;

    CompletableFuture<UserDTO> displayUser(String username);

    CompletableFuture<UserDTO> assignRole(AssignRoleDTO userRoleDTO);

    CompletableFuture<Resource> downloadUser(String username) throws IOException;

    public CompletableFuture<Boolean> uploadStats(String user, MultipartFile file);
}
