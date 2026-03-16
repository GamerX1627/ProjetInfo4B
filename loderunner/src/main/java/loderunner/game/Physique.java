package loderunner.game;
// Classe qui applique les lois de la physique aux joueurs et au PNJ (perso non Joueur)

import loderunner.model.Case;
import loderunner.model.Entite;
import loderunner.model.Plateau;
import loderunner.utils.Direction;

public class Physique {
    private Plateau plateau;
    private final int vitesseChute = 2;
    private final int vitesseDeplacement = 1;

    public Physique(Plateau P) {
        setPlateau(P);
    }

    // Accesseurs en lecture
    public int getVitesseChute() {
        return this.vitesseChute;
    }

    public int getVitesseDeplacement() {
        return this.vitesseDeplacement;
    }

    public Plateau getPlateau() {
        return this.plateau;
    }

    // Accesseurs en Ecriture ( dans notre cas, on ne pourra modifer que le plateau,
    // rien d'autre)
    public void setPlateau(Plateau p) {
        this.plateau = p;
    }

    // Prédiction de collision (si y'a le vide , si y'a un mur dans la direction
    // souhaitée , si il y a un trou afin de savoir ce qu'il faut faire)
    public boolean peutSeDeplacer(Entite e, Direction D) {
        // récupération des différentes coordonnées afin de savoir si le joueur peut se
        // déplacer dans ses cases
        int x = e.getX();
        int y = e.getY();

        switch (D) {
            case HAUT:
                y--;
                break;
            case BAS:
                y++;
                break;
            case GAUCHE:
                x--;
                break;
            case DROITE:
                x++;
                break;
            case AUCUNE:
                return true;
        }
        // maintennt, vérification des positions(les positions actuelles et les
        // prochaines positions) si elles sont valides ou pas
        if (!this.plateau.estPositionValide(x, y)) {
            return false;
        }

        Case caseCible = this.plateau.getCase(x, y);
        Case caseActuelle = this.plateau.getCase(e.getX(), e.getY());

        if (D == Direction.HAUT) {
            return (caseActuelle == Case.ECHELLE || caseCible == Case.ECHELLE);
        }

        if (caseCible == Case.SORTIE) {
            return this.plateau.tousLesLingotsRecoltes();
        }

        return true;
    }

    public boolean doitTomber(Entite e) {
        int x = e.getX();
        int y = e.getY();

        Case caseActuelle = this.plateau.getCase(x, y);

        if (caseActuelle == Case.ECHELLE) {
            return false;
        }

        int ySousEntite = y + 1;

        if (!this.plateau.estdansLePlateau(x, ySousEntite)) {
            return false;
        }

        Case caseEnDessous = this.plateau.getCase(x, ySousEntite);

        if (caseEnDessous == Case.MUR || caseEnDessous == Case.ECHELLE) {
            return false;
        }

        return true;
    }
}
