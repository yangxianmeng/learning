package tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Yxm
 */
public class ClassReload {


    private static Instrumentation inst = null;

    public static void premain(String agentArgs, Instrumentation i) {
        inst = i;
        System.out.println("================= 热加载机制初始化 =============");
    }

    public ClassReload() {

    }

    public static void reload(Class<?> clazz, File f) throws IOException, ClassNotFoundException, UnmodifiableClassException {
        byte[] bytes = fileLoadToByte(clazz, f);
        if (bytes == null) {
            throw new IOException("Unknown file :" + clazz.getName());
        }
        ClassDefinition classDefinition = new ClassDefinition(clazz, bytes);
        inst.redefineClasses(classDefinition);

    }

    private static byte[] fileLoadToByte(Class<?> clazz, File f) throws IOException, ClassNotFoundException {
        String name = f.getName();
        if (name.endsWith(".jar")) {
            return fileToByteByJar(clazz, f);
        } else if (name.endsWith(".class")) {
            return fileToByteByClass(clazz, f);
        }
        return null;
    }

    private static byte[] fileToByteByClass(Class<?> clazz, File f) throws FileNotFoundException {
        long length = f.length();
        byte[] buffer = new byte[(int) length];
        readBuffer(new FileInputStream(f), buffer);
        return buffer;
    }

    private static void readBuffer(InputStream fileInputStream, byte[] buffer) {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            bufferedInputStream.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] fileToByteByJar(Class<?> clazz, File f) throws IOException, ClassNotFoundException {
        JarFile file = new JarFile(f);
        String name = clazz.getName();
        name = name.replaceAll("\\.", "/") + ".class";
        JarEntry entry = file.getJarEntry(name);
        if (entry == null) {
            throw new ClassNotFoundException(name);
        }
        byte[] buffer = new byte[(int) entry.getSize()];
        readBuffer(file.getInputStream(entry), buffer);
        return buffer;
    }
}
