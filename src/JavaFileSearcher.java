import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JavaFileSearcher extends JFrame {

    // Initialize the components
    // Swing components
    private final JTextField fileNameField = new JTextField(20);
    //private final JTextField destinationField = new JTextField(20);
    private final JTextField entryField = new JTextField(50);
    private final JButton searchButton = new JButton("Rechercher");
    //private JButton selectButton;
    private final JButton pauseButton = new JButton("Pause");
    private final JButton stopButton = new JButton("Arrêt");
    private final JButton advancedButton = new JButton("Mode Avancé");
    private final JButton entryButton = new JButton("Entrer");
    private final JTextArea messageArea = new JTextArea();
    private final JCheckBox copyCheckBox = new JCheckBox("Copier les fichiers dans un dossier");
    private final JCheckBox searchMustMatchesCheckBox = new JCheckBox("La recherche doit correspondre au nom du fichier");
    private final JPanel searchResultsPanel = new JPanel();
    private final JScrollPane searchResultsScrollPane = new JScrollPane(searchResultsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private final JMenu fileSearcherMenu = new JMenu("File Searcher");
    private final JMenu optionsMenu = new JMenu("Options");
    private final JMenuBar jMenuBar = new JMenuBar();
    private final JMenuItem destinationFieldItem = new JMenuItem("Modifier le dossier de destination");
    private JFrame searchResultsFrame;

    // Search parameters
    private String searchInput;
    private String destination;
    private boolean advancedMode;
    private boolean copyOption;
    private boolean searchMustMatches;

    // Search state
    private boolean searching;
    private volatile boolean paused;
    private volatile boolean puttingEntry;

    // Search thread
    private final Runnable searchRunnable;

    private Instant whenSearchBegan;
    private final List<File> foundFiles = new ArrayList<>();

    {
        searchResultsPanel.setLayout(new BoxLayout(searchResultsPanel, BoxLayout.PAGE_AXIS));

        searchRunnable = () -> {
            appendMessage("Début de la recherche...");

            whenSearchBegan = Instant.now();

            try {
                File[] roots = File.listRoots();
                for (File root : roots) {
                    if (searching) {
                        search(root);
                    } else {
                        return;
                    }
                }

                if (searching) {
                    // If the search is not stopped, show a message that the search is finished
                    appendMessage("Recherche finie en " + (Instant.now().getEpochSecond() - whenSearchBegan.getEpochSecond()) + " secondes.");

                    if (foundFiles.size() > 0) {
                        appendMessage(foundFiles.size() + " fichiers trouvés.");
                        //appendMessage(foundFiles.size() + "Voulez-vous les voir, les ouvrir ou les afficher dans l'explorateur de fichier (vous tapez : voir / ouvrir / afficher) ? Vous pouvez également recommencer une recherche avec le bouton Rechercher.");

                        //String entry = waitEntry();
//
                        //appendMessage(entry);
//
                        //switch (entry) {
                        //    case "voir":
                        //        for (File file : foundFiles) {
                        //            appendMessage("nom : %s, chemin d'accès : %s, taille : %d octets".formatted(file.getName(), file.getAbsolutePath(), file.length()));
                        //        }
                        //        break;
                        //    case "ouvrir":
                        //        for (File file : foundFiles) {
                        //            openFile(file);
                        //        }
                        //        break;
                        //    case "afficher":
                        //        for (File file : foundFiles) {
                        //            openFileInManager(file);
                        //        }
                        //        break;
                        //    default:
//
                        //}
                    } else appendMessage("Aucun fichier trouvé.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Reset the search state and buttons
                searching = false;
                paused = false;
                pauseButton.setEnabled(false);
                stopButton.setEnabled(false);
                searchButton.setEnabled(true);
            }
        };
    }

    public JavaFileSearcher() {
        // Initialize the frame
        super("File Searcher");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //destinationField.setEnabled(false);
        destinationFieldItem.setEnabled(false);
        entryField.setEnabled(false);
        entryButton.setEnabled(false);
        messageArea.setEditable(false);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        // Add action listeners to the buttons
        searchButton.addActionListener(e -> startSearch());

        pauseButton.addActionListener(e -> pauseSearch());

        stopButton.addActionListener(e -> stopSearch());

        advancedButton.addActionListener(e -> toggleAdvancedMode());

        entryButton.addActionListener(e -> putEntry());

        copyCheckBox.addActionListener(e -> toggleCopyOption());

        searchMustMatchesCheckBox.addActionListener(e -> toggleMatchesSearchOption());

        // Disable the pause and stop buttons initially
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        copyOption = false;

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel generalPanel = new JPanel();

        generalPanel.add(new JLabel("Terme de recherche :"));
        generalPanel.add(fileNameField);
        generalPanel.add(searchButton);
        generalPanel.add(pauseButton);
        generalPanel.add(stopButton);

        tabbedPane.addTab("Général", generalPanel);

        JPanel optionsPanel = new JPanel();

        optionsPanel.add(advancedButton);
        optionsPanel.add(copyCheckBox);
        //optionsPanel.add(new JLabel("Destination :"));
        //optionsPanel.add(destinationField);
        optionsPanel.add(searchMustMatchesCheckBox);

        tabbedPane.addTab("Options", optionsPanel);

        JPanel entryPanel = new JPanel();

        entryPanel.add(entryField);
        entryPanel.add(entryButton);

        add(tabbedPane, BorderLayout.NORTH);

        add(scrollPane, BorderLayout.CENTER);

        add(entryPanel, BorderLayout.SOUTH);

        JMenuItem searchItem = new JMenuItem("Rechercher");
        searchItem.addActionListener(itemEvent -> startSearch());

        JMenuItem exitItem = new JMenuItem("Quitter");
        exitItem.addActionListener(itemEvent -> dispose());

        fileSearcherMenu.add(searchItem);
        fileSearcherMenu.add(exitItem);

        JRadioButtonMenuItem advancedModeItem = new JRadioButtonMenuItem("Mode Avancé");
        advancedModeItem.addItemListener(itemEvent -> toggleAdvancedMode());

        JRadioButtonMenuItem copyOptionItem = new JRadioButtonMenuItem("Copier les fichiers dans un dossier");
        copyOptionItem.addItemListener(itemEvent -> toggleCopyOption());

        destinationFieldItem.addActionListener(actionEvent -> destination = JOptionPane.showInputDialog("Veuillez entrer la destination des fichiers trouvés."));

        optionsMenu.add(advancedModeItem);
        optionsMenu.add(copyOptionItem);
        optionsMenu.add(destinationFieldItem);

        jMenuBar.add(fileSearcherMenu);
        jMenuBar.add(optionsMenu);

        setJMenuBar(jMenuBar);

        // Set the frame visible
        setVisible(true);

        //JDialog jDialog = new JDialog();
        //jDialog.setLayout(new BorderLayout());
        //JTextArea jTextArea = new JTextArea("Ce programme cherche un fichier dans tout votre appareil (stockage externe compris) puis le déplace dans le dossier choisi.");
        //jTextArea.setEditable(false);
        //jTextArea.setLineWrap(true);
        //jTextArea.setWrapStyleWord(true);
        //jTextArea.setFocusable(false);
        //jDialog.add(jTextArea, BorderLayout.CENTER);
        //JButton jButton = new JButton("OK");
        //jButton.addActionListener(e -> jDialog.dispose());
        //jDialog.add(jButton, BorderLayout.SOUTH);
        //jDialog.setSize(300, 300);
        //jDialog.setLocationRelativeTo(null);
        //jDialog.setVisible(true);
    }

    // Start a new search
    private void startSearch() {
        // Get the file name and destination from the text fields
        searchInput = fileNameField.getText().trim();
        //if (copyOption) {
        //    destination = destinationField.getText().trim();
        //}

        // Check if they are valid
        if (searchInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Merci d'entrer un terme de recherche.");
            return;
        }

        if (copyOption && destination.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Merci d'entrer une destination (vous avez coché l'option copier vers un dossier).");
            return;
        }

        // Check if the destination is a directory and writable
        if (copyOption) {
            File destFile = new File(destination);

            if (!destFile.isDirectory() || !destFile.canWrite()) {
                JOptionPane.showMessageDialog(this, "Merci d'entrer une destination valide.");
                return;
            }
        }

        // Clear the message area
        messageArea.setText("");

        // Set the search state to true and not paused
        searching = true;
        paused = false;

        // Enable the pause and stop buttons and disable the search button
        pauseButton.setEnabled(true);
        stopButton.setEnabled(true);
        searchButton.setEnabled(false);

        foundFiles.clear();

        if (searchResultsFrame != null) {
            searchResultsFrame.dispose();
        }

        searchResultsPanel.removeAll();

        searchResultsFrame = new JFrame();
        searchResultsFrame.add(searchResultsScrollPane);
        searchResultsFrame.setTitle("Résultats de la recherche");
        searchResultsFrame.setSize(500, 500);
        searchResultsFrame.setLocationRelativeTo(null);
        searchResultsFrame.setVisible(true);

        // Start the thread
        new Thread(searchRunnable).start();
    }

    // Pause or resume the search
    private void pauseSearch() {
        // Toggle the paused state
        paused = !paused;

        // Change the pause button text accordingly
        if (paused) {
            pauseButton.setText("Lecture");
        } else {
            pauseButton.setText("Pause");
        }
    }

    // Stop the search
    private void stopSearch() {
        // Set the search state to false
        searching = false;

        // Show a message that the search is stopped
        appendMessage("Recherche arrêtée.");
    }

    // Toggle the advanced mode
    private void toggleAdvancedMode() {
        // Toggle the advanced mode state
        advancedMode = !advancedMode;

        // Change the advanced button text accordingly
        if (advancedMode) {
            advancedButton.setText("Mode Normal");
        } else {
            advancedButton.setText("Mode Avancé");
        }
    }

    private void putEntry() {
        puttingEntry = true;
    }

    private void toggleCopyOption() {
        // Toggle the copy option state
        copyOption = !copyOption;

        // Enable or disable the destination field accordingly
        //destinationField.setEnabled(copyOption);
        destinationFieldItem.setEnabled(copyOption);
    }

    private void toggleMatchesSearchOption() {
        // Toggle the copy option state
        searchMustMatches = !searchMustMatches;
    }

    // Search for the file in a given directory recursively
    private void search(File dir) throws IOException {
        // Check if the search is still running
        if (!searching) {
            return;
        }

        // Check if the directory is readable
        if (!dir.canRead()) {
            return;
        }

        // Check if the search is paused and wait until it is resumed
        while (paused) {
            Thread.onSpinWait();
        }

        // Show the current directory in advanced mode
        if (advancedMode) {
            appendMessage("Recherche dans " + dir.getAbsolutePath());
        }

        // List all the files and subdirectories in the current directory
        File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {
            if (!searching) {
                return;
            }

            // Check if the file name matches the search query
            String fileName = file.getName();
            if ((fileName.contains(searchInput) && !searchMustMatches) || fileName.equals(searchInput)) {
                // Show a message that the file is found
                appendMessage("Trouvé en " + (Instant.now().getEpochSecond() - whenSearchBegan.getEpochSecond()) + " secondes " + file.getAbsolutePath());

                foundFiles.add(file);

                addSearchResult(file);

                if (copyOption) {
                    // Copy the file to the destination without deleting the original
                    copyFile(file, new File(destination, searchInput));

                    // Show a message that the file is copied
                    appendMessage("Copié " + file.getName() + " dans " + destination);
                }

                //// Open the file in the system file explorer
                //openFile(file);

                //// Show a message that the file is opened
                //appendMessage("Ouvert " + file.getAbsolutePath());
            }

            // If the file is a directory, search recursively in it
            if (file.isDirectory()) {
                search(file);
            }
        }
    }

    // Copy a file from source to target using Java NIO
    private void copyFile(File source, File target) throws IOException {
        Path sourcePath = source.toPath();
        Path targetPath = target.toPath();

        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void openFile(File file) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.open(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openFileInManager(File file) throws IOException {
        if (Desktop.isDesktopSupported()) {
            //List<String> args = new ArrayList<>();
            //args.add("explorer.exe");
            //args.add("/select");
            //args.add("\"" + file.getAbsolutePath() + "\"");
            //new ProcessBuilder(args).start();
            Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,\"" + file.getAbsolutePath() + "\""});
            System.out.println(file.getAbsolutePath());
        }
    }

    // Append a message to the message area with a new line
    private void appendMessage(String message) {
        messageArea.append(message + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private String waitEntry() {
        entryField.setEnabled(true);
        entryButton.setEnabled(true);

        while (!puttingEntry) {
            Thread.onSpinWait();
        }

        entryField.setEnabled(false);
        entryButton.setEnabled(false);
        puttingEntry = false;

        String entry = entryField.getText();

        entryField.setText("");

        return entry;

        //int linesNumberBeforeEntry = messageArea.getLineCount();
        //
        //messageArea.setEditable(true);
        //
        //while (!(messageArea.getLineCount() > linesNumberBeforeEntry)) {
        //    Thread.onSpinWait();
        //}
        //
        //messageArea.setEditable(false);
        //
        //return messageArea.getText().split("\n")[messageArea.getLineCount()];
    }

    private void addSearchResult(File file) {
        JPanel jPanel = new JPanel();
        JLabel jLabel = new JLabel(file.getName());
        jLabel.setBackground(Color.GRAY);
        jPanel.add(jLabel);
        final boolean[] isExtend = {false};
        JLabel jLabel2 = new JLabel("Cliquez pour voir le chemin");
        jLabel2.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (isExtend[0]) {
                    jLabel2.setText("Cliquez pour voir le chemin");
                    isExtend[0] = false;
                } else {
                    jLabel2.setText(file.getParent());
                    isExtend[0] = true;
                }
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {

            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {

            }
        });
        jPanel.add(jLabel2);
        JButton jButton = new JButton("Ouvrir dans le dossier");
        jButton.addActionListener(actionEvent -> {
            try {
                openFileInManager(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        JButton jButton2 = new JButton("Ouvrir");
        jButton2.addActionListener(actionEvent -> {
            try {
                openFile(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        jPanel.add(jButton);
        jPanel.add(jButton2);
        searchResultsPanel.add(jPanel);
        searchResultsPanel.updateUI();
    }

    public static void main(String[] args) {
        new JavaFileSearcher();
    }

    private static class MessageArea extends JTextArea {
        private boolean waitingEntry = false;
        private String textBeforeEntry;

        @Override
        public void updateUI() {
            super.updateUI();

            if (waitingEntry) {
                String[] linesBeforeEntry = textBeforeEntry.split("\n");


            } else textBeforeEntry = getText();
        }

        public void setWaitingEntry(boolean waitingEntry) {
            this.waitingEntry = waitingEntry;
        }
    }
}
