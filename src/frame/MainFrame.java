/*
 * MainFrame.java
 *
 * Created on __DATE__, __TIME__
 */

package frame;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.ParserConfigurationException;

import org.opendedup.sdfs.Main;
import org.opendedup.util.OSValidator;
import org.opendedup.util.date.WeekPolicy;
import org.xml.sax.SAXException;

import frame.threads.CreateBackupJobThread;

import xjtu.dedup.DB.MySQLDB;
import xjtu.dedup.backupmngt.BackupApp;
import xjtu.dedup.backupmngt.BackupJob;
import xjtu.dedup.backupmngt.BackupJobRecord;
import xjtu.dedup.backupmngt.BackupServiceProxy;
import xjtu.dedup.mngtobject.SparseVolume;
import xjtu.dedup.multithread.BackupClient;

/**
 *
 * @author  __USER__
 */
public class MainFrame extends javax.swing.JFrame {
	//mysql db info
	Object[][] backupinfo = null;
	Object[][] volumesinfo = null;
	String[][] backupjobinfo = null;
	String host = "localhost:3306";
	String database = "backupmetadata";
	String user = "root";
	String passwd = "123456";
	String url = "jdbc:mysql://" + host + "/" + database;
	//MySQLDB sqldb = MySQLDB.getInstance(host, database, user, passwd);

	//volume
	static String vol_name = null;
	static String vol_cap;
	static String chunk_size;
	static String vol_type;
	static String RL;
	static boolean is_local = true;
	int size_unit_type;//单位代表 0:MB ,1:GB,2:TB.

	static String configfile = null;// backup config file for backup parsing
	BackupApp ba = new BackupApp();//backup object 
	File file = null;

	private ArrayList list = new ArrayList();

	//backup job record
	BackupClient bclient = null; //备份客户端对象，触发备份任务
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	String backupJobname = null;
	String backupSrc = null;
	String backupVolume = null;
	WeekPolicy weekp = new WeekPolicy();//每天的备份类型
	int type = -1;//备份类型：0：全备份 1：增量备份 2：差异备份
	long backupFilesLength = 0l; //备份数据大小
	long backupJobFilesCount = 01;//备份任务所含文件数
	String backupDate;

	//others
	int index = 0;

	/** Creates new form MainFrame */
	public MainFrame() {
		this.setTitle("BackupDedup");

		initComponents(true);
		//		initComponents();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(880, 640);
		this.setLocation(d.width / 2 - 880 / 2, d.height / 2 - 640 / 2);

		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
					.getSystemLookAndFeelClassName());
			javax.swing.SwingUtilities.updateComponentTreeUI(this);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			try {
				javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
						.getCrossPlatformLookAndFeelClassName());
				javax.swing.SwingUtilities.updateComponentTreeUI(this);
			} catch (Exception e1) {
				// TODO: handle exception
				e1.printStackTrace();
			}
		}
	}

	public static String[][] getVolumeInfoFromFile() {
		String[][] volumesinfo = null;
		int i = 0;
		int rownum = 0;
		String volumesfilepath = OSValidator.getBackupConfigPath()
				+ "volumesfile.info";
		File volumesfile = new File(volumesfilepath);
		if (!volumesfile.exists()) {
			volumesfile.getParentFile().mkdirs();
			try {
				volumesfile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		BufferedReader br = null;
		FileReader fr = null;
		StringBuffer sBuffer = new StringBuffer();
		try {
			try {
				fr = new FileReader(volumesfilepath);// 建立FileReader对象，并实例化为fr
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			br = new BufferedReader(fr);// 建立BufferedReader对象，并实例化为br
			String Line = br.readLine();// 从文件读取一行字符串
			// 判断读取到的字符串是否不为空
			while (Line != null) {
				rownum++;
				sBuffer.append(Line);
				sBuffer.append("\n");
				Line = br.readLine();// 从文件中继续读取一行数据
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();// 关闭BufferedReader对象
				if (fr != null)
					fr.close();// 关闭文件
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (rownum != 0) {
			volumesinfo = new String[rownum][5];
			String remote_local = null;
			String strvolume = new String(sBuffer);
			String volumes[] = strvolume.split("\n");
			String volume[] = new String[5];
			for (int j = 0; j < volumes.length; j++) {
				volume = volumes[j].split(" ");
				volumesinfo[j][0] = volume[0];//vol_name
				volumesinfo[j][1] = volume[1];//vol_cap
				volumesinfo[j][2] = volume[2];//chunk_size
				volumesinfo[j][3] = volume[3];//vol_type
				if (volume[4] == "")
					remote_local = null;
				else {
					if (Boolean.parseBoolean(volume[4]) == true)
						remote_local = "local";
					else if (Boolean.parseBoolean(volume[4]) == false)
						remote_local = "remote";
				}
				volumesinfo[j][4] = remote_local;
			}
		} else {
			volumesinfo = null;
		}
		//	System.out.println("vol_name:"+vol_name+" volume_capacity:"+vol_cap+" chunk_size:"+chunk_size+" volume_type:"+vol_type);
		return volumesinfo;
	}

	public void delvolumeconfigfile(String volume_name) {
		File dir = new File(OSValidator.getConfigPath());
		if (!dir.exists()) {
			System.out.println("making" + dir.getAbsolutePath());
			dir.mkdirs();
		}
		File file = new File(OSValidator.getConfigPath() + volume_name
				+ "-volume-cfg.xml");
		if (file.exists()) {
			file.delete();
		}
	}

	public void delVolumeInfoFromFile(int delnum) throws FileNotFoundException {
		int linenum = 0;
		String volumesfilepath = OSValidator.getBackupConfigPath()
				+ "volumesfile.info";
		File volumesfile = new File(volumesfilepath);
		BufferedReader br = null;
		FileReader fr = null;
		StringBuffer sBuffer = new StringBuffer();
		try {
			try {
				fr = new FileReader(volumesfilepath);// 建立FileReader对象，并实例化为fr
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			br = new BufferedReader(fr);// 建立BufferedReader对象，并实例化为br
			String Line = br.readLine();// 从文件读取一行字符串
			// 判断读取到的字符串是否不为空
			while (Line != null) {
				if (linenum != delnum) {
					sBuffer.append(Line);
					sBuffer.append("\r\n");
				}
				Line = br.readLine();// 从文件中继续读取一行数据
				linenum++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();// 关闭BufferedReader对象
				if (fr != null)
					fr.close();// 关闭文件
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(volumesfile, false);
			bw = new BufferedWriter(fw);
			bw.write(sBuffer.toString());
			bw.flush();
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO: handle exception
		}
	}

	public static String[][] getBackupInfoFromXML() {
		configfile = (new BackupJobRecord()).getbackupjobconfigpath();
		String[][] backinfo = null;
		if (configfile == null)
			backinfo = null;
		else
			backinfo = BackupApp.getBackupInfo(configfile);
		return backinfo;
	}

	public void delBackupInfoFromXML(String backupTaskID, String backupSrc) {
		configfile = (new BackupJobRecord()).getbackupjobconfigpath();
		BackupApp.delBackupTaskRecord(configfile, backupTaskID, backupSrc);
	}

	//parse the config file and get the restore information.
	public String[][] getRestoreInfo(String configfile) {
		String filepath = configfile;
		String[][] restoreinfo = null;
		if (filepath == null) {
			filepath = (new BackupJobRecord()).getbackupjobconfigpath();
		}
		if (filepath == null)
			restoreinfo = null;
		else
			restoreinfo = (new BackupApp()).preProcessRestoreJob(filepath);
		return restoreinfo;
	}

	public StringBuffer getLogInfo(String logpath) {
		StringBuffer strbuf = new StringBuffer();
		try {
			File file = new File(logpath);
			//FileInputStream fis=new FileInputStream(file);
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			FileReader fr = new FileReader(file);
			BufferedReader bReader = new BufferedReader(fr);
			String line;
			while ((line = bReader.readLine()) != null) {
				list.add(line);
				strbuf.append(line + "\r\n");
			}
			bReader.close();
		} catch (FileNotFoundException e) {
			// TODO: handle exception
			e.printStackTrace();
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return strbuf;
	}

	public static void initvolumeinfo() {
		String[][] volumesinfo = getVolumeInfoFromFile();
		jTable1.setModel(new javax.swing.table.DefaultTableModel(volumesinfo,
				new String[] { "volume_name", "volume_capacity", "chunk_size",
						"volume_type", "R/L", "current_size" }));
	}

	public static void initbackuptaskinfo() {
		String[][] backupjobinfo = getBackupInfoFromXML();
		jTable2.setModel(new javax.swing.table.DefaultTableModel(backupjobinfo,
				new String[] { "backupName", "userName", "backupSrc",
						"backupVolume", "BackupState" }));
	}

	private void initjPanel8() {
		jPanel8 = new javax.swing.JPanel();
		jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8,
				javax.swing.BoxLayout.PAGE_AXIS));

		jSeparator3.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel8.add(jSeparator3);

		jPanel50.setMaximumSize(new java.awt.Dimension(32767, 50));
		jPanel50.setPreferredSize(new java.awt.Dimension(100, 50));

		jPanel8.add(jPanel50);

		jPanel51.setMaximumSize(new java.awt.Dimension(32767, 50));
		jPanel51.setPreferredSize(new java.awt.Dimension(640, 40));

		jPanel9.setMaximumSize(new java.awt.Dimension(640, 21));
		jPanel9.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9,
				javax.swing.BoxLayout.X_AXIS));

		jLabel8.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel8.setText("\u7528\u6237\u540d");
		jLabel8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jLabel8.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel8.setMinimumSize(new java.awt.Dimension(192, 15));
		jLabel8.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel9.add(jLabel8);

		jTextField1.setFont(new java.awt.Font("宋体", 0, 14));
		jTextField1.setText("jTextField1");
		jTextField1.setMaximumSize(new java.awt.Dimension(400, 21));
		jTextField1.setPreferredSize(new java.awt.Dimension(360, 21));
		jPanel9.add(jTextField1);

		jPanel51.add(jPanel9);

		jPanel8.add(jPanel51);

		jPanel10.setMaximumSize(new java.awt.Dimension(64320, 50));
		jPanel10.setPreferredSize(new java.awt.Dimension(110, 40));

		jPanel52.setMaximumSize(new java.awt.Dimension(640, 21));
		jPanel52.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel52.setLayout(new javax.swing.BoxLayout(jPanel52,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel9.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel9.setText("\u5bc6\u7801");
		jLabel9.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel9.setPreferredSize(new java.awt.Dimension(120, 15));
		jPanel52.add(jLabel9);

		jTextField2.setFont(new java.awt.Font("宋体", 0, 14));
		jTextField2.setText("jTextField2");
		jTextField2.setMaximumSize(new java.awt.Dimension(400, 21));
		jTextField2.setPreferredSize(new java.awt.Dimension(360, 21));
		jPanel52.add(jTextField2);

		jPanel10.add(jPanel52);

		jPanel8.add(jPanel10);

		jPanel11.setMaximumSize(new java.awt.Dimension(2147483647, 50));
		jPanel11.setPreferredSize(new java.awt.Dimension(110, 40));

		jPanel53.setMaximumSize(new java.awt.Dimension(640, 50));
		jPanel53.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel53.setLayout(new javax.swing.BoxLayout(jPanel53,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel10.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel10.setText("\u786e\u8ba4\u5bc6\u7801");
		jLabel10.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel10.setPreferredSize(new java.awt.Dimension(120, 15));
		jPanel53.add(jLabel10);

		jTextField3.setFont(new java.awt.Font("宋体", 0, 14));
		jTextField3.setText("jTextField3");
		jTextField3.setMaximumSize(new java.awt.Dimension(280, 21));
		jTextField3.setPreferredSize(new java.awt.Dimension(360, 21));
		jPanel53.add(jTextField3);

		jPanel11.add(jPanel53);

		jPanel8.add(jPanel11);

		jPanel54.setMaximumSize(new java.awt.Dimension(2147483647, 50));
		jPanel54.setPreferredSize(new java.awt.Dimension(110, 40));

		jPanel55.setMaximumSize(new java.awt.Dimension(640, 50));
		jPanel55.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel55.setLayout(new javax.swing.BoxLayout(jPanel55,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel27.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel27.setText("\u7528\u6237\u7c7b\u578b");
		jLabel27.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel27.setPreferredSize(new java.awt.Dimension(120, 15));
		jPanel55.add(jLabel27);

		jCheckBox9.setText("User");

		jCheckBox8.setText("Admin");

		javax.swing.GroupLayout jPanel69Layout = new javax.swing.GroupLayout(
				jPanel69);
		jPanel69.setLayout(jPanel69Layout);
		jPanel69Layout.setHorizontalGroup(jPanel69Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGroup(
				jPanel69Layout.createSequentialGroup().addContainerGap()
						.addComponent(jCheckBox8).addGap(18, 18, 18)
						.addComponent(jCheckBox9).addContainerGap(231,
								Short.MAX_VALUE)));
		jPanel69Layout
				.setVerticalGroup(jPanel69Layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								jPanel69Layout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												jPanel69Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jCheckBox8)
														.addComponent(
																jCheckBox9))
										.addContainerGap(11, Short.MAX_VALUE)));

		jPanel55.add(jPanel69);

		jPanel54.add(jPanel55);

		jPanel8.add(jPanel54);

		jPanel13.setEnabled(false);
		jPanel13.setMaximumSize(new java.awt.Dimension(4315422, 240));

		jPanel49.setMaximumSize(new java.awt.Dimension(480, 100));
		jPanel49.setPreferredSize(new java.awt.Dimension(240, 40));

		jButton7.setFont(new java.awt.Font("宋体", 0, 14));
		jButton7.setText("\u521b\u5efa");
		jButton7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jButton7.setMaximumSize(new java.awt.Dimension(70, 25));
		jButton7.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton7ActionPerformed(evt);
			}
		});
		jPanel49.add(jButton7);

		jButton8.setFont(new java.awt.Font("宋体", 0, 14));
		jButton8.setText("\u53d6\u6d88");
		jButton8.setMaximumSize(new java.awt.Dimension(70, 25));
		jButton8.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton8ActionPerformed(evt);
			}
		});
		jPanel49.add(jButton8);

		jPanel13.add(jPanel49);

		jPanel8.add(jPanel13);

		jSeparator7.setDoubleBuffered(true);
		jSeparator7.setMaximumSize(new java.awt.Dimension(32767, 200));
		jPanel8.add(jSeparator7);

		javax.swing.SwingUtilities.updateComponentTreeUI(jPanel8);
		jTabbedPane1.addTab("用户管理", jPanel8);
	}

	private void removejPanel14Action() {
		for (ActionListener al : jButton9.getActionListeners()) {
			jButton9.removeActionListener(al);
		}
		for (ActionListener al : jButton10.getActionListeners()) {
			jButton10.removeActionListener(al);
		}
	}

	private void initjPanel14() {
		jPanel14 = new javax.swing.JPanel();
		jPanel14.setLayout(new javax.swing.BoxLayout(jPanel14,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel15.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel15.setLayout(new javax.swing.BoxLayout(jPanel15,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel16.setPreferredSize(new java.awt.Dimension(640, 400));
		jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16,
				javax.swing.BoxLayout.LINE_AXIS));

		jScrollPane3.setPreferredSize(new java.awt.Dimension(640, 3200));

		//String[][] volumesinfo=this.getVolumeInfoFromSQL();
		String[][] volumesinfo = getVolumeInfoFromFile();
		jTable1.setModel(new javax.swing.table.DefaultTableModel(volumesinfo,
				new String[] { "volume_name", "volume_capacity", "chunk_size",
						"volume_type", "R/L", "current_size" }));
		jTable1.setPreferredSize(new java.awt.Dimension(620, 400));
		jScrollPane3.setViewportView(jTable1);

		jPanel16.add(jScrollPane3);

		jPanel15.add(jPanel16);

		jPanel18.setMaximumSize(new java.awt.Dimension(37642, 320));
		jPanel18.setMinimumSize(new java.awt.Dimension(215, 100));

		jButton9.setFont(new java.awt.Font("宋体", 0, 14));
		jButton9.setText("\u521b\u5efa\u5377");
		//创建卷
		jButton9.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton9ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton9);

		jButton10.setFont(new java.awt.Font("宋体", 0, 14));
		jButton10.setText("\u5220\u9664\u5377");
		//删除卷
		jButton10.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton10ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton10);

		jButton11.setFont(new java.awt.Font("宋体", 0, 14));
		jButton11.setText("\u9009\u62e9");
		jButton11.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton11ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton11);

		jPanel15.add(jPanel18);

		jPanel14.add(jPanel15);
		javax.swing.SwingUtilities.updateComponentTreeUI(jPanel14);
		jTabbedPane1.addTab("\u5377\u7ba1\u7406", jPanel14);
	}

	private void removejPanel12Action() {
		for (ActionListener al : jButton26.getActionListeners()) {
			jButton26.removeActionListener(al);
		}
		for (ActionListener al : jButton18.getActionListeners()) {
			jButton18.removeActionListener(al);
		}
		for (ActionListener al : jButton19.getActionListeners()) {
			jButton19.removeActionListener(al);
		}
		for (ActionListener al : jButton21.getActionListeners()) {
			jButton21.removeActionListener(al);
		}
	}

	private void initjPanel12() {
		jPanel12 = new javax.swing.JPanel();
		jPanel12.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel56.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel56.setLayout(new javax.swing.BoxLayout(jPanel56,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel57.setPreferredSize(new java.awt.Dimension(640, 400));
		jPanel57.setLayout(new javax.swing.BoxLayout(jPanel57,
				javax.swing.BoxLayout.LINE_AXIS));

		jScrollPane5.setPreferredSize(new java.awt.Dimension(640, 3200));

		//String[][] backupjobinfo=this.getBackupInfoFromSQL();
		String[][] backupjobinfo = getBackupInfoFromXML();
		jTable2.setModel(new javax.swing.table.DefaultTableModel(backupjobinfo,
				new String[] { "backupName", "userName", "backupSrc",
						"backupVolume", "BackupState" }));
		jTable2.setPreferredSize(new java.awt.Dimension(620, 400));
		jScrollPane5.setViewportView(jTable2);

		jPanel57.add(jScrollPane5);

		jPanel56.add(jPanel57);

		jPanel59.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel59.setPreferredSize(new java.awt.Dimension(231, 35));

		jButton26.setFont(new java.awt.Font("宋体", 0, 14));
		jButton26.setText("\u5907\u4efd\u4f5c\u4e1a\u5b9a\u5236");
		//备份作业定制
		jButton26.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton26ActionPerformed(evt);
			}
		});
		jPanel59.add(jButton26);

		jButton18.setFont(new java.awt.Font("宋体", 0, 14));
		jButton18.setText("\u5220\u9664");
		//删除备份信息
		jButton18.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton18ActionPerformed(evt);
			}
		});
		jPanel59.add(jButton18);

		jButton19.setFont(new java.awt.Font("宋体", 0, 14));
		jButton19.setText("\u53d6\u6d88");
		jPanel59.add(jButton19);

		jButton21.setFont(new java.awt.Font("宋体", 0, 14));
		jButton21.setText("\u5f00\u59cb\u5907\u4efd");
		//开始备份按钮
		jButton21.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton21ActionPerformed(evt);
			}
		});
		jPanel59.add(jButton21);

		jPanel56.add(jPanel59);

		jPanel12.add(jPanel56);

		javax.swing.SwingUtilities.updateComponentTreeUI(jPanel12);
		jTabbedPane1.addTab("\u5907\u4efd\u4fe1\u606f\u67e5\u8be2", jPanel12);

	}

	private void removejPanel60Action() {
		for (ActionListener al : jButton20.getActionListeners()) {
			jButton20.removeActionListener(al);
		}
		for (ActionListener al : jButton22.getActionListeners()) {
			jButton22.removeActionListener(al);
		}
		for (ActionListener al : jButton23.getActionListeners()) {
			jButton23.removeActionListener(al);
		}
	}

	private void initjPanel60() {
		jPanel60 = new javax.swing.JPanel();
		jPanel60.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel60.setLayout(new javax.swing.BoxLayout(jPanel60,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel61.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel61.setLayout(new javax.swing.BoxLayout(jPanel61,
				javax.swing.BoxLayout.PAGE_AXIS));
		jPanel62.setPreferredSize(new java.awt.Dimension(640, 400));
		jPanel62.setLayout(new javax.swing.BoxLayout(jPanel62,
				javax.swing.BoxLayout.LINE_AXIS));

		jScrollPane6.setPreferredSize(new java.awt.Dimension(640, 3200));

		String[][] restoreinfo;
		if (configfile == null)
			configfile = (new BackupJobRecord()).getbackupjobconfigpath();
		restoreinfo = getRestoreInfo(configfile);
		jTable3.setModel(new javax.swing.table.DefaultTableModel(restoreinfo,
				new String[] { "BackupJobID", "BackupHostName", "BackupSrc",
						"BackupSize", "BackupDate", "BackupVolume",
						"BackupFilesCount" }));
		jTable3.setPreferredSize(new java.awt.Dimension(620, 400));
		jScrollPane6.setViewportView(jTable3);

		jPanel62.add(jScrollPane6);

		jPanel61.add(jPanel62);

		jPanel64.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel64.setPreferredSize(new java.awt.Dimension(231, 35));

		jButton20.setFont(new java.awt.Font("宋体", 0, 14));
		jButton20.setText("\u9009\u62e9\u6062\u590d\u4f4d\u7f6e");
		jButton20.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton20ActionPerformed(evt);
			}
		});
		jPanel64.add(jButton20);

		jButton22.setFont(new java.awt.Font("宋体", 0, 14));
		jButton22.setText("\u5f00\u59cb\u6062\u590d");
		//开始恢复
		jButton22.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton22ActionPerformed(evt);
			}
		});
		jPanel64.add(jButton22);

		jButton23.setFont(new java.awt.Font("宋体", 0, 14));
		jButton23.setText("\u53d6\u6d88");
		jPanel64.add(jButton23);

		jPanel61.add(jPanel64);

		jPanel60.add(jPanel61);

		javax.swing.SwingUtilities.updateComponentTreeUI(jPanel60);
		jTabbedPane1.add("恢复管理", jPanel60);
	}

	private void removejPanel63Action() {
		for (ActionListener al : jButton24.getActionListeners()) {
			jButton24.removeActionListener(al);
		}
		for (ActionListener al : jButton25.getActionListeners()) {
			jButton25.removeActionListener(al);
		}
		for (ActionListener al : jButton12.getActionListeners()) {
			jButton12.removeActionListener(al);
		}
	}

	private void initjPanel63() {
		jPanel63.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel63.setLayout(new javax.swing.BoxLayout(jPanel63,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel66.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel66.setLayout(new javax.swing.BoxLayout(jPanel66,
				javax.swing.BoxLayout.PAGE_AXIS));

		jScrollPane7.setPreferredSize(new java.awt.Dimension(640, 330));

		jTextArea3.setColumns(20);
		jTextArea3.setRows(5);
		jTextArea3.setPreferredSize(new java.awt.Dimension(1726, 2147483));
		jTextArea3.append(getLogInfo(Main.logPath).toString());
		jScrollPane7.setViewportView(jTextArea3);

		jPanel66.add(jScrollPane7);

		jPanel68.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel68.setPreferredSize(new java.awt.Dimension(231, 35));

		jButton24.setFont(new java.awt.Font("宋体", 0, 14));
		jButton24.setText("\u5907\u4efd\u4fe1\u606f\u67e5\u8be2");
		jButton24.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton24ActionPerformed(evt);
			}
		});
		jPanel68.add(jButton24);

		jButton25.setFont(new java.awt.Font("宋体", 0, 14));
		jButton25.setText("\u6062\u590d\u4fe1\u606f\u67e5\u8be2");
		jButton25.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton25ActionPerformed(evt);
			}
		});
		jPanel68.add(jButton25);

		jButton12.setFont(new java.awt.Font("宋体", 0, 14));
		jButton12.setText("\u8fd0\u884c\u72b6\u6001\u67e5\u8be2");
		jButton12.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton12ActionPerformed(evt);
			}
		});
		jPanel68.add(jButton12);

		jPanel66.add(jPanel68);

		jPanel63.add(jPanel66);
		javax.swing.SwingUtilities.updateComponentTreeUI(jPanel63);
		jTabbedPane1.add("日志管理", jPanel63);
	}

	private void initComponents(boolean ischanged) {

		jFileChooser1 = new javax.swing.JFileChooser();
		jFileChooser2 = new javax.swing.JFileChooser();
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jButton1 = new javax.swing.JButton();
		jLabel2 = new javax.swing.JLabel();
		jButton2 = new javax.swing.JButton();
		jLabel3 = new javax.swing.JLabel();
		jButton3 = new javax.swing.JButton();
		jLabel4 = new javax.swing.JLabel();
		jButton4 = new javax.swing.JButton();
		jLabel5 = new javax.swing.JLabel();
		jButton5 = new javax.swing.JButton();
		jLabel6 = new javax.swing.JLabel();
		jButton6 = new javax.swing.JButton();
		jSeparator1 = new javax.swing.JSeparator();
		jPanel3 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		jPanel6 = new javax.swing.JPanel();
		jTabbedPane1 = new javax.swing.JTabbedPane();
		jPanel8 = new javax.swing.JPanel();
		jSeparator3 = new javax.swing.JSeparator();
		jPanel50 = new javax.swing.JPanel();
		jPanel51 = new javax.swing.JPanel();
		jPanel9 = new javax.swing.JPanel();
		jLabel8 = new javax.swing.JLabel();
		jTextField1 = new javax.swing.JTextField();
		jPanel10 = new javax.swing.JPanel();
		jPanel52 = new javax.swing.JPanel();
		jLabel9 = new javax.swing.JLabel();
		jTextField2 = new javax.swing.JTextField();
		jPanel11 = new javax.swing.JPanel();
		jPanel53 = new javax.swing.JPanel();
		jLabel10 = new javax.swing.JLabel();
		jTextField3 = new javax.swing.JTextField();
		jPanel54 = new javax.swing.JPanel();
		jPanel55 = new javax.swing.JPanel();
		jLabel27 = new javax.swing.JLabel();
		jPanel69 = new javax.swing.JPanel();
		jCheckBox9 = new javax.swing.JCheckBox();
		jCheckBox8 = new javax.swing.JCheckBox();
		jPanel13 = new javax.swing.JPanel();
		jPanel49 = new javax.swing.JPanel();
		jButton7 = new javax.swing.JButton();
		jButton8 = new javax.swing.JButton();
		jSeparator7 = new javax.swing.JSeparator();
		jPanel14 = new javax.swing.JPanel();
		jPanel15 = new javax.swing.JPanel();
		jPanel16 = new javax.swing.JPanel();
		jScrollPane3 = new javax.swing.JScrollPane();
		jTable1 = new javax.swing.JTable();
		jPanel18 = new javax.swing.JPanel();
		jButton9 = new javax.swing.JButton();
		jButton10 = new javax.swing.JButton();
		jButton11 = new javax.swing.JButton();
		jPanel27 = new javax.swing.JPanel();
		jSeparator8 = new javax.swing.JSeparator();
		jPanel28 = new javax.swing.JPanel();
		jPanel29 = new javax.swing.JPanel();
		jLabel21 = new javax.swing.JLabel();
		jTextField8 = new javax.swing.JTextField();
		jPanel30 = new javax.swing.JPanel();
		jLabel22 = new javax.swing.JLabel();
		jButton14 = new javax.swing.JButton();
		jPanel36 = new javax.swing.JPanel();
		jPanel37 = new javax.swing.JPanel();
		jPanel38 = new javax.swing.JPanel();
		jScrollPane2 = new javax.swing.JScrollPane();
		jTextArea1 = new javax.swing.JTextArea();
		jPanel39 = new javax.swing.JPanel();
		jPanel31 = new javax.swing.JPanel();
		jLabel23 = new javax.swing.JLabel();
		jButton15 = new javax.swing.JButton();
		jTextField9 = new javax.swing.JTextField();
		jPanel32 = new javax.swing.JPanel();
		jLabel24 = new javax.swing.JLabel();
		jPanel40 = new javax.swing.JPanel();
		jCheckBox5 = new javax.swing.JCheckBox();
		jCheckBox6 = new javax.swing.JCheckBox();
		jCheckBox7 = new javax.swing.JCheckBox();
		jPanel33 = new javax.swing.JPanel();
		jLabel25 = new javax.swing.JLabel();
		jPanel41 = new javax.swing.JPanel();
		jRadioButton3 = new javax.swing.JRadioButton();
		jRadioButton4 = new javax.swing.JRadioButton();
		jRadioButton5 = new javax.swing.JRadioButton();
		jRadioButton6 = new javax.swing.JRadioButton();
		jPanel34 = new javax.swing.JPanel();
		jPanel42 = new javax.swing.JPanel();
		jPanel43 = new javax.swing.JPanel();
		jRadioButton7 = new javax.swing.JRadioButton();
		jRadioButton8 = new javax.swing.JRadioButton();
		jRadioButton9 = new javax.swing.JRadioButton();
		jPanel44 = new javax.swing.JPanel();
		jPanel35 = new javax.swing.JPanel();
		jButton16 = new javax.swing.JButton();
		jButton17 = new javax.swing.JButton();
		jSeparator6 = new javax.swing.JSeparator();
		jPanel12 = new javax.swing.JPanel();
		jPanel56 = new javax.swing.JPanel();
		jPanel57 = new javax.swing.JPanel();
		jScrollPane5 = new javax.swing.JScrollPane();
		jTable2 = new javax.swing.JTable();
		jPanel59 = new javax.swing.JPanel();
		jButton26 = new javax.swing.JButton();
		jButton18 = new javax.swing.JButton();
		jButton19 = new javax.swing.JButton();
		jButton21 = new javax.swing.JButton();
		jPanel60 = new javax.swing.JPanel();
		jPanel61 = new javax.swing.JPanel();
		jPanel62 = new javax.swing.JPanel();
		jScrollPane6 = new javax.swing.JScrollPane();
		jTable3 = new javax.swing.JTable();
		jPanel64 = new javax.swing.JPanel();
		jButton20 = new javax.swing.JButton();
		jButton22 = new javax.swing.JButton();
		jButton23 = new javax.swing.JButton();
		jPanel63 = new javax.swing.JPanel();
		jPanel66 = new javax.swing.JPanel();
		jScrollPane7 = new javax.swing.JScrollPane();
		jTextArea3 = new javax.swing.JTextArea();
		jPanel68 = new javax.swing.JPanel();
		jButton12 = new javax.swing.JButton();
		jButton24 = new javax.swing.JButton();
		jButton25 = new javax.swing.JButton();
		jSeparator2 = new javax.swing.JSeparator();
		jPanel7 = new javax.swing.JPanel();
		jMenuBar1 = new javax.swing.JMenuBar();
		jMenu1 = new javax.swing.JMenu();
		jMenu2 = new javax.swing.JMenu();
		jMenuItem3 = new javax.swing.JMenuItem();
		jMenu3 = new javax.swing.JMenu();
		jMenu5 = new javax.swing.JMenu();
		jMenuItem1 = new javax.swing.JMenuItem();
		jMenuItem2 = new javax.swing.JMenuItem();
		jLabel7 = new javax.swing.JLabel();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		getContentPane().setLayout(
				new javax.swing.BoxLayout(getContentPane(),
						javax.swing.BoxLayout.LINE_AXIS));

		jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel2.setMaximumSize(new java.awt.Dimension(37624, 36));
		jPanel2.setPreferredSize(new java.awt.Dimension(100, 60));
		jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel1.setIcon(new javax.swing.ImageIcon(
				"D:\\MyEclipseWS\\BackupDedup\\icons\\binary.png")); // NOI18N
		jLabel1.setMaximumSize(new java.awt.Dimension(120, 21));
		jLabel1.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel2.add(jLabel1);

		jButton1.setFont(new java.awt.Font("宋体", 0, 14));
		jButton1.setText("\u7528\u6237\u7ba1\u7406");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton1);

		jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel2.setIcon(new javax.swing.ImageIcon(
				"D:\\MyEclipseWS\\BackupDedup\\icons\\database.png")); // NOI18N
		jLabel2.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel2.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel2.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel2);

		jButton2.setFont(new java.awt.Font("宋体", 0, 14));
		jButton2.setText("\u5377\u7ba1\u7406");
		jButton2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton2);

		jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel3.setIcon(new javax.swing.ImageIcon(
				"D:\\MyEclipseWS\\BackupDedup\\icons\\db_add.png")); // NOI18N
		jLabel3.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel3.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel3.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel3);

		jButton3.setFont(new java.awt.Font("宋体", 0, 14));
		jButton3.setText("\u5907\u4efd");
		jButton3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton3ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton3);

		jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel4.setIcon(new javax.swing.ImageIcon(
				"D:\\MyEclipseWS\\BackupDedup\\icons\\desktop.png")); // NOI18N
		jLabel4.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel4.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel4.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel4);

		jButton4.setFont(new java.awt.Font("宋体", 0, 14));
		jButton4.setText("\u6062\u590d");
		jButton4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton4ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton4);

		jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel5.setIcon(new javax.swing.ImageIcon(
				"D:\\MyEclipseWS\\BackupDedup\\icons\\log.png")); // NOI18N
		jLabel5.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel5.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel5.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel5);

		jButton5.setFont(new java.awt.Font("宋体", 0, 14));
		jButton5.setText("\u65e5\u5fd7\u7ba1\u7406");
		jButton5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton5ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton5);

		jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel6.setIcon(new javax.swing.ImageIcon(
				"D:\\MyEclipseWS\\BackupDedup\\icons\\big\\configure.png")); // NOI18N
		jLabel6.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel6.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel6.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel6);

		jButton6.setFont(new java.awt.Font("宋体", 0, 14));
		jButton6.setText("\u7f51\u7edc\u7ba1\u7406");
		jButton6.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton6ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton6);

		jPanel1.add(jPanel2);

		jSeparator1.setMaximumSize(new java.awt.Dimension(32767, 640));
		jPanel1.add(jSeparator1);

		jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel6.setPreferredSize(new java.awt.Dimension(100, 480));
		jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6,
				javax.swing.BoxLayout.LINE_AXIS));

		jTabbedPane1.setFont(new java.awt.Font("宋体", 0, 14));
		jTabbedPane1.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jTabbedPane1MousePressed(evt);
			}
		});
		jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				jTabbedPane1StateChanged(evt);
			}
		});

		jPanel6.add(jTabbedPane1);

		jPanel5.add(jPanel6);

		jSeparator2.setMaximumSize(new java.awt.Dimension(32767, 640));
		jPanel5.add(jSeparator2);

		jPanel7.setMaximumSize(new java.awt.Dimension(2147483647, 20));
		jPanel7.setMinimumSize(new java.awt.Dimension(558, 15));
		jPanel7.setPreferredSize(new java.awt.Dimension(600, 20));
		jPanel7.setLayout(new java.awt.BorderLayout());

		jLabel7.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel7
				.setText("BackupDedup\u662f\u4e00\u6b3e\u57fa\u4e8e\u53bb\u91cd\u7684\u5907\u4efd\u6062\u590d\u8f6f\u4ef6.");
		jPanel7.add(jLabel7, java.awt.BorderLayout.CENTER);

		jPanel5.add(jPanel7);

		jPanel3.add(jPanel5);

		jPanel1.add(jPanel3);

		getContentPane().add(jPanel1);

		jMenu1.setText("File");
		jMenuBar1.add(jMenu1);

		jMenu2.setText("Tools");

		jMenuItem3.setText("Item");
		jMenu2.add(jMenuItem3);

		jMenuBar1.add(jMenu2);

		jMenu3.setText("Look&Feel");
		jMenuBar1.add(jMenu3);

		jMenu5.setText("Others");

		jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_H, 0));
		jMenuItem1.setText("Help");
		jMenu5.add(jMenuItem1);

		jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_A,
				java.awt.event.InputEvent.CTRL_MASK));
		jMenuItem2.setText("About");
		jMenu5.add(jMenuItem2);

		jMenuBar1.add(jMenu5);

		setJMenuBar(jMenuBar1);

		pack();
	}

	//GEN-BEGIN:initComponents
	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents() {

		jFileChooser1 = new javax.swing.JFileChooser();
		jFileChooser2 = new javax.swing.JFileChooser();
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jButton1 = new javax.swing.JButton();
		jLabel2 = new javax.swing.JLabel();
		jButton2 = new javax.swing.JButton();
		jLabel3 = new javax.swing.JLabel();
		jButton3 = new javax.swing.JButton();
		jLabel4 = new javax.swing.JLabel();
		jButton4 = new javax.swing.JButton();
		jLabel5 = new javax.swing.JLabel();
		jButton5 = new javax.swing.JButton();
		jLabel6 = new javax.swing.JLabel();
		jButton6 = new javax.swing.JButton();
		jSeparator1 = new javax.swing.JSeparator();
		jPanel3 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		jPanel6 = new javax.swing.JPanel();
		jTabbedPane1 = new javax.swing.JTabbedPane();
		jPanel8 = new javax.swing.JPanel();
		jPanel50 = new javax.swing.JPanel();
		jSeparator3 = new javax.swing.JSeparator();
		jPanel51 = new javax.swing.JPanel();
		jPanel9 = new javax.swing.JPanel();
		jLabel8 = new javax.swing.JLabel();
		jTextField1 = new javax.swing.JTextField();
		jPanel10 = new javax.swing.JPanel();
		jPanel52 = new javax.swing.JPanel();
		jLabel9 = new javax.swing.JLabel();
		jTextField2 = new javax.swing.JTextField();
		jPanel11 = new javax.swing.JPanel();
		jPanel53 = new javax.swing.JPanel();
		jLabel10 = new javax.swing.JLabel();
		jTextField3 = new javax.swing.JTextField();
		jPanel54 = new javax.swing.JPanel();
		jPanel55 = new javax.swing.JPanel();
		jLabel27 = new javax.swing.JLabel();
		jPanel69 = new javax.swing.JPanel();
		jCheckBox9 = new javax.swing.JCheckBox();
		jCheckBox8 = new javax.swing.JCheckBox();
		jPanel13 = new javax.swing.JPanel();
		jPanel49 = new javax.swing.JPanel();
		jButton7 = new javax.swing.JButton();
		jButton8 = new javax.swing.JButton();
		jSeparator7 = new javax.swing.JSeparator();
		jPanel14 = new javax.swing.JPanel();
		jPanel15 = new javax.swing.JPanel();
		jPanel16 = new javax.swing.JPanel();
		jScrollPane3 = new javax.swing.JScrollPane();
		jTable1 = new javax.swing.JTable();
		jPanel18 = new javax.swing.JPanel();
		jButton9 = new javax.swing.JButton();
		jButton10 = new javax.swing.JButton();
		jButton11 = new javax.swing.JButton();
		jPanel27 = new javax.swing.JPanel();
		jSeparator8 = new javax.swing.JSeparator();
		jPanel28 = new javax.swing.JPanel();
		jPanel29 = new javax.swing.JPanel();
		jLabel21 = new javax.swing.JLabel();
		jTextField8 = new javax.swing.JTextField();
		jPanel30 = new javax.swing.JPanel();
		jLabel22 = new javax.swing.JLabel();
		jButton14 = new javax.swing.JButton();
		jPanel36 = new javax.swing.JPanel();
		jPanel37 = new javax.swing.JPanel();
		jPanel38 = new javax.swing.JPanel();
		jScrollPane2 = new javax.swing.JScrollPane();
		jTextArea1 = new javax.swing.JTextArea();
		jPanel39 = new javax.swing.JPanel();
		jPanel31 = new javax.swing.JPanel();
		jLabel23 = new javax.swing.JLabel();
		jButton15 = new javax.swing.JButton();
		jTextField9 = new javax.swing.JTextField();
		jPanel32 = new javax.swing.JPanel();
		jLabel24 = new javax.swing.JLabel();
		jPanel40 = new javax.swing.JPanel();
		jCheckBox5 = new javax.swing.JCheckBox();
		jCheckBox6 = new javax.swing.JCheckBox();
		jCheckBox7 = new javax.swing.JCheckBox();
		jPanel33 = new javax.swing.JPanel();
		jLabel25 = new javax.swing.JLabel();
		jPanel41 = new javax.swing.JPanel();
		jRadioButton3 = new javax.swing.JRadioButton();
		jRadioButton4 = new javax.swing.JRadioButton();
		jRadioButton5 = new javax.swing.JRadioButton();
		jRadioButton6 = new javax.swing.JRadioButton();
		jPanel34 = new javax.swing.JPanel();
		jPanel42 = new javax.swing.JPanel();
		jPanel43 = new javax.swing.JPanel();
		jRadioButton7 = new javax.swing.JRadioButton();
		jRadioButton8 = new javax.swing.JRadioButton();
		jRadioButton9 = new javax.swing.JRadioButton();
		jPanel44 = new javax.swing.JPanel();
		jPanel35 = new javax.swing.JPanel();
		jButton16 = new javax.swing.JButton();
		jButton17 = new javax.swing.JButton();
		jSeparator6 = new javax.swing.JSeparator();
		jPanel12 = new javax.swing.JPanel();
		jPanel56 = new javax.swing.JPanel();
		jPanel57 = new javax.swing.JPanel();
		jScrollPane5 = new javax.swing.JScrollPane();
		jTable2 = new javax.swing.JTable();
		jPanel59 = new javax.swing.JPanel();
		jButton26 = new javax.swing.JButton();
		jButton18 = new javax.swing.JButton();
		jButton19 = new javax.swing.JButton();
		jButton21 = new javax.swing.JButton();
		jPanel60 = new javax.swing.JPanel();
		jPanel61 = new javax.swing.JPanel();
		jPanel62 = new javax.swing.JPanel();
		jScrollPane6 = new javax.swing.JScrollPane();
		jTable3 = new javax.swing.JTable();
		jPanel64 = new javax.swing.JPanel();
		jButton20 = new javax.swing.JButton();
		jButton22 = new javax.swing.JButton();
		jButton23 = new javax.swing.JButton();
		jPanel63 = new javax.swing.JPanel();
		jPanel66 = new javax.swing.JPanel();
		jScrollPane7 = new javax.swing.JScrollPane();
		jTextArea3 = new javax.swing.JTextArea();
		jPanel68 = new javax.swing.JPanel();
		jButton24 = new javax.swing.JButton();
		jButton25 = new javax.swing.JButton();
		jButton12 = new javax.swing.JButton();
		jSeparator2 = new javax.swing.JSeparator();
		jPanel7 = new javax.swing.JPanel();
		jLabel7 = new javax.swing.JLabel();
		jMenuBar1 = new javax.swing.JMenuBar();
		jMenu1 = new javax.swing.JMenu();
		jMenu2 = new javax.swing.JMenu();
		jMenuItem3 = new javax.swing.JMenuItem();
		jMenu3 = new javax.swing.JMenu();
		jMenu5 = new javax.swing.JMenu();
		jMenuItem1 = new javax.swing.JMenuItem();
		jMenuItem2 = new javax.swing.JMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		getContentPane().setLayout(
				new javax.swing.BoxLayout(getContentPane(),
						javax.swing.BoxLayout.LINE_AXIS));

		jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel2.setMaximumSize(new java.awt.Dimension(37624, 36));
		jPanel2.setPreferredSize(new java.awt.Dimension(100, 60));
		jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel1.setMaximumSize(new java.awt.Dimension(60, 21));
		jLabel1.setPreferredSize(new java.awt.Dimension(60, 21));
		jPanel2.add(jLabel1);

		jButton1.setFont(new java.awt.Font("宋体", 0, 14));
		jButton1.setText("\u7528\u6237\u7ba1\u7406");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton1);

		jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel2.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel2.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel2.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel2);

		jButton2.setFont(new java.awt.Font("宋体", 0, 14));
		jButton2.setText("\u5377\u7ba1\u7406");
		jButton2.setMaximumSize(new java.awt.Dimension(89, 25));
		jButton2.setMinimumSize(new java.awt.Dimension(89, 25));
		jButton2.setPreferredSize(new java.awt.Dimension(89, 25));
		jButton2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton2);

		jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel3.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel3.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel3.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel3);

		jButton3.setFont(new java.awt.Font("宋体", 0, 14));
		jButton3.setText("\u5907\u4efd");
		jButton3.setMaximumSize(new java.awt.Dimension(89, 25));
		jButton3.setMinimumSize(new java.awt.Dimension(89, 25));
		jButton3.setPreferredSize(new java.awt.Dimension(89, 25));
		jButton3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton3ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton3);

		jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel4.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel4.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel4.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel4);

		jButton4.setFont(new java.awt.Font("宋体", 0, 14));
		jButton4.setText("\u6062\u590d\u4e0e\u5220\u9664");
		jButton4.setMaximumSize(new java.awt.Dimension(89, 25));
		jButton4.setMinimumSize(new java.awt.Dimension(89, 25));
		jButton4.setPreferredSize(new java.awt.Dimension(89, 25));
		jButton4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton4ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton4);

		jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel5.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel5.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel5.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel5);

		jButton5.setFont(new java.awt.Font("宋体", 0, 14));
		jButton5.setText("\u65e5\u5fd7\u7ba1\u7406");
		jButton5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton5ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton5);

		jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		jLabel6.setMaximumSize(new java.awt.Dimension(40, 21));
		jLabel6.setMinimumSize(new java.awt.Dimension(0, 21));
		jLabel6.setPreferredSize(new java.awt.Dimension(30, 21));
		jPanel2.add(jLabel6);

		jButton6.setFont(new java.awt.Font("宋体", 0, 14));
		jButton6.setText("\u7f51\u7edc\u7ba1\u7406");
		jButton6.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton6ActionPerformed(evt);
			}
		});
		jPanel2.add(jButton6);

		jPanel1.add(jPanel2);

		jSeparator1.setMaximumSize(new java.awt.Dimension(32767, 640));
		jPanel1.add(jSeparator1);

		jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel6.setPreferredSize(new java.awt.Dimension(100, 480));
		jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6,
				javax.swing.BoxLayout.LINE_AXIS));

		jTabbedPane1.setFont(new java.awt.Font("宋体", 0, 14));
		jTabbedPane1.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mousePressed(java.awt.event.MouseEvent evt) {
				jTabbedPane1MousePressed(evt);
			}
		});
		jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				jTabbedPane1StateChanged(evt);
			}
		});

		jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel50.setMaximumSize(new java.awt.Dimension(32767, 50));
		jPanel50.setPreferredSize(new java.awt.Dimension(100, 50));

		jSeparator3.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel50.add(jSeparator3);

		jPanel8.add(jPanel50);

		jPanel51.setMaximumSize(new java.awt.Dimension(32767, 50));
		jPanel51.setPreferredSize(new java.awt.Dimension(640, 40));

		jPanel9.setMaximumSize(new java.awt.Dimension(640, 21));
		jPanel9.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9,
				javax.swing.BoxLayout.X_AXIS));

		jLabel8.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel8.setText("\u7528\u6237\u540d");
		jLabel8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jLabel8.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel8.setMinimumSize(new java.awt.Dimension(192, 15));
		jLabel8.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel9.add(jLabel8);

		jTextField1.setFont(new java.awt.Font("宋体", 0, 14));
		jTextField1.setText("jTextField1");
		jTextField1.setMaximumSize(new java.awt.Dimension(400, 21));
		jTextField1.setPreferredSize(new java.awt.Dimension(360, 21));
		jTextField1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField1ActionPerformed(evt);
			}
		});
		jPanel9.add(jTextField1);

		jPanel51.add(jPanel9);

		jPanel8.add(jPanel51);

		jPanel10.setMaximumSize(new java.awt.Dimension(64320, 50));
		jPanel10.setPreferredSize(new java.awt.Dimension(110, 40));

		jPanel52.setMaximumSize(new java.awt.Dimension(640, 21));
		jPanel52.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel52.setLayout(new javax.swing.BoxLayout(jPanel52,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel9.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel9.setText("\u5bc6\u7801");
		jLabel9.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel9.setPreferredSize(new java.awt.Dimension(120, 15));
		jPanel52.add(jLabel9);

		jTextField2.setFont(new java.awt.Font("宋体", 0, 14));
		jTextField2.setText("jTextField2");
		jTextField2.setMaximumSize(new java.awt.Dimension(400, 21));
		jTextField2.setPreferredSize(new java.awt.Dimension(360, 21));
		jPanel52.add(jTextField2);

		jPanel10.add(jPanel52);

		jPanel8.add(jPanel10);

		jPanel11.setMaximumSize(new java.awt.Dimension(2147483647, 50));
		jPanel11.setPreferredSize(new java.awt.Dimension(110, 40));

		jPanel53.setMaximumSize(new java.awt.Dimension(640, 50));
		jPanel53.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel53.setLayout(new javax.swing.BoxLayout(jPanel53,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel10.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel10.setText("\u786e\u8ba4\u5bc6\u7801");
		jLabel10.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel10.setPreferredSize(new java.awt.Dimension(120, 15));
		jPanel53.add(jLabel10);

		jTextField3.setFont(new java.awt.Font("宋体", 0, 14));
		jTextField3.setText("jTextField3");
		jTextField3.setMaximumSize(new java.awt.Dimension(280, 21));
		jTextField3.setPreferredSize(new java.awt.Dimension(360, 21));
		jPanel53.add(jTextField3);

		jPanel11.add(jPanel53);

		jPanel8.add(jPanel11);

		jPanel54.setMaximumSize(new java.awt.Dimension(2147483647, 50));
		jPanel54.setPreferredSize(new java.awt.Dimension(110, 40));

		jPanel55.setMaximumSize(new java.awt.Dimension(640, 50));
		jPanel55.setPreferredSize(new java.awt.Dimension(480, 40));
		jPanel55.setLayout(new javax.swing.BoxLayout(jPanel55,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel27.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel27.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel27.setText("\u7528\u6237\u7c7b\u578b");
		jLabel27.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel27.setPreferredSize(new java.awt.Dimension(120, 15));
		jPanel55.add(jLabel27);

		jCheckBox9.setText("User");

		jCheckBox8.setText("Admin");

		javax.swing.GroupLayout jPanel69Layout = new javax.swing.GroupLayout(
				jPanel69);
		jPanel69.setLayout(jPanel69Layout);
		jPanel69Layout.setHorizontalGroup(jPanel69Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGroup(
				jPanel69Layout.createSequentialGroup().addComponent(jCheckBox8)
						.addGap(24, 24, 24).addComponent(jCheckBox9)
						.addContainerGap(233, Short.MAX_VALUE)));
		jPanel69Layout
				.setVerticalGroup(jPanel69Layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								jPanel69Layout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												jPanel69Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jCheckBox9)
														.addComponent(
																jCheckBox8))
										.addContainerGap(11, Short.MAX_VALUE)));

		jPanel55.add(jPanel69);

		jPanel54.add(jPanel55);

		jPanel8.add(jPanel54);

		jPanel13.setEnabled(false);
		jPanel13.setMaximumSize(new java.awt.Dimension(4315422, 240));

		jPanel49.setMaximumSize(new java.awt.Dimension(480, 100));
		jPanel49.setPreferredSize(new java.awt.Dimension(240, 40));

		jButton7.setFont(new java.awt.Font("宋体", 0, 14));
		jButton7.setText("\u521b\u5efa");
		jButton7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
		jButton7.setMaximumSize(new java.awt.Dimension(70, 25));
		jButton7.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton7ActionPerformed(evt);
			}
		});
		jPanel49.add(jButton7);

		jButton8.setFont(new java.awt.Font("宋体", 0, 14));
		jButton8.setText("\u53d6\u6d88");
		jButton8.setMaximumSize(new java.awt.Dimension(70, 25));
		jButton8.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton8ActionPerformed(evt);
			}
		});
		jPanel49.add(jButton8);

		jPanel13.add(jPanel49);

		jPanel8.add(jPanel13);

		jSeparator7.setDoubleBuffered(true);
		jSeparator7.setMaximumSize(new java.awt.Dimension(32767, 200));
		jPanel8.add(jSeparator7);

		jTabbedPane1.addTab("\u7528\u6237\u7ba1\u7406", jPanel8);

		jPanel14.setLayout(new javax.swing.BoxLayout(jPanel14,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel15.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel15.setLayout(new javax.swing.BoxLayout(jPanel15,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel16.setPreferredSize(new java.awt.Dimension(640, 400));
		jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16,
				javax.swing.BoxLayout.LINE_AXIS));

		jScrollPane3.setPreferredSize(new java.awt.Dimension(640, 3200));

		//String[][] volumesinfo=this.getVolumeInfoFromSQL();
		String[][] volumesinfo = this.getVolumeInfoFromFile();
		jTable1.setModel(new javax.swing.table.DefaultTableModel(volumesinfo,
				new String[] { "volume_name", "volume_capacity", "chunk_size",
						"volume_type", "R/L", "current_size" }));
		jTable1.setPreferredSize(new java.awt.Dimension(620, 400));
		jScrollPane3.setViewportView(jTable1);

		jPanel16.add(jScrollPane3);

		jPanel15.add(jPanel16);

		jPanel18.setMaximumSize(new java.awt.Dimension(37642, 320));
		jPanel18.setMinimumSize(new java.awt.Dimension(215, 100));

		jButton9.setFont(new java.awt.Font("宋体", 0, 14));
		jButton9.setText("\u521b\u5efa\u5377");
		jButton9.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton9ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton9);

		jButton10.setFont(new java.awt.Font("宋体", 0, 14));
		jButton10.setText("\u5220\u9664\u5377");
		jButton10.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton10ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton10);

		jButton11.setFont(new java.awt.Font("宋体", 0, 14));
		jButton11.setText("\u9009\u62e9");
		jButton11.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton11ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton11);

		jPanel15.add(jPanel18);

		jPanel14.add(jPanel15);

		jTabbedPane1.addTab("\u5377\u7ba1\u7406", jPanel14);

		jPanel27.setLayout(new javax.swing.BoxLayout(jPanel27,
				javax.swing.BoxLayout.PAGE_AXIS));
		jPanel27.add(jSeparator8);

		javax.swing.GroupLayout jPanel28Layout = new javax.swing.GroupLayout(
				jPanel28);
		jPanel28.setLayout(jPanel28Layout);
		jPanel28Layout.setHorizontalGroup(jPanel28Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 823,
				Short.MAX_VALUE));
		jPanel28Layout.setVerticalGroup(jPanel28Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 99,
				Short.MAX_VALUE));

		jPanel27.add(jPanel28);

		jPanel29.setMaximumSize(new java.awt.Dimension(32767, 200));
		jPanel29.setPreferredSize(new java.awt.Dimension(240, 40));
		jPanel29.setLayout(new javax.swing.BoxLayout(jPanel29,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel21.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel21.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel21.setText("\u5907\u4efd\u4efb\u52a1\u540d");
		jLabel21.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel21.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel29.add(jLabel21);

		jTextField8.setMaximumSize(new java.awt.Dimension(480, 21));
		jTextField8.setPreferredSize(new java.awt.Dimension(120, 21));
		jTextField8.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jTextField8ActionPerformed(evt);
			}
		});
		jPanel29.add(jTextField8);

		jPanel27.add(jPanel29);

		jPanel30.setMaximumSize(new java.awt.Dimension(32767, 480));
		jPanel30.setPreferredSize(new java.awt.Dimension(181, 40));
		jPanel30.setLayout(new javax.swing.BoxLayout(jPanel30,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel22.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel22.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel22.setText("\u5907\u4efd\u6e90\u6587\u4ef6");
		jLabel22.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel22.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel30.add(jLabel22);

		jButton14.setFont(new java.awt.Font("宋体", 0, 14));
		jButton14.setText("\u9009\u62e9");
		jButton14.setMaximumSize(new java.awt.Dimension(80, 25));
		jButton14.setMinimumSize(new java.awt.Dimension(40, 25));
		jButton14.setPreferredSize(new java.awt.Dimension(0, 25));
		jButton14.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton14ActionPerformed(evt);
			}
		});
		jPanel30.add(jButton14);

		jPanel27.add(jPanel30);

		jPanel36.setLayout(new javax.swing.BoxLayout(jPanel36,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel37.setMaximumSize(new java.awt.Dimension(240, 240));
		jPanel37.setPreferredSize(new java.awt.Dimension(240, 100));

		javax.swing.GroupLayout jPanel37Layout = new javax.swing.GroupLayout(
				jPanel37);
		jPanel37.setLayout(jPanel37Layout);
		jPanel37Layout.setHorizontalGroup(jPanel37Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 240,
				Short.MAX_VALUE));
		jPanel37Layout.setVerticalGroup(jPanel37Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 108,
				Short.MAX_VALUE));

		jPanel36.add(jPanel37);

		jPanel38.setMaximumSize(new java.awt.Dimension(480, 240));
		jPanel38.setLayout(new javax.swing.BoxLayout(jPanel38,
				javax.swing.BoxLayout.LINE_AXIS));

		jTextArea1.setColumns(20);
		jTextArea1.setRows(5);
		jTextArea1.setMargin(new java.awt.Insets(2, 240, 2, 2));
		jTextArea1.setMaximumSize(new java.awt.Dimension(1000, 240));
		jTextArea1.setPreferredSize(new java.awt.Dimension(0, 200));
		jScrollPane2.setViewportView(jTextArea1);

		jPanel38.add(jScrollPane2);

		jPanel36.add(jPanel38);

		jPanel39.setMaximumSize(new java.awt.Dimension(750, 480));
		jPanel39.setPreferredSize(new java.awt.Dimension(80, 100));

		javax.swing.GroupLayout jPanel39Layout = new javax.swing.GroupLayout(
				jPanel39);
		jPanel39.setLayout(jPanel39Layout);
		jPanel39Layout.setHorizontalGroup(jPanel39Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 170,
				Short.MAX_VALUE));
		jPanel39Layout.setVerticalGroup(jPanel39Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 108,
				Short.MAX_VALUE));

		jPanel36.add(jPanel39);

		jPanel27.add(jPanel36);

		jPanel31.setMaximumSize(new java.awt.Dimension(32767, 50));
		jPanel31.setPreferredSize(new java.awt.Dimension(0, 50));
		jPanel31.setLayout(new javax.swing.BoxLayout(jPanel31,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel23.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel23.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel23.setText("\u5907\u4efd\u76ee\u7684\u5377");
		jLabel23.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel23.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel31.add(jLabel23);

		jButton15.setFont(new java.awt.Font("宋体", 0, 14));
		jButton15.setText("\u9009\u62e9");
		jButton15.setMaximumSize(new java.awt.Dimension(80, 25));
		jButton15.setPreferredSize(new java.awt.Dimension(70, 25));
		jButton15.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton15ActionPerformed(evt);
			}
		});
		jPanel31.add(jButton15);

		jTextField9.setText("jTextField9");
		jTextField9.setMaximumSize(new java.awt.Dimension(400, 21));
		jPanel31.add(jTextField9);

		jPanel27.add(jPanel31);

		jPanel32.setMaximumSize(new java.awt.Dimension(33007, 50));
		jPanel32.setPreferredSize(new java.awt.Dimension(156, 50));
		jPanel32.setLayout(new javax.swing.BoxLayout(jPanel32,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel24.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel24.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel24.setText("\u5907\u4efd\u7c7b\u578b");
		jLabel24.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel24.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel32.add(jLabel24);

		jPanel40.setMaximumSize(new java.awt.Dimension(480, 40));
		jPanel40.setPreferredSize(new java.awt.Dimension(120, 100));
		jPanel40.setLayout(new javax.swing.BoxLayout(jPanel40,
				javax.swing.BoxLayout.LINE_AXIS));

		jCheckBox5.setFont(new java.awt.Font("宋体", 0, 14));
		jCheckBox5.setText("\u5168\u5907\u4efd");
		jCheckBox5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jCheckBox5ActionPerformed(evt);
			}
		});
		jPanel40.add(jCheckBox5);

		jCheckBox6.setFont(new java.awt.Font("宋体", 0, 14));
		jCheckBox6.setText("\u589e\u91cf\u5907\u4efd");
		jCheckBox6.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jCheckBox6ActionPerformed(evt);
			}
		});
		jPanel40.add(jCheckBox6);

		jCheckBox7.setFont(new java.awt.Font("宋体", 0, 14));
		jCheckBox7.setText("\u5dee\u5f02\u5907\u4efd");
		jCheckBox7.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jCheckBox7ActionPerformed(evt);
			}
		});
		jPanel40.add(jCheckBox7);

		jPanel32.add(jPanel40);

		jPanel27.add(jPanel32);

		jPanel33.setMaximumSize(new java.awt.Dimension(32674, 50));
		jPanel33.setPreferredSize(new java.awt.Dimension(220, 50));
		jPanel33.setLayout(new javax.swing.BoxLayout(jPanel33,
				javax.swing.BoxLayout.LINE_AXIS));

		jLabel25.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel25.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel25.setText("\u6267\u884c\u65e5\u671f");
		jLabel25.setMaximumSize(new java.awt.Dimension(240, 21));
		jLabel25.setPreferredSize(new java.awt.Dimension(120, 21));
		jPanel33.add(jLabel25);

		jPanel41.setMaximumSize(new java.awt.Dimension(480, 23));
		jPanel41.setPreferredSize(new java.awt.Dimension(120, 50));
		jPanel41.setLayout(new javax.swing.BoxLayout(jPanel41,
				javax.swing.BoxLayout.LINE_AXIS));

		jRadioButton3.setFont(new java.awt.Font("宋体", 0, 14));
		jRadioButton3.setText("\u661f\u671f\u4e00");
		jRadioButton3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton3ActionPerformed(evt);
			}
		});
		jPanel41.add(jRadioButton3);

		jRadioButton4.setFont(new java.awt.Font("宋体", 0, 14));
		jRadioButton4.setText("\u661f\u671f\u4e8c");
		jRadioButton4.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton4ActionPerformed(evt);
			}
		});
		jPanel41.add(jRadioButton4);

		jRadioButton5.setFont(new java.awt.Font("宋体", 0, 14));
		jRadioButton5.setText("\u661f\u671f\u4e09");
		jRadioButton5.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton5ActionPerformed(evt);
			}
		});
		jPanel41.add(jRadioButton5);

		jRadioButton6.setFont(new java.awt.Font("宋体", 0, 14));
		jRadioButton6.setText("\u661f\u671f\u56db");
		jRadioButton6.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton6ActionPerformed(evt);
			}
		});
		jPanel41.add(jRadioButton6);

		jPanel33.add(jPanel41);

		jPanel27.add(jPanel33);

		jPanel34.setMaximumSize(new java.awt.Dimension(32767, 40));
		jPanel34.setLayout(new javax.swing.BoxLayout(jPanel34,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel42.setMaximumSize(new java.awt.Dimension(240, 21));
		jPanel42.setPreferredSize(new java.awt.Dimension(120, 21));

		javax.swing.GroupLayout jPanel42Layout = new javax.swing.GroupLayout(
				jPanel42);
		jPanel42.setLayout(jPanel42Layout);
		jPanel42Layout.setHorizontalGroup(jPanel42Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 240,
				Short.MAX_VALUE));
		jPanel42Layout.setVerticalGroup(jPanel42Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 21,
				Short.MAX_VALUE));

		jPanel34.add(jPanel42);

		jPanel43.setMaximumSize(new java.awt.Dimension(240, 21));
		jPanel43.setMinimumSize(new java.awt.Dimension(120, 25));
		jPanel43.setPreferredSize(new java.awt.Dimension(0, 21));
		jPanel43.setLayout(new javax.swing.BoxLayout(jPanel43,
				javax.swing.BoxLayout.LINE_AXIS));

		jRadioButton7.setFont(new java.awt.Font("宋体", 0, 14));
		jRadioButton7.setText("\u661f\u671f\u4e94");
		jRadioButton7.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton7ActionPerformed(evt);
			}
		});
		jPanel43.add(jRadioButton7);

		jRadioButton8.setFont(new java.awt.Font("宋体", 0, 14));
		jRadioButton8.setText("\u661f\u671f\u516d");
		jRadioButton8.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton8ActionPerformed(evt);
			}
		});
		jPanel43.add(jRadioButton8);

		jRadioButton9.setFont(new java.awt.Font("宋体", 0, 14));
		jRadioButton9.setText("\u661f\u671f\u65e5");
		jRadioButton9.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jRadioButton9ActionPerformed(evt);
			}
		});
		jPanel43.add(jRadioButton9);

		jPanel34.add(jPanel43);

		jPanel44.setMaximumSize(new java.awt.Dimension(240, 21));
		jPanel44.setPreferredSize(new java.awt.Dimension(120, 21));

		javax.swing.GroupLayout jPanel44Layout = new javax.swing.GroupLayout(
				jPanel44);
		jPanel44.setLayout(jPanel44Layout);
		jPanel44Layout.setHorizontalGroup(jPanel44Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 240,
				Short.MAX_VALUE));
		jPanel44Layout.setVerticalGroup(jPanel44Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 21,
				Short.MAX_VALUE));

		jPanel34.add(jPanel44);

		jPanel27.add(jPanel34);

		jButton16.setFont(new java.awt.Font("宋体", 0, 14));
		jButton16.setText("\u4fdd\u5b58");
		jButton16.setPreferredSize(new java.awt.Dimension(70, 25));
		jButton16.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton16ActionPerformed(evt);
			}
		});

		jButton17.setFont(new java.awt.Font("宋体", 0, 14));
		jButton17.setText("\u53d6\u6d88");
		jButton17.setPreferredSize(new java.awt.Dimension(70, 25));

		javax.swing.GroupLayout jPanel35Layout = new javax.swing.GroupLayout(
				jPanel35);
		jPanel35.setLayout(jPanel35Layout);
		jPanel35Layout
				.setHorizontalGroup(jPanel35Layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								jPanel35Layout
										.createSequentialGroup()
										.addGap(241, 241, 241)
										.addComponent(
												jButton16,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(
												jButton17,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(432, Short.MAX_VALUE)));
		jPanel35Layout
				.setVerticalGroup(jPanel35Layout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								jPanel35Layout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												jPanel35Layout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																jButton16,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																jButton17,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addContainerGap(91, Short.MAX_VALUE)));

		jPanel27.add(jPanel35);
		jPanel27.add(jSeparator6);

		jTabbedPane1.addTab("\u5907\u4efd\u4f5c\u4e1a\u5b9a\u5236", jPanel27);

		jPanel12.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel56.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel56.setLayout(new javax.swing.BoxLayout(jPanel56,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel57.setPreferredSize(new java.awt.Dimension(640, 400));
		jPanel57.setLayout(new javax.swing.BoxLayout(jPanel57,
				javax.swing.BoxLayout.LINE_AXIS));

		jScrollPane5.setPreferredSize(new java.awt.Dimension(640, 3200));

		//String[][] backupjobinfo=this.getBackupInfoFromSQL();
		String[][] backupjobinfo = this.getBackupInfoFromXML();
		jTable2.setAutoCreateRowSorter(true);
		jTable2.setModel(new javax.swing.table.DefaultTableModel(backupjobinfo,
				new String[] { "backupName", "userName", "backupSrc",
						"backupVolume", "BackupState" }));
		jTable2.setPreferredSize(new java.awt.Dimension(620, 400));
		jScrollPane5.setViewportView(jTable2);

		jPanel57.add(jScrollPane5);

		jPanel56.add(jPanel57);

		jPanel59.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel59.setPreferredSize(new java.awt.Dimension(231, 35));

		jButton26.setFont(new java.awt.Font("宋体", 0, 14));
		jButton26.setText("\u5907\u4efd\u4f5c\u4e1a\u5b9a\u5236");
		jButton26.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton26ActionPerformed(evt);
			}
		});
		jPanel59.add(jButton26);

		jButton18.setFont(new java.awt.Font("宋体", 0, 14));
		jButton18.setText("\u5220\u9664");
		jButton18.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton18ActionPerformed(evt);
			}
		});
		jPanel59.add(jButton18);

		jButton19.setFont(new java.awt.Font("宋体", 0, 14));
		jButton19.setText("\u53d6\u6d88");
		jPanel59.add(jButton19);

		jButton21.setFont(new java.awt.Font("宋体", 0, 14));
		jButton21.setText("\u5f00\u59cb\u5907\u4efd");
		jButton21.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton21ActionPerformed(evt);
			}
		});
		jPanel59.add(jButton21);

		jPanel56.add(jPanel59);

		jPanel12.add(jPanel56);

		jTabbedPane1.addTab("\u5907\u4efd\u4fe1\u606f\u67e5\u8be2", jPanel12);

		jPanel60.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel60.setLayout(new javax.swing.BoxLayout(jPanel60,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel61.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel61.setLayout(new javax.swing.BoxLayout(jPanel61,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel62.setPreferredSize(new java.awt.Dimension(640, 400));
		jPanel62.setLayout(new javax.swing.BoxLayout(jPanel62,
				javax.swing.BoxLayout.LINE_AXIS));

		jScrollPane6.setPreferredSize(new java.awt.Dimension(640, 3200));

		String[][] restoreinfo;
		if (configfile == null)
			restoreinfo = null;
		else
			restoreinfo = getRestoreInfo(configfile);
		jTable3.setModel(new javax.swing.table.DefaultTableModel(restoreinfo,
				new String[] { "BackupJobID", "BackupHostName", "BackupSrc",
						"BackupSize", "BackupDate", "BackupVolume",
						"BackupFilesCount" }));
		jTable3.setPreferredSize(new java.awt.Dimension(620, 400));
		jScrollPane6.setViewportView(jTable3);

		jPanel62.add(jScrollPane6);

		jPanel61.add(jPanel62);

		jPanel64.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel64.setPreferredSize(new java.awt.Dimension(231, 35));

		jButton20.setFont(new java.awt.Font("宋体", 0, 14));
		jButton20.setText("\u9009\u62e9\u6062\u590d\u4f4d\u7f6e");
		jButton20.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton20ActionPerformed(evt);
			}
		});
		jPanel64.add(jButton20);

		jButton22.setFont(new java.awt.Font("宋体", 0, 14));
		jButton22.setText("\u5f00\u59cb\u6062\u590d");
		jButton22.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton22ActionPerformed(evt);
			}
		});
		jPanel64.add(jButton22);

		jButton23.setFont(new java.awt.Font("宋体", 0, 14));
		jButton23.setText("\u53d6\u6d88");
		jPanel64.add(jButton23);

		jPanel61.add(jPanel64);

		jPanel60.add(jPanel61);

		jTabbedPane1.addTab("\u6062\u590d\u7ba1\u7406", jPanel60);

		jPanel63.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel63.setLayout(new javax.swing.BoxLayout(jPanel63,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel66.setPreferredSize(new java.awt.Dimension(800, 640));
		jPanel66.setLayout(new javax.swing.BoxLayout(jPanel66,
				javax.swing.BoxLayout.PAGE_AXIS));

		jScrollPane7.setPreferredSize(new java.awt.Dimension(640, 330));

		jTextArea3.setColumns(20);
		jTextArea3.setRows(5);
		jTextArea3.setPreferredSize(new java.awt.Dimension(1726, 2147483));
		jTextArea3.append(getLogInfo(Main.logPath).toString());
		jScrollPane7.setViewportView(jTextArea3);

		jPanel66.add(jScrollPane7);

		jPanel68.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel68.setPreferredSize(new java.awt.Dimension(231, 35));

		jButton24.setFont(new java.awt.Font("宋体", 0, 14));
		jButton24.setText("\u5907\u4efd\u4fe1\u606f\u67e5\u8be2");
		jButton24.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton24ActionPerformed(evt);
			}
		});
		jPanel68.add(jButton24);

		jButton25.setFont(new java.awt.Font("宋体", 0, 14));
		jButton25.setText("\u6062\u590d\u4fe1\u606f\u67e5\u8be2");
		jButton25.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton25ActionPerformed(evt);
			}
		});
		jPanel68.add(jButton25);

		jButton12.setFont(new java.awt.Font("宋体", 0, 14));
		jButton12.setText("\u8fd0\u884c\u72b6\u6001\u67e5\u8be2");
		jButton12.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton12ActionPerformed(evt);
			}
		});
		jPanel68.add(jButton12);

		jPanel66.add(jPanel68);

		jPanel63.add(jPanel66);

		jTabbedPane1.addTab("\u65e5\u5fd7\u7ba1\u7406", jPanel63);

		jPanel6.add(jTabbedPane1);

		jPanel5.add(jPanel6);

		jSeparator2.setMaximumSize(new java.awt.Dimension(32767, 640));
		jPanel5.add(jSeparator2);

		jPanel7.setMaximumSize(new java.awt.Dimension(2147483647, 20));
		jPanel7.setMinimumSize(new java.awt.Dimension(558, 15));
		jPanel7.setPreferredSize(new java.awt.Dimension(100, 20));

		jLabel7.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel7
				.setText("Backupdedup\u662f\u4e00\u6b3e\u652f\u6301\u5728\u7ebf\u53bb\u91cd\u7684\u5907\u4efd\u8f6f\u4ef6.CopyRight:XJTU.");
		jPanel7.add(jLabel7);

		jPanel5.add(jPanel7);

		jPanel3.add(jPanel5);

		jPanel1.add(jPanel3);

		getContentPane().add(jPanel1);

		jMenu1.setText("\u6587\u4ef6");
		jMenuBar1.add(jMenu1);

		jMenu2.setText("\u5de5\u5177");

		jMenuItem3.setText("Item");
		jMenu2.add(jMenuItem3);

		jMenuBar1.add(jMenu2);

		jMenu3.setText("\u67e5\u627e");
		jMenuBar1.add(jMenu3);

		jMenu5.setText("\u5176\u4ed6");

		jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_H, 0));
		jMenuItem1.setText("Help");
		jMenu5.add(jMenuItem1);

		jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_A,
				java.awt.event.InputEvent.CTRL_MASK));
		jMenuItem2.setText("About");
		jMenu5.add(jMenuItem2);

		jMenuBar1.add(jMenu5);

		setJMenuBar(jMenuBar1);

		pack();
	}// </editor-fold>
	//GEN-END:initComponents

	private void jButton12ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		jTextArea3.setText(null);
		jTextArea3.append(getLogInfo(Main.runstatelogPath).toString());
	}

	private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
	}

	private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
	}

	private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		int i = jTable1.getSelectedRow();
		try {
			delVolumeInfoFromFile(i);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		delvolumeconfigfile(jTable1.getValueAt(i, 0).toString());
		DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
		model.removeRow(i);//remove the ith row	
	}

	private void jButton26ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new CreateBackupJob().setVisible(true);
			}
		});
		//		new CreateBackupJobThread().start();
	}

	private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
			//			if (jTabbedPane1.getComponentAt(i) == jPanel63)
			//				jTabbedPane1.remove(jPanel63);
			jTabbedPane1.remove(i);
		}
		removejPanel63Action();
		initjPanel63();
		//		jTabbedPane1.add("日志管理", jPanel63);
	}

	private void jRadioButton9ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jRadioButton9.isSelected()) {
			weekp.Sunday = this.type;
		}
	}

	private void jRadioButton8ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jRadioButton8.isSelected()) {
			weekp.Saturday = this.type;
		}
	}

	private void jRadioButton7ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jRadioButton7.isSelected()) {
			weekp.Friday = this.type;
		}
	}

	private void jRadioButton5ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jRadioButton5.isSelected()) {
			weekp.Wednesday = this.type;
		}
	}

	private void jRadioButton4ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jRadioButton4.isSelected()) {
			weekp.Tuesday = this.type;
		}
	}

	private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jRadioButton3.isSelected()) {
			weekp.Monday = this.type;
		}
	}

	private void jRadioButton6ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jRadioButton6.isSelected()) {
			weekp.Thursday = this.type;
		}
	}

	private void jCheckBox7ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jCheckBox7.isSelected()) {
			jCheckBox5.setSelected(false);
			jCheckBox6.setSelected(false);

			type = 2;
		}
	}

	private void jCheckBox6ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jCheckBox6.isSelected()) {
			jCheckBox5.setSelected(false);
			jCheckBox7.setSelected(false);

			type = 1;
		}
	}

	private void jCheckBox5ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		if (jCheckBox5.isSelected()) {
			jCheckBox6.setSelected(false);
			jCheckBox7.setSelected(false);

			type = 0;
		}
	}

	private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {
		// TODO add your handling code here:
		if (index != jTabbedPane1.getSelectedIndex()) {
			if (index < jTabbedPane1.getTabCount() - 1)
				jTabbedPane1.setEnabledAt(index + 1, true);
		}
		index = jTabbedPane1.getSelectedIndex();

	}

	private void jButton15ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:

	}

	private void jButton14ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		File file = null;
		jFileChooser1.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);//set file selection mode:we can select file and directory.
		int returnVal = jFileChooser1.showOpenDialog(new javax.swing.JFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = jFileChooser1.getSelectedFile();
			if (jTextArea1.getText() == null)
				jTextArea1.setText(file.getAbsolutePath() + "\n");
			else
				jTextArea1.append(file.getAbsolutePath() + "\n");
		}
	}

	private void jTextField8ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		backupJobname = jTextField8.getText().trim().toString();
	}

	private void jButton16ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		jTabbedPane1.remove(jPanel27);
		jTabbedPane1.add("备份信息查询", jPanel12);
		//jTabbedPane1.setEnabledAt(5, true);
	}

	private void jButton25ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		jTextArea3.setText(null);
		jTextArea3.append(getLogInfo(Main.restorelogPath).toString());
	}

	private void jButton24ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		jTextArea3.setText(null);
		jTextArea3.append(getLogInfo(Main.backuplogPath).toString());
	}

	private void jButton22ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		int i = jTable3.getSelectedRow();
		if (i == -1) {
			JOptionPane.showMessageDialog(null,
					"There are no restore job be selected.");
		} else {
			String backupjobid = jTable3.getValueAt(i, 0).toString();
			String backuphostname = jTable3.getValueAt(i, 1).toString();
			String backupsrc = jTable3.getValueAt(i, 2).toString();
			String backupdate = jTable3.getValueAt(i, 4).toString();
			String backupvolume = jTable3.getValueAt(i, 5).toString();
			long filescount = Long.parseLong(jTable3.getValueAt(i, 6)
					.toString());
			String restoredest = file.getAbsolutePath() + File.separator;

			BackupApp ba = new BackupApp();
			int clients = 10;
			String configfile = null;
			try {
				configfile = BackupApp.writeRestoreConfigToXML(backupjobid,
						backuphostname, restoredest, backupsrc, clients,
						backupvolume, filescount, backupdate);
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//写恢复信息，然后解析恢复信息，多次一举？——>不写，直接解析
			try {
				BackupApp.startrestore(configfile);
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void jButton20ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		jFileChooser2.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);//set file selection mode:we can select file and directory.
		int returnVal = jFileChooser2.showOpenDialog(new javax.swing.JFrame());
		file = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			file = jFileChooser2.getSelectedFile();
		}
	}

	private void jButton21ActionPerformed(java.awt.event.ActionEvent evt) {
		//parsing backup information
		int i = jTable2.getSelectedRow();
		String volume_name = jTable2.getValueAt(i, 3).toString().trim();
		//		if(bclient != null && !volume_name.equals(BackupJob.backup_volume_name)){//停止上一次备份任务对象
		//			bclient.stopsdfs();
		//		}
		if (i == -1) {
			JOptionPane.showMessageDialog(null,
					"There are no backup jobs be selected.");
		} else {
			try {
				if (configfile == null) {
					configfile = (new BackupJobRecord())
							.getbackupjobconfigpath();
				}
				ba.preProcessBackupJob(configfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// TODO add your handling code here:
			String backupJobID = jTable2.getValueAt(i, 0).toString().trim();
			bclient = BackupServiceProxy.backupclients.get(backupJobID);
			if (bclient == null) {
				try {
					try {
						BackupJob.backupClientHostName = InetAddress
								.getLocalHost().getHostName();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					bclient = new BackupClient(backupJobID,
							BackupJob.backupClientHostName,
							BackupJob.clientPort,
							BackupJob.backupServerHostName,
							BackupJob.serverPort, backupSrc, weekp, backupDate,
							backupVolume, backupFilesLength,
							backupJobFilesCount);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//		jProgressBar1.setVisible(true);
			boolean backupstate = false;
			try {
				backupstate = bclient.execBackupJob();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//change the value of field which is selected.
			if (backupstate) {
				jTable2.getModel().setValueAt("done", i, 4);
				try {
					ba.updateBackupStateToXML(backupJobID, configfile);
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void jButton18ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		int i = jTable2.getSelectedRow();
		if (i == -1) {
			JOptionPane.showMessageDialog(null,
					"There are no rows have been selected.");
		} else {
			delBackupInfoFromXML(jTable2.getValueAt(i, 0).toString().trim(),
					jTable2.getValueAt(i, 2).toString().trim());
			DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
			model.removeRow(i);//remove the ith row
		}
	}

	private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
			//			if (jTabbedPane1.getComponentAt(i) == jPanel12)
			//				jTabbedPane1.remove(jPanel12);
			jTabbedPane1.remove(i);
		}
		removejPanel12Action();
		initjPanel12();
		//		jTabbedPane1.add("备份作业查询", jPanel12);
		//jTabbedPane1.setEnabledAt(3, true);

	}

	private void jTabbedPane1MousePressed(java.awt.event.MouseEvent evt) {
		// TODO add your handling code here:
		if (evt.getClickCount() == 2) {
			//	myDoubleClick(selRow, selPath);
			//	jTabbedPane1.getSelectedComponent();
			jTabbedPane1.remove(jTabbedPane1.getSelectedComponent());
		}
	}

	private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		//jTabbedPane1.addTab("创建卷", jPanel19);
		//	final CreateVolume cVolume=new CreateVolume();
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new CreateVolume().setVisible(true);
			}
		});
		//	((DefaultTableModel)jTable1.getModel()).addRow(new CreateVolume().getvolume());
	}

	private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		int i = jTable1.getSelectedRow();
		if (i == -1) {
			JOptionPane.showMessageDialog(null,
					"There is no volume be selected.");
		} else {
			@SuppressWarnings("unused")
			String volume_name = jTable1.getValueAt(i, 0).toString().trim();
		}
	}

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
			//			if (jTabbedPane1.getComponentAt(i) == jPanel14)
			//				jTabbedPane1.remove(jPanel14);
			jTabbedPane1.remove(i);
		}
		removejPanel14Action();
		initjPanel14();
		//		jTabbedPane1.addTab("卷管理", jPanel14);

	}

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
			//			if (jTabbedPane1.getComponentAt(i) == jPanel8)
			//				jTabbedPane1.remove(jPanel8);
			jTabbedPane1.remove(i);
		}
		initjPanel8();
		//		jTabbedPane1.add("用户管理", jPanel8);
		//	jTabbedPane1.setEnabledAt(6, false);

	}

	private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		System.exit(0);
	}

	private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:

	}

	private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		for (int i = 0; i < jTabbedPane1.getTabCount(); i++) {
			//			if (jTabbedPane1.getComponentAt(i) == jPanel60)
			//				jTabbedPane1.remove(jPanel60);
			jTabbedPane1.remove(i);
		}
		removejPanel60Action();
		initjPanel60();
		//		jTabbedPane1.add("恢复管理", jPanel60);

	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new MainFrame().setVisible(true);
			}
		});
	}

	//GEN-BEGIN:variables
	// Variables declaration - do not modify
	private javax.swing.JButton jButton1;
	private javax.swing.JButton jButton10;
	private javax.swing.JButton jButton11;
	private javax.swing.JButton jButton12;
	private javax.swing.JButton jButton14;
	private javax.swing.JButton jButton15;
	private javax.swing.JButton jButton16;
	private javax.swing.JButton jButton17;
	private javax.swing.JButton jButton18;
	private javax.swing.JButton jButton19;
	private javax.swing.JButton jButton2;
	private javax.swing.JButton jButton20;
	private javax.swing.JButton jButton21;
	private javax.swing.JButton jButton22;
	private javax.swing.JButton jButton23;
	private javax.swing.JButton jButton24;
	private javax.swing.JButton jButton25;
	private javax.swing.JButton jButton26;
	private javax.swing.JButton jButton3;
	private javax.swing.JButton jButton4;
	private javax.swing.JButton jButton5;
	private javax.swing.JButton jButton6;
	private javax.swing.JButton jButton7;
	private javax.swing.JButton jButton8;
	private javax.swing.JButton jButton9;
	private javax.swing.JCheckBox jCheckBox5;
	private javax.swing.JCheckBox jCheckBox6;
	private javax.swing.JCheckBox jCheckBox7;
	private javax.swing.JCheckBox jCheckBox8;
	private javax.swing.JCheckBox jCheckBox9;
	private javax.swing.JFileChooser jFileChooser1;
	private javax.swing.JFileChooser jFileChooser2;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel10;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel21;
	private javax.swing.JLabel jLabel22;
	private javax.swing.JLabel jLabel23;
	private javax.swing.JLabel jLabel24;
	private javax.swing.JLabel jLabel25;
	private javax.swing.JLabel jLabel27;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JLabel jLabel9;
	private javax.swing.JMenu jMenu1;
	private javax.swing.JMenu jMenu2;
	private javax.swing.JMenu jMenu3;
	private javax.swing.JMenu jMenu5;
	private javax.swing.JMenuBar jMenuBar1;
	private javax.swing.JMenuItem jMenuItem1;
	private javax.swing.JMenuItem jMenuItem2;
	private javax.swing.JMenuItem jMenuItem3;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel10;
	private javax.swing.JPanel jPanel11;
	private javax.swing.JPanel jPanel12;
	private javax.swing.JPanel jPanel13;
	private javax.swing.JPanel jPanel14;
	private javax.swing.JPanel jPanel15;
	private javax.swing.JPanel jPanel16;
	private javax.swing.JPanel jPanel18;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel27;
	private javax.swing.JPanel jPanel28;
	private javax.swing.JPanel jPanel29;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel30;
	private javax.swing.JPanel jPanel31;
	private javax.swing.JPanel jPanel32;
	private javax.swing.JPanel jPanel33;
	private javax.swing.JPanel jPanel34;
	private javax.swing.JPanel jPanel35;
	private javax.swing.JPanel jPanel36;
	private javax.swing.JPanel jPanel37;
	private javax.swing.JPanel jPanel38;
	private javax.swing.JPanel jPanel39;
	private javax.swing.JPanel jPanel40;
	private javax.swing.JPanel jPanel41;
	private javax.swing.JPanel jPanel42;
	private javax.swing.JPanel jPanel43;
	private javax.swing.JPanel jPanel44;
	private javax.swing.JPanel jPanel49;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel50;
	private javax.swing.JPanel jPanel51;
	private javax.swing.JPanel jPanel52;
	private javax.swing.JPanel jPanel53;
	private javax.swing.JPanel jPanel54;
	private javax.swing.JPanel jPanel55;
	private javax.swing.JPanel jPanel56;
	private javax.swing.JPanel jPanel57;
	private javax.swing.JPanel jPanel59;
	private javax.swing.JPanel jPanel6;
	private javax.swing.JPanel jPanel60;
	private javax.swing.JPanel jPanel61;
	private javax.swing.JPanel jPanel62;
	private javax.swing.JPanel jPanel63;
	private javax.swing.JPanel jPanel64;
	private javax.swing.JPanel jPanel66;
	private javax.swing.JPanel jPanel68;
	private javax.swing.JPanel jPanel69;
	private javax.swing.JPanel jPanel7;
	public javax.swing.JPanel jPanel8;
	private javax.swing.JPanel jPanel9;
	private javax.swing.JRadioButton jRadioButton3;
	private javax.swing.JRadioButton jRadioButton4;
	private javax.swing.JRadioButton jRadioButton5;
	private javax.swing.JRadioButton jRadioButton6;
	private javax.swing.JRadioButton jRadioButton7;
	private javax.swing.JRadioButton jRadioButton8;
	private javax.swing.JRadioButton jRadioButton9;
	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JScrollPane jScrollPane5;
	private javax.swing.JScrollPane jScrollPane6;
	private javax.swing.JScrollPane jScrollPane7;
	private javax.swing.JSeparator jSeparator1;
	private javax.swing.JSeparator jSeparator2;
	private javax.swing.JSeparator jSeparator3;
	private javax.swing.JSeparator jSeparator6;
	private javax.swing.JSeparator jSeparator7;
	private javax.swing.JSeparator jSeparator8;
	private javax.swing.JTabbedPane jTabbedPane1;
	private static javax.swing.JTable jTable1;
	private static javax.swing.JTable jTable2;
	private javax.swing.JTable jTable3;
	private javax.swing.JTextArea jTextArea1;
	private javax.swing.JTextArea jTextArea3;
	private javax.swing.JTextField jTextField1;
	private javax.swing.JTextField jTextField2;
	private javax.swing.JTextField jTextField3;
	private javax.swing.JTextField jTextField8;
	private javax.swing.JTextField jTextField9;
	// End of variables declaration//GEN-END:variables

}