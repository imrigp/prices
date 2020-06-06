package database;

import server.Store;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class StoreDaoPostgres implements StoreDao {

    private final DataSource ds;

    public StoreDaoPostgres(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public ArrayList<Store> getChainStores(long chainId) {
        ArrayList<Store> stores = new ArrayList<>();

        try (Connection db = ds.getConnection();
             PreparedStatement pst = db.prepareStatement(GET_CHAIN_STORES_SQL)) {

            pst.setLong(1, chainId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    stores.add(extractStoreFromResult(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

        return stores;
    }

    @Override
    public Store getStore(long chainId, int storeId) {
        Store store = null;
        try (Connection db = ds.getConnection();
             PreparedStatement pst = db.prepareStatement(GET_STORE_SQL)) {

            pst.setInt(1, storeId);
            pst.setLong(2, chainId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    store = extractStoreFromResult(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return store;
    }

    @Override
    public void addStore(long chainId, Store store) {
        assert false;
    }

    @Override
    public void addStores(long chainId, ArrayList<Store> stores) {
        try (Connection db = ds.getConnection();
             PreparedStatement pst = db.prepareStatement(ADD_STORES_SQL)) {

            for (Store store : stores) {
                pst.setInt(1, store.getStoreId());
                pst.setLong(2, chainId);
                pst.setString(3, store.getName());
                pst.setString(4, store.getAddress());
                pst.setString(5, store.getCity());
                pst.setInt(6, store.getType());
                pst.addBatch();
            }
            pst.executeBatch();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private Store extractStoreFromResult(ResultSet rs) throws SQLException {
        return new Store(rs.getInt("id"),
                rs.getLong("chain_id"),
                rs.getString("name"),
                rs.getString("address"),
                rs.getString("city"),
                rs.getInt("type"));
    }

    private static final String ADD_STORES_SQL =
            "INSERT INTO store (id, chain_id, name, address, city, type) " + "VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (id) " +
                    "DO UPDATE " +
                    "SET name = EXCLUDED.name, address = EXCLUDED.address, " +
                    "city = EXCLUDED.city, type = EXCLUDED.type";
    private static final String GET_STORE_SQL = "SELECT * FROM store WHERE id = ? AND chain_id = ?";
    private static final String GET_CHAIN_STORES_SQL = "SELECT * FROM store WHERE chain_id = ?";
}