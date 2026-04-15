package loderunner.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import loderunner.utils.Direction;

public class InputHandler implements KeyListener {
    private Direction directionCourante = Direction.AUCUNE;
    private boolean creuser = false;

    public Direction getDirection() {
        return directionCourante;
    }

    // on retourne true une seule fois par appui sur espace, pour éviter de creuser en continu
    public boolean consommerCreuser() {
        if (creuser) {
            creuser = false;
            return true;
        }
        return false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:     directionCourante = Direction.HAUT;   break;
            case KeyEvent.VK_DOWN:   directionCourante = Direction.BAS;    break;
            case KeyEvent.VK_LEFT:   directionCourante = Direction.GAUCHE; break;
            case KeyEvent.VK_RIGHT:  directionCourante = Direction.DROITE; break;
            case KeyEvent.VK_SPACE:  creuser = true;                       break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_RIGHT:
                directionCourante = Direction.AUCUNE;
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}