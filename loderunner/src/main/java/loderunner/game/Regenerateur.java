package loderunner.game;

import loderunner.model.Case;
import loderunner.model.Entite;
import loderunner.model.Garde;
import loderunner.model.Plateau;
import java.util.ArrayList;
import java.util.Iterator;

public class Regenerateur {
    private Plateau plateau;
    private final int TEMPS_REBOUCHAGE = 80; // 80 ticks à 20 FPS = 4 secondes avant que le trou se rebouche
    private ArrayList<TrouEnAttente> trous;

    public Regenerateur(Plateau p) {
        this.plateau = p;
        this.trous = new ArrayList<>();
    }

    // appelé quand le joueur creuse une brique — on enregistre le trou et on le crée dans le plateau
    public void ajouterTrou(int x, int y) {
        trous.add(new TrouEnAttente(x, y, TEMPS_REBOUCHAGE));
        plateau.setCase(x, y, Case.TROU);
    }

    // appelé à chaque tick par le Moteur pour décrémenter les compteurs
    // on utilise un Iterator pour pouvoir supprimer pendant qu'on parcourt la liste
    // (sans ça on avait une ConcurrentModificationException)
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

    // quand le trou se rebouche, si quelqu'un est encore dedans on le ramène à sa position de départ
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

    // classe interne juste pour regrouper les infos d'un trou en attente de rebouchage
    private class TrouEnAttente {
        int x, y, tempsRestant;

        TrouEnAttente(int x, int y, int t) {
            this.x = x;
            this.y = y;
            this.tempsRestant = t;
        }
    }
}