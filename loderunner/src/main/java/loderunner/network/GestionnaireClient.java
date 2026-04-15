package loderunner.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import loderunner.utils.Direction;

// un thread par client connecté côté serveur
// il lit les inputs du client et lui envoie les états du jeu
public class GestionnaireClient implements Runnable {

    private final Socket  socket;
    private final Serveur serveur;

    private ObjectOutputStream sortie;
    private ObjectInputStream  entree;

    // le rôle du client (joueur ou garde) et l'index de son entité dans le plateau
    private MessageProtocol.RoleJoueur role       = MessageProtocol.RoleJoueur.JOUEUR;
    private int                        indexEntite = -1;
    private String                     nomEquipe  = ""; // équipe choisie par le joueur

    // dernier input reçu (on garde seulement le plus récent)
    private Direction directionCourante = Direction.AUCUNE;
    private boolean   creuser           = false;
    private boolean   connecte          = false;

    public GestionnaireClient(Socket socket, Serveur serveur) {
        this.socket  = socket;
        this.serveur = serveur;
    }

    @Override
    public void run() {
        try {
            // flux de sortie en premier des deux côtés, sinon deadlock garanti
            sortie = new ObjectOutputStream(socket.getOutputStream());
            sortie.flush();
            entree = new ObjectInputStream(socket.getInputStream());

            // le premier message qu'on reçoit c'est le rôle et l'équipe choisis par le client
            Object premier = entree.readObject();
            if (premier instanceof MessageProtocol.MessageConnexion) {
                MessageProtocol.MessageConnexion conn = (MessageProtocol.MessageConnexion) premier;
                role      = conn.role;
                nomEquipe = conn.nomEquipe;
            }

            // on demande au serveur de nous assigner une entité
            serveur.assignerEntite(this, role);

            // on confirme au client son rôle et l'index de son entité
            sortie.writeObject(new MessageProtocol.MessageAssignation(indexEntite, role));
            sortie.flush();

            connecte = true;
            System.out.println(role + " " + indexEntite + " connecté depuis " + socket.getInetAddress());

            // boucle de lecture des inputs du client
            while (!socket.isClosed()) {
                Object message = entree.readObject();
                if (message instanceof MessageProtocol.MessageInput) {
                    MessageProtocol.MessageInput input = (MessageProtocol.MessageInput) message;
                    directionCourante = input.direction;
                    if (input.creuser) {
                        creuser = true;
                    }
                }
            }

        } catch (EOFException | java.net.SocketException e) {
            // le client s'est déconnecté normalement
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            connecte = false;
            fermer();
            serveur.deconnecterClient(this);
        }
    }

    // consomme l'action creuser : retourne true une seule fois par appui puis remet à false
    public boolean consommerCreuser() {
        if (creuser) {
            creuser = false;
            return true;
        }
        return false;
    }

    public Direction getDirectionCourante() {
        return directionCourante;
    }

    public MessageProtocol.RoleJoueur getRole() {
        return role;
    }

    public int getIndexEntite() {
        return indexEntite;
    }

    public void setIndexEntite(int index) {
        this.indexEntite = index;
    }

    public String getNomEquipe() {
        return nomEquipe;
    }

    public boolean estConnecte() {
        return connecte;
    }

    // envoie le snapshot du jeu au client
    // writeUnshared est important ici : sans ça, Java enverrait une référence vers le même objet et le client ne verrait pas les mises à jour
    public void envoyerEtat(MessageProtocol.EtatJeu etat) {
        if (sortie == null || !connecte) return;
        try {
            sortie.writeUnshared(etat);
            sortie.flush();
        } catch (IOException e) {
            connecte = false;
        }
    }

    // envoie le leaderboard au client
    public void envoyerLeaderboard(MessageProtocol.MessageLeaderboard lb) {
        if (sortie == null || !connecte) return;
        try {
            sortie.writeObject(lb);
            sortie.flush();
        } catch (IOException e) {
            connecte = false;
        }
    }

    public void fermer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // on ignore
        }
    }
}
