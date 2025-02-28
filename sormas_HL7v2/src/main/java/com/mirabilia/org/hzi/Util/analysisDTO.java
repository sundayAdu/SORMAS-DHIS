/*
 * Copyright (c) 2020, Mathew Official
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.mirabilia.org.hzi.Util;


import com.mirabilia.org.hzi.sormas.doa.DbConnector;
import static java.lang.System.out;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mathew Official
 */
public class analysisDTO {

    private static List<String> fieldList = new ArrayList();

    public static void fastSender(int primer, int destin, int t_val, String cunty_code) throws ClassNotFoundException {
        /*
        *matching starts from here
        *this method do the major duplication analysis and also try to match each or unit with the entire DB to ascertain there are no deplicates and hf misplacement
        *data are feed into this method by the amount sent in cutter method while in Ward/Community or HF level
         */
        String lister = "";

        PreparedStatement ps;
        ResultSet rx;

        PreparedStatement xps;
        ResultSet sorm_rs;

        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DbConnector.getConnection();

        String stack = "";
        String xstack = "";
        int found = 0;

        String dub_string = "";
        String ps_uuid = "";
        int ro = 0;

        //String pr = primer + "";
        String t_v = t_val + "";
        
         cleanParentPath(t_val);
        //  String ds = (destin + 1) + "";

        //TODO: Implement stripping mechanism from database and remove hard codes
        try {
            ps = conn.prepareStatement("SELECT UUID, name, shortname, level, updated_last, path_parent FROM raw_ where level = ? ORDER BY idx limit ?, ?;");
            ps.setString(1, t_v);
            ps.setInt(2, primer);
            ps.setInt(3, destin);

            System.out.println("Now running matching analytics at level : " + t_val);
            System.out.println("sql_TO_RUN : " + ps.toString());

            rx = ps.executeQuery();
            while (rx.next()) {
                ro = ro + 1;
                //lets clean dhis data...
                //check if org unit is valid from dhis

                //TO-DO: Get directive to strip first two prefix from FrontEND
                if (cunty_code.contains("NG")) {
                    //    System.out.println("_______________ = "+cunty_code);
                    if (rx.getString("name").contains(" ")) {
                        String[] bb = rx.getString("name").split(" ");
                        //verify if it has convetional first two prefix and strip them

                        if (bb[0].length() == 2) {
                            stack = rx.getString("name").replaceFirst(bb[0], "");
                        }else{
                        stack = rx.getString("name");
                        }
                    }

                    /* TO-DO: Get this from frontEND */
                    //lets remove the ward / LGA / HF Strings
                    switch (rx.getString("level")) {
                        case "1":
                            // code block
                            stack = stack.replaceFirst("ng ", "");
                            break;
                        case "2":
                            // code block
                            stack = stack.replaceFirst(" State", "");
                            break;
                        case "3":
                            // code block
                            stack = stack.replaceFirst(" Local Government Area", "");
                            break;

                        default:
                        // code block
                    }

                } else {
                    //    System.out.println("++++++++++++++");
                    stack = rx.getString("name");
                }

                //start comparison
                //TODO: Add what to remove while running comparisy algorithm from frontEnd
                try {
                    stack = stack.toLowerCase().replaceAll(" ", "");
                    xps = conn.prepareStatement("SELECT UUID, namex, level FROM sormas_local where level = ?");
                    xps.setString(1, t_v);
                    sorm_rs = xps.executeQuery();
                    int dub = 0;
                    System.out.println("checkpoint 1");

                    while (sorm_rs.next()) {
                        if (sorm_rs.getString("namex").length() > 2) {
                            xstack = sorm_rs.getString("namex").replaceAll(" ", "");

                            //xstack = Compare(xstack);
                            //    System.out.println(stack+"|"+xstack);
                            
                            System.out.println("stack lagos= "+stack);
                             System.out.println("stacxk lagosstate= "+xstack);
                            if (stack.equals(xstack.toLowerCase())) {

                                System.out.println("found a match at EXACT : '" + sorm_rs.getString("namex").toLowerCase() + "' matching dhis : '" + stack + "' with uuid :" + sorm_rs.getString("UUID"));

                                found = 1;
                                dub = dub + 1;
                                dub_string = rx.getString("UUID") + ", " + dub_string;
                                fieldList.add(sorm_rs.getString("UUID"));
                                ps_uuid = sorm_rs.getString("UUID");

                            } else if (stack.contains(xstack.toLowerCase())) {
                                System.out.println("found a match AT MAybe MATCH: '" + sorm_rs.getString("namex").toLowerCase() + "' matching dhis : '" + stack + "' with uuid :" + sorm_rs.getString("UUID"));

                                found = 1;
                                dub = dub + 1;
                                dub_string = rx.getString("UUID") + ", " + dub_string;
                                fieldList.add(sorm_rs.getString("UUID"));
                                ps_uuid = sorm_rs.getString("UUID");

                            }

                        }
                    }

                    /*
                     * System.out.println("duble no : "+dub);
                     * System.out.println("found : "+found);
                     * System.out.println("stack : "+stack);
                     * System.out.println("xstack : "+xstack);
                     *
                     */
                    if (dub > 1) {
                        System.out.println("DUPLICATE: times = " + dub + " likely duplicates are : " + dub_string);
                        Localizer("", dub_string, dub_string, "",rx.getString("path_parent"));

                    } else if (found > 1 || found == 1) {
                        //   System.out.println("Found a MATCH for : " + rx.getString("name")+ " at "+ ps_uuid);
                        //   System.out.println(rx.getString("uuid")+ "// "+rx.getString("updated_last")+"  //  "+ ps_uuid);
                        Localizer(rx.getString("uuid"), "", rx.getString("updated_last"), ps_uuid,rx.getString("path_parent"));
                    }
//
                    //taking care of the garbage
                    dub = 0;
                    found = 0;
                    stack = "";
                    xstack = "";
                    dub_string = "";
                    //  System.out.println(lister);
                    fieldList.clear();
                    ps_uuid = "";

                } catch (SQLException ex) {
                    out.print("SQLException: " + ex.getMessage());
                    out.print("SQLState: " + ex.getSQLState());
                    out.print("VendorError: " + ex.getErrorCode());
                }

                //  System.out.println("Total at SORCE 1 : " + rx.getString(1));
                System.out.println("number of round made = " + ro);
            }
            System.out.println(lister);
            conn.close();
        } catch (SQLException ex) {
            out.print("SQLException: " + ex.getMessage());
            out.print("SQLState: " + ex.getSQLState());
            out.print("VendorError: " + ex.getErrorCode());
        }
    }
private static void cleanParentPath(int get_level_x) throws ClassNotFoundException {
    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
    System.out.println(get_level_x);
        PreparedStatement ps_xx;
        PreparedStatement ps_x;
        ResultSet rx_xx;
        
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DbConnector.getConnection();

        try {
           
            int get_level = get_level_x - 1;
            ps_xx = conn.prepareStatement("SELECT path_parent, idx FROM raw_ where path_parent like '%/%' and level = ?");
            ps_xx.setInt(1, get_level_x);
            
            System.out.println(">>>>>>>>>>>>>>>>>>>>>"+ps_xx.toString());
            rx_xx = ps_xx.executeQuery();

            while (rx_xx.next()) {
                String rdc = rx_xx.getString(1);
                String[] ss = rdc.split("/");

                ps_x = conn.prepareStatement("update raw_ set path_parent = ? where idx = ?");
                ps_x.setString(1, ss[get_level]);
                ps_x.setString(2, rx_xx.getString(2));

                ps_x.executeUpdate();
            }

        } catch (SQLException ex) {
            Logger.getLogger(analysisDTO.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    public static void WARD_HF_fastSender(int primer, int destin, int t_val) throws ClassNotFoundException {
        //matching Ward/Community upward
        //this method do the major duplicationa analysis and also try to match each or unit with the entire DB to ascertain there are no deplicates and hf misplacement

        //data are feed into this method by the amount sent in cutter method
        String lister = "";

        PreparedStatement ps;
        ResultSet rx;

        PreparedStatement xps;
        ResultSet xrx;

        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DbConnector.getConnection();
        String stack = "";
        String xstack = "";
        int found = 0;
        String dub_string = "";
        String ps_uuid = "";
        int ro = 0;

        String pr = primer + "";
        String ds = (destin + 1) + "";

        try {

            ps = conn.prepareStatement("SELECT UUID, name, shortname, level, updated_last, path_parent FROM raw_ where idx > ? and idx < ? ORDER BY idx;");
            ps.setString(1, pr);
            ps.setString(2, ds);
            System.out.println("Now running : " + ps);

            rx = ps.executeQuery();
            while (rx.next()) {
                ro = ro + 1;
                //lets clean dhis data...
                //check if org unit is valid from dhis
                if (rx.getString("name").contains(" ")) {
                    // String[] bb = rx.getString("name").split(" ");
                    //verify if it has convetional first two prefix and strip them
                    // if (bb[0].length() == 2) {
                    //   stack = rx.getString("name").replaceFirst(bb[0], "");
                    // }
                    stack = rx.getString("name");
                }
                /*
                //lets remove the ward / LGA / HF Strings
                switch (rx.getString("level")) {
                    case "1":
                        // code block
                        stack = stack.replaceFirst("ng ", "");
                        break;
                    case "2":
                        // code block
                        stack = stack.replaceFirst(" State", "");
                        break;
                    case "3":
                        // code block
                        stack = stack.replaceFirst(" Local Government Area", "");
                        break;

                    default:
                    // code block
                }
                 */
                try {
                    stack = stack.toLowerCase();
                    xps = conn.prepareStatement("SELECT UUID, namex, level FROM sormas_local where level = ?");
                    xps.setString(1, rx.getString("level"));
                    xrx = xps.executeQuery();
                    int dub = 0;

                    while (xrx.next()) {
                        if (xrx.getString("namex").length() > 2) {
                            xstack = xrx.getString("namex");//.replace(" ward", "");

                            //xstack = Compare(xstack);
                            if (stack.contains(xstack.toLowerCase())) {
                                //   System.out.println("found a match : '" + xrx.getString("namex").toLowerCase() + "' matching dhis : '" + stack + "' with uuid :" + xrx.getString("UUID"));

                                found = 1;
                                dub = dub + 1;
                                dub_string = rx.getString("UUID") + ", " + dub_string;
                                fieldList.add(xrx.getString("UUID"));
                                ps_uuid = xrx.getString("UUID");

                            }

                        }
                    }

                    /**
                     * System.out.println("duble no : "+dub);
                     * System.out.println("found : "+found);
                     * System.out.println("stack : "+stack);
                     * System.out.println("xstack : "+xstack);
                     *
                     */
                    if (dub > 1) {
                        System.out.println("DUPLICATE: times = " + dub + " likely duplicates are : " + dub_string);
                        Localizer("", dub_string, dub_string, "",rx.getString("path_parent"));

                    } else if (found > 1 || found == 1) {
                        //   System.out.println("Found a MATCH for : " + rx.getString("name")+ " at "+ ps_uuid);
                        //   System.out.println(rx.getString("uuid")+ "// "+rx.getString("updated_last")+"  //  "+ ps_uuid);
                        Localizer(rx.getString("uuid"), "", rx.getString("updated_last"), ps_uuid,rx.getString("path_parent"));
                    }
//
                    //taking care of the garbage
                    dub = 0;
                    found = 0;
                    stack = "";
                    dub_string = "";
                    //  System.out.println(lister);
                    fieldList.clear();
                    ps_uuid = "";

                } catch (SQLException ex) {
                    out.print("SQLException: " + ex.getMessage());
                    out.print("SQLState: " + ex.getSQLState());
                    out.print("VendorError: " + ex.getErrorCode());
                }

                //  System.out.println("Total at SORCE 1 : " + rx.getString(1));
                System.out.println("number of round made = " + ro);
            }
            System.out.println(lister);
            conn.close();
        } catch (SQLException ex) {
            out.print("SQLException: " + ex.getMessage());
            out.print("SQLState: " + ex.getSQLState());
            out.print("VendorError: " + ex.getErrorCode());
        }
    }

    public static int Cutter(int t_v) throws ClassNotFoundException, SQLException {

        PreparedStatement ps = null;
        ResultSet rx = null;
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DbConnector.getConnection();

        int how_many = 0;

        try {

            ps = conn.prepareStatement("select count(*) from raw_ where level = " + t_v);
            rx = ps.executeQuery();
            if (rx.next()) {

                int all = Integer.parseInt(rx.getString(1));
                if (all > 499) {
                    how_many = all / 500;
                } else {
                    how_many = 1;
                }
                //   System.out.println(how_many);
                //    System.out.println(rx.getString(1));

            }

        } finally {
            conn.close();
        }
        return how_many + 1;
    }

    public static int Cutter_() throws ClassNotFoundException, SQLException {

        PreparedStatement ps = null;
        ResultSet rx = null;
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DbConnector.getConnection();

        int how_many = 0;

        try {

            ps = conn.prepareStatement("select count(*) from raw_");
            rx = ps.executeQuery();
            if (rx.next()) {

                int all = rx.getInt(1);
                if (all > 499) {
                    how_many = all;
                } else {
                    how_many = 1;
                }
            }

        } finally {
            conn.close();
        }
        return how_many;
    }

    public static void Deduplicate() throws ClassNotFoundException, SQLException {

        PreparedStatement ps = null;
        ResultSet rx = null;

        PreparedStatement ps_ = null;
        ResultSet rx_ = null;

        PreparedStatement ps_x = null;
        ResultSet rx_x = null;
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DbConnector.getConnection();

        try {

            ps = conn.prepareStatement("SELECT duplicate_with, idx FROM sormas_local where duplicate_with is not null and externalid is null;");
            rx = ps.executeQuery();
            while (rx.next()) {

                String s = rx.getString(1);
                s = s + "~";
                s = s.replace(", ~", "");
                //  System.out.println(s);
                String[] words = s.split(",");
                String dup_ = "";
                int dx = words.length;
                // System.out.println("checking if "+words[0]+"equals"+words[dx - 1]);
                if (words[0].equals(words[dx - 1])) {
                } else {

                    ps_x = conn.prepareStatement("select idx from sormas_local where externalid = ?");
                    ps_x.setString(1, words[0]);
                    rx_x = ps_x.executeQuery();
                    if (rx_x.next()) {
                    } else {
                        ps_ = conn.prepareStatement("update sormas_local set externalid = ?, duplicate_with = '' where idx = ? and externalid is null");
                        ps_.setString(1, words[0]);
                        ps_.setString(2, rx.getString(2));
                        System.out.println(ps_.toString());
                        ps_.executeUpdate();
                    }
                }

            }

        } finally {
            conn.close();
        }
    }

    public static void Localizer(String ch, String cm, String cc, String cdt, String prtid) throws ClassNotFoundException, SQLException {
        // System.out.println(ch + "-" + cm + "-" + cc);

        PreparedStatement ps;
        Class.forName("com.mysql.jdbc.Driver");
        Connection conn = DbConnector.getConnection();

        String str = "";

        try {
            if (cm.length() == 0) {
                ps = conn.prepareStatement("update sormas_local set changedate = now(), externalid = ?, ext_cdate = ?, parent_id = ? where uuid = ?");
                ps.setString(1, ch);
                ps.setString(2, cc);
                ps.setString(3, prtid);
                ps.setString(4, cdt);
                ps.execute();
            } else {

                StringBuilder sb = new StringBuilder();

                sb.append("  update sormas_local set changedate = now(), duplicate_with = ? \n");
                sb.append("  WHERE uuid IN (\n");

                for (int i = 0; i < fieldList.size(); i++) {
                    if (i == 0) {
                        sb.append("'").append(fieldList.get(i)).append("'\n");
                    } else {
                        sb.append(",'").append(fieldList.get(i)).append("'\n");
                    }
                }
                sb.append(")\n");

                ps = conn.prepareStatement(sb.toString());
                //  System.out.println(sb.toString());
                ps.setString(1, cm);

                ps.execute();

            }

            //   System.out.println(ps);
        } finally {
            conn.close();
        }

    }
}
