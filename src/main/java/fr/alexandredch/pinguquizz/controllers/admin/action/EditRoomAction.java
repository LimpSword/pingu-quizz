package fr.alexandredch.pinguquizz.controllers.admin.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EditRoomAction {

    private String code;
    private Action action;
    private String body;

    public enum Action {
        START,
        PAUSE,
        RESUME,
        CHANGE_QUIZZ
    }
}
