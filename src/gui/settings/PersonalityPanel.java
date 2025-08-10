package gui.settings;

import core.AppState;
import personality.Personality;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PersonalityPanel extends JPanel {
    public PersonalityPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        List<Personality> personalities = AppState.getAvailablePersonalities();

        if (personalities.isEmpty()) {
            JLabel noPersonalitiesLabel = new JLabel("No personalities found. Check data folder.");
            noPersonalitiesLabel.setForeground(Color.WHITE);
            add(noPersonalitiesLabel);
            return;
        }

        ButtonGroup personalityGroup = new ButtonGroup();

        for (Personality personality : personalities) {
            JRadioButton radioButton = new JRadioButton(personality.getName());
            gui.UIUtils.styleRadio(radioButton);
            radioButton.setForeground(Color.WHITE);
            radioButton.setFont(new Font("Arial", Font.PLAIN, 12));
            personalityGroup.add(radioButton);

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

            add(radioButton);
        }
    }
}
