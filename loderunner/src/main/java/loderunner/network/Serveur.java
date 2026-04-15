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
import loderunner.game.GestionnaireNiveaux;
import loderunner.game.IA;
import loderunner.game.Physique;
import loderunner.game.Regenerateur;
import loderunner.model.Case;
import loderunner.model.Garde;
import loderunner.model.Joueur;
import loderunner.model.Plateau;
import loderunner.utils.Direction;
import loderunner.utils.LevelLoader;

// le serveur héberge la partie en réseau
// c'est lui qui calcule toute la physique et l'IA, les clients reçoivent juste l'état à afficher
public class Serveur implements Runnable {

    private static final int    MAX_JOUEURS            = 4;
    private static final long   DUREE_FRAME            = 50L; // 20 FPS
    private static final int    DELAI_MOUVEMENT        = 3;
    private static final int    DELAI_MOUVEMENT_GARDES = 6;
    private static final String CHEMIN_NIVEAUX         = "loderunner/src/main/ressources/level/level";
    private static final String FICHIER_LEADERBOARD    = "loderunner/src/main/ressources/leaderboard.txt";
    private static final long   DELAI_TRANSITION       = 2000L; // pause entre niveaux

    private int niveauCourant = 1;
    private int nbNiveaux     = 2;

    private final int port;
    private ServerSocket serverSocket;

    // tous les clients actuellement connectés
    private final List<GestionnaireClient> clients = new ArrayList<>();

    // indices à assigner au prochain joueur/garde qui se connecte
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

    // classement des meilleurs scores, chargé depuis un fichier texte au démarrage
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

    // démarrage sans interface graphique, depuis le terminal
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

    // démarrage depuis l'interface graphique, on attend que l'hôte clique sur "Lancer"
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

    // appelé par le bouton "Lancer la partie" dans MainWindow
    public void lancerMaintenantDepuisUI() {
        demanderLancement = true;
    }

    // tourne en permanence pour accepter les nouveaux clients qui se connectent
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

    // boucle principale à 20 FPS : on met à jour le jeu et on envoie l'état à tous les clients
    // quand un niveau est terminé on passe au suivant, on s'arrête seulement si tout le monde est mort
    private void lancerBoucleJeu() {
        System.out.println("Partie lancée !");

        boolean continuer = true;
        while (continuer) {
            // on repart de zéro pour les compteurs à chaque nouveau niveau
            encours = true;
            tick = 0;
            compteurMouvement = 0;
            compteurMouvementGardes = 0;

            // boucle du niveau courant
            while (encours) {
                long debut = System.currentTimeMillis();
                update();
                broadcastEtat();
                long attente = DUREE_FRAME - (System.currentTimeMillis() - debut);
                if (attente > 0) {
                    try { Thread.sleep(attente); }
                    catch (InterruptedException e) { encours = false; continuer = false; }
                }
            }

            if (plateau.isPartieGagnee()) {
                // niveau terminé : on envoie l'état de victoire puis on attend avant de charger le suivant
                broadcastEtat();
                try { Thread.sleep(DELAI_TRANSITION); }
                catch (InterruptedException e) { break; }
                niveauCourant++;
                passerAuNiveauSuivant();
            } else {
                // tous les joueurs sont morts, la partie est finie
                continuer = false;
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

        // copie de la liste pour éviter une ConcurrentModificationException si un client se déconnecte pendant l'update
        List<GestionnaireClient> clientsCopie;
        synchronized (clients) {
            clientsCopie = new ArrayList<>(clients);
        }

        List<Joueur> joueurs = plateau.getJoueurs();

        // on traite les inputs de chaque client connecté en tant que joueur
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

        // pour chaque garde : si un client le contrôle on prend ses inputs, sinon l'IA décide
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
                // est-ce qu'un joueur humain contrôle ce garde ?
                GestionnaireClient gcGarde = trouverClientGarde(gi, clientsCopie);
                Direction dirGarde;
                if (gcGarde != null) {
                    dirGarde = gcGarde.getDirectionCourante();
                } else if (!joueurs.isEmpty()) {
                    dirGarde = ia.calculerMouvement(g, trouverJoueurLePlusProche(g, joueurs));
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

    // envoie un snapshot du jeu à tous les clients
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

    // crée un objet avec toutes les infos du jeu à envoyer aux clients
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
                    j.getScore(), j.getVies(), false, j.getNomEquipe());
        }
        for (Garde g : gardes) {
            dtos[i++] = new MessageProtocol.EntiteDTO(
                    MessageProtocol.EntiteDTO.TypeEntite.GARDE,
                    g.getX(), g.getY(), g.getDirection(),
                    0, 0, g.estBloque(), "");
        }

        boolean toutsMorts = !joueurs.isEmpty()
                && joueurs.stream().allMatch(Joueur::estMort);

        return new MessageProtocol.EtatJeu(largeur, hauteur, cases, dtos,
                plateau.isPartieGagnee(), toutsMorts, tick);
    }

    // quand un client se connecte, on lui assigne un joueur ou un garde du plateau
    // on stocke aussi son nom d'équipe directement sur l'entité
    public synchronized void assignerEntite(GestionnaireClient gc, MessageProtocol.RoleJoueur role) {
        if (role == MessageProtocol.RoleJoueur.GARDE) {
            int index = prochainIndexGarde++;
            // on utilise les gardes déjà présents dans le niveau
            if (index < plateau.getGardes().size()) {
                gc.setIndexEntite(index);
            } else {
                // pas assez de gardes dans le niveau pour ce client
                gc.setIndexEntite(-1);
            }
        } else {
            int index = prochainIndexJoueur++;
            if (index >= plateau.getJoueurs().size()) {
                plateau.ajouterJoueur(new Joueur(1, plateau.getHauteur() - 2, plateau));
            }
            gc.setIndexEntite(index);
            // on mémorise l'équipe sur l'objet Joueur lui-même pour pouvoir l'envoyer aux clients
            plateau.getJoueurs().get(index).setNomEquipe(gc.getNomEquipe());
        }
    }

    // trouve le joueur le plus proche d'un garde (distance de Manhattan)
    // utilisé par l'IA pour savoir qui poursuivre quand il y a plusieurs joueurs
    private Joueur trouverJoueurLePlusProche(Garde garde, List<Joueur> joueurs) {
        Joueur plusProche = null;
        int distMin = Integer.MAX_VALUE;
        for (Joueur j : joueurs) {
            int dist = Math.abs(j.getX() - garde.getX()) + Math.abs(j.getY() - garde.getY());
            if (dist < distMin) {
                distMin = dist;
                plusProche = j;
            }
        }
        return plusProche;
    }

    // cherche si un client contrôle le garde à cet index, retourne null si c'est l'IA qui gère
    private GestionnaireClient trouverClientGarde(int indexGarde, List<GestionnaireClient> liste) {
        for (GestionnaireClient gc : liste) {
            if (gc.getRole() == MessageProtocol.RoleJoueur.GARDE
                    && gc.getIndexEntite() == indexGarde) {
                return gc;
            }
        }
        return null;
    }

    // appelé par GestionnaireClient quand un client quitte la partie
    public void deconnecterClient(GestionnaireClient gc) {
        synchronized (clients) {
            clients.remove(gc);
        }
        System.out.println(gc.getRole() + " " + gc.getIndexEntite() + " déconnecté.");
    }

    // à la fin de la partie, on ajoute les scores dans le classement et on le sauvegarde
    private void enregistrerScores() {
        List<Joueur> joueurs = plateau.getJoueurs();
        for (int i = 0; i < joueurs.size(); i++) {
            Joueur j = joueurs.get(i);
            // si le joueur a un nom d'équipe on l'utilise comme label, sinon "JoueurN"
            String label = j.getNomEquipe().isEmpty() ? "Joueur" + (i + 1) : j.getNomEquipe();
            leaderboard.add(new MessageProtocol.EntreeLeaderboard(label, j.getScore()));
        }
        leaderboard.sort((a, b) -> b.score - a.score);
        while (leaderboard.size() > 10) leaderboard.remove(leaderboard.size() - 1);
        sauvegarderLeaderboard();
    }

    // calcule les scores cumulés par équipe à partir des joueurs présents sur le plateau
    private List<MessageProtocol.EntreeEquipe> calculerScoresEquipes() {
        List<MessageProtocol.EntreeEquipe> equipes = new ArrayList<>();
        List<Joueur> joueurs = plateau.getJoueurs();
        for (Joueur j : joueurs) {
            String equipe = j.getNomEquipe();
            if (equipe.isEmpty()) continue; // joueur sans équipe, on ignore
            // on cherche si l'équipe existe déjà dans la liste
            boolean trouve = false;
            for (int i = 0; i < equipes.size(); i++) {
                if (equipes.get(i).nomEquipe.equals(equipe)) {
                    equipes.set(i, new MessageProtocol.EntreeEquipe(equipe,
                            equipes.get(i).score + j.getScore()));
                    trouve = true;
                    break;
                }
            }
            if (!trouve) {
                equipes.add(new MessageProtocol.EntreeEquipe(equipe, j.getScore()));
            }
        }
        equipes.sort((a, b) -> b.score - a.score);
        return equipes;
    }

    private void broadcastLeaderboard() {
        MessageProtocol.EntreeLeaderboard[] entrees =
                leaderboard.toArray(new MessageProtocol.EntreeLeaderboard[0]);
        List<MessageProtocol.EntreeEquipe> equipesList = calculerScoresEquipes();
        MessageProtocol.EntreeEquipe[] equipes =
                equipesList.toArray(new MessageProtocol.EntreeEquipe[0]);
        MessageProtocol.MessageLeaderboard lb =
                new MessageProtocol.MessageLeaderboard(entrees, equipes);
        List<GestionnaireClient> clientsCopie;
        synchronized (clients) {
            clientsCopie = new ArrayList<>(clients);
        }
        for (GestionnaireClient gc : clientsCopie) {
            gc.envoyerLeaderboard(lb);
        }
    }

    // lecture du classement sauvegardé depuis la dernière partie
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
            // pas de fichier leaderboard au premier lancement, c'est normal
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

    // passe au niveau suivant en gardant les clients connectés
    private void passerAuNiveauSuivant() {
        if (niveauCourant >= nbNiveaux) {
            // plus de niveaux fixes, on en génère un nouveau aléatoirement (toujours dans level3.txt)
            new GestionnaireNiveaux(null, null).genererNiveau();
            if (nbNiveaux < 3) nbNiveaux = 3;
        }
        chargerNiveau(niveauCourant);
        reassignerEntitesClients();
        System.out.println("Niveau " + niveauCourant + " chargé.");
    }

    // le nouveau plateau chargé depuis le fichier n'a qu'un seul joueur (le 'P')
    // donc on re-ajoute les joueurs supplémentaires pour tous les clients déjà connectés
    private void reassignerEntitesClients() {
        List<GestionnaireClient> clientsCopie;
        synchronized (clients) {
            clientsCopie = new ArrayList<>(clients);
        }
        for (GestionnaireClient gc : clientsCopie) {
            int idx = gc.getIndexEntite();
            if (idx < 0) continue;
            if (gc.getRole() == MessageProtocol.RoleJoueur.JOUEUR) {
                while (plateau.getJoueurs().size() <= idx) {
                    plateau.ajouterJoueur(new Joueur(1, plateau.getHauteur() - 2, plateau));
                }
            }
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
