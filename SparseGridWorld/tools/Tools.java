package tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.border.TitledBorder;
import javax.swing.text.MaskFormatter;
import javax.swing.text.NumberFormatter;

public final class Tools
{

    public static final char COPYRIGHT_SYMBOL = 0xA9;

    public static final char PI = '\u03A0';

    public static final NumberFormat numberFormat = NumberFormat
	    .getNumberInstance();

    private static BufferedReader reader = new BufferedReader(
	    new InputStreamReader(System.in));

    private static boolean DEBUG = false;
    
    private static long TIME = System.currentTimeMillis();

    static
    {
	numberFormat.setMaximumFractionDigits(5);
    }

    /**
         * 定位窗体到屏幕中间
         * 
         * @param win Window 对象
         */
    public static void center(Window win)
    {
	Toolkit tkit = Toolkit.getDefaultToolkit();
	Dimension screenSize = tkit.getScreenSize();
	Dimension windowSize = win.getSize();
	if (windowSize.height > screenSize.height)
	{
	    windowSize.height = screenSize.height;
	}
	if (windowSize.width > screenSize.width)
	{
	    windowSize.width = screenSize.width;
	}
	win.setLocation((screenSize.width - windowSize.width) / 2,
		(screenSize.height - windowSize.height) / 2);
    }

    /**
         * 返回以<code>title</code>为标题的TitledBorder,标题颜色为<code>c</code>
         * 
         * @param title 标题
         * @param c 标题颜色
         * @return TitledBorder对象
         * @see BorderFactory
         * @see TitledBorder
         */
    public static TitledBorder getTitledBorder(String title, Color c)
    {
	TitledBorder border = BorderFactory.createTitledBorder(title);
	border.setTitleColor(c);
	return border;
    }

    /**
         * 返回以<code>title</code>为标题的TitledBorder
         * 
         * @param title 标题
         * @return TitledBorder对象
         * @see BorderFactory
         * @see TitledBorder
         */
    public static TitledBorder getTitledBorder(String title)
    {
	return BorderFactory.createTitledBorder(title);
    }

    /**
         * 向布局为GridBagLayout的<code>Container c</code>中添加组件
         * 
         * @param c 容器
         * @param gridx 网格布局中的x方向的位置
         * @param gridy 网格布局中的y方向的位置
         * @param gridWidth 所占网格的宽度
         * @param gridHeight 所占网格的高度
         * @param gbCon GridBagConstraints的对象
         * @param comp 被添加的组件
         * @see GridBagLayout
         * @see GridBagConstraints
         */
    public static void addComponent(Container c, int gridx, int gridy,
	    int gridWidth, int gridHeight, GridBagConstraints gbCon,
	    Component comp)
    {
	gbCon.insets = new Insets(5, 5, 5, 5);
	gbCon.gridx = gridx;
	gbCon.gridy = gridy;
	gbCon.gridwidth = gridWidth;
	gbCon.gridheight = gridHeight;
	c.add(comp, gbCon);
    }

    /**
         * 得到一个输入日期的输入文本域
         * 
         * @param df SimpleDateFormat的对象
         * @return JTextField的对象
         * @see SimpleDateFormat
         * @see JTextField
         */
    public static JTextField getDateInputField(SimpleDateFormat df)
    {
	JFormattedTextField txtField = new JFormattedTextField(df);
	txtField.setColumns(20);
	return txtField;
    }

    /**
         * 得到一个输入整数的输入文本域
         * 
         * @param format 格式化字符串,使用方法见MaskFormatter
         * @return 输入文本域
         * @see MaskFormatter
         */
    public static JFormattedTextField getIntegerFormattedTextField(String format)
    {
	AbstractFormatter formatter = null;
	try
	{
	    formatter = new MaskFormatter(format);
	    MaskFormatter f = ((MaskFormatter) formatter);
	    f.setPlaceholderCharacter('0');
	}
	catch (Exception e)
	{
	    NumberFormat nf = NumberFormat.getIntegerInstance();
	    nf.setGroupingUsed(false);
	    formatter = new NumberFormatter(nf);
	    e.printStackTrace();
	}
	JFormattedTextField txtField = new JFormattedTextField(formatter);
	txtField.setColumns(20);
	return txtField;
    }

    /**
         * 得到一个输入整数的文本域
         * 
         * @return 输入整数的文本域
         */
    public static JFormattedTextField getIntegerFormattedTextField()
    {
	NumberFormat nf = NumberFormat.getIntegerInstance();
	nf.setGroupingUsed(false);
	JFormattedTextField txtField = new JFormattedTextField(nf);
	txtField.setColumns(20);
	return txtField;
    }

    /**
         * 得到一个输入数,包括整数,浮点数的文本域
         * 
         * @return 输入数值的文本域
         */
    public static JFormattedTextField getNumberInputField()
    {
	NumberFormat nf = NumberFormat.getNumberInstance();
	nf.setMaximumFractionDigits(10);
	nf.setGroupingUsed(false);
	JFormattedTextField txtField = new JFormattedTextField(nf);
	txtField.setColumns(20);
	return txtField;
    }

    // public static TableCellEditor getIntegerTableCellEditor()
    // {
    // return new DefaultCellEditor(getIntegerFormattedTextField());
    // }

    /**
         * 得到一个布局为<code>FlowLayout(FlowLayout.LEFT,10,10)</code>的面板, 并浆参数<code>JComponent c</code>添加到面板中
         * 
         * @param c JComponent的对象
         * @return 面板
         */
    public static JPanel getFlowLayoutPanel(JComponent c)
    {
	return Tools.getFlowLayoutPanel(c, FlowLayout.LEFT);
    }

    /**
         * 得到一个布局为<code>FlowLayout(align,10,10)</code>的面板, 并浆参数<code>JComponent c</code>添加到面板中
         * 
         * @param c JComponent的对象
         * @param align 面板的对其方式,参加<code>FlowLayout</code>
         * @return 面板
         * @see FlowLayout
         */
    public static JPanel getFlowLayoutPanel(JComponent c, int align)
    {
	JPanel p = new JPanel(new FlowLayout(align, 10, 10));
	p.setOpaque(false);
	p.add(c);
	return p;
    }

    /**
         * 得到文件的后缀名
         * 
         * @param file 文件名(可以带有路径)
         * @return 文件后缀,如果没有后缀返回null
         */
    public static String getFileType(String file)
    {
	int index = file.lastIndexOf('.');
	if (index == -1)
	    return null;
	else
	    return file.substring(index + 1, file.length());
    }

    /**
         * 得到除文件后缀的文件名
         * 
         * @param file 完整文件名(可以包含路径)
         * @return 除去文件后缀的文件名
         */
    public static String getFileName(String file)
    {
	int index = file.lastIndexOf('.');
	if (index == -1)
	    return file;
	else
	    return file.substring(0, index);
    }

    /**
         * 得到修改文件后缀的文件名
         * 
         * @param file 源文件名
         * @param type 文件后缀
         * @return 修改文件后缀后的文件名
         */
    public static String changeFileType(String file, String type)
    {
	String name = getFileName(file);
	return name + "." + type;
    }

    /**
         * 拷贝文件
         * 
         * @param src 源文件路径
         * @param dst 目的文件路径
         */
    public static void copyFile(String src, String dst)
    {
	try
	{
	    FileInputStream in = new FileInputStream(src);
	    FileOutputStream out = new FileOutputStream(dst);
	    byte[] buffer = new byte[8192];
	    while ((in.read(buffer)) != -1)
	    {
		out.write(buffer);
	    }
	    out.flush();
	    out.close();
	    in.close();
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	}
    }

    // /**
    // * 打印数组到标准输出
    // *
    // * @param arr 数组
    // */
    // public static void printArray(Object[] arr)
    // {
    // for (int i = 0; i < arr.length; i++)
    // {
    // System.out.print(arr[i] + " ");
    // }
    // System.out.println();
    // }

    public static int sum(int[] arr)
    {
	int sum = 0;
	for (int i = 0; i < arr.length; i++)
	{
	    sum += arr[i];
	}
	return sum;
    }

    public static <T> void printArray(T[] arr)
    {
	for (int i = 0; i < arr.length; i++)
	{
	    System.out.print(arr[i] + " ");
	}
	System.out.println();
    }

    public static <T> void printArray(double[] arr)
    {
	for (int i = 0; i < arr.length; i++)
	{
	    System.out.print(numberFormat.format(arr[i]) + " \t");
	}
	System.out.println();
    }

    public static void printArray(double[][] arr)
    {
	for (int i = 0; i < arr.length; i++)
	{
	    for (int j = 0; j < arr[0].length; j++)
		System.out.print(numberFormat.format(arr[i][j]) + "  ");
	    System.out.println();
	}
    }

    /**
         * 打印Map的内容,包括key和value,打印形式为key->value
         * 
         * @param map
         */
    public static void printMap(Map<?, ?> map)
    {
	if (map.size() == 0)
	{
	    System.out.println("Empty map");
	    return;
	}
	for (Object ele : map.keySet())
	{
	    System.out.println(ele + " -> " + map.get(ele));
	}
    }

    /**
         * 打印系统属性
         */
    public static void printSystemProperties()
    {
	Properties p = System.getProperties();
	printMap(p);
    }

    public static void enableDebug(boolean b)
    {
	DEBUG = b;
    }

    public static void DEBUG(String str)
    {
	if (DEBUG)
	{
	    System.out.println(str);
	}
    }

    public static String readLine()
    {
	String line = null;
	try
	{
	    line = reader.readLine();
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
	return line;
    }

    public static int readInt()
    {
	String input = readLine();
	return Integer.parseInt(input.trim());
    }

    public static double readDouble()
    {
	String input = readLine();
	return Double.parseDouble(input.trim());
    }

    public static void pause()
    {
	System.out.print("Press enter to continue...");
	try
	{
	    System.in.read();
	}
	catch (IOException e)
	{
	    e.printStackTrace();
	}
    }
    
    public static void timerBegin()
    {
	TIME = System.currentTimeMillis();
    }

    public static void timerStop()
    {
	long time = System.currentTimeMillis() - TIME;
	long sec = time/1000;
	double t = sec;
	String des = " Seconds";
	if(sec>60)
	{
	    t= sec/60.0;
	    des = " Minutes";
	}
	System.out.println("Time used:"+numberFormat.format(t)+des);
    }
}
