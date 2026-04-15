package loderunner.model;

// un garde, c'est l'adversaire du joueur — soit contrôlé par l'IA, soit par un autre joueur en réseau
public class Garde extends Entite {
    private boolean Bloque; // vrai quand le garde est tombé dans un trou et ne peut plus bouger

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
        super.reset();
        this.Bloque = false; // quand il respawn il n'est plus bloqué
    }
}
