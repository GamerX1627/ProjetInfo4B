package loderunner.model;

import loderunner.utils.Direction;

// classe abstraite qui regroupe ce que le joueur et les gardes ont en commun
// comme ça on évite de dupliquer du code entre Joueur et Garde
public abstract class Entite {
    protected int x; // position en colonne
    protected int y; // position en ligne
    protected int x_Initiale; // position de départ, on en a besoin pour le reset
    protected int y_Initiale; // pareil pour y
    protected Plateau plateau; // référence au plateau, pour vérifier les déplacements
    private Direction directionActuelle;
    private String nomEquipe = ""; // équipe du joueur dans les parties coopératives
    public Entite(int x, int y, Plateau plateau) {
        this.setPosition(x, y);
        this.x_Initiale = x;
        this.y_Initiale = y;
        this.plateau = plateau;
        this.directionActuelle=Direction.AUCUNE;
    }
    public Direction getDirection(){
        return this.directionActuelle;
    }
    public void setDirection(Direction d){
        this.directionActuelle=d;
    }

    public String getNomEquipe() {
        return nomEquipe;
    }

    public void setNomEquipe(String nomEquipe) {
        this.nomEquipe = nomEquipe != null ? nomEquipe : "";
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public void deplacer(Direction d){
        switch (d) {
            case HAUT:   deplacer(0, -1); break;
            case BAS:    deplacer(0,  1); break;
            case GAUCHE: deplacer(-1, 0); break;
            case DROITE: deplacer(1,  0); break;
            default: break;
        }
    }
    public void deplacer(int dx, int dy) {
        int newX = this.x + dx;
        int newY = this.y + dy;
        if (plateau.estPositionValide(newX, newY)) {
            this.setPosition(newX, newY);
        }
    }
    public void tomber(){
        this.deplacer(0, 1);
    }

    public void reset() {
        this.setPosition(x_Initiale, y_Initiale); // on remet l'entité à sa position de départ
    }
}
