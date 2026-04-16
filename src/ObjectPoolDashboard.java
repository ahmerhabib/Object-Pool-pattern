import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ObjectPoolDashboard {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AtomicInteger idSequence = new AtomicInteger(1);
    private final DecimalFormat utilizationFormat = new DecimalFormat("0.0");

    private final BenchmarkService benchmarkService = new BenchmarkService();
    private final DashboardPersistence persistence = new DashboardPersistence(Path.of("data"));
    private final PoolProfileStore profileStore = new PoolProfileStore(Path.of("data", "profiles"));

    private final Map<String, DashboardConfig> builtInPresets = createBuiltInPresets();
    private final Map<String, DashboardConfig> customProfiles = new LinkedHashMap<>();
    private final Deque<Double> utilizationTrend = new ArrayDeque<>();

    private JFrame frame;
    private JPanel rootPanel;
    private JPanel headerPanel;
    private JPanel controlsCard;
    private JPanel activeCard;
    private JPanel availableCard;
    private JPanel statusCard;
    private JPanel logCard;
    private JPanel detailsCard;

    private JSpinner initialSizeSpinner;
    private JSpinner maxSizeSpinner;
    private JSpinner collectFrequencySpinner;
    private JSpinner resetValueSpinner;
    private JSpinner benchmarkOpsSpinner;
    private JSpinner fontScaleSpinner;

    private JLabel validationHintLabel;
    private JLabel activeCountLabel;
    private JLabel availableCountLabel;
    private JLabel utilizationLabel;
    private JLabel trendLabel;
    private JLabel benchmarkLabel;

    private JTextField filterField;
    private JTextField profileNameField;
    private JTextArea eventLog;
    private JTextArea detailArea;

    private JComboBox<String> profileCombo;
    private JComboBox<ThemeMode> themeCombo;
    private JCheckBox highContrastCheck;

    private DefaultListModel<PooledItem> activeModel;
    private DefaultListModel<PooledItem> availableModel;
    private JList<PooledItem> activeList;
    private JList<PooledItem> availableList;

    private JButton createPoolButton;
    private JButton acquireButton;
    private JButton releaseSelectedButton;
    private JButton releaseLatestButton;
    private JButton runBenchmarkButton;
    private JButton saveProfileButton;
    private JButton loadProfileButton;
    private JButton deleteProfileButton;
    private JButton exportConfigButton;
    private JButton importConfigButton;

    private ObjectPool_main<PooledItem> pool;
    private PooledItem latestBorrowed;
    private Timer refreshTimer;
    private long lastPersistedMetricMs;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configureLookAndFeel();
            new ObjectPoolDashboard().show();
        });
    }

    private static void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            // Falls back to default LAF.
        }
    }

    private void show() {
        persistence.init();
        loadCustomProfiles();

        frame = new JFrame("Object Pool Studio");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1320, 860));

        rootPanel = new JPanel(new BorderLayout(14, 14));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        frame.setContentPane(rootPanel);

        headerPanel = buildHeader();
        rootPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(14, 14));
        content.setOpaque(false);
        rootPanel.add(content, BorderLayout.CENTER);

        controlsCard = buildControlsCard();
        content.add(controlsCard, BorderLayout.NORTH);

        JSplitPane center = buildCenterSplit();
        content.add(center, BorderLayout.CENTER);

        JPanel lower = new JPanel(new GridLayout(1, 2, 14, 14));
        lower.setOpaque(false);
        detailsCard = buildDetailsCard();
        logCard = buildLogCard();
        lower.add(detailsCard);
        lower.add(logCard);
        content.add(lower, BorderLayout.SOUTH);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                stopRefreshTimer();
                disposePool();
                audit("app.closed", "Dashboard closed", Map.of());
            }
        });

        initializePool();
        startRefreshTimer();
        updateValidationHint();
        applyThemeAndAccessibility();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        audit("app.opened", "Dashboard launched", Map.of());
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Palette palette = palette();
                Graphics2D g2 = (Graphics2D) graphics.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, palette.headerStart, getWidth(), getHeight(), palette.headerEnd));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 26, 26);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("Object Pool Studio");
        title.setName("titleLabel");
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Profiles, persistence, benchmarking, and accessible modern controls.");
        subtitle.setName("subtitleLabel");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(232, 242, 255));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);
        return header;
    }

    private JPanel buildControlsCard() {
        JPanel card = createCardPanel(new BorderLayout(10, 10));

        JPanel topRow = new JPanel(new GridBagLayout());
        topRow.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        initialSizeSpinner = spinner(DashboardConfig.DEFAULT_INITIAL_SIZE, DashboardConfig.MIN_INITIAL_SIZE, DashboardConfig.MAX_INITIAL_SIZE, 1);
        maxSizeSpinner = spinner(DashboardConfig.DEFAULT_MAX_SIZE, DashboardConfig.MIN_MAX_SIZE, DashboardConfig.MAX_MAX_SIZE, 1);
        collectFrequencySpinner = spinner(DashboardConfig.DEFAULT_COLLECT_FREQUENCY_MS, -1, DashboardConfig.MAX_COLLECT_FREQUENCY_MS, 50);
        resetValueSpinner = spinner(DashboardConfig.DEFAULT_RESET_VALUE, -1_000_000, 1_000_000, 1);
        benchmarkOpsSpinner = spinner(DashboardConfig.DEFAULT_BENCHMARK_OPERATIONS, DashboardConfig.MIN_BENCHMARK_OPERATIONS, DashboardConfig.MAX_BENCHMARK_OPERATIONS, 500);
        fontScaleSpinner = spinner(DashboardConfig.DEFAULT_FONT_SCALE_PERCENT, DashboardConfig.MIN_FONT_SCALE_PERCENT, DashboardConfig.MAX_FONT_SCALE_PERCENT, 5);

        themeCombo = new JComboBox<>(ThemeMode.values());
        themeCombo.setSelectedItem(ThemeMode.LIGHT);
        highContrastCheck = new JCheckBox("High contrast");
        highContrastCheck.setOpaque(false);

        addField(topRow, gbc, 0, "Initial", initialSizeSpinner);
        addField(topRow, gbc, 1, "Max", maxSizeSpinner);
        addField(topRow, gbc, 2, "Collector ms (-1 off)", collectFrequencySpinner);
        addField(topRow, gbc, 3, "Reset Value", resetValueSpinner);
        addField(topRow, gbc, 4, "Benchmark Ops", benchmarkOpsSpinner);
        addField(topRow, gbc, 5, "Theme", themeCombo);
        addField(topRow, gbc, 6, "Font %", fontScaleSpinner);
        addField(topRow, gbc, 7, "A11y", highContrastCheck);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);

        createPoolButton = primaryButton("Create / Reset Pool");
        createPoolButton.addActionListener(e -> initializePool());

        acquireButton = primaryButton("Acquire");
        acquireButton.addActionListener(e -> acquireObject());

        releaseSelectedButton = secondaryButton("Release Selected");
        releaseSelectedButton.addActionListener(e -> releaseSelected());

        releaseLatestButton = secondaryButton("Release Latest");
        releaseLatestButton.addActionListener(e -> releaseLatest());

        runBenchmarkButton = secondaryButton("Run Benchmark");
        runBenchmarkButton.addActionListener(e -> runBenchmark());

        actionRow.add(createPoolButton);
        actionRow.add(acquireButton);
        actionRow.add(releaseSelectedButton);
        actionRow.add(releaseLatestButton);
        actionRow.add(runBenchmarkButton);

        JPanel profileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        profileRow.setOpaque(false);

        profileCombo = new JComboBox<>();
        refreshProfileCombo();

        profileNameField = new JTextField(16);
        profileNameField.setToolTipText("Profile name for save");

        saveProfileButton = secondaryButton("Save Profile");
        saveProfileButton.addActionListener(e -> saveProfile());

        loadProfileButton = secondaryButton("Load Profile");
        loadProfileButton.addActionListener(e -> loadSelectedProfile());

        deleteProfileButton = secondaryButton("Delete Profile");
        deleteProfileButton.addActionListener(e -> deleteSelectedProfile());

        exportConfigButton = secondaryButton("Export Settings");
        exportConfigButton.addActionListener(e -> exportSettings());

        importConfigButton = secondaryButton("Import Settings");
        importConfigButton.addActionListener(e -> importSettings());

        profileRow.add(new JLabel("Profile"));
        profileRow.add(profileCombo);
        profileRow.add(new JLabel("Name"));
        profileRow.add(profileNameField);
        profileRow.add(saveProfileButton);
        profileRow.add(loadProfileButton);
        profileRow.add(deleteProfileButton);
        profileRow.add(exportConfigButton);
        profileRow.add(importConfigButton);

        validationHintLabel = new JLabel("Ready");
        benchmarkLabel = new JLabel("Benchmark: not run yet");

        JPanel hintRow = new JPanel(new GridLayout(2, 1, 2, 2));
        hintRow.setOpaque(false);
        hintRow.add(validationHintLabel);
        hintRow.add(benchmarkLabel);

        card.add(topRow, BorderLayout.NORTH);
        card.add(actionRow, BorderLayout.CENTER);
        card.add(profileRow, BorderLayout.SOUTH);
        card.add(hintRow, BorderLayout.EAST);

        wireValidationListeners();
        return card;
    }

    private JSplitPane buildCenterSplit() {
        activeCard = createCardPanel(new BorderLayout(8, 8));
        availableCard = createCardPanel(new BorderLayout(8, 8));
        statusCard = createCardPanel(new GridLayout(4, 1, 8, 8));

        activeModel = new DefaultListModel<>();
        activeList = new JList<>(activeModel);
        activeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailPanel(activeList.getSelectedValue());
            }
        });

        availableModel = new DefaultListModel<>();
        availableList = new JList<>(availableModel);
        availableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailPanel(availableList.getSelectedValue());
            }
        });

        filterField = new JTextField(20);
        filterField.setToolTipText("Filter lists by id/value/state");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshView();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshView();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshView();
            }
        });

        JPanel activeTop = new JPanel(new BorderLayout(6, 6));
        activeTop.setOpaque(false);
        activeTop.add(sectionTitle("Active Objects"), BorderLayout.WEST);
        activeTop.add(filterField, BorderLayout.EAST);

        JPanel availableTop = new JPanel(new BorderLayout(6, 6));
        availableTop.setOpaque(false);
        availableTop.add(sectionTitle("Available Objects"), BorderLayout.WEST);

        activeCard.add(activeTop, BorderLayout.NORTH);
        activeCard.add(new JScrollPane(activeList), BorderLayout.CENTER);

        availableCard.add(availableTop, BorderLayout.NORTH);
        availableCard.add(new JScrollPane(availableList), BorderLayout.CENTER);

        activeCountLabel = metricLabel("Active: 0");
        availableCountLabel = metricLabel("Available: 0");
        utilizationLabel = metricLabel("Utilization: 0%");
        trendLabel = metricLabel("Trend: No samples yet");
        statusCard.add(activeCountLabel);
        statusCard.add(availableCountLabel);
        statusCard.add(utilizationLabel);
        statusCard.add(trendLabel);

        JPanel rightStack = new JPanel(new BorderLayout(10, 10));
        rightStack.setOpaque(false);
        rightStack.add(availableCard, BorderLayout.CENTER);
        rightStack.add(statusCard, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, activeCard, rightStack);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerLocation(660);
        splitPane.setBorder(null);
        return splitPane;
    }

    private JPanel buildDetailsCard() {
        JPanel panel = createCardPanel(new BorderLayout(8, 8));
        panel.add(sectionTitle("Object Details"), BorderLayout.NORTH);

        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setText("Select an item from either list to inspect details.");

        panel.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLogCard() {
        JPanel panel = createCardPanel(new BorderLayout(8, 8));
        panel.add(sectionTitle("Activity + Audit Trail"), BorderLayout.NORTH);

        eventLog = new JTextArea();
        eventLog.setEditable(false);
        eventLog.setLineWrap(true);
        eventLog.setWrapStyleWord(true);

        panel.add(new JScrollPane(eventLog), BorderLayout.CENTER);

        JButton clearButton = secondaryButton("Clear Visible Log");
        clearButton.addActionListener(e -> eventLog.setText(""));
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.add(clearButton);
        panel.add(actionRow, BorderLayout.SOUTH);

        return panel;
    }

    private void initializePool() {
        DashboardConfig config = readConfigFromInputs().sanitized();
        ValidationResult validationResult = config.validate();
        if (!validationResult.valid) {
            validationHintLabel.setText(validationResult.message);
            validationHintLabel.setForeground(new Color(184, 45, 45));
            return;
        }

        disposePool();
        try {
            ObjectPoolOpts<PooledItem> opts = new ObjectPoolOpts<>(
                config.initialSize,
                config.maxSize,
                new CreateFunction<>() {
                    @Override
                    PooledItem create() {
                        return PooledItem.fresh(idSequence.getAndIncrement());
                    }
                },
                new ResetFunction<>() {
                    @Override
                    PooledItem reset(PooledItem obj, Object... args) {
                        int newValue = obj.value;
                        if (args.length > 0 && args[0] instanceof Integer) {
                            newValue = (Integer) args[0];
                        }
                        obj.value = newValue;
                        obj.state = "active";
                        obj.borrowCount++;
                        obj.lastUsedAt = LocalDateTime.now();
                        return obj;
                    }
                },
                config.collectFrequencyMs
            );

            pool = new ObjectPool_main<>(opts);
            latestBorrowed = null;
            utilizationTrend.clear();
            logLine("Pool initialized (initial=%d, max=%d, collect=%d).", config.initialSize, config.maxSize, config.collectFrequencyMs);
            audit("pool.initialized", "Pool initialized", Map.of(
                "initial", Integer.toString(config.initialSize),
                "max", Integer.toString(config.maxSize),
                "collectMs", Integer.toString(config.collectFrequencyMs)
            ));
            refreshView();
        } catch (Exception ex) {
            telemetry("pool.initialize.error", ex);
            showError(ex.getMessage());
        }
    }

    private void acquireObject() {
        if (pool == null) {
            return;
        }

        DashboardConfig config = readConfigFromInputs().sanitized();
        try {
            PooledItem item = pool.get(config.resetValue);
            if (item == null) {
                logLine("Acquire denied: pool is at max capacity.");
                audit("pool.acquire.denied", "Acquire denied", Map.of("reason", "at_capacity"));
                return;
            }

            item.state = "active";
            item.lastUsedAt = LocalDateTime.now();
            latestBorrowed = item;
            logLine("Acquired %s", item.summary());
            audit("pool.acquire", "Object acquired", Map.of("itemId", Integer.toString(item.id)));
            refreshView();
        } catch (Exception ex) {
            telemetry("pool.acquire.error", ex);
            showError(ex.getMessage());
        }
    }

    private void releaseSelected() {
        if (pool == null) {
            return;
        }
        PooledItem selected = activeList.getSelectedValue();
        if (selected == null) {
            logLine("No active object selected.");
            return;
        }
        doRelease(selected, "pool.release.selected");
    }

    private void releaseLatest() {
        if (pool == null) {
            return;
        }
        if (latestBorrowed == null) {
            logLine("No latest object to release.");
            return;
        }
        doRelease(latestBorrowed, "pool.release.latest");
        latestBorrowed = null;
    }

    private void doRelease(PooledItem item, String action) {
        try {
            item.state = "available";
            item.lastUsedAt = LocalDateTime.now();
            pool.free(item);
            logLine("Released %s", item.summary());
            audit(action, "Object released", Map.of("itemId", Integer.toString(item.id)));
            refreshView();
        } catch (Exception ex) {
            telemetry(action + ".error", ex);
            showError(ex.getMessage());
        }
    }

    private void refreshView() {
        if (pool == null) {
            activeModel.clear();
            availableModel.clear();
            activeCountLabel.setText("Active: 0");
            availableCountLabel.setText("Available: 0");
            utilizationLabel.setText("Utilization: 0%");
            trendLabel.setText("Trend: No samples yet");
            return;
        }

        String filterText = filterField == null ? "" : filterField.getText();

        updateListModel(activeModel, pool.activeSnapshot(), filterText);
        updateListModel(availableModel, pool.availableSnapshot(), filterText);

        int activeCount = pool.activeCount();
        int availableCount = pool.availableCount();
        int max = pool.maxSize();
        double utilization = max == 0 ? 0 : (activeCount * 100.0) / max;

        activeCountLabel.setText("Active: " + activeCount);
        availableCountLabel.setText("Available: " + availableCount);
        utilizationLabel.setText("Utilization: " + utilizationFormat.format(utilization) + "%");

        utilizationTrend.add(utilization);
        while (utilizationTrend.size() > 36) {
            utilizationTrend.removeFirst();
        }
        trendLabel.setText("Trend: " + DashboardUiSupport.utilizationSparkline(new ArrayList<>(utilizationTrend)));

        long now = System.currentTimeMillis();
        if (now - lastPersistedMetricMs >= 1000) {
            persistence.appendUtilizationPoint(activeCount, availableCount, utilization);
            lastPersistedMetricMs = now;
        }
    }

    private void updateListModel(DefaultListModel<PooledItem> model, List<PooledItem> items, String filterText) {
        model.clear();
        for (PooledItem item : items) {
            if (DashboardUiSupport.matchesFilter(item.searchable(), filterText)) {
                model.addElement(item);
            }
        }
    }

    private void updateDetailPanel(PooledItem item) {
        if (item == null) {
            detailArea.setText("Select an item from either list to inspect details.");
            return;
        }
        String details = DashboardUiSupport.buildDetailText(
            item.id,
            item.value,
            item.state,
            item.borrowCount,
            item.createdAt.format(CLOCK_FORMAT),
            item.lastUsedAt.format(CLOCK_FORMAT)
        );
        detailArea.setText(details);
    }

    private void runBenchmark() {
        DashboardConfig config = readConfigFromInputs().sanitized();
        ValidationResult validationResult = config.validate();
        if (!validationResult.valid) {
            validationHintLabel.setText(validationResult.message);
            validationHintLabel.setForeground(new Color(184, 45, 45));
            return;
        }

        runBenchmarkButton.setEnabled(false);
        benchmarkLabel.setText("Benchmark: running...");

        SwingWorker<BenchmarkService.BenchmarkResult, Void> worker = new SwingWorker<>() {
            @Override
            protected BenchmarkService.BenchmarkResult doInBackground() {
                return benchmarkService.run(config.benchmarkOperations, config.maxSize);
            }

            @Override
            protected void done() {
                runBenchmarkButton.setEnabled(true);
                try {
                    BenchmarkService.BenchmarkResult result = get();
                    String summary = String.format(
                        "Benchmark: pooled=%.2fµs/op, raw=%.2fµs/op, speedup=%.2fx",
                        result.pooledAverageMicros(),
                        result.rawAverageMicros(),
                        result.speedup()
                    );
                    benchmarkLabel.setText(summary);
                    logLine(summary);
                    audit("benchmark.completed", "Benchmark finished", Map.of(
                        "operations", Integer.toString(result.operations),
                        "speedup", String.format("%.2f", result.speedup())
                    ));
                } catch (Exception ex) {
                    telemetry("benchmark.error", ex);
                    benchmarkLabel.setText("Benchmark: failed");
                    showError(ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void saveProfile() {
        String rawName = profileNameField.getText().trim();
        if (rawName.isEmpty()) {
            showError("Enter a profile name before saving.");
            return;
        }

        DashboardConfig config = readConfigFromInputs().sanitized();
        ValidationResult validationResult = config.validate();
        if (!validationResult.valid) {
            showError(validationResult.message);
            return;
        }

        try {
            profileStore.saveProfile(rawName, config);
            customProfiles.put(rawName, config);
            refreshProfileCombo();
            profileCombo.setSelectedItem(rawName);
            logLine("Saved profile '%s'.", rawName);
            audit("profile.saved", "Profile saved", Map.of("profile", rawName));
        } catch (IOException ex) {
            telemetry("profile.save.error", ex);
            showError(ex.getMessage());
        }
    }

    private void loadSelectedProfile() {
        Object selected = profileCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        String name = selected.toString();

        DashboardConfig config = builtInPresets.get(name);
        if (config == null) {
            config = customProfiles.get(name);
        }
        if (config == null) {
            showError("Selected profile is unavailable.");
            return;
        }

        applyConfigToInputs(config);
        initializePool();
        logLine("Loaded profile '%s'.", name);
        audit("profile.loaded", "Profile loaded", Map.of("profile", name));
    }

    private void deleteSelectedProfile() {
        Object selected = profileCombo.getSelectedItem();
        if (selected == null) {
            return;
        }

        String name = selected.toString();
        if (builtInPresets.containsKey(name)) {
            showError("Built-in presets cannot be deleted.");
            return;
        }

        if (!customProfiles.containsKey(name)) {
            showError("Only saved custom profiles can be deleted.");
            return;
        }

        try {
            profileStore.deleteProfile(name);
            customProfiles.remove(name);
            refreshProfileCombo();
            logLine("Deleted profile '%s'.", name);
            audit("profile.deleted", "Profile deleted", Map.of("profile", name));
        } catch (IOException ex) {
            telemetry("profile.delete.error", ex);
            showError(ex.getMessage());
        }
    }

    private void exportSettings() {
        DashboardConfig config = readConfigFromInputs().sanitized();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Settings");
        chooser.setSelectedFile(new java.io.File("pool-settings.properties"));
        int decision = chooser.showSaveDialog(frame);
        if (decision != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            Path file = chooser.getSelectedFile().toPath();
            profileStore.exportConfig(file, config);
            logLine("Exported settings to %s", file.toAbsolutePath());
            audit("settings.exported", "Settings exported", Map.of("file", file.toAbsolutePath().toString()));
        } catch (IOException ex) {
            telemetry("settings.export.error", ex);
            showError(ex.getMessage());
        }
    }

    private void importSettings() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Settings");
        int decision = chooser.showOpenDialog(frame);
        if (decision != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            Path file = chooser.getSelectedFile().toPath();
            DashboardConfig config = profileStore.importConfig(file);
            applyConfigToInputs(config);
            initializePool();
            logLine("Imported settings from %s", file.toAbsolutePath());
            audit("settings.imported", "Settings imported", Map.of("file", file.toAbsolutePath().toString()));
        } catch (IOException ex) {
            telemetry("settings.import.error", ex);
            showError(ex.getMessage());
        }
    }

    private void loadCustomProfiles() {
        try {
            customProfiles.clear();
            customProfiles.putAll(profileStore.loadProfiles());
        } catch (IOException ex) {
            telemetry("profile.load.error", ex);
        }
    }

    private void refreshProfileCombo() {
        if (profileCombo == null) {
            return;
        }
        profileCombo.removeAllItems();
        for (String preset : builtInPresets.keySet()) {
            profileCombo.addItem(preset);
        }
        for (String profile : customProfiles.keySet()) {
            if (!builtInPresets.containsKey(profile)) {
                profileCombo.addItem(profile);
            }
        }
        if (profileCombo.getItemCount() > 0 && profileCombo.getSelectedIndex() == -1) {
            profileCombo.setSelectedIndex(0);
        }
    }

    private void startRefreshTimer() {
        stopRefreshTimer();
        refreshTimer = new Timer(400, e -> refreshView());
        refreshTimer.start();
    }

    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }

    private void disposePool() {
        if (pool != null) {
            pool.dispose();
            pool = null;
        }
    }

    private void wireValidationListeners() {
        ChangeListener changeListener = e -> {
            updateValidationHint();
            applyThemeAndAccessibility();
        };

        initialSizeSpinner.addChangeListener(changeListener);
        maxSizeSpinner.addChangeListener(changeListener);
        collectFrequencySpinner.addChangeListener(changeListener);
        resetValueSpinner.addChangeListener(changeListener);
        benchmarkOpsSpinner.addChangeListener(changeListener);
        fontScaleSpinner.addChangeListener(changeListener);
        themeCombo.addActionListener(e -> {
            updateValidationHint();
            applyThemeAndAccessibility();
        });
        highContrastCheck.addActionListener(e -> {
            updateValidationHint();
            applyThemeAndAccessibility();
        });
    }

    private void updateValidationHint() {
        DashboardConfig config = readConfigFromInputs().sanitized();
        ValidationResult result = config.validate();

        if (result.valid) {
            validationHintLabel.setText("Configuration valid.");
            validationHintLabel.setForeground(new Color(44, 131, 63));
            createPoolButton.setEnabled(true);
            acquireButton.setEnabled(pool != null);
            runBenchmarkButton.setEnabled(true);
        } else {
            validationHintLabel.setText(result.message);
            validationHintLabel.setForeground(new Color(184, 45, 45));
            createPoolButton.setEnabled(false);
            acquireButton.setEnabled(false);
            runBenchmarkButton.setEnabled(false);
        }
    }

    private DashboardConfig readConfigFromInputs() {
        return new DashboardConfig(
            (Integer) initialSizeSpinner.getValue(),
            (Integer) maxSizeSpinner.getValue(),
            (Integer) collectFrequencySpinner.getValue(),
            (Integer) resetValueSpinner.getValue(),
            (Integer) benchmarkOpsSpinner.getValue(),
            Objects.requireNonNull((ThemeMode) themeCombo.getSelectedItem()),
            (Integer) fontScaleSpinner.getValue(),
            highContrastCheck.isSelected()
        );
    }

    private void applyConfigToInputs(DashboardConfig config) {
        initialSizeSpinner.setValue(config.initialSize);
        maxSizeSpinner.setValue(config.maxSize);
        collectFrequencySpinner.setValue(config.collectFrequencyMs);
        resetValueSpinner.setValue(config.resetValue);
        benchmarkOpsSpinner.setValue(config.benchmarkOperations);
        themeCombo.setSelectedItem(config.themeMode);
        fontScaleSpinner.setValue(config.fontScalePercent);
        highContrastCheck.setSelected(config.highContrast);
    }

    private void applyThemeAndAccessibility() {
        if (frame == null) {
            return;
        }

        DashboardConfig config = readConfigFromInputs().sanitized();
        Palette palette = palette();

        rootPanel.setBackground(palette.background);
        headerPanel.repaint();

        for (JPanel card : List.of(controlsCard, activeCard, availableCard, statusCard, detailsCard, logCard)) {
            card.setBackground(palette.card);
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.border),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));
        }

        activeList.setBackground(palette.surface);
        activeList.setForeground(palette.text);
        availableList.setBackground(palette.surface);
        availableList.setForeground(palette.text);

        for (Component component : List.of(
            validationHintLabel,
            benchmarkLabel,
            activeCountLabel,
            availableCountLabel,
            utilizationLabel,
            trendLabel
        )) {
            component.setForeground(palette.text);
        }

        eventLog.setBackground(palette.surface);
        eventLog.setForeground(palette.text);
        detailArea.setBackground(palette.surface);
        detailArea.setForeground(palette.text);

        filterField.setBackground(palette.surface);
        filterField.setForeground(palette.text);
        profileNameField.setBackground(palette.surface);
        profileNameField.setForeground(palette.text);

        for (JButton primary : List.of(createPoolButton, acquireButton)) {
            primary.setBackground(palette.accent);
            primary.setForeground(Color.WHITE);
        }

        for (JButton secondary : List.of(
            releaseSelectedButton,
            releaseLatestButton,
            runBenchmarkButton,
            saveProfileButton,
            loadProfileButton,
            deleteProfileButton,
            exportConfigButton,
            importConfigButton
        )) {
            secondary.setBackground(palette.surface);
            secondary.setForeground(palette.text);
        }

        float scale = config.fontScalePercent / 100.0f;
        applyFontScale(frame, scale);

        frame.revalidate();
        frame.repaint();
    }

    private Palette palette() {
        DashboardConfig config = readConfigFromInputs().sanitized();
        if (config.themeMode == ThemeMode.DARK) {
            if (config.highContrast) {
                return new Palette(
                    new Color(14, 14, 16),
                    new Color(25, 25, 29),
                    new Color(35, 35, 41),
                    new Color(74, 74, 82),
                    new Color(234, 234, 238),
                    new Color(123, 183, 255),
                    new Color(54, 92, 148),
                    new Color(38, 67, 121)
                );
            }
            return new Palette(
                new Color(27, 29, 34),
                new Color(37, 40, 47),
                new Color(45, 48, 56),
                new Color(74, 80, 92),
                new Color(228, 231, 237),
                new Color(102, 168, 255),
                new Color(38, 67, 121),
                new Color(21, 43, 86)
            );
        }

        if (config.highContrast) {
            return new Palette(
                new Color(241, 246, 252),
                new Color(255, 255, 255),
                new Color(250, 251, 255),
                new Color(160, 176, 198),
                new Color(21, 25, 31),
                new Color(40, 122, 202),
                new Color(31, 92, 154),
                new Color(20, 66, 120)
            );
        }

        return new Palette(
            new Color(243, 247, 252),
            Color.WHITE,
            new Color(250, 252, 255),
            new Color(214, 225, 239),
            new Color(26, 31, 38),
            new Color(41, 128, 185),
            new Color(37, 166, 154),
            new Color(33, 74, 135)
        );
    }

    private void applyFontScale(Component component, float scale) {
        Font currentFont = component.getFont();
        if (currentFont != null) {
            Font baseFont = currentFont;
            if (component instanceof JComponent) {
                JComponent jComponent = (JComponent) component;
                Object stored = jComponent.getClientProperty("baseFont");
                if (stored instanceof Font) {
                    baseFont = (Font) stored;
                } else {
                    jComponent.putClientProperty("baseFont", currentFont);
                }
            }
            float newSize = Math.max(11f, baseFont.getSize2D() * scale);
            component.setFont(baseFont.deriveFont(newSize));
        }

        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                applyFontScale(child, scale);
            }
        }
    }

    private static JPanel createCardPanel(BorderLayout layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private static JPanel createCardPanel(GridLayout layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return panel;
    }

    private static JLabel sectionTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 17));
        return label;
    }

    private static JLabel metricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return label;
    }

    private static JSpinner spinner(int value, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        JFormattedTextField textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        textField.setColumns(7);
        return spinner;
    }

    private static void addField(JPanel panel, GridBagConstraints gbc, int column, String label, Component field) {
        gbc.gridx = column;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridy = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        button.setFocusPainted(false);
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        button.setFocusPainted(false);
        return button;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
        logLine("Error: %s", message);
    }

    private void logLine(String format, Object... args) {
        String line = String.format(format, args);
        String decorated = "[" + LocalDateTime.now().format(CLOCK_FORMAT) + "] " + line;
        eventLog.append(decorated + "\n");
        eventLog.setCaretPosition(eventLog.getDocument().getLength());
        persistence.appendSessionEvent(line);
    }

    private void audit(String action, String message, Map<String, String> fields) {
        try {
            persistence.logEvent(action, message, fields);
        } catch (Exception ignored) {
            // Avoid crashing the app if telemetry is unavailable.
        }
    }

    private void telemetry(String action, Throwable throwable) {
        persistence.logError(action, throwable);
        audit(action, "error", Map.of("error", throwable == null ? "unknown" : String.valueOf(throwable.getMessage())));
    }

    private static Map<String, DashboardConfig> createBuiltInPresets() {
        Map<String, DashboardConfig> presets = new LinkedHashMap<>();
        presets.put("Balanced", DashboardConfig.defaults());
        presets.put(
            "Throughput",
            new DashboardConfig(8, 120, 2000, 0, 60000, ThemeMode.DARK, 100, false)
        );
        presets.put(
            "Memory Saver",
            new DashboardConfig(1, 12, -1, 0, 20000, ThemeMode.LIGHT, 100, false)
        );
        return presets;
    }

    private static final class Palette {
        private final Color background;
        private final Color card;
        private final Color surface;
        private final Color border;
        private final Color text;
        private final Color accent;
        private final Color headerEnd;
        private final Color headerStart;

        private Palette(
            Color background,
            Color card,
            Color surface,
            Color border,
            Color text,
            Color accent,
            Color headerEnd,
            Color headerStart
        ) {
            this.background = background;
            this.card = card;
            this.surface = surface;
            this.border = border;
            this.text = text;
            this.accent = accent;
            this.headerEnd = headerEnd;
            this.headerStart = headerStart;
        }
    }

    private static final class PooledItem {
        private final int id;
        private int value;
        private String state;
        private int borrowCount;
        private final LocalDateTime createdAt;
        private LocalDateTime lastUsedAt;

        private PooledItem(int id, int value, String state) {
            this.id = id;
            this.value = value;
            this.state = state;
            this.borrowCount = 0;
            this.createdAt = LocalDateTime.now();
            this.lastUsedAt = createdAt;
        }

        static PooledItem fresh(int id) {
            return new PooledItem(id, 0, "fresh");
        }

        String summary() {
            return "Item#" + id + " value=" + value + " state=" + state;
        }

        String searchable() {
            return ("item#" + id + " " + value + " " + state).toLowerCase();
        }

        @Override
        public String toString() {
            return "Item#" + id + " | value=" + value + " | " + state + " | borrows=" + borrowCount;
        }
    }
}
