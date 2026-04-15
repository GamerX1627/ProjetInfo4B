package loderunner.ui;

import java.awt.*;
import javax.swing.*;
import loderunner.network.MessageProtocol;

// fenêtre qui s'affiche à la fin de la partie pour montrer le classement des meilleurs scores
public class LeaderboardWindow extends JFrame {

    private static final Color COULEUR_FOND    = new Color(20, 20, 40);
    private static final Color COULEUR_TITRE   = new Color(255, 200, 0);
    private static final Color COULEUR_OR      = new Color(255, 215, 0);
    private static final Color COULEUR_ARGENT  = new Color(192, 192, 192);
    private static final Color COULEUR_BRONZE  = new Color(205, 127, 50);
    private static final Color COULEUR_NORMALE = Color.WHITE;
    private static final Font  POLICE_TITRE    = new Font("Arial", Font.BOLD, 32);
    private static final Font  POLICE_LIGNE    = new Font("Arial", Font.PLAIN, 18);
    private static final Font  POLICE_RANG     = new Font("Arial", Font.BOLD, 18);

    public LeaderboardWindow(MessageProtocol.EntreeLeaderboard[] entrees,
                             MessageProtocol.EntreeEquipe[] equipes) {
        super("Lode Runner — Classement");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel fond = new JPanel(new BorderLayout(0, 16));
        fond.setBackground(COULEUR_FOND);
        fond.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));

        // titre
        JLabel titre = new JLabel("CLASSEMENT", SwingConstants.CENTER);
        titre.setFont(POLICE_TITRE);
        titre.setForeground(COULEUR_TITRE);
        titre.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        fond.add(titre, BorderLayout.NORTH);

        // zone centrale : scores individuels + classement équipes si y'en a
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setBackground(COULEUR_FOND);
        centre.add(construireTableau(entrees));
        if (equipes != null && equipes.length > 0) {
            centre.add(Box.createVerticalStrut(18));
            centre.add(construireTableauEquipes(equipes));
        }
        fond.add(centre, BorderLayout.CENTER);

        // bouton fermer
        JButton btnFermer = new JButton("Fermer");
        btnFermer.setFont(new Font("Arial", Font.BOLD, 16));
        btnFermer.setBackground(new Color(50, 50, 80));
        btnFermer.setForeground(Color.WHITE);
        btnFermer.setFocusPainted(false);
        btnFermer.setBorderPainted(false);
        btnFermer.setOpaque(true);
        btnFermer.setPreferredSize(new Dimension(160, 40));
        btnFermer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFermer.addActionListener(e -> dispose());

        JPanel panelBouton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBouton.setBackground(COULEUR_FOND);
        panelBouton.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        panelBouton.add(btnFermer);
        fond.add(panelBouton, BorderLayout.SOUTH);

        setContentPane(fond);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel construireTableau(MessageProtocol.EntreeLeaderboard[] entrees) {
        JPanel tableau = new JPanel(new GridBagLayout());
        tableau.setBackground(COULEUR_FOND);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < entrees.length; i++) {
            Color couleur = couleurPourRang(i);

            // numéro de rang
            gbc.gridx = 0; gbc.gridy = i;
            gbc.anchor = GridBagConstraints.CENTER;
            JLabel rang = new JLabel(String.format("%2d.", i + 1));
            rang.setFont(POLICE_RANG);
            rang.setForeground(couleur);
            rang.setPreferredSize(new Dimension(35, 25));
            tableau.add(rang, gbc);

            // nom du joueur
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            JLabel nom = new JLabel(entrees[i].nomJoueur);
            nom.setFont(POLICE_LIGNE);
            nom.setForeground(couleur);
            nom.setPreferredSize(new Dimension(180, 25));
            tableau.add(nom, gbc);

            // score aligné à droite
            gbc.gridx = 2;
            gbc.anchor = GridBagConstraints.EAST;
            JLabel score = new JLabel(entrees[i].score + " pts");
            score.setFont(POLICE_RANG);
            score.setForeground(couleur);
            score.setPreferredSize(new Dimension(90, 25));
            tableau.add(score, gbc);
        }

        // si le classement est vide
        if (entrees.length == 0) {
            JLabel vide = new JLabel("Aucun score enregistré.", SwingConstants.CENTER);
            vide.setFont(POLICE_LIGNE);
            vide.setForeground(COULEUR_NORMALE);
            gbc.gridx = 0; gbc.gridy = 0;
            gbc.gridwidth = 3;
            tableau.add(vide, gbc);
        }

        return tableau;
    }

    // or pour le 1er, argent pour le 2e, bronze pour le 3e, blanc pour les autres
    private Color couleurPourRang(int rang) {
        switch (rang) {
            case 0:  return COULEUR_OR;
            case 1:  return COULEUR_ARGENT;
            case 2:  return COULEUR_BRONZE;
            default: return COULEUR_NORMALE;
        }
    }

    // tableau classement par équipes, affiché en dessous du classement individuel
    private JPanel construireTableauEquipes(MessageProtocol.EntreeEquipe[] equipes) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COULEUR_FOND);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 10, 3, 10);

        // sous-titre
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel sousTitre = new JLabel("CLASSEMENT PAR ÉQUIPES", SwingConstants.CENTER);
        sousTitre.setFont(new Font("Arial", Font.BOLD, 16));
        sousTitre.setForeground(COULEUR_TITRE);
        sousTitre.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        panel.add(sousTitre, gbc);

        gbc.gridwidth = 1;
        for (int i = 0; i < equipes.length; i++) {
            Color couleur = couleurPourRang(i);
            gbc.gridy = i + 1;

            gbc.gridx = 0; gbc.anchor = GridBagConstraints.CENTER;
            JLabel rang = new JLabel(String.format("%2d.", i + 1));
            rang.setFont(POLICE_RANG);
            rang.setForeground(couleur);
            rang.setPreferredSize(new Dimension(35, 25));
            panel.add(rang, gbc);

            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            JLabel nom = new JLabel(equipes[i].nomEquipe);
            nom.setFont(POLICE_LIGNE);
            nom.setForeground(couleur);
            nom.setPreferredSize(new Dimension(180, 25));
            panel.add(nom, gbc);

            gbc.gridx = 2; gbc.anchor = GridBagConstraints.EAST;
            JLabel score = new JLabel(equipes[i].score + " pts");
            score.setFont(POLICE_RANG);
            score.setForeground(couleur);
            score.setPreferredSize(new Dimension(90, 25));
            panel.add(score, gbc);
        }

        return panel;
    }

    // méthode statique pour l'appeler facilement depuis le Client
    public static void afficher(MessageProtocol.EntreeLeaderboard[] entrees,
                                 MessageProtocol.EntreeEquipe[] equipes) {
        SwingUtilities.invokeLater(() -> new LeaderboardWindow(entrees, equipes).setVisible(true));
    }
}
