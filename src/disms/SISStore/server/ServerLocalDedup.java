package disms.SISStore.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class ServerLocalDedup{	
  
	 public static String findLocalDedup(String fileInfo,String path){   
	        //要查询的字符串   
	        String str = fileInfo;   
	        //设置读取文件内容时的文件编码   
	        String encoding = "utf8";   
	        //某个文件的内容   
	        String content = "";   
	        //所有文件的地址   
	        Set fileNameSet = SearchStr.fileNameSet(path);   
	        //匹配表示
	        boolean isDup = false;
	        int count = 0;
	           
	        for (Iterator i = new TreeSet(fileNameSet).iterator() ; i.hasNext() ; )      
	        {      
	            String filePath = i.next().toString();   
	            filePath = filePath.replaceAll("\\\\", "\\\\\\\\");   
	            
	            //System.out.println(filePath);   
	           // if(filePath.length() > 5 && ".txt".equals(filePath.substring(filePath.length()-5, filePath.length()))){   
	                try {   
	                    content = SearchStr.readTxt(filePath, encoding);
	                    String[] content2 = content.split("[#]");
	                    for(count = 1;count < content2.length; count++ ){
	                    	if(content2[count].indexOf(str)!=-1){	                    	  
	                        System.out.println(content2[count]);  
	                        isDup = true;
	                        break;
	                    	}	                    	
	                    }
	                    if(isDup){   
	                        System.out.println("ServerLocalDedup-->findLocalDedup:found matching infomation in records"); 
	                        return content2[count];  
	                    }
	                    else{
	                        System.out.println("ServerLocalDedup-->findLocalDedup:not found matching information in records");
	                        return null;  	
	                    }
	                       
	                } catch (IOException e) {   
	                    e.printStackTrace();   
	                }   
	          	}   
	       // }
			return null;
			  
	    }   
	       
	       
	       
	    /**  
	     * 读取文本文件内容  
	     *   
	     * @param filePathAndName  
	     *            带有完整绝对路径的文件名  
	     * @param encoding  
	     *            文本文件打开的编码方式  
	     * @return 返回文本文件的内容  
	     */  
	    public static String readTxt(String filePathAndName, String encoding)   
	            throws IOException {   
	        encoding = encoding.trim();   
	        StringBuffer str = new StringBuffer("");   
	        String st = "";   
	        try {   
	            FileInputStream fs = new FileInputStream(filePathAndName);   
	            InputStreamReader isr;   
	            if (encoding.equals("")) {   
	                isr = new InputStreamReader(fs);   
	            } else {   
	                isr = new InputStreamReader(fs, encoding);   
	            }   
	            BufferedReader br = new BufferedReader(isr);   
	            try {   
	                String data = "";   
	                while ((data = br.readLine()) != null) {   
	                    str.append(data + " ");   
	                }   
	            } catch (Exception e) {   
	                str.append(e.toString());   
	            }   
	            st = str.toString();   
	        } catch (IOException es) {   
	            st = "";   
	        }   
	        return st;   
	    }   
	       
	       
	    /**  
	     * 把一个文件夹及所有子文件夹中包含的文件读取出来  
	     * @param path 绝对地址，例如"c:\\test\\"  
	     * @return  
	     */  
	    public static Set fileNameSet(String path)      
	    {      
	        File directory = new File(path);      
	        List fileList = listAllFiles(directory);      
	     
	        Set fileNameSet = new HashSet(fileList.size());      
	        for (int i = 0 ; i< fileList.size() ; i++)      
	        {      
	            File file = (File)fileList.get(i);      
	            fileNameSet.add(file.getAbsolutePath());      
	        }      
	     
	        return fileNameSet;   
	     
	    }   
	       
	    /**  
	     * fileNameSet 方法的附属方法  
	     * @param directory  
	     * @return  
	     */  
	    private static List listAllFiles(File directory)      
	    {      
	        if (directory == null || !directory.isDirectory()){      
	            return null;      
	        }      
	        List fileList = new ArrayList();      
	        addSubFileList(directory, fileList);      
	     
	        return fileList;      
	    }      
	     
	    /**  
	     * fileNameSet 方法的附属方法  
	     * @param file  
	     * @param fileList  
	     */  
	    private static void addSubFileList(File file,List fileList){      
	        File[] subFileArray = file.listFiles();      
	        if (subFileArray == null     
	            || subFileArray.length == 0     
	        ){      
	            return;      
	        }      
	     
	        for (int i = 0 ; i < subFileArray.length ; i++)      
	        {      
	            File subFile = subFileArray[i];      
	            if (subFile == null     
	            ){      
	                continue;      
	            }      
	            if (subFile.isFile()      
	            ){      
	                fileList.add(subFile);      
	                continue;      
	            }      
	            else if (subFile.isDirectory()      
	            ){      
	                addSubFileList(subFile, fileList);      
	            }      
	        }      
	    }    
}