package loderunner.model;
// Classe qui représente le joueur dans le jeu, avec ses caractéristiques et ses actions possibles.
public class Joueur extends Entite {
    private int score; // Score du joueur
    private int vies; // Nombre de vies restantes du joueur
    public Joueur(int x, int y, Plateau plateau) {
        super(x, y, plateau);
        this.score = 0;
        this.vies = 5; // car le joueur commence avec 5 vies
    }
    public int getScore() {
        return this.score;
    }
    public int getVies(){
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
        this.ajouterScore(10); // Chaque paquet rapporte 10 points
    }
    public void prendreLingot() {
        this.ajouterScore(50); // Chaque lingot rapporte 50 points
    }
    public void entrerSortie() {
        this.ajouterScore(100); // Finir la partie rapporte 100 points
    }
    //cette fonction est utilisée lorsque le joueur perd toutes ses vies ou lorsqu'il veut recommencer une partie
    @Override
    public void reset() {
        super.reset(); // Réinitialiser la position du joueur
        this.score = 0; // Réinitialiser le score
        this.vies = 5; // Réinitialiser les vies
    }
}
