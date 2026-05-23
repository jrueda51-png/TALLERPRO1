package controlGastos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO con CRUD para GASTOS y GANANCIAS.
 */
public class GastoDAO {

    // ------------------------------------------------------------------
    // MODELOS
    // ------------------------------------------------------------------

    public static class FilaGasto {
        public int idGasto;
        public int idMes;
        public String categoria;
        public double previsto;
        public double real;
        public double diferencia;

        public FilaGasto() {
        }

        public FilaGasto(int idGasto, int idMes, String categoria,
                         double previsto, double real, double diferencia) {
            this.idGasto = idGasto;
            this.idMes = idMes;
            this.categoria = categoria;
            this.previsto = previsto;
            this.real = real;
            this.diferencia = diferencia;
        }

        @Override
        public String toString() {
            return String.format("[%d] %s | Prev: %.2f | Real: %.2f | Dif: %.2f",
                    idGasto, categoria, previsto, real, diferencia);
        }
    }

    public static class FilaGanancia {
        public int idGanancia;
        public int idMes;
        public String categoria;
        public double previsto;
        public double real;
        public double diferencia;

        public FilaGanancia() {
        }

        public FilaGanancia(int idGanancia, int idMes, String categoria,
                            double previsto, double real, double diferencia) {
            this.idGanancia = idGanancia;
            this.idMes = idMes;
            this.categoria = categoria;
            this.previsto = previsto;
            this.real = real;
            this.diferencia = diferencia;
        }
    }

    public static class InformeMensual {
        public int anio;
        public int mes;
        public double saldoInicial;
        public double totalGastoPrev;
        public double totalGastoReal;
        public double totalGananciaPrev;
        public double totalGananciaReal;
        public double saldoFinal;
    }

    // ------------------------------------------------------------------
    // UTILIDADES INTERNAS
    // ------------------------------------------------------------------

    private static Connection conexion() throws SQLException {
        Connection con = ConexionDB.getConexion();
        if (con == null) {
            throw new SQLException("No se pudo obtener conexion. Revisa driver, IP, SID, usuario y clave.");
        }
        return con;
    }

    private static void error(String metodo, SQLException e) {
        System.err.println(metodo + ": " + e.getMessage());
        ConexionDB.rollback();
    }

    /**
     * Busca el ID_MES correspondiente al anio y mes seleccionados en la interfaz.
     * Retorna null cuando el mes no existe en PRESUPUESTO_MES.
     */
    public static Integer obtenerIdMes(int anio, int mes) {
        String sql = "SELECT ID_MES FROM PRESUPUESTO_MES WHERE ANIO = ? AND MES = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setInt(2, mes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ID_MES");
                }
            }
        } catch (SQLException e) {
            error("obtenerIdMes", e);
        }
        return null;
    }

    // ------------------------------------------------------------------
    // CRUD - GASTOS
    // ------------------------------------------------------------------

    /** Inserta un gasto usando directamente el ID_MES. */
    public static boolean crearGasto(int idMes, String categoria,
                                     double previsto, double real) {
        String sql = "INSERT INTO GASTOS (ID_GASTO, ID_MES, CATEGORIA, PREVISTO, REAL_GASTO) "
                + "VALUES (SEQ_GASTO.NEXTVAL, ?, ?, ?, ?)";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMes);
            ps.setString(2, categoria);
            ps.setDouble(3, previsto);
            ps.setDouble(4, real);
            ps.executeUpdate();
            con.commit();
            return true;
        } catch (SQLException e) {
            error("crearGasto", e);
            return false;
        }
    }

    /** Inserta un gasto usando anio/mes de la pantalla. */
    public static boolean crearGastoPorFecha(int anio, int mes, String categoria,
                                             double previsto, double real) {
        Integer idMes = obtenerIdMes(anio, mes);
        if (idMes == null) {
            System.err.println("crearGastoPorFecha: no existe PRESUPUESTO_MES para " + anio + "-" + mes);
            return false;
        }
        return crearGasto(idMes, categoria, previsto, real);
    }

    public static List<FilaGasto> listarGastos(int anio, int mes) {
        List<FilaGasto> lista = new ArrayList<>();
        String sql = "SELECT g.ID_GASTO, g.ID_MES, g.CATEGORIA, g.PREVISTO, "
                + "g.REAL_GASTO, (g.REAL_GASTO - g.PREVISTO) AS DIFERENCIA "
                + "FROM GASTOS g "
                + "JOIN PRESUPUESTO_MES pm ON g.ID_MES = pm.ID_MES "
                + "WHERE pm.ANIO = ? AND pm.MES = ? "
                + "ORDER BY g.CATEGORIA";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setInt(2, mes);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new FilaGasto(
                            rs.getInt("ID_GASTO"),
                            rs.getInt("ID_MES"),
                            rs.getString("CATEGORIA"),
                            rs.getDouble("PREVISTO"),
                            rs.getDouble("REAL_GASTO"),
                            rs.getDouble("DIFERENCIA")));
                }
            }
        } catch (SQLException e) {
            error("listarGastos", e);
        }
        return lista;
    }

    public static FilaGasto obtenerGastoPorId(int idGasto) {
        String sql = "SELECT ID_GASTO, ID_MES, CATEGORIA, PREVISTO, REAL_GASTO, "
                + "(REAL_GASTO - PREVISTO) AS DIFERENCIA "
                + "FROM GASTOS WHERE ID_GASTO = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGasto);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FilaGasto(
                            rs.getInt("ID_GASTO"),
                            rs.getInt("ID_MES"),
                            rs.getString("CATEGORIA"),
                            rs.getDouble("PREVISTO"),
                            rs.getDouble("REAL_GASTO"),
                            rs.getDouble("DIFERENCIA"));
                }
            }
        } catch (SQLException e) {
            error("obtenerGastoPorId", e);
        }
        return null;
    }

    public static boolean actualizarGasto(int idGasto, String categoria,
                                          double previsto, double real) {
        String sql = "UPDATE GASTOS SET CATEGORIA = ?, PREVISTO = ?, REAL_GASTO = ? "
                + "WHERE ID_GASTO = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, categoria);
            ps.setDouble(2, previsto);
            ps.setDouble(3, real);
            ps.setInt(4, idGasto);
            int filas = ps.executeUpdate();
            con.commit();
            return filas > 0;
        } catch (SQLException e) {
            error("actualizarGasto", e);
            return false;
        }
    }

    public static boolean eliminarGasto(int idGasto) {
        String sql = "DELETE FROM GASTOS WHERE ID_GASTO = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGasto);
            int filas = ps.executeUpdate();
            con.commit();
            return filas > 0;
        } catch (SQLException e) {
            error("eliminarGasto", e);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // CRUD - GANANCIAS
    // ------------------------------------------------------------------

    /** Inserta una ganancia usando directamente el ID_MES. */
    public static boolean crearGanancia(int idMes, String categoria,
                                        double previsto, double real) {
        String sql = "INSERT INTO GANANCIAS (ID_GANANCIA, ID_MES, CATEGORIA, PREVISTO, REAL_GANANCIA) "
                + "VALUES (SEQ_GANANCIA.NEXTVAL, ?, ?, ?, ?)";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idMes);
            ps.setString(2, categoria);
            ps.setDouble(3, previsto);
            ps.setDouble(4, real);
            ps.executeUpdate();
            con.commit();
            return true;
        } catch (SQLException e) {
            error("crearGanancia", e);
            return false;
        }
    }

    /** Inserta una ganancia usando anio/mes de la pantalla. */
    public static boolean crearGananciaPorFecha(int anio, int mes, String categoria,
                                                double previsto, double real) {
        Integer idMes = obtenerIdMes(anio, mes);
        if (idMes == null) {
            System.err.println("crearGananciaPorFecha: no existe PRESUPUESTO_MES para " + anio + "-" + mes);
            return false;
        }
        return crearGanancia(idMes, categoria, previsto, real);
    }

    public static List<FilaGanancia> listarGanancias(int anio, int mes) {
        List<FilaGanancia> lista = new ArrayList<>();
        String sql = "SELECT ga.ID_GANANCIA, ga.ID_MES, ga.CATEGORIA, ga.PREVISTO, "
                + "ga.REAL_GANANCIA, (ga.REAL_GANANCIA - ga.PREVISTO) AS DIFERENCIA "
                + "FROM GANANCIAS ga "
                + "JOIN PRESUPUESTO_MES pm ON ga.ID_MES = pm.ID_MES "
                + "WHERE pm.ANIO = ? AND pm.MES = ? "
                + "ORDER BY ga.CATEGORIA";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setInt(2, mes);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new FilaGanancia(
                            rs.getInt("ID_GANANCIA"),
                            rs.getInt("ID_MES"),
                            rs.getString("CATEGORIA"),
                            rs.getDouble("PREVISTO"),
                            rs.getDouble("REAL_GANANCIA"),
                            rs.getDouble("DIFERENCIA")));
                }
            }
        } catch (SQLException e) {
            error("listarGanancias", e);
        }
        return lista;
    }

    public static FilaGanancia obtenerGananciaPorId(int idGanancia) {
        String sql = "SELECT ID_GANANCIA, ID_MES, CATEGORIA, PREVISTO, REAL_GANANCIA, "
                + "(REAL_GANANCIA - PREVISTO) AS DIFERENCIA "
                + "FROM GANANCIAS WHERE ID_GANANCIA = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGanancia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FilaGanancia(
                            rs.getInt("ID_GANANCIA"),
                            rs.getInt("ID_MES"),
                            rs.getString("CATEGORIA"),
                            rs.getDouble("PREVISTO"),
                            rs.getDouble("REAL_GANANCIA"),
                            rs.getDouble("DIFERENCIA"));
                }
            }
        } catch (SQLException e) {
            error("obtenerGananciaPorId", e);
        }
        return null;
    }

    public static boolean actualizarGanancia(int idGanancia, String categoria,
                                             double previsto, double real) {
        String sql = "UPDATE GANANCIAS SET CATEGORIA = ?, PREVISTO = ?, REAL_GANANCIA = ? "
                + "WHERE ID_GANANCIA = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, categoria);
            ps.setDouble(2, previsto);
            ps.setDouble(3, real);
            ps.setInt(4, idGanancia);
            int filas = ps.executeUpdate();
            con.commit();
            return filas > 0;
        } catch (SQLException e) {
            error("actualizarGanancia", e);
            return false;
        }
    }

    public static boolean eliminarGanancia(int idGanancia) {
        String sql = "DELETE FROM GANANCIAS WHERE ID_GANANCIA = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idGanancia);
            int filas = ps.executeUpdate();
            con.commit();
            return filas > 0;
        } catch (SQLException e) {
            error("eliminarGanancia", e);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // INFORME MENSUAL
    // ------------------------------------------------------------------

    public static InformeMensual obtenerInforme(int anio, int mes) {
        String sql = "SELECT ANIO, MES, SALDO_INICIAL, "
                + "TOTAL_GASTO_PREV, TOTAL_GASTO_REAL, "
                + "TOTAL_GANANCIA_PREV, TOTAL_GANANCIA_REAL, SALDO_FINAL "
                + "FROM V_INFORME_MENSUAL WHERE ANIO = ? AND MES = ?";
        try (Connection con = conexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setInt(2, mes);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    InformeMensual inf = new InformeMensual();
                    inf.anio = rs.getInt("ANIO");
                    inf.mes = rs.getInt("MES");
                    inf.saldoInicial = rs.getDouble("SALDO_INICIAL");
                    inf.totalGastoPrev = rs.getDouble("TOTAL_GASTO_PREV");
                    inf.totalGastoReal = rs.getDouble("TOTAL_GASTO_REAL");
                    inf.totalGananciaPrev = rs.getDouble("TOTAL_GANANCIA_PREV");
                    inf.totalGananciaReal = rs.getDouble("TOTAL_GANANCIA_REAL");
                    inf.saldoFinal = rs.getDouble("SALDO_FINAL");
                    return inf;
                }
            }
        } catch (SQLException e) {
            error("obtenerInforme", e);
        }
        return null;
    }
}
