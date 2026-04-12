package loderunner.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import loderunner.utils.Direction;

// Classe qui gère la connexion avec un client côté serveur
// On crée un thread de cette classe pour chaque client connecté
public class GestionnaireClient implements Runnable {

    private final Socket  socket;
    private final Serveur serveur;

    private ObjectOutputStream sortie;
    private ObjectInputStream  entree;

    // rôle et index de l'entité contrôlée par ce client
    private MessageProtocol.RoleJoueur role       = MessageProtocol.RoleJoueur.JOUEUR;
    private int                        indexEntite = -1;

    // dernier input reçu du client
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
            // on crée le flux de sortie en premier, sinon ça bloque des deux côtés
            sortie = new ObjectOutputStream(socket.getOutputStream());
            sortie.flush();
            entree = new ObjectInputStream(socket.getInputStream());

            // on lit le rôle choisi par le client
            Object premier = entree.readObject();
            if (premier instanceof MessageProtocol.MessageConnexion) {
                role = ((MessageProtocol.MessageConnexion) premier).role;
            }

            // le serveur assigne l'entité correspondante et met à jour indexEntite
            serveur.assignerEntite(this, role);

            // on envoie au client son index et son rôle
            sortie.writeObject(new MessageProtocol.MessageAssignation(indexEntite, role));
            sortie.flush();

            connecte = true;
            System.out.println(role + " " + indexEntite + " connecté depuis " + socket.getInetAddress());

            // on lit en boucle les inputs envoyés par le client
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

    // retourne true si le joueur veut creuser, et remet à false
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

    public boolean estConnecte() {
        return connecte;
    }

    // envoie l'état du jeu au client
    // writeUnshared au lieu de writeObject pour que le client reçoive bien un nouvel objet à chaque fois
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
