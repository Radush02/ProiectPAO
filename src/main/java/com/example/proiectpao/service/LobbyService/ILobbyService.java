package com.example.proiectpao.service.LobbyService;

import com.example.proiectpao.collection.Lobby;
import com.example.proiectpao.dtos.lobbyDTOs.CreateLobbyDTO;
import com.example.proiectpao.dtos.lobbyDTOs.JoinLobbyDTO;
import com.example.proiectpao.dtos.lobbyDTOs.KickLobbyDTO;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;

public interface ILobbyService {

    @Async
    CompletableFuture<Lobby> createLobby(CreateLobbyDTO lobbyDTO);

    @Async
    CompletableFuture<Lobby> joinLobby(JoinLobbyDTO lobbyDTO);

    @Async
    CompletableFuture<Lobby> kickFromLobby(KickLobbyDTO lobbyDTO);
}