import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

class WordCounterSwing {

    private static final Map<String, Set<String>> STOP_WORDS_MAP = new HashMap<>();

    static {
        STOP_WORDS_MAP.put("English", new HashSet<>(Arrays.asList(
                "a", "an", "the", "and", "or", "but", "is", "are", "was", "were", "it", "they", "of", "to", "in", "on", "for", "with", "about", "as", "by"
        )));
        STOP_WORDS_MAP.put("Spanish", new HashSet<>(Arrays.asList(
                "un", "una", "el", "la", "y", "o", "pero", "es", "son", "fue", "fueron", "lo", "ellos", "de", "para", "en", "sobre", "con", "como", "por"
        )));
        // Add more languages as needed
    }

    private JTextArea resultArea;
    private JTextField fileInputField;
    private JCheckBox caseSensitiveCheckbox;
    private JComboBox<String> languageComboBox;
    private JProgressBar progressBar;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WordCounterSwing().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Advanced Word Counter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        frame.setLayout(new BorderLayout());

        JPanel filePanel = new JPanel();
        filePanel.add(new JLabel("Select Input Files:"));
        fileInputField = new JTextField(30);
        filePanel.add(fileInputField);
        JButton browseButton = new JButton("Browse");
        filePanel.add(browseButton);

        JPanel casePanel = new JPanel();
        casePanel.add(new JLabel("Case Sensitive:"));
        caseSensitiveCheckbox = new JCheckBox();
        casePanel.add(caseSensitiveCheckbox);

        JPanel languagePanel = new JPanel();
        languagePanel.add(new JLabel("Select Language:"));
        languageComboBox = new JComboBox<>(new String[]{"English", "Spanish"});
        languagePanel.add(languageComboBox);

        JPanel bottomPanel = new JPanel();
        JButton analyzeButton = new JButton("Analyze");
        analyzeButton.addActionListener(new AnalyzeActionListener());
        bottomPanel.add(analyzeButton);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        bottomPanel.add(progressBar);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        frame.add(filePanel, BorderLayout.NORTH);
        frame.add(casePanel, BorderLayout.CENTER);
        frame.add(languagePanel, BorderLayout.WEST);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                StringBuilder sb = new StringBuilder();
                for (File file : selectedFiles) {
                    sb.append(file.getAbsolutePath()).append("\n");
                }
                fileInputField.setText(sb.toString().trim());
            }
        });

        frame.setVisible(true);
    }

    private class AnalyzeActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String filePaths = fileInputField.getText();
            if (filePaths.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please select files.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedLanguage = (String) languageComboBox.getSelectedItem();
            Set<String> stopWords = STOP_WORDS_MAP.get(selectedLanguage);

            String[] paths = filePaths.split("\n");
            Map<String, Integer> wordCounts = new HashMap<>();
            Map<Character, Integer> letterCounts = new HashMap<>();

            progressBar.setVisible(true);

            for (int i = 0; i < paths.length; i++) {
                File file = new File(paths[i].trim());
                if (file.exists()) {
                    countWordsAndLettersInFile(file, wordCounts, letterCounts, stopWords);
                } else {
                    JOptionPane.showMessageDialog(null, "File not found: " + paths[i].trim(), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                progressBar.setValue((i + 1) * 100 / paths.length);  // Update progress bar
            }

            boolean caseSensitive = caseSensitiveCheckbox.isSelected();
            if (!caseSensitive) {
                wordCounts = convertWordCountsToLowerCase(wordCounts);
                letterCounts = convertLetterCountsToLowerCase(letterCounts);
            }

            removeStopWords(wordCounts, stopWords);
            String output = formatWordCountsAndLetters(wordCounts, letterCounts);
            resultArea.setText(output);

            progressBar.setVisible(false);  // Hide progress bar
        }
    }

    private void countWordsAndLettersInFile(File file, Map<String, Integer> wordCounts, Map<Character, Integer> letterCounts, Set<String> stopWords) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\W+");
                for (String word : words) {
                    if (!word.isEmpty() && !stopWords.contains(word.toLowerCase())) {
                        word = word.toLowerCase();
                        wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                    }
                }

                // Count letters (excluding non-alphabetical characters)
                for (char c : line.toCharArray()) {
                    if (Character.isLetter(c)) {
                        char lowerChar = Character.toLowerCase(c);
                        letterCounts.put(lowerChar, letterCounts.getOrDefault(lowerChar, 0) + 1);
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading the file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Map<String, Integer> convertWordCountsToLowerCase(Map<String, Integer> wordCounts) {
        Map<String, Integer> lowerCaseCounts = new HashMap<>();
        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
            lowerCaseCounts.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return lowerCaseCounts;
    }

    private Map<Character, Integer> convertLetterCountsToLowerCase(Map<Character, Integer> letterCounts) {
        Map<Character, Integer> lowerCaseCounts = new HashMap<>();
        for (Map.Entry<Character, Integer> entry : letterCounts.entrySet()) {
            lowerCaseCounts.put(Character.toLowerCase(entry.getKey()), entry.getValue());
        }
        return lowerCaseCounts;
    }

    private void removeStopWords(Map<String, Integer> wordCounts, Set<String> stopWords) {
        wordCounts.keySet().removeIf(word -> stopWords.contains(word));
    }

    private String formatWordCountsAndLetters(Map<String, Integer> wordCounts, Map<Character, Integer> letterCounts) {
        if (wordCounts.isEmpty() && letterCounts.isEmpty()) {
            return "No words or letters found after processing the files (or they were all stop words).";
        }

        StringBuilder output = new StringBuilder("Word Counts:\n");
        wordCounts.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .forEach(entry -> output.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        int totalWords = wordCounts.values().stream().mapToInt(Integer::intValue).sum();
        output.append("\nTotal Words (excluding stop words): ").append(totalWords);

        output.append("\n\nLetter Counts:\n");
        letterCounts.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .forEach(entry -> output.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        int totalLetters = letterCounts.values().stream().mapToInt(Integer::intValue).sum();
        output.append("\nTotal Letters: ").append(totalLetters);

        return output.toString();
    }
}
