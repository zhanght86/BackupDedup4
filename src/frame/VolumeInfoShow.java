/*
 * VolumeInfoShow.java
 *
 * Created on __DATE__, __TIME__
 */

package frame;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import org.opendedup.util.OSValidator;

import xjtu.dedup.DB.MySQLDB;

/**
 *
 * @author  __USER__
 */
public class VolumeInfoShow extends javax.swing.JFrame{
	//mysql db info
	Object[][] backupinfo = null;
	Object[][] volumesinfo = null;
	String[][] backupjobinfo = null;
//	String host = "localhost:3306";
//	String database = "backupmetadata";
//	String user = "root";
//	String passwd = "123456";
//	String url = "jdbc:mysql://" + host + "/" + database;
//	MySQLDB sqldb = MySQLDB.getInstance(host, database, user, passwd);

//	/*
//	 * 从数据库中读取卷信息，返回Object[].
//	 * */
//	//	MySQLDB sqldb = MySQLDB.getInstance(host, database, user, passwd);
//	public String[][] getVolumeInfoFromSQL() {
//		String[][] volumesinfo = null;
//		String sql = "select * from volumesinfo";
//		//MySQLDB sqldb = MySQLDB.getInstance(host, database, user, passwd);
//		ResultSet rs = sqldb.executeQuery(sql);
//		int i = 0;
//		int rownum = 0;
//		try {
//			while (rs.next())
//				rownum++;
//		} catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//			try {
//				rs.close();
//			} catch (SQLException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		}
//
//		volumesinfo = new String[rownum][5];
//		rs = sqldb.executeQuery(sql);
//		try {
//			//		System.out.println("connecting database......");	
//			String vol_name;
//			String vol_cap;
//			String chunk_size;
//			String vol_type;
//			String curr_size;
//			String remote_local = null;
//			while (rs.next()) {
//				vol_name = rs.getString("volume_name");
//				vol_cap = rs.getString("volume_capacity");
//				chunk_size = rs.getString("chunk_size");
//				vol_type = rs.getString("volume_type");
//				if (rs.getString("is_local") == "")
//					remote_local = null;
//				else {
//					if (Boolean.parseBoolean(rs.getString("is_local")) == true)
//						remote_local = "local";
//					else if (Boolean.parseBoolean(rs.getString("is_local")) == false)
//						remote_local = "remote";
//				}
//				volumesinfo[i][0] = vol_name;
//				volumesinfo[i][1] = vol_cap;
//				volumesinfo[i][2] = chunk_size;
//				volumesinfo[i][3] = vol_type;
//				volumesinfo[i][4] = remote_local;
//				i++;
//				//	System.out.println("vol_name:"+vol_name+" volume_capacity:"+vol_cap+" chunk_size:"+chunk_size+" volume_type:"+vol_type);
//			}
//			//	sqldb.close(); //No operations allowed after statement closed.
//		} catch (SQLException e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
//		return volumesinfo;
//	}

	public String[][] getVolumeInfoFromFile() {
		String[][] volumesinfo = null;
		int i = 0;
		int rownum = 0;
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
		if(rownum!=0){
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
		}else{
			volumesinfo=null;
		}
		//	System.out.println("vol_name:"+vol_name+" volume_capacity:"+vol_cap+" chunk_size:"+chunk_size+" volume_type:"+vol_type);
		return volumesinfo;
	}

	public void delVolumeInfoFromFile(int delnum) throws FileNotFoundException {	
		int linenum=0;
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
				if(linenum!=delnum){
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
		try{
			fw=new FileWriter(volumesfile,false);
			bw=new BufferedWriter(fw);
			bw.write(sBuffer.toString());
			bw.flush();
			bw.close();
			fw.close();
		}catch (IOException e) {
			// TODO: handle exception
		}
	}
	/** Creates new form VolumeInfoShow */
	public VolumeInfoShow() {
		this.setTitle("选择卷");
		initComponents();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(640, 480);
		this.setLocation(d.width / 2 - 640 / 2, d.height / 2 - 480 / 2);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	//GEN-BEGIN:initComponents
	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents() {

		jPanel14 = new javax.swing.JPanel();
		jPanel15 = new javax.swing.JPanel();
		jPanel17 = new javax.swing.JPanel();
		jLabel16 = new javax.swing.JLabel();
		jPanel16 = new javax.swing.JPanel();
		jScrollPane3 = new javax.swing.JScrollPane();
		jTable1 = new javax.swing.JTable();
		jPanel18 = new javax.swing.JPanel();
		jButton11 = new javax.swing.JButton();
		jButton1 = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				formWindowClosing(evt);
			}
		});
		getContentPane().setLayout(
				new javax.swing.BoxLayout(getContentPane(),
						javax.swing.BoxLayout.LINE_AXIS));

		jPanel14.setLayout(new javax.swing.BoxLayout(jPanel14,
				javax.swing.BoxLayout.LINE_AXIS));

		jPanel15.setPreferredSize(new java.awt.Dimension(640, 480));
		jPanel15.setLayout(new javax.swing.BoxLayout(jPanel15,
				javax.swing.BoxLayout.PAGE_AXIS));

		jPanel17.setMaximumSize(new java.awt.Dimension(32767, 320));
		jPanel17.setPreferredSize(new java.awt.Dimension(320, 100));

		jLabel16.setFont(new java.awt.Font("宋体", 0, 14));
		jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel16.setText("\u5377\u4fe1\u606f\u67e5\u8be2");
		jLabel16.setMaximumSize(new java.awt.Dimension(800, 21));
		jLabel16.setMinimumSize(new java.awt.Dimension(120, 21));
		jLabel16.setPreferredSize(new java.awt.Dimension(480, 21));

		javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(
				jPanel17);
		jPanel17.setLayout(jPanel17Layout);
		jPanel17Layout.setHorizontalGroup(jPanel17Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 400,
				Short.MAX_VALUE).addGroup(
				jPanel17Layout.createParallelGroup(
						javax.swing.GroupLayout.Alignment.LEADING).addGroup(
						jPanel17Layout.createSequentialGroup().addGap(0, 0,
								Short.MAX_VALUE).addComponent(jLabel16,
								javax.swing.GroupLayout.PREFERRED_SIZE, 120,
								javax.swing.GroupLayout.PREFERRED_SIZE).addGap(
								0, 0, Short.MAX_VALUE))));
		jPanel17Layout.setVerticalGroup(jPanel17Layout.createParallelGroup(
				javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 52,
				Short.MAX_VALUE).addGroup(
				jPanel17Layout.createParallelGroup(
						javax.swing.GroupLayout.Alignment.LEADING).addGroup(
						jPanel17Layout.createSequentialGroup().addGap(0, 0,
								Short.MAX_VALUE).addComponent(jLabel16,
								javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE,
								javax.swing.GroupLayout.PREFERRED_SIZE).addGap(
								0, 0, Short.MAX_VALUE))));

		jPanel15.add(jPanel17);

		jPanel16.setPreferredSize(new java.awt.Dimension(640, 400));
		jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16,
				javax.swing.BoxLayout.LINE_AXIS));

		jScrollPane3.setPreferredSize(new java.awt.Dimension(640, 3200));

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

		jButton11.setFont(new java.awt.Font("宋体", 0, 14));
		jButton11.setText("\u9009\u62e9");
		jButton11.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton11ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton11);

		jButton1.setFont(new java.awt.Font("宋体", 0, 14));
		jButton1.setText("\u5220\u9664");
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		jPanel18.add(jButton1);

		jPanel15.add(jPanel18);

		jPanel14.add(jPanel15);

		getContentPane().add(jPanel14);

		pack();
	}// </editor-fold>
	//GEN-END:initComponents

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		int i = jTable1.getSelectedRow();
		try {
			delVolumeInfoFromFile(i);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DefaultTableModel model=(DefaultTableModel)jTable1.getModel();
		model.removeRow(i);//remove the ith row
	}

	private void formWindowClosing(java.awt.event.WindowEvent evt) {
		// TODO add your handling code here:
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {
		// TODO add your handling code here:
		int i = jTable1.getSelectedRow();
		if(i!=-1){
			TempStorage.vol_name = jTable1.getValueAt(i, 0).toString().trim();
		}		
		CreateBackupJob.setTextField9(TempStorage.vol_name);
		//jTextField8.setText(volume_name);
		//jpanel1card.show(jPanel4, "card6");
		this.dispose();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new VolumeInfoShow().setVisible(true);
			}
		});
	}

	//GEN-BEGIN:variables
	// Variables declaration - do not modify
	private javax.swing.JButton jButton1;
	private javax.swing.JButton jButton11;
	private javax.swing.JLabel jLabel16;
	private javax.swing.JPanel jPanel14;
	private javax.swing.JPanel jPanel15;
	private javax.swing.JPanel jPanel16;
	private javax.swing.JPanel jPanel17;
	private javax.swing.JPanel jPanel18;
	private javax.swing.JScrollPane jScrollPane3;
	private javax.swing.JTable jTable1;
	// End of variables declaration//GEN-END:variables

}
