package loderunner.model;
//Classe qui représente le plateau de jeu, et qui est composé de cases.

import java.util.ArrayList;

public class Plateau {
    private Case[][] cases; // Matrice représentant les cases du plateau
    private ArrayList<Joueur> joueurs; // Liste des joueurs présents sur le plateau
    private ArrayList<Garde> gardes; // Liste des gardes présents sur le plateau
    private int largeur; // Largeur du plateau
    private int hauteur; // Hauteur du plateau

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
    public int gethauteur(){
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
    // ajouter des éléments au plateau

    public void ajouterJoueur(Joueur joueur) {
        this.joueurs.add(joueur);
    }

    public void ajouterGarde(Garde garde) {
        this.gardes.add(garde);
    }

    // récuperer les éléments du plateau
    public ArrayList<Joueur> getJoueurs() {
        return joueurs;
    }

    public ArrayList<Garde> getGardes() {
        return gardes;
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

    // modifier le plateau
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
    //à modifier plus tard
    public boolean tousLesLingotsRecoltes(){
        return true;
    }
    // Ici, on vérifie si une position est libre, c'est à dire qu'elle n'est pas un
    // mur et qu'elle n'est pas occupée par un joueur ou un garde. à noter que on
    // pourra plus tard ajouter d'autres éléments comme les paquets et les lingots,
    // mais pour l'instant on se concentre sur les éléments de base.
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
