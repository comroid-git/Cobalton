package org.comroid.util;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public final class DNSUtil {
    public static String getTxtContent(String domain) {
        // based on https://www.inprose.com/content/how-get-dns-txt-record-java/
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

        try {
            DirContext dirContext = new InitialDirContext(env);
            Attributes attrs = dirContext.getAttributes(domain, new String[]{"TXT"});
            Attribute attr = attrs.get("TXT");

            String txtRecord = "";

            if (attr != null) {
                txtRecord = attr.get().toString();
            }

            return txtRecord;
        } catch (NamingException e) {
            e.printStackTrace();
            return "";
        }
    }
}
