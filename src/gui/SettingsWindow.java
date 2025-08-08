package gui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import api.TtsApiClient;
import core.AppState;
import core.Main;
import gui.settings.*;

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
        JTabbedPane tabbedPane = getJTabbedPane();

        // Main tab - Voice and Personality
        JPanel mainTab = createMainTab(voices);
        tabbedPane.addTab("Main", new ImageIcon(), mainTab, "Voice and Personality settings");

        // Settings tab - Advanced options
        JPanel settingsTab = createSettingsTab();
        tabbedPane.addTab("Settings", new ImageIcon(), settingsTab, "Advanced model settings");

        // Profile tab - Levels and Skills
        JPanel profileTab = createProfileTab();
        tabbedPane.addTab("Profile", new ImageIcon(), profileTab, "User attributes & skills");

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Control panel with start/stop button
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private static JTabbedPane getJTabbedPane() {
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
        return tabbedPane;
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
        JPanel voiceSection = createSection("Assistant Voice", new VoicePanel(voices));
        mainTab.add(voiceSection);
        mainTab.add(Box.createVerticalStrut(20));

        // Personality section
        JPanel personalitySection = createSection("Personality", new PersonalityPanel());
        mainTab.add(personalitySection);

        // Add flexible space at the bottom
        mainTab.add(Box.createVerticalGlue());

        // Wrap in scroll pane with improved sensitivity
        JScrollPane scrollPane = new JScrollPane(mainTab);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Increase scroll sensitivity significantly
        scrollPane.getVerticalScrollBar().setUnitIncrement(20); // Increased from default 1
        scrollPane.getVerticalScrollBar().setBlockIncrement(80); // Increased from default 10

        // Custom scrollbar styling
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(200, 100, 200, 150); // Pink semi-transparent thumb
                this.trackColor = new Color(40, 40, 40, 100); // Dark semi-transparent track
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton button = super.createDecreaseButton(orientation);
                button.setBackground(new Color(150, 100, 200, 100));
                button.setBorder(null);
                return button;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton button = super.createIncreaseButton(orientation);
                button.setBackground(new Color(150, 100, 200, 100));
                button.setBorder(null);
                return button;
            }
        });

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
        JPanel languageSection = createSection("Language", new LanguagePanel());
        settingsTab.add(languageSection);
        settingsTab.add(Box.createVerticalStrut(20));

        // Multimodal section
        JPanel multimodalSection = createSection("Multimodal Model", new MultimodalPanel(this::refreshSettingsTab));
        settingsTab.add(multimodalSection);
        settingsTab.add(Box.createVerticalStrut(20));

        // Conditional sections based on multimodal mode
        if (AppState.useMultimodal()) {
            // Show unified Model Settings section
            JPanel modelSection = createSection("Model Settings", new UnifiedModelPanel());
            settingsTab.add(modelSection);
        } else {
            // Show separate Vision and Analysis sections
            JPanel visionSection = createSection("Vision Model", new VisionModelPanel());
            settingsTab.add(visionSection);
            settingsTab.add(Box.createVerticalStrut(20));

            JPanel analysisSection = createSection("Analysis Model", new AnalysisModelPanel());
            settingsTab.add(analysisSection);
        }

        // Add flexible space at the bottom
        settingsTab.add(Box.createVerticalGlue());

        // Wrap in scroll pane with improved sensitivity
        JScrollPane scrollPane = new JScrollPane(settingsTab);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Increase scroll sensitivity significantly
        scrollPane.getVerticalScrollBar().setUnitIncrement(20); // Increased from default 1
        scrollPane.getVerticalScrollBar().setBlockIncrement(80); // Increased from default 10

        // Custom scrollbar styling
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(200, 100, 200, 150); // Pink semi-transparent thumb
                this.trackColor = new Color(40, 40, 40, 100); // Dark semi-transparent track
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                JButton button = super.createDecreaseButton(orientation);
                button.setBackground(new Color(150, 100, 200, 100));
                button.setBorder(null);
                return button;
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                JButton button = super.createIncreaseButton(orientation);
                button.setBackground(new Color(150, 100, 200, 100));
                button.setBorder(null);
                return button;
            }
        });

        // Create wrapper panel to return
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createProfileTab() {
        JPanel tab = new JPanel();
        tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
        tab.setOpaque(false);
        tab.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel statsSection = createSection("Attributes", new ProfilePanel());
        tab.add(statsSection);

        JScrollPane scrollPane = new JScrollPane(tab);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(80);

        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(200, 100, 200, 150);
                this.trackColor = new Color(40, 40, 40, 100);
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSection(String title, JPanel content) {
        return new SectionPanel(title, content);
    }

    // old inline creators replaced by modular panels
    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setOpaque(false); // Make transparent to show background
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton startStopButton = new JButton("Start Assistant");
        startStopButton.setPreferredSize(new Dimension(150, 35));
        startStopButton.setFont(new Font("Arial", Font.BOLD, 12));

        // Transparent background with pink text and border
        startStopButton.setOpaque(false);
        startStopButton.setContentAreaFilled(false);
        startStopButton.setForeground(new Color(200, 100, 200)); // Pink text color matching theme
        startStopButton.setFocusPainted(false);
        startStopButton.setBorder(BorderFactory.createLineBorder(new Color(200, 100, 200), 2)); // Pink border

        // Add hover effect
        startStopButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                // Brighter pink on hover
                startStopButton.setForeground(new Color(220, 120, 220));
                startStopButton.setBorder(BorderFactory.createLineBorder(new Color(220, 120, 220), 2));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                // Return to original pink color
                startStopButton.setForeground(new Color(200, 100, 200));
                startStopButton.setBorder(BorderFactory.createLineBorder(new Color(200, 100, 200), 2));
            }
        });

        startStopButton.addActionListener(e -> {
            if (AppState.isRunning) {
                Main.assistantCore.stopProcessing();
                startStopButton.setText("Start Assistant");
            } else {
                // Check if TTS API is available before starting
                if (!AppState.isTtsApiAvailable) {
                    // TTS wasn't available at startup, check again now
                    if (TtsApiClient.isApiAvailable()) {
                        // TTS is now available, update everything
                        AppState.isTtsApiAvailable = true;
                        List<String> voices = TtsApiClient.getAvailableCharacters();
                        if (voices != null && !voices.isEmpty()) {
                            // Update voice selection if needed
                            if (AppState.selectedTtsCharacterVoice == null) {
                                AppState.selectedTtsCharacterVoice = voices.getFirst();
                                AppState.saveCurrentSettings();
                            }
                            // Update the voice selector in the UI
                            updateVoiceSelector(voices);
                            // Proceed with starting the assistant
                            Main.assistantCore.startProcessing();
                            startStopButton.setText("Stop Assistant");
                        } else {
                            // Still no voices available
                            JOptionPane.showMessageDialog(this,
                                "TTS API is responding but no voices are available.",
                                "No Voices Available",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        // TTS is still not available - simple message
                        JOptionPane.showMessageDialog(this,
                            "TTS API Server is still not running.\nPlease start the TTS API server (start_api_coqui.py) and try again.",
                            "TTS API Not Available",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // TTS was available at startup, proceed normally
                    Main.assistantCore.startProcessing();
                    startStopButton.setText("Stop Assistant");
                }
            }
        });

        controlPanel.add(startStopButton);
        return controlPanel;
    }

    // unified model now modularized below

    private void refreshSettingsTab() {
        // Find the tabbed pane and refresh the settings tab
        Container parent = this.getContentPane();
        JTabbedPane tabbedPane = findTabbedPane(parent);
        if (tabbedPane != null) {
            // Recreate the settings tab
            JPanel newSettingsTab = createSettingsTab();
            tabbedPane.setComponentAt(1, newSettingsTab); // Settings tab is at index 1
            tabbedPane.revalidate();
            tabbedPane.repaint();
        }
    }

    private JTabbedPane findTabbedPane(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTabbedPane) {
                return (JTabbedPane) component;
            } else if (component instanceof Container) {
                JTabbedPane found = findTabbedPane((Container) component);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void updateVoiceSelector(List<String> voices) {
        // Find the voice selector and update it with new voices
        Component voiceSelector = findVoiceSelector(this.getContentPane());
        if (voiceSelector instanceof JComboBox) {
            @SuppressWarnings("unchecked")
            JComboBox<String> comboBox = (JComboBox<String>) voiceSelector;
            comboBox.removeAllItems();
            for (String voice : voices) {
                comboBox.addItem(voice);
            }
            // Set the selected voice
            if (AppState.selectedTtsCharacterVoice != null) {
                comboBox.setSelectedItem(AppState.selectedTtsCharacterVoice);
            }
        }
    }

    private Component findVoiceSelector(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox) {
                // This is a simple check - in a more complex app you'd want to be more specific
                return component;
            } else if (component instanceof Container) {
                Component found = findVoiceSelector((Container) component);
                if (found != null) return found;
            }
        }
        return null;
    }
}
