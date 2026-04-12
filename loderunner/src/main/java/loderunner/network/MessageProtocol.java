package loderunner.network;

import java.io.Serializable;
import loderunner.utils.Direction;

// Classe qui contient tous les messages qu'on envoie entre le client et le serveur
// Chaque message est Serializable pour pouvoir être envoyé avec ObjectOutputStream
public class MessageProtocol {

    public static final int PORT_DEFAUT = 12345;

    // Message envoyé par le client au serveur : la direction et si le joueur creuse
    public static class MessageInput implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Direction direction;
        public final boolean creuser;

        public MessageInput(Direction direction, boolean creuser) {
            this.direction = direction;
            this.creuser   = creuser;
        }
    }

    // Représente une entité (joueur ou garde) dans un snapshot du jeu
    public static class EntiteDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        public enum TypeEntite { JOUEUR, GARDE }

        public final TypeEntite type;
        public final int        x;
        public final int        y;
        public final Direction  direction;
        public final int        score;
        public final int        vies;
        public final boolean    bloque;

        public EntiteDTO(TypeEntite type, int x, int y, Direction direction,
                         int score, int vies, boolean bloque) {
            this.type      = type;
            this.x         = x;
            this.y         = y;
            this.direction = direction;
            this.score     = score;
            this.vies      = vies;
            this.bloque    = bloque;
        }
    }

    // Snapshot complet du jeu envoyé par le serveur à chaque tick
    // cases[x][y] contient l'ordinal() de l'enum Case
    public static class EtatJeu implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int          largeur;
        public final int          hauteur;
        public final int[][]      cases;
        public final EntiteDTO[]  entites;
        public final boolean      partieGagnee;
        public final boolean      partiePerdue;
        public final long         tick;

        public EtatJeu(int largeur, int hauteur, int[][] cases,
                       EntiteDTO[] entites, boolean partieGagnee,
                       boolean partiePerdue, long tick) {
            this.largeur      = largeur;
            this.hauteur      = hauteur;
            this.cases        = cases;
            this.entites      = entites;
            this.partieGagnee = partieGagnee;
            this.partiePerdue = partiePerdue;
            this.tick         = tick;
        }
    }

    // Rôle choisi par le client : joueur normal ou garde (mode combat)
    public enum RoleJoueur { JOUEUR, GARDE }

    // Envoyé par le client au serveur juste après la connexion pour indiquer son rôle
    public static class MessageConnexion implements Serializable {
        private static final long serialVersionUID = 1L;
        public final RoleJoueur role;

        public MessageConnexion(RoleJoueur role) {
            this.role = role;
        }
    }

    // Envoyé par le serveur au client pour lui dire quelle entité il contrôle et son rôle
    public static class MessageAssignation implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int        indexEntite;
        public final RoleJoueur role;

        public MessageAssignation(int indexEntite, RoleJoueur role) {
            this.indexEntite = indexEntite;
            this.role        = role;
        }
    }

    // Une ligne du leaderboard
    public static class EntreeLeaderboard implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String nomJoueur;
        public final int    score;

        public EntreeLeaderboard(String nomJoueur, int score) {
            this.nomJoueur = nomJoueur;
            this.score     = score;
        }
    }

    // Le classement complet envoyé à tous les clients en fin de partie
    public static class MessageLeaderboard implements Serializable {
        private static final long serialVersionUID = 1L;
        public final EntreeLeaderboard[] entrees;

        public MessageLeaderboard(EntreeLeaderboard[] entrees) {
            this.entrees = entrees;
        }
    }
}
