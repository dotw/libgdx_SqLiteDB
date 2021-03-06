/*
 * Copyright (C) 2017 team-cachebox.de
 *
 * Licensed under the : GNU General  License (GPL);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.longri.gdx.sqlite;


import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.jnigen.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * Created by Longri on 18.12.2017.
 */
public class SQLiteBuild {

    public static void main(String[] args) throws Exception {

        CommandLine cmd = getCommandLine(args);


        FileDescriptor targetDescriptor = new FileDescriptor("libs");
        FileDescriptor jniDescriptor = new FileDescriptor("jni");
        FileDescriptor buildDescriptor = new FileDescriptor("../GdxSqlite/build/classes/main");
        FileDescriptor sqliteDescriptor = new FileDescriptor("sqlite_src");

        File jniPath = jniDescriptor.file().getAbsoluteFile();
        String jniPathString = jniPath.getAbsolutePath();


        File sqlitePath = sqliteDescriptor.file().getAbsoluteFile();
        String sqlitePathString = sqlitePath.getAbsolutePath();

        File buildPath = buildDescriptor.file().getAbsoluteFile();
        String buildPathString = buildPath.getAbsolutePath();


        //cleanup
        jniDescriptor.deleteDirectory();
//        targetDescriptor.deleteDirectory();


        String cFlags = " -DSQLITE_ENABLE_API_ARMOR" +
                " -DSQLITE_ENABLE_FTS3" +
                " -DSQLITE_ENABLE_FTS3_PARENTHESIS" +
                " -DSQLITE_ENABLE_RTREE" +
                " -DSQLITE_OMIT_AUTORESET" +
                " -DSQLITE_OMIT_LOAD_EXTENSION" +
                " -DSQLITE_SYSTEM_MALLOC" +
                " -DSQLITE_THREADSAFE=2";


        String[] headers = new String[]{sqlitePathString};

        // generate native code
        new NativeCodeGenerator().generate("../GdxSqlite/src", buildPathString, jniPathString);


        //copy c/c++ src to 'jni' folder
        for (String headerPath : headers) {
            FileDescriptor fd = new FileDescriptor(headerPath);
            FileDescriptor[] list = fd.list();
            for (FileDescriptor descriptor : list) {
                descriptor.copyTo(jniDescriptor.child(descriptor.name()));
            }
        }

        //generate build scripts
        boolean all = cmd.hasOption("all");
        Array<BuildTarget> targets = new Array<>();


        if (all || cmd.hasOption("win64")) {
            BuildTarget win64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Windows, true);
//            win64.compilerSuffix = ".exe";
            win64.headerDirs = headers;
            win64.cFlags += cFlags;
//            win64.cppFlags += " -fpermissive";
            targets.add(win64);
        }

        if (all || cmd.hasOption("win32")) {
            BuildTarget win32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Windows, false);
            win32.compilerPrefix = "";
            win32.compilerSuffix = "";
            win32.headerDirs = headers;
            win32.cFlags += cFlags;
            targets.add(win32);
        }

        BuildTarget mac64 = null;
        if (all || cmd.hasOption("mac64")) {
            mac64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.MacOsX, true);
            mac64.compilerPrefix = "";
            mac64.compilerSuffix = "";
            mac64.headerDirs = headers;
            mac64.cFlags += cFlags;
            targets.add(mac64);
        }


        if (all || cmd.hasOption("mac32")) {
            BuildTarget mac32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.MacOsX, false);
            mac32.compilerPrefix = "";
            mac32.compilerSuffix = "";
            mac32.headerDirs = headers;
            mac32.cFlags += cFlags;
            targets.add(mac32);
        }


        if (all || cmd.hasOption("ios32")) {
            BuildTarget ios32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.IOS, false);
            ios32.compilerPrefix = "";
            ios32.compilerSuffix = "";
            ios32.headerDirs = headers;
            ios32.cppFlags += " -stdlib=libc++";
            ios32.cExcludes = new String[]{"shell.c", "sqlite3.c"};

            targets.add(ios32);
        }

        if (all || cmd.hasOption("linux32")) {
            BuildTarget linux32 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Linux, false);
            linux32.compilerPrefix = "";
            linux32.compilerSuffix = "";
            linux32.headerDirs = headers;
//            linux32.linkerFlags="-shared -m32 -z execstack";
            linux32.cFlags += cFlags;
            targets.add(linux32);
        }

        if (all || cmd.hasOption("linux64")) {
            BuildTarget linux64 = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Linux, true);
            linux64.headerDirs = headers;
            linux64.linkerFlags = "-shared -m64 -z noexecstack";
            linux64.cFlags += cFlags;
            linux64.cExcludes = new String[]{"shell.c"};
            targets.add(linux64);
        }

        if (all || cmd.hasOption("android")) {
            BuildTarget android = BuildTarget.newDefaultTarget(BuildTarget.TargetOs.Android, false);
            android.headerDirs = headers;
            android.cFlags += cFlags;

            if (System.getProperty("os.name").startsWith("Windows")) {
                android.ndkHome = "C:/android-ndk-r16b";
                android.ndkSuffix = ".cmd";
            } else {
                android.ndkHome = "/Volumes/HDD_DATA/android-ndk-r16b";
            }

            targets.add(android);
        }


        BuildConfig config = new BuildConfig("GdxSqlite");
        new AntScriptGenerator().generate(config, targets);

        FileDescriptor projectPath = new FileDescriptor("../");
        FileDescriptor buildLibsPath = projectPath.child("GdxSqliteBuild/libs");



        //delete outdated files
        buildLibsPath.deleteDirectory();


        if (all || cmd.hasOption("linux32"))
            BuildExecutor.executeAnt("build-linux32.xml", "-v", jniPath);
        if (all || cmd.hasOption("linux64"))
            BuildExecutor.executeAnt("build-linux64.xml", "-v", jniPath);
        if (all || cmd.hasOption("win32")) BuildExecutor.executeAnt("build-windows32.xml", "-v", jniPath);
        if (all || cmd.hasOption("win64")) BuildExecutor.executeAnt("build-windows64.xml", "-v", jniPath);
        if (all || cmd.hasOption("mac64")) BuildExecutor.executeAnt("build-macosx64.xml", "-v", jniPath);
        if (all || cmd.hasOption("mac32")) BuildExecutor.executeAnt("build-macosx32.xml", "-v", jniPath);
        if (all || cmd.hasOption("ios32")) {
            BuildExecutor.executeAnt("build-ios32.xml", "-v", jniPath);
        }
        if (all || cmd.hasOption("android")) BuildExecutor.executeAnt("build-android32.xml", "-v", jniPath);

        syncPrecopmiledLibs();


        BuildExecutor.executeAnt("build.xml", "-v", jniPath);


        //##############################################
        // Test native SQLite
        //##############################################
        runTest();


        //copy libs to local modules


        FileDescriptor java = projectPath.child("GdxSqlite/build/libs/GdxSqlite-1.0.jar");

        FileDescriptor core = projectPath.child("core");
        FileDescriptor coreJar = projectPath.child("GdxSqlite/build/libs");
        coreJar.copyTo(core);

        FileDescriptor desktop = projectPath.child("desktop/libs/");
        FileDescriptor test = projectPath.child("GdxSqlite/testNatives/");
        FileDescriptor desktopNative = projectPath.child("GdxSqliteBuild/libs/GdxSqlite-platform-1.0-natives-desktop.jar");


        desktop.mkdirs();
        test.mkdirs();
        desktopNative.copyTo(desktop);
        desktopNative.copyTo(test);

        FileDescriptor androidNative_arm64 = projectPath.child("GdxSqliteBuild/libs/arm64-v8a/libGdxSqlite.so");
        FileDescriptor androidNative_arm = projectPath.child("GdxSqliteBuild/libs/armeabi/libGdxSqlite.so");
        FileDescriptor androidNative_armv7 = projectPath.child("GdxSqliteBuild/libs/armeabi-v7a/libGdxSqlite.so");
        FileDescriptor androidNative_x86 = projectPath.child("GdxSqliteBuild/libs/x86/libGdxSqlite.so");
        FileDescriptor androidNative_x86_64 = projectPath.child("GdxSqliteBuild/libs/x86_64/libGdxSqlite.so");

        FileDescriptor androidLibs = projectPath.child("android/libs/");

        try {
            androidLibs.mkdirs();
            androidNative_arm64.copyTo(androidLibs.child("arm64-v8a/libGdxSqlite.so"));
            androidNative_arm.copyTo(androidLibs.child("armeabi/libGdxSqlite.so"));
            androidNative_armv7.copyTo(androidLibs.child("armeabi-v7a/libGdxSqlite.so"));
            androidNative_x86.copyTo(androidLibs.child("x86/libGdxSqlite.so"));
            androidNative_x86_64.copyTo(androidLibs.child("x86_64/libGdxSqlite.so"));
            java.copyTo(androidLibs.child(java.name()));
        } catch (Exception e) {

        }


        //copy to iOS
        try {
            FileDescriptor iOSLibs = projectPath.child("ios/libs/");

            FileDescriptor iOSNative = projectPath.child("GdxSqliteBuild/libs/ios32/libGdxSqlite.a");
            iOSLibs.mkdirs();
            java.copyTo(iOSLibs.child(java.name()));
            iOSNative.copyTo(iOSLibs.child(iOSNative.name()));
        } catch (Exception e) {

        }

    }

    private static void syncPrecopmiledLibs() {
        FileDescriptor libsPath = new FileDescriptor("../GdxSqliteBuild/libs/");
        FileDescriptor precompiledLibsPath = new FileDescriptor("../GdxSqliteBuild/precompiledLibs/");
        sync(libsPath, precompiledLibsPath, "windows64");
        sync(libsPath, precompiledLibsPath, "armeabi");
        sync(libsPath, precompiledLibsPath, "armeabi-v7a");
        sync(libsPath, precompiledLibsPath, "arm64-v8a");
        sync(libsPath, precompiledLibsPath, "ios32");
        sync(libsPath, precompiledLibsPath, "linux32");
        sync(libsPath, precompiledLibsPath, "linux64");
        sync(libsPath, precompiledLibsPath, "macosx32");
        sync(libsPath, precompiledLibsPath, "macosx64");
        sync(libsPath, precompiledLibsPath, "x86");
        sync(libsPath, precompiledLibsPath, "x86_64");
    }

    private static void sync(FileDescriptor libsPath, FileDescriptor precompiledLibsPath, String folder) {
        FileDescriptor lib = libsPath.child(folder);
        FileDescriptor pre = precompiledLibsPath.child(folder);

        if (folderExistAndNotEmpty(lib)) {
            //copy to precompiled!
            lib.copyTo(precompiledLibsPath);
            System.out.println("New    compiled :" + folder);
        } else {
            //get from precompiled if exist
            if (pre.exists()) {
                pre.copyTo(libsPath);
                System.out.println("Use precompiled :" + folder);
            }
        }
    }

    private static boolean folderExistAndNotEmpty(FileDescriptor folder) {

        if (!folder.exists() || !folder.isDirectory()) return false;
        return (folder.list().length > 0);
    }

    private static void runTest() {
        //delete alt test folder
        FileHandle clear = new FileHandle("test");
        clear.deleteDirectory();


        try {
            new JniGenSharedLibraryLoader("libs/GdxSqlite-platform-1.0-natives-desktop.jar").load("GdxSqlite");


            System.out.println(GdxSqlite.getSqliteVersion());

            try {
                FileHandle fileHandleExc = new FileHandle("test/fail/testDB.db3");
                GdxSqlite dbexc = new GdxSqlite(fileHandleExc);
                dbexc.openOrCreateDatabase();
            } catch (SQLiteGdxException e) {
                e.printStackTrace();
            }


            FileHandle fileHandle = new FileHandle("test/testDB.db3");
            fileHandle.parent().mkdirs();
            GdxSqlite db = new GdxSqlite(fileHandle);
            db.openOrCreateDatabase();

            System.out.println("Pointer to open DB: " + db.ptr);


            String sql = "CREATE TABLE COMPANY( \n" +
                    "         ID INT PRIMARY KEY     NOT NULL, \n" +
                    "         NAME           TEXT    NOT NULL, \n" +
                    "         AGE            INT     NOT NULL, \n" +
                    "         ADDRESS        CHAR(50), \n" +
                    "         SALARY         REAL );";
            db.execSQL(sql);


            sql = " INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY)    \n" +
                    "          VALUES (1, 'Paul', 32, 'California', 20000.00 );   \n" +
                    "          INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY)    \n" +
                    "          VALUES (2, 'Allen', 25, 'Texas', 15000.00 );       \n" +
                    "          INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY)  \n" +
                    "          VALUES (3, 'Teddy', 23, 'Norway', NULL );  \n" +
                    "          INSERT INTO COMPANY (ID,NAME,AGE,ADDRESS,SALARY)  \n" +
                    "          VALUES (4, 'Mark', 25, 'Rich-Mond ', 65000.00 );";
            db.execSQL(sql);

            sql = "SELECT * from COMPANY";

            db.rawQuery(sql, new GdxSqlite.RowCallback() {
                @Override
                public void newRow(String[] columnNames, Object[] values, int[] types) {
                    System.out.println("Native Callback : "
                            + " => " + arrayToString(columnNames)
                            + " => " + arrayToString(values)
                    );
                }
            });


            GdxSqliteCursor cursor = db.rawQuery(sql);

            cursor.moveToFirst();
            StringBuilder sb = new StringBuilder();
            while (cursor.isAfterLast() == false) {
                sb.append("Cursor row : => [");
                sb.append(cursor.getInt(0)).append(", ");
                sb.append(cursor.getString(1)).append(", ");
                sb.append(cursor.getInt(2)).append(", ");
                sb.append(cursor.getString(3)).append(", ");
                sb.append(cursor.isNull(4) ? "NULL" : cursor.getDouble(4)).append("] ");
                System.out.println(sb.toString());
                sb.length = 0; //clear
                cursor.moveToNext();
            }
            cursor.close();

            db.closeDatabase();
            System.out.println("Pointer to closed DB: " + db.ptr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Test prepared statement
        try {
            FileHandle fileHandle = new FileHandle("test/testDB2.db3");
            fileHandle.parent().mkdirs();
            GdxSqlite db = new GdxSqlite(fileHandle);
            db.openOrCreateDatabase();


            String sql = "CREATE TABLE COMPANY( \n" +
                    "  NAME  TEXT PRIMARY KEY  NOT NULL);";
            db.execSQL(sql);

            String statement = "INSERT INTO COMPANY VALUES (?)";
            GdxSqlitePreparedStatement preparedStatement = db.prepare(statement);

            preparedStatement.bind("Test1").commit().reset();
            preparedStatement.bind("Test2").commit().reset();
            preparedStatement.bind("Test3").commit().reset();
            preparedStatement.bind("Test4").commit().reset();

            preparedStatement.close();

            try {
                preparedStatement.bind("Test5").commit().reset();
            } catch (Exception e) {
                e.printStackTrace();
            }

            db.closeDatabase();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String arrayToString(Object[] items) {

        if (items == null) return "NULL";

        if (items.length == 0) return "[]";
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(items[0]);
        for (int i = 1; i < items.length; i++) {
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }


    private static CommandLine getCommandLine(String[] args) {
        Options options = new Options();


        Option all = new Option("a", "all", false, "compile for all platforms");
        all.setRequired(false);
        options.addOption(all);

        Option mac64 = new Option(null, "mac64", false, "compile for mac 64 bit");
        mac64.setRequired(false);
        options.addOption(mac64);

        Option mac32 = new Option(null, "mac32", false, "compile for mac 32 bit");
        mac32.setRequired(false);
        options.addOption(mac32);

        Option linux32 = new Option(null, "linux32", false, "compile for linux 32 bit");
        linux32.setRequired(false);
        options.addOption(linux32);

        Option linux64 = new Option(null, "linux64", false, "compile for linux 64 bit");
        linux64.setRequired(false);
        options.addOption(linux64);

        Option win32 = new Option(null, "win32", false, "compile for windows 32 bit");
        win32.setRequired(false);
        options.addOption(win32);

        Option win64 = new Option(null, "win64", false, "compile for windows 64 bit");
        win64.setRequired(false);
        options.addOption(win64);

        Option ios32 = new Option(null, "ios32", false, "compile for iOs");
        ios32.setRequired(false);
        options.addOption(ios32);

        Option android = new Option(null, "android", false, "compile for Android");
        android.setRequired(false);
        options.addOption(android);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("SQLite native builder", options);

            System.exit(1);
            return null;
        }
        return cmd;
    }

}
