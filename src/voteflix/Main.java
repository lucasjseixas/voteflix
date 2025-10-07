//package testevoteflix;
//
//import testevoteflix.gui.ClientGUI;
//import testevoteflix.gui.ServerGUI;
//
//import javax.swing.*;
//
//public class Main {
//    public static void main(String[] args) {
//
////        SwingUtilities.invokeLater(() -> {
////            SwingUtilities.invokeLater(() -> {
////                ServerGUI servidorGUI = new ServerGUI();
////                ClientGUI clienteGUI = new ClientGUI();
////                servidorGUI.setVisible(true);
////                clienteGUI.setVisible(true);
////            });
////        });
//    }
//}

package voteflix;

import voteflix.gui.ServerGUI;
import voteflix.gui.ClientGUI;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        // Define Look and Feel do sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Cria diálogo de escolha
        SwingUtilities.invokeLater(() -> {
            String[] opcoes = {"Servidor", "Cliente", "Cancelar"};

            int escolha = JOptionPane.showOptionDialog(
                    null,
                    "Escolha o que deseja executar:",
                    "VoteFlix - Launcher",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]
            );

            switch (escolha) {
                case 0: // Servidor
                    new ServerGUI().setVisible(true);
                    break;
                case 1: // Cliente
                    new ClientGUI().setVisible(true);
                    break;
                default: // Cancelar ou fechar
                    System.exit(0);
            }
        });
    }
}
