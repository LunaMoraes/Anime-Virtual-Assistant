package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;

public class UnifiedModelPanel extends JPanel {
    public UnifiedModelPanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);

        ButtonGroup modelGroup = new ButtonGroup();
        JRadioButton localButton = new JRadioButton("Local");
        JRadioButton apiButton = new JRadioButton("API");

        gui.UIUtils.styleRadio(localButton);
        localButton.setForeground(Color.WHITE);
        gui.UIUtils.styleRadio(apiButton);
        apiButton.setForeground(Color.WHITE);

        modelGroup.add(localButton);
        modelGroup.add(apiButton);

        if (AppState.useApiMultimodal()) {
            apiButton.setSelected(true);
        } else {
            localButton.setSelected(true);
        }

        if (!AppState.isMultimodalApiConfigAvailable()) {
            apiButton.setEnabled(false);
            apiButton.setToolTipText("Multimodal API configuration not available in data/system/system.json");
        }

        localButton.addActionListener(e -> {
            if (localButton.isSelected()) {
                AppState.setUseApiMultimodal(false);
            }
        });

        apiButton.addActionListener(e -> {
            if (apiButton.isSelected()) {
                AppState.setUseApiMultimodal(true);
            }
        });

        add(localButton);
        add(apiButton);
    }
}
