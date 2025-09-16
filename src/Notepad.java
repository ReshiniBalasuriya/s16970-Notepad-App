import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Notepad extends JFrame implements ActionListener {
    private JTextArea textArea;
    private JFileChooser fileChooser;
    private File currentFile = null;
    private boolean changed = false;

    public Notepad() {
        setTitle("Simple Notepad");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        initUI();

        // Ask to save when window closed
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });
    }

    private void initUI() {
        // Text area with scroll
        textArea = new JTextArea();
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        // Track changes to the document so we can prompt to save
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed = true; }
            public void removeUpdate(DocumentEvent e) { changed = true; }
            public void changedUpdate(DocumentEvent e) { changed = true; }
        });

        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "text"));

        // --- Menu bar ---
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem miNew = new JMenuItem("New");
        miNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        miNew.setActionCommand("New"); miNew.addActionListener(this); fileMenu.add(miNew);

        JMenuItem miOpen = new JMenuItem("Open...");
        miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        miOpen.setActionCommand("Open"); miOpen.addActionListener(this); fileMenu.add(miOpen);

        JMenuItem miSave = new JMenuItem("Save");
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        miSave.setActionCommand("Save"); miSave.addActionListener(this); fileMenu.add(miSave);

        JMenuItem miSaveAs = new JMenuItem("Save As...");
        miSaveAs.setActionCommand("SaveAs"); miSaveAs.addActionListener(this); fileMenu.add(miSaveAs);

        fileMenu.addSeparator();
        JMenuItem miExit = new JMenuItem("Exit");
        miExit.setActionCommand("Exit"); miExit.addActionListener(this); fileMenu.add(miExit);

        menuBar.add(fileMenu);

        // Edit menu: Cut / Copy / Paste / Select All
        JMenu editMenu = new JMenu("Edit");
        JMenuItem miCut = new JMenuItem("Cut");
        miCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        miCut.setActionCommand("Cut"); miCut.addActionListener(this); editMenu.add(miCut);

        JMenuItem miCopy = new JMenuItem("Copy");
        miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        miCopy.setActionCommand("Copy"); miCopy.addActionListener(this); editMenu.add(miCopy);

        JMenuItem miPaste = new JMenuItem("Paste");
        miPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        miPaste.setActionCommand("Paste"); miPaste.addActionListener(this); editMenu.add(miPaste);

        JMenuItem miSelectAll = new JMenuItem("Select All");
        miSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        miSelectAll.setActionCommand("SelectAll"); miSelectAll.addActionListener(this); editMenu.add(miSelectAll);

        menuBar.add(editMenu);

        // Optional: Format menu (Font and Color chooser)
        JMenu formatMenu = new JMenu("Format");
        JMenuItem miFont = new JMenuItem("Font...");
        miFont.setActionCommand("Font"); miFont.addActionListener(this); formatMenu.add(miFont);

        JMenuItem miColor = new JMenuItem("Color...");
        miColor.setActionCommand("Color"); miColor.addActionListener(this); formatMenu.add(miColor);

        menuBar.add(formatMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem miAbout = new JMenuItem("About");
        miAbout.setActionCommand("About"); miAbout.addActionListener(this); helpMenu.add(miAbout);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    // Action handler for menu items
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            switch (cmd) {
                case "New": newFile(); break;
                case "Open": openFile(); break;
                case "Save": saveFile(); break;
                case "SaveAs": saveFileAs(); break;
                case "Exit": exitApp(); break;
                case "Cut": textArea.cut(); break;
                case "Copy": textArea.copy(); break;
                case "Paste": textArea.paste(); break;
                case "SelectAll": textArea.selectAll(); break;
                case "About": showAbout(); break;
                case "Font": showFontDialog(); break;
                case "Color": showColorChooser(); break;
                default:
                    // nothing
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Create new (ask to save if changed)
    private void newFile() {
        if (!confirmSaveIfNeeded()) return;
        textArea.setText("");
        currentFile = null;
        changed = false;
        setTitle("Simple Notepad - Untitled");
    }

    // Open file from disk
    private void openFile() {
        if (!confirmSaveIfNeeded()) return;
        int ret = fileChooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                textArea.setText(sb.toString());
                currentFile = f;
                changed = false;
                setTitle("Simple Notepad - " + f.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Could not open file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Save (if file known) otherwise Save As
    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
            return;
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(currentFile))) {
            bw.write(textArea.getText());
            changed = false;
            setTitle("Simple Notepad - " + currentFile.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Save As dialog
    private void saveFileAs() {
        int ret = fileChooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = fileChooser.getSelectedFile();
            // If no extension, add .txt (optional)
            if (!f.getName().contains(".")) {
                f = new File(f.getAbsolutePath() + ".txt");
            }
            currentFile = f;
            saveFile();
        }
    }

    // Ask to save if there are unsaved changes. Returns false if user cancelled.
    private boolean confirmSaveIfNeeded() {
        if (!changed) return true;
        int ans = JOptionPane.showConfirmDialog(this, "You have unsaved changes. Save now?", "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans == JOptionPane.CANCEL_OPTION || ans == JOptionPane.CLOSED_OPTION) return false;
        if (ans == JOptionPane.YES_OPTION) {
            saveFile();
            return !changed; // if still changed then save failed and we return false
        }
        return true; // NO chosen - continue without saving
    }

    // Exit application (ask to save)
    private void exitApp() {
        if (!confirmSaveIfNeeded()) return;
        dispose();
        System.exit(0);
    }

    // About dialog
    private void showAbout() {
        String about = "Simple Notepad\n\nAuthor: Reshini Balasuriya\nID: 2022s19554\n\nA basic Java Swing notepad.";
        JOptionPane.showMessageDialog(this, about, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    // Font chooser (simple custom dialog)
    private void showFontDialog() {
        JDialog dlg = new JDialog(this, "Choose Font", true);
        dlg.setSize(450, 360);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(6,6));

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        JList<String> fontList = new JList<>(fonts);
        fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fontList.setSelectedValue(textArea.getFont().getFamily(), true);

        String[] styles = {"Plain", "Bold", "Italic", "Bold Italic"};
        JComboBox<String> styleBox = new JComboBox<>(styles);
        int curStyle = textArea.getFont().getStyle();
        if (curStyle == Font.PLAIN) styleBox.setSelectedIndex(0);
        else if (curStyle == Font.BOLD) styleBox.setSelectedIndex(1);
        else if (curStyle == Font.ITALIC) styleBox.setSelectedIndex(2);
        else styleBox.setSelectedIndex(3);

        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(textArea.getFont().getSize(), 6, 72, 1));

        JPanel top = new JPanel(new BorderLayout(4,4));
        top.add(new JScrollPane(fontList), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.add(new JLabel("Style:")); controls.add(styleBox);
        controls.add(new JLabel("Size:")); controls.add(sizeSpinner);

        top.add(controls, BorderLayout.SOUTH);

        dlg.add(top, BorderLayout.CENTER);

        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        JPanel bottom = new JPanel();
        bottom.add(btnOk); bottom.add(btnCancel);
        dlg.add(bottom, BorderLayout.SOUTH);

        btnOk.addActionListener(ae -> {
            String selectedFont = fontList.getSelectedValue();
            int style = styleBox.getSelectedIndex();
            int size = (Integer) sizeSpinner.getValue();
            Font f = new Font(selectedFont, style, size);
            textArea.setFont(f);
            dlg.dispose();
        });

        btnCancel.addActionListener(ae -> dlg.dispose());
        dlg.setVisible(true);
    }

    // Color chooser for text color
    private void showColorChooser() {
        Color c = JColorChooser.showDialog(this, "Choose Text Color", textArea.getForeground());
        if (c != null) textArea.setForeground(c);
    }

    // Main
    public static void main(String[] args) {
        // Run GUI in Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                // Use system look and feel for nicer appearance
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            Notepad n = new Notepad();
            n.setVisible(true);
        });
    }
}
