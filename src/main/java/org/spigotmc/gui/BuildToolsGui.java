package org.spigotmc.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import org.spigotmc.builder.BuildInfo;
import org.spigotmc.builder.Builder;
import org.spigotmc.gui.attributes.Lockable;
import org.spigotmc.gui.data.BuildSettings;
import org.spigotmc.gui.data.BuildData;
import org.spigotmc.gui.panels.ThemeSwitcherPanel;
import org.spigotmc.gui.panels.about.AboutPanel;
import org.spigotmc.gui.panels.general.GeneralPanel;
import org.spigotmc.gui.panels.options.OptionsPanel;
import org.spigotmc.utils.Constants;
import org.spigotmc.utils.SwingUtils;
import org.spigotmc.utils.Utils;

import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BuildToolsGui extends JFrame implements Lockable {

    private final BuildData buildData;
    private final BuildSettings buildSettings;

    private List<String> temp = new ArrayList<>();

    private JTabbedPane menuPane;
    private GeneralPanel generalPanel;
    private OptionsPanel optionsPanel;
    private AboutPanel aboutPanel;

    private int currentTab;

    public static void main(String[] args)
    {
        SwingUtils.applyInitialTheme();

        BuildToolsGui gui = new BuildToolsGui();
        gui.setLocationRelativeTo(null);
        gui.setVisible(true);
    }

    public BuildToolsGui() {
        Builder.CWD = Utils.getFile().getParentFile();

        this.buildSettings = new BuildSettings();
        Theme.LIGHT.apply();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(new Dimension(1200, 700));
        setTitle("BuildTools");
        SwingUtils.applyIcon(this);

        this.buildData = new BuildData(this::onDataReceived);

        initComponents();
        menuPane.addChangeListener(this::onTabChange);
        SwingUtils.toggleLockComponents(this, LockReason.START);
        SwingUtils.changeTheme(this, Theme.LIGHT);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                BuildToolsProcess buildToolsProcess = generalPanel.getBuildToolsProcess();

                if (buildToolsProcess == null) {
                    return;
                }

                Process process = buildToolsProcess.getProcess();
                if (process == null) {
                    return;
                }

                process.destroyForcibly();
            }
        });
    }

    private void initComponents() {
        this.menuPane = new JTabbedPane();
        menuPane.setEnabled(false);

        this.generalPanel = new GeneralPanel(buildData, buildSettings, this);
        menuPane.addTab("General", generalPanel);

        this.optionsPanel = new OptionsPanel(generalPanel, buildData, buildSettings);
        menuPane.addTab("Options", optionsPanel);

        this.aboutPanel = new AboutPanel();
        menuPane.addTab("About", aboutPanel);

        final ThemeSwitcherPanel themeSwitcherPanel = new ThemeSwitcherPanel(this);

        final GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addGap(0, 1108, Short.MAX_VALUE)
                    .addComponent(themeSwitcherPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(menuPane, GroupLayout.Alignment.TRAILING))
        );

        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(themeSwitcherPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 667, Short.MAX_VALUE))
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(menuPane, GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE))
        );

    }

    private void onDataReceived(Map<String, Supplier<CompletableFuture<BuildInfo>>> buildInfo) {
        try {
            menuPane.setEnabled(true);
            // Equivalent to JTextArea#setVisible(false)
            generalPanel.getVersionWarningPane().setWarningText(null, null);
            generalPanel.getSettingsPanel().setVersions(buildData.getVersions());
            optionsPanel.setJavaExecutable(buildData.getJavaInstallationManager().getSelectedInstallation());
            SwingUtils.toggleLockComponents(this, LockReason.START);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onTabChange(ChangeEvent event) {
        if (!(event.getSource() instanceof JTabbedPane)) {
            return;
        }

        final JTabbedPane pane = (JTabbedPane) event.getSource();
        if (pane.getSelectedIndex() == 0 && currentTab == 1) {
            final List<String> preCompilationText = buildData.generatePreCompilationText(buildSettings);
            if (this.temp.equals(preCompilationText)) {
                return;
            }

            generalPanel.getConsolePane().updateConsoleAreaText(buildData.generatePreCompilationText(buildSettings));
        }

        if (pane.getSelectedIndex() == 1) {
            this.temp = buildData.generatePreCompilationText(buildSettings);
            optionsPanel.setJavaExecutable(buildData.getJavaInstallationManager().getSelectedInstallation());
        }

        if (pane.getSelectedIndex() == 2) {
            this.aboutPanel.setAboutHeaderImage(Utils.randomElement(
                    Constants.LOGO.getImage().getScaledInstance(64, 64, Image.SCALE_DEFAULT)
                    , Constants.LOGO_ABOUT_EASTER_EGG.getImage()
                    , 20)
            );
        }

        currentTab = pane.getSelectedIndex();
    }

    @Override
    public void onLockToggle(LockReason reason) {
        if (reason == LockReason.BUILD) {
            menuPane.setEnabledAt(1, !menuPane.isEnabledAt(1));
        }
    }
}
