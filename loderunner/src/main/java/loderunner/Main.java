package loderunner;

import javax.swing.JFrame;
import loderunner.game.Moteur;
import loderunner.model.Plateau;
import loderunner.ui.GamePanel;
import loderunner.ui.InputHandler;
import loderunner.utils.LevelLoader;

public class Main {
    public static void main(String[] args) {
        Plateau plateau = LevelLoader.loadMap("loderunner/src/main/ressources/level/level1.txt");
        InputHandler ih = new InputHandler();
        GamePanel panel = new GamePanel(plateau);
        panel.addKeyListener(ih);
        panel.setFocusable(true);
        panel.requestFocusInWindow();
        JFrame frame = new JFrame("Lode Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        Moteur moteur = new Moteur(plateau, panel, ih);
        Thread threadMoteur = new Thread(moteur);
        threadMoteur.start();
    }
}

