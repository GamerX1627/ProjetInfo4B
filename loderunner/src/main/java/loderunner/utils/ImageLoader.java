package loderunner.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageLoader {
    // ici on stocke les images déjà chargées en mémoire
    private static Map<String, BufferedImage> cacheImages = new HashMap<>();

    public static BufferedImage getImage(String nomFichier) {
        // On vérifie si l'image est déjà chargée en mémoir
        if (cacheImages.containsKey(nomFichier)) {
            return cacheImages.get(nomFichier);
        }

        // Si non, on tente de la charger depuis le dossier "assets"
        try {
            BufferedImage img = ImageIO.read(new File("loderunner/src/main/ressources/images/" + nomFichier));
            cacheImages.put(nomFichier, img); // On garde l'image pour éviter qu'elle soit encore chargée à chaque
                                              // niveau
            return img;
        } catch (IOException e) {
            System.err.println("Erreur : Impossible de charger l'image " + nomFichier);
            return genererImageErreur(); //
        }
    }

    /**
     * Méthode générée par l'IA pour faire un carré rose en cas de fichier non
     * trouvé.
     */
    private static BufferedImage genererImageErreur() {
        BufferedImage backup = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                backup.setRGB(x, y, 0xFF00FF); // carré rose pour éviter que le programme il s'arrête à cause d'une
                                               // image mal chargée
            }
        }
        return backup;
    }
}