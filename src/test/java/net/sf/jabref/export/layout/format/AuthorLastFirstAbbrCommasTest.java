/*
 * Copyright (C) 2006 Jabref-Team
 * 
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author $Author$
 * @version $Revision$ ($Date$)
 */
public class AuthorLastFirstAbbrCommasTest {

    /**
     * Test method for {@link net.sf.jabref.export.layout.format.AuthorLastFirstAbbrCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorLastFirstAbbrCommas();

        // Empty case
        assertEquals("", a.format(""));

        // Single Names
        assertEquals("Someone, V. S.", a.format("Van Something Someone"));

        // Two names
        assertEquals("von Neumann, J. and Black Brown, P.", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));

    }

}
