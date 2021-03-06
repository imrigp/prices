/*
 *
 *  * Copyright 2020 Imri
 *  *
 *  * This application is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package database;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.entities.ItemPrice;

public class ItemPriceDaoPostgres implements ItemPriceDao {
    private static final Logger log = LoggerFactory.getLogger(StoreDaoPostgres.class);
    private final DataSource ds;

    public ItemPriceDaoPostgres(DataSource ds) {
        this.ds = ds;
    }

    @NotNull
    @Override
    public List<ItemPrice> getItemsPrice(long chainId, int storeId, List<Long> itemIds) {
        ArrayList<ItemPrice> list = new ArrayList<>();

        try (Connection db = ds.getConnection();
             PreparedStatement pst = db.prepareStatement(GET_PRICES_SQL)) {

            final Array array = db.createArrayOf("BIGINT", itemIds.toArray(new Long[0]));
            pst.setLong(1, chainId);
            pst.setInt(2, storeId);
            pst.setArray(3, array);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(new ItemPrice(
                            chainId, storeId, rs.getLong("item_id"), rs.getFloat("price")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("", e);
        }

        return list;
    }

    private static final String GET_PRICES_SQL =
            "SELECT * FROM price WHERE chain_id = ? AND store_id = ? and item_id = ANY (?)";
}
