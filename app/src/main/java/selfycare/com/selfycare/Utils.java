package selfycare.com.selfycare;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Some utility methods.
 */
public class Utils {
    public static boolean unpackZip(String zipPath, String outputFolder)
    {
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(zipPath);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();
                Log.d(Utils.class.toString(), "Decompressing file " + filename);
                if (ze.isDirectory()) {
                    File fmd = new File(outputFolder + filename);
                    if(fmd.mkdirs()){
                        Log.d(Utils.class.toString(), "Creating a new folder in " + filename);
                    }
                    continue;
                }

                FileOutputStream fOut = new FileOutputStream(outputFolder + filename);

                while ((count = zis.read(buffer)) != -1)
                {
                    fOut.write(buffer, 0, count);
                }

                fOut.close();
                zis.closeEntry();
            }

            zis.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
