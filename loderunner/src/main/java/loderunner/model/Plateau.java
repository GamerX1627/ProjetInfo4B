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
        this.cases = new Case[largeur][hauteur];
    }
    public boolean estPositionValide(int x, int y) {
        return true;
    }   
}
