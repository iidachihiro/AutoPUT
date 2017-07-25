package jp.mzw.tri.util;

import org.eclipse.jdt.core.dom.MethodInvocation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 7/20/17.
 */
public class Utils {

    /**
     * Get files under given directory recursively
     *
     * @param dir
     *            Given Directory
     * @return File found
     */
    public static List<File> getFiles(File dir) {
        ArrayList<File> ret = new ArrayList<File>();
        if (!dir.exists()) {
            return ret;
        }
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                ret.addAll(getFiles(file));
            } else if (file.isFile()) {
                ret.add(file);
            }
        }
        return ret;
    }

    public static boolean compareAssetionMethod(MethodInvocation src, MethodInvocation dst) {

        return false;
    }


}
