import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import personality.Personality;

/**
 * Modern settings window with tabbed interface and background image.
 * UPDATED: Now features background image and proper image scaling.
 */
public class SettingsWindow extends JFrame {

    // Background image for the entire window
    private Image backgroundImage;

    public SettingsWindow(String[] voices) {
        setTitle("AI Assistant Settings");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setResizable(false);

        // Load background image
        loadBackgroundImage();

        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default if system LAF not available
        }

        // Main panel with background
        JPanel mainPanel = createBackgroundPanel();
        mainPanel.setLayout(new BorderLayout());

        // Title panel with image
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // Tabbed pane for different sections
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 12));
        tabbedPane.setOpaque(false); // Make transparent to show background
        tabbedPane.setBackground(new Color(0, 0, 0, 0)); // Fully transparent
        tabbedPane.setForeground(new Color(200, 100, 200)); // Pink text for tabs

        // Make tab content area transparent
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Don't paint content border to keep transparent
            }
            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
                // Make tab backgrounds transparent
                Graphics2D g2d = (Graphics2D) g.create();
                if (isSelected) {
                    g2d.setColor(new Color(150, 100, 200, 100)); // Semi-transparent purple for selected tab
                } else {
                    g2d.setColor(new Color(80, 80, 80, 80)); // Semi-transparent gray for unselected tabs
                }
                g2d.fillRect(x, y, w, h);
                g2d.dispose();
            }
            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setFont(font);
                g2d.setColor(new Color(200, 100, 200)); // Pink color for tab text
                g2d.drawString(title, textRect.x, textRect.y + metrics.getAscent());
                g2d.dispose();
            }
        });

        // Main tab - Voice and Personality
        JPanel mainTab = createMainTab(voices);
        tabbedPane.addTab("Main", new ImageIcon(), mainTab, "Voice and Personality settings");

        // Settings tab - Advanced options
        JPanel settingsTab = createSettingsTab();
        tabbedPane.addTab("Settings", new ImageIcon(), settingsTab, "Advanced model settings");

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Control panel with start/stop button
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadBackgroundImage() {
        try {
            File backgroundFile = new File("data/system/images/background.png");
            if (backgroundFile.exists()) {
                backgroundImage = ImageIO.read(backgroundFile);
            }
        } catch (IOException e) {
            System.err.println("Could not load background image: " + e.getMessage());
            backgroundImage = null;
        }
    }

    private JPanel createBackgroundPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                    g2d.dispose();
                }
            }
        };
    }

    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false); // Make transparent to show background
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Load title image
        JLabel imageLabel = new JLabel();
        try {
            File imageFile = new File("data/system/images/title.png");
            if (imageFile.exists()) {
                Image img = ImageIO.read(imageFile);
                // Keep square aspect ratio for 1024x1024 image
                Image scaledImg = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImg));
            } else {
                // Fallback text if image not found
                imageLabel.setText("AI Assistant Settings");
                imageLabel.setFont(new Font("Arial", Font.BOLD, 24));
                imageLabel.setForeground(Color.WHITE);
            }
        } catch (IOException e) {
            // Fallback text if image loading fails
            imageLabel.setText("AI Assistant Settings");
            imageLabel.setFont(new Font("Arial", Font.BOLD, 24));
            imageLabel.setForeground(Color.WHITE);
        }

        imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(imageLabel);

        return titlePanel;
    }

    private JPanel createMainTab(String[] voices) {
        JPanel mainTab = new JPanel();
        mainTab.setLayout(new BoxLayout(mainTab, BoxLayout.Y_AXIS));
        mainTab.setOpaque(false); // Make transparent to show background
        mainTab.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Voice section
        JPanel voiceSection = createSection("Assistant Voice", createVoicePanel(voices));
        mainTab.add(voiceSection);
        mainTab.add(Box.createVerticalStrut(20));

        // Personality section
        JPanel personalitySection = createSection("Personality", createPersonalityPanel());
        mainTab.add(personalitySection);

        // Add flexible space at the bottom
        mainTab.add(Box.createVerticalGlue());

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(mainTab);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Create wrapper panel to return
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createSettingsTab() {
        JPanel settingsTab = new JPanel();
        settingsTab.setLayout(new BoxLayout(settingsTab, BoxLayout.Y_AXIS));
        settingsTab.setOpaque(false); // Make transparent to show background
        settingsTab.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Language section
        JPanel languageSection = createSection("Language", createLanguagePanel());
        settingsTab.add(languageSection);
        settingsTab.add(Box.createVerticalStrut(20));

        // Vision Model section
        JPanel visionSection = createSection("Vision Model", createVisionModelPanel());
        settingsTab.add(visionSection);
        settingsTab.add(Box.createVerticalStrut(20));

        // Analysis Model section
        JPanel analysisSection = createSection("Analysis Model", createAnalysisModelPanel());
        settingsTab.add(analysisSection);

        // Add flexible space at the bottom
        settingsTab.add(Box.createVerticalGlue());

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(settingsTab);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Create wrapper panel to return
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createSection(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout());
        // Semi-transparent dark background for readability
        section.setBackground(new Color(40, 40, 40, 200)); // Dark with transparency
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 100, 200, 180), 2), // Purple border
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE); // White text for dark mode
        section.add(titleLabel, BorderLayout.NORTH);

        section.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        section.add(content, BorderLayout.SOUTH);

        return section;
    }

    private JPanel createVoicePanel(String[] voices) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false); // Transparent to match section

        JComboBox<String> voiceSelector = new JComboBox<>(voices);
        voiceSelector.setSelectedItem(AppState.selectedTtsCharacterVoice);
        voiceSelector.setPreferredSize(new Dimension(200, 30));
        panel.add(voiceSelector);

        voiceSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                AppState.selectedTtsCharacterVoice = (String) e.getItem();
                System.out.println("Voice changed to: " + AppState.selectedTtsCharacterVoice);
                AppState.saveCurrentSettings();
            }
        });

        return panel;
    }

    private JPanel createPersonalityPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false); // Transparent to match section

        List<Personality> personalities = AppState.getAvailablePersonalities();

        if (personalities.isEmpty()) {
            JLabel noPersonalitiesLabel = new JLabel("No personalities found. Check data folder.");
            noPersonalitiesLabel.setForeground(Color.WHITE); // White text for dark mode
            panel.add(noPersonalitiesLabel);
            return panel;
        }

        ButtonGroup personalityGroup = new ButtonGroup();

        for (Personality personality : personalities) {
            JRadioButton radioButton = new JRadioButton(personality.getName());
            radioButton.setOpaque(false); // Transparent background
            radioButton.setForeground(Color.WHITE); // White text for dark mode
            radioButton.setFont(new Font("Arial", Font.PLAIN, 12));
            personalityGroup.add(radioButton);

            // Set selected if this is the current personality
            Personality selectedPersonality = AppState.getSelectedPersonality();
            if (selectedPersonality != null &&
                    personality.getName().equals(selectedPersonality.getName())) {
                radioButton.setSelected(true);
            }

            radioButton.addActionListener(e -> {
                if (radioButton.isSelected()) {
                    AppState.setSelectedPersonality(personality);
                }
            });

            panel.add(radioButton);
        }

        return panel;
    }

    private JPanel createLanguagePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false); // Transparent to match section

        String[] languages = {"English", "Japanese", "Chinese"};
        JComboBox<String> languageSelector = new JComboBox<>(languages);
        languageSelector.setSelectedItem(AppState.selectedLanguage);
        languageSelector.setPreferredSize(new Dimension(200, 30));
        panel.add(languageSelector);

        languageSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                AppState.selectedLanguage = (String) e.getItem();
                System.out.println("Language changed to: " + AppState.selectedLanguage);
                AppState.saveCurrentSettings();
            }
        });

        return panel;
    }

    private JPanel createVisionModelPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false); // Transparent to match section

        ButtonGroup visionModelGroup = new ButtonGroup();

        JRadioButton localVisionButton = new JRadioButton("Local");
        JRadioButton apiVisionButton = new JRadioButton("API");

        localVisionButton.setOpaque(false);
        localVisionButton.setForeground(Color.WHITE);
        apiVisionButton.setOpaque(false);
        apiVisionButton.setForeground(Color.WHITE);

        visionModelGroup.add(localVisionButton);
        visionModelGroup.add(apiVisionButton);

        // Set initial selection based on current state
        if (AppState.useApiVision()) {
            apiVisionButton.setSelected(true);
        } else {
            localVisionButton.setSelected(true);
        }

        // Disable API option if configuration is not available
        if (!AppState.isVisionApiConfigAvailable()) {
            apiVisionButton.setEnabled(false);
            apiVisionButton.setToolTipText("Vision API configuration not available in data/system/system.json");
        }

        localVisionButton.addActionListener(e -> {
            if (localVisionButton.isSelected()) {
                AppState.setUseApiVision(false);
            }
        });

        apiVisionButton.addActionListener(e -> {
            if (apiVisionButton.isSelected()) {
                AppState.setUseApiVision(true);
            }
        });

        panel.add(localVisionButton);
        panel.add(apiVisionButton);

        return panel;
    }

    private JPanel createAnalysisModelPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false); // Transparent to match section

        ButtonGroup analysisModelGroup = new ButtonGroup();

        JRadioButton localAnalysisButton = new JRadioButton("Local");
        JRadioButton apiAnalysisButton = new JRadioButton("API");

        localAnalysisButton.setOpaque(false);
        localAnalysisButton.setForeground(Color.WHITE);
        apiAnalysisButton.setOpaque(false);
        apiAnalysisButton.setForeground(Color.WHITE);

        analysisModelGroup.add(localAnalysisButton);
        analysisModelGroup.add(apiAnalysisButton);

        // Set initial selection based on current state
        if (AppState.useApiAnalysis()) {
            apiAnalysisButton.setSelected(true);
        } else {
            localAnalysisButton.setSelected(true);
        }

        // Disable API option if configuration is not available
        if (!AppState.isAnalysisApiConfigAvailable()) {
            apiAnalysisButton.setEnabled(false);
            apiAnalysisButton.setToolTipText("Analysis API configuration not available in data/system/system.json");
        }

        localAnalysisButton.addActionListener(e -> {
            if (localAnalysisButton.isSelected()) {
                AppState.setUseApiAnalysis(false);
            }
        });

        apiAnalysisButton.addActionListener(e -> {
            if (apiAnalysisButton.isSelected()) {
                AppState.setUseApiAnalysis(true);
            }
        });

        panel.add(localAnalysisButton);
        panel.add(apiAnalysisButton);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setOpaque(false); // Make transparent to show background
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton startStopButton = new JButton("Start Assistant");
        startStopButton.setPreferredSize(new Dimension(150, 35));
        startStopButton.setFont(new Font("Arial", Font.BOLD, 12));

        // Dark mode styling with pink/purple theme
        startStopButton.setBackground(new Color(50, 50, 50)); // Dark background
        startStopButton.setForeground(Color.WHITE); // White text
        startStopButton.setFocusPainted(false);
        startStopButton.setBorder(BorderFactory.createLineBorder(new Color(200, 100, 200), 2)); // Pink/purple border

        // Add hover effect
        startStopButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startStopButton.setBackground(new Color(70, 50, 70)); // Slightly lighter on hover
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (AppState.isRunning) {
                    startStopButton.setBackground(new Color(80, 30, 60)); // Dark red when running
                } else {
                    startStopButton.setBackground(new Color(50, 50, 50)); // Original dark
                }
            }
        });

        startStopButton.addActionListener(e -> {
            if (AppState.isRunning) {
                Main.assistantCore.stopProcessing();
                startStopButton.setText("Start Assistant");
                startStopButton.setBackground(new Color(50, 50, 50)); // Dark background
                startStopButton.setBorder(BorderFactory.createLineBorder(new Color(200, 100, 200), 2)); // Pink border
            } else {
                Main.assistantCore.startProcessing();
                startStopButton.setText("Stop Assistant");
                startStopButton.setBackground(new Color(80, 30, 60)); // Dark red when active
                startStopButton.setBorder(BorderFactory.createLineBorder(new Color(220, 80, 120), 2)); // Brighter pink when active
            }
        });

        controlPanel.add(startStopButton);
        return controlPanel;
    }
}
