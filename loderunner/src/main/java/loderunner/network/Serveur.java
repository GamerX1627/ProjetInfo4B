package loderunner.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import loderunner.game.IA;
import loderunner.game.Physique;
import loderunner.game.Regenerateur;
import loderunner.model.Case;
import loderunner.model.Garde;
import loderunner.model.Joueur;
import loderunner.model.Plateau;
import loderunner.utils.Direction;
import loderunner.utils.LevelLoader;

// Classe qui représente le serveur du jeu en mode réseau
// Le serveur héberge la partie : il fait tourner la physique, l'IA et envoie
// l'état du jeu à tous les clients connectés à chaque tick
public class Serveur implements Runnable {

    private static final int    MAX_JOUEURS            = 4;
    private static final long   DUREE_FRAME            = 50L; // 20 FPS
    private static final int    DELAI_MOUVEMENT        = 3;
    private static final int    DELAI_MOUVEMENT_GARDES = 6;
    private static final String CHEMIN_NIVEAUX         = "loderunner/src/main/ressources/level/level";
    private static final String FICHIER_LEADERBOARD    = "leaderboard.txt";

    private final int port;
    private ServerSocket serverSocket;

    // liste des clients connectés
    private final List<GestionnaireClient> clients = new ArrayList<>();

    // compteurs pour assigner les entités aux clients qui se connectent
    private int prochainIndexJoueur = 0;
    private int prochainIndexGarde  = 0;

    private Plateau      plateau;
    private Physique     physique;
    private Regenerateur regenerateur;
    private IA           ia;
    private boolean encours           = false;
    private boolean demanderLancement = false;
    private long tick                 = 0;
    private int  compteurMouvement      = 0;
    private int  compteurMouvementGardes = 0;

    // le leaderboard est sauvegardé dans un fichier texte
    private final List<MessageProtocol.EntreeLeaderboard> leaderboard = new ArrayList<>();

    public Serveur(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = MessageProtocol.PORT_DEFAUT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException e) { }
        }
        new Serveur(port).demarrer();
    }

    // démarrage en mode terminal (on attend que l'utilisateur appuie sur Entrée)
    public void demarrer() {
        chargerLeaderboard();
        chargerNiveau(1);

        // thread qui accepte les connexions en arrière-plan
        Thread accepteur = new Thread(this, "Accepteur-Connexions");
        accepteur.setDaemon(true);
        accepteur.start();

        System.out.println("Serveur démarré sur le port " + port);
        System.out.println("En attente de joueurs... Appuyez sur Entrée pour lancer.");

        try { System.in.read(); } catch (IOException e) { }

        lancerBoucleJeu();
    }

    // démarrage depuis l'interface graphique (MainWindow)
    // on attend que l'hôte clique sur "Lancer la partie"
    public void demarrerDepuisUI() {
        chargerLeaderboard();
        chargerNiveau(1);

        Thread accepteur = new Thread(this, "Accepteur-Connexions");
        accepteur.setDaemon(true);
        accepteur.start();

        System.out.println("Serveur démarré sur le port " + port);

        while (!demanderLancement) {
            try { Thread.sleep(100); } catch (InterruptedException e) { return; }
        }

        lancerBoucleJeu();
    }

    // appelé par MainWindow quand on clique sur "Lancer la partie"
    public void lancerMaintenantDepuisUI() {
        demanderLancement = true;
    }

    // thread qui accepte les nouvelles connexions clients
    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                synchronized (clients) {
                    if (clients.size() >= MAX_JOUEURS) {
                        System.out.println("Connexion refusée : serveur plein.");
                        socket.close();
                        continue;
                    }
                    GestionnaireClient gc = new GestionnaireClient(socket, this);
                    clients.add(gc);
                    new Thread(gc, "Client-" + clients.size()).start();
                }
            }
        } catch (IOException e) {
            if (serverSocket != null && !serverSocket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    // boucle de jeu principale : mise à jour + envoi de l'état à 20 FPS
    private void lancerBoucleJeu() {
        encours = true;
        System.out.println("Partie lancée !");

        while (encours) {
            long debut = System.currentTimeMillis();

            update();
            broadcastEtat();

            long tempsPasse = System.currentTimeMillis() - debut;
            long attente    = DUREE_FRAME - tempsPasse;
            if (attente > 0) {
                try { Thread.sleep(attente); }
                catch (InterruptedException e) { encours = false; }
            }
        }

        enregistrerScores();
        broadcastLeaderboard();

        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException e) { }

        System.out.println("Partie terminée.");
    }

    private void update() {
        tick++;
        regenerateur.mettreAJour();
        compteurMouvement++;
        compteurMouvementGardes++;

        // on fait une copie de la liste pour éviter les problèmes si un client se déconnecte pendant l'update
        List<GestionnaireClient> clientsCopie;
        synchronized (clients) {
            clientsCopie = new ArrayList<>(clients);
        }

        List<Joueur> joueurs = plateau.getJoueurs();

        // mise à jour de chaque joueur selon les inputs reçus de son client
        for (GestionnaireClient gc : clientsCopie) {
            if (gc.getRole() != MessageProtocol.RoleJoueur.JOUEUR) continue;
            int idx = gc.getIndexEntite();
            if (idx < 0 || idx >= joueurs.size()) continue;
            Joueur j = joueurs.get(idx);

            Direction dir     = gc.getDirectionCourante();
            boolean   enChute = physique.doitTomber(j);

            if (enChute) {
                j.tomber();
            }

            j.setDirection(dir);
            boolean dirLaterale = dir == Direction.GAUCHE || dir == Direction.DROITE;

            if (!enChute || dirLaterale) {
                if (compteurMouvement >= DELAI_MOUVEMENT
                        && dir != Direction.AUCUNE
                        && physique.peutSeDeplacer(j, dir)) {
                    j.deplacer(dir);

                    Case caseActuelle = plateau.getCase(j.getX(), j.getY());
                    if (caseActuelle == Case.LINGOT) {
                        plateau.setCase(j.getX(), j.getY(), Case.VIDE);
                        j.ajouterScore(10);
                    }
                    if (caseActuelle == Case.SORTIE) {
                        j.ajouterScore(100);
                        plateau.setPartieGagnee(true);
                        encours = false;
                    }
                }
            }

            if (gc.consommerCreuser()) {
                int dx = 0;
                if (dir == Direction.GAUCHE)      dx = -1;
                else if (dir == Direction.DROITE) dx =  1;
                if (dx != 0) {
                    int xCible = j.getX() + dx;
                    int yCible = j.getY() + 1;
                    if (physique.peutCreuser(xCible, yCible)) {
                        regenerateur.ajouterTrou(xCible, yCible);
                    }
                }
            }
        }

        // mise à jour des gardes : humain si un client contrôle ce garde, sinon IA
        List<Garde> gardes = plateau.getGardes();
        for (int gi = 0; gi < gardes.size(); gi++) {
            Garde g = gardes.get(gi);
            boolean enChute = physique.doitTomber(g);
            if (enChute) g.tomber();

            if (plateau.getCase(g.getX(), g.getY()) == Case.TROU) {
                g.setBloque(true);
                g.setDirection(Direction.AUCUNE);
            }

            if (!enChute && !g.estBloque()
                    && compteurMouvementGardes >= DELAI_MOUVEMENT_GARDES) {
                // on cherche si un client contrôle ce garde
                GestionnaireClient gcGarde = trouverClientGarde(gi, clientsCopie);
                Direction dirGarde;
                if (gcGarde != null) {
                    dirGarde = gcGarde.getDirectionCourante();
                } else if (!joueurs.isEmpty()) {
                    dirGarde = ia.calculerMouvement(g, joueurs.get(0));
                } else {
                    dirGarde = Direction.AUCUNE;
                }
                g.setDirection(dirGarde);
                if (dirGarde != Direction.AUCUNE && physique.peutSeDeplacer(g, dirGarde)) {
                    g.deplacer(dirGarde);
                }
            }
        }

        if (compteurMouvement       >= DELAI_MOUVEMENT)        compteurMouvement       = 0;
        if (compteurMouvementGardes >= DELAI_MOUVEMENT_GARDES) compteurMouvementGardes = 0;

        verifierCollisions();
    }

    private void verifierCollisions() {
        for (Joueur j : plateau.getJoueurs()) {
            for (Garde g : plateau.getGardes()) {
                if (plateau.getCase(g.getX(), g.getY()) == Case.TROU) continue;
                if (j.getX() == g.getX() && j.getY() == g.getY()) {
                    j.perdreVie();
                    if (j.estMort()) {
                        encours = false;
                        return;
                    }
                    j.reset();
                    g.reset();
                }
            }
        }
    }

    // envoie l'état du jeu à tous les clients connectés
    public void broadcastEtat() {
        MessageProtocol.EtatJeu etat = construireEtat();
        List<GestionnaireClient> clientsCopie;
        synchronized (clients) {
            clientsCopie = new ArrayList<>(clients);
        }
        for (GestionnaireClient gc : clientsCopie) {
            gc.envoyerEtat(etat);
        }
    }

    // construit un objet EtatJeu à partir du plateau actuel
    private MessageProtocol.EtatJeu construireEtat() {
        int largeur = plateau.getLargeur();
        int hauteur = plateau.getHauteur();

        int[][] cases = new int[largeur][hauteur];
        for (int x = 0; x < largeur; x++) {
            for (int y = 0; y < hauteur; y++) {
                cases[x][y] = plateau.getCase(x, y).ordinal();
            }
        }

        List<Joueur> joueurs = plateau.getJoueurs();
        List<Garde>  gardes  = plateau.getGardes();
        MessageProtocol.EntiteDTO[] dtos =
                new MessageProtocol.EntiteDTO[joueurs.size() + gardes.size()];
        int i = 0;
        for (Joueur j : joueurs) {
            dtos[i++] = new MessageProtocol.EntiteDTO(
                    MessageProtocol.EntiteDTO.TypeEntite.JOUEUR,
                    j.getX(), j.getY(), j.getDirection(),
                    j.getScore(), j.getVies(), false);
        }
        for (Garde g : gardes) {
            dtos[i++] = new MessageProtocol.EntiteDTO(
                    MessageProtocol.EntiteDTO.TypeEntite.GARDE,
                    g.getX(), g.getY(), g.getDirection(),
                    0, 0, g.estBloque());
        }

        boolean toutsMorts = !joueurs.isEmpty()
                && joueurs.stream().allMatch(Joueur::estMort);

        return new MessageProtocol.EtatJeu(largeur, hauteur, cases, dtos,
                plateau.isPartieGagnee(), toutsMorts, tick);
    }

    // assigne une entité (joueur ou garde) au client selon son rôle
    public synchronized void assignerEntite(GestionnaireClient gc, MessageProtocol.RoleJoueur role) {
        if (role == MessageProtocol.RoleJoueur.GARDE) {
            int index = prochainIndexGarde++;
            // on utilise les gardes déjà présents dans le niveau
            if (index < plateau.getGardes().size()) {
                gc.setIndexEntite(index);
            } else {
                // pas assez de gardes dans le niveau, on refuse silencieusement
                gc.setIndexEntite(-1);
            }
        } else {
            int index = prochainIndexJoueur++;
            if (index >= plateau.getJoueurs().size()) {
                plateau.ajouterJoueur(new Joueur(1, plateau.getHauteur() - 2, plateau));
            }
            gc.setIndexEntite(index);
        }
    }

    // retourne le client qui contrôle le garde à l'index donné, ou null si c'est l'IA
    private GestionnaireClient trouverClientGarde(int indexGarde, List<GestionnaireClient> liste) {
        for (GestionnaireClient gc : liste) {
            if (gc.getRole() == MessageProtocol.RoleJoueur.GARDE
                    && gc.getIndexEntite() == indexGarde) {
                return gc;
            }
        }
        return null;
    }

    // appelé par GestionnaireClient quand un client se déconnecte
    public void deconnecterClient(GestionnaireClient gc) {
        clients.remove(gc);
        System.out.println(gc.getRole() + " " + gc.getIndexEntite() + " déconnecté.");
    }

    // enregistre les scores des joueurs dans le leaderboard
    private void enregistrerScores() {
        List<Joueur> joueurs = plateau.getJoueurs();
        List<GestionnaireClient> clientsCopie;
        synchronized (clients) {
            clientsCopie = new ArrayList<>(clients);
        }
        for (int i = 0; i < clientsCopie.size() && i < joueurs.size(); i++) {
            leaderboard.add(new MessageProtocol.EntreeLeaderboard(
                    "Joueur" + (i + 1), joueurs.get(i).getScore()));
        }
        leaderboard.sort((a, b) -> b.score - a.score);
        while (leaderboard.size() > 10) leaderboard.remove(leaderboard.size() - 1);
        sauvegarderLeaderboard();
    }

    private void broadcastLeaderboard() {
        MessageProtocol.EntreeLeaderboard[] entrees =
                leaderboard.toArray(new MessageProtocol.EntreeLeaderboard[0]);
        MessageProtocol.MessageLeaderboard lb = new MessageProtocol.MessageLeaderboard(entrees);
        List<GestionnaireClient> clientsCopie;
        synchronized (clients) {
            clientsCopie = new ArrayList<>(clients);
        }
        for (GestionnaireClient gc : clientsCopie) {
            gc.envoyerLeaderboard(lb);
        }
    }

    // charge le leaderboard depuis le fichier texte
    private void chargerLeaderboard() {
        try (BufferedReader br = new BufferedReader(new FileReader(FICHIER_LEADERBOARD))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                String[] parts = ligne.trim().split(" ");
                if (parts.length == 2) {
                    try {
                        leaderboard.add(new MessageProtocol.EntreeLeaderboard(
                                parts[0], Integer.parseInt(parts[1])));
                    } catch (NumberFormatException e) { }
                }
            }
        } catch (IOException e) {
            // pas de fichier au premier lancement, c'est normal
        }
    }

    private void sauvegarderLeaderboard() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FICHIER_LEADERBOARD))) {
            for (MessageProtocol.EntreeLeaderboard e : leaderboard) {
                bw.write(e.nomJoueur + " " + e.score);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void chargerNiveau(int num) {
        String chemin = CHEMIN_NIVEAUX + num + ".txt";
        plateau      = LevelLoader.loadMap(chemin);
        physique     = new Physique(plateau);
        regenerateur = new Regenerateur(plateau);
        ia           = new IA(plateau);
    }
}
