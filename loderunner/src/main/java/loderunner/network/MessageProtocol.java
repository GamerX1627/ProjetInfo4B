package loderunner.network;

import java.io.Serializable;
import loderunner.utils.Direction;

// tous les types de messages échangés entre le client et le serveur
// ils sont Serializable pour pouvoir transiter par ObjectOutputStream
public class MessageProtocol {

    public static final int PORT_DEFAUT = 12345;

    // envoyé par le client à chaque frame : direction + si on creuse
    public static class MessageInput implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Direction direction;
        public final boolean creuser;

        public MessageInput(Direction direction, boolean creuser) {
            this.direction = direction;
            this.creuser   = creuser;
        }
    }

    // représente une entité (joueur ou garde) dans l'état envoyé par le serveur
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
        public final String     nomEquipe; // équipe du joueur, vide si pas d'équipe

        public EntiteDTO(TypeEntite type, int x, int y, Direction direction,
                         int score, int vies, boolean bloque, String nomEquipe) {
            this.type      = type;
            this.x         = x;
            this.y         = y;
            this.direction = direction;
            this.score     = score;
            this.vies      = vies;
            this.bloque    = bloque;
            this.nomEquipe = nomEquipe != null ? nomEquipe : "";
        }
    }

    // l'état complet du jeu envoyé à chaque tick aux clients
    // cases[x][y] contient l'ordinal() de l'enum Case (plus simple à sérialiser qu'un enum directement)
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

    // le rôle qu'on choisit au moment de se connecter
    public enum RoleJoueur { JOUEUR, GARDE }

    // premier message envoyé par le client juste après la connexion
    public static class MessageConnexion implements Serializable {
        private static final long serialVersionUID = 1L;
        public final RoleJoueur role;
        public final String     nomEquipe; // nom de l'équipe choisie par le joueur

        public MessageConnexion(RoleJoueur role, String nomEquipe) {
            this.role      = role;
            this.nomEquipe = nomEquipe != null ? nomEquipe : "";
        }
    }

    // réponse du serveur : quelle entité on contrôle et avec quel rôle
    public static class MessageAssignation implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int        indexEntite;
        public final RoleJoueur role;

        public MessageAssignation(int indexEntite, RoleJoueur role) {
            this.indexEntite = indexEntite;
            this.role        = role;
        }
    }

    // une entrée du classement individuel : nom du joueur + score
    public static class EntreeLeaderboard implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String nomJoueur;
        public final int    score;

        public EntreeLeaderboard(String nomJoueur, int score) {
            this.nomJoueur = nomJoueur;
            this.score     = score;
        }
    }

    // une entrée du classement par équipe : nom de l'équipe + score cumulé
    public static class EntreeEquipe implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String nomEquipe;
        public final int    score;

        public EntreeEquipe(String nomEquipe, int score) {
            this.nomEquipe = nomEquipe;
            this.score     = score;
        }
    }

    // le classement complet envoyé à la fin de la partie
    // entrees = scores individuels, equipes = scores par équipe (peut être vide si pas d'équipes)
    public static class MessageLeaderboard implements Serializable {
        private static final long serialVersionUID = 1L;
        public final EntreeLeaderboard[] entrees;
        public final EntreeEquipe[]      equipes;

        public MessageLeaderboard(EntreeLeaderboard[] entrees, EntreeEquipe[] equipes) {
            this.entrees = entrees;
            this.equipes = equipes;
        }
    }
}
