package org.spigotmc.gui.panels.general;

import java.io.File;
import java.util.concurrent.ExecutionException;
import org.spigotmc.gui.BuildToolsProcess;
import org.spigotmc.gui.attributes.Lockable;
import org.spigotmc.gui.data.BuildSettings;
import org.spigotmc.gui.data.BuildData;
import org.spigotmc.gui.modals.DebugInfoModal;
import org.spigotmc.gui.modals.MessageModal;
import org.spigotmc.gui.panels.general.console.ConsolePane;
import org.spigotmc.gui.panels.general.console.ConsolePanelHeader;
import org.spigotmc.gui.panels.general.settings.SettingsPanel;
import org.spigotmc.gui.panels.general.settings.SettingsPanelHeader;
import org.spigotmc.gui.panels.general.settings.VersionWarningPane;
import org.spigotmc.utils.AwtExecutor;
import org.spigotmc.utils.Constants;
import org.spigotmc.utils.SwingUtils;
import org.spigotmc.utils.Utils;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.LayoutStyle;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GeneralPanel extends JPanel implements Lockable {

    private final JButton buildButton = new JButton();
    private final JButton copyLogButton = new JButton();
    private final JButton cancelButton = new JButton();
    private final JButton debugInfoButton = new JButton();

    private final JProgressBar progressBar = new JProgressBar();

    private SettingsPanel settingsPanel;
    private VersionWarningPane versionWarningPane;
    private ConsolePane consolePane;
    private ConsolePanelHeader consolePanelHeader;

    private final JFrame parent;
    private final BuildData buildData;
    private final BuildSettings buildSettings;

    private BuildToolsProcess buildProcess;

    public GeneralPanel(BuildData buildData, final BuildSettings buildSettings, JFrame parent) {
        this.buildData = buildData;
        this.buildSettings = buildSettings;
        this.parent = parent;

        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setPreferredSize(new Dimension(1200, 700));

        initComponents();
    }

    private void initComponents() {
        this.consolePane = new ConsolePane();
        this.versionWarningPane = new VersionWarningPane();
        this.settingsPanel = new SettingsPanel(buildData, buildSettings, consolePane, versionWarningPane);

        final SettingsPanelHeader settingsPanelHeader = new SettingsPanelHeader();
        this.consolePanelHeader = new ConsolePanelHeader();

        progressBar.setPreferredSize(new Dimension(146, 8));

        buildButton.setBackground(Constants.ACCENT_COLOR);
        buildButton.setForeground(new Color(255, 255, 255));
        buildButton.setText("Compile");
        buildButton.setToolTipText("Start");
        buildButton.setMargin(new Insets(8, 16, 8, 16));
        buildButton.addActionListener(this::buildButtonActionPerformed);
        buildButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        copyLogButton.setText("Copy Log");
        copyLogButton.setToolTipText("Copy the log to your clipboard.");
        copyLogButton.setMargin(new Insets(8, 16, 8, 16));
        copyLogButton.addActionListener(this::copyLogButtonActionPerformed);
        copyLogButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelButton.setText("Cancel");
        cancelButton.setToolTipText("Terminates the currently running compilation operation");
        cancelButton.setMargin(new Insets(8, 16, 8, 16));
        cancelButton.addActionListener(this::cancelButtonActionPerformed);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setEnabled(false);

        debugInfoButton.setText("Debug Info");
        debugInfoButton.setToolTipText("View the debug panel.");
        debugInfoButton.setMargin(new Insets(8, 16, 8, 16));
        debugInfoButton.addActionListener(this::debugInfoButtonActionPerformed);
        debugInfoButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(debugInfoButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cancelButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(copyLogButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(buildButton))
                .addComponent(settingsPanelHeader, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(consolePane)
                .addComponent(consolePanelHeader, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(settingsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(progressBar, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE,
                    GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(versionWarningPane));
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(settingsPanelHeader, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, 0)
                    .addComponent(settingsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addComponent(versionWarningPane, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(consolePanelHeader, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, 0)
                    .addComponent(consolePane, GroupLayout.DEFAULT_SIZE, 418, Short.MAX_VALUE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                    .addGap(8, 8, 8)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(debugInfoButton, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
                        .addComponent(cancelButton, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
                        .addComponent(copyLogButton, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE)
                        .addComponent(buildButton, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE))));

        setFocusCycleRoot(true);
    }

    private void buildButtonActionPerformed(ActionEvent event) {
        try {
            if (!buildData.updateJavaExecutable(buildSettings, true).get()) {
                return;
            }
        } catch (InterruptedException | ExecutionException e) {
            MessageModal.displayError(Utils.getReadableStacktrace(e));
            return;
        }

        cancelButton.setEnabled(true);
        consolePane.updateConsoleAreaText(new ArrayList<>());

        progressBar.setIndeterminate(true);
        progressBar.setForeground(Constants.ACCENT_COLOR_DESATURATED);

        List<String> args = buildData.buildArgs(buildSettings);

        buildButton.setText("Compiling...");
        SwingUtils.toggleLockComponents(parent, LockReason.BUILD);

        CompletableFuture.supplyAsync(() -> {
            consolePane.updateStreams();

            // Hacky workaround for empty output dir fields.
            if (!buildSettings.getWorkDirectory().isEmpty() && buildSettings.getOutputDirectory().isEmpty()) {
                buildSettings.setOutputDirectory(buildSettings.getWorkDirectory());
            }

            buildProcess = new BuildToolsProcess(args, System.out, new File(buildSettings.getWorkDirectory()));
            final Process process;
            try {
                process = buildProcess.execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                return process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).whenCompleteAsync((Integer exitValue, Throwable throwable) -> {
            cancelButton.setEnabled(false);
            buildButton.setText("Compile");
            progressBar.setIndeterminate(false);
            progressBar.setForeground(Constants.ACCENT_COLOR);
            progressBar.setValue(100);

            SwingUtils.toggleLockComponents(parent, LockReason.BUILD);

            if (consolePanelHeader.copyLogsOnFinish()) {
                SwingUtils.copyToClipboard(new File(Constants.LOG_FILE));
            }

            if (throwable != null) {
                MessageModal.displayError(Utils.getReadableStacktrace(throwable));
                return;
            }

            if (exitValue == 2) {
                String style = Utils.getFileContentsFromResource("web/reset.css");
                String message;
                message = Utils.getFileContentsFromResource("web/success_no_compile.html");
                message = message.replace("%STYLESHEET%", "<style>" + style + "</style>");
                MessageModal.displaySuccess(message);
                return;
            }

            if (exitValue != 0) {
                MessageModal.displayError("Task exited with error code " + exitValue);
                return;
            }

            String style = Utils.getFileContentsFromResource("web/reset.css");
            String message;

            message = Utils.getFileContentsFromResource("web/success.html");
            message = message.replace("%STYLESHEET%", "<style>" + style + "</style>");
            message = message.replace("%OUTPUT_DIRECTORY%", new File(buildSettings.getOutputDirectory()).getAbsolutePath());
            message = message.replace("%PLUGINS_LINK%", Constants.PLUGINS_LINK);

            MessageModal.displaySuccess(message);

            // Reset the field if it was empty.
            if (settingsPanel.getOutputDirField().getText().isEmpty()) {
                buildSettings.setOutputDirectory("");
            }
        }, AwtExecutor.INSTANCE);
    }

    private void debugInfoButtonActionPerformed(ActionEvent event) {
        JFrame debug = new DebugInfoModal(buildData, buildSettings);
        debug.setLocationRelativeTo(null);
        debug.setVisible(true);
    }

    private void copyLogButtonActionPerformed(ActionEvent event) {
        SwingUtils.copyToClipboard(new File(Constants.LOG_FILE));
        SwingUtils.buttonCooldown((AbstractButton) event.getSource(), 3, "Copied To Clipboard");
    }

    private void cancelButtonActionPerformed(ActionEvent event) {
        cancelButton.setEnabled(false);

        if (buildProcess == null || buildProcess.getProcess() == null) {
            return;
        }

        buildProcess.getProcess().destroy();
        System.out.println("Compilation terminated by the user");
    }

    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }

    public VersionWarningPane getVersionWarningPane() {
        return versionWarningPane;
    }

    public ConsolePane getConsolePane() {
        return consolePane;
    }

    @Override
    public void onLockToggle(final LockReason reason) {
        buildButton.setEnabled(!buildButton.isEnabled());
        debugInfoButton.setEnabled(!debugInfoButton.isEnabled());
        copyLogButton.setEnabled(!copyLogButton.isEnabled());
    }

    public BuildToolsProcess getBuildToolsProcess() {
        return buildProcess;
    }
}
