package loderunner;

import javax.swing.JFrame;

import loderunner.model.Plateau;
import loderunner.ui.GamePanel;
import loderunner.utils.LevelLoader;

public class Main {
    public static void main(String[] args) {
        Plateau p = LevelLoader.loadMap("loderunner/src/main/ressources/level/level1.txt");
        System.out.println("Taille du plateau : " + p.getLargeur() + "x" + p.getHauteur());
        // Vérifie si une case spécifique est bien un MUR
        System.out.println("Case (0,0) : " + p.getCase(0, 0));
        GamePanel ui = new GamePanel(p);

        JFrame frame = new JFrame("Lode Runner");
        frame.add(ui);
        frame.pack();

        frame.setLocationRelativeTo(null); // Centre la fenêtre
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Boucle de rendu de test
        while (true) {
            ui.incrementerTick(); // On fait avancer l'animation
            ui.repaint(); // On demande le dessin

            try {
                Thread.sleep(16); // Pause de 16ms (~60 FPS)
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
