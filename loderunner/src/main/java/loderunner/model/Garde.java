package loderunner.model;
// Classe qui représente un garde dans le jeu, avec ses caractéristiques et ses actions possibles.
public class Garde extends Entite {
    private boolean Bloque; //Indique si le garde est bloqué dans le trou ou non
    public Garde(int x, int y, Plateau plateau) {
        super(x, y, plateau);
        this.Bloque = false;
    }
    public boolean estBloque() {
        return Bloque;
    }
    public void setBloque(boolean bloque) {
        this.Bloque = bloque;
    }
    @Override
    public void reset() {
        super.reset(); // Réinitialiser la position du garde
        this.Bloque = false; // Réinitialiser l'état de blocage
    }
}
