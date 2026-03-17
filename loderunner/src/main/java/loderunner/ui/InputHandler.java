package loderunner.ui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import loderunner.utils.Direction;

public class InputHandler implements KeyListener {
    private Direction directionCourante = Direction.AUCUNE;

    public Direction getDirection() {
        return directionCourante;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:    directionCourante = Direction.HAUT; break;
            case KeyEvent.VK_DOWN:  directionCourante = Direction.BAS; break;
            case KeyEvent.VK_LEFT:  directionCourante = Direction.GAUCHE; break;
            case KeyEvent.VK_RIGHT: directionCourante = Direction.DROITE; break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // lorsque la touche est relâchée, on ramène la direction à aucune.
        directionCourante = Direction.AUCUNE;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }
}