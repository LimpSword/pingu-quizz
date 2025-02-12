package fr.alexandredch.pinguquizz.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean correct;
    private String answer;
    private String image;
    private String originalImageName;

    public static Answer minimal(Answer answer) {
        Answer minimal = new Answer();
        minimal.setCorrect(false);
        minimal.setAnswer(answer.getAnswer());
        minimal.setImage(answer.getImage());
        return minimal;
    }
}
