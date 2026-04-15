package loderunner.game;
// gère tout ce qui est physique : est-ce qu'on peut se déplacer, est-ce qu'on tombe, est-ce qu'on peut creuser

import loderunner.model.Case;
import loderunner.model.Entite;
import loderunner.model.Garde;
import loderunner.model.Joueur;
import loderunner.model.Plateau;
import loderunner.utils.Direction;

public class Physique {
    private Plateau plateau;
    private final int vitesseChute = 2;
    private final int vitesseDeplacement = 1;

    public Physique(Plateau P) {
        setPlateau(P);
    }

    // getters classiques
    public int getVitesseChute() {
        return this.vitesseChute;
    }

    public int getVitesseDeplacement() {
        return this.vitesseDeplacement;
    }

    public Plateau getPlateau() {
        return this.plateau;
    }

    // setter — on peut changer le plateau (utile quand on change de niveau)
    public void setPlateau(Plateau p) {
        this.plateau = p;
    }

    // vérifie si une entité peut se déplacer dans une direction donnée
    // on prend en compte les murs, les échelles, les trous occupés, etc.
    public boolean peutSeDeplacer(Entite e, Direction D) {
        // on calcule la position cible selon la direction
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
        // si la case cible est hors du plateau, on peut pas y aller
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

        // deux entités ne peuvent pas être dans le même trou
        if (caseCible == Case.TROU && this.plateau.getEntiteAt(x, y) != null) {
            return false;
        }

        // les gardes ne peuvent pas se superposer entre eux
        if (e instanceof Garde) {
            for (Garde g : this.plateau.getGardes()) {
                if (g != e && g.getX() == x && g.getY() == y) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean doitTomber(Entite e) {
        int x = e.getX();
        int y = e.getY();

        Case caseActuelle = this.plateau.getCase(x, y);

        if (caseActuelle == Case.ECHELLE || caseActuelle == Case.PASSERELLE) {
            return false;
        }

        if (caseActuelle == Case.TROU && e instanceof Garde) {
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

        if (caseEnDessous == Case.TROU && plateau.getEntiteAt(x, ySousEntite) != null) {
            return false;
        }

        return true;
    }
    public boolean peutCreuser(int xCible, int yCible) {
        if (!plateau.estdansLePlateau(xCible, yCible)) return false;
        if (xCible <= 0 || xCible >= plateau.getLargeur() - 1) return false;
        if (yCible <= 0) return false;
        return plateau.getCase(xCible, yCible) == Case.MUR;
    }
}
