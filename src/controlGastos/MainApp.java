package controlGastos;

import controlGastos.GastoDAO.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

/**
 * Interfaz gráfica Swing con CRUD completo de Gastos y Ganancias.
 * Dos pestañas: Resumen del mes | Administrar registros
 */
public class MainApp extends JFrame {

    // -- Paleta ----------------------------------------------------------
    private static final Color NARANJA = new Color(0xE8552B);
    private static final Color AZUL    = new Color(0x2563EB);
    private static final Color VERDE   = new Color(0x16A34A);
    private static final Color ROJO    = new Color(0xDC2626);
    private static final Color FONDO   = new Color(0xF7F7F5);
    private static final Color CARD    = Color.WHITE;
    private static final Color TEXTO   = new Color(0x1C1C1A);
    private static final Color GRIS    = new Color(0x6B6B67);

    // -- Controles de mes ------------------------------------------------
    private JSpinner spAnio, spMes;

    // -- Pestaña RESUMEN -------------------------------------------------
    private JLabel lblSaldoInicial, lblSaldoFinal, lblAhorro, lblPorcAhorro;
    private JLabel lblTotGP, lblTotGR, lblTotNP, lblTotNR;
    private DefaultTableModel mdlResGastos, mdlResGanancias;

    // -- Pestaña ADMINISTRAR ---------------------------------------------
    private DefaultTableModel mdlAdmGastos, mdlAdmGanancias;
    private JTable tblAdmGastos, tblAdmGanancias;

    // -- IDs seleccionados -----------------------------------------------
    private int gastoSelId    = -1;
    private int gananciaSelId = -1;

    // ====================================================================
    public MainApp() {
        setTitle("Control de Gastos Mensuales — CRUD");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ConexionDB.cerrar();
            }
        });
        setSize(1150, 740);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FONDO);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.addTab("  Resumen del mes  ",  buildTabResumen());
        tabs.addTab("  Administrar registros  ", buildTabAdmin());
        tabs.setBackground(FONDO);
        add(tabs, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
        cargarResumen();
        cargarTablaAdmin();
    }

    // ====================================================================
    //  HEADER
    // ====================================================================
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout(20, 0));
        h.setBackground(TEXTO);
        h.setBorder(new EmptyBorder(13, 24, 13, 24));

        JLabel titulo = new JLabel("Presupuesto mensual");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(Color.WHITE);

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ctrl.setOpaque(false);

        spAnio = new JSpinner(new SpinnerNumberModel(2025, 2020, 2030, 1));
        spMes  = new JSpinner(new SpinnerNumberModel(5, 1, 12, 1));
        estSpinner(spAnio, 70); estSpinner(spMes, 55);

        JButton btnVer = btn("Ver informe", NARANJA);
        btnVer.addActionListener(e -> { cargarResumen(); cargarTablaAdmin(); });

        ctrl.add(label("Año:", Color.WHITE)); ctrl.add(spAnio);
        ctrl.add(label("Mes:", Color.WHITE)); ctrl.add(spMes);
        ctrl.add(btnVer);

        h.add(titulo, BorderLayout.WEST);
        h.add(ctrl,   BorderLayout.EAST);
        return h;
    }

    // ====================================================================
    //  PESTAÑA RESUMEN
    // ====================================================================
    private JPanel buildTabResumen() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(14, 20, 10, 20));

        // KPIs
        JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 0));
        kpis.setBackground(FONDO);
        kpis.setPreferredSize(new Dimension(0, 108));
        lblSaldoInicial = new JLabel("$ 0"); lblSaldoFinal = new JLabel("$ 0");
        lblAhorro       = new JLabel("$ 0"); lblPorcAhorro  = new JLabel("0 %");
        kpis.add(kpi("Saldo inicial",    lblSaldoInicial, AZUL));
        kpis.add(kpi("Saldo final",      lblSaldoFinal,   NARANJA));
        kpis.add(kpi("Ahorro del mes",   lblAhorro,       VERDE));
        kpis.add(kpi("% aumento ahorro", lblPorcAhorro,   new Color(0x7C3AED)));
        p.add(kpis, BorderLayout.NORTH);

        // Tablas resumen
        mdlResGastos    = tableModel();
        mdlResGanancias = tableModel();
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panelTablaResumen("Gastos",    mdlResGastos,    true),
                panelTablaResumen("Ganancias", mdlResGanancias, false));
        sp.setDividerLocation(560);
        sp.setDividerSize(7);
        sp.setBorder(null);
        sp.setBackground(FONDO);
        p.add(sp, BorderLayout.CENTER);

        // Totales
        JPanel tots = new JPanel(new GridLayout(1, 2, 12, 0));
        tots.setBackground(FONDO);
        tots.setPreferredSize(new Dimension(0, 34));
        JPanel tg = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        tg.setBackground(FONDO);
        lblTotGP = totLabel("Previsto: $0"); lblTotGR = totLabel("Real: $0");
        tg.add(lblTotGP); tg.add(lblTotGR);
        JPanel tn = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        tn.setBackground(FONDO);
        lblTotNP = totLabel("Previsto: $0"); lblTotNR = totLabel("Real: $0");
        tn.add(lblTotNP); tn.add(lblTotNR);
        tots.add(tg); tots.add(tn);
        p.add(tots, BorderLayout.SOUTH);
        return p;
    }

    private JPanel panelTablaResumen(String titulo, DefaultTableModel mdl, boolean esGasto) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(FONDO);
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(esGasto ? NARANJA : VERDE);
        JTable t = makeTable(mdl);
        p.add(lbl,                   BorderLayout.NORTH);
        p.add(new JScrollPane(t),    BorderLayout.CENTER);
        return p;
    }

    // ====================================================================
    //  PESTAÑA ADMINISTRAR (CRUD)
    // ====================================================================
    private JPanel buildTabAdmin() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(FONDO);
        p.setBorder(new EmptyBorder(14, 20, 10, 20));

        // Columnas para tablas admin (incluyen ID)
        String[] cols = {"ID", "Categoría", "Previsto ($)", "Real ($)", "Diferencia ($)"};
        mdlAdmGastos    = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        mdlAdmGanancias = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblAdmGastos    = makeTable(mdlAdmGastos);
        tblAdmGanancias = makeTable(mdlAdmGanancias);

        // Listeners de selección
        tblAdmGastos.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblAdmGastos.getSelectedRow();
            if (row >= 0) row = tblAdmGastos.convertRowIndexToModel(row);
            gastoSelId = row >= 0 ? (int) mdlAdmGastos.getValueAt(row, 0) : -1;
        });
        tblAdmGanancias.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblAdmGanancias.getSelectedRow();
            if (row >= 0) row = tblAdmGanancias.convertRowIndexToModel(row);
            gananciaSelId = row >= 0 ? (int) mdlAdmGanancias.getValueAt(row, 0) : -1;
        });

        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                panelCRUD("Gastos",    tblAdmGastos,    true),
                panelCRUD("Ganancias", tblAdmGanancias, false));
        sp.setDividerLocation(560);
        sp.setDividerSize(7);
        sp.setBorder(null);
        sp.setBackground(FONDO);
        p.add(sp, BorderLayout.CENTER);

        JLabel hint = new JLabel("Selecciona una fila para editar o eliminar");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hint.setForeground(GRIS);
        hint.setBorder(new EmptyBorder(4, 0, 0, 0));
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private JPanel panelCRUD(String titulo, JTable tabla, boolean esGasto) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(FONDO);

        // Encabezado
        JLabel lbl = new JLabel(titulo);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(esGasto ? NARANJA : VERDE);

        // Botones CRUD
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botones.setBackground(FONDO);

        JButton btnNuevo    = btn("+ Nuevo",   AZUL);
        JButton btnEditar   = btn("Editar",    new Color(0xCA8A04));
        JButton btnEliminar = btn("Eliminar",  ROJO);
        JButton btnRefresh  = btn("Actualizar",GRIS);

        btnNuevo.addActionListener(e -> dialogoNuevo(esGasto));
        btnEditar.addActionListener(e -> dialogoEditar(esGasto));
        btnEliminar.addActionListener(e -> confirmarEliminar(esGasto));
        btnRefresh.addActionListener(e -> cargarTodo());

        botones.add(btnNuevo); botones.add(btnEditar);
        botones.add(btnEliminar); botones.add(btnRefresh);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(FONDO);
        top.add(lbl,     BorderLayout.WEST);
        top.add(botones, BorderLayout.EAST);

        p.add(top,                   BorderLayout.NORTH);
        p.add(new JScrollPane(tabla),BorderLayout.CENTER);
        return p;
    }

    // ====================================================================
    //  DIÁLOGOS CRUD
    // ====================================================================

    /** CREATE */
    private void dialogoNuevo(boolean esGasto) {
        String tipo = esGasto ? "Gasto" : "Ganancia";
        int anio = (int) spAnio.getValue();
        int mes = (int) spMes.getValue();

        JPanel form = formPanel();
        JTextField txtCat = new JTextField(16);
        JTextField txtPrev = new JTextField("0", 10);
        JTextField txtReal = new JTextField("0", 10);

        form.add(new JLabel("Año / Mes:"));
        form.add(new JLabel(anio + " / " + mes));
        form.add(new JLabel("Categoría:"));
        form.add(txtCat);
        form.add(new JLabel("Previsto ($):"));
        form.add(txtPrev);
        form.add(new JLabel("Real ($):"));
        form.add(txtReal);

        int ok = JOptionPane.showConfirmDialog(this, form,
                "Nuevo " + tipo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            String cat = txtCat.getText().trim();
            double prev = parseMonto(txtPrev.getText());
            double real = parseMonto(txtReal.getText());

            if (cat.isEmpty()) {
                error("La categoría no puede estar vacía.");
                return;
            }

            Integer idMes = GastoDAO.obtenerIdMes(anio, mes);
            if (idMes == null) {
                error("No existe un registro en PRESUPUESTO_MES para " + anio + "/" + mes
                        + ". Primero crea ese mes en la base de datos.");
                return;
            }

            boolean ok2 = esGasto
                    ? GastoDAO.crearGasto(idMes, cat, prev, real)
                    : GastoDAO.crearGanancia(idMes, cat, prev, real);

            if (ok2) {
                exito(tipo + " creado correctamente.");
                cargarTodo();
            } else {
                error("No se pudo guardar. Revisa la consola de NetBeans y la estructura de la BD.");
            }
        } catch (NumberFormatException ex) {
            error("Verifica que los valores numéricos sean válidos. Ejemplo: 150000 o 150000.50");
        }
    }

    /** UPDATE */
    private void dialogoEditar(boolean esGasto) {
        String tipo = esGasto ? "Gasto" : "Ganancia";
        int selId   = esGasto ? gastoSelId : gananciaSelId;
        if (selId == -1) { error("Selecciona un registro de la tabla para editar."); return; }

        // Cargar datos actuales
        String catActual = ""; double prevActual = 0, realActual = 0;
        if (esGasto) {
            FilaGasto fg = GastoDAO.obtenerGastoPorId(selId);
            if (fg == null) { error("Registro no encontrado."); return; }
            catActual = fg.categoria; prevActual = fg.previsto; realActual = fg.real;
        } else {
            FilaGanancia fg = GastoDAO.obtenerGananciaPorId(selId);
            if (fg == null) { error("Registro no encontrado."); return; }
            catActual = fg.categoria; prevActual = fg.previsto; realActual = fg.real;
        }

        JPanel form = formPanel();
        JTextField txtCat  = new JTextField(catActual, 16);
        JTextField txtPrev = new JTextField(String.valueOf(prevActual), 10);
        JTextField txtReal = new JTextField(String.valueOf(realActual), 10);

        form.add(new JLabel("ID:"));        form.add(new JLabel(String.valueOf(selId)));
        form.add(new JLabel("Categoría:")); form.add(txtCat);
        form.add(new JLabel("Previsto ($):")); form.add(txtPrev);
        form.add(new JLabel("Real ($):"));  form.add(txtReal);

        int ok = JOptionPane.showConfirmDialog(this, form,
                "Editar " + tipo + " #" + selId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            String cat  = txtCat.getText().trim();
            double prev = parseMonto(txtPrev.getText());
            double real = parseMonto(txtReal.getText());
            if (cat.isEmpty()) { error("La categoría no puede estar vacía."); return; }

            boolean ok2 = esGasto
                    ? GastoDAO.actualizarGasto(selId, cat, prev, real)
                    : GastoDAO.actualizarGanancia(selId, cat, prev, real);
            if (ok2) { exito(tipo + " actualizado correctamente."); cargarTodo(); }
            else     error("No se pudo actualizar el registro.");
        } catch (NumberFormatException ex) { error("Verifica que los valores numéricos sean válidos."); }
    }

    /** DELETE */
    private void confirmarEliminar(boolean esGasto) {
        String tipo = esGasto ? "Gasto" : "Ganancia";
        int selId   = esGasto ? gastoSelId : gananciaSelId;
        if (selId == -1) { error("Selecciona un registro de la tabla para eliminar."); return; }

        int conf = JOptionPane.showConfirmDialog(this,
                "¿Eliminar " + tipo.toLowerCase() + " #" + selId + "? Esta acción no se puede deshacer.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf != JOptionPane.YES_OPTION) return;

        boolean ok = esGasto
                ? GastoDAO.eliminarGasto(selId)
                : GastoDAO.eliminarGanancia(selId);
        if (ok) {
            if (esGasto) gastoSelId = -1; else gananciaSelId = -1;
            exito(tipo + " eliminado.");
            cargarTodo();
        } else {
            error("No se pudo eliminar. El registro puede tener dependencias.");
        }
    }

    // ====================================================================
    //  CARGA DE DATOS
    // ====================================================================
    private void cargarTodo() { cargarResumen(); cargarTablaAdmin(); }

    private void cargarResumen() {
        int anio = (int) spAnio.getValue(), mes = (int) spMes.getValue();
        mdlResGastos.setRowCount(0);
        double gP = 0, gR = 0;
        for (FilaGasto f : GastoDAO.listarGastos(anio, mes)) {
            mdlResGastos.addRow(new Object[]{f.categoria,
                fmt(f.previsto), fmt(f.real), fmt(f.diferencia)});
            gP += f.previsto; gR += f.real;
        }
        lblTotGP.setText("Previsto: $" + fmt(gP));
        lblTotGR.setText("Real: $"     + fmt(gR));

        mdlResGanancias.setRowCount(0);
        double nP = 0, nR = 0;
        for (FilaGanancia f : GastoDAO.listarGanancias(anio, mes)) {
            mdlResGanancias.addRow(new Object[]{f.categoria,
                fmt(f.previsto), fmt(f.real), fmt(f.diferencia)});
            nP += f.previsto; nR += f.real;
        }
        lblTotNP.setText("Previsto: $" + fmt(nP));
        lblTotNR.setText("Real: $"     + fmt(nR));

        InformeMensual inf = GastoDAO.obtenerInforme(anio, mes);
        if (inf != null) {
            double ah   = inf.saldoFinal - inf.saldoInicial;
            double porc = inf.saldoInicial > 0 ? (ah / inf.saldoInicial) * 100 : 0;
            lblSaldoInicial.setText("$" + fmt(inf.saldoInicial));
            lblSaldoFinal  .setText("$" + fmt(inf.saldoFinal));
            lblAhorro      .setText("$" + fmt(ah));
            lblPorcAhorro  .setText(String.format("%.0f%%", porc));
        } else {
            lblSaldoInicial.setText("—"); lblSaldoFinal.setText("—");
            lblAhorro.setText("—");       lblPorcAhorro.setText("—");
        }
    }

    private void cargarTablaAdmin() {
        int anio = (int) spAnio.getValue(), mes = (int) spMes.getValue();
        mdlAdmGastos.setRowCount(0);
        gastoSelId = -1;
        for (FilaGasto f : GastoDAO.listarGastos(anio, mes)) {
            mdlAdmGastos.addRow(new Object[]{
                f.idGasto, f.categoria, fmt(f.previsto), fmt(f.real), fmt(f.diferencia)});
        }
        mdlAdmGanancias.setRowCount(0);
        gananciaSelId = -1;
        for (FilaGanancia f : GastoDAO.listarGanancias(anio, mes)) {
            mdlAdmGanancias.addRow(new Object[]{
                f.idGanancia, f.categoria, fmt(f.previsto), fmt(f.real), fmt(f.diferencia)});
        }
    }

    // ====================================================================
    //  FOOTER
    // ====================================================================
    private JPanel buildFooter() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 7));
        f.setBackground(new Color(0xEFEEEB));
        f.setBorder(new MatteBorder(1, 0, 0, 0, new Color(0xD3D1C7)));
        JLabel lbl = new JLabel("Taller 2 Corte 3 — Programación 2  |  Oracle SQL*Plus + Java Swing  |  CRUD completo");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(GRIS);
        f.add(lbl);
        return f;
    }

    // ====================================================================
    //  UTILIDADES
    // ====================================================================
    private JPanel kpi(String titulo, JLabel val, Color acento) {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(0xE5E5E3), 1, true),
                new EmptyBorder(12, 14, 12, 14)));
        JLabel t = new JLabel(titulo);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12)); t.setForeground(GRIS);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        val.setFont(new Font("Segoe UI", Font.BOLD, 20)); val.setForeground(acento);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t); p.add(Box.createVerticalStrut(6)); p.add(val);
        return p;
    }

    private JTable makeTable(DefaultTableModel mdl) {
        JTable t = new JTable(mdl);
        t.setRowHeight(25); t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(0xF1F0EC));
        t.setGridColor(new Color(0xE5E5E3)); t.setSelectionBackground(new Color(0xDBEAFE));
        t.setFillsViewportHeight(true); t.setShowGrid(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        return t;
    }

    private DefaultTableModel tableModel() {
        return new DefaultTableModel(
            new String[]{"Categoría", "Previsto ($)", "Real ($)", "Diferencia ($)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JPanel formPanel() {
        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.setBorder(new EmptyBorder(10, 10, 10, 10));
        return f;
    }

    private JButton btn(String txt, Color bg) {
        JButton b = new JButton(txt);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(5, 12, 5, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel label(String txt, Color c) {
        JLabel l = new JLabel(txt); l.setForeground(c);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JLabel totLabel(String txt) {
        JLabel l = new JLabel(txt); l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TEXTO); return l;
    }

    private void estSpinner(JSpinner s, int w) {
        s.setPreferredSize(new Dimension(w, 28));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private String fmt(double v) { return String.format(Locale.US, "%.2f", v); }

    private double parseMonto(String texto) {
        if (texto == null) throw new NumberFormatException("Valor vacío");
        return Double.parseDouble(texto.trim().replace(",", "."));
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    private void exito(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Listo", JOptionPane.INFORMATION_MESSAGE);
    }

    // ====================================================================
    //  MAIN
    // ====================================================================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainApp().setVisible(true));
    }
}