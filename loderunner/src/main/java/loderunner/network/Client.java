package loderunner.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import loderunner.model.Case;
import loderunner.model.Garde;
import loderunner.model.Joueur;
import loderunner.model.Plateau;
import loderunner.ui.GamePanel;
import loderunner.ui.InputHandler;
import loderunner.utils.Direction;

// Classe qui représente le client en mode réseau
// Le client se connecte au serveur, envoie ses inputs et reçoit l'état du jeu pour l'afficher
public class Client implements Runnable {

    private final String                       hote;
    private final int                          port;
    private final MessageProtocol.RoleJoueur   role;

    private Socket             socket;
    private ObjectOutputStream sortie;
    private ObjectInputStream  entree;

    private int indexJoueur = -1;

    private JFrame       frame;
    private GamePanel    panel;
    private InputHandler inputHandler;

    // plateau local utilisé uniquement pour l'affichage
    // c'est le serveur qui calcule toute la physique
    private Plateau plateauLocal;

    public Client(String hote, int port, MessageProtocol.RoleJoueur role) {
        this.hote = hote;
        this.port = port;
        this.role = role;
    }

    public static void main(String[] args) {
        String hote = "localhost";
        int    port = MessageProtocol.PORT_DEFAUT;
        if (args.length >= 1) hote = args[0];
        if (args.length >= 2) {
            try { port = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { }
        }
        new Client(hote, port, MessageProtocol.RoleJoueur.JOUEUR).connecter();
    }

    public void connecter() {
        try {
            socket = new Socket(hote, port);
            // on crée le flux de sortie en premier, sinon ça bloque des deux côtés
            sortie = new ObjectOutputStream(socket.getOutputStream());
            sortie.flush();
            entree = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connecté à " + hote + ":" + port);

            // on envoie notre rôle au serveur
            sortie.writeObject(new MessageProtocol.MessageConnexion(role));
            sortie.flush();

            // on reçoit l'index de l'entité qu'on contrôle
            Object premier = entree.readObject();
            if (premier instanceof MessageProtocol.MessageAssignation) {
                MessageProtocol.MessageAssignation assignation = (MessageProtocol.MessageAssignation) premier;
                indexJoueur = assignation.indexEntite;
                System.out.println("Assigné comme " + assignation.role + " " + indexJoueur);
            }

            // on crée un plateau vide pour l'affichage, il sera mis à jour dès le premier EtatJeu reçu
            plateauLocal = new Plateau(20, 15);
            plateauLocal.initPlateau();
            inputHandler = new InputHandler();

            SwingUtilities.invokeLater(() -> {
                String titre = role == MessageProtocol.RoleJoueur.GARDE
                        ? "Lode Runner — Garde " + indexJoueur
                        : "Lode Runner — Joueur " + indexJoueur;
                frame = new JFrame(titre);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                panel = new GamePanel(plateauLocal);
                if (role == MessageProtocol.RoleJoueur.GARDE) {
                    panel.setIndexGardeLocal(indexJoueur);
                } else {
                    panel.setIndexJoueurLocal(indexJoueur);
                }
                panel.addKeyListener(inputHandler);
                panel.setFocusable(true);
                frame.add(panel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                panel.requestFocusInWindow();
            });

            // thread séparé pour envoyer les inputs au serveur
            Thread envoyeur = new Thread(this, "Envoyeur-Inputs");
            envoyeur.setDaemon(true);
            envoyeur.start();

            // boucle principale : on attend les messages du serveur
            while (!socket.isClosed()) {
                Object message = entree.readObject();
                if (message instanceof MessageProtocol.EtatJeu) {
                    appliquerEtat((MessageProtocol.EtatJeu) message);
                    if (panel != null) {
                        panel.incrementerTick();
                        panel.repaint();
                    }
                } else if (message instanceof MessageProtocol.MessageLeaderboard) {
                    afficherLeaderboard((MessageProtocol.MessageLeaderboard) message);
                }
            }

        } catch (EOFException | java.net.SocketException e) {
            System.out.println("Connexion au serveur perdue.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            fermer();
        }
    }

    // thread qui envoie les inputs du joueur au serveur 20 fois par seconde
    @Override
    public void run() {
        while (socket != null && !socket.isClosed()) {
            try {
                Direction dir     = inputHandler.getDirection();
                boolean   creuser = inputHandler.consommerCreuser();
                sortie.writeUnshared(new MessageProtocol.MessageInput(dir, creuser));
                sortie.flush();
                Thread.sleep(50);
            } catch (IOException | InterruptedException e) {
                break;
            }
        }
    }

    // met à jour le plateau local avec l'état reçu du serveur
    private void appliquerEtat(MessageProtocol.EtatJeu etat) {
        // si les dimensions ont changé on recrée le plateau
        if (plateauLocal.getLargeur() != etat.largeur
                || plateauLocal.getHauteur() != etat.hauteur) {
            plateauLocal = new Plateau(etat.largeur, etat.hauteur);
            plateauLocal.initPlateau();
            if (panel != null) {
                panel.setPlateau(plateauLocal);
            }
        }

        // on met à jour toutes les cases
        for (int x = 0; x < etat.largeur; x++) {
            for (int y = 0; y < etat.hauteur; y++) {
                plateauLocal.setCase(x, y, Case.values()[etat.cases[x][y]]);
            }
        }

        // on compte combien de joueurs et gardes il y a dans l'état reçu
        int nbJoueurs = 0;
        int nbGardes  = 0;
        for (MessageProtocol.EntiteDTO dto : etat.entites) {
            if (dto.type == MessageProtocol.EntiteDTO.TypeEntite.JOUEUR) nbJoueurs++;
            else nbGardes++;
        }

        // on ajoute des entités locales si le serveur en a plus que nous
        while (plateauLocal.getJoueurs().size() < nbJoueurs) {
            plateauLocal.ajouterJoueur(new Joueur(0, 0, plateauLocal));
        }
        while (plateauLocal.getGardes().size() < nbGardes) {
            plateauLocal.ajouterGarde(new Garde(0, 0, plateauLocal));
        }

        // on met à jour la position et la direction de chaque entité
        int iJ = 0;
        int iG = 0;
        for (MessageProtocol.EntiteDTO dto : etat.entites) {
            if (dto.type == MessageProtocol.EntiteDTO.TypeEntite.JOUEUR) {
                Joueur j = plateauLocal.getJoueurs().get(iJ++);
                j.setPosition(dto.x, dto.y);
                j.setDirection(dto.direction);
                syncScore(j, dto.score);
                syncVies(j, dto.vies);
            } else {
                Garde g = plateauLocal.getGardes().get(iG++);
                g.setPosition(dto.x, dto.y);
                g.setDirection(dto.direction);
                g.setBloque(dto.bloque);
            }
        }

        plateauLocal.setPartieGagnee(etat.partieGagnee);
    }

    // met à jour le score local pour qu'il corresponde au score du serveur
    private void syncScore(Joueur j, int scoreServeur) {
        int diff = scoreServeur - j.getScore();
        if (diff != 0) j.ajouterScore(diff);
    }

    // met à jour les vies locales pour qu'elles correspondent aux vies du serveur
    private void syncVies(Joueur j, int viesServeur) {
        while (j.getVies() > viesServeur) {
            j.perdreVie();
        }
    }

    private void afficherLeaderboard(MessageProtocol.MessageLeaderboard lb) {
        System.out.println("\n=== LEADERBOARD ===");
        for (int i = 0; i < lb.entrees.length; i++) {
            System.out.printf("%2d. %-15s %5d pts%n",
                    i + 1, lb.entrees[i].nomJoueur, lb.entrees[i].score);
        }
        System.out.println("===================\n");
    }

    private void fermer() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // on ignore
        }
    }
}
