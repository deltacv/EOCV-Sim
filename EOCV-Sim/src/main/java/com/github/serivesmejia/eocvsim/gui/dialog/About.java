/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.gui.dialog;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.Icons;
import com.github.serivesmejia.eocvsim.gui.Visualizer;
import com.github.serivesmejia.eocvsim.gui.component.ImageX;
import com.github.serivesmejia.eocvsim.gui.util.GuiUtil;
import com.github.serivesmejia.eocvsim.util.StrUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class About {

    public JDialog about = null;

    public static ListModel<String> CONTRIBS_LIST_MODEL;
    public static ListModel<String> OSL_LIST_MODEL;

    static {
        try {
            CONTRIBS_LIST_MODEL = GuiUtil.isToListModel(About.class.getResourceAsStream("/contributors.txt"), StandardCharsets.UTF_8);
            OSL_LIST_MODEL = GuiUtil.isToListModel(About.class.getResourceAsStream("/opensourcelibs.txt"), StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public About(JFrame parent, EOCVSim eocvSim) {

        about = new JDialog(parent);

        eocvSim.visualizer.childDialogs.add(about);
        initAbout();

    }

    private void initAbout() {

        about.setModal(true);

        about.setTitle("About");

        JPanel contents = new JPanel(new GridLayout(2, 1));
        contents.setAlignmentX(Component.CENTER_ALIGNMENT);

        ImageX icon = new ImageX(Icons.INSTANCE.getImage("ico_eocvsim"));
        icon.setSize(50, 50);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        icon.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel appInfo = new JLabel("EasyOpenCV Simulator v" + EOCVSim.VERSION);
        appInfo.setFont(appInfo.getFont().deriveFont(appInfo.getFont().getStyle() | Font.BOLD)); //set font to bold

        JPanel appInfoLogo = new JPanel(new FlowLayout());

        appInfoLogo.add(icon);
        appInfoLogo.add(appInfo);

        appInfoLogo.setBorder(BorderFactory.createEmptyBorder(10, 10, -30, 10));

        contents.add(appInfoLogo);

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel contributors = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JList<String> contribsList = new JList<>();
        contribsList.setModel(CONTRIBS_LIST_MODEL);
        contribsList.setSelectionModel(new GuiUtil.NoSelectionModel());
        contribsList.setLayout(new FlowLayout(FlowLayout.CENTER));
        contribsList.setAlignmentY(Component.TOP_ALIGNMENT);

        contribsList.setVisibleRowCount(4);

        JPanel contributorsList = new JPanel(new FlowLayout(FlowLayout.CENTER));
        contributorsList.setAlignmentY(Component.TOP_ALIGNMENT);

        JScrollPane contribsListScroll = new JScrollPane();
        contribsListScroll.setBorder(new EmptyBorder(0,0,20,10));
        contribsListScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        contribsListScroll.setAlignmentY(Component.TOP_ALIGNMENT);
        contribsListScroll.setViewportView(contribsList);

        contributors.setAlignmentY(Component.TOP_ALIGNMENT);
        contents.setAlignmentY(Component.TOP_ALIGNMENT);

        contributorsList.add(contribsListScroll);
        contributors.add(contributorsList);

        tabbedPane.addTab("Contributors", contributors);

        JPanel osLibs = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JList<String> osLibsList = new JList<>();
        osLibsList.setModel(OSL_LIST_MODEL);
        osLibsList.setLayout(new FlowLayout(FlowLayout.CENTER));
        osLibsList.setAlignmentY(Component.TOP_ALIGNMENT);

        osLibsList.setVisibleRowCount(4);

        osLibsList.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {

                String text = osLibsList.getModel().getElementAt(osLibsList.getSelectedIndex());
                String[] urls = StrUtil.findUrlsInString(text);

                if(urls.length > 0) {
                    try {
                        Desktop.getDesktop().browse(new URI(urls[0]));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                osLibsList.clearSelection();

            }
        });

        JPanel osLibsListPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        osLibsList.setAlignmentY(Component.TOP_ALIGNMENT);

        JScrollPane osLibsListScroll = new JScrollPane();
        osLibsListScroll.setBorder(new EmptyBorder(0,0,20,10));
        osLibsListScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        osLibsListScroll.setAlignmentY(Component.TOP_ALIGNMENT);
        osLibsListScroll.setViewportView(osLibsList);

        osLibs.setAlignmentY(Component.TOP_ALIGNMENT);

        osLibsListPane.add(osLibsListScroll);
        osLibs.add(osLibsListPane);

        tabbedPane.addTab("Open Source Libraries", osLibs);

        contents.add(tabbedPane);

        contents.setBorder(new EmptyBorder(10,10,10,10));

        about.add(contents);

        about.pack();
        about.setLocationRelativeTo(null);
        about.setResizable(false);
        about.setVisible(true);
    }

}
