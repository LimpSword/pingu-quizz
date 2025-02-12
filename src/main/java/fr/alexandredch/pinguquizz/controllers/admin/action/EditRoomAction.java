package fr.alexandredch.pinguquizz.controllers.admin.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EditRoomAction {

    private String code;
    private Action action;

    public enum Action {
        START,
        PAUSE,
        RESUME
    }
}
