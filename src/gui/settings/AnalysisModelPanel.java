package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;

public class AnalysisModelPanel extends JPanel {
    public AnalysisModelPanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);

        ButtonGroup analysisModelGroup = new ButtonGroup();
        JRadioButton localAnalysisButton = new JRadioButton("Local");
        JRadioButton apiAnalysisButton = new JRadioButton("API");

        localAnalysisButton.setOpaque(false);
        localAnalysisButton.setForeground(Color.WHITE);
        apiAnalysisButton.setOpaque(false);
        apiAnalysisButton.setForeground(Color.WHITE);

        analysisModelGroup.add(localAnalysisButton);
        analysisModelGroup.add(apiAnalysisButton);

        if (AppState.useApiAnalysis()) {
            apiAnalysisButton.setSelected(true);
        } else {
            localAnalysisButton.setSelected(true);
        }

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

        add(localAnalysisButton);
        add(apiAnalysisButton);
    }
}
