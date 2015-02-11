/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sonews.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Class that allows simple String template handling.
 * 
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class StringTemplate {

    private String str = null;
    private String templateDelimiter = "%";
    private Map<String, String> templateValues = new HashMap<String, String>();

    public StringTemplate(String str, final String templateDelimiter) {
        if (str == null || templateDelimiter == null) {
            throw new IllegalArgumentException("null arguments not allowed");
        }

        this.str = str;
        this.templateDelimiter = templateDelimiter;
    }

    public StringTemplate(String str) {
        this(str, "%");
    }

    public StringTemplate set(String template, String value) {
        if (template == null || value == null) {
            throw new IllegalArgumentException("null arguments not allowed");
        }

        this.templateValues.put(template, value);
        return this;
    }

    public StringTemplate set(String template, long value) {
        return set(template, Long.toString(value));
    }

    public StringTemplate set(String template, double value) {
        return set(template, Double.toString(value));
    }

    public StringTemplate set(String template, Object obj) {
        if (template == null || obj == null) {
            throw new IllegalArgumentException("null arguments not allowed");
        }

        return set(template, obj.toString());
    }

    @Override
    public String toString() {
        String ret = str;

        for (String key : this.templateValues.keySet()) {
            String value = this.templateValues.get(key);
            ret = ret.replace(templateDelimiter + key, value);
        }

        return ret;
    }
}
