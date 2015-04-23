package disms.SISStore.server2;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
/**
* 复制目录下的文件（不包括该目录）到指定目录，会连同子目录一起复制过去。
*
* @param targetFile
* @param path
*/
public static void copyFileFromDir(String targetDir, String path) {
	File file = new File(path);
	createFile(targetDir, false);
	if (file.isDirectory()) {
		copyFileToDir(targetDir, listFile(file));
}
}
/**
* 复制目录下的文件（不包含该目录和子目录，只复制目录下的文件）到指定目录。
*
* @param targetDir
* @param path
*/
public static void copyFileOnly(String targetDir, String path) {
	File file = new File(path);
	File targetFile = new File(targetDir);
	if (file.isDirectory()) {
		File[] files = file.listFiles();
		for (File subFile : files) {
		if (subFile.isFile()) {
			copyFile(targetFile, subFile);
			}
		}
	}
}
/**
* 复制目录到指定目录。targetDir是目标目录，path是源目录。
* 该方法会将path以及path下的文件和子目录全部复制到目标目录
*
* @param targetDir
* @param path
*/
public static void copyDir(String targetDir, String path) {
	File targetFile = new File(targetDir);
	createFile(targetFile, false);
	File file = new File(path);
	if (targetFile.isDirectory() && file.isDirectory()) {
		copyFileToDir(targetFile.getAbsolutePath() + "/" + file.getName(),
		listFile(file));
	}
}
/**
* 复制一组文件到指定目录。targetDir是目标目录，filePath是需要复制的文件路径
*
* @param targetDir
* @param filePath
*/
public static void copyFileToDir(String targetDir, String... filePath) {
	if (targetDir == null || "".equals(targetDir)) {
	System.out.println("参数错误，目标路径不能为空");
	return;
	}
	File targetFile = new File(targetDir);
	if (!targetFile.exists()) {
	targetFile.mkdir();
	} else {
		if (!targetFile.isDirectory()) {
		System.out.println("参数错误，目标路径指向的不是一个目录！");
		return;
		}
	}
	for (String path : filePath) {
		File file = new File(path);
		if (file.isDirectory()) {
		copyFileToDir(targetDir + "/" + file.getName(), listFile(file));
		} else {
		copyFileToDir(targetDir, file, "");
		}
	}
}
/**
* 复制文件到指定目录。targetDir是目标目录，file是源文件名，newName是重命名的名字。
*
* @param targetFile
* @param file
* @param newName
*/
public static void copyFileToDir(String targetDir, File file, String newName) {
	String newFile = "";
	if (newName != null && !"".equals(newName)) {
	newFile = targetDir + "/" + newName;
	} else {
	newFile = targetDir + "/" + file.getName();
	}
	File tFile = new File(newFile);
	copyFile(tFile, file);
}
/**
* 复制文件。targetFile为目标文件，file为源文件
*
* @param targetFile
* @param file
*/
public static void copyFile(File targetFile, File file) {
	if (targetFile.exists()) {
	System.out.println("文件" + targetFile.getAbsolutePath()
	+ "已经存在，跳过该文件！");
	return;
	} else {
	createFile(targetFile, true);
	}
	System.out.println("复制文件" + file.getAbsolutePath() + "到"
	+ targetFile.getAbsolutePath());
	try {
	InputStream is = new FileInputStream(file);
	FileOutputStream fos = new FileOutputStream(targetFile);
	byte[] buffer = new byte[1024];
	while (is.read(buffer) != -1) {
	fos.write(buffer);
	}
	is.close();
	fos.close();
	} catch (FileNotFoundException e) {
	e.printStackTrace();
	} catch (IOException e) {
	e.printStackTrace();
	}
}
public static String[] listFile(File dir) {
	String absolutPath = dir.getAbsolutePath();
	String[] paths = dir.list();
	String[] files = new String[paths.length];
	for (int i = 0; i < paths.length; i++) {
	files[i] = absolutPath + "/" + paths[i];
	}
	return files;
}
public static void createFile(String path, boolean isFile){
	createFile(new File(path), isFile);
}
public static void createFile(File file, boolean isFile) {
	if (!file.exists()) {
			if (!file.getParentFile().exists()) {
			createFile(file.getParentFile(), false);
			} else {
				if (isFile) {
				try {
				file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				} else {
					file.mkdir();
				}
			}
		}
	}
}