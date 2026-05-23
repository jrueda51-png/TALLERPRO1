package controlGastos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase utilitaria para gestionar la conexion a Oracle.
 *
 * Requisitos en NetBeans:
 * - Agregar el .jar de Oracle JDBC a Libraries del proyecto.
 * - Verificar IP, puerto, SID, usuario y clave.
 */
public class ConexionDB {

    private static final String URL = "jdbc:oracle:thin:@192.168.254.215:1521:orcl";
    private static final String USUARIO = "TALLERPRO1";
    private static final String PASSWORD = "TALLERPRO1";

    private static Connection instancia;

    private ConexionDB() {
        // Evita instancias de esta clase utilitaria.
    }

    /** Devuelve una conexion activa reutilizable. */
    public static synchronized Connection getConexion() {
        try {
            if (instancia == null || instancia.isClosed()) {
                cargarDriverOracle();
                instancia = DriverManager.getConnection(URL, USUARIO, PASSWORD);
                instancia.setAutoCommit(false);
                System.out.println("Conexion a Oracle establecida.");
            }
            return instancia;
        } catch (ClassNotFoundException e) {
            System.err.println("Driver Oracle no encontrado: " + e.getMessage());
            System.err.println("Agrega ojdbc8.jar/ojdbc6.jar/ojdbc14.jar en Libraries del proyecto.");
        } catch (SQLException e) {
            System.err.println("Error al conectar con Oracle: " + e.getMessage());
        }
        return null;
    }

    /**
     * Carga el driver moderno y, si no existe, intenta con el nombre antiguo.
     * Esto ayuda con diferentes versiones de ojdbc usadas en NetBeans.
     */
    private static void cargarDriverOracle() throws ClassNotFoundException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException ex) {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        }
    }

    /** Confirma cambios pendientes. */
    public static void commit() {
        try {
            if (instancia != null && !instancia.isClosed()) {
                instancia.commit();
            }
        } catch (SQLException e) {
            System.err.println("Error al confirmar transaccion: " + e.getMessage());
        }
    }

    /** Revierte cambios pendientes. */
    public static void rollback() {
        try {
            if (instancia != null && !instancia.isClosed()) {
                instancia.rollback();
            }
        } catch (SQLException e) {
            System.err.println("Error al revertir transaccion: " + e.getMessage());
        }
    }

    /** Cierra la conexion de forma segura. */
    public static synchronized void cerrar() {
        try {
            if (instancia != null && !instancia.isClosed()) {
                instancia.close();
                System.out.println("Conexion cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexion: " + e.getMessage());
        } finally {
            instancia = null;
        }
    }
}
