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
import loderunner.ui.LeaderboardWindow;
import loderunner.utils.Direction;

// le client se connecte au serveur, envoie les touches pressées et affiche l'état reçu
// toute la logique du jeu est côté serveur, le client ne fait qu'afficher
public class Client implements Runnable {

    private final String                       hote;
    private final int                          port;
    private final MessageProtocol.RoleJoueur   role;
    private final String                       nomEquipe;

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
        this(hote, port, role, "");
    }

    public Client(String hote, int port, MessageProtocol.RoleJoueur role, String nomEquipe) {
        this.hote      = hote;
        this.port      = port;
        this.role      = role;
        this.nomEquipe = nomEquipe != null ? nomEquipe : "";
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
            // important : il faut créer le flux de sortie en premier des deux côtés, sinon ça bloque indéfiniment
            sortie = new ObjectOutputStream(socket.getOutputStream());
            sortie.flush();
            entree = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connecté à " + hote + ":" + port);

            // on dit au serveur notre rôle et notre équipe
            sortie.writeObject(new MessageProtocol.MessageConnexion(role, nomEquipe));
            sortie.flush();

            // le serveur nous répond avec l'index de l'entité qui nous est assignée
            Object premier = entree.readObject();
            if (premier instanceof MessageProtocol.MessageAssignation) {
                MessageProtocol.MessageAssignation assignation = (MessageProtocol.MessageAssignation) premier;
                indexJoueur = assignation.indexEntite;
                System.out.println("Assigné comme " + assignation.role + " " + indexJoueur);
            }

            // plateau local vide pour l'instant, il sera mis à jour dès qu'on reçoit le premier état du serveur
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

            // on envoie les inputs dans un thread séparé pour ne pas bloquer la réception
            Thread envoyeur = new Thread(this, "Envoyeur-Inputs");
            envoyeur.setDaemon(true);
            envoyeur.start();

            // boucle principale : on attend les états du serveur et on les affiche
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

    // envoie la direction et si on creuse au serveur, 20 fois par seconde
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

    // on applique l'état reçu du serveur sur notre plateau local pour l'affichage
    private void appliquerEtat(MessageProtocol.EtatJeu etat) {
        // si les dimensions changent (nouveau niveau), on recrée le plateau local
        if (plateauLocal.getLargeur() != etat.largeur
                || plateauLocal.getHauteur() != etat.hauteur) {
            plateauLocal = new Plateau(etat.largeur, etat.hauteur);
            plateauLocal.initPlateau();
            if (panel != null) {
                panel.setPlateau(plateauLocal);
            }
        }

        // mise à jour de toutes les cases du plateau (murs, lingots, trous...)
        for (int x = 0; x < etat.largeur; x++) {
            for (int y = 0; y < etat.hauteur; y++) {
                plateauLocal.setCase(x, y, Case.values()[etat.cases[x][y]]);
            }
        }

        // on compte les joueurs et gardes dans l'état pour savoir si on doit en ajouter localement
        int nbJoueurs = 0;
        int nbGardes  = 0;
        for (MessageProtocol.EntiteDTO dto : etat.entites) {
            if (dto.type == MessageProtocol.EntiteDTO.TypeEntite.JOUEUR) nbJoueurs++;
            else nbGardes++;
        }

        // on synchronise le nombre d'entités avec le serveur (ajout ET suppression)
        while (plateauLocal.getJoueurs().size() < nbJoueurs)
            plateauLocal.ajouterJoueur(new Joueur(0, 0, plateauLocal));
        while (plateauLocal.getJoueurs().size() > nbJoueurs)
            plateauLocal.getJoueurs().remove(plateauLocal.getJoueurs().size() - 1);

        while (plateauLocal.getGardes().size() < nbGardes)
            plateauLocal.ajouterGarde(new Garde(0, 0, plateauLocal));
        while (plateauLocal.getGardes().size() > nbGardes)
            plateauLocal.getGardes().remove(plateauLocal.getGardes().size() - 1);

        // mise à jour des positions et directions de toutes les entités
        int iJ = 0;
        int iG = 0;
        for (MessageProtocol.EntiteDTO dto : etat.entites) {
            if (dto.type == MessageProtocol.EntiteDTO.TypeEntite.JOUEUR) {
                Joueur j = plateauLocal.getJoueurs().get(iJ++);
                j.setPosition(dto.x, dto.y);
                j.setDirection(dto.direction);
                j.setNomEquipe(dto.nomEquipe);
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
        plateauLocal.setPartiePerdue(etat.partiePerdue);
    }

    // synchronise le score local avec celui du serveur (on calcule la différence et on l'ajoute)
    private void syncScore(Joueur j, int scoreServeur) {
        int diff = scoreServeur - j.getScore();
        if (diff != 0) j.ajouterScore(diff);
    }

    // idem pour les vies
    private void syncVies(Joueur j, int viesServeur) {
        while (j.getVies() > viesServeur) {
            j.perdreVie();
        }
    }

    private void afficherLeaderboard(MessageProtocol.MessageLeaderboard lb) {
        LeaderboardWindow.afficher(lb.entrees, lb.equipes);
    }

    private void fermer() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // on ignore
        }
    }
}
