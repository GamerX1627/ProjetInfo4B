package loderunner.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import loderunner.model.Case;
import loderunner.model.Plateau;

public class LevelLoader {
    public static Plateau loadMap(String cheminFichier) { // méthode qui permet de charger le plateau à partir d'un
                                                          // fichier texte.
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
                        case '#': { // ici, on se sert du # dans le fichier texte pour indiquer au LevelLoader que
                                    // c'est un mur
                            P.setCase(i, j, Case.MUR);
                            break;
                        }
                        case 'H': { // ici, on se sert du H pour indiquer que c'est une echelle
                            P.setCase(i, j, Case.ECHELLE);
                            break;
                        }
                        case '-': { // on se sert du - pour indiquer que c'est une passerelle
                            P.setCase(i, j, Case.PASSERELLE);
                            break;
                        }
                        case ' ': { // on se sert de l'espace pour indiquer que c'est le vide
                            P.setCase(i, j, Case.VIDE);
                            break;
                        }
                        case '£': { // on se sert du £ pour indiquer que c'est un lingot d'or
                            P.setCase(i, j, Case.LINGOT);
                            break;
                        }
                        case 'S': { // on se sert du S pour indiquer la sortie
                            P.setCase(i, j, Case.SORTIE);
                            break;
                        }
                        case 'O': { // on se sert du O pour indiquer un trou
                            P.setCase(i, j, Case.TROU);
                            break;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return P;
    }
}
