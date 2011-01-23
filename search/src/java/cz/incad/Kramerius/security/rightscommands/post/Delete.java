/*
 * Copyright (C) 2010 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.Kramerius.security.rightscommands.post;

import java.sql.SQLException;
import java.util.logging.Level;

import cz.incad.Kramerius.security.RightsServlet;
import cz.incad.Kramerius.security.ServletCommand;
import cz.incad.Kramerius.security.rightscommands.ServletRightsCommand;
import cz.incad.kramerius.security.Right;

public class Delete extends ServletRightsCommand {

    static java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Delete.class.getName());
    
    @Override
    public void doCommand() {
        try {
            Right right = RightsServlet.createRightFromPostIds(this.requestProvider.get(), rightsManager, userManager);
            rightsManager.deleteRight(right);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } catch (InstantiationException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(),e);
        }
    }

    
}
