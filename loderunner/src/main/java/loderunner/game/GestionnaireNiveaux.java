package loderunner.game;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.JFrame;
import loderunner.model.Plateau;
import loderunner.ui.GamePanel;
import loderunner.ui.InputHandler;
import loderunner.utils.LevelLoader;

public class GestionnaireNiveaux {

    private static final String CHEMIN_NIVEAUX = "src/main/ressources/level/level";
    private static final long DELAI_TRANSITION = 2000; // 2 secondes d'attente entre les niveaux pour que le joueur voie l'écran de victoire

    private int nbNiveaux = 2;
    private int niveauCourant;
    private JFrame frame;
    private InputHandler inputHandler;
    private Moteur moteurCourant;
    private GamePanel panel;

    public GestionnaireNiveaux(JFrame frame, InputHandler ih) {
        this.frame = frame;
        this.inputHandler = ih;
    }

    public void demarrer() {
        niveauCourant = 1;
        chargerNiveau(niveauCourant);
    }

    public void niveauSuivant() {
        if (niveauCourant < nbNiveaux) {
            niveauCourant++;
            chargerNiveau(niveauCourant);
        } else {
            // dernier niveau atteint, on recharge le même (qui se regénère à chaque fois dans chargerNiveau)
            chargerNiveau(niveauCourant);
        }
    }

    // génère un niveau aléatoire et l'écrit dans level3.txt (utilisé à partir du 3ème niveau)
    public void genererNiveau() {
        final int LARGEUR = 20;
        final int HAUTEUR = 15;
        char[][] grille = new char[HAUTEUR][LARGEUR];

        for (int y = 0; y < HAUTEUR; y++)
            Arrays.fill(grille[y], ' ');

        Arrays.fill(grille[0], '#');
        Arrays.fill(grille[HAUTEUR - 1], '#');
        for (int y = 0; y < HAUTEUR; y++) {
            grille[y][0] = '#';
            grille[y][LARGEUR - 1] = '#';
        }

        // on choisit un nombre d'étages entre 2 et 4 au hasard
        int nbEtages = 2 + alea(3);
        int espacement = (HAUTEUR - 2) / (nbEtages + 1);
        int[] rowsMurs = new int[nbEtages];
        for (int i = 0; i < nbEtages; i++)
            rowsMurs[i] = Math.max(2, Math.min(HAUTEUR - 3, 1 + espacement * (i + 1) + alea(3) - 1));

        // pour chaque étage on place un mur horizontal, parfois avec un trou au milieu pour passer
        for (int rowMur : rowsMurs) {
            if (alea(3) > 0) {
                // mur complet
                Arrays.fill(grille[rowMur], 1, LARGEUR - 1, '#');
            } else {
                // mur en deux segments avec un trou au milieu
                int xGap = 4 + alea(LARGEUR - 8);
                int tailleGap = 2 + alea(4);
                Arrays.fill(grille[rowMur], 1, xGap, '#');
                int xApres = Math.min(xGap + tailleGap, LARGEUR - 1);
                Arrays.fill(grille[rowMur], xApres, LARGEUR - 1, '#');
            }
        }

        // une grande échelle qui traverse tout le niveau pour qu'on puisse toujours atteindre la sortie
        int xEchPrincipale = 2 + alea(LARGEUR - 4);
        grille[0][xEchPrincipale] = 'S';
        for (int y = 1; y < HAUTEUR - 1; y++)
            grille[y][xEchPrincipale] = 'H';

        // quelques échelles courtes entre étages pour varier les chemins
        int nbEchSec = 1 + alea(3);
        for (int e = 0; e < nbEchSec; e++) {
            int xE = 1 + alea(LARGEUR - 2);
            int etage = alea(rowsMurs.length);
            int yHaut = (etage == 0) ? 1 : rowsMurs[etage - 1] + 1;
            int yBas = rowsMurs[etage];
            for (int y = yHaut; y <= yBas; y++)
                grille[y][xE] = 'H';
        }

        // des passerelles de longueur variable pour avoir des plateformes dans chaque section
        int[] limites = new int[nbEtages + 2];
        limites[0] = 1;
        for (int i = 0; i < nbEtages; i++)
            limites[i + 1] = rowsMurs[i];
        limites[nbEtages + 1] = HAUTEUR - 2;

        for (int s = 0; s < limites.length - 1; s++) {
            int debut = limites[s];
            int fin = limites[s + 1];
            if (fin - debut < 2)
                continue;
            int nbPass = alea(3);
            for (int p = 0; p < nbPass; p++) {
                int rowPass = debut + 1 + alea(fin - debut - 1);
                int x = 1;
                while (x < LARGEUR - 1) {
                    if (alea(2) == 0) {
                        int longueur = 2 + alea(7);
                        for (int i = 0; i < longueur && x + i < LARGEUR - 1; i++)
                            if (grille[rowPass][x + i] == ' ')
                                grille[rowPass][x + i] = '-';
                        x += longueur;
                    } else {
                        x += 1 + alea(4);
                    }
                }
            }
        }

        // on place les lingots sur des cases vides qui ont un sol en dessous, sinon ils flotteraient dans le vide
        int nbLingots = 8 + alea(5);
        int tentativesL = nbLingots * 10;
        while (nbLingots > 0 && tentativesL-- > 0) {
            int x = 1 + alea(LARGEUR - 2);
            int y = 1 + alea(HAUTEUR - 3);
            if (grille[y][x] == ' ' && estSoutenu(grille, x, y, HAUTEUR)) {
                grille[y][x] = '£';
                nbLingots--;
            }
        }

        grille[HAUTEUR - 2][1 + alea(Math.max(2, xEchPrincipale - 1))] = 'P';

        // on place entre 1 et 3 gardes sur des cases qui ont un sol
        int nbGardes = 1 + alea(3);
        for (int g = 0; g < nbGardes; g++) {
            int tentatives = 20;
            while (tentatives-- > 0) {
                int xG = 1 + alea(LARGEUR - 2);
                int yG = 1 + alea(HAUTEUR - 3);
                if (grille[yG][xG] == ' ' && estSoutenu(grille, xG, yG, HAUTEUR)) {
                    grille[yG][xG] = 'G';
                    break;
                }
            }
        }

        String chemin = CHEMIN_NIVEAUX + 3 + ".txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(chemin))) {
            bw.write(LARGEUR + " " + HAUTEUR);
            bw.newLine();
            for (int y = 0; y < HAUTEUR; y++) {
                bw.write(new String(grille[y]));
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (nbNiveaux < 3)
            nbNiveaux = 3;
    }

    // vérifie qu'il y a un sol sous la case — pour éviter de placer des trucs dans le vide
    private boolean estSoutenu(char[][] grille, int x, int y, int hauteur) {
        if (y + 1 >= hauteur)
            return true;
        char caseBas = grille[y + 1][x];
        return caseBas == '#' || caseBas == '-' || caseBas == 'H';
    }

    private int alea(int n) {
        return (int) (Math.random() * n);
    }

    private void chargerNiveau(int num) {
        if (num == nbNiveaux)
            genererNiveau();

        if (moteurCourant != null) {
            moteurCourant.stop();
        }

        String chemin = CHEMIN_NIVEAUX + num + ".txt";
        Plateau plateau = LevelLoader.loadMap(chemin);

        if (panel == null) {
            panel = new GamePanel(plateau);
            panel.addKeyListener(inputHandler);
            panel.setFocusable(true);
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } else {
            panel.setPlateau(plateau);
            frame.pack();
        }
        panel.requestFocusInWindow();

        moteurCourant = new Moteur(plateau, panel, inputHandler);
        Thread threadMoteur = new Thread(moteurCourant);
        threadMoteur.setDaemon(true);
        threadMoteur.start();

        final Plateau plateauSuivi = plateau;
        final int niveauLance = num;
        Thread moniteur = new Thread(() -> {
            try {
                threadMoteur.join();
            } catch (InterruptedException e) {
                return;
            }
            // on vérifie que c'est bien le niveau actuel qui vient de se terminer avant de passer au suivant
        if (niveauLance == niveauCourant && plateauSuivi.isPartieGagnee()) {
                try {
                    Thread.sleep(DELAI_TRANSITION);
                } catch (InterruptedException ex) {
                    return;
                }
                niveauSuivant();
            }
        });
        moniteur.setDaemon(true);
        moniteur.start();
    }
}
