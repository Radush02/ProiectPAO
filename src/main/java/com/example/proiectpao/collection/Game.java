package com.example.proiectpao.collection;

import com.example.proiectpao.enums.Results;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
@Data
@Builder
@Document(collection = "games")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Game {
    private String userId;
    private String opponentId;
    private Results result;
    private String score;
    private Date date;

}
