package loderunner.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import loderunner.model.Case;
import loderunner.model.Plateau;

public class LevelLoader {
    // lit le fichier texte du niveau et construit le plateau correspondant
    public static Plateau loadMap(String cheminFichier) {
        Plateau P = null;
        try (BufferedReader br = new BufferedReader(new FileReader(cheminFichier))) {
            String ligne;
            ligne = br.readLine();
            String dim[] = ligne.trim().split(" ");
            int largeur = Integer.parseInt(dim[0]);
            int hauteur = Integer.parseInt(dim[1]);
            P = new Plateau(largeur, hauteur);
            P.initPlateau();
            for (int j = 0; j < hauteur; j++) {
                ligne = br.readLine();
                if (ligne == null)
                    ligne = "";
                while (ligne.length() < largeur) {
                    ligne += " ";
                }
                for (int i = 0; i < largeur; i++) {
                    switch (ligne.charAt(i)) {
                        case '#': // mur
                            P.setCase(i, j, Case.MUR);
                            break;
                        case 'H': // échelle
                            P.setCase(i, j, Case.ECHELLE);
                            break;
                        case '-': // passerelle
                            P.setCase(i, j, Case.PASSERELLE);
                            break;
                        case ' ': // vide (case vide)
                            P.setCase(i, j, Case.VIDE);
                            break;
                        case '£': // lingot d'or à ramasser
                            P.setCase(i, j, Case.LINGOT);
                            break;
                        case 'S': // sortie du niveau
                            P.setCase(i, j, Case.SORTIE);
                            break;
                        case 'O': // trou
                            P.setCase(i, j, Case.TROU);
                            break;
                        case 'P': // position de départ du joueur
                            P.ajouterJoueur(new loderunner.model.Joueur(i, j, P));
                            break;
                        case 'G': // position de départ d'un garde
                            P.ajouterGarde(new loderunner.model.Garde(i, j, P));
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return P;
    }
}
