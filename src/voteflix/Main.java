package voteflix;

import voteflix.gui.ServerGUI;
import voteflix.gui.ClientGUI;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            String[] opcoes = {"Servidor", "Cliente", "Cancelar"};

            int escolha = JOptionPane.showOptionDialog(
                    null,
                    "Escolha o que deseja executar:",
                    "VoteFlix",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]
            );

            switch (escolha) {
                case 0:
                    new ServerGUI().setVisible(true);
                    break;
                case 1:
                    new ClientGUI().setVisible(true);
                    break;
                default:
                    System.exit(0);
            }
        });
    }
}
