import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class TagExtractor extends JFrame {
    private JTextArea resultArea;
    private JButton extractButton;
    private JButton saveButton;
    private JFileChooser fileChooser;
    private File inputFile;
    private File stopWordsFile;
    private Map<String, Integer> tagFrequency;

    public TagExtractor() {
        // Main frame
        this.setTitle("Tag Extractor");
        this.setSize(600, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        //Components inside of the GUI (TextArea w/ ScrollPane, 2 Buttons)
        resultArea = new JTextArea();
        resultArea.setEditable(false);// We don't want user to edit the TextArea
        JScrollPane scrollPane = new JScrollPane(resultArea);// Adding JTextArea to the scrollPane
        this.add(scrollPane, BorderLayout.CENTER);// Adding scrollPane to the frame since scrollPane has JTextArea,

        JPanel buttonPanel = new JPanel();
        extractButton = new JButton("Extract Tags");
        extractButton.setFocusable(false);
        saveButton = new JButton("Save Results");
        saveButton.setFocusable(false);
        saveButton.setEnabled(false);// We don't the user to save without first selecting files
        buttonPanel.add(extractButton);//We need to add buttons from left to right for the order we want.
        buttonPanel.add(saveButton);
        this.add(buttonPanel, BorderLayout.SOUTH);//Setting and adding the buttons at the bottom of the frame.

        fileChooser = new JFileChooser();

        // Set up action listeners
        extractButton.addActionListener((ActionEvent ae) -> selectFiles());
        saveButton.addActionListener((ActionEvent ae) -> saveResults());


        // Initialize tag frequency map
        tagFrequency = new ConcurrentHashMap<>();
    }

    private void selectFiles() {
        // Select input file
        fileChooser.setDialogTitle("Select Input Text File");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            inputFile = fileChooser.getSelectedFile();
        } else {
            return;
        }

        // Select stop words file
        fileChooser.setDialogTitle("Select Stop Words File");
        result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            stopWordsFile = fileChooser.getSelectedFile();
            extractTags();
        }
    }

    private void extractTags() {
        // Run extraction in a separate thread to keep UI responsive
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Set<String> stopWords = loadStopWords();
                processInputFile(stopWords);//What does this do...
                return null;
            }

            @Override
            protected void done() {
                displayResults();
                saveButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    /**
     * This method we are adding the stopWordsFile to our "stopWords" HashSet.
     * @return - Returns a hashSet with all the words(all words are lower case) from the stopWordsFile.
     * @throws IOException
     */
    private Set<String> loadStopWords() throws IOException {
        Set<String> stopWords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(stopWordsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stopWords.add(line.trim().toLowerCase());// We are removing spaces with trim and setting all words from array "line".
            }
        } catch (IOException e) {
            showErrorDialog("Error loading stop words file: " + e.getMessage());
            throw e;
        }
        return stopWords;
    }

    private void processInputFile(Set<String> stopWords) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    // Remove non-letter characters and convert to lowercase
                    word = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
                    if (!word.isEmpty() && !stopWords.contains(word)) {
                        tagFrequency.merge(word, 1, Integer::sum);
                    }
                }
            }
        } catch (IOException e) {
            showErrorDialog("Error processing input file: " + e.getMessage());
            throw e;
        }
    }

    private void displayResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("File: ").append(inputFile.getName()).append("\n\n");
        sb.append("Tags and Frequencies:\n\n");

        // Sort the map by value (frequency) in descending order
        tagFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        resultArea.setText(sb.toString());
    }

    private void saveResults() {
        fileChooser.setDialogTitle("Save Results");
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(file)) {
                tagFrequency.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(entry -> writer.println(entry.getKey() + ": " + entry.getValue()));
                JOptionPane.showMessageDialog(this, "Results saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                showErrorDialog("Error saving results: " + e.getMessage());
            }
        }
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

}