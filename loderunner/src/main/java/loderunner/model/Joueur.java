package loderunner.model;

// le joueur, c'est le personnage principal qu'on contrôle
// il hérite d'Entite pour avoir les déplacements de base
public class Joueur extends Entite {
    private int score; // points accumulés pendant la partie
    private int vies;  // nombre de vies restantes

    public Joueur(int x, int y, Plateau plateau) {
        super(x, y, plateau);
        this.score = 0;
        this.vies = 5; // on commence avec 5 vies comme dans le jeu original
    }

    public int getScore() {
        return this.score;
    }

    public int getVies() {
        return this.vies;
    }

    public void ajouterScore(int points) {
        this.score += points;
    }

    public void perdreVie() {
        this.vies--;
    }

    public boolean estMort() {
        return this.vies <= 0;
    }

    public void prendrePaquet() {
        this.ajouterScore(10); // 10 points par paquet
    }

    public void prendreLingot() {
        this.ajouterScore(50); // 50 points par lingot
    }

    public void entrerSortie() {
        this.ajouterScore(100); // bonus de fin de niveau
    }

    // on remet le joueur à sa position de départ (quand il meurt ou que le niveau repart)
    @Override
    public void reset() {
        super.reset();
    }
}
