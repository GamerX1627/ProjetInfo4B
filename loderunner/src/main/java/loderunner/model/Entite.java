package loderunner.model;

// Classe qui représente une entité dans le jeu, comme un joueur, un ennemi, ou un objet.
public abstract class Entite {
    protected int x; // Position horizontale de l'entité
    protected int y; // Position verticale de l'entité
    protected int x_Initiale; // Position horizontale initiale de l'entité pour pouvoir la réinitialiser
    protected int y_Initiale; // Position verticale initiale de l'entité pour pouvoir la réinitialiser
    protected Plateau plateau; // Référence au plateau de jeu

    public Entite(int x, int y, Plateau plateau) {
        this.setPosition(x, y);
        this.x_Initiale = x;
        this.y_Initiale = y;
        this.plateau = plateau;
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

    public void deplacer(int dx, int dy) {
        int newX = this.x + dx;
        int newY = this.y + dy;
        if (plateau.estPositionValide(newX, newY)) {
            this.setPosition(newX, newY);
        }
    }

    public void reset() {
        this.setPosition(x_Initiale, y_Initiale); // Réinitialiser la position de l'entité
    }
}
