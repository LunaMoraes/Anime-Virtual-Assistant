package gui.settings;

import core.AppState;

import javax.swing.*;
import java.awt.*;

public class MultimodalPanel extends JPanel {
    private final Runnable onModeChanged;

    public MultimodalPanel(Runnable onModeChanged) {
        super(new FlowLayout(FlowLayout.LEFT));
        setOpaque(false);
        this.onModeChanged = onModeChanged;

        JCheckBox useMultimodalCheckBox = new JCheckBox("Enable Multimodal (Vision)");
        useMultimodalCheckBox.setOpaque(false);
        useMultimodalCheckBox.setForeground(Color.WHITE);
        useMultimodalCheckBox.setSelected(AppState.useMultimodal());

        useMultimodalCheckBox.addActionListener(e -> {
            AppState.setUseMultimodal(useMultimodalCheckBox.isSelected());
            if (this.onModeChanged != null) this.onModeChanged.run();
        });

        add(useMultimodalCheckBox);
    }
}
