package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;

public class VisionModelPanel extends JPanel {
    public VisionModelPanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);

        ButtonGroup visionModelGroup = new ButtonGroup();
        JRadioButton localVisionButton = new JRadioButton("Local");
        JRadioButton apiVisionButton = new JRadioButton("API");

        gui.UIUtils.styleRadio(localVisionButton);
        localVisionButton.setForeground(Color.WHITE);
        gui.UIUtils.styleRadio(apiVisionButton);
        apiVisionButton.setForeground(Color.WHITE);

        visionModelGroup.add(localVisionButton);
        visionModelGroup.add(apiVisionButton);

        if (AppState.useApiVision()) {
            apiVisionButton.setSelected(true);
        } else {
            localVisionButton.setSelected(true);
        }

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

        add(localVisionButton);
        add(apiVisionButton);
    }
}
