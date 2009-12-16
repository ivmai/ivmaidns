/*
 * @(#) src/net/sf/ivmaidns/dnsx.java --
 * Internet DNS zones explorer (Experimental).
 **
 * Copyright (c) 1999-2001 Ivan Maidanski <ivmai@mail.ru>
 * All rights reserved.
 **
 * Used external libraries: Borland JBCL v3.
 * Tested with: JDK-1.2-V.
 */

/*
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 **
 * This software is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License (GPL) for more details.
 **
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 **
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module. An independent module is a module which is not derived from
 * or based on this library. If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

package net.sf.ivmaidns;

import java.io.IOException;
import java.io.EOFException;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.MenuShortcut;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import com.borland.jbcl.control.TreeControl;
import com.borland.jbcl.model.GraphLocation;
import com.borland.jbcl.model.LinkedTreeNode;
import com.borland.jbcl.view.TreeAdapter;
import com.borland.jbcl.view.TreeEvent;

import net.sf.ivmaidns.dns.DNSConnection;
import net.sf.ivmaidns.dns.DNSMsgHeader;
import net.sf.ivmaidns.dns.DNSName;
import net.sf.ivmaidns.dns.DNSRecord;

/**
 * Internet DNS zones explorer.
 **
 * @version 3.0
 * @author Ivan Maidanski
 */
public final class dnsx
{

 protected static int windowNumber, windowsCount;
 protected static InetAddress defaultNS;
 protected static final Image logoImage =
  Toolkit.getDefaultToolkit().getImage("");

 public static void main(String[] args)
 {
  try
  {
   defaultNS = InetAddress.getByName(args.length > 0 ?
    args[0] : "8.8.8.8");
  }
  catch (UnknownHostException e)
  {
   System.err.println(args.length > 0 ? "Name server unknown: " +
    args[0] : "Could not find default name server");
   System.exit(1);
  }
  UIManager.setLookAndFeel();
  new dnsx1("", null);
 }
}

class dnsx1 extends Frame
{

 final TextField domainField;
 final TreeControl dnsTree;
 final StatusBar statusBar;
 ThreadGroup loadersGroup;

 dnsx1(String topLevelDomain, Frame parent)
 {
  super("dnsx" + (++dnsx.windowNumber > 1 ?
   " [" + dnsx.windowNumber + "]" : ""), dnsx.logoImage);
  dnsx.windowsCount++;
  domainField =
   new TextField(topLevelDomain, "Top-level domain name");
  getContentPane().setLayout(new BorderLayout());
  setMenuBar(new MenuBar(new Menu[]
  {
   new Menu("File", 'F', new MenuItem[]
   {
    new MenuItem("New window", 'N', 'N', new ActionListener()
    {
     public void actionPerformed(ActionEvent e)
     {
      new dnsx1(domainField.getText(), dnsx1.this);
     }
    }),
    new MenuItem("-"),
    new MenuItem("Exit", 'x', new ActionListener()
    {
     public void actionPerformed(ActionEvent e)
     {
      exit();
     }
    }),
   }),
   new Menu("Help", 'H', new MenuItem[]
   {
    new MenuItem("About...", 'A', new ActionListener()
    {
     public void actionPerformed(ActionEvent e)
     {
      new dnsx2(dnsx1.this);
     }
    })
   })
  }, true));
  Panel domainPanel = new Panel(new BorderLayout(3, 5));
  domainPanel.add(new Label("Domain: ", Label.CENTER, 'D',
   domainField), BorderLayout.WEST);
  domainField.select(0, domainField.getText().length());
  domainField.addKeyListener(new KeyAdapter()
  {
   public void keyPressed(KeyEvent e)
   {
    if (e.getKeyCode() == KeyEvent.VK_ENTER)
     initLookUps();
   }
  });
  domainPanel.add(domainField, BorderLayout.CENTER);
  getContentPane().add(domainPanel, BorderLayout.NORTH);
  dnsTree = new TreeControl();
   /* FIXME: TreeControl (TreeView) throws NPE with J2SE v1.3+ */
  dnsTree.setEditInPlace(false);
  dnsTree.addTreeListener(new TreeAdapter()
  {
   public void nodeExpanded(TreeEvent e)
   {
    startLookUp(e.getLocation());
   }
  });
  getContentPane().add(dnsTree, BorderLayout.CENTER);
  statusBar = new StatusBar("Ready...");
  getContentPane().add(statusBar, BorderLayout.SOUTH);
  if (parent == null)
  {
   setSize(300, 400);
   setLocationRelativeTo(null);
  }
   else
   {
    setSize(parent.getSize());
    setLocation(parent.getLocation().x + 30,
     parent.getLocation().y + 40);
   }
  validate();
  setVisible(true);
 }

 protected void processWindowEvent(WindowEvent e)
 {
  super.processWindowEvent(e);
  if (e.getID() == WindowEvent.WINDOW_CLOSING)
   exit();
 }

 protected void exit()
 {
  if (--dnsx.windowsCount > 0)
   dispose();
   else System.exit(0);
 }

 protected void initLookUps()
 {
  try
  {
   DNSName rootName = new DNSName(domainField.getText(), null);
   if (!rootName.equals(dnsTree.get(dnsTree.getRoot())))
   {
    dnsTree.removeAllItems();
    dnsTree.setRoot(rootName);
    loadersGroup = new ThreadGroup(rootName.getAbsolute());
    (new Thread(loadersGroup, new dnsx3(rootName, dnsTree,
     statusBar), rootName.getAbsolute())).start();
   }
  }
  catch (IllegalArgumentException e)
  {
   statusBar.setText("Illegal name: " + domainField.getText());
  }
 }

 protected void startLookUp(GraphLocation location)
 {
  if (!"?".equals(dnsTree.get(((LinkedTreeNode)location).
      getFirstChild())))
   return;
  Thread[] list = new Thread[loadersGroup.activeCount()];
  loadersGroup.enumerate(list);
  String name = "";
  boolean next = false;
  do
  {
   if (next)
    name = name + ".";
   next = true;
   name = name + dnsTree.get(location).toString();
  } while ((location = location.getParent()) != null);
  for (int i = 0; i < list.length; i++)
   if (name.equals(list[i].getName()))
    return;
  (new Thread(loadersGroup, new dnsx3(new DNSName(name, null),
              dnsTree, statusBar), name)).start();
 }
}

class dnsx2 extends Dialog
{

 dnsx2(Frame parent)
 {
  super(parent, "About DNSX", dnsx.logoImage);
  getContentPane().setLayout(new BorderLayout());
  Panel titlePanel = new Panel(new BorderLayout());
  Panel logoSubPanel = new Panel();
  logoSubPanel.add(new ImageCanvas(dnsx.logoImage),
   BorderLayout.CENTER);
  Panel nameSubPanel = new Panel(new BorderLayout());
  Label nameLabel = new Label("DNSX v3.0");
  nameLabel.setFont(new Font("Dialog", 1, 14));
  nameSubPanel.add(nameLabel, BorderLayout.NORTH);
  nameSubPanel.add(new Label("Internet DNS Explorer"),
                   BorderLayout.CENTER);
  titlePanel.add(logoSubPanel, BorderLayout.WEST);
  titlePanel.add(nameSubPanel, BorderLayout.CENTER);
  getContentPane().add(titlePanel, BorderLayout.NORTH);
  Panel copyrightPanel = new Panel(new GridLayout(0, 1));
  copyrightPanel.setForeground(Color.blue);
  copyrightPanel.add(new Label(
   "Copyright\u00A9 2001 Ivan Maidanski,", Label.CENTER));
  copyrightPanel.add(new Label(
   "<ivmai@mail.ru>, http://ivmaidns.sf.net", Label.CENTER));
  getContentPane().add(copyrightPanel, BorderLayout.CENTER);
  Panel okPanel = new Panel();
  Button okButton = new Button("OK", "Close", 'O',
   new ActionListener()
   {
    public void actionPerformed(ActionEvent e)
    {
     dispose();
    }
   });
  okPanel.add(okButton, BorderLayout.CENTER);
  getContentPane().add(okPanel, BorderLayout.SOUTH);
  setDefaultButton(okButton);
  pack();
  setLocationRelativeTo(parent);
  setVisible(true);
 }
}

class dnsx3
 implements Runnable
{

 final DNSName domainName;
 final TreeControl dnsTree;
 final StatusBar statusBar;

 public dnsx3(DNSName domainName, TreeControl dnsTree,
         StatusBar statusBar)
 {
  this.domainName = domainName;
  this.dnsTree = dnsTree;
  this.statusBar = statusBar;
 }

 public synchronized void run()
 {
  byte[] msgBytes;
  DNSMsgHeader header;
  DNSRecord[] records = null;
  DNSName[] serversNames;
  DNSConnection connection = new DNSConnection();
  int count = 0;
  try
  {
   statusBar.setText("Resolving name servers for: " +
    domainName.getAbsolute());
   connection.open(dnsx.defaultNS);
   records = new DNSRecord[1];
   records[0] =
    new DNSRecord(domainName, DNSRecord.NS, DNSRecord.IN);
   connection.send(DNSConnection.encode(DNSMsgHeader.construct(
    DNSMsgHeader.QUERY, true, 1, 0, 0, 0, false), records));
   msgBytes = connection.receive(true);
   connection.close();
   if ((records = DNSConnection.decode(msgBytes)) == null ||
       !(header = new DNSMsgHeader(msgBytes)).isResponse() ||
       header.getQdCount() != 1 ||
       (count = header.getAnCount()) >= records.length)
    throw new IOException();
  }
  catch (IOException e)
  {
   connection.close();
   statusBar.setText("Could not resolve name server for: " +
    domainName.getAbsolute());
   return;
  }
  if (count == 0)
  {
   statusBar.setText("Error: Domain not found - " +
    domainName.getAbsolute());
   return;
  }
  serversNames = new DNSName[count];
  Object[] rData;
  for (int index = 0; index < count; index++)
   if (records[index + 1].getRType() == DNSRecord.NS &&
       (rData = records[index + 1].getRData()).length > 0)
    serversNames[index] = (DNSName)rData[0];
  statusBar.setText("Retriving zone file for: " +
   domainName.getAbsolute());
  for (int i = 0; i < count; i++)
  {
   if (serversNames[i] == null)
    continue;
   InetAddress address;
   try
   {
    address =
     InetAddress.getByName(serversNames[i].getRelativeAt(0));
   }
   catch (UnknownHostException e)
   {
    continue;
   }
   int cnt = 0;
   try
   {
    connection.open(address);
    records = new DNSRecord[1];
    records[0] =
     new DNSRecord(domainName, DNSRecord.AXFR, DNSRecord.IN);
    connection.send(DNSConnection.encode(DNSMsgHeader.construct(
     DNSMsgHeader.QUERY, false, 1, 0, 0, 0, false), records));
    msgBytes = connection.receive(true);
    if ((records = DNSConnection.decode(msgBytes)) == null ||
        !(header = new DNSMsgHeader(msgBytes)).isResponse() ||
        header.getAnCount() != 1 ||
        header.getQdCount() >= records.length)
     throw new IOException();
    DNSRecord record = records[header.getQdCount()];
    do
    {
     GraphLocation addedNode = addDomainName(record.getRName());
     cnt++;
     if (addedNode != null && record.getRType() == DNSRecord.NS &&
         addedNode.hasChildren() == 0)
      dnsTree.addChild(addedNode, "?");
     msgBytes = connection.receive(true);
     if ((records = DNSConnection.decode(msgBytes)) == null ||
         !(header = new DNSMsgHeader(msgBytes)).isResponse() ||
         header.getAnCount() != 1 ||
         header.getQdCount() >= records.length)
      throw new IOException();
     record = records[header.getQdCount()];
    } while (record.getRType() != DNSRecord.SOA);
    GraphLocation maskNode =
     ((LinkedTreeNode)addDomainName(domainName)).getFirstChild();
    if (maskNode != null && "?".equals(dnsTree.get(maskNode)))
     dnsTree.remove(maskNode);
    statusBar.setText("Zone transfer finished: " +
     domainName.getAbsolute() + " (" + (cnt - 1) + " records)");
    connection.close();
    return;
   }
   catch (IOException e)
   {
    connection.close();
    statusBar.setText(serversNames[i] +
     ": Failed. Trying next server");
   }
  }
  statusBar.setText("Error: Zone transfer impossible - " +
   domainName.getAbsolute());
 }

 protected synchronized GraphLocation addDomainName(
            DNSName domainName)
 {
  LinkedTreeNode parent = (LinkedTreeNode)dnsTree.getRoot();
  DNSName rootName = (DNSName)dnsTree.get(parent);
  if (rootName == null || !domainName.isInDomain(rootName, false))
   return null;
  int level = domainName.getLevel();
  int rootLevel = rootName.getLevel();
  while (rootLevel < level)
  {
   int r = -1;
   String name = domainName.getLabelAt(rootLevel++);
   String lowerCasedName = name.toLowerCase();
   LinkedTreeNode child = parent.getFirstChild();
   while (child != null && (r = lowerCasedName.compareTo(
          ((String)dnsTree.get(child)).toLowerCase())) > 0)
    child = child.getNextSibling();
   parent = r != 0 ? (LinkedTreeNode)dnsTree.addChild(parent,
    child, name) : child;
  }
  return parent;
 }
}

class ImageCanvas extends Canvas
{

 protected Image image;

 public ImageCanvas()
 {
  super();
  setSize(0, 0);
  setVisible(true);
 }

 public ImageCanvas(Image img)
 {
  this();
  setImage(img);
 }

 public ImageCanvas(String imageName)
 {
  this();
  setImage(imageName);
 }

 public synchronized void setImage(Image img)
 {
  if (image != img)
  {
   image = img;
   if (image != null)
   {
    if (image.getWidth(this) >= 0 && image.getHeight(this) >= 0)
     setSize(image.getWidth(null), image.getHeight(null));
    if (prepareImage(image, this))
     repaint();
   }
    else setSize(0, 0);
  }
 }

 public void setImage(String imageName)
 {
  setImage(Toolkit.getDefaultToolkit().getImage(imageName));
 }

 public final Image getImage()
 {
  return image;
 }

 public synchronized boolean imageUpdate(Image img, int flags,
                                         int x, int y, int w, int h)
 {
  if ((flags & (WIDTH | HEIGHT)) == (WIDTH | HEIGHT))
  {
   setSize(w, h);
   return super.imageUpdate(img, flags, x, y, w, h);
  }
   else
   {
    super.imageUpdate(img, flags, x, y, w, h);
    return (flags & ABORT) == 0;
   }
 }

 public synchronized void paint(Graphics g)
 {
  super.paint(g);
  if (image != null)
   g.drawImage(image, 0, 0, null);
 }
}

class Button extends java.awt.Button
{

 public Button()
 {
  super();
 }

 public Button(String label)
 {
  super(label);
 }

 public Button(String label, String toolTipText)
 {
  super(label);
  setToolTipText(toolTipText);
 }

 public Button(String label, String toolTipText,
               char mnemonic, ActionListener actionListener)
 {
  super(label);
  setToolTipText(toolTipText);
  setMnemonic(mnemonic);
  addActionListener(actionListener);
 }

 public void setText(String label)
 {
  setLabel(label);
 }

 public String getText()
 {
  return getLabel();
 }

 public void setToolTipText(String text) {}

 public void setMnemonic(char mnemonic) {}
}

class Dialog extends java.awt.Dialog
{

 public Dialog(Frame parent)
 {
  this(parent, "", null);
 }

 public Dialog(Frame parent, String title)
 {
  this(parent, title, null);
 }

 public Dialog(Frame parent, String title, Image icon)
 {
  super(parent, title, true);
  setResizable(false);
  enableEvents(AWTEvent.WINDOW_EVENT_MASK);
 }

 public void setDefaultButton(Button defaultButton) {}

 public void update(Graphics g)
 {
  paint(g);
 }

 protected void processWindowEvent(WindowEvent e)
 {
  super.processWindowEvent(e);
  if (e.getID() == WindowEvent.WINDOW_CLOSING)
  {
   setVisible(false);
   dispose();
  }
 }

 public Container getContentPane()
 {
  return this;
 }

 public synchronized void setLocationRelativeTo(Component c)
 {
  Container root = null;
  if (c instanceof Window || c instanceof java.applet.Applet)
   root = (Container)c;
   else if (c != null)
    for (Container parent = c.getParent();
         parent != null; parent = parent.getParent())
     if (parent instanceof Window ||
         parent instanceof java.applet.Applet)
     {
      root = parent;
      break;
     }
  if (c != null && !c.isShowing() || root == null ||
      !root.isShowing())
  {
   Dimension paneSize = getSize();
   Dimension screenSize = getToolkit().getScreenSize();
   setLocation((screenSize.width - paneSize.width) / 2,
               (screenSize.height - paneSize.height) / 2);
  }
   else
   {
    Dimension invokerSize = c.getSize();
    Point invokerScreenLocation = c.getLocationOnScreen();
    Rectangle dialogBounds = getBounds();
    int dx = invokerScreenLocation.x +
             ((invokerSize.width - dialogBounds.width) >> 1);
    int dy = invokerScreenLocation.y +
             ((invokerSize.height - dialogBounds.height) >> 1);
    Dimension ss = getToolkit().getScreenSize();
    if (dy + dialogBounds.height > ss.height)
    {
     dy = ss.height - dialogBounds.height;
     dx = invokerScreenLocation.x < (ss.width >> 1) ?
          invokerScreenLocation.x + invokerSize.width :
          invokerScreenLocation.x - dialogBounds.width;
    }
    if (dx + dialogBounds.width > ss.width)
     dx = ss.width - dialogBounds.width;
    setLocation(Math.max(dx, 0), Math.max(dy, 0));
   }
 }
}

class Frame extends java.awt.Frame
{

 public Frame()
 {
  this("");
 }

 public Frame(String title)
 {
  super(title);
  enableEvents(AWTEvent.WINDOW_EVENT_MASK);
 }

 public Frame(String title, Image icon)
 {
  this(title);
  setIconImage(icon);
 }

 public void update(Graphics g)
 {
  paint(g);
 }

 public void setDefaultButton(Button defaultButton) {}

 protected void processWindowEvent(WindowEvent e)
 {
  super.processWindowEvent(e);
  if (e.getID() == WindowEvent.WINDOW_CLOSING)
  {
   setVisible(false);
   dispose();
  }
 }

 public Container getContentPane()
 {
  return this;
 }

 public synchronized void setLocationRelativeTo(Component c)
 {
  Container root = null;
  if (c instanceof Window || c instanceof java.applet.Applet)
   root = (Container)c;
   else if (c != null)
    for (Container parent = c.getParent();
         parent != null; parent = parent.getParent())
     if (parent instanceof Window ||
         parent instanceof java.applet.Applet)
     {
      root = parent;
      break;
     }
  if (c != null && !c.isShowing() || root == null ||
      !root.isShowing())
  {
   Dimension paneSize = getSize();
   Dimension screenSize = getToolkit().getScreenSize();
   setLocation((screenSize.width - paneSize.width) / 2,
               (screenSize.height - paneSize.height) / 2);
  }
   else
   {
    Dimension invokerSize = c.getSize();
    Point invokerScreenLocation = c.getLocationOnScreen();
    Rectangle dialogBounds = getBounds();
    int dx = invokerScreenLocation.x +
             ((invokerSize.width - dialogBounds.width) >> 1);
    int dy = invokerScreenLocation.y +
             ((invokerSize.height - dialogBounds.height) >> 1);
    Dimension ss = getToolkit().getScreenSize();
    if (dy + dialogBounds.height > ss.height)
    {
     dy = ss.height - dialogBounds.height;
     dx = invokerScreenLocation.x < (ss.width >> 1) ?
          invokerScreenLocation.x + invokerSize.width :
          invokerScreenLocation.x - dialogBounds.width;
    }
    if (dx + dialogBounds.width > ss.width)
     dx = ss.width - dialogBounds.width;
    setLocation(Math.max(dx, 0), Math.max(dy, 0));
   }
 }
}

class Label extends java.awt.Label
{

 public static final int TOP = 3;

 public static final int BOTTOM = 4;

 public Label()
 {
  super();
 }

 public Label(String label)
 {
  super(label);
 }

 public Label(String label, int hAlignment)
 {
  super(label, hAlignment);
 }

 public Label(String label, int hAlignment, String toolTipText)
 {
  super(label, hAlignment);
  setToolTipText(toolTipText);
 }

 public Label(String label, int hAlignment,
              char mnemonic, Component labelForComponent)
 {
  super(label, hAlignment);
  setDisplayedMnemonic(mnemonic);
  setLabelFor(labelForComponent);
 }

 public Label(String label, int hAlignment, String toolTipText,
              char mnemonic, Component labelForComponent)
 {
  this(label, hAlignment, mnemonic, labelForComponent);
  setToolTipText(toolTipText);
 }

 public void setHorizontalAlignment(int hAlignment)
 {
  setAlignment(hAlignment);
 }

 public void setVerticalAlignment(int vAlignment)
 {
  if (vAlignment != TOP && vAlignment != CENTER &&
      vAlignment != BOTTOM)
   throw new IllegalArgumentException("improper alignment: " +
              vAlignment);
 }

 public void setToolTipText(String text) {}

 public void setDisplayedMnemonic(char mnemonic) {}

 public void setLabelFor(Component labelForComponent) {}
}

class Menu extends java.awt.Menu
{

 public Menu()
 {
  this("", '\0');
 }

 public Menu(String label)
 {
  this(label, '\0');
 }

 public Menu(String label, char mnemonic)
 {
  super(label);
  setMnemonic(mnemonic);
 }

 public Menu(String label, char mnemonic, MenuItem[] menuItems)
 {
  this(label, mnemonic);
  if (menuItems != null)
   for (int i = 0; i < menuItems.length; i++)
    add(menuItems[i]);
 }

 public void setMnemonic(char mnemonic) {}
}

class MenuBar extends java.awt.MenuBar
{

 public MenuBar()
 {
  super();
 }

 public MenuBar(Menu[] menus)
 {
  this(menus, false);
 }

 public MenuBar(Menu[] menus, boolean containsHelpMenu)
 {
  super();
  if (menus != null)
  {
   for (int i = 0; i < menus.length; i++)
    add(menus[i]);
   if (containsHelpMenu && menus.length > 0)
    setHelpMenu(menus[menus.length - 1]);
  }
 }
}

class MenuItem extends java.awt.MenuItem
{

 public MenuItem()
 {
  this("", '\0');
 }

 public MenuItem(String label)
 {
  this(label, '\0');
 }

 public MenuItem(String label, char mnemonic)
 {
  super(label);
  setMnemonic(mnemonic);
 }

 public MenuItem(String label, char mnemonic, int key)
 {
  this(label, mnemonic, key, false);
 }

 public MenuItem(String label, char mnemonic, int key,
                 boolean useShift)
 {
  super(label, new MenuShortcut(key, useShift));
  setMnemonic(mnemonic);
 }

 public MenuItem(String label, char mnemonic,
                 ActionListener actionListener)
 {
  this(label, mnemonic);
  addActionListener(actionListener);
 }

 public MenuItem(String label, char mnemonic, int key,
                 ActionListener actionListener)
 {
  this(label, mnemonic, key, false);
  addActionListener(actionListener);
 }

 public MenuItem(String label, char mnemonic, int key,
                 boolean useShift, ActionListener actionListener)
 {
  this(label, mnemonic, key, useShift);
  addActionListener(actionListener);
 }

 public void setMnemonic(char mnemonic) {}

 public void setAccelerator(int key)
 {
  setAccelerator(key, false);
 }

 public void setAccelerator(int key, boolean useShift)
 {
  setShortcut(new MenuShortcut(key, useShift));
 }
}

class Panel extends java.awt.Panel
{

 public Panel()
 {
  this(new FlowLayout());
 }

 public Panel(LayoutManager layout)
 {
  super(layout);
  setBackground(Color.lightGray);
 }

 public Panel(LayoutManager layout, String toolTipText)
 {
  this(layout);
  setToolTipText(toolTipText);
 }

 public void setToolTipText(String text) {}
}

class StatusBar extends Panel
{

 protected Label label;

 public StatusBar()
 {
  this("");
 }

 public StatusBar(String text)
 {
  super(new BorderLayout());
  setBackground(Color.pink);
  label = new Label(text);
  add(label, BorderLayout.CENTER);
 }

 public StatusBar(String text, String toolTipText)
 {
  this(text);
  setToolTipText(toolTipText);
 }

 public void setText(String text)
 {
  label.setText(text);
 }

 public String getText()
 {
  return label.getText();
 }

 public void setToolTipText(String text)
 {
  label.setToolTipText(text);
 }
}

class TextField extends java.awt.TextField
{

 public TextField()
 {
  super();
 }

 public TextField(int columns)
 {
  super(columns);
 }

 public TextField(String text)
 {
  super(text);
 }

 public TextField(String text, String toolTipText)
 {
  super(text);
  setToolTipText(toolTipText);
 }

 public TextField(String text, int columns)
 {
  super(text, columns);
 }

 public TextField(String text, int columns, String toolTipText)
 {
  super(text, columns);
  setToolTipText(toolTipText);
 }

 public void setToolTipText(String text) {}
}

class UIManager
{

 public static void setLookAndFeel()
 {
  setLookAndFeel((Component)null);
 }

 public static void setLookAndFeel(Component c)
 {
  setLookAndFeel(getSystemLookAndFeelClassName(), c);
 }

 public static void setLookAndFeel(int lnfIndex, Component c)
 {
  setLookAndFeel(getLNFClassName(lnfIndex), c);
 }

 public static void setLookAndFeel(String lnfClassName,
         Component c) {}

 public static int getLNFCount()
 {
  return 0;
 }

 public static String getLNFName(int lnfIndex)
  throws IndexOutOfBoundsException
 {
  throw new IndexOutOfBoundsException();
 }

 public static String getLNFClassName(int lnfIndex)
  throws IndexOutOfBoundsException
 {
  throw new IndexOutOfBoundsException();
 }

 public static String getCurrentLookAndFeelClassName()
 {
  return null;
 }

 public static String getCrossPlatformLookAndFeelClassName()
 {
  return null;
 }

 public static String getSystemLookAndFeelClassName()
 {
  return null;
 }
}

/*
class Button extends javax.swing.JButton
{

 public Button()
 {
  this("", null);
 }

 public Button(String text)
 {
  this(text, null);
 }

 public Button(String text, String toolTipText)
 {
  super(text);
  setToolTipText(toolTipText);
 }

 public Button(String text, String toolTipText,
               char mnemonic, ActionListener actionListener)
 {
  this(text, toolTipText);
  setMnemonic(mnemonic);
  addActionListener(actionListener);
 }
}

class Dialog extends javax.swing.JDialog
{

 public Dialog(Frame parent)
 {
  this(parent, "", null);
 }

 public Dialog(Frame parent, String title)
 {
  this(parent, title, null);
 }

 public Dialog(Frame parent, String title, Image icon)
 {
  super(parent, title, true);
  setResizable(false);
  setDefaultCloseOperation(DISPOSE_ON_CLOSE);
 }

 public void setDefaultButton(Button defaultButton)
 {
  getRootPane().setDefaultButton(defaultButton);
 }
}

class Frame extends javax.swing.JFrame
{

 public Frame()
 {
  this("");
 }

 public Frame(String title)
 {
  super(title);
  setDefaultCloseOperation(DISPOSE_ON_CLOSE);
 }

 public Frame(String title, Image icon)
 {
  this(title);
  setIconImage(icon);
 }

 public void setMenuBar(MenuBar menuBar)
 {
  setJMenuBar(menuBar);
 }

 public void setDefaultButton(Button defaultButton)
 {
  getRootPane().setDefaultButton(defaultButton);
 }

 public synchronized void setLocationRelativeTo(Component c)
 {
  Container root = null;
  if (c instanceof Window || c instanceof java.applet.Applet)
   root = (Container)c;
   else if (c != null)
    for (Container parent = c.getParent();
         parent != null; parent = parent.getParent())
     if (parent instanceof Window ||
         parent instanceof java.applet.Applet)
     {
      root = parent;
      break;
     }
  if (c != null && !c.isShowing() || root == null ||
      !root.isShowing())
  {
   Dimension paneSize = getSize();
   Dimension screenSize = getToolkit().getScreenSize();
   setLocation((screenSize.width - paneSize.width) / 2,
               (screenSize.height - paneSize.height) / 2);
  }
   else
   {
    Dimension invokerSize = c.getSize();
    Point invokerScreenLocation = c.getLocationOnScreen();
    Rectangle dialogBounds = getBounds();
    int dx = invokerScreenLocation.x +
             ((invokerSize.width - dialogBounds.width) >> 1);
    int dy = invokerScreenLocation.y +
             ((invokerSize.height - dialogBounds.height) >> 1);
    Dimension ss = getToolkit().getScreenSize();
    if (dy + dialogBounds.height > ss.height)
    {
     dy = ss.height - dialogBounds.height;
     dx = invokerScreenLocation.x < (ss.width >> 1) ?
          invokerScreenLocation.x + invokerSize.width :
          invokerScreenLocation.x - dialogBounds.width;
    }
    if (dx + dialogBounds.width > ss.width)
     dx = ss.width - dialogBounds.width;
    setLocation(Math.max(dx, 0), Math.max(dy, 0));
   }
 }
}

class Label extends javax.swing.JLabel
{

 public Label()
 {
  super();
 }

 public Label(String label)
 {
  super(label);
 }

 public Label(String label, int hAlignment)
 {
  super(label, hAlignment);
 }

 public Label(String label, int hAlignment, String toolTipText)
 {
  super(label, hAlignment);
  setToolTipText(toolTipText);
 }

 public Label(String label, int hAlignment,
              char mnemonic, Component labelForComponent)
 {
  super(label, hAlignment);
  setDisplayedMnemonic(mnemonic);
  setLabelFor(labelForComponent);
 }

 public Label(String label, int hAlignment, String toolTipText,
              char mnemonic, Component labelForComponent)
 {
  this(label, hAlignment, mnemonic, labelForComponent);
  setToolTipText(toolTipText);
 }
}

class Menu extends javax.swing.JMenu
{

 public Menu()
 {
  this("", '\0');
 }

 public Menu(String label)
 {
  this(label, '\0');
 }

 public Menu(String label, char mnemonic)
 {
  super(label);
  setMnemonic(mnemonic);
 }

 public Menu(String label, char mnemonic, MenuItem[] menuItems)
 {
  this(label, mnemonic);
  if (menuItems != null)
   for (int i = 0; i < menuItems.length; i++)
    add(menuItems[i]);
 }

 public MenuItem add(MenuItem menuItem)
 {
  if (menuItem != null && "-".equals(menuItem.getText()))
   super.add(new javax.swing.JSeparator());
   else super.add(menuItem);
  return menuItem;
 }
}

class MenuBar extends javax.swing.JMenuBar
{

 public MenuBar()
 {
  super();
 }

 public MenuBar(Menu[] menus)
 {
  this(menus, false);
 }

 public MenuBar(Menu[] menus, boolean containsHelpMenu)
 {
  super();
  if (menus != null)
  {
   for (int i = 0; i < menus.length; i++)
    add(menus[i]);
   if (containsHelpMenu && menus.length > 0)
    setHelpMenu(menus[menus.length - 1]);
  }
 }

 public void setHelpMenu(Menu menu) {}
}

class MenuItem extends javax.swing.JMenuItem
{

 public MenuItem()
 {
  this("", '\0');
 }

 public MenuItem(String label)
 {
  this(label, '\0');
 }

 public MenuItem(String label, char mnemonic)
 {
  super(label);
  setMnemonic(mnemonic);
 }

 public MenuItem(String label, char mnemonic, int key)
 {
  this(label, mnemonic);
  setAccelerator(key);
 }

 public MenuItem(String label, char mnemonic, int key,
                 boolean useShift)
 {
  this(label, mnemonic);
  setAccelerator(key, useShift);
 }

 public MenuItem(String label, char mnemonic,
                 ActionListener actionListener)
 {
  this(label, mnemonic);
  addActionListener(actionListener);
 }

 public MenuItem(String label, char mnemonic, int key,
                 ActionListener actionListener)
 {
  this(label, mnemonic, key);
  addActionListener(actionListener);
 }

 public MenuItem(String label, char mnemonic, int key,
                 boolean useShift, ActionListener actionListener)
 {
  this(label, mnemonic, key, useShift);
  addActionListener(actionListener);
 }

 public void setAccelerator(int key)
 {
  setAccelerator(key, false);
 }

 public void setAccelerator(int key, boolean useShift)
 {
  setAccelerator(javax.swing.KeyStroke.getKeyStroke(key,
   Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() |
   (useShift ? Event.SHIFT_MASK : 0)));
 }
}

class Panel extends javax.swing.JPanel
{

 public Panel()
 {
  super();
 }

 public Panel(LayoutManager layout)
 {
  super(layout);
 }

 public Panel(LayoutManager layout, String toolTipText)
 {
  super(layout);
  setToolTipText(toolTipText);
 }
}

class StatusBar extends Panel
{

 protected Label label;

 public StatusBar()
 {
  this("");
 }

 public StatusBar(String text)
 {
  super(new BorderLayout());
  setBackground(Color.pink);
  label = new Label(text);
  add(label, BorderLayout.CENTER);
 }

 public StatusBar(String text, String toolTipText)
 {
  this(text);
  setToolTipText(toolTipText);
 }

 public void setText(String text)
 {
  label.setText(text);
 }

 public String getText()
 {
  return label.getText();
 }

 public void setToolTipText(String text)
 {
  label.setToolTipText(text);
 }
}

class TextField extends javax.swing.JTextField
{

 public TextField()
 {
  super();
 }

 public TextField(int columns)
 {
  super(columns);
 }

 public TextField(String text)
 {
  super(text);
 }

 public TextField(String text, String toolTipText)
 {
  super(text);
  setToolTipText(toolTipText);
 }

 public TextField(String text, int columns)
 {
  super(text, columns);
 }

 public TextField(String text, int columns, String toolTipText)
 {
  super(text, columns);
  setToolTipText(toolTipText);
 }
}

class UIManager extends javax.swing.UIManager
{

 public static void setLookAndFeel()
 {
  setLookAndFeel((Component)null);
 }

 public static void setLookAndFeel(Component c)
 {
  setLookAndFeel(getSystemLookAndFeelClassName(), c);
 }

 public static void setLookAndFeel(int lnfIndex, Component c)
 {
  setLookAndFeel(getLNFClassName(lnfIndex), c);
 }

 public static void setLookAndFeel(String lnfClassName, Component c)
 {
  try
  {
   setLookAndFeel(lnfClassName);
   if (c != null)
    javax.swing.SwingUtilities.updateComponentTreeUI(c);
  }
  catch (Exception e)
  {
   System.err.println("Could not load LookAndFeel: " +
                      lnfClassName);
  }
 }

 public static int getLNFCount()
 {
  return UIManager.getInstalledLookAndFeels().length;
 }

 public static String getLNFName(int lnfIndex)
  throws IndexOutOfBoundsException
 {
  return UIManager.getInstalledLookAndFeels()[lnfIndex].getName();
 }

 public static String getLNFClassName(int lnfIndex)
  throws IndexOutOfBoundsException
 {
  return
   UIManager.getInstalledLookAndFeels()[lnfIndex].getClassName();
 }

 public static String getCurrentLookAndFeelClassName()
 {
  return getLookAndFeel().getClass().getName();
 }
}
*/

/*
class Panel extends com.borland.jbcl.control.BevelPanel
{

 public Panel()
 {
  this(new FlowLayout());
 }

 public Panel(LayoutManager layout)
 {
  super();
  setLayout(layout);
 }

 public Panel(LayoutManager layout, String toolTipText)
 {
  this(layout);
  setToolTipText(toolTipText);
 }
}

class StatusBar extends com.borland.jbcl.control.StatusBar
{

 public StatusBar()
 {
  super();
 }

 public StatusBar(String text)
 {
  super();
  setText(text);
 }

 public StatusBar(String text, String toolTipText)
 {
  super();
  setText(text);
  setToolTipText(toolTipText);
 }
}

class TextField extends com.borland.jbcl.control.TextFieldControl
{

 public TextField()
 {
  super();
 }

 public TextField(int columns)
 {
  super();
  setColumns(columns);
 }

 public TextField(String text)
 {
  super();
  setText(text);
 }

 public TextField(String text, String toolTipText)
 {
  this(text);
  setToolTipText(toolTipText);
 }

 public TextField(String text, int columns)
 {
  super();
  setText(text);
  setColumns(columns);
 }

 public TextField(String text, int columns, String toolTipText)
 {
  this(text, columns);
  setToolTipText(toolTipText);
 }

 public void setToolTipText(String text) {}
}
*/
