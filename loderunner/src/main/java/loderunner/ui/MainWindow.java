package loderunner.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import loderunner.game.GestionnaireNiveaux;
import loderunner.network.Client;
import loderunner.network.MessageProtocol;
import loderunner.network.Serveur;

/**
 * Fenêtre principale du jeu — menu de démarrage.
 *
 * Propose trois choix :
 *  - Solo      : lance directement GestionnaireNiveaux en local
 *  - Créer     : démarre le serveur TCP (port fixe) et attend des clients
 *  - Rejoindre : demande l'adresse IP du serveur et se connecte en client
 */
public class MainWindow extends JFrame {

    private static final Color COULEUR_TITRE  = new Color(255, 200, 0);
    private static final Color COULEUR_BOUTON = new Color(50, 50, 80);
    private static final Color COULEUR_TEXTE  = Color.WHITE;
    private static final Font  POLICE_TITRE   = new Font("Arial", Font.BOLD, 42);
    private static final Font  POLICE_BOUTON  = new Font("Arial", Font.BOLD, 18);

    private Serveur serveurGlobal;

    public MainWindow() {
        super("Lode Runner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        construireInterface();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ------------------------------------------------------------------ //
    //  Construction de l'interface                                        //
    // ------------------------------------------------------------------ //

    private void construireInterface() {
        JPanel fond = new JPanel(new BorderLayout(0, 30));
        fond.setBackground(getBackground());
        fond.setBorder(BorderFactory.createEmptyBorder(50, 80, 50, 80));
        fond.add(creerTitre(), BorderLayout.NORTH);
        fond.add(creerBoutons(), BorderLayout.CENTER);
        setContentPane(fond);
    }

    private JLabel creerTitre() {
        JLabel titre = new JLabel("LODE RUNNER", SwingConstants.CENTER);
        titre.setFont(POLICE_TITRE);
        titre.setForeground(COULEUR_TITRE);
        titre.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        return titre;
    }

    private JPanel creerBoutons() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(getBackground());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.insets  = new Insets(10, 0, 10, 0);
        gbc.weightx = 1.0;
        gbc.gridx   = 0;

        gbc.gridy = 0;
        panel.add(creerBouton("Jouer en solo", this::lancerSolo), gbc);

        gbc.gridy = 1;
        panel.add(creerBouton("Créer une partie", this::lancerServeur), gbc);

        gbc.gridy = 2;
        panel.add(creerBouton("Rejoindre une partie", this::rejoindrePartie), gbc);

        return panel;
    }

    private JButton creerBouton(String texte, ActionListener action) {
        JButton btn = new JButton(texte);
        btn.setFont(POLICE_BOUTON);
        btn.setForeground(COULEUR_TEXTE);
        btn.setBackground(COULEUR_BOUTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(300, 55));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(COULEUR_BOUTON.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(COULEUR_BOUTON); }
        });

        btn.addActionListener(action);
        return btn;
    }

    // ------------------------------------------------------------------ //
    //  Actions des boutons                                                //
    // ------------------------------------------------------------------ //

    private void lancerSolo(ActionEvent e) {
        dispose();
        InputHandler ih = new InputHandler();
        JFrame frame = new JFrame("Lode Runner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GestionnaireNiveaux gestionnaire = new GestionnaireNiveaux(frame, ih);
        gestionnaire.demarrer();
    }

    private void lancerServeur(ActionEvent e) {
        dispose();

        // Démarrer le serveur en arrière-plan (il accepte les connexions immédiatement)
        serveurGlobal = new Serveur(MessageProtocol.PORT_DEFAUT);
        new Thread(() -> serveurGlobal.demarrerDepuisUI(), "Thread-Serveur").start();

        // Fenêtre d'attente avec IP et bouton "Lancer"
        JFrame attente = new JFrame("Lode Runner — Serveur");
        attente.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(getBackground());
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridx  = 0;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JLabel titreLbl = new JLabel("Partie créée — en attente de joueurs", SwingConstants.CENTER);
        titreLbl.setFont(new Font("Arial", Font.BOLD, 18));
        titreLbl.setForeground(COULEUR_TITRE);
        gbc.gridy = 0; panel.add(titreLbl, gbc);

        JLabel portLbl = new JLabel("Port : " + MessageProtocol.PORT_DEFAUT, SwingConstants.CENTER);
        portLbl.setFont(new Font("Arial", Font.PLAIN, 15));
        portLbl.setForeground(COULEUR_TEXTE);
        gbc.gridy = 1; panel.add(portLbl, gbc);

        JLabel ipLbl = new JLabel("Ton IP locale : " + obtenirIpLocale(), SwingConstants.CENTER);
        ipLbl.setFont(new Font("Arial", Font.BOLD, 15));
        ipLbl.setForeground(new Color(100, 220, 100));
        gbc.gridy = 2; panel.add(ipLbl, gbc);

        JLabel conseilLbl = new JLabel("Donne cette adresse aux autres joueurs.", SwingConstants.CENTER);
        conseilLbl.setFont(new Font("Arial", Font.ITALIC, 13));
        conseilLbl.setForeground(new Color(180, 180, 180));
        gbc.gridy = 3; panel.add(conseilLbl, gbc);

        gbc.gridy  = 4;
        gbc.insets = new Insets(20, 0, 0, 0);
        panel.add(creerBouton("Lancer la partie", ev -> {
            attente.dispose();
            serveurGlobal.lancerMaintenantDepuisUI();
        }), gbc);

        attente.setContentPane(panel);
        attente.pack();
        attente.setLocationRelativeTo(null);
        attente.setVisible(true);
    }

    private void rejoindrePartie(ActionEvent e) {
        String ip = JOptionPane.showInputDialog(
                this,
                "Adresse IP du serveur :",
                "Rejoindre une partie",
                JOptionPane.QUESTION_MESSAGE);

        if (ip == null || ip.isBlank()) return;

        // choix du rôle : joueur normal ou garde (mode combat)
        String[] options = { "Joueur", "Garde (mode combat)" };
        int choix = JOptionPane.showOptionDialog(
                this,
                "Quel rôle veux-tu jouer ?",
                "Choix du rôle",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choix < 0) return; // annulé

        loderunner.network.MessageProtocol.RoleJoueur role = (choix == 1)
                ? loderunner.network.MessageProtocol.RoleJoueur.GARDE
                : loderunner.network.MessageProtocol.RoleJoueur.JOUEUR;

        dispose();
        Client client = new Client(ip.trim(), MessageProtocol.PORT_DEFAUT, role);
        new Thread(() -> client.connecter(), "Thread-Client").start();
    }

    // ------------------------------------------------------------------ //
    //  Utilitaires                                                        //
    // ------------------------------------------------------------------ //

    private String obtenirIpLocale() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException ex) {
            return "introuvable";
        }
    }

    // ------------------------------------------------------------------ //
    //  Point d'entrée                                                     //
    // ------------------------------------------------------------------ //

    public static void afficher() {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
