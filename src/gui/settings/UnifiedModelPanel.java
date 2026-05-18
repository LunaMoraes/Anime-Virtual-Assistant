package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;

public class UnifiedModelPanel extends JPanel {
    private final Runnable onModeChanged;

    public UnifiedModelPanel(Runnable onModeChanged) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.onModeChanged = onModeChanged;

        ButtonGroup modelGroup = new ButtonGroup();
        JRadioButton localButton = new JRadioButton("Local");
        JRadioButton apiButton = new JRadioButton("API");
        JCheckBox useMultimodalCheckBox = new JCheckBox("Enable Multimodal (Vision)");
        ButtonGroup localBackendGroup = new ButtonGroup();
        JRadioButton ollamaButton = new JRadioButton("Ollama");
        JRadioButton lmStudioButton = new JRadioButton("LM Studio");

        gui.UIUtils.styleRadio(localButton);
        localButton.setForeground(Color.WHITE);
        gui.UIUtils.styleRadio(apiButton);
        apiButton.setForeground(Color.WHITE);
        gui.UIUtils.styleCheckBox(useMultimodalCheckBox);
        useMultimodalCheckBox.setForeground(Color.WHITE);
        gui.UIUtils.styleRadio(ollamaButton);
        ollamaButton.setForeground(Color.WHITE);
        gui.UIUtils.styleRadio(lmStudioButton);
        lmStudioButton.setForeground(Color.WHITE);

        modelGroup.add(localButton);
        modelGroup.add(apiButton);
        localBackendGroup.add(ollamaButton);
        localBackendGroup.add(lmStudioButton);

        boolean apiAvailable = AppState.isMultimodalApiConfigAvailable();
        boolean useApiMultimodal = apiAvailable && AppState.useApiMultimodal();

        if (useApiMultimodal) {
            apiButton.setSelected(true);
        } else {
            localButton.setSelected(true);
        }

        useMultimodalCheckBox.setSelected(useApiMultimodal ? AppState.useMultimodal() : true);
        useMultimodalCheckBox.setEnabled(useApiMultimodal);

        String localProvider = AppState.getLocalLlmProvider();
        if ("lm_studio".equalsIgnoreCase(localProvider) || "lm-studio".equalsIgnoreCase(localProvider) || "lmstudio".equalsIgnoreCase(localProvider)) {
            lmStudioButton.setSelected(true);
        } else {
            ollamaButton.setSelected(true);
        }

        ollamaButton.setEnabled(!useApiMultimodal);
        lmStudioButton.setEnabled(!useApiMultimodal);

        if (!apiAvailable) {
            apiButton.setEnabled(false);
            apiButton.setToolTipText("Multimodal API configuration not available in data/system/system.json");
        }

        localButton.addActionListener(e -> {
            if (localButton.isSelected()) {
                AppState.setUseApiMultimodal(false);
                AppState.setUseMultimodal(true);
                if (this.onModeChanged != null) this.onModeChanged.run();
            }
        });

        apiButton.addActionListener(e -> {
            if (apiButton.isSelected()) {
                AppState.setUseApiMultimodal(true);
                if (this.onModeChanged != null) this.onModeChanged.run();
            }
        });

        useMultimodalCheckBox.addActionListener(e -> {
            AppState.setUseMultimodal(useMultimodalCheckBox.isSelected());
            if (!useMultimodalCheckBox.isSelected()) {
                AppState.setUseApiVision(true);
                AppState.setUseApiAnalysis(true);
            }
            if (this.onModeChanged != null) this.onModeChanged.run();
        });

        ollamaButton.addActionListener(e -> {
            if (ollamaButton.isSelected()) {
                AppState.setLocalLlmProvider("ollama");
            }
        });

        lmStudioButton.addActionListener(e -> {
            if (lmStudioButton.isSelected()) {
                AppState.setLocalLlmProvider("lm_studio");
            }
        });

        add(createRow(localButton, apiButton));
        add(createRow(useMultimodalCheckBox));
        add(createRow(ollamaButton, lmStudioButton));
    }

    private JPanel createRow(JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        for (JComponent component : components) {
            row.add(component);
        }
        return row;
    }
}
