package loderunner.game;

import loderunner.model.Case;
import loderunner.model.Entite;
import loderunner.model.Garde;
import loderunner.model.Plateau;
import java.util.ArrayList;
import java.util.Iterator;

public class Regenerateur {
    private Plateau plateau;
    private final int TEMPS_REBOUCHAGE = 80; // Environ 4 secondes à 20 FPS
    private ArrayList<TrouEnAttente> trous;

    public Regenerateur(Plateau p) {
        this.plateau = p;
        this.trous = new ArrayList<>();
    }

    // Appelé par le Moteur quand le joueur creuse
    public void ajouterTrou(int x, int y) {
        trous.add(new TrouEnAttente(x, y, TEMPS_REBOUCHAGE));
        plateau.setCase(x, y, Case.TROU); // On crée le trou physiquement
    }

    // Appelé à chaque "tick" par le Moteur
    // générée par l'IA car lors de l'exécution y'avait une erreur qui s'affiche
    // "ConcurrentModificationException" donc l'IA nous a généré cette partie
    public void mettreAJour() {
        Iterator<TrouEnAttente> it = trous.iterator();
        while (it.hasNext()) {
            TrouEnAttente t = it.next();
            t.tempsRestant--;

            if (t.tempsRestant <= 0) {
                reboucher(t.x, t.y);
                it.remove();
            }
        }
    }

    // si un garde est piégé dans le trou on le réinitialise avant de reboucher
    // si le joueur est aussi dedans on le replace aussi
    private void reboucher(int x, int y) {
        for (Garde g : plateau.getGardes()) {
            if (g.getX() == x && g.getY() == y) {
                g.reset();
                break;
            }
        }
        Entite entite = plateau.getEntiteAt(x, y);
        if (entite != null) {
            entite.reset();
        }
        plateau.setCase(x, y, Case.MUR);
    }

    // Petite classe interne pour stocker les infos du trou
    private class TrouEnAttente {
        int x, y, tempsRestant;

        TrouEnAttente(int x, int y, int t) {
            this.x = x;
            this.y = y;
            this.tempsRestant = t;
        }
    }
}