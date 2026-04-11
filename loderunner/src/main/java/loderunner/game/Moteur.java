package loderunner.game;

import loderunner.model.*;
import loderunner.ui.*;
import loderunner.utils.*;

public class Moteur implements Runnable {
    private Plateau plateau;
    private GamePanel panel;
    private Physique physique;
    private Regenerateur regenerateur;
    private InputHandler inputHandler;
    private IA ia;
    private boolean encours;
    private int compteurMouvement = 0;
    private int compteurMouvementGardes = 0;
    private static final int DELAI_MOUVEMENT        = 3;  // le joueur bouge toutes les 3 frames
    private static final int DELAI_MOUVEMENT_GARDES = 6;  // les gardes bougent toutes les 6 frames (plus lents)

    public Moteur(Plateau plateau, GamePanel panel, InputHandler inputHandler) {
        this.plateau = plateau;
        this.panel = panel;
        this.inputHandler = inputHandler;
        this.physique = new Physique(plateau);
        this.regenerateur = new Regenerateur(plateau);
        this.ia = new IA(plateau);
        this.encours = false;
    }

    public void update() {
        if (!encours) return;

        panel.incrementerTick();
        regenerateur.mettreAJour();

        compteurMouvement++;
        compteurMouvementGardes++;

        Direction directionJoueur = inputHandler.getDirection();

        for (Entite e : plateau.getEntites()) {
            boolean enChute = physique.doitTomber(e);
            if (enChute) {
                e.tomber();
            }

            if (e instanceof Joueur) {
                Joueur j = (Joueur) e;
                j.setDirection(directionJoueur);
                // même en tombant le joueur peut se déplacer à gauche/droite
                boolean directionLaterale = directionJoueur == Direction.GAUCHE || directionJoueur == Direction.DROITE;
                if (!enChute || directionLaterale) {
                    if (compteurMouvement >= DELAI_MOUVEMENT && directionJoueur != Direction.AUCUNE && physique.peutSeDeplacer(j, directionJoueur)) {
                        compteurMouvement = 0;
                        j.deplacer(directionJoueur);

                        if (plateau.getCase(j.getX(), j.getY()) == Case.LINGOT) {
                            plateau.setCase(j.getX(), j.getY(), Case.VIDE);
                            j.ajouterScore(10);
                        }

                        // la sortie est bloquée tant que tous les lingots ne sont pas ramassés
                        if (plateau.getCase(j.getX(), j.getY()) == Case.SORTIE) {
                            j.ajouterScore(100);
                            plateau.setPartieGagnee(true);
                            stop();
                        }
                    }
                }

                if (inputHandler.consommerCreuser()) {
                    int dx = 0;
                    if (directionJoueur == Direction.GAUCHE) dx = -1;
                    else if (directionJoueur == Direction.DROITE) dx = 1;

                    if (dx != 0) {
                        int xCible = j.getX() + dx;
                        int yCible = j.getY() + 1;
                        if (physique.peutCreuser(xCible, yCible)) {
                            regenerateur.ajouterTrou(xCible, yCible);
                        }
                    }
                }
            } else if (e instanceof Garde) {
                Garde g = (Garde) e;

                if (plateau.getCase(g.getX(), g.getY()) == Case.TROU) {
                    g.setBloque(true);
                    g.setDirection(Direction.AUCUNE);
                }

                if (!enChute && !g.estBloque() && compteurMouvementGardes >= DELAI_MOUVEMENT_GARDES) {
                    Joueur cible = plateau.getJoueurs().get(0);
                    Direction dirGarde = ia.calculerMouvement(g, cible);
                    g.setDirection(dirGarde);
                    if (dirGarde != Direction.AUCUNE && physique.peutSeDeplacer(g, dirGarde)) {
                        g.deplacer(dirGarde);
                    }
                }
            }
        }

        if (compteurMouvement >= DELAI_MOUVEMENT) {
            compteurMouvement = 0;
        }
        if (compteurMouvementGardes >= DELAI_MOUVEMENT_GARDES) {
            compteurMouvementGardes = 0;
        }

        verifierCollisions();
    }

    private void verifierCollisions() {
        Joueur j = plateau.getJoueurs().get(0);

        for (Garde g : plateau.getGardes()) {
            if (plateau.getCase(g.getX(), g.getY()) == Case.TROU) continue;
            if (j.getX() == g.getX() && j.getY() == g.getY()) {
                j.perdreVie();
                if (j.estMort()) {
                    stop();
                    return;
                }
                resetPositions();
            }
        }
    }

    private void resetPositions() {
        for (Entite e : plateau.getEntites()) {
            e.reset();
        }
    }

    @Override
    public void run() {
        final long DUREE_FRAME = 50; // 20 FPS
        this.encours = true;

        while (encours) {
            long debut = System.currentTimeMillis();

            update();
            panel.repaint();

            long tempsPasse = System.currentTimeMillis() - debut;
            long tempsAttente = DUREE_FRAME - tempsPasse;

            if (tempsAttente > 0) {
                try {
                    Thread.sleep(tempsAttente);
                } catch (InterruptedException e) {
                    encours = false;
                }
            }
        }
    }

    public void stop() {
        this.encours = false;
    }
}