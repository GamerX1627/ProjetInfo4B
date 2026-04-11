package loderunner;

import javax.swing.JFrame;
import loderunner.game.GestionnaireNiveaux;
import loderunner.ui.InputHandler;

public class Main {
    public static void main(String[] args) {
        InputHandler ih = new InputHandler();
        JFrame frame = new JFrame("Lode Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GestionnaireNiveaux gestionnaire = new GestionnaireNiveaux(frame, ih);
        gestionnaire.demarrer();
    }
}
