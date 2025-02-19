package fr.alexandredch.pinguquizz.models.room;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@ToString
public class RoomAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String answer;
    private boolean answered;
    private boolean correct;

    public static RoomAnswer byDefault() {
        RoomAnswer roomAnswer = new RoomAnswer();
        roomAnswer.setAnswered(false);
        roomAnswer.setCorrect(false);
        return roomAnswer;
    }
}
