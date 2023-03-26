package net.wensc.mitemod.autobackupplayers.thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.ChatMessage;
import net.minecraft.EnumChatFormat;
import net.minecraft.server.MinecraftServer;

public class BackupThread extends Thread {
    private static final File backupFolder = new File("backup");
    private static final String backupFolderStr = (new File("backup")).getAbsolutePath();
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    private static BackupThread INSTANCE;
    private static long delay = 600000L;
    private static boolean isRunning;
    private final MinecraftServer server;
    private File worldFile;

    public BackupThread(File worldFile, MinecraftServer server) {
        this.setName("Backup Thread|备份线程");
        this.worldFile = worldFile;
        this.server = server;
        File file = new File("BackupPlayersDelay.cfg");

        try {
            if (!file.exists()) {
                if (file.createNewFile()) {
                    FileWriter writer = new FileWriter(file);
                    writer.write(String.valueOf(delay));
                    writer.close();
                } else {
                    server.getLogAgent().logSevere("无法创建备份间隔时间配置文件");
                }
            } else {
                Scanner scanner = new Scanner(new FileInputStream(file));
                String time = "";
                if (scanner.hasNextLine()) {
                    time = scanner.nextLine();
                    server.getLogAgent().logInfo("已将自动备份时间设置为" + time + "ms");
                }

                scanner.close();
                if (!time.equals("")) {
                    try {
                        delay = Long.parseLong(time);
                    } catch (NumberFormatException var7) {
                        server.getLogAgent().logSevereException("无法获取备份时间间隔,将备份时间间隔设为默认值(10分钟)", var7);
                        delay = 600000L;
                    }
                }
            }
        } catch (IOException var8) {
            server.getLogAgent().logSevereException("无法获取备份时间间隔,将备份时间间隔设为默认值(10分钟)", var8);
            delay = 600000L;
        }

    }

    private static void compress(File sourceFile, ZipOutputStream zos, String name, boolean KeepDirStructure) throws Exception {
        byte[] buf = new byte[1024];
        if (sourceFile.isFile()) {
            zos.putNextEntry(new ZipEntry(name));
            FileInputStream in = new FileInputStream(sourceFile);

            int len;
            while((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }

            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles != null && listFiles.length != 0) {
                File[] var11 = listFiles;
                int var7 = listFiles.length;

                for(int var8 = 0; var8 < var7; ++var8) {
                    File file = var11[var8];
                    if (KeepDirStructure) {
                        compress(file, zos, name + "/" + file.getName(), KeepDirStructure);
                    } else {
                        compress(file, zos, file.getName(), KeepDirStructure);
                    }
                }
            } else if (KeepDirStructure) {
                zos.putNextEntry(new ZipEntry(name + "/"));
                zos.closeEntry();
            }
        }

    }

    public static void setWorldDir(String dir, MinecraftServer server) {
        dir = dir + "//players";
        if (!isRunning) {
            INSTANCE = new BackupThread(new File(dir), server);
            INSTANCE.start();
        } else {
            INSTANCE.setWorldFile(new File(dir));
        }

    }

    public static void toZip(File srcDir, OutputStream out, boolean KeepDirStructure) throws RuntimeException {
        ZipOutputStream zos = null;

        try {
            zos = new ZipOutputStream(out);
            compress(srcDir, zos, srcDir.getName(), KeepDirStructure);
        } catch (Exception var12) {
            throw new RuntimeException("zip error from ZipUtils", var12);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException var11) {
                    var11.printStackTrace();
                }
            }

        }

    }

    public void run() {
        while(true) {
            try {
                if (!backupFolder.exists()) {
                    backupFolder.mkdir();
                }

                long start = System.currentTimeMillis();
                toZip(this.worldFile, new FileOutputStream(backupFolderStr + "//" + this.worldFile.getName() + "_" + format.format(new Date()) + ".zip"), true);
                long end = System.currentTimeMillis();
                long took = end - start;
                this.server.getLogAgent().logInfo("玩家存档 " + this.worldFile.getName() + " 压缩备份完成，耗时：" + took + " ms");
                this.sendChatMsgToAllPlayers(ChatMessage.createFromText("服务端玩家存档备份完成!耗时:" + took + "ms"));
                isRunning = true;
                Thread.sleep(Math.max(delay - 10000L, 10000L));
                this.sendChatMsgToAllPlayers(ChatMessage.createFromText("将在10秒后开始备份存档!可能会有些许卡顿").setColor(EnumChatFormat.values()[6]));
                Thread.sleep(10000L);
                this.sendChatMsgToAllPlayers(ChatMessage.createFromText("服务器开始备份...").setColor(EnumChatFormat.DARK_GREEN));
            } catch (NullPointerException | InterruptedException | FileNotFoundException var7) {
                var7.printStackTrace();
            }
        }
    }

    private void sendChatMsgToAllPlayers(ChatMessage msg) {
        this.server.getConfigurationManager().sendChatMsg(ChatMessage.createFromText("[Server] ").appendComponent(msg));
    }

    private void setWorldFile(File file) {
        this.worldFile = file;
    }
}
