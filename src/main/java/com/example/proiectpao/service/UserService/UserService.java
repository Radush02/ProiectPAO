package com.example.proiectpao.service.UserService;

import static com.example.proiectpao.enums.Role.Admin;
import static com.example.proiectpao.enums.Role.Moderator;

import com.example.proiectpao.collection.Stats;
import com.example.proiectpao.collection.User;
import com.example.proiectpao.dtos.userDTOs.*;
import com.example.proiectpao.enums.Penalties;
import com.example.proiectpao.exceptions.AlreadyExistsException;
import com.example.proiectpao.exceptions.NonExistentException;
import com.example.proiectpao.exceptions.UnauthorizedActionException;
import com.example.proiectpao.repository.PunishRepository;
import com.example.proiectpao.repository.UserRepository;
import com.example.proiectpao.service.S3Service.S3Service;
import com.example.proiectpao.utils.FileParser.FileParser;
import com.example.proiectpao.utils.FileParser.JsonFileParser;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService implements IUserService {
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final PunishRepository punishRepository;
    private final JsonFileParser jsonFileParser;

    public UserService(
            UserRepository userRepository, S3Service s3Service, PunishRepository punishRepository) {
        this.userRepository = userRepository;
        this.s3Service = s3Service;
        this.punishRepository = punishRepository;
        this.jsonFileParser = FileParser.getInstance(JsonFileParser.class);
    }

    /**
     * O metoda ce converteste modelul user in DTO-ul specific userului.
     * @param k (userul ce trebuie convertit)
     * @return DTO-ul userului
     */
    private UserDTO configureDTO(User k) {
        UserDTO u = new UserDTO();
        u.setUserId(k.getUserId());
        u.setUsername(k.getUsername());
        u.setRole(k.getRole());
        u.setName(k.getName());
        u.setEmail(k.getEmail());
        Stats w = k.getStats();
        double wr = 0, kdr = 0, hsp = 0;
        if (w.getWins() + w.getLosses() != 0)
            wr = (double) w.getWins() / (w.getWins() + w.getLosses());
        if (k.getStats().getDeaths() != 0) kdr = (double) w.getKills() / w.getDeaths();
        if (k.getStats().getHits() != 0) hsp = (double) w.getHeadshots() / w.getKills();

        StatsDTO stats =
                StatsDTO.builder()
                        .wins(w.getWins())
                        .losses(w.getLosses())
                        .WR(wr)
                        .HSp(hsp)
                        .KDR(kdr)
                        .kills(w.getKills())
                        .deaths(w.getDeaths())
                        .hits(w.getHits())
                        .headshots(w.getHeadshots())
                        .build();
        u.setStats(stats);
        return u;
    }

    /**
     * Metoda pentru inregistrarea unui utilizator.
     * @param userRegisterDTO (DTO-ul ce contine datele necesare pentru inregistrare)
     * @return Utilizatorul inregistrat.
     */
    @Override
    @Async
    public CompletableFuture<User> register(UserRegisterDTO userRegisterDTO) {
        if (userRepository.findByUsernameIgnoreCase(userRegisterDTO.getUsername()) != null) {
            // System.out.println("aaa");
            throw new AlreadyExistsException("Exista deja un user cu acest username");
        }
        User u = new User();
        u.setUserId(UUID.randomUUID().toString().split("-")[0]);
        u.setUsername(userRegisterDTO.getUsername());
        u.setEmail(userRegisterDTO.getEmail());
        u.setName(userRegisterDTO.getName());
        u.setStats(Stats.builder().build());
        u.setGameIDs(new ArrayList<>());
        String password = userRegisterDTO.getPassword();
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        u.setSeed(encodedSalt);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (md != null) {
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            String encodedHash = Base64.getEncoder().encodeToString(hashedPassword);
            u.setHash(encodedHash);
        }
        return CompletableFuture.completedFuture(userRepository.save(u));
    }

    /**
     * Metoda login verifica daca un utilizator exista in baza de date si daca parola este corecta.
     * @param userLoginDTO (DTO-ul ce contine datele necesare pentru logare)
     * @return Utilizatorul logat.
     */
    @Override
    @Async
    public CompletableFuture<UserDTO> login(UserLoginDTO userLoginDTO) {
        User k = userRepository.findByUsernameIgnoreCase(userLoginDTO.getUsername());
        if (k == null) {
            throw new NonExistentException("Userul nu exista sau parola incorecta.");
        }
        if (!punishRepository
                .findAllByUserIDAndSanctionAndExpiryDateIsAfter(
                        k.getUserId(), Penalties.Ban, new Date())
                .isEmpty()) throw new UnauthorizedActionException("Userul este banat.");
        UserDTO u = configureDTO(k);

        String password = userLoginDTO.getPassword();
        // System.out.println(password);
        byte[] salt = Base64.getDecoder().decode(k.getSeed());
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // System.out.println(new Gson().toJson(u));
        if (md != null) {
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            String encodedHash = Base64.getEncoder().encodeToString(hashedPassword);
            if (encodedHash.equals(k.getHash())) {
                if (k.getRole() == Admin || k.getRole() == Moderator) {
                    try (FileWriter fw =
                            new FileWriter(
                                    "src/main/java/com/example/proiectpao/logs/adminlog.csv",
                                    true)) {
                        String w =
                                "\n\""
                                        + k.getUsername()
                                        + "\", "
                                        + k.getRole()
                                        + "\", Logged in @ "
                                        + java.time.LocalDateTime.now()
                                        + "\"";
                        fw.write(w);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return CompletableFuture.completedFuture(u);
            }
        }
        throw new NonExistentException("Userul nu exista sau parola incorecta.");
    }

    /**
     * Metoda displayUser afiseaza un utilizator.
     * @param username numele utilizatorului
     * @return Utilizatorul afisat.
     */
    @Override
    @Async
    public CompletableFuture<UserDTO> displayUser(String username) {
        User k = userRepository.findByUsernameIgnoreCase(username);
        if (k == null) {
            throw new NonExistentException("Userul nu exista.");
        }
        return CompletableFuture.completedFuture(configureDTO(k));
    }

    /**
     * Metoda assignRole atribuie un rol unui utilizator.
     * @param userRoleDTO (DTO-ul ce contine username-ul si rolul atribuit)
     * @return Utilizatorul cu rolul atribuit.
     */
    @Override
    @Async
    public CompletableFuture<UserDTO> assignRole(AssignRoleDTO userRoleDTO) {
        User k = userRepository.findByUsernameIgnoreCase(userRoleDTO.getUsername());
        User adm = userRepository.findById(userRoleDTO.getPossibleAdminID()).orElse(null);
        if (k == null || adm == null) {
            throw new NonExistentException("Userul nu exista.");
        }
        if (adm.getRole() != Admin) {
            throw new NonExistentException("Adminul nu exista.");
        }
        try {
            k.setRole(userRoleDTO.getRole());
        } catch (IllegalArgumentException e) {
            throw new NonExistentException("Rolul nu exista.");
        }
        userRepository.save(k);
        return CompletableFuture.completedFuture(configureDTO(k));
    }

    /**
     * Metoda downloadUser returneaza un fisier JSON cu informatiile despre un utilizator.
     * @param username numele utilizatorului
     * @return - Fisierul JSON.
     * @see <a href="https://medium.com/@mertcakmak2/object-storage-with-spring-boot-and-aws-s3-64448c91018f">Object Storage with Spring Boot and AWS S3</a>
     */
    @Override
    @Async
    public CompletableFuture<Resource> downloadUser(String username) throws IOException {
        User k = userRepository.findByUsernameIgnoreCase(username);
        if (k == null) {
            throw new NonExistentException("Userul nu exista.");
        }
        UserDTO u = configureDTO(k);
        String userJson = new Gson().toJson(u);
        String nume = jsonFileParser.write(userJson, s3Service);
        return CompletableFuture.completedFuture(
                new InputStreamResource(s3Service.getFile(nume + ".json").getObjectContent()));
    }

    /**
     * Metoda uploadStats incarca statisticile unui utilizator.
     * @param user numele utilizatorului
     * @param file fisierul JSON cu statisticile
     * @return true daca s-a incarcat cu succes, false altfel.
     */
    @Override
    @Async
    public CompletableFuture<Boolean> uploadStats(String user, MultipartFile file) {
        User k = userRepository.findByUsernameIgnoreCase(user);
        if (k == null) {
            throw new NonExistentException("Userul nu exista.");
        }
        if (file.getContentType() == null || !file.getContentType().equals("application/json")) {
            throw new NonExistentException("Fisierul nu este de tip JSON.");
        }
        if (jsonFileParser.read(k, file, s3Service)) {
            userRepository.save(k);
            return CompletableFuture.completedFuture(true);
        }
        return CompletableFuture.completedFuture(false);
    }
}
