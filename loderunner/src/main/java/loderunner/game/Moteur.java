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
    private boolean encours;
    private int compteurMouvement = 0;
    private static final int DELAI_MOUVEMENT = 2; // 1 déplacement toutes les 2 frames

    public Moteur(Plateau plateau, GamePanel panel, InputHandler inputHandler) {
        this.plateau = plateau;
        this.panel = panel;
        this.inputHandler = inputHandler;
        this.physique = new Physique(plateau);
        this.regenerateur = new Regenerateur(plateau);
        this.encours = false;
    }

    // Appelée à chaque frame : gravité, déplacement joueur, collecte lingots
    public void update() {
        panel.incrementerTick();
        regenerateur.mettreAJour();

        Direction directionJoueur = inputHandler.getDirection();

        for (Entite e : plateau.getEntites()) {
            // Gravité : toutes les entités tombent si rien sous leurs pieds
            boolean enChute = physique.doitTomber(e);
            if (enChute) {
                e.tomber();
            }

            if (e instanceof Joueur) {
                Joueur j = (Joueur) e;
                j.setDirection(directionJoueur);
                // En chute, on autorise quand même les déplacements latéraux
                boolean directionLaterale = directionJoueur == Direction.GAUCHE || directionJoueur == Direction.DROITE;
                if (!enChute || directionLaterale) {
                    compteurMouvement++;
                    if (compteurMouvement >= DELAI_MOUVEMENT && directionJoueur != Direction.AUCUNE && physique.peutSeDeplacer(j, directionJoueur)) {
                        compteurMouvement = 0;
                        j.deplacer(directionJoueur);

                        // Collecte d'un lingot sur la case où arrive le joueur
                        if (plateau.getCase(j.getX(), j.getY()) == Case.LINGOT) {
                            plateau.setCase(j.getX(), j.getY(), Case.VIDE);
                            j.ajouterScore(10);
                        }
                    }
                }
            } else if (e instanceof Garde) {
                Garde g = (Garde) e;
                // IA à implémenter
            }
        }

        verifierCollisions();
    }

    private void verifierCollisions() {
        Joueur j = plateau.getJoueurs().get(0); 
        
        for (Garde g : plateau.getGardes()) {
            if (j.getX() == g.getX() && j.getY() == g.getY()) {
                j.perdreVie();
                resetPositions();
            }
        }
    }

    private void resetPositions() {
        for (Entite e : plateau.getEntites()) {
            e.reset();
        }
    }

    // Boucle principale : 10 FPS, update logique + rendu à chaque frame
    @Override
    public void run() {
        final long DUREE_FRAME = 100; // ms par frame (10 FPS)
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