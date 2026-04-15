package loderunner.game;

import java.util.LinkedList;
import java.util.Queue;

import loderunner.model.Case;
import loderunner.model.Entite;
import loderunner.model.Garde;
import loderunner.model.Plateau;
import loderunner.utils.Direction;


public class IA {

    private Plateau plateau;

    public IA(Plateau plateau) {
        this.plateau = plateau;
    }

    public Direction calculerMouvement(Garde garde, Entite cible) {
        if (garde.estBloque()) {
            return Direction.AUCUNE;
        }

        int startX  = garde.getX();
        int startY  = garde.getY();
        int targetX = cible.getX();
        int targetY = cible.getY();

        if (startX == targetX && startY == targetY) {
            return Direction.AUCUNE;
        }

        return bfs(startX, startY, targetX, targetY);
    }

    private Direction bfs(int startX, int startY, int targetX, int targetY) {
        int largeur = plateau.getLargeur();
        int hauteur = plateau.getHauteur();

        boolean[][]   visite    = new boolean[largeur][hauteur];
        Direction[][] premierPas = new Direction[largeur][hauteur];

        Queue<int[]> file = new LinkedList<>();
        file.add(new int[]{startX, startY});
        visite[startX][startY] = true;

        Direction[] directions = {Direction.GAUCHE, Direction.DROITE, Direction.HAUT, Direction.BAS};
        int[]       dx         = {-1, 1, 0, 0};
        int[]       dy         = {0, 0, -1, 1};

        while (!file.isEmpty()) {
            int[] pos = file.poll();
            int x = pos[0];
            int y = pos[1];

            for (int i = 0; i < directions.length; i++) {
                Direction dir = directions[i];

                if (!peutSeDeplacer(x, y, dir)) {
                    continue;
                }

                int nx = x + dx[i];
                int ny = y + dy[i];

                if (visite[nx][ny]) {
                    continue;
                }

                visite[nx][ny] = true;
                // on mémorise le premier pas depuis le départ, c'est ça qui nous intéresse au final
                premierPas[nx][ny] = (x == startX && y == startY) ? dir : premierPas[x][y];

                if (nx == targetX && ny == targetY) {
                    return premierPas[nx][ny];
                }

                file.add(new int[]{nx, ny});
            }
        }

        return Direction.AUCUNE;
    }

    private boolean peutSeDeplacer(int x, int y, Direction dir) {
        int nx = x;
        int ny = y;

        switch (dir) {
            case GAUCHE: nx--; break;
            case DROITE: nx++; break;
            case HAUT:   ny--; break;
            case BAS:    ny++; break;
            default: return false;
        }

        if (!plateau.estdansLePlateau(nx, ny)) {
            return false;
        }

        Case caseActuelle = plateau.getCase(x, y);
        Case caseCible    = plateau.getCase(nx, ny);

        if (caseCible == Case.MUR) {
            return false;
        }

        // on peut monter seulement si on est sur une échelle ou que la case du dessus en est une
        if (dir == Direction.HAUT) {
            return caseActuelle == Case.ECHELLE || caseCible == Case.ECHELLE;
        }

        // pour se déplacer horizontalement il faut avoir un sol sous les pieds
        if (dir == Direction.GAUCHE || dir == Direction.DROITE) {
            return estSupporte(x, y);
        }

        return true;
    }

    private boolean estSupporte(int x, int y) {
        Case caseActuelle = plateau.getCase(x, y);
        if (caseActuelle == Case.ECHELLE || caseActuelle == Case.PASSERELLE) {
            return true;
        }
        int yBas = y + 1;
        if (!plateau.estdansLePlateau(x, yBas)) {
            return true; // bord inférieur du plateau, on considère que c'est solide
        }
        Case caseBas = plateau.getCase(x, yBas);
        return caseBas == Case.MUR || caseBas == Case.ECHELLE || caseBas == Case.TROU;
    }
}
