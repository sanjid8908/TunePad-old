package com.megh.notepad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
 
        // TBoxUtils.java ফাইলের একদম উপরে এই ইমপোর্টগুলো যোগ করে নিন (যদি না থাকে)
    import java.io.InputStream;
    import java.util.zip.ZipInputStream;
    import javax.crypto.CipherInputStream;


public class TBoxUtils {

    // 🌟 আপনার অ্যাপের নিজস্ব গোপন চাবি (১৬ অক্ষরের হতে হবে) 🌟
    // কেউ এই চাবি না জানলে ফাইল আনলক করতে পারবে না!
    private static final String SECRET_KEY = "TunePadKey123456"; 

    // 🌟 ফোল্ডার প্যাক করে লোহা-কঠিন এনক্রিপ্টেড .tbox বানানোর মেথড 🌟
    public static void zipAndEncryptFolder(File sourceFolder, File destinationTboxFile) throws Exception {
        
        // ১. প্রথমে টেম্পোরারি (অস্থায়ী) সাধারণ জিপ ফাইল বানানো হবে
        File tempZip = new File(destinationTboxFile.getAbsolutePath() + ".tmp");
        FileOutputStream fos = new FileOutputStream(tempZip);
        ZipOutputStream zos = new ZipOutputStream(fos);
        zipFiles(sourceFolder, sourceFolder, zos);
        zos.close();
        fos.close();

        // ২. এবার সেই জিপ ফাইলটাকে AES দিয়ে এনক্রিপ্ট করে আসল .tbox বানানো হবে
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        FileInputStream fis = new FileInputStream(tempZip);
        FileOutputStream finalFos = new FileOutputStream(destinationTboxFile);
        CipherOutputStream cos = new CipherOutputStream(finalFos, cipher);

        byte[] buffer = new byte[8192];
        int read;
        while ((read = fis.read(buffer)) != -1) {
            cos.write(buffer, 0, read);
        }

        // ক্লোজ করা
        cos.close();
        fis.close();
        finalFos.close();

        // ৩. প্রমাণ লোপাট! (টেম্পোরারি ফাইল ডিলিট করে দেওয়া হলো)
        if (tempZip.exists()) {
            tempZip.delete();
        }
    }

    private static void zipFiles(File rootFolder, File currentFolder, ZipOutputStream zos) throws Exception {
        File[] files = currentFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    zipFiles(rootFolder, file, zos); 
                } else {
                    String entryName = rootFolder.toURI().relativize(file.toURI()).getPath();
                    zos.putNextEntry(new ZipEntry(entryName));
                    
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    fis.close();
                }
            }
        }
    }
    
   
    // 🌟 সুপার-সিকিউর .tbox আনলক এবং আনজিপ করার ম্যাজিক মেথড 🌟
    public static void decryptAndUnzipFolder(InputStream encryptedInputStream, File destinationFolder) throws Exception {
        // ১. প্রথমে টেম্পোরারি ফাইলে আনলক (Decrypt) করা
        File tempZip = new File(destinationFolder.getAbsolutePath() + "_temp.zip");
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs();
        }

        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        CipherInputStream cis = new CipherInputStream(encryptedInputStream, cipher);
        FileOutputStream fos = new FileOutputStream(tempZip);

        byte[] buffer = new byte[8192];
        int read;
        while ((read = cis.read(buffer)) != -1) {
            fos.write(buffer, 0, read);
        }
        fos.close();
        cis.close();
        encryptedInputStream.close();

        // ২. এবার আনলক হওয়া জিপ ফাইলটিকে আনজিপ (Extract) করা
        ZipInputStream zis = new ZipInputStream(new FileInputStream(tempZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = new File(destinationFolder, zipEntry.getName());
            if (zipEntry.isDirectory()) {
                newFile.mkdirs();
            } else {
                newFile.getParentFile().mkdirs();
                FileOutputStream fosUnzip = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fosUnzip.write(buffer, 0, len);
                }
                fosUnzip.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();

        // ৩. কাজ শেষে প্রমাণ লোপাট!
        if (tempZip.exists()) {
            tempZip.delete();
        }
    }

}
