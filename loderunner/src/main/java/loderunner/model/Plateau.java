package loderunner.model;
// le plateau contient toutes les cases du niveau ainsi que les joueurs et les gardes

import java.util.ArrayList;

public class Plateau {
    private Case[][] cases;           // grille 2D des cases, indexée par [x][y]
    private ArrayList<Joueur> joueurs; // tous les joueurs sur le plateau
    private ArrayList<Garde> gardes;   // tous les gardes sur le plateau
    private int largeur;               // nombre de colonnes
    private int hauteur;               // nombre de lignes
    private boolean partieGagnee  = false;
    private boolean partiePerdue  = false;

    public Plateau(int largeur, int hauteur) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.joueurs = new ArrayList<Joueur>();
        this.gardes = new ArrayList<Garde>();
        this.cases = new Case[largeur][hauteur];
    }
    public int getLargeur(){
        return this.largeur;
    }
    public int getHauteur(){
        return this.hauteur;
    }

    public boolean estPositionValide(int x, int y) {
        return estdansLePlateau(x, y) && estMarchable(x, y);
    }

    public Case getCase(int x, int y) {
        return cases[x][y];
    }

    public void setCase(int x, int y, Case type) {
        cases[x][y] = type;
    }

    public boolean estdansLePlateau(int x, int y) {
        return x >= 0 && x < largeur && y >= 0 && y < hauteur;
    }

    public boolean estMarchable(int x, int y) {
        Case type = getCase(x, y);
        return type != Case.MUR;
    }
    // méthodes pour ajouter des entités au plateau (appelées par le LevelLoader)

    public void ajouterJoueur(Joueur joueur) {
        this.joueurs.add(joueur);
    }

    public void ajouterGarde(Garde garde) {
        this.gardes.add(garde);
    }

    // getters pour accéder aux entités depuis les autres classes
    public ArrayList<Joueur> getJoueurs() {
        return joueurs;
    }

    public ArrayList<Garde> getGardes() {
        return gardes;
    }
    public ArrayList<Entite> getEntites(){
        ArrayList<Entite> s=new ArrayList<>();
        s.addAll(this.joueurs);
        s.addAll(this.gardes);
        return s;
    }
    public Entite getEntiteAt(int x, int y) {
        for (Joueur joueur : joueurs) {
            if (joueur.getX() == x && joueur.getY() == y) {
                return joueur;
            }
        }
        for (Garde garde : gardes) {
            if (garde.getX() == x && garde.getY() == y) {
                return garde;
            }
        }
        return null;
    }

    // remet toutes les cases à VIDE (utilisé avant de recharger un niveau)
    public void initPlateau() {
        for (int x = 0; x < largeur; x++) {
            for (int y = 0; y < hauteur; y++) {
                cases[x][y] = Case.VIDE;
            }
        }
    }

    public void viderPlateau() {
        for (int x = 0; x < largeur; x++) {
            for (int y = 0; y < hauteur; y++) {
                cases[x][y] = Case.VIDE;
            }
        }
        joueurs.clear();
        gardes.clear();
    }

    public void resetToutesEntites() {
        for (Joueur joueur : joueurs) {
            joueur.reset();
        }
        for (Garde garde : gardes) {
            garde.reset();
        }
    }
    public boolean isPartieGagnee() {
        return partieGagnee;
    }

    public void setPartieGagnee(boolean partieGagnee) {
        this.partieGagnee = partieGagnee;
    }

    public boolean isPartiePerdue() {
        return partiePerdue;
    }

    public void setPartiePerdue(boolean partiePerdue) {
        this.partiePerdue = partiePerdue;
    }

    public boolean tousLesLingotsRecoltes(){
        for (int x = 0; x < largeur; x++) {
            for (int y = 0; y < hauteur; y++) {
                if (cases[x][y] == Case.LINGOT) {
                    return false;
                }
            }
        }
        return true;
    }
    // vérifie qu'une case est libre : pas un mur et pas déjà occupée par une entité
    public boolean estLibre(int x, int y) {
        if (!estPositionValide(x, y)) {
            return false;
        }
        Case type = getCase(x, y);
        if (type == Case.MUR) {
            return false;
        }
        for (Joueur joueur : joueurs) {
            if (joueur.getX() == x && joueur.getY() == y) {
                return false;
            }
        }
        for (Garde garde : gardes) {
            if (garde.getX() == x && garde.getY() == y) {
                return false;
            }
        }
        return true;

    }
}
